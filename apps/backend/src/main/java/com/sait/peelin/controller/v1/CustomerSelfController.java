package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.*;
import com.sait.peelin.service.CustomerPreferenceService;
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

@PreAuthorize("hasRole('CUSTOMER')")
@RestController
@RequestMapping("/api/v1/customers/me")
@RequiredArgsConstructor
@Tag(name = "Customer profile", description = "Read and update the authenticated customer's own profile")
@SecurityRequirement(name = "bearer-jwt")
public class CustomerSelfController {

    private final CustomerService customerService;
    private final CustomerPreferenceService customerPreferenceService;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto createMe(@Valid @RequestBody CustomerBootstrapRequest req) {
        return customerService.createMyProfile(req);
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

    @Operation(summary = "Delete my account", description = "Permanently deletes the authenticated customer's account.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe() {
        customerService.deleteMe();
    }

    @GetMapping("/preferences")
    public List<CustomerPreferenceDto> getPreferences() {
        return customerPreferenceService.getMyPreferences();
    }

    @PutMapping("/preferences")
    public List<CustomerPreferenceDto> savePreferences(@RequestBody CustomerPreferenceSaveRequest request) {
        return customerPreferenceService.saveMyPreferences(request);
    }

    @Operation(summary = "Deactivate my account", description = "Deactivates the authenticated customer's account without deleting it.")
    @PatchMapping("/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateMe() {
        customerService.deactivateMe();
    }
}
