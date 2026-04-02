package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.DataPointDto;
import com.sait.peelin.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Admin analytics", description = "KPI metrics and time-series data for the management dashboard. Requires ADMIN or EMPLOYEE role.")
@SecurityRequirement(name = "bearer-jwt")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Total revenue", description = "Returns total order revenue within the date range. Optionally scoped to a single bakery via `bakerySelection`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Total revenue as a decimal"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping("/metrics/total-revenue")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public BigDecimal totalRevenue(
            @Parameter(description = "Start date (inclusive), ISO format", example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date (inclusive), ISO format", example = "2024-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @Parameter(description = "Bakery name to filter by; omit for all bakeries") @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.totalRevenue(start, end, bakerySelection);
    }

    @Operation(summary = "Revenue over time", description = "Returns daily or aggregated revenue data points for charting within the date range.")
    @ApiResponse(responseCode = "200", description = "Revenue data points returned")
    @GetMapping("/revenue-over-time")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<DataPointDto> revenueOverTime(
            @Parameter(description = "Start date (inclusive), ISO format", example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date (inclusive), ISO format", example = "2024-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @Parameter(description = "Bakery name to filter by; omit for all bakeries") @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.revenueOverTime(start, end, bakerySelection);
    }

    @Operation(summary = "Revenue by bakery", description = "Returns a breakdown of total revenue grouped by bakery within the date range.")
    @ApiResponse(responseCode = "200", description = "Per-bakery revenue data points returned")
    @GetMapping("/revenue-by-bakery")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<DataPointDto> revenueByBakery(
            @Parameter(description = "Start date (inclusive), ISO format", example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date (inclusive), ISO format", example = "2024-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return analyticsService.revenueByBakery(start, end);
    }

    @Operation(summary = "Average order value", description = "Returns the mean order value within the date range.")
    @ApiResponse(responseCode = "200", description = "Average order value as a decimal")
    @GetMapping("/metrics/average-order-value")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public BigDecimal averageOrderValue(
            @Parameter(description = "Start date (inclusive), ISO format", example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date (inclusive), ISO format", example = "2024-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @Parameter(description = "Bakery name to filter by; omit for all bakeries") @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.averageOrderValue(start, end, bakerySelection);
    }

    @Operation(summary = "Average order value over time", description = "Returns average order value data points over time for charting.")
    @ApiResponse(responseCode = "200", description = "Average order value time-series data returned")
    @GetMapping("/series/average-order-value-over-time")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<DataPointDto> averageOrderValueOverTime(
            @Parameter(description = "Start date (inclusive), ISO format", example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date (inclusive), ISO format", example = "2024-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @Parameter(description = "Bakery name to filter by; omit for all bakeries") @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.averageOrderValueOverTime(start, end, bakerySelection);
    }

    @Operation(summary = "Order completion rate", description = "Returns the percentage of orders that reached COMPLETED status within the date range.")
    @ApiResponse(responseCode = "200", description = "Completion rate as a decimal (e.g. 0.87 = 87%)")
    @GetMapping("/metrics/completion-rate")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public BigDecimal completionRate(
            @Parameter(description = "Start date (inclusive), ISO format", example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date (inclusive), ISO format", example = "2024-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @Parameter(description = "Bakery name to filter by; omit for all bakeries") @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.completionRate(start, end, bakerySelection);
    }

    @Operation(summary = "Completion rate over time", description = "Returns order completion rate data points over time for charting.")
    @ApiResponse(responseCode = "200", description = "Completion rate time-series data returned")
    @GetMapping("/series/completion-rate-over-time")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<DataPointDto> completionRateOverTime(
            @Parameter(description = "Start date (inclusive), ISO format", example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date (inclusive), ISO format", example = "2024-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @Parameter(description = "Bakery name to filter by; omit for all bakeries") @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.completionRateOverTime(start, end, bakerySelection);
    }

    @Operation(summary = "Top products", description = "Returns the best-selling products ranked by total units sold within the date range.")
    @ApiResponse(responseCode = "200", description = "Top products data points returned")
    @GetMapping("/series/top-products")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<DataPointDto> topProducts(
            @Parameter(description = "Start date (inclusive), ISO format", example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date (inclusive), ISO format", example = "2024-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @Parameter(description = "Bakery name to filter by; omit for all bakeries") @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.topProducts(start, end, bakerySelection);
    }

    @Operation(summary = "Total sales by employee", description = "Returns the total value of orders attributed to employees within the date range.")
    @ApiResponse(responseCode = "200", description = "Total employee sales as a decimal")
    @GetMapping("/metrics/sales-by-employee-total")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public BigDecimal totalSalesByEmployee(
            @Parameter(description = "Start date (inclusive), ISO format", example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date (inclusive), ISO format", example = "2024-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @Parameter(description = "Bakery name to filter by; omit for all bakeries") @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.totalSalesByEmployee(start, end, bakerySelection);
    }

    @Operation(summary = "Sales by employee breakdown", description = "Returns per-employee sales data points for the date range.")
    @ApiResponse(responseCode = "200", description = "Per-employee sales data returned")
    @GetMapping("/series/sales-by-employee")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<DataPointDto> salesByEmployee(
            @Parameter(description = "Start date (inclusive), ISO format", example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date (inclusive), ISO format", example = "2024-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @Parameter(description = "Bakery name to filter by; omit for all bakeries") @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.salesByEmployee(start, end, bakerySelection);
    }

    @Operation(summary = "List bakery names", description = "Returns the display names of all bakeries. Used to populate the bakery filter dropdown in the dashboard.")
    @ApiResponse(responseCode = "200", description = "Bakery names returned")
    @GetMapping("/meta/bakery-names")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<String> bakeryNames() {
        return analyticsService.bakeryNames();
    }

    @Operation(summary = "List order dates in range", description = "Returns all dates within the range on which at least one order was placed.")
    @ApiResponse(responseCode = "200", description = "Order dates returned")
    @GetMapping("/meta/order-dates")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<LocalDate> orderDates(
            @Parameter(description = "Start date (inclusive), ISO format", example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date (inclusive), ISO format", example = "2024-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @Parameter(description = "Bakery name to filter by; omit for all bakeries") @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.orderDatesInRange(start, end, bakerySelection);
    }
}
