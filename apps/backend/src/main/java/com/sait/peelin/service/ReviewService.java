package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ReviewCreateRequest;
import com.sait.peelin.dto.v1.ReviewDto;
import com.sait.peelin.dto.v1.ReviewStatusPatchRequest;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.*;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.ProductRepository;
import com.sait.peelin.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<ReviewDto> forProduct(Integer productId) {
        return reviewRepository.findByProduct_Id(productId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Double averageForProduct(Integer productId) {
        return reviewRepository.averageRatingForProduct(productId).orElse(null);
    }

    @Transactional
    public ReviewDto create(Integer productId, ReviewCreateRequest req) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.customer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Customer customer = customerRepository.findByUser_UserId(u.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer profile required"));
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Review r = new Review();
        r.setCustomer(customer);
        r.setProduct(product);
        r.setReviewRating(req.getRating());
        r.setReviewComment(req.getComment());
        r.setReviewSubmittedDate(OffsetDateTime.now());
        r.setReviewStatus(ReviewStatus.pending);
        return toDto(reviewRepository.save(r));
    }

    @Transactional
    public ReviewDto patchStatus(UUID reviewId, ReviewStatusPatchRequest req) {
        Review r = reviewRepository.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        r.setReviewStatus(req.getStatus());
        if (req.getStatus() == ReviewStatus.approved) {
            r.setReviewApprovalDate(OffsetDateTime.now());
        }
        return toDto(reviewRepository.save(r));
    }

    private ReviewDto toDto(Review r) {
        return new ReviewDto(
                r.getId(),
                r.getCustomer().getId(),
                r.getProduct().getId(),
                r.getEmployee() != null ? r.getEmployee().getId() : null,
                r.getReviewRating(),
                r.getReviewComment(),
                r.getReviewStatus(),
                r.getReviewSubmittedDate(),
                r.getReviewApprovalDate()
        );
    }
}
