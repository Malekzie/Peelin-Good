package com.sait.peelin.repository;

import com.sait.peelin.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    Optional<Product> findFirstByProductNameIgnoreCase(String productName);

    List<Product> findByProductNameContainingIgnoreCase(String search);

    @Query("SELECT DISTINCT pt.product FROM ProductTag pt WHERE pt.tag.id = :tagId")
    List<Product> findByTagId(@Param("tagId") Integer tagId);
}
