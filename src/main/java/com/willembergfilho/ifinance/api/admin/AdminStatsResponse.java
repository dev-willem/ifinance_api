package com.willembergfilho.ifinance.api.admin;

import com.willembergfilho.ifinance.application.admin.UserAdministrationUseCase.AdminStats;

import java.util.Map;

public record AdminStatsResponse(
        long totalUsers,
        Map<String, Long> usersByRole,
        Map<String, Long> usersByProvider,
        long totalSimulations,
        long totalInvestments
) {
    public static AdminStatsResponse from(AdminStats stats) {
        return new AdminStatsResponse(stats.totalUsers(), stats.usersByRole(), stats.usersByProvider(),
                stats.totalSimulations(), stats.totalInvestments());
    }
}
