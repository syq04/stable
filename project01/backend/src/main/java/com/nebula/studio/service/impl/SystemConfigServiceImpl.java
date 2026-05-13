package com.nebula.studio.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nebula.studio.common.BusinessException;
import com.nebula.studio.dto.request.UpdateConfigRequest;
import com.nebula.studio.entity.SystemConfig;
import com.nebula.studio.mapper.SystemConfigMapper;
import com.nebula.studio.service.DynamicConfigService;
import com.nebula.studio.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    private final DynamicConfigService dynamicConfigService;

    @Override
    public IPage<SystemConfig> listConfigs(int page, int size) {
        return page(new Page<>(page, size));
    }

    @Override
    public SystemConfig getByKey(String key) {
        return lambdaQuery().eq(SystemConfig::getConfigKey, key).one();
    }

    @Override
    public SystemConfig updateConfig(Long id, UpdateConfigRequest request) {
        SystemConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        if (request.getConfigValue() != null) {
            config.setConfigValue(request.getConfigValue());
        }
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }
        updateById(config);
        dynamicConfigService.invalidateCache();
        return config;
    }

    @Override
    public SystemConfig updateConfigByKey(String key, UpdateConfigRequest request) {
        SystemConfig config = getByKey(key);
        if (config == null) {
            throw new BusinessException("配置不存在: " + key);
        }
        if (request.getConfigValue() != null) {
            config.setConfigValue(request.getConfigValue());
        }
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }
        updateById(config);
        dynamicConfigService.invalidateCache();
        return config;
    }
}
