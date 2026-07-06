CREATE DATABASE IF NOT EXISTS `nebula_studio` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `nebula_studio`;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `email` VARCHAR(128) NOT NULL COMMENT '邮箱地址',
    `password_hash` VARCHAR(256) NOT NULL COMMENT '密码哈希值',
    `role` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '角色: USER/DESIGNER/ADMIN',
    `avatar_url` VARCHAR(512) COMMENT '头像URL',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT COMMENT '创建人ID',
    `updated_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `image_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `style_id` BIGINT COMMENT '风格ID',
    `type` VARCHAR(32) NOT NULL COMMENT '类型: TEXT2IMAGE/IMAGE2TEXT',
    `input_content` TEXT COMMENT '输入内容',
    `output_content` TEXT COMMENT '输出内容',
    `image_url` VARCHAR(512) COMMENT '图像URL',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/SUCCESS/FAILED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT NOT NULL COMMENT '创建人ID',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图像记录表';

CREATE TABLE IF NOT EXISTS `style` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '风格ID',
    `designer_id` BIGINT NOT NULL COMMENT '设计师ID',
    `name` VARCHAR(64) NOT NULL COMMENT '风格名称',
    `description` TEXT COMMENT '风格描述',
    `preview_url` VARCHAR(512) COMMENT '预览图URL',
    `config` TEXT COMMENT '风格配置参数(JSON)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT NOT NULL COMMENT '创建人ID',
    `updated_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_style_name` (`name`),
    KEY `idx_designer_id` (`designer_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风格表';

CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_key` VARCHAR(128) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(1024) COMMENT '配置值',
    `description` VARCHAR(512) COMMENT '配置说明',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT COMMENT '创建人ID',
    `updated_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `operation_type` VARCHAR(64) NOT NULL COMMENT '操作类型',
    `target_type` VARCHAR(64) NOT NULL COMMENT '目标类型',
    `target_id` BIGINT COMMENT '目标ID',
    `operator_id` BIGINT COMMENT '操作人ID',
    `before_data` LONGTEXT COMMENT '操作前数据(JSON)',
    `after_data` LONGTEXT COMMENT '操作后数据(JSON)',
    `ip_address` VARCHAR(64) COMMENT 'IP地址',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_target_type` (`target_type`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- Seed data is initialized by DataInitializer.java at application startup.
-- See com.nebula.studio.config.DataInitializer for default users, styles, and configs.
