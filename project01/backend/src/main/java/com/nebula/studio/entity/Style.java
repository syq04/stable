package com.nebula.studio.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("style")
public class Style {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long designerId;

    private String name;

    private String description;

    private String previewUrl;

    private String config;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;
}
