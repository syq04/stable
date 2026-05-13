package com.nebula.studio.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nebula.studio.common.Result;
import com.nebula.studio.entity.AuditLog;
import com.nebula.studio.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/system/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public Result<IPage<AuditLog>> listLogs(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(auditLogService.listLogs(operationType, targetType, page, size));
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportLogs(@RequestBody(required = false) Map<String, String> params) {
        String operationType = params != null ? params.get("operationType") : null;
        String targetType = params != null ? params.get("targetType") : null;
        String csv = auditLogService.exportCsv(operationType, targetType);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_logs.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }
}
