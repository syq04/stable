package com.nebula.studio.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class StableDiffusionClient implements AutoCloseable {

    private final String apiUrl;
    private final int timeoutSeconds;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public StableDiffusionClient(String apiUrl, int timeoutSeconds) {
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isAvailable() {
        Request request = new Request.Builder()
                .url(apiUrl + "/sdapi/v1/sd-models")
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (Exception e) {
            log.warn("Stable Diffusion API 不可用: {}", e.getMessage());
            return false;
        }
    }

    public SdImageResponse txt2img(SdTxt2ImgRequest req) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("prompt", req.getPrompt());
        if (req.getNegativePrompt() != null) {
            body.put("negative_prompt", req.getNegativePrompt());
        }
        body.put("width", req.getWidth());
        body.put("height", req.getHeight());
        body.put("steps", req.getSteps());
        body.put("cfg_scale", req.getCfgScale());
        if (req.getSeed() != null && req.getSeed() > 0) {
            body.put("seed", req.getSeed());
        } else {
            body.put("seed", -1);
        }
        body.put("sampler_name", req.getSamplerName() != null ? req.getSamplerName() : "Euler a");
        body.put("n_iter", 1);
        body.put("batch_size", 1);
        body.put("send_images", true);
        body.put("save_images", false);

        RequestBody httpBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(apiUrl + "/sdapi/v1/txt2img")
                .post(httpBody)
                .build();

        log.info("调用 SD txt2img, prompt={}, size={}x{}, steps={}", req.getPrompt(), req.getWidth(), req.getHeight(), req.getSteps());

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "unknown";
                throw new IOException("SD API 返回错误 HTTP " + response.code() + ": " + errBody);
            }
            String respStr = response.body().string();
            JsonNode root = objectMapper.readTree(respStr);

            SdImageResponse result = new SdImageResponse();
            if (root.has("images") && root.get("images").isArray() && !root.get("images").isEmpty()) {
                result.setBase64Image(root.get("images").get(0).asText());
            }
            if (root.has("parameters")) {
                JsonNode params = root.get("parameters");
                if (params.has("seed")) {
                    result.setSeed(params.get("seed").asLong());
                }
            }
            if (root.has("info")) {
                try {
                    JsonNode info = objectMapper.readTree(root.get("info").asText());
                    if (info.has("seed") && result.getSeed() == 0) {
                        result.setSeed(info.get("seed").asLong());
                    }
                } catch (Exception ignored) {}
            }
            return result;
        }
    }

    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
