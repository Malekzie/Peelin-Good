package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.CustomerDto;
import com.sait.peelin.dto.v1.CustomerPatchRequest;
import com.sait.peelin.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@Tag(name = "Admin customers")
public class AdminCustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<CustomerDto> list(@RequestParam(required = false) String search) {
        return customerService.listAdmin(search);
    }

    @GetMapping("/pending-photos")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<CustomerDto> pendingPhotos() {
        return customerService.pendingPhotos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public CustomerDto get(@PathVariable UUID id) {
        return customerService.get(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerDto patch(@PathVariable UUID id, @Valid @RequestBody CustomerPatchRequest req) {
        return customerService.patch(id, req);
    }

    @PostMapping("/{id}/approve-photo")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approvePhoto(@PathVariable UUID id) {
        customerService.approvePhoto(id);
    }

    @PostMapping("/{id}/reject-photo")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectPhoto(@PathVariable UUID id) {
        customerService.rejectPhoto(id);
    }
}
