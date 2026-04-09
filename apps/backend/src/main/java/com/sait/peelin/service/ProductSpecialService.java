package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ProductSpecialDto;
import com.sait.peelin.dto.v1.ProductSpecialTodayDto;
import com.sait.peelin.model.Product;
import com.sait.peelin.repository.ProductRepository;
import com.sait.peelin.repository.ProductSpecialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSpecialService {

    private final ProductSpecialRepository productSpecialRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "product-specials-v2", key = "#date")
    public ProductSpecialTodayDto findFirstForDate(LocalDate date) {
        return productSpecialRepository.findFirstByFeaturedOnOrderByProductSpecialIdAsc(date)
                .map(ps -> new ProductSpecialTodayDto(ps.getProductId(), ps.getDiscountPercent()))
                .orElse(new ProductSpecialTodayDto(null, null));
    }

    public List<ProductSpecialDto> findAllSpecials() {
        return productSpecialRepository.findAll().stream()
                .map(s -> {
                    Product p = productRepository.findById(s.getProductId())
                            .orElseThrow(() -> new RuntimeException("Product not found: " + s.getProductId()));
                    return new ProductSpecialDto(
                            s.getProductSpecialId(),
                            s.getFeaturedOn(),
                            s.getDiscountPercent(),
                            p.getId(),
                            p.getProductName(),
                            p.getProductDescription(),
                            p.getProductBasePrice(),
                            p.getProductImageUrl()
                    );
                })
                .toList();
    }
}
