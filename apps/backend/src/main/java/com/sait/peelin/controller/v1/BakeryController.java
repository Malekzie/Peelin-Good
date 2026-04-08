package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.BakeryDto;
import com.sait.peelin.dto.v1.BakeryHourDto;
import com.sait.peelin.dto.v1.BakeryUpsertRequest;
import com.sait.peelin.service.BakeryService;
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
@RequestMapping("/api/v1/bakeries")
@RequiredArgsConstructor
@Tag(name = "Bakeries", description = "Browse bakery locations and hours. Create/update/delete require ADMIN role.")
public class BakeryController {

    private final BakeryService bakeryService;

    @Operation(summary = "List bakeries", description = "Returns all bakeries. Optionally filter by name using the search parameter.")
    @ApiResponse(responseCode = "200", description = "List of bakeries returned")
    @GetMapping
    public List<BakeryDto> list(@RequestParam(required = false) String search) {
        return bakeryService.list(search);
    }

    @Operation(summary = "Get bakery", description = "Returns a single bakery by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bakery found"),
            @ApiResponse(responseCode = "404", description = "Bakery not found", content = @Content)
    })
    @GetMapping("/{id}")
    public BakeryDto get(@PathVariable Integer id) {
        return bakeryService.get(id);
    }

    @Operation(summary = "Get bakery hours", description = "Returns the weekly operating hours for a bakery.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hours returned"),
            @ApiResponse(responseCode = "404", description = "Bakery not found", content = @Content)
    })
    @GetMapping("/{id}/hours")
    public List<BakeryHourDto> hours(@PathVariable Integer id) {
        return bakeryService.hours(id);
    }

    @Operation(summary = "Create bakery", description = "Create a new bakery location. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bakery created"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BakeryDto create(@Valid @RequestBody BakeryUpsertRequest req) {
        return bakeryService.create(req);
    }

    @Operation(summary = "Update bakery", description = "Replace all fields on an existing bakery. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bakery updated"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Bakery not found", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BakeryDto update(@PathVariable Integer id, @Valid @RequestBody BakeryUpsertRequest req) {
        return bakeryService.update(id, req);
    }

    @Operation(summary = "Delete bakery", description = "Permanently delete a bakery. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Bakery deleted"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Bakery not found", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        bakeryService.delete(id);
    }
}
