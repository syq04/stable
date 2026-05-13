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
public class ZhipuText2ImageProvider implements Text2ImageProvider {

    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/images/generations";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DynamicConfigService dynamicConfigService;

    @Value("${ai.zhipu.text2image-model:cogview-3-flash}")
    private String text2ImageModel;

    @Value("${ai.zhipu.api-key:}")
    private String defaultApiKey;

    public ZhipuText2ImageProvider(DynamicConfigService dynamicConfigService) {
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
        dynamicConfigService.registerDefault("ai.zhipu.api-key", defaultApiKey);
    }

    @Override
    public String getProviderName() {
        return "zhipu";
    }

    @Override
    public boolean isAvailable() {
        String apiKey = dynamicConfigService.getConfigValue("ai.zhipu.api-key");
        return apiKey != null && !apiKey.isBlank();
    }

    private static final String[] SUPPORTED_SIZES = {
            "1024x1024", "768x1344", "864x1152", "1344x768", "1152x864", "1440x720", "720x1440"
    };

    @Override
    public Text2ImageResult generate(Text2ImageRequest request, String builtPrompt) {
        String apiKey = dynamicConfigService.getConfigValue("ai.zhipu.api-key");
        if (apiKey == null || apiKey.isBlank()) {
            return Text2ImageResult.fail("智谱AI API Key 未配置", getProviderName());
        }
        try {
            int width = request.getWidth() != null ? request.getWidth() : 1024;
            int height = request.getHeight() != null ? request.getHeight() : 1024;
            int seed = request.getSeed() != null ? request.getSeed().intValue() : (int) (System.currentTimeMillis() % 2147483647);

            String size = findClosestSize(width, height);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", text2ImageModel);
            body.put("prompt", builtPrompt);
            body.put("size", size);
            body.put("seed", seed);

            RequestBody httpBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
            Request httpRequest = new Request.Builder()
                    .url(API_URL)
                    .post(httpBody)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            log.info("调用智谱AI文生图, model={}, prompt={}, size={}x{}", text2ImageModel, builtPrompt, width, height);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "unknown";
                    return Text2ImageResult.fail("智谱AI API 返回错误 HTTP " + response.code() + ": " + errBody, getProviderName());
                }
                String respStr = response.body().string();
                JsonNode root = objectMapper.readTree(respStr);

                if (root.has("data") && root.get("data").isArray() && root.get("data").size() > 0) {
                    String imageUrl = root.get("data").get(0).get("url").asText();
                    Text2ImageResult result = Text2ImageResult.success(imageUrl, getProviderName());
                    result.setSeed(seed);
                    return result;
                }
                return Text2ImageResult.fail("智谱AI API 未返回有效图片", getProviderName());
            }
        } catch (IOException e) {
            log.error("智谱AI文生图调用失败: {}", e.getMessage());
            return Text2ImageResult.fail("调用失败: " + e.getMessage(), getProviderName());
        }
    }

    private String findClosestSize(int width, int height) {
        String target = width + "x" + height;
        for (String s : SUPPORTED_SIZES) {
            if (s.equals(target)) return s;
        }
        double targetRatio = (double) width / height;
        String best = SUPPORTED_SIZES[0];
        double minDiff = Double.MAX_VALUE;
        for (String s : SUPPORTED_SIZES) {
            String[] parts = s.split("x");
            double ratio = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            double diff = Math.abs(ratio - targetRatio);
            if (diff < minDiff) {
                minDiff = diff;
                best = s;
            }
        }
        log.info("智谱AI尺寸适配: {}x{} -> {}", width, height, best);
        return best;
    }
}
