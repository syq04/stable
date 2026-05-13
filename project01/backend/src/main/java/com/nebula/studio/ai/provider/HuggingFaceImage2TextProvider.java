package com.nebula.studio.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.studio.service.DynamicConfigService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class HuggingFaceImage2TextProvider implements Image2TextProvider {

    private static final String API_BASE = "https://api-inference.huggingface.co/models/";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DynamicConfigService dynamicConfigService;

    @Value("${ai.huggingface.image2text.model:Salesforce/blip-image-captioning-large}")
    private String model;

    @Value("${ai.huggingface.api-key:}")
    private String defaultApiKey;

    public HuggingFaceImage2TextProvider(DynamicConfigService dynamicConfigService) {
        this.dynamicConfigService = dynamicConfigService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
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
    public Image2TextResult analyze(byte[] imageBytes, String mimeType, String prompt) {
        String apiKey = dynamicConfigService.getConfigValue("ai.huggingface.api-key");
        if (apiKey == null || apiKey.isBlank()) {
            return Image2TextResult.fail("HuggingFace API Key 未配置", getProviderName());
        }
        try {
            RequestBody httpBody = RequestBody.create(imageBytes, MediaType.parse(mimeType));
            Request request = new Request.Builder()
                    .url(API_BASE + model)
                    .post(httpBody)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            log.info("调用 HuggingFace 图生文, model={}", model);

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "unknown";
                    return Image2TextResult.fail("API 返回错误 HTTP " + response.code() + ": " + errBody, getProviderName());
                }
                String respStr = response.body().string();
                JsonNode root = objectMapper.readTree(respStr);

                StringBuilder result = new StringBuilder();
                if (root.isArray()) {
                    for (JsonNode item : root) {
                        if (item.has("generated_text")) {
                            result.append(item.get("generated_text").asText());
                        }
                    }
                }
                if (result.isEmpty() && root.isObject() && root.has("generated_text")) {
                    result.append(root.get("generated_text").asText());
                }
                if (result.isEmpty()) {
                    return Image2TextResult.fail("API 未返回有效内容: " + respStr, getProviderName());
                }
                return Image2TextResult.success(result.toString(), getProviderName());
            }
        } catch (Exception e) {
            log.error("HuggingFace 图生文失败: {}", e.getMessage());
            return Image2TextResult.fail("分析失败: " + e.getMessage(), getProviderName());
        }
    }

    @Override
    public Image2TextResult analyzeUrl(String imageUrl, String prompt) {
        return Image2TextResult.fail("HuggingFace 图生文不支持URL输入，请上传图片文件", getProviderName());
    }
}
