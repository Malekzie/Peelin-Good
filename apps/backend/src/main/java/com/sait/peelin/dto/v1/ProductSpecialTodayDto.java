package com.sait.peelin.dto.v1;

import java.math.BigDecimal;

/**
 * Today's calendar special: {@code productId} is null when no row exists for the requested date.
 */
public record ProductSpecialTodayDto(Integer productId, BigDecimal discountPercent) {}
