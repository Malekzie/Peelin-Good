package com.sait.peelin.dto.v1;

import com.sait.peelin.model.OrderMethod;
import com.sait.peelin.model.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDto(
        UUID id,
        String orderNumber,
        UUID customerId,
        String customerName,
        Integer bakeryId,
        String bakeryName,
        Integer addressId,
        OrderMethod orderMethod,
        OrderStatus status,
        BigDecimal orderTotal,
        BigDecimal orderDiscount,
        BigDecimal orderTaxRate,
        BigDecimal orderTaxAmount,
        BigDecimal orderGrandTotal,
        OffsetDateTime placedAt,
        OffsetDateTime scheduledAt,
        OffsetDateTime deliveredAt,
        String comment,
        /** True if this customer has a pending or approved location/service review for this order (rejected attempts may retry). */
        boolean locationReviewSubmitted,
        BigDecimal orderSpecialDiscountAmount,
        BigDecimal orderTierDiscountAmount,
        BigDecimal orderEmployeeDiscountAmount,
        BigDecimal deliveryFee,
        List<OrderItemDto> items
) {}
