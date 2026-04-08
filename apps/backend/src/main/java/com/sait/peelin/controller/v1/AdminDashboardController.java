package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.DashboardSummaryDto;
import com.sait.peelin.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin dashboard", description = "High-level summary KPIs for the staff dashboard. Requires ADMIN or EMPLOYEE role.")
@SecurityRequirement(name = "bearer-jwt")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get dashboard summary", description = "Returns aggregated KPIs including total orders, revenue, active customers, and pending items for today.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard summary returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public DashboardSummaryDto summary() {
        return dashboardService.summary();
    }
}
