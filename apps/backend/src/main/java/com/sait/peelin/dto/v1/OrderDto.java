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
        /** True if this customer already submitted a location/service review for this order (any status). */
        boolean locationReviewSubmitted,
        List<OrderItemDto> items
) {}
