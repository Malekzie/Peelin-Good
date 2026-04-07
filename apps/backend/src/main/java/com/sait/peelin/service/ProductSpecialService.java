package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ProductSpecialTodayDto;
import com.sait.peelin.repository.ProductSpecialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProductSpecialService {

    private final ProductSpecialRepository productSpecialRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "product-specials-v2", key = "#date")
    public ProductSpecialTodayDto findFirstForDate(LocalDate date) {
        return productSpecialRepository.findFirstByFeaturedOnOrderByProductSpecialIdAsc(date)
                .map(ps -> new ProductSpecialTodayDto(ps.getProductId(), ps.getDiscountPercent()))
                .orElse(new ProductSpecialTodayDto(null, null));
    }
}
