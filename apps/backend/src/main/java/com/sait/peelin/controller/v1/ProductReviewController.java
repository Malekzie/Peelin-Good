package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.ReviewCreateRequest;
import com.sait.peelin.dto.v1.ReviewDto;
import com.sait.peelin.dto.v1.ReviewStatusPatchRequest;
import com.sait.peelin.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Customer reviews for products and bakery locations. New reviews are moderated by AI; manual status changes are admin-only.")
public class ProductReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "List reviews for product", description = "Returns all approved reviews for a given product.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reviews returned"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @GetMapping("/products/{productId}/reviews")
    public List<ReviewDto> list(@PathVariable Integer productId) {
        return reviewService.forProduct(productId);
    }

    @Operation(summary = "Get average rating for product", description = "Returns the average star rating across all approved reviews for a product.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Average rating returned (null if no reviews)"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @GetMapping("/products/{productId}/reviews/average")
    public Double average(@PathVariable Integer productId) {
        return reviewService.averageForProduct(productId);
    }

    @Operation(summary = "Submit a review", description = "Submit a customer review for a product. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review submitted"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/bakeries/{bakeryId}/reviews")
    public List<ReviewDto> listForBakery(@PathVariable Integer bakeryId) {
        return reviewService.forBakery(bakeryId);
    }

    @GetMapping("/bakeries/{bakeryId}/reviews/average")
    public Double averageForBakery(@PathVariable Integer bakeryId) {
        return reviewService.averageForBakery(bakeryId);
    }

    @PostMapping("/products/{productId}/reviews")
    public ReviewDto create(@PathVariable Integer productId, @Valid @RequestBody ReviewCreateRequest req) {
        return reviewService.create(productId, req);
    }

    @Operation(summary = "Update review status", description = "Approve or reject a review (override). Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review status updated"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Review not found", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping("/orders/{orderId}/reviews")
    public ReviewDto createForOrder(@PathVariable UUID orderId,
                                      @Valid @RequestBody ReviewCreateRequest req) {
        req.setOrderId(orderId);
        return reviewService.createForOrder(req);
    }

    @PatchMapping("/reviews/{reviewId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewDto> patchStatus(@PathVariable UUID reviewId, @Valid @RequestBody ReviewStatusPatchRequest req) {
        return reviewService.patchStatus(reviewId, req)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/reviews/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReviewDto> pending() {
        return reviewService.pending();
    }

    @GetMapping("/reviews/top")
    public List<ReviewDto> top(@RequestParam(defaultValue = "3") int limit) {
        return reviewService.topReviews(limit);
    }
}
