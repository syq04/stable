package com.nebula.studio.interceptor;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    public RateLimitInterceptor() {
        limiters.put("/api/auth/login",    RateLimiter.create(0.2));
        limiters.put("/api/auth/register", RateLimiter.create(0.1));
        limiters.put("/api/text2image/generate", RateLimiter.create(1.0));
        limiters.put("/api/image2text/analyze",  RateLimiter.create(0.5));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String path = request.getRequestURI();
        RateLimiter limiter = limiters.get(path);
        if (limiter == null) {
            return true;
        }
        if (limiter.tryAcquire(3, TimeUnit.SECONDS)) {
            return true;
        }
        log.warn("Rate limit exceeded for path: {} from IP: {}", path, request.getRemoteAddr());
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后重试\"}");
        return false;
    }
}
