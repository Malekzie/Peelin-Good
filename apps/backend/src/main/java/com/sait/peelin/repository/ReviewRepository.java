package com.sait.peelin.repository;

import com.sait.peelin.model.Review;
import com.sait.peelin.model.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByProduct_Id(Integer productId);
    List<Review> findByProduct_IdAndReviewStatusAndOrderIsNull(Integer productId, com.sait.peelin.model.ReviewStatus status);

    List<Review> findByBakery_IdAndOrderIsNotNullAndReviewStatus(Integer bakeryId, ReviewStatus reviewStatus);
    List<Review> findByReviewStatusOrderByReviewSubmittedDateDesc(com.sait.peelin.model.ReviewStatus status);
    List<Review> findByReviewStatusAndBakery_IdOrderByReviewSubmittedDateDesc(
            com.sait.peelin.model.ReviewStatus status,
            Integer bakeryId
    );

    /** Product-only reviews: {@code order} is null (excludes post-order / location reviews that reuse line-item product). */
    @Query("SELECT AVG(r.reviewRating) FROM Review r WHERE r.product.id = :productId AND r.reviewStatus = 'approved' AND r.order IS NULL")
    Optional<Double> averageRatingForProduct(@Param("productId") Integer productId);

    /** Location / service reviews only: tied to an order. Excludes product-detail reviews for the same bakery. */
    @Query("SELECT AVG(r.reviewRating) FROM Review r WHERE r.bakery.id = :bakeryId AND r.reviewStatus = 'approved' AND r.order IS NOT NULL")
    Optional<Double> averageRatingForBakery(@Param("bakeryId") Integer bakeryId);

    List<Review> findByReviewStatusOrderByReviewRatingDescReviewSubmittedDateDesc(ReviewStatus status);

    boolean existsByCustomer_IdAndProduct_IdAndOrderIsNull(UUID customerId, Integer productId);

    boolean existsByOrder_IdAndCustomer_Id(UUID orderId, UUID customerId);
}
