package com.nebula.studio.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DoubaoVisionClient implements AutoCloseable {

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DoubaoVisionClient(String apiUrl, String apiKey, String model, int timeoutSeconds) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String analyzeImage(byte[] imageBytes, String mimeType, String prompt) throws IOException {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + mimeType + ";base64," + base64Image;

        return callVisionApi(dataUrl, prompt);
    }

    public String analyzeImageUrl(String imageUrl, String prompt) throws IOException {
        ObjectNode imageUrlNode = objectMapper.createObjectNode();
        imageUrlNode.put("type", "image_url");
        ObjectNode imageUrlObj = imageUrlNode.putObject("image_url");
        imageUrlObj.put("url", imageUrl);

        return callVisionApiInternal(imageUrlNode, prompt);
    }

    private String callVisionApi(String dataUrl, String prompt) throws IOException {
        ObjectNode imageUrlNode = objectMapper.createObjectNode();
        imageUrlNode.put("type", "image_url");
        ObjectNode imageUrlObj = imageUrlNode.putObject("image_url");
        imageUrlObj.put("url", dataUrl);

        return callVisionApiInternal(imageUrlNode, prompt);
    }

    private String callVisionApiInternal(ObjectNode imageContent, String prompt) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);

        ArrayNode messages = body.putArray("messages");
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");

        ArrayNode content = userMsg.putArray("content");
        ObjectNode textPart = content.addObject();
        textPart.put("type", "text");
        textPart.put("text", prompt != null ? prompt : "请详细描述这张图片的内容，包括场景、物体、风格、颜色等。");

        content.add(imageContent);

        RequestBody httpBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(httpBody)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        log.info("调用豆包视觉大模型, model={}, prompt长度={}", model, prompt != null ? prompt.length() : 0);

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "unknown";
                throw new IOException("豆包API返回错误 HTTP " + response.code() + ": " + errBody);
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
            return result.toString();
        }
    }

    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
