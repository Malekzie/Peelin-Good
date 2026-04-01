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
        Integer bakeryId,
        String bakeryName,
        Integer addressId,
        OrderMethod orderMethod,
        OrderStatus status,
        BigDecimal orderTotal,
        BigDecimal orderDiscount,
        OffsetDateTime placedAt,
        OffsetDateTime scheduledAt,
        OffsetDateTime deliveredAt,
        String comment,
        List<OrderItemDto> items
) {}
