package com.sait.peelin.service;

import com.sait.peelin.dto.v1.OrderDto;
import com.sait.peelin.dto.v1.OrderItemDto;
import com.sait.peelin.model.Order;
import com.sait.peelin.model.OrderItem;
import com.sait.peelin.repository.OrderItemRepository;

import java.math.BigDecimal;
import java.util.List;

public final class OrderMapper {

    private OrderMapper() {}

    public static OrderDto toDto(Order o, OrderItemRepository orderItemRepository) {
        List<OrderItem> items = orderItemRepository.findByOrder_Id(o.getId());
        List<OrderItemDto> itemDtos = items.stream().map(OrderMapper::itemDto).toList();
        BigDecimal subtotal = o.getOrderTotal();
        BigDecimal taxAmount = o.getOrderTaxAmount();
        BigDecimal grandTotal = subtotal;
        if (subtotal != null && taxAmount != null) {
            grandTotal = subtotal.add(taxAmount);
        }
        return new OrderDto(
                o.getId(),
                o.getOrderNumber(),
                o.getCustomer() != null ? o.getCustomer().getId() : null,
                o.getBakery().getId(),
                o.getBakery().getBakeryName(),
                o.getAddress() != null ? o.getAddress().getId() : null,
                o.getOrderMethod(),
                o.getOrderStatus(),
                o.getOrderTotal(),
                o.getOrderDiscount(),
                o.getOrderTaxRate(),
                o.getOrderTaxAmount(),
                grandTotal,
                o.getOrderPlacedDatetime(),
                o.getOrderScheduledDatetime(),
                o.getOrderDeliveredDatetime(),
                o.getOrderComment(),
                itemDtos
        );
    }

    private static OrderItemDto itemDto(OrderItem i) {
        return new OrderItemDto(
                i.getId(),
                i.getProduct().getId(),
                i.getProduct().getProductName(),
                i.getBatch() != null ? i.getBatch().getId() : null,
                i.getOrderItemQuantity(),
                i.getOrderItemUnitPriceAtTime(),
                i.getOrderItemLineTotal()
        );
    }
}
