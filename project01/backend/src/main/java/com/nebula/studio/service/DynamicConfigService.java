package com.nebula.studio.service;

import com.nebula.studio.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicConfigService {

    private final SystemConfigMapper systemConfigMapper;

    /**
     * 缓存：key=configKey, value=configValue
     * 每次调用 getConfigValue 时都会尝试从 DB 刷新，确保拿到最新值
     */
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final Map<String, String> defaults = new ConcurrentHashMap<>();

    /**
     * 注册默认值（从 application.yml 注入）
     * 当数据库中没有对应配置时，使用默认值兜底
     */
    public void registerDefault(String key, String value) {
        if (value != null && !value.isBlank()) {
            defaults.put(key, value);
            log.info("注册默认配置: {}={}", key, maskValue(key, value));
        }
    }

    /**
     * 获取配置值：优先读数据库，数据库为空时读默认值
     */
    public String getConfigValue(String key) {
        // 每次都从 DB 刷新，确保拿到最新值（配置变更不频繁，直接查DB可接受）
        refreshCache();
        String val = cache.get(key);
        if (val != null && !val.isBlank()) {
            return val;
        }
        String defaultVal = defaults.getOrDefault(key, "");
        if (!defaultVal.isBlank()) {
            log.debug("配置 [{}] 数据库无值，使用默认值", key);
        }
        return defaultVal;
    }

    public String getConfigValue(String key, String defaultValue) {
        String val = getConfigValue(key);
        return (val == null || val.isBlank()) ? defaultValue : val;
    }

    /**
     * 从数据库刷新所有配置到缓存
     */
    private void refreshCache() {
        try {
            systemConfigMapper.selectList(null).forEach(c -> {
                String val = c.getConfigValue() != null ? c.getConfigValue() : "";
                cache.put(c.getConfigKey(), val);
            });
            log.debug("配置缓存已刷新，共 {} 条", cache.size());
        } catch (Exception e) {
            log.error("刷新配置缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 使缓存失效（配置更新后调用，强制下次读取时刷新）
     */
    public void invalidateCache() {
        cache.clear();
        log.info("配置缓存已清空，下次读取将从数据库加载");
    }

    /**
     * 对敏感值（如 API Key）进行脱敏，用于日志输出
     */
    private static String maskValue(String key, String value) {
        if (key != null && key.toLowerCase().contains("key")) {
            if (value != null && value.length() > 8) {
                return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
            }
            return "****";
        }
        return value;
    }
}
