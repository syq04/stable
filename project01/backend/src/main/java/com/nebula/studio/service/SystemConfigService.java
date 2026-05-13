package com.nebula.studio.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nebula.studio.dto.request.UpdateConfigRequest;
import com.nebula.studio.entity.SystemConfig;

public interface SystemConfigService extends IService<SystemConfig> {

    IPage<SystemConfig> listConfigs(int page, int size);

    SystemConfig getByKey(String key);

    SystemConfig updateConfig(Long id, UpdateConfigRequest request);

    SystemConfig updateConfigByKey(String key, UpdateConfigRequest request);
}
