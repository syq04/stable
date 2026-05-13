package com.nebula.studio.ai.provider;

import com.nebula.studio.ai.DoubaoVisionClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DoubaoImage2TextProvider implements Image2TextProvider {

    private final DoubaoVisionClient doubaoClient;

    @Value("${ai.doubao.enabled:false}")
    private boolean doubaoEnabled;

    @Override
    public String getProviderName() {
        return "doubao";
    }

    @Override
    public boolean isAvailable() {
        return doubaoEnabled && doubaoClient.isAvailable();
    }

    @Override
    public Image2TextResult analyze(byte[] imageBytes, String mimeType, String prompt) {
        try {
            String description = doubaoClient.analyzeImage(imageBytes, mimeType, prompt);
            return Image2TextResult.success(description, getProviderName());
        } catch (Exception e) {
            log.error("豆包视觉大模型调用失败: {}", e.getMessage());
            return Image2TextResult.fail("分析失败: " + e.getMessage(), getProviderName());
        }
    }

    @Override
    public Image2TextResult analyzeUrl(String imageUrl, String prompt) {
        try {
            String description = doubaoClient.analyzeImageUrl(imageUrl, prompt);
            return Image2TextResult.success(description, getProviderName());
        } catch (Exception e) {
            log.error("豆包视觉大模型(URL)调用失败: {}", e.getMessage());
            return Image2TextResult.fail("分析失败: " + e.getMessage(), getProviderName());
        }
    }
}
