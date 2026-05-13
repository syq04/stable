package com.nebula.studio.ai.provider;

public interface Image2TextProvider {

    String getProviderName();

    boolean isAvailable();

    Image2TextResult analyze(byte[] imageBytes, String mimeType, String prompt);

    Image2TextResult analyzeUrl(String imageUrl, String prompt);
}
