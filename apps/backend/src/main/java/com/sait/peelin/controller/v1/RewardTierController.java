package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.RewardTierDto;
import com.sait.peelin.dto.v1.RewardTierUpsertRequest;
import com.sait.peelin.service.RewardTierService;
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

@RestController
@RequestMapping("/api/v1/reward-tiers")
@RequiredArgsConstructor
@Tag(name = "Reward tiers", description = "Loyalty program tier configuration. Create/update/delete require ADMIN role.")
public class RewardTierController {

    private final RewardTierService rewardTierService;

    @Operation(summary = "List reward tiers", description = "Returns all configured loyalty reward tiers.")
    @ApiResponse(responseCode = "200", description = "Reward tiers returned")
    @GetMapping
    public List<RewardTierDto> list() {
        return rewardTierService.list();
    }

    @Operation(summary = "Get reward tier", description = "Returns a single reward tier by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tier found"),
            @ApiResponse(responseCode = "404", description = "Tier not found", content = @Content)
    })
    @GetMapping("/{id}")
    public RewardTierDto get(@PathVariable Integer id) {
        return rewardTierService.get(id);
    }

    @Operation(summary = "Create reward tier", description = "Create a new loyalty reward tier. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tier created"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public RewardTierDto create(@Valid @RequestBody RewardTierUpsertRequest req) {
        return rewardTierService.create(req);
    }

    @Operation(summary = "Update reward tier", description = "Replace all fields on an existing reward tier. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tier updated"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Tier not found", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RewardTierDto update(@PathVariable Integer id, @Valid @RequestBody RewardTierUpsertRequest req) {
        return rewardTierService.update(id, req);
    }

    @Operation(summary = "Delete reward tier", description = "Permanently delete a reward tier. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tier deleted"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Tier not found", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        rewardTierService.delete(id);
    }
}
