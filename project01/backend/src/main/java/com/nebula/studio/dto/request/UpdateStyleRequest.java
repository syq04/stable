package com.nebula.studio.dto.request;

import lombok.Data;

@Data
public class UpdateStyleRequest {
    private String name;
    private String description;
    private String previewUrl;
    private String config;
    private String status;
}
