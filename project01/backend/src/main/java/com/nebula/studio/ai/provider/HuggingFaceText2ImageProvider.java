package com.nebula.studio.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class HuggingFaceText2ImageProvider implements Text2ImageProvider {

    private static final String API_BASE = "https://api-inference.huggingface.co/models/";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DynamicConfigService dynamicConfigService;

    @Value("${ai.huggingface.text2image.model:stabilityai/stable-diffusion-xl-base-1.0}")
    private String model;

    @Value("${ai.huggingface.api-key:}")
    private String defaultApiKey;

    public HuggingFaceText2ImageProvider(DynamicConfigService dynamicConfigService) {
        this.dynamicConfigService = dynamicConfigService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        dynamicConfigService.registerDefault("ai.huggingface.api-key", defaultApiKey);
    }

    @Override
    public String getProviderName() {
        return "huggingface";
    }

    @Override
    public boolean isAvailable() {
        String apiKey = dynamicConfigService.getConfigValue("ai.huggingface.api-key");
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public Text2ImageResult generate(Text2ImageRequest request, String builtPrompt) {
        String apiKey = dynamicConfigService.getConfigValue("ai.huggingface.api-key");
        if (apiKey == null || apiKey.isBlank()) {
            return Text2ImageResult.fail("HuggingFace API Key 未配置", getProviderName());
        }
        try {
            String jsonBody = objectMapper.writeValueAsString(new Object(){
                public final String inputs = builtPrompt;
                public final Object parameters = new Object() {
                    public final int width = request.getWidth() != null ? request.getWidth() : 512;
                    public final int height = request.getHeight() != null ? request.getHeight() : 512;
                    public final int num_inference_steps = request.getSteps() != null ? request.getSteps() : 20;
                    public final double guidance_scale = request.getCfgScale() != null ? request.getCfgScale() : 7.5;
                };
            });

            RequestBody httpBody = RequestBody.create(jsonBody, MediaType.parse("application/json"));
            Request httpRequest = new Request.Builder()
                    .url(API_BASE + model)
                    .post(httpBody)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            log.info("调用 HuggingFace 文生图, model={}, prompt={}", model, builtPrompt);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "unknown";
                    if (response.code() == 503 && errBody.contains("loading")) {
                        return Text2ImageResult.fail("模型正在加载中，请稍后重试", getProviderName());
                    }
                    return Text2ImageResult.fail("HuggingFace API 返回错误 HTTP " + response.code() + ": " + errBody, getProviderName());
                }
                byte[] imageData = response.body().bytes();
                if (imageData.length < 100) {
                    return Text2ImageResult.fail("返回数据过小，可能生成失败", getProviderName());
                }
                return Text2ImageResult.success(imageData, getProviderName());
            }
        } catch (IOException e) {
            log.error("HuggingFace 文生图调用失败: {}", e.getMessage());
            return Text2ImageResult.fail("调用失败: " + e.getMessage(), getProviderName());
        }
    }
}
