package com.nebula.studio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nebula.studio.entity.AuditLog;
import com.nebula.studio.mapper.AuditLogMapper;
import com.nebula.studio.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {

    @Override
    public IPage<AuditLog> listLogs(String operationType, String targetType, int page, int size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(AuditLog::getOperationType, operationType);
        }
        if (StringUtils.hasText(targetType)) {
            wrapper.eq(AuditLog::getTargetType, targetType);
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public void log(String operationType, String targetType, Long targetId, Long operatorId,
                    String beforeData, String afterData, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setOperationType(operationType);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setOperatorId(operatorId);
        auditLog.setBeforeData(beforeData);
        auditLog.setAfterData(afterData);
        auditLog.setIpAddress(ipAddress);
        save(auditLog);
    }

    @Override
    public String exportCsv(String operationType, String targetType) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(AuditLog::getOperationType, operationType);
        }
        if (StringUtils.hasText(targetType)) {
            wrapper.eq(AuditLog::getTargetType, targetType);
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt);

        StringBuilder sb = new StringBuilder();
        sb.append("ID,操作类型,目标类型,目标ID,操作人ID,IP地址,操作时间\n");
        list(wrapper).forEach(log -> {
            sb.append(log.getId()).append(",")
              .append(escapeCsv(log.getOperationType())).append(",")
              .append(escapeCsv(log.getTargetType())).append(",")
              .append(log.getTargetId() != null ? log.getTargetId() : "").append(",")
              .append(log.getOperatorId() != null ? log.getOperatorId() : "").append(",")
              .append(escapeCsv(log.getIpAddress())).append(",")
              .append(log.getCreatedAt()).append("\n");
        });
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
