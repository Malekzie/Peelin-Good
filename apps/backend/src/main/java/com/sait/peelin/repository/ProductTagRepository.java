package com.sait.peelin.repository;

import com.sait.peelin.model.ProductTag;
import com.sait.peelin.model.ProductTagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductTagRepository extends JpaRepository<ProductTag, ProductTagId> {
    List<ProductTag> findByProduct_Id(Integer productId);

    List<ProductTag> findByProduct_IdIn(java.util.Collection<Integer> productIds);

    void deleteByProduct_Id(Integer productId);
}
