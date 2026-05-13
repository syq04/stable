package com.nebula.studio.ai.provider;

import lombok.Data;

@Data
public class Text2ImageResult {

    private boolean success;
    private byte[] imageData;
    private String imageUrl;
    private String errorMessage;
    private String providerName;
    private long seed;

    public static Text2ImageResult success(byte[] imageData, String providerName) {
        Text2ImageResult r = new Text2ImageResult();
        r.setSuccess(true);
        r.setImageData(imageData);
        r.setProviderName(providerName);
        return r;
    }

    public static Text2ImageResult success(String imageUrl, String providerName) {
        Text2ImageResult r = new Text2ImageResult();
        r.setSuccess(true);
        r.setImageUrl(imageUrl);
        r.setProviderName(providerName);
        return r;
    }

    public static Text2ImageResult fail(String errorMessage, String providerName) {
        Text2ImageResult r = new Text2ImageResult();
        r.setSuccess(false);
        r.setErrorMessage(errorMessage);
        r.setProviderName(providerName);
        return r;
    }
}
