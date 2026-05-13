package com.nebula.studio.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nebula.studio.common.Result;
import com.nebula.studio.dto.request.UpdateConfigRequest;
import com.nebula.studio.entity.SystemConfig;
import com.nebula.studio.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/configs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public Result<IPage<SystemConfig>> listConfigs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(systemConfigService.listConfigs(page, size));
    }

    @GetMapping("/{key:.+}")
    public Result<SystemConfig> getByKey(@PathVariable("key") String key) {
        return Result.success(systemConfigService.getByKey(key));
    }

    @PutMapping("/{key:.+}")
    public Result<SystemConfig> updateConfigByKey(@PathVariable("key") String key,
                                                  @RequestBody UpdateConfigRequest request) {
        return Result.success(systemConfigService.updateConfigByKey(key, request));
    }
}
