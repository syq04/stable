package com.nebula.studio.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Text2ImageRequest {

    @NotBlank(message = "提示词不能为空")
    private String prompt;

    private String negativePrompt;

    private Long styleId;

    private Integer width = 512;

    private Integer height = 512;

    private Integer steps = 20;

    private Double cfgScale = 7.5;

    private Long seed;

    private String samplerName;

    /**
     * LoRA 模型ID（关联 lora_model 表）
     */
    private Long loraModelId;

    /**
     * LoRA 权重（0.0 ~ 1.0）
     */
    private Double loraWeight = 0.7;
}
