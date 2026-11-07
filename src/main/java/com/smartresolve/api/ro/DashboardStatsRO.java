package com.smartresolve.api.ro;

public record DashboardStatsRO(
        long open,
        long assigned,
        long inProgress,
        long resolved,
        long closed,
        long total
) {
}
