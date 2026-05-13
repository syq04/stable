package com.nebula.studio.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockImage2TextProvider implements Image2TextProvider {

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Image2TextResult analyze(byte[] imageBytes, String mimeType, String prompt) {
        return generateMockResult("uploaded-image");
    }

    @Override
    public Image2TextResult analyzeUrl(String imageUrl, String prompt) {
        return generateMockResult(imageUrl);
    }

    private Image2TextResult generateMockResult(String source) {
        String desc = "【模拟模式】AI视觉服务未配置或不可用，当前为模拟分析结果。\n\n" +
                "图片来源: " + source + "\n\n" +
                "提示：要启用真实的图片分析功能，请配置以下任一AI视觉服务：\n" +
                "1. OpenRouter API（免费）- 在系统设置中配置 OpenRouter API Key\n" +
                "2. Google Gemini API（免费）- 在系统设置中配置 Gemini API Key\n" +
                "3. 豆包视觉 API - 在系统设置中配置豆包 API Key\n" +
                "4. HuggingFace API - 在系统设置中配置 HuggingFace API Key";
        return Image2TextResult.success(desc, getProviderName());
    }
}
