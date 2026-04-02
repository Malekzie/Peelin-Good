package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.RewardDto;
import com.sait.peelin.service.RewardQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Rewards", description = "Customer loyalty reward queries")
@SecurityRequirement(name = "bearer-jwt")
public class RewardQueryController {

    private final RewardQueryService rewardQueryService;

    @Operation(summary = "List rewards for a customer", description = "Returns all rewards earned by a specific customer. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rewards returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping("/customers/{customerId}/rewards")
    @PreAuthorize("isAuthenticated()")
    public List<RewardDto> forCustomer(@PathVariable UUID customerId) {
        return rewardQueryService.listForCustomer(customerId);
    }

    @Operation(summary = "List all rewards", description = "Returns rewards across all customers. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All rewards returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping("/rewards")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RewardDto> all() {
        return rewardQueryService.listAll();
    }
}
