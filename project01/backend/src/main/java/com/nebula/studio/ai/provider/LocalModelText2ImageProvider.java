package com.nebula.studio.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.studio.dto.request.Text2ImageRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LocalModelText2ImageProvider implements Text2ImageProvider {

    private final ObjectMapper objectMapper;

    @Value("${ai.local-model.api-url:http://127.0.0.1:5000}")
    private String apiUrl;

    @Value("${ai.local-model.enabled:true}")
    private boolean enabled;

    private volatile Boolean cachedAvailable;
    private volatile long lastCheckTime;
    private static final long CHECK_INTERVAL_MS = 30_000;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public LocalModelText2ImageProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return "local-model";
    }

    @Override
    public boolean isAvailable() {
        if (!enabled) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (cachedAvailable != null && (now - lastCheckTime) < CHECK_INTERVAL_MS) {
            return cachedAvailable;
        }
        try {
            Request request = new Request.Builder()
                    .url(apiUrl + "/health")
                    .get()
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                cachedAvailable = response.isSuccessful();
                lastCheckTime = now;
                return cachedAvailable;
            }
        } catch (Exception e) {
            log.warn("Local model service unavailable ({}): {}", apiUrl, e.getMessage());
            cachedAvailable = false;
            lastCheckTime = now;
            return false;
        }
    }

    @Override
    public Text2ImageResult generate(Text2ImageRequest request, String builtPrompt) {
        try {
            String checkpointName = request.getCheckpointName();

            StringBuilder prompt = new StringBuilder(builtPrompt);

            LocalModelRequest req = new LocalModelRequest();
            req.prompt = prompt.toString();
            req.negativePrompt = request.getNegativePrompt() != null ? request.getNegativePrompt() : "";
            req.width = request.getWidth() != null ? request.getWidth() : 512;
            req.height = request.getHeight() != null ? request.getHeight() : 512;
            req.steps = request.getSteps() != null ? request.getSteps() : 20;
            req.cfgScale = request.getCfgScale() != null ? request.getCfgScale() : 7.5;
            req.seed = request.getSeed();
            req.samplerName = request.getSamplerName();
            req.checkpointName = checkpointName;
            req.taskId = request.getTaskId();

            String jsonBody = objectMapper.writeValueAsString(req);
            log.info("Calling local model service: {}, prompt: {}", apiUrl, builtPrompt.substring(0, Math.min(100, builtPrompt.length())));

            Request httpRequest = new Request.Builder()
                    .url(apiUrl + "/generate")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "unknown";
                    return Text2ImageResult.fail("Local model error HTTP " + response.code() + ": " + errBody, getProviderName());
                }

                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null) {
                    return Text2ImageResult.fail("Local model service returned empty response", getProviderName());
                }

                JsonNode root = objectMapper.readTree(responseBody);
                boolean success = root.path("success").asBoolean(false);
                if (!success) {
                    String errMsg = root.path("error_message").asText("Unknown error");
                    return Text2ImageResult.fail(errMsg, getProviderName());
                }

                String imageBase64 = root.path("image_base64").asText();
                if (imageBase64 == null || imageBase64.isEmpty()) {
                    return Text2ImageResult.fail("Local model returned empty image data", getProviderName());
                }

                byte[] imageData = java.util.Base64.getDecoder().decode(imageBase64);
                long seed = root.path("seed").asLong();

                Text2ImageResult result = Text2ImageResult.success(imageData, getProviderName());
                result.setSeed(seed);
                return result;
            }
        } catch (IOException e) {
            log.error("Local model call failed: {}", e.getMessage());
            return Text2ImageResult.fail("Local model call failed: " + e.getMessage(), getProviderName());
        }
    }

    private static class LocalModelRequest {
        public String prompt;
        public String negativePrompt;
        public Integer width;
        public Integer height;
        public Integer steps;
        public Double cfgScale;
        public Long seed;
        public String samplerName;
        public String checkpointName;
        public String taskId;
    }

    public Map<String, Object> getProgress(String taskId) {
        try {
            Request request = new Request.Builder()
                    .url(apiUrl + "/progress/" + taskId)
                    .get()
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return objectMapper.readValue(response.body().string(), Map.class);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get progress for {}: {}", taskId, e.getMessage());
        }
        return Map.of("step", 0, "totalSteps", 30, "elapsed", 0.0, "its", 0.0, "finished", false);
    }
}
