package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.UserActivePatchRequest;
import com.sait.peelin.dto.v1.UserSummaryDto;
import com.sait.peelin.service.AdminUserService;
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
@Tag(name = "Admin users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<UserSummaryDto> list() {
        return adminUserService.list();
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public UserSummaryDto setActive(@PathVariable UUID id, @Valid @RequestBody UserActivePatchRequest req) {
        return adminUserService.setActive(id, req.getActive());
    }
}
