package com.sait.peelin.dto.v1;

import java.math.BigDecimal;
import java.util.List;

public class DashboardSummaryDto {
    private final BigDecimal totalRevenue;
    private final long totalOrders;
    private final long totalCustomers;
    private final long totalProducts;
    private final List<OrderDto> recentOrders;

    public DashboardSummaryDto(
            BigDecimal totalRevenue,
            long totalOrders,
            long totalCustomers,
            long totalProducts,
            List<OrderDto> recentOrders
    ) {
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.totalCustomers = totalCustomers;
        this.totalProducts = totalProducts;
        this.recentOrders = recentOrders;
    }

    public BigDecimal totalRevenue() { return totalRevenue; }
    public long totalOrders() { return totalOrders; }
    public long totalCustomers() { return totalCustomers; }
    public long totalProducts() { return totalProducts; }
    public List<OrderDto> recentOrders() { return recentOrders; }
}
