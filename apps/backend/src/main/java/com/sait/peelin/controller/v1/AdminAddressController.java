package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.AddressCreateRequest;
import com.sait.peelin.dto.v1.AddressSummaryDto;
import com.sait.peelin.service.AddressAdminService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/addresses")
@RequiredArgsConstructor
@Tag(name = "Admin addresses", description = "Manage postal addresses used by bakery locations. Requires ADMIN role.")
@SecurityRequirement(name = "bearer-jwt")
public class AdminAddressController {

    private final AddressAdminService addressAdminService;

    @Operation(summary = "List all addresses", description = "Returns a summary list of all addresses on record.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AddressSummaryDto> list() {
        return addressAdminService.listAll();
    }

    @Operation(summary = "Create address", description = "Create a new address record. Returns the generated address ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created, ID returned"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> create(@Valid @RequestBody AddressCreateRequest req) {
        return Map.of("id", addressAdminService.create(req));
    }
}
