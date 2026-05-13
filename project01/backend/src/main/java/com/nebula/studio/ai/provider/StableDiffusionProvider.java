package com.nebula.studio.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.studio.dto.request.Text2ImageRequest;
import com.nebula.studio.entity.LoraModel;
import com.nebula.studio.service.LoraModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Stable Diffusion 1.5 / SDXL 文生图 Provider
 * 对接 SD WebUI API (sdapi/v1/txt2img)
 * 支持 LoRA 模型注入: <lora:name:weight>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StableDiffusionProvider implements Text2ImageProvider {

    private final ObjectMapper objectMapper;
    private final LoraModelService loraModelService;

    @Value("${ai.sd.api.url:http://127.0.0.1:7860}")
    private String sdApiUrl;

    private static final String TXT2IMG_ENDPOINT = "/sdapi/v1/txt2img";

    private volatile Boolean cachedAvailable;
    private volatile long lastCheckTime;
    private static final long CHECK_INTERVAL_MS = 30_000;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public String getProviderName() {
        return "stable-diffusion";
    }

    @Override
    public boolean isAvailable() {
        long now = System.currentTimeMillis();
        if (cachedAvailable != null && (now - lastCheckTime) < CHECK_INTERVAL_MS) {
            return cachedAvailable;
        }
        try {
            Request request = new Request.Builder()
                    .url(sdApiUrl + "/sdapi/v1/sd-models")
                    .get()
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                cachedAvailable = response.isSuccessful();
                lastCheckTime = now;
                return cachedAvailable;
            }
        } catch (Exception e) {
            log.warn("Stable Diffusion API 不可用 ({}): {}", sdApiUrl, e.getMessage());
            cachedAvailable = false;
            lastCheckTime = now;
            return false;
        }
    }

    @Override
    public Text2ImageResult generate(Text2ImageRequest request, String builtPrompt) {
        try {
            SdText2ImgRequest sdRequest = buildSdRequest(request, builtPrompt);

            String jsonBody = objectMapper.writeValueAsString(sdRequest);
            log.info("调用 Stable Diffusion API: {}, prompt: {}", sdApiUrl, builtPrompt);

            Request httpRequest = new Request.Builder()
                    .url(sdApiUrl + TXT2IMG_ENDPOINT)
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "unknown";
                    return Text2ImageResult.fail("SD API 错误 HTTP " + response.code() + ": " + errBody, getProviderName());
                }

                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null) {
                    return Text2ImageResult.fail("SD API 未返回数据", getProviderName());
                }

                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode imagesNode = root.get("images");
                if (imagesNode == null || !imagesNode.isArray() || imagesNode.size() == 0) {
                    return Text2ImageResult.fail("SD API 返回图片数据为空", getProviderName());
                }

                // SD API 返回 base64 编码的图片
                String base64Image = imagesNode.get(0).asText();
                byte[] imageData = java.util.Base64.getDecoder().decode(base64Image);

                Text2ImageResult result = Text2ImageResult.success(imageData, getProviderName());
                result.setSeed(sdRequest.seed);
                return result;
            }
        } catch (IOException e) {
            log.error("Stable Diffusion 调用失败: {}", e.getMessage());
            return Text2ImageResult.fail("调用失败: " + e.getMessage(), getProviderName());
        }
    }

    /**
     * 构建 SD API 请求体
     * 支持 LoRA 注入: <lora:name:weight> 放在 prompt 最前面
     */
    private SdText2ImgRequest buildSdRequest(Text2ImageRequest request, String builtPrompt) {
        SdText2ImgRequest req = new SdText2ImgRequest();

        // 处理 LoRA：通过 loraModelId 查询模型名称，注入到 prompt
        String finalPrompt = builtPrompt;
        if (request.getLoraModelId() != null) {
            try {
                LoraModel lora = loraModelService.getById(request.getLoraModelId());
                if (lora != null && "ACTIVE".equals(lora.getStatus())) {
                    double weight = request.getLoraWeight() != null ? request.getLoraWeight() : 0.7;
                    String loraTag = "<lora:" + lora.getName() + ":" + weight + ">";
                    finalPrompt = loraTag + " " + builtPrompt;
                    log.info("注入 LoRA 模型: {} (weight={})", lora.getName(), weight);
                }
            } catch (Exception e) {
                log.warn("LoRA 模型注入失败: {}", e.getMessage());
            }
        }

        req.prompt = finalPrompt;
        req.negative_prompt = request.getNegativePrompt() != null ? request.getNegativePrompt() : "";
        req.width = request.getWidth() != null ? request.getWidth() : 512;
        req.height = request.getHeight() != null ? request.getHeight() : 512;
        req.steps = request.getSteps() != null ? request.getSteps() : 20;
        req.cfg_scale = request.getCfgScale() != null ? request.getCfgScale() : 7.5;
        req.sampler_index = request.getSamplerName() != null ? request.getSamplerName() : "Euler a";
        req.n_iter = 1;
        req.batch_size = 1;
        req.seed = request.getSeed() != null ? request.getSeed().intValue() : -1; // -1 = 随机

        return req;
    }

    /**
     * SD API txt2img 请求体
     */
    private static class SdText2ImgRequest {
        public String prompt;
        public String negative_prompt;
        public Integer width;
        public Integer height;
        public Integer steps;
        public Double cfg_scale;
        public String sampler_index;
        public Integer n_iter;
        public Integer batch_size;
        public Integer seed = -1;
        public Boolean restore_faces = false;
        public Boolean tiling = false;
    }
}
