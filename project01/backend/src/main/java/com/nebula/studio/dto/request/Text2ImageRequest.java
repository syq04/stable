package com.nebula.studio.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Text2ImageRequest {

    @NotBlank(message = "提示词不能为空")
    private String prompt;

    private String negativePrompt;

    private Long styleId;

    @Min(value = 64, message = "宽度不能小于64")
    @Max(value = 2048, message = "宽度不能超过2048")
    private Integer width = 512;

    @Min(value = 64, message = "高度不能小于64")
    @Max(value = 2048, message = "高度不能超过2048")
    private Integer height = 512;

    @Min(value = 1, message = "采样步数不能小于1")
    @Max(value = 150, message = "采样步数不能超过150")
    private Integer steps = 20;

    private Double cfgScale = 7.5;

    private Long seed;

    private String samplerName;

    /**
     * 本地模型名称（对应 sd-models 目录下的 safetensors 文件名，不含扩展名）
     */
    private String checkpointName;

    private String taskId;
}
