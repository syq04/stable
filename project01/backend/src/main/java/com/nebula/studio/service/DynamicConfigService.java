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

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final Map<String, String> defaults = new ConcurrentHashMap<>();
    private volatile long lastRefreshTime = 0;
    private static final long CACHE_TTL_MS = 60_000;

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

    public String getConfigValue(String key) {
        if (System.currentTimeMillis() - lastRefreshTime > CACHE_TTL_MS) {
            refreshCache();
        }
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

    private void refreshCache() {
        try {
            systemConfigMapper.selectList(null).forEach(c -> {
                String val = c.getConfigValue() != null ? c.getConfigValue() : "";
                cache.put(c.getConfigKey(), val);
            });
            lastRefreshTime = System.currentTimeMillis();
            log.debug("配置缓存已刷新，共 {} 条", cache.size());
        } catch (Exception e) {
            log.error("刷新配置缓存失败: {}", e.getMessage());
        }
    }

    public void invalidateCache() {
        cache.clear();
        lastRefreshTime = 0;
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
