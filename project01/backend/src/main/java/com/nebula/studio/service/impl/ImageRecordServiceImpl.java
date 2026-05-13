package com.nebula.studio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.studio.ai.provider.*;
import com.nebula.studio.common.BusinessException;
import com.nebula.studio.dto.request.Text2ImageRequest;
import com.nebula.studio.entity.ImageRecord;
import com.nebula.studio.entity.Style;
import com.nebula.studio.mapper.ImageRecordMapper;
import com.nebula.studio.service.FileStorageService;
import com.nebula.studio.service.ImageRecordService;
import com.nebula.studio.service.LoraModelService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageRecordServiceImpl extends ServiceImpl<ImageRecordMapper, ImageRecord> implements ImageRecordService {

    private final StyleService styleService;
    private final FileStorageService fileStorageService;
    private final AiProviderManager providerManager;
    private final LoraModelService loraModelService;
    private final ObjectMapper objectMapper;

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
                log.error("存储图片失败: {}", e.getMessage());
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
    public ImageRecord image2Text(Long userId, MultipartFile image) {
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
            Image2TextResult result = provider.analyze(image.getBytes(), image.getContentType(), null);
            if (result.isSuccess()) {
                record.setOutputContent(result.getDescription());
                record.setStatus("SUCCESS");
            } else {
                record.setOutputContent("分析失败: " + result.getErrorMessage());
                record.setStatus("FAILED");
            }
        } catch (Exception e) {
            log.error("图生文处理失败: {}", e.getMessage());
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
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build();
            Request request = new Request.Builder().url(url).get().build();
            try (Response response = client.newCall(request).execute()) {
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
