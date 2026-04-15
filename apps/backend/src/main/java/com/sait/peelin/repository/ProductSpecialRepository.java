package com.sait.peelin.repository;

import com.sait.peelin.model.ProductSpecial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductSpecialRepository extends JpaRepository<ProductSpecial, Integer> {

    /**
     * First matching row for the calendar day (lowest {@code product_special_id} if several share the date).
     */
    Optional<ProductSpecial> findFirstByFeaturedOnOrderByProductSpecialIdAsc(LocalDate featuredOn);
    List<ProductSpecial> findByFeaturedOnOrderByProductSpecialIdAsc(LocalDate featuredOn);

    /**
     * Returns {@code true} when any row already has {@code featuredOn} set to the given date.
     * Used by the create path to enforce the one-special-per-day constraint.
     */
    boolean existsByFeaturedOn(LocalDate featuredOn);

    /**
     * Returns {@code true} when a <em>different</em> row already has {@code featuredOn} set to the
     * given date.  The {@code productSpecialId} exclusion prevents a self-conflict when updating an
     * existing special to keep the same date.
     */
    boolean existsByFeaturedOnAndProductSpecialIdNot(LocalDate featuredOn, Integer productSpecialId);
}
