package com.sait.peelin.service;

import com.sait.peelin.dto.v1.OrderDto;
import com.sait.peelin.dto.v1.OrderItemDto;
import com.sait.peelin.model.Customer;
import com.sait.peelin.model.Order;
import com.sait.peelin.model.OrderItem;
import com.sait.peelin.model.OrderMethod;
import com.sait.peelin.model.ReviewStatus;
import com.sait.peelin.repository.OrderItemRepository;
import com.sait.peelin.repository.ReviewRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class OrderMapper {

    private static final BigDecimal DELIVERY_FEE = new BigDecimal("7.00");
    private static final BigDecimal DELIVERY_FREE_THRESHOLD = new BigDecimal("50.00");

    private OrderMapper() {}

    public static OrderDto toDto(Order o, OrderItemRepository orderItemRepository) {
        return toDto(o, orderItemRepository, null);
    }

    /**
     * When {@code reviewRepository} is non-null and the order has a customer, line items include
     * {@link OrderItemDto#productReviewSubmitted} and the order includes {@link OrderDto#locationReviewSubmitted}.
     */
    public static OrderDto toDto(Order o, OrderItemRepository orderItemRepository, ReviewRepository reviewRepository) {
        List<OrderItem> items = orderItemRepository.findByOrder_Id(o.getId());
        UUID customerId = o.getCustomer() != null ? o.getCustomer().getId() : null;
        boolean locationReviewSubmitted = false;
        if (customerId != null && reviewRepository != null) {
            locationReviewSubmitted = reviewRepository.existsByOrder_IdAndCustomer_IdAndProductIsNullAndReviewStatusIn(
                    o.getId(), customerId, List.of(ReviewStatus.approved, ReviewStatus.pending));
        }
        List<OrderItemDto> itemDtos = items.stream()
                .map(i -> itemDto(i, customerId, reviewRepository))
                .toList();
        BigDecimal subtotal = o.getOrderTotal();
        BigDecimal taxAmount = o.getOrderTaxAmount();

        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (OrderMethod.delivery.equals(o.getOrderMethod())
                && subtotal != null
                && subtotal.compareTo(DELIVERY_FREE_THRESHOLD) < 0) {
            deliveryFee = DELIVERY_FEE;
        }

        BigDecimal grandTotal = subtotal;
        if (subtotal != null && taxAmount != null) {
            grandTotal = subtotal.add(taxAmount).add(deliveryFee);
        }

        return new OrderDto(
                o.getId(),
                o.getOrderNumber(),
                o.getCustomer() != null ? o.getCustomer().getId() : null,
                buildCustomerName(o.getCustomer()),
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
                locationReviewSubmitted,
                o.getOrderSpecialDiscountAmount(),
                o.getOrderTierDiscountAmount(),
                o.getOrderEmployeeDiscountAmount(),
                deliveryFee,
                itemDtos
        );
    }

    private static String buildCustomerName(Customer c) {
        if (c == null) {
            return null;
        }
        String first = c.getCustomerFirstName() != null ? c.getCustomerFirstName() : "";
        String last = c.getCustomerLastName() != null ? c.getCustomerLastName() : "";
        String name = (first + " " + last).trim();
        if (!name.isEmpty()) {
            return name;
        }
        return c.getCustomerEmail();
    }

    private static OrderItemDto itemDto(OrderItem i, UUID customerId, ReviewRepository reviewRepository) {
        boolean productReviewSubmitted = false;
        Integer productId = i.getProduct() != null ? i.getProduct().getId() : null;
        UUID orderId = i.getOrder() != null ? i.getOrder().getId() : null;
        if (customerId != null && productId != null && reviewRepository != null) {
            if (orderId != null) {
                productReviewSubmitted = reviewRepository.existsByCustomer_IdAndProduct_IdAndOrder_IdAndReviewStatusIn(
                        customerId, productId, orderId, List.of(ReviewStatus.approved, ReviewStatus.pending));
            } else {
                productReviewSubmitted = reviewRepository.existsByCustomer_IdAndProduct_IdAndOrderIsNull(customerId, productId);
            }
        }
        return new OrderItemDto(
                i.getId(),
                productId,
                i.getProduct() != null ? i.getProduct().getProductName() : null,
                i.getProduct() != null ? i.getProduct().getProductImageUrl() : null,
                i.getBatch() != null ? i.getBatch().getId() : null,
                i.getOrderItemQuantity(),
                i.getOrderItemUnitPriceAtTime(),
                i.getOrderItemLineTotal(),
                productReviewSubmitted
        );
    }
}
