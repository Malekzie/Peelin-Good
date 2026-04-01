package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.EmployeeDto;
import com.sait.peelin.dto.v1.EmployeeUpsertRequest;
import com.sait.peelin.service.EmployeeAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
@Tag(name = "Admin employees")
public class AdminEmployeeController {

    private final EmployeeAdminService employeeAdminService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeDto create(@Valid @RequestBody EmployeeUpsertRequest req) {
        return employeeAdminService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeDto update(@PathVariable UUID id, @Valid @RequestBody EmployeeUpsertRequest req) {
        return employeeAdminService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        employeeAdminService.delete(id);
    }
}
