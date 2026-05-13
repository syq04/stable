package com.nebula.studio.ai.provider;

import com.nebula.studio.dto.request.Text2ImageRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PollinationsText2ImageProvider implements Text2ImageProvider {

    private static final String BASE_URL = "https://image.pollinations.ai/prompt/";
    private final OkHttpClient httpClient;
    private volatile Boolean cachedAvailable;
    private volatile long lastCheckTime;
    private static final long CHECK_INTERVAL_MS = 60_000;

    public PollinationsText2ImageProvider() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    @Override
    public String getProviderName() {
        return "pollinations";
    }

    @Override
    public boolean isAvailable() {
        long now = System.currentTimeMillis();
        if (cachedAvailable != null && (now - lastCheckTime) < CHECK_INTERVAL_MS) {
            return cachedAvailable;
        }
        Request request = new Request.Builder()
                .url("https://image.pollinations.ai/prompt/test?width=64&height=64&nologo=true&seed=1")
                .head()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            cachedAvailable = response.isSuccessful();
            lastCheckTime = now;
            return cachedAvailable;
        } catch (Exception e) {
            log.warn("Pollinations.ai 不可用: {}", e.getMessage());
            cachedAvailable = false;
            lastCheckTime = now;
            return false;
        }
    }

    @Override
    public Text2ImageResult generate(Text2ImageRequest request, String builtPrompt) {
        try {
            String encodedPrompt = URLEncoder.encode(builtPrompt, StandardCharsets.UTF_8);
            int width = request.getWidth() != null ? request.getWidth() : 1024;
            int height = request.getHeight() != null ? request.getHeight() : 1024;
            int seed = request.getSeed() != null ? request.getSeed().intValue() : (int) (System.currentTimeMillis() % 2147483647);

            HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + encodedPrompt).newBuilder()
                    .addQueryParameter("width", String.valueOf(width))
                    .addQueryParameter("height", String.valueOf(height))
                    .addQueryParameter("seed", String.valueOf(seed))
                    .addQueryParameter("nologo", "true")
                    .addQueryParameter("enhance", "true");

            Request httpRequest = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .build();

            log.info("调用 Pollinations.ai 文生图, prompt={}, size={}x{}", builtPrompt, width, height);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "unknown";
                    return Text2ImageResult.fail("Pollinations API 返回错误 HTTP " + response.code() + ": " + errBody, getProviderName());
                }
                if (response.body() == null) {
                    return Text2ImageResult.fail("Pollinations API 未返回数据", getProviderName());
                }
                byte[] imageData = response.body().bytes();
                if (imageData.length < 100) {
                    return Text2ImageResult.fail("Pollinations API 返回数据过小，可能生成失败", getProviderName());
                }
                Text2ImageResult result = Text2ImageResult.success(imageData, getProviderName());
                result.setSeed(seed);
                return result;
            }
        } catch (IOException e) {
            log.error("Pollinations.ai 调用失败: {}", e.getMessage());
            return Text2ImageResult.fail("调用失败: " + e.getMessage(), getProviderName());
        }
    }
}
