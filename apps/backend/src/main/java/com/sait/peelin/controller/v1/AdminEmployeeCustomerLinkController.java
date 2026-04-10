package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.EmployeeCustomerLinkCreateRequest;
import com.sait.peelin.service.EmployeeCustomerLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/employee-customer-links")
@RequiredArgsConstructor
@Tag(name = "Admin — employee/customer link", description = "One-to-one link for employee discount eligibility")
public class AdminEmployeeCustomerLinkController {

    private final EmployeeCustomerLinkService employeeCustomerLinkService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create employee↔customer link (1:1)")
    public void create(@Valid @RequestBody EmployeeCustomerLinkCreateRequest body) {
        employeeCustomerLinkService.createLinkAdmin(body.employeeId(), body.customerId());
    }
}
