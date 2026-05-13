package com.nebula.studio.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nebula.studio.entity.AuditLog;

public interface AuditLogService extends IService<AuditLog> {

    IPage<AuditLog> listLogs(String operationType, String targetType, int page, int size);

    void log(String operationType, String targetType, Long targetId, Long operatorId,
             String beforeData, String afterData, String ipAddress);

    String exportCsv(String operationType, String targetType);
}
