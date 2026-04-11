package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.UserActivePatchRequest;
import com.sait.peelin.dto.v1.UserSummaryDto;
import com.sait.peelin.service.AdminUserService;
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
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin users", description = "User account management — view and toggle active status. Requires ADMIN or EMPLOYEE role.")
@SecurityRequirement(name = "bearer-jwt")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "List users", description = "Returns a summary list of all user accounts across all roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User list returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<UserSummaryDto> list() {
        return adminUserService.list();
    }

    @Operation(summary = "List staff users", description = "Returns all admin and employee users. Used for messaging recipient lists.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff list returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<UserSummaryDto> listStaff() {
        return adminUserService.listStaff();
    }

    @Operation(summary = "Set user active status", description = "Enable or disable a user account. Disabled users cannot log in.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User active status updated"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public UserSummaryDto setActive(@PathVariable UUID id, @Valid @RequestBody UserActivePatchRequest req) {
        return adminUserService.setActive(id, req.getActive());
    }
}
