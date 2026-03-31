package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.DashboardSummaryDto;
import com.sait.peelin.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin dashboard")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public DashboardSummaryDto summary() {
        return dashboardService.summary();
    }
}
