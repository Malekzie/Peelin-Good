package com.sait.peelin.dto.v1;

import java.math.BigDecimal;
import java.util.List;

public record ProductDto(
        Integer id,
        String name,
        String description,
        BigDecimal basePrice,
        String imageUrl,
        List<Integer> tagIds
) {}
