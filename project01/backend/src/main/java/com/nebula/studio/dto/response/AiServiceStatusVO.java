package com.nebula.studio.dto.response;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AiServiceStatusVO {

    private Map<String, ServiceInfo> text2ImageProviders = new LinkedHashMap<>();
    private Map<String, ServiceInfo> image2TextProviders = new LinkedHashMap<>();
    private String activeText2ImageProvider;
    private String activeImage2TextProvider;

    @Data
    public static class ServiceInfo {
        private boolean enabled;
        private boolean available;
        private boolean active;
        private String mode;
        private String message;
    }

    public void addText2ImageProvider(String name, ServiceInfo info) {
        text2ImageProviders.put(name, info);
    }

    public void addImage2TextProvider(String name, ServiceInfo info) {
        image2TextProviders.put(name, info);
    }
}
