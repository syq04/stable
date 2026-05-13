package com.nebula.studio.ai;

import lombok.Data;

@Data
public class SdImageResponse {
    private String base64Image;
    private long seed;
}
