package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.DataPointDto;
import com.sait.peelin.service.AnalyticsService;
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
@Tag(name = "Admin analytics")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/metrics/total-revenue")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public BigDecimal totalRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.totalRevenue(start, end, bakerySelection);
    }

    @GetMapping("/revenue-over-time")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<DataPointDto> revenueOverTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.revenueOverTime(start, end, bakerySelection);
    }

    @GetMapping("/revenue-by-bakery")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<DataPointDto> revenueByBakery(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return analyticsService.revenueByBakery(start, end);
    }

    @GetMapping("/metrics/average-order-value")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public BigDecimal averageOrderValue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.averageOrderValue(start, end, bakerySelection);
    }

    @GetMapping("/series/average-order-value-over-time")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<DataPointDto> averageOrderValueOverTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.averageOrderValueOverTime(start, end, bakerySelection);
    }

    @GetMapping("/metrics/completion-rate")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public BigDecimal completionRate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.completionRate(start, end, bakerySelection);
    }

    @GetMapping("/series/completion-rate-over-time")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<DataPointDto> completionRateOverTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.completionRateOverTime(start, end, bakerySelection);
    }

    @GetMapping("/series/top-products")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<DataPointDto> topProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.topProducts(start, end, bakerySelection);
    }

    @GetMapping("/metrics/sales-by-employee-total")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public BigDecimal totalSalesByEmployee(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.totalSalesByEmployee(start, end, bakerySelection);
    }

    @GetMapping("/series/sales-by-employee")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<DataPointDto> salesByEmployee(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.salesByEmployee(start, end, bakerySelection);
    }

    @GetMapping("/meta/bakery-names")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<String> bakeryNames() {
        return analyticsService.bakeryNames();
    }

    @GetMapping("/meta/order-dates")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<LocalDate> orderDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String bakerySelection
    ) {
        return analyticsService.orderDatesInRange(start, end, bakerySelection);
    }
}
