package com.nebula.studio.dto.request;

import lombok.Data;

@Data
public class CreateStyleRequest {
    private String name;
    private String description;
    private String previewUrl;
    private String config;
}
