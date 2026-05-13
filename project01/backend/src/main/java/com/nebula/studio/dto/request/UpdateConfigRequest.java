package com.nebula.studio.dto.request;

import lombok.Data;

@Data
public class UpdateConfigRequest {
    private String configValue;
    private String description;
}
