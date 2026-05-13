package com.nebula.studio.dto.response;

import lombok.Data;

@Data
public class DashboardStatsVO {
    private long totalUsers;
    private long totalImages;
    private long totalStyles;
    private long totalTasks;
    private long todayImages;
    private long runningTasks;
}
