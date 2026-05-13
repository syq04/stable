package com.nebula.studio.ai;

import lombok.Data;

@Data
public class SdTxt2ImgRequest {
    private String prompt;
    private String negativePrompt;
    private int width = 512;
    private int height = 512;
    private int steps = 20;
    private double cfgScale = 7.5;
    private Long seed;
    private String samplerName;
}
