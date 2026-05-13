package com.nebula.studio.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("training_task")
public class TrainingTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String status;

    private String params;

    private String dataPath;

    private String modelPath;

    private Float progress;

    private String logs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private Long createdBy;
}
