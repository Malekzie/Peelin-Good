package com.sait.peelin.dto.v1;

import java.time.OffsetDateTime;

public record BatchDto(
        Integer id,
        Integer bakeryId,
        Integer productId,
        OffsetDateTime productionDate,
        OffsetDateTime expiryDate,
        Integer quantityProduced
) {}
