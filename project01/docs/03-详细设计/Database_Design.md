# 文图互转主题设计系统 - 数据库设计说明书

## 1. 设计概述

本数据库设计基于需求规格说明书和系统架构设计文档，采用关系型数据库MySQL 8.0+作为存储方案，遵循第三范式(3NF)设计原则，确保数据完整性和可维护性。

## 2. ER实体关系图

```mermaid
erDiagram
    USER ||--o{ IMAGE_RECORD : creates
    USER ||--o{ STYLE : designs
    USER ||--o{ TRAINING_TASK : creates
    STYLE ||--o{ IMAGE_RECORD : uses
    TRAINING_TASK ||--|| LORA_MODEL : produces
    
    USER {
        bigint id PK "用户ID"
        varchar username "用户名"
        varchar email "邮箱"
        varchar password_hash "密码哈希"
        varchar role "角色"
        varchar avatar_url "头像URL"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        bigint created_by "创建人ID"
        bigint updated_by "更新人ID"
    }
    
    IMAGE_RECORD {
        bigint id PK "记录ID"
        bigint user_id FK "用户ID"
        bigint style_id FK "风格ID"
        varchar type "类型"
        text input_content "输入内容"
        text output_content "输出内容"
        varchar image_url "图像URL"
        varchar status "状态"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        bigint created_by "创建人ID"
    }
    
    STYLE {
        bigint id PK "风格ID"
        bigint designer_id FK "设计师ID"
        varchar name "风格名称"
        text description "风格描述"
        varchar preview_url "预览图URL"
        text config "配置参数"
        varchar status "状态"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        bigint created_by "创建人ID"
        bigint updated_by "更新人ID"
    }
    
    TRAINING_TASK {
        bigint id PK "任务ID"
        bigint user_id FK "用户ID"
        varchar name "任务名称"
        varchar status "状态"
        text params "训练参数"
        varchar data_path "训练数据路径"
        varchar model_path "模型输出路径"
        float progress "进度"
        text logs "训练日志"
        datetime created_at "创建时间"
        datetime started_at "开始时间"
        datetime completed_at "完成时间"
        bigint created_by "创建人ID"
    }
    
    LORA_MODEL {
        bigint id PK "模型ID"
        bigint task_id FK "训练任务ID"
        varchar name "模型名称"
        varchar file_path "模型文件路径"
        varchar version "版本号"
        datetime created_at "创建时间"
        bigint created_by "创建人ID"
    }
    
    SYSTEM_CONFIG {
        bigint id PK "配置ID"
        varchar config_key "配置键"
        varchar config_value "配置值"
        varchar description "配置说明"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        bigint created_by "创建人ID"
        bigint updated_by "更新人ID"
    }
    
    AUDIT_LOG {
        bigint id PK "日志ID"
        varchar operation_type "操作类型"
        varchar target_type "目标类型"
        bigint target_id "目标ID"
        bigint operator_id "操作人ID"
        text before_data "操作前数据"
        text after_data "操作后数据"
        varchar ip_address "IP地址"
        datetime created_at "操作时间"
    }
```

## 3. DDL定义

### 3.1 用户表 (USER)

```sql
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键自增',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名，唯一标识',
    `email` VARCHAR(128) NOT NULL COMMENT '邮箱地址',
    `password_hash` VARCHAR(256) NOT NULL COMMENT '密码哈希值(BCrypt)',
    `role` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '用户角色：USER-普通用户，DESIGNER-设计师，ADMIN-管理员',
    `avatar_url` VARCHAR(512) COMMENT '头像URL',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT COMMENT '创建人ID',
    `updated_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_role` (`role`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

### 3.2 图像记录表 (IMAGE_RECORD)

```sql
CREATE TABLE `image_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID，主键自增',
    `user_id` BIGINT NOT NULL COMMENT '用户ID，关联user表',
    `style_id` BIGINT COMMENT '风格ID，关联style表',
    `type` VARCHAR(32) NOT NULL COMMENT '类型：TEXT2IMAGE-文生图，IMAGE2TEXT-图生文',
    `input_content` TEXT COMMENT '输入内容（文本描述或图像特征）',
    `output_content` TEXT COMMENT '输出内容（图像URL或文本描述）',
    `image_url` VARCHAR(512) COMMENT '生成的图像URL',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-处理中，SUCCESS-成功，FAILED-失败',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT NOT NULL COMMENT '创建人ID',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_style_id` (`style_id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_image_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_image_record_style` FOREIGN KEY (`style_id`) REFERENCES `style` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图像记录表';
```

### 3.3 风格表 (STYLE)

```sql
CREATE TABLE `style` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '风格ID，主键自增',
    `designer_id` BIGINT NOT NULL COMMENT '设计师ID，关联user表',
    `name` VARCHAR(64) NOT NULL COMMENT '风格名称',
    `description` TEXT COMMENT '风格描述',
    `preview_url` VARCHAR(512) COMMENT '预览图URL',
    `config` TEXT COMMENT '风格配置参数(JSON格式)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-启用，INACTIVE-禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT NOT NULL COMMENT '创建人ID',
    `updated_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_style_name` (`name`),
    KEY `idx_designer_id` (`designer_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_style_designer` FOREIGN KEY (`designer_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风格表';
```

### 3.4 训练任务表 (TRAINING_TASK)

```sql
CREATE TABLE `training_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID，主键自增',
    `user_id` BIGINT NOT NULL COMMENT '用户ID，关联user表',
    `name` VARCHAR(128) NOT NULL COMMENT '任务名称',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-等待中，RUNNING-运行中，COMPLETED-完成，FAILED-失败',
    `params` TEXT COMMENT '训练参数(JSON格式)',
    `data_path` VARCHAR(512) COMMENT '训练数据存储路径',
    `model_path` VARCHAR(512) COMMENT '训练结果模型路径',
    `progress` FLOAT DEFAULT 0 COMMENT '训练进度(0-100)',
    `logs` LONGTEXT COMMENT '训练日志',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `started_at` DATETIME COMMENT '开始时间',
    `completed_at` DATETIME COMMENT '完成时间',
    `created_by` BIGINT NOT NULL COMMENT '创建人ID',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_training_task_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练任务表';
```

### 3.5 LoRA模型表 (LORA_MODEL)

```sql
CREATE TABLE `lora_model` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模型ID，主键自增',
    `task_id` BIGINT NOT NULL COMMENT '训练任务ID，关联training_task表',
    `name` VARCHAR(128) NOT NULL COMMENT '模型名称',
    `file_path` VARCHAR(512) NOT NULL COMMENT '模型文件存储路径',
    `version` VARCHAR(32) DEFAULT '1.0.0' COMMENT '版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT NOT NULL COMMENT '创建人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_name` (`name`),
    KEY `idx_task_id` (`task_id`),
    CONSTRAINT `fk_lora_model_task` FOREIGN KEY (`task_id`) REFERENCES `training_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LoRA模型表';
```

### 3.6 系统配置表 (SYSTEM_CONFIG)

```sql
CREATE TABLE `system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID，主键自增',
    `config_key` VARCHAR(128) NOT NULL COMMENT '配置键，唯一标识',
    `config_value` VARCHAR(1024) COMMENT '配置值',
    `description` VARCHAR(512) COMMENT '配置说明',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT COMMENT '创建人ID',
    `updated_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
```

### 3.7 审计日志表 (AUDIT_LOG)

```sql
CREATE TABLE `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID，主键自增',
    `operation_type` VARCHAR(64) NOT NULL COMMENT '操作类型：CREATE-创建，UPDATE-更新，DELETE-删除，QUERY-查询',
    `target_type` VARCHAR(64) NOT NULL COMMENT '目标类型：USER-用户，STYLE-风格，TASK-训练任务等',
    `target_id` BIGINT COMMENT '目标ID',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `before_data` LONGTEXT COMMENT '操作前数据(JSON格式)',
    `after_data` LONGTEXT COMMENT '操作后数据(JSON格式)',
    `ip_address` VARCHAR(64) COMMENT '操作IP地址',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_target_type` (`target_type`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';
```

## 4. 初始化数据

### 4.1 系统角色初始化

```sql
-- 创建默认管理员用户（密码：admin123，已BCrypt加密）
INSERT INTO `user` (`username`, `email`, `password_hash`, `role`, `created_at`, `updated_at`, `created_by`)
VALUES ('admin', 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', 'ADMIN', NOW(), NOW(), 1);

-- 创建默认设计师用户（密码：designer123，已BCrypt加密）
INSERT INTO `user` (`username`, `email`, `password_hash`, `role`, `created_at`, `updated_at`, `created_by`)
VALUES ('designer', 'designer@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', 'DESIGNER', NOW(), NOW(), 1);
```

### 4.2 系统配置初始化

```sql
INSERT INTO `system_config` (`config_key`, `config_value`, `description`, `created_at`, `updated_at`) VALUES
('app.name', '文图互转主题设计系统', '系统名称', NOW(), NOW()),
('app.version', '1.0.0', '系统版本', NOW(), NOW()),
('ai.text2image.provider', 'comfyui', '默认文生图Provider', NOW(), NOW()),
('ai.image2text.provider', 'mock', '默认图生文Provider', NOW(), NOW()),
('ai.comfyui.enabled', 'true', 'ComfyUI是否启用', NOW(), NOW()),
('ai.comfyui.api-url', 'http://127.0.0.1:8188', 'ComfyUI API地址', NOW(), NOW()),
('storage.minio.url', 'http://localhost:9000', 'MinIO存储地址', NOW(), NOW()),
('storage.minio.bucket', 'ai-content', 'MinIO存储桶名称', NOW(), NOW()),
('security.jwt.secret', 'your-secret-key-here-change-in-production', 'JWT密钥', NOW(), NOW()),
('security.jwt.expire-hours', '24', 'JWT过期时间(小时)', NOW(), NOW()),
('training.default.epochs', '10', '默认训练轮数', NOW(), NOW()),
('training.default.batch-size', '8', '默认批次大小', NOW(), NOW());
```

### 4.3 默认风格初始化

```sql
INSERT INTO `style` (`designer_id`, `name`, `description`, `config`, `status`, `created_at`, `updated_at`, `created_by`) VALUES
(2, '写实风格', '真实感强的图像风格，适合产品展示、人物肖像等场景', '{"prompt": "realistic, photorealistic, high detail", "negative_prompt": "cartoon, anime, drawing"}', 'ACTIVE', NOW(), NOW(), 2),
(2, '插画风格', '艺术插画风格，色彩丰富，适合宣传海报、社交媒体配图', '{"prompt": "illustration, colorful, artistic, painting style", "negative_prompt": "photorealistic, realistic"}', 'ACTIVE', NOW(), NOW(), 2),
(2, '极简风格', '简约现代风格，线条简洁，适合UI设计、品牌素材', '{"prompt": "minimalist, clean, modern design, simple", "negative_prompt": "complex, cluttered"}', 'ACTIVE', NOW(), NOW(), 2),
(2, '复古风格', '怀旧复古风格，带有年代感，适合复古主题内容', '{"prompt": "vintage, retro, nostalgic, classic style", "negative_prompt": "modern, futuristic"}', 'ACTIVE', NOW(), NOW(), 2);
```

## 5. 索引设计

### 5.1 主键索引

| 表名 | 主键字段 | 说明 |
|-----|---------|-----|
| user | id | 用户ID |
| image_record | id | 记录ID |
| style | id | 风格ID |
| training_task | id | 任务ID |
| lora_model | id | 模型ID |
| system_config | id | 配置ID |
| audit_log | id | 日志ID |

### 5.2 唯一索引

| 表名 | 字段 | 说明 |
|-----|-----|-----|
| user | username | 用户名唯一 |
| user | email | 邮箱唯一 |
| style | name | 风格名称唯一 |
| system_config | config_key | 配置键唯一 |
| lora_model | name | 模型名称唯一 |

### 5.3 普通索引

| 表名 | 字段 | 说明 |
|-----|-----|-----|
| user | role | 按角色查询 |
| user | created_at | 按创建时间排序 |
| image_record | user_id | 按用户查询记录 |
| image_record | style_id | 按风格查询记录 |
| image_record | type | 按类型查询 |
| image_record | status | 按状态查询 |
| style | designer_id | 按设计师查询 |
| style | status | 按状态查询 |
| training_task | user_id | 按用户查询任务 |
| training_task | status | 按状态查询 |
| audit_log | operation_type | 按操作类型查询 |
| audit_log | target_type | 按目标类型查询 |
| audit_log | operator_id | 按操作人查询 |

## 6. 数据字典

### 6.1 用户角色枚举

| 值 | 含义 | 说明 |
|-----|-----|-----|
| USER | 普通用户 | 基础权限，可使用文生图、图生文功能 |
| DESIGNER | 设计师 | 继承普通用户权限，可管理风格和训练LoRA模型 |
| ADMIN | 管理员 | 最高权限，可管理用户、系统配置、日志等 |

### 6.2 记录类型枚举

| 值 | 含义 | 说明 |
|-----|-----|-----|
| TEXT2IMAGE | 文生图 | 文本描述生成图像 |
| IMAGE2TEXT | 图生文 | 图像分析生成文本描述 |

### 6.3 状态枚举

| 值 | 含义 | 适用表 |
|-----|-----|-----|
| PENDING | 等待中/处理中 | image_record, training_task |
| RUNNING | 运行中 | training_task |
| SUCCESS | 成功 | image_record |
| COMPLETED | 完成 | training_task |
| FAILED | 失败 | image_record, training_task |
| ACTIVE | 启用 | style |
| INACTIVE | 禁用 | style |

### 6.4 操作类型枚举

| 值 | 含义 | 说明 |
|-----|-----|-----|
| CREATE | 创建 | 新增记录 |
| UPDATE | 更新 | 修改记录 |
| DELETE | 删除 | 删除记录 |
| QUERY | 查询 | 查询记录 |

---

**文档版本**: v1.0  
**创建日期**: 2026-05-10  
**适用数据库**: MySQL 8.0+
