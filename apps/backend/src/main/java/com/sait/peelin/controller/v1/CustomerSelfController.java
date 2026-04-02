package com.sait.peelin.controller.v1;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('CUSTOMER')")
@RestController
@RequestMapping("/api/v1/customers/me")
@RequiredArgsConstructor
@Tag(name = "Customer profile", description = "Read and update the authenticated customer's own profile")
@SecurityRequirement(name = "bearer-jwt")
public class CustomerSelfController {

    private final CustomerService customerService;

    @Operation(summary = "Get my profile", description = "Returns the full profile for the currently authenticated customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not a customer account", content = @Content)
    })
    @GetMapping
    public CustomerDto me() {
        return customerService.me();
    }

    @Operation(summary = "Update my profile", description = "Partially update the authenticated customer's profile fields.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated profile returned"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @PatchMapping
    public CustomerDto patch(@Valid @RequestBody CustomerPatchRequest req) {
        return customerService.patchMe(req);
    }
}
