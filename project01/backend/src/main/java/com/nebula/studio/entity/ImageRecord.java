package com.nebula.studio.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("image_record")
public class ImageRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long styleId;

    private String type;

    private String inputContent;

    private String outputContent;

    private String imageUrl;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long createdBy;
}
