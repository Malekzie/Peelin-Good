package com.sait.peelin.repository;

import com.sait.peelin.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Integer> {

    List<Batch> findByBakery_Id(Integer bakeryId);

    List<Batch> findByProduct_Id(Integer productId);

    @Query("SELECT b FROM Batch b WHERE b.bakery.id = :bakeryId AND b.batchExpiryDate >= :now ORDER BY b.batchExpiryDate ASC")
    List<Batch> findActiveByBakery(@Param("bakeryId") Integer bakeryId, @Param("now") OffsetDateTime now);
}
