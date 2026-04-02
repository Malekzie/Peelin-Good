package com.sait.peelin.repository;

import com.sait.peelin.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByProduct_Id(Integer productId);
    List<Review> findByBakery_Id(Integer bakeryId);
    List<Review> findByReviewStatusOrderByReviewSubmittedDateDesc(com.sait.peelin.model.ReviewStatus status);
    List<Review> findByReviewStatusAndBakery_IdOrderByReviewSubmittedDateDesc(
            com.sait.peelin.model.ReviewStatus status,
            Integer bakeryId
    );

    @Query("SELECT AVG(r.reviewRating) FROM Review r WHERE r.product.id = :productId AND r.reviewStatus = 'approved'")
    Optional<Double> averageRatingForProduct(@Param("productId") Integer productId);

    @Query("SELECT AVG(r.reviewRating) FROM Review r WHERE r.bakery.id = :bakeryId AND r.reviewStatus = 'approved'")
    Optional<Double> averageRatingForBakery(@Param("bakeryId") Integer bakeryId);
}
