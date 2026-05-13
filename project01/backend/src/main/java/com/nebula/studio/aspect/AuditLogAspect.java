package com.nebula.studio.aspect;

import com.nebula.studio.security.JwtUserDetails;
import com.nebula.studio.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 审计日志切面：拦截 Controller 层的写操作，自动记录审计日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    /**
     * 拦截所有 Controller 的写操作（POST/PUT/PATCH/DELETE）
     */
    @Around("execution(* com.nebula.studio.controller..*(..))")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        String httpMethod = getHttpMethod();
        if (!isWriteOperation(httpMethod)) {
            return joinPoint.proceed();
        }

        String targetType = resolveTargetType(joinPoint);
        Long targetId = resolveTargetId(joinPoint);
        Long operatorId = getCurrentUserId();
        String ipAddress = getClientIp();
        String operationType = resolveOperationType(httpMethod);

        try {
            Object result = joinPoint.proceed();
            // 从返回结果中尝试提取 targetId（适用于 CREATE）
            Long newId = resolveTargetIdFromResult(result);
            if (newId != null) targetId = newId;
            auditLogService.log(operationType, targetType, targetId, operatorId,
                    null, "操作成功: " + httpMethod + " " + joinPoint.getSignature().getName(), ipAddress);
            return result;
        } catch (Throwable e) {
            auditLogService.log(operationType, targetType, targetId, operatorId,
                    null, "操作失败: " + e.getMessage(), ipAddress);
            throw e;
        }
    }

    private boolean isWriteOperation(String method) {
        return "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private String resolveOperationType(String httpMethod) {
        return switch (httpMethod) {
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "UNKNOWN";
        };
    }

    private String resolveTargetType(ProceedingJoinPoint joinPoint) {
        // 使用 getSignature().getDeclaringTypeName() 获取真实类名（不受 AOP 代理影响）
        String className = joinPoint.getSignature().getDeclaringTypeName();
        // 提取简单类名
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        // AdminUserController -> USER
        if (simpleName.startsWith("Admin") && simpleName.contains("Controller")) {
            String entity = simpleName.substring(5).replace("Controller", "").toUpperCase();
            if (entity.contains("USER")) return "USER";
            if (entity.contains("STYLE")) return "STYLE";
            if (entity.contains("IMAGE")) return "IMAGE";
            if (entity.contains("CONFIG")) return "CONFIG";
            if (entity.contains("LOG")) return "LOG";
            if (entity.contains("MODEL")) return "MODEL";
            if (entity.contains("TASK")) return "TASK";
            return entity;
        }
        // 非 Admin 前缀的 Controller（如 StyleController）
        if (simpleName.endsWith("Controller")) {
            String entity = simpleName.replace("Controller", "").toUpperCase();
            if (entity.contains("USER")) return "USER";
            if (entity.contains("STYLE")) return "STYLE";
            if (entity.contains("IMAGE")) return "IMAGE";
            if (entity.contains("CONFIG")) return "CONFIG";
            if (entity.contains("MODEL")) return "MODEL";
            return entity;
        }
        return "UNKNOWN";
    }

    private Long resolveTargetId(ProceedingJoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Long) return (Long) arg;
            if (arg instanceof Integer) return ((Integer) arg).longValue();
        }
        return null;
    }

    private Long resolveTargetIdFromResult(Object result) {
        if (result == null) return null;
        try {
            // 支持 Result<Entity> 或 Entity
            Object data = result;
            try {
                java.lang.reflect.Method getData = result.getClass().getMethod("getData");
                data = getData.invoke(result);
            } catch (Exception ignored) {}

            if (data == null) return null;
            java.lang.reflect.Method getId = data.getClass().getMethod("getId");
            Object id = getId.invoke(data);
            if (id instanceof Long) return (Long) id;
            if (id instanceof Integer) return ((Integer) id).longValue();
        } catch (Exception ignored) {}
        return null;
    }

    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                Object principal = auth.getPrincipal();
                if (principal instanceof JwtUserDetails) {
                    return ((JwtUserDetails) principal).getUserId();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getHttpMethod() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "UNKNOWN";
            return attrs.getRequest().getMethod();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
