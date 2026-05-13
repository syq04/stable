package com.nebula.studio.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nebula.studio.service.DynamicConfigService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ComfyUIClient implements AutoCloseable {

    private static final String DEFAULT_WORKFLOW = "workflow/comfyui_default.json";

    private static final String NODE_POSITIVE = "26";
    private static final String NODE_NEGATIVE = "22";
    private static final String NODE_SAMPLER = "24";
    private static final String NODE_LATENT = "25";

    private final String defaultApiUrl;
    private final int timeoutSeconds;
    private final int pollIntervalMs;
    private final String workflowPath;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DynamicConfigService dynamicConfigService;

    public ComfyUIClient(String defaultApiUrl, int timeoutSeconds, int pollIntervalMs,
                         String workflowPath, DynamicConfigService dynamicConfigService) {
        this.defaultApiUrl = defaultApiUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.pollIntervalMs = pollIntervalMs;
        this.workflowPath = workflowPath != null ? workflowPath : DEFAULT_WORKFLOW;
        this.dynamicConfigService = dynamicConfigService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String getEffectiveApiUrl() {
        return dynamicConfigService.getConfigValue("ai.comfyui.api-url", defaultApiUrl);
    }

    public boolean isAvailable() {
        String apiUrl = getEffectiveApiUrl();
        try {
            Request req = new Request.Builder()
                    .url(apiUrl + "/object_info")
                    .get()
                    .build();
            try (Response resp = httpClient.newCall(req).execute()) {
                return resp.isSuccessful();
            }
        } catch (Exception e) {
            log.warn("ComfyUI API [{}] 不可用: {}", apiUrl, e.getMessage());
            return false;
        }
    }

    public byte[] generate(String positivePrompt, String negativePrompt,
                           int width, int height, int steps, double cfg, Long seed,
                           String samplerName) throws Exception {
        String apiUrl = getEffectiveApiUrl();
        log.info("ComfyUI API URL: {}", apiUrl);

        JsonNode workflow = loadWorkflow();
        injectParams(workflow, positivePrompt, negativePrompt, width, height, steps, cfg, seed, samplerName);

        String promptId = queuePrompt(workflow, apiUrl);
        log.info("ComfyUI prompt queued, id={}", promptId);

        JsonNode history = waitForCompletion(promptId, apiUrl);
        byte[] imageData = downloadFirstImage(history, apiUrl);
        if (imageData == null) {
            throw new IOException("ComfyUI 未返回图片数据");
        }
        return imageData;
    }

    JsonNode loadWorkflow() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(workflowPath);
        if (is == null) {
            throw new IOException("Workflow 文件未找到: " + workflowPath);
        }
        return objectMapper.readTree(is);
    }

    void injectParams(JsonNode workflow, String positivePrompt, String negativePrompt,
                      int width, int height, int steps, double cfg, Long seed,
                      String samplerName) {
        setNodeText(workflow, NODE_POSITIVE, positivePrompt);
        setNodeText(workflow, NODE_NEGATIVE, negativePrompt);

        ObjectNode sampler = (ObjectNode) workflow.get(NODE_SAMPLER);
        if (sampler != null) {
            ObjectNode inputs = (ObjectNode) sampler.get("inputs");
            if (inputs != null) {
                if (seed != null) {
                    inputs.put("seed", seed);
                }
                inputs.put("steps", steps);
                inputs.put("cfg", cfg);
                if (samplerName != null && !samplerName.isBlank()) {
                    inputs.put("sampler_name", samplerName);
                }
            }
        }

        ObjectNode latent = (ObjectNode) workflow.get(NODE_LATENT);
        if (latent != null) {
            ObjectNode inputs = (ObjectNode) latent.get("inputs");
            if (inputs != null) {
                inputs.put("width", width);
                inputs.put("height", height);
            }
        }
    }

    private void setNodeText(JsonNode workflow, String nodeId, String text) {
        ObjectNode node = (ObjectNode) workflow.get(nodeId);
        if (node != null) {
            ObjectNode inputs = (ObjectNode) node.get("inputs");
            if (inputs != null) {
                inputs.put("text", text);
            }
        }
    }

    String queuePrompt(JsonNode workflow, String apiUrl) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("prompt", workflow);

        Request httpReq = new Request.Builder()
                .url(apiUrl + "/prompt")
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();

        try (Response resp = httpClient.newCall(httpReq).execute()) {
            if (!resp.isSuccessful()) {
                String err = resp.body() != null ? resp.body().string() : "unknown";
                throw new IOException("ComfyUI queue 失败 HTTP " + resp.code() + ": " + err);
            }
            JsonNode json = objectMapper.readTree(resp.body().string());
            return json.get("prompt_id").asText();
        }
    }

    JsonNode waitForCompletion(String promptId, String apiUrl) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(pollIntervalMs);
            Request req = new Request.Builder()
                    .url(apiUrl + "/history/" + promptId)
                    .get()
                    .build();
            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    continue;
                }
                JsonNode history = objectMapper.readTree(resp.body().string());
                if (history.has(promptId)) {
                    return history.get(promptId);
                }
            }
        }
        throw new IOException("ComfyUI 任务超时 (" + timeoutSeconds + "s): " + promptId);
    }

    byte[] downloadFirstImage(JsonNode history, String apiUrl) throws IOException {
        JsonNode outputs = history.get("outputs");
        if (outputs == null) return null;

        Iterator<String> nodeIds = outputs.fieldNames();
        while (nodeIds.hasNext()) {
            String nodeId = nodeIds.next();
            JsonNode nodeOutput = outputs.get(nodeId);
            JsonNode images = nodeOutput.get("images");
            if (images != null && images.isArray() && !images.isEmpty()) {
                for (JsonNode img : images) {
                    String filename = img.get("filename").asText();
                    String type = img.has("type") ? img.get("type").asText() : "output";
                    String subfolder = img.has("subfolder") ? img.get("subfolder").asText() : "";
                    return downloadImage(filename, type, subfolder, apiUrl);
                }
            }
        }
        return null;
    }

    private byte[] downloadImage(String filename, String type, String subfolder, String apiUrl) throws IOException {
        HttpUrl url = HttpUrl.parse(apiUrl + "/view").newBuilder()
                .addQueryParameter("filename", filename)
                .addQueryParameter("type", type)
                .addQueryParameter("subfolder", subfolder)
                .build();

        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("下载图片失败 HTTP " + resp.code() + ": " + filename);
            }
            return resp.body().bytes();
        }
    }

    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
