package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.ReviewCreateRequest;
import com.sait.peelin.dto.v1.ReviewDto;
import com.sait.peelin.dto.v1.ReviewStatusPatchRequest;
import com.sait.peelin.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reviews")
public class ProductReviewController {

    private final ReviewService reviewService;

    @GetMapping("/products/{productId}/reviews")
    public List<ReviewDto> list(@PathVariable Integer productId) {
        return reviewService.forProduct(productId);
    }

    @GetMapping("/products/{productId}/reviews/average")
    public Double average(@PathVariable Integer productId) {
        return reviewService.averageForProduct(productId);
    }

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

    @PostMapping("/orders/{orderId}/reviews")
    public ReviewDto createForOrder(@PathVariable UUID orderId,
                                      @Valid @RequestBody ReviewCreateRequest req) {
        req.setOrderId(orderId);
        return reviewService.createForOrder(req);
    }

    @PatchMapping("/reviews/{reviewId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ReviewDto patchStatus(@PathVariable UUID reviewId, @Valid @RequestBody ReviewStatusPatchRequest req) {
        return reviewService.patchStatus(reviewId, req);
    }

    @GetMapping("/reviews/pending")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<ReviewDto> pending() {
        return reviewService.pending();
    }
}
