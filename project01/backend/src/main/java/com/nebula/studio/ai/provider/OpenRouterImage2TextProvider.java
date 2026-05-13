package com.nebula.studio.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class OpenRouterImage2TextProvider implements Image2TextProvider {

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String DEFAULT_MODEL = "meta-llama/llama-3.2-11b-vision-instruct:free";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DynamicConfigService dynamicConfigService;

    @Value("${ai.openrouter.model:meta-llama/llama-3.2-11b-vision-instruct:free}")
    private String model;

    @Value("${ai.openrouter.api-key:}")
    private String defaultApiKey;

    public OpenRouterImage2TextProvider(DynamicConfigService dynamicConfigService) {
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
        dynamicConfigService.registerDefault("ai.openrouter.api-key", defaultApiKey);
    }

    @Override
    public String getProviderName() {
        return "openrouter";
    }

    @Override
    public boolean isAvailable() {
        String apiKey = dynamicConfigService.getConfigValue("ai.openrouter.api-key");
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public Image2TextResult analyze(byte[] imageBytes, String mimeType, String prompt) {
        String apiKey = dynamicConfigService.getConfigValue("ai.openrouter.api-key");
        if (apiKey == null || apiKey.isBlank()) {
            return Image2TextResult.fail("OpenRouter API Key 未配置", getProviderName());
        }
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;
            return callVisionApi(dataUrl, prompt, apiKey);
        } catch (Exception e) {
            log.error("OpenRouter 图生文失败: {}", e.getMessage());
            return Image2TextResult.fail("分析失败: " + e.getMessage(), getProviderName());
        }
    }

    @Override
    public Image2TextResult analyzeUrl(String imageUrl, String prompt) {
        String apiKey = dynamicConfigService.getConfigValue("ai.openrouter.api-key");
        if (apiKey == null || apiKey.isBlank()) {
            return Image2TextResult.fail("OpenRouter API Key 未配置", getProviderName());
        }
        try {
            return callVisionApi(imageUrl, prompt, apiKey);
        } catch (Exception e) {
            log.error("OpenRouter 图生文(URL)失败: {}", e.getMessage());
            return Image2TextResult.fail("分析失败: " + e.getMessage(), getProviderName());
        }
    }

    private Image2TextResult callVisionApi(String imageData, String prompt, String apiKey) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);

        ArrayNode messages = body.putArray("messages");
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");

        ArrayNode content = userMsg.putArray("content");
        ObjectNode textPart = content.addObject();
        textPart.put("type", "text");
        textPart.put("text", prompt != null ? prompt : "请详细描述这张图片的内容，包括场景、物体、风格、颜色等。");

        ObjectNode imagePart = content.addObject();
        imagePart.put("type", "image_url");
        ObjectNode imageUrlObj = imagePart.putObject("image_url");
        imageUrlObj.put("url", imageData);

        RequestBody httpBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(API_URL)
                .post(httpBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("HTTP-Referer", "https://nebula-studio.app")
                .addHeader("X-Title", "Nebula Studio")
                .build();

        log.info("调用 OpenRouter 图生文, model={}", model);

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "unknown";
                return Image2TextResult.fail("API 返回错误 HTTP " + response.code() + ": " + errBody, getProviderName());
            }
            String respStr = response.body().string();
            JsonNode root = objectMapper.readTree(respStr);

            StringBuilder result = new StringBuilder();
            if (root.has("choices") && root.get("choices").isArray()) {
                for (JsonNode choice : root.get("choices")) {
                    if (choice.has("message") && choice.get("message").has("content")) {
                        result.append(choice.get("message").get("content").asText());
                    }
                }
            }
            if (result.isEmpty()) {
                return Image2TextResult.fail("API 未返回有效内容", getProviderName());
            }
            return Image2TextResult.success(result.toString(), getProviderName());
        }
    }
}
