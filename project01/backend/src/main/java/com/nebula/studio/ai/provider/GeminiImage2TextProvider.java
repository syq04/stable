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
public class GeminiImage2TextProvider implements Image2TextProvider {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DynamicConfigService dynamicConfigService;

    @Value("${ai.gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${ai.gemini.api-key:}")
    private String defaultApiKey;

    public GeminiImage2TextProvider(DynamicConfigService dynamicConfigService) {
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
        dynamicConfigService.registerDefault("ai.gemini.api-key", defaultApiKey);
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public boolean isAvailable() {
        String apiKey = dynamicConfigService.getConfigValue("ai.gemini.api-key");
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public Image2TextResult analyze(byte[] imageBytes, String mimeType, String prompt) {
        String apiKey = dynamicConfigService.getConfigValue("ai.gemini.api-key");
        if (apiKey == null || apiKey.isBlank()) {
            return Image2TextResult.fail("Gemini API Key 未配置", getProviderName());
        }
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode contents = body.putArray("contents");
            ObjectNode contentObj = contents.addObject();
            ArrayNode parts = contentObj.putArray("parts");

            ObjectNode textPart = parts.addObject();
            textPart.put("text", prompt != null ? prompt : "请详细描述这张图片的内容，包括场景、物体、风格、颜色等。");

            ObjectNode imagePart = parts.addObject();
            ObjectNode inlineData = imagePart.putObject("inline_data");
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", base64Image);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            RequestBody httpBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(httpBody)
                    .addHeader("Content-Type", "application/json")
                    .build();

            log.info("调用 Google Gemini 图生文, model={}", model);

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "unknown";
                    return Image2TextResult.fail("Gemini API 返回错误 HTTP " + response.code() + ": " + errBody, getProviderName());
                }
                String respStr = response.body().string();
                JsonNode root = objectMapper.readTree(respStr);

                StringBuilder result = new StringBuilder();
                if (root.has("candidates") && root.get("candidates").isArray()) {
                    for (JsonNode candidate : root.get("candidates")) {
                        if (candidate.has("content") && candidate.get("content").has("parts")) {
                            for (JsonNode part : candidate.get("content").get("parts")) {
                                if (part.has("text")) {
                                    result.append(part.get("text").asText());
                                }
                            }
                        }
                    }
                }
                if (result.isEmpty()) {
                    return Image2TextResult.fail("Gemini API 未返回有效内容", getProviderName());
                }
                return Image2TextResult.success(result.toString(), getProviderName());
            }
        } catch (Exception e) {
            log.error("Gemini 图生文失败: {}", e.getMessage());
            return Image2TextResult.fail("分析失败: " + e.getMessage(), getProviderName());
        }
    }

    @Override
    public Image2TextResult analyzeUrl(String imageUrl, String prompt) {
        return Image2TextResult.fail("Gemini 图生文暂不支持URL输入，请上传图片文件", getProviderName());
    }
}
