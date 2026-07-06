package com.nebula.studio.config;

import com.nebula.studio.entity.Style;
import com.nebula.studio.entity.SystemConfig;
import com.nebula.studio.entity.User;
import com.nebula.studio.mapper.StyleMapper;
import com.nebula.studio.mapper.SystemConfigMapper;
import com.nebula.studio.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final StyleMapper styleMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdminUser();
        initDesignerUser();
        initNormalUser();
        initSystemConfigs();
        initStyles();
        log.info("Data initialization completed");
    }

    private void initAdminUser() {
        if (userMapper.selectByEmail("admin@nebula.com") == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@nebula.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setCreatedBy(0L);
            userMapper.insert(admin);
            log.info("Admin user created: admin@nebula.com");
        }
    }

    private void initDesignerUser() {
        if (userMapper.selectByEmail("designer@nebula.com") == null) {
            User designer = new User();
            designer.setUsername("designer");
            designer.setEmail("designer@nebula.com");
            designer.setPasswordHash(passwordEncoder.encode("designer123"));
            designer.setRole("DESIGNER");
            designer.setCreatedBy(0L);
            userMapper.insert(designer);
            log.info("Designer user created: designer@nebula.com");
        }
    }

    private void initNormalUser() {
        if (userMapper.selectByEmail("user@nebula.com") == null) {
            User user = new User();
            user.setUsername("user");
            user.setEmail("user@nebula.com");
            user.setPasswordHash(passwordEncoder.encode("user123"));
            user.setRole("USER");
            user.setCreatedBy(0L);
            userMapper.insert(user);
            log.info("Normal user created: user@nebula.com");
        }
    }

    private void initSystemConfigs() {
        List<String> keys = List.of("app.name", "app.version",
                "ai.local-model.api-url", "ai.local-model.model-dir",
                "ai.qwen.api-key", "ai.qwen.api-url", "ai.qwen.model",
                "storage.minio.url", "storage.minio.bucket",
                "security.jwt.expire-hours", "training.default.epochs", "training.default.batch-size");

        List<String[]> configs = List.of(
                new String[]{"app.name", "文图互转主题设计系统", "系统名称"},
                new String[]{"app.version", "1.0.0", "系统版本"},
                new String[]{"ai.local-model.api-url", "http://127.0.0.1:5000", "本地模型推理服务地址"},
                new String[]{"ai.local-model.model-dir", "../sd-models", "safetensors 模型存放目录"},
                new String[]{"ai.qwen.api-key", "", "千问 API Key"},
                new String[]{"ai.qwen.api-url", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "千问 API 地址"},
                new String[]{"ai.qwen.model", "qwen3.5-omni-plus", "千问 模型名称"},
                new String[]{"storage.minio.url", "http://localhost:9000", "MinIO存储地址"},
                new String[]{"storage.minio.bucket", "ai-content", "MinIO存储桶名称"},
                new String[]{"security.jwt.expire-hours", "24", "JWT过期时间(小时)"},
                new String[]{"training.default.epochs", "10", "默认训练轮数"},
                new String[]{"training.default.batch-size", "8", "默认批次大小"}
        );

        java.util.Set<String> existingKeys = new java.util.HashSet<>();
        systemConfigMapper.selectList(null).forEach(c -> existingKeys.add(c.getConfigKey()));

        for (String[] cfg : configs) {
            if (!existingKeys.contains(cfg[0])) {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(cfg[0]);
                config.setConfigValue(cfg[1]);
                config.setDescription(cfg[2]);
                config.setCreatedBy(1L);
                systemConfigMapper.insert(config);
                existingKeys.add(cfg[0]);
            }
        }
    }

    private void initStyles() {
        if (styleMapper.selectCount(null) == 0) {
            // 风格数据：[名称, 描述, config(JSON), 预览图URL]
            // 使用 picsum.photos 提供稳定可访问的预览图
            List<String[]> styles = List.of(
                    new String[]{"写实风格", "真实感强的图像风格，适合产品展示、人物肖像等场景",
                            "{\"category\":\"realistic\",\"prompt\": \"realistic, photorealistic, high detail\", \"negative_prompt\": \"cartoon, anime, drawing\"}",
                            "https://picsum.photos/seed/realistic/400/300"},
                    new String[]{"插画风格", "艺术插画风格，色彩丰富，适合宣传海报、社交媒体配图",
                            "{\"category\":\"artistic\",\"prompt\": \"illustration, colorful, artistic, painting style\", \"negative_prompt\": \"photorealistic, realistic\"}",
                            "https://picsum.photos/seed/illustration/400/300"},
                    new String[]{"极简风格", "简约现代风格，线条简洁，适合UI设计、品牌素材",
                            "{\"category\":\"general\",\"prompt\": \"minimalist, clean, modern design, simple\", \"negative_prompt\": \"complex, cluttered\"}",
                            "https://picsum.photos/seed/minimalist/400/300"},
                    new String[]{"复古风格", "怀旧复古风格，带有年代感，适合复古主题内容",
                            "{\"category\":\"artistic\",\"prompt\": \"vintage, retro, nostalgic, classic style\", \"negative_prompt\": \"modern, futuristic\"}",
                            "https://picsum.photos/seed/vintage/400/300"}
            );

            for (String[] s : styles) {
                Style style = new Style();
                style.setDesignerId(2L);
                style.setName(s[0]);
                style.setDescription(s[1]);
                style.setConfig(s[2]);
                style.setPreviewUrl(s[3]);  // 设置预览图
                style.setStatus("ACTIVE");
                style.setCreatedBy(2L);
                styleMapper.insert(style);
            }
            log.info("Default styles created with preview images");
        }
    }
}
