package com.sait.peelin.dto.v1;

import com.sait.peelin.model.BakeryStatus;

import java.math.BigDecimal;

public record BakeryDto(
        Integer id,
        String name,
        String phone,
        String email,
        BakeryStatus status,
        BigDecimal latitude,
        BigDecimal longitude,
        AddressDto address
) {}
