package com.nebula.studio.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lora_model")
public class LoraModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String name;

    private String filePath;

    private String version;

    private String status; // ACTIVE, INACTIVE

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private Long createdBy;
}
