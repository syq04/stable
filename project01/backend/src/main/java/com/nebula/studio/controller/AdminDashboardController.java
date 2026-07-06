package com.nebula.studio.controller;

import com.nebula.studio.common.Result;
import com.nebula.studio.dto.response.DashboardStatsVO;
import com.nebula.studio.service.AuditLogService;
import com.nebula.studio.service.ImageRecordService;
import com.nebula.studio.service.StyleService;
import com.nebula.studio.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/system/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final UserService userService;
    private final ImageRecordService imageRecordService;
    private final StyleService styleService;

    @GetMapping("/stats")
    public Result<DashboardStatsVO> getStats() {
        DashboardStatsVO stats = new DashboardStatsVO();
        stats.setTotalUsers(userService.count());
        stats.setTotalImages(imageRecordService.count());
        stats.setTotalStyles(styleService.count());

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        stats.setTodayImages(imageRecordService.lambdaQuery()
                .ge(com.nebula.studio.entity.ImageRecord::getCreatedAt, todayStart)
                .count());

        return Result.success(stats);
    }
}
