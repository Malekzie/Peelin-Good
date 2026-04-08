package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.ProductDto;
import com.sait.peelin.dto.v1.ProductUpsertRequest;
import com.sait.peelin.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Browse bakery products. Create/update/delete require ADMIN role.")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "List products", description = "Returns all products. Filter by name with `search` or by tag ID with `tagId`.")
    @ApiResponse(responseCode = "200", description = "List of products returned")
    @GetMapping
    public List<ProductDto> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer tagId
    ) {
        return productService.list(search, tagId);
    }

    @Operation(summary = "Get product", description = "Returns a single product by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ProductDto get(@PathVariable Integer id) {
        return productService.get(id);
    }

    @Operation(summary = "Create product", description = "Create a new bakery product. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody ProductUpsertRequest req) {
        return productService.create(req);
    }

    @Operation(summary = "Update product", description = "Replace all fields on an existing product. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDto update(@PathVariable Integer id, @Valid @RequestBody ProductUpsertRequest req) {
        return productService.update(id, req);
    }

    @Operation(summary = "Delete product", description = "Permanently delete a product. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        productService.delete(id);
    }

    /** Upload or replace the image for a product (stored in the {@code bakery/} folder). */
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDto uploadImage(@PathVariable Integer id,
                                  @RequestParam("image") MultipartFile image) {
        return productService.uploadImage(id, image);
    }
}
