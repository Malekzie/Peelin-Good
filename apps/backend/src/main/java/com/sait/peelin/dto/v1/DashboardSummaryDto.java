package com.sait.peelin.dto.v1;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDto(
        BigDecimal totalRevenue,
        long totalOrders,
        long totalCustomers,
        long totalProducts,
        List<OrderDto> recentOrders
) {}
