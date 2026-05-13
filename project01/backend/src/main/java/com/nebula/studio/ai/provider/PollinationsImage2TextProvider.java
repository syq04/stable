package com.nebula.studio.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PollinationsImage2TextProvider implements Image2TextProvider {

    private static final String CHAT_API_URL = "https://text.pollinations.ai/openai";
    private static final String MODEL = "openai-fast";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private volatile Boolean cachedAvailable;
    private volatile long lastCheckTime;
    private static final long CHECK_INTERVAL_MS = 60_000;

    public PollinationsImage2TextProvider() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getProviderName() {
        return "pollinations";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Image2TextResult analyze(byte[] imageBytes, String mimeType, String prompt) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;
            return callVisionApi(dataUrl, prompt);
        } catch (Exception e) {
            log.error("Pollinations 图生文失败: {}", e.getMessage());
            return Image2TextResult.fail("分析失败: " + e.getMessage(), getProviderName());
        }
    }

    @Override
    public Image2TextResult analyzeUrl(String imageUrl, String prompt) {
        try {
            return callVisionApi(imageUrl, prompt);
        } catch (Exception e) {
            log.error("Pollinations 图生文(URL)失败: {}", e.getMessage());
            return Image2TextResult.fail("分析失败: " + e.getMessage(), getProviderName());
        }
    }

    private Image2TextResult callVisionApi(String imageData, String prompt) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", MODEL);

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
                .url(CHAT_API_URL)
                .post(httpBody)
                .addHeader("Content-Type", "application/json")
                .build();

        log.info("调用 Pollinations.ai 图生文");

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
