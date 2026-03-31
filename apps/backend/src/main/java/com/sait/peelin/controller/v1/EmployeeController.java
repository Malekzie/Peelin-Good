package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.EmployeeDto;
import com.sait.peelin.service.EmployeeProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
@Tag(name = "Employee")
public class EmployeeController {

    private final EmployeeProfileService employeeProfileService;

    @GetMapping("/me")
    public EmployeeDto me() {
        return employeeProfileService.me();
    }

    @GetMapping("/me/bakeries")
    public List<Integer> myBakeries() {
        return employeeProfileService.myBakeryIds();
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public List<EmployeeDto> list() {
        return employeeProfileService.listAll();
    }

    @GetMapping("/staff/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeDto get(@PathVariable UUID id) {
        return employeeProfileService.get(id);
    }
}
