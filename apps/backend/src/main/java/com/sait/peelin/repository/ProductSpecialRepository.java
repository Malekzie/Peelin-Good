package com.sait.peelin.repository;

import com.sait.peelin.model.ProductSpecial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ProductSpecialRepository extends JpaRepository<ProductSpecial, Integer> {

    /**
     * First matching row for the calendar day (lowest {@code product_special_id} if several share the date).
     */
    Optional<ProductSpecial> findFirstByFeaturedOnOrderByProductSpecialIdAsc(LocalDate featuredOn);
}
