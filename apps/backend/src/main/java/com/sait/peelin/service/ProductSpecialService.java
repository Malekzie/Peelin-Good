package com.sait.peelin.service;

import com.sait.peelin.model.ProductSpecial;
import com.sait.peelin.repository.ProductSpecialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductSpecialService {

    private final ProductSpecialRepository productSpecialRepository;

    @Transactional(readOnly = true)
    public Optional<ProductSpecial> findFirstForDate(LocalDate date) {
        return productSpecialRepository.findFirstByFeaturedOnOrderByProductSpecialIdAsc(date);
    }
}
