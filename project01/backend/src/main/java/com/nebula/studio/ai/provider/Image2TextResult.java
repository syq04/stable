package com.nebula.studio.ai.provider;

import lombok.Data;

@Data
public class Image2TextResult {

    private boolean success;
    private String description;
    private String errorMessage;
    private String providerName;
    private Double accuracy;
    private String accuracyDetail;

    public static Image2TextResult success(String description, String providerName) {
        Image2TextResult r = new Image2TextResult();
        r.setSuccess(true);
        r.setDescription(description);
        r.setProviderName(providerName);
        return r;
    }

    public static Image2TextResult fail(String errorMessage, String providerName) {
        Image2TextResult r = new Image2TextResult();
        r.setSuccess(false);
        r.setErrorMessage(errorMessage);
        r.setProviderName(providerName);
        return r;
    }
}
