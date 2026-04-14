package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.ProductSpecialDto;
import com.sait.peelin.dto.v1.ProductSpecialTodayDto;
import com.sait.peelin.dto.v1.ProductSpecialUpsertRequest;
import com.sait.peelin.service.ProductSpecialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/product-specials")
@RequiredArgsConstructor
@Tag(name = "Product specials", description = "Daily featured product promotions")
public class ProductSpecialController {

    private final ProductSpecialService productSpecialService;

    // ── Read ─────────────────────────────────────────────────────

    @Operation(
            summary = "Get today's special",
            description = "Returns the featured product for the given calendar date. Pass the client's local date via `date` (ISO format, e.g. 2024-06-01); defaults to the server's current date. `productId` is null when no special is configured for that day."
    )
    @ApiResponse(responseCode = "200", description = "Today's special returned (productId may be null if none configured)")
    @GetMapping("/today")
    public ProductSpecialTodayDto today(
            @Parameter(description = "Calendar date in ISO format (yyyy-MM-dd). Defaults to today's server date.", example = "2024-06-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate d = date != null ? date : LocalDate.now();
        return productSpecialService.findFirstForDate(d);
    }

    @Operation(summary = "Get all specials")
    @ApiResponse(responseCode = "200", description = "All product specials returned")
    @GetMapping("/all")
    public List<ProductSpecialDto> allSpecials() {
        return productSpecialService.findAllSpecials();
    }

    // ── Write ─────────────────────────────────────────────────────

    @Operation(summary = "Create a product special", description = "Creates a new product special entry. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product special created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Referenced product not found")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductSpecialDto create(@Valid @RequestBody ProductSpecialUpsertRequest req) {
        return productSpecialService.create(req);
    }

    @Operation(summary = "Update a product special", description = "Replaces all mutable fields on an existing product special. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product special updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Product special or referenced product not found")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductSpecialDto update(@PathVariable Integer id,
                                    @Valid @RequestBody ProductSpecialUpsertRequest req) {
        return productSpecialService.update(id, req);
    }

    @Operation(summary = "Delete a product special", description = "Permanently removes a product special entry. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product special deleted"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Product special not found")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        productSpecialService.delete(id);
    }
}
