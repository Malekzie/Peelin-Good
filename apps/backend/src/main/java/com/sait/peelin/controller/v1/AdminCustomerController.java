package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.CustomerAdminCreateRequest;
import com.sait.peelin.dto.v1.CustomerDto;
import com.sait.peelin.dto.v1.CustomerPatchRequest;
import com.sait.peelin.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "Admin customers", description = "Staff-facing customer management, including photo moderation. Requires ADMIN or EMPLOYEE role.")
@SecurityRequirement(name = "bearer-jwt")
public class AdminCustomerController {

    private final CustomerService customerService;

    @Operation(summary = "List customers", description = "Returns all customers. Optionally filter by name or email using `search`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer list returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<CustomerDto> list(@RequestParam(required = false) String search) {
        return customerService.listAdmin(search);
    }

    @Operation(summary = "List customers with pending profile photos", description = "Returns customers who have uploaded a profile photo that is awaiting approval.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending photo customers returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping("/pending-photos")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<CustomerDto> pendingPhotos() {
        return customerService.pendingPhotos();
    }

    @Operation(summary = "Get customer", description = "Returns a single customer by UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Customer not found", content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public CustomerDto get(@PathVariable UUID id) {
        return customerService.get(id);
    }

    @Operation(summary = "Create customer", description = "Create a guest customer or link a customer-role user that has no profile yet. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer created"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "409", description = "User already has a profile or is an employee", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerDto create(@Valid @RequestBody CustomerAdminCreateRequest req) {
        return customerService.createAdmin(req);
    }

    @Operation(summary = "Patch customer", description = "Partially update a customer record. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer updated"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Customer not found", content = @Content)
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerDto patch(@PathVariable UUID id, @Valid @RequestBody CustomerPatchRequest req) {
        return customerService.patch(id, req);
    }

    @Operation(summary = "Approve profile photo", description = "Approve a customer's pending profile photo, making it publicly visible.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Photo approved"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Customer not found", content = @Content)
    })
    @PostMapping("/{id}/approve-photo")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approvePhoto(@PathVariable UUID id) {
        customerService.approvePhoto(id);
    }

    @Operation(summary = "Reject profile photo", description = "Reject and remove a customer's pending profile photo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Photo rejected"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Customer not found", content = @Content)
    })
    @PostMapping("/{id}/reject-photo")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectPhoto(@PathVariable UUID id) {
        customerService.rejectPhoto(id);
    }
}
