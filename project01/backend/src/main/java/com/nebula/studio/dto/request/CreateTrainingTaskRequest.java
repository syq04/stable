package com.nebula.studio.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTrainingTaskRequest {

    @NotBlank(message = "任务名称不能为空")
    private String name;

    private String params;

    private String dataPath;
}
