package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.EmployeeDto;
import com.sait.peelin.dto.v1.EmployeePatchRequest;
import com.sait.peelin.service.EmployeeProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
@Tag(name = "Employee", description = "Employee self-service profile and staff directory. Directory endpoints require ADMIN role.")
@SecurityRequirement(name = "bearer-jwt")
public class EmployeeController {

    private final EmployeeProfileService employeeProfileService;

    @Operation(summary = "Get my employee profile", description = "Returns the authenticated employee's own profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @GetMapping("/me")
    public EmployeeDto me() {
        return employeeProfileService.me();
    }

    @Operation(summary = "Update my employee profile", description = "Partially update the authenticated employee's own profile fields.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated profile returned"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @PatchMapping("/me")
    public EmployeeDto patchMe(@Valid @RequestBody EmployeePatchRequest req) {
        return employeeProfileService.patchMe(req);
    }

    @Operation(summary = "Get my assigned bakery IDs", description = "Returns the list of bakery IDs the authenticated employee is assigned to.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bakery ID list returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @GetMapping("/me/bakeries")
    public List<Integer> myBakeries() {
        return employeeProfileService.myBakeryIds();
    }

    @Operation(summary = "List all staff", description = "Returns all employee records. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff list returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public List<EmployeeDto> list() {
        return employeeProfileService.listAll();
    }

    @Operation(summary = "Get staff member", description = "Returns a specific employee by UUID. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Employee not found", content = @Content)
    })
    @GetMapping("/staff/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeDto get(@PathVariable UUID id) {
        return employeeProfileService.get(id);
    }
}
