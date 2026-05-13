package com.nebula.studio.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nebula.studio.dto.request.Text2ImageRequest;
import com.nebula.studio.service.DynamicConfigService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SiliconFlowText2ImageProvider implements Text2ImageProvider {

    private static final String API_URL = "https://api.siliconflow.cn/v1/images/generations";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DynamicConfigService dynamicConfigService;

    @Value("${ai.siliconflow.text2image-model:Kwai-Kolors/Kolors}")
    private String model;

    @Value("${ai.siliconflow.api-key:}")
    private String defaultApiKey;

    public SiliconFlowText2ImageProvider(DynamicConfigService dynamicConfigService) {
        this.dynamicConfigService = dynamicConfigService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        dynamicConfigService.registerDefault("ai.siliconflow.api-key", defaultApiKey);
    }

    @Override
    public String getProviderName() {
        return "siliconflow";
    }

    @Override
    public boolean isAvailable() {
        String apiKey = dynamicConfigService.getConfigValue("ai.siliconflow.api-key");
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public Text2ImageResult generate(Text2ImageRequest request, String builtPrompt) {
        String apiKey = dynamicConfigService.getConfigValue("ai.siliconflow.api-key");
        if (apiKey == null || apiKey.isBlank()) {
            return Text2ImageResult.fail("硅基流动 API Key 未配置", getProviderName());
        }
        try {
            int width = request.getWidth() != null ? request.getWidth() : 1024;
            int height = request.getHeight() != null ? request.getHeight() : 1024;

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("prompt", builtPrompt);
            body.put("image_size", width + "x" + height);
            body.put("num_inference_steps", 20);
            body.put("batch_size", 1);

            if (request.getNegativePrompt() != null && !request.getNegativePrompt().isBlank()) {
                body.put("negative_prompt", request.getNegativePrompt());
            }

            int seed = request.getSeed() != null ? request.getSeed().intValue() : (int) (System.currentTimeMillis() % 2147483647);
            body.put("seed", seed);

            RequestBody httpBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
            Request httpRequest = new Request.Builder()
                    .url(API_URL)
                    .post(httpBody)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            log.info("调用硅基流动文生图, model={}, prompt={}, size={}x{}", model, builtPrompt, width, height);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "unknown";
                    return Text2ImageResult.fail("硅基流动 API 返回错误 HTTP " + response.code() + ": " + errBody, getProviderName());
                }
                String respStr = response.body().string();
                JsonNode root = objectMapper.readTree(respStr);

                if (root.has("images") && root.get("images").isArray() && root.get("images").size() > 0) {
                    String imageUrl = root.get("images").get(0).get("url").asText();
                    Text2ImageResult result = Text2ImageResult.success(imageUrl, getProviderName());
                    result.setSeed(seed);
                    return result;
                }
                return Text2ImageResult.fail("硅基流动 API 未返回有效图片", getProviderName());
            }
        } catch (IOException e) {
            log.error("硅基流动文生图调用失败: {}", e.getMessage());
            return Text2ImageResult.fail("调用失败: " + e.getMessage(), getProviderName());
        }
    }
}
