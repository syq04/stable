package com.nebula.studio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.studio.ai.provider.*;
import com.nebula.studio.common.BusinessException;
import com.nebula.studio.dto.request.Text2ImageRequest;
import com.nebula.studio.entity.ImageRecord;
import com.nebula.studio.entity.Style;
import com.nebula.studio.mapper.ImageRecordMapper;
import com.nebula.studio.service.FileStorageService;
import com.nebula.studio.service.ImageRecordService;
import com.nebula.studio.service.StyleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageRecordServiceImpl extends ServiceImpl<ImageRecordMapper, ImageRecord> implements ImageRecordService {

    private final StyleService styleService;
    private final FileStorageService fileStorageService;
    private final AiProviderManager providerManager;
    private final ObjectMapper objectMapper;

    private final OkHttpClient downloadClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build();

    @Override
    public ImageRecord text2Image(Long userId, Text2ImageRequest request) {
        String prompt = buildPrompt(request);

        ImageRecord record = new ImageRecord();
        record.setUserId(userId);
        record.setStyleId(request.getStyleId());
        record.setType("TEXT2IMAGE");
        record.setInputContent(request.getPrompt());
        record.setStatus("PENDING");
        record.setCreatedBy(userId);
        save(record);

        Text2ImageProvider provider = providerManager.getText2ImageProvider();
        log.info("文生图使用提供商: {}, 可用: {}", provider.getProviderName(), provider.isAvailable());

        Text2ImageResult result = provider.generate(request, prompt);

        if (result.isSuccess()) {
            try {
                String imageUrl;
                if (result.getImageData() != null && result.getImageData().length > 0) {
                    String filename = result.getProviderName() + "_" + result.getSeed() + ".png";
                    imageUrl = fileStorageService.store(result.getImageData(), "text2image", filename);
                } else if (StringUtils.hasText(result.getImageUrl())) {
                    byte[] downloaded = downloadImage(result.getImageUrl());
                    if (downloaded != null && downloaded.length > 0) {
                        String filename = result.getProviderName() + "_" + result.getSeed() + ".png";
                        imageUrl = fileStorageService.store(downloaded, "text2image", filename);
                    } else {
                        imageUrl = result.getImageUrl();
                    }
                } else {
                    imageUrl = "/generated/error.png";
                }
                record.setImageUrl(imageUrl);
                record.setOutputContent(buildOutputContent(prompt, request, result.getProviderName()));
                record.setStatus("SUCCESS");
            } catch (Exception e) {
                log.error("存储图片失败", e);
                record.setOutputContent("存储失败: " + e.getMessage());
                record.setStatus("FAILED");
            }
        } else {
            record.setOutputContent("生成失败: " + result.getErrorMessage());
            record.setStatus("FAILED");
        }

        updateById(record);
        return record;
    }

    @Override
    public Map<String, Object> evaluate(Long userId, Text2ImageRequest request) {
        String prompt = buildPrompt(request);

        Text2ImageProvider t2iProvider = providerManager.getText2ImageProvider();
        if (t2iProvider == null) {
            throw new BusinessException("文生图服务未配置");
        }
        log.info("评估-文生图使用提供商: {}, 可用: {}", t2iProvider.getProviderName(), t2iProvider.isAvailable());

        Text2ImageResult t2iResult = t2iProvider.generate(request, prompt);

        if (!t2iResult.isSuccess()) {
            throw new BusinessException("图片生成失败: " + t2iResult.getErrorMessage());
        }

        byte[] imageData = t2iResult.getImageData();
        if (imageData == null || imageData.length == 0) {
            throw new BusinessException("图片生成结果为空");
        }

        String filename = t2iResult.getProviderName() + "_eval_" + t2iResult.getSeed() + ".png";
        String imageUrl = fileStorageService.store(imageData, "text2image", filename);

        ImageRecord record = new ImageRecord();
        record.setUserId(userId);
        record.setStyleId(request.getStyleId());
        record.setType("TEXT2IMAGE");
        record.setInputContent(request.getPrompt());
        record.setImageUrl(imageUrl);
        record.setCreatedBy(userId);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("prompt", prompt);
        output.put("width", request.getWidth() != null ? request.getWidth() : 512);
        output.put("height", request.getHeight() != null ? request.getHeight() : 512);
        output.put("steps", request.getSteps() != null ? request.getSteps() : 20);
        output.put("cfgScale", request.getCfgScale() != null ? request.getCfgScale() : 7.5);
        output.put("provider", t2iResult.getProviderName());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("imageUrl", imageUrl);
        response.put("originalPrompt", request.getPrompt());
        response.put("builtPrompt", prompt);
        response.put("seed", t2iResult.getSeed());

        boolean evalOk = false;
        try {
            Image2TextProvider i2tProvider = providerManager.getImage2TextProvider();
            if (i2tProvider != null) {
                log.info("评估-图生文使用提供商: {}, 可用: {}", i2tProvider.getProviderName(), i2tProvider.isAvailable());
                Image2TextResult evalResult = i2tProvider.evaluateImageQuality(imageData, "image/png", request.getPrompt());

                if (evalResult.isSuccess()) {
                    output.put("evaluation", evalResult.getDescription());
                    output.put("accuracy", evalResult.getAccuracy());
                    output.put("accuracyDetail", evalResult.getAccuracyDetail());
                    response.put("accuracy", evalResult.getAccuracy());
                    response.put("accuracyDetail", evalResult.getAccuracyDetail());
                    evalOk = true;
                    if (evalResult.getDescription() != null) {
                        try {
                            JsonNode analysisNode = objectMapper.readTree(evalResult.getDescription());
                            response.put("analysis", objectMapper.convertValue(analysisNode, Map.class));
                        } catch (Exception e) {
                            response.put("analysisRaw", evalResult.getDescription());
                        }
                    }
                } else {
                    output.put("evalError", evalResult.getErrorMessage());
                    response.put("evalError", evalResult.getErrorMessage());
                }
            } else {
                output.put("evalError", "图生文服务未配置");
                response.put("evalError", "图生文服务未配置");
            }
        } catch (Exception e) {
            log.error("评估过程异常", e);
            output.put("evalError", e.getMessage());
            response.put("evalError", e.getMessage());
        }

        if (evalOk) {
            record.setStatus("SUCCESS");
        } else {
            record.setStatus("EVAL_FAILED");
        }
        response.put("status", record.getStatus());

        try {
            record.setOutputContent(objectMapper.writeValueAsString(output));
        } catch (Exception e) {
            log.error("构建评估输出失败", e);
            record.setOutputContent("记录构建失败: " + e.getMessage());
        }

        save(record);
        response.put("recordId", record.getId());
        return response;
    }

    @Override
    public ImageRecord image2Text(Long userId, MultipartFile image, String analysisType) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException("请上传图片");
        }

        String imageUrl = fileStorageService.store(image, "image2text");

        ImageRecord record = new ImageRecord();
        record.setUserId(userId);
        record.setType("IMAGE2TEXT");
        record.setInputContent(image.getOriginalFilename());
        record.setImageUrl(imageUrl);
        record.setStatus("PENDING");
        record.setCreatedBy(userId);
        save(record);

        Image2TextProvider provider = providerManager.getImage2TextProvider();
        log.info("图生文使用提供商: {}, 可用: {}", provider.getProviderName(), provider.isAvailable());

        try {
            Image2TextResult result = provider.analyze(image.getBytes(), image.getContentType(), analysisType);
            if (result.isSuccess()) {
                record.setOutputContent(result.getDescription());
                record.setStatus("SUCCESS");
            } else {
                record.setOutputContent("分析失败: " + result.getErrorMessage());
                record.setStatus("FAILED");
            }
        } catch (Exception e) {
            log.error("图生文处理失败", e);
            record.setOutputContent("处理失败: " + e.getMessage());
            record.setStatus("FAILED");
        }

        updateById(record);
        return record;
    }

    @Override
    public IPage<ImageRecord> listHistory(Long userId, String type, int page, int size) {
        LambdaQueryWrapper<ImageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImageRecord::getUserId, userId);
        if (StringUtils.hasText(type)) {
            wrapper.eq(ImageRecord::getType, type);
        }
        wrapper.orderByDesc(ImageRecord::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public ImageRecord getByIdAndUser(Long id, Long userId) {
        ImageRecord record = getOne(new LambdaQueryWrapper<ImageRecord>()
                .eq(ImageRecord::getId, id)
                .eq(ImageRecord::getUserId, userId));
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        return record;
    }

    @Override
    public ImageRecord updateRecord(Long id, Long userId, ImageRecord request) {
        ImageRecord record = getByIdAndUser(id, userId);
        if (request.getOutputContent() != null) {
            record.setOutputContent(request.getOutputContent());
        }
        updateById(record);
        return record;
    }

    @Override
    public void deleteRecord(Long id, Long userId) {
        ImageRecord record = getByIdAndUser(id, userId);
        if (StringUtils.hasText(record.getImageUrl())) {
            fileStorageService.delete(record.getImageUrl());
        }
        removeById(record.getId());
    }

    private String buildPrompt(Text2ImageRequest request) {
        String prompt = request.getPrompt();
        if (request.getStyleId() != null) {
            Style style = styleService.getById(request.getStyleId());
            if (style != null && StringUtils.hasText(style.getConfig())) {
                try {
                    Map<String, Object> config = objectMapper.readValue(style.getConfig(), Map.class);
                    String prefix = (String) config.getOrDefault("promptPrefix", "");
                    String suffix = (String) config.getOrDefault("promptSuffix", "");
                    if (StringUtils.hasText(prefix)) prompt = prefix + ", " + prompt;
                    if (StringUtils.hasText(suffix)) prompt = prompt + ", " + suffix;
                } catch (Exception e) {
                    prompt = style.getConfig() + ", " + prompt;
                }
            }
        }
        return prompt;
    }

    private String buildOutputContent(String prompt, Text2ImageRequest request, String providerName) {
        try {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("prompt", prompt);
            output.put("width", request.getWidth() != null ? request.getWidth() : 512);
            output.put("height", request.getHeight() != null ? request.getHeight() : 512);
            output.put("steps", request.getSteps() != null ? request.getSteps() : 20);
            output.put("cfgScale", request.getCfgScale() != null ? request.getCfgScale() : 7.5);
            output.put("provider", providerName);
            return objectMapper.writeValueAsString(output);
        } catch (Exception e) {
            return "Generated image";
        }
    }

    private byte[] downloadImage(String url) {
        try {
            Request request = new Request.Builder().url(url).get().build();
            try (Response response = downloadClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().bytes();
                }
            }
        } catch (Exception e) {
            log.warn("下载图片失败: {}", e.getMessage());
        }
        return null;
    }
}
