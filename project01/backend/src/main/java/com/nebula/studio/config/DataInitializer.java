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
        if (userMapper.selectById(1L) == null) {
            User admin = new User();
            admin.setId(1L);
            admin.setUsername("admin");
            admin.setEmail("admin@nebula.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setCreatedBy(0L);
            userMapper.insert(admin);
            log.info("Admin user created: admin@nebula.com / admin123");
        }
    }

    private void initDesignerUser() {
        if (userMapper.selectById(2L) == null) {
            User designer = new User();
            designer.setId(2L);
            designer.setUsername("designer");
            designer.setEmail("designer@nebula.com");
            designer.setPasswordHash(passwordEncoder.encode("designer123"));
            designer.setRole("DESIGNER");
            designer.setCreatedBy(0L);
            userMapper.insert(designer);
            log.info("Designer user created: designer@nebula.com / designer123");
        }
    }

    private void initNormalUser() {
        if (userMapper.selectById(3L) == null) {
            User user = new User();
            user.setId(3L);
            user.setUsername("user");
            user.setEmail("user@nebula.com");
            user.setPasswordHash(passwordEncoder.encode("user123"));
            user.setRole("USER");
            user.setCreatedBy(0L);
            userMapper.insert(user);
            log.info("Normal user created: user@nebula.com / user123");
        }
    }

    private void initSystemConfigs() {
        List<String> keys = List.of("app.name", "app.version", "ai.sd.api.url",
                "ai.doubao.api.url", "ai.openrouter.api-key", "ai.gemini.api-key",
                "ai.huggingface.api-key", "ai.siliconflow.api-key", "ai.zhipu.api-key",
                "storage.minio.url", "storage.minio.bucket",
                "security.jwt.expire-hours", "training.default.epochs", "training.default.batch-size");

        List<String[]> configs = List.of(
                new String[]{"app.name", "文图互转主题设计系统", "系统名称"},
                new String[]{"app.version", "1.0.0", "系统版本"},
                new String[]{"ai.sd.api.url", "http://localhost:7860", "Stable Diffusion API地址"},
                new String[]{"ai.doubao.api.url", "https://api.doubao.com", "豆包API地址"},
                new String[]{"ai.openrouter.api-key", "", "OpenRouter API Key（免费视觉模型）"},
                new String[]{"ai.gemini.api-key", "", "Google Gemini API Key"},
                new String[]{"ai.huggingface.api-key", "", "HuggingFace API Key"},
                new String[]{"ai.siliconflow.api-key", "", "硅基流动 API Key（国内免费文生图，推荐）"},
                new String[]{"ai.zhipu.api-key", "", "智谱AI API Key（国内免费文生图+图生文，推荐）"},
                new String[]{"storage.minio.url", "http://localhost:9000", "MinIO存储地址"},
                new String[]{"storage.minio.bucket", "ai-content", "MinIO存储桶名称"},
                new String[]{"security.jwt.expire-hours", "24", "JWT过期时间(小时)"},
                new String[]{"training.default.epochs", "10", "默认训练轮数"},
                new String[]{"training.default.batch-size", "8", "默认批次大小"}
        );

        for (String[] cfg : configs) {
            if (systemConfigMapper.selectList(null).stream()
                    .noneMatch(c -> cfg[0].equals(c.getConfigKey()))) {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(cfg[0]);
                config.setConfigValue(cfg[1]);
                config.setDescription(cfg[2]);
                config.setCreatedBy(1L);
                systemConfigMapper.insert(config);
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
