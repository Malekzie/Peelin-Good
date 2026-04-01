package com.sait.peelin.service;

import com.sait.peelin.dto.v1.DashboardSummaryDto;
import com.sait.peelin.dto.v1.OrderDto;
import com.sait.peelin.model.Order;
import com.sait.peelin.model.OrderStatus;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.OrderItemRepository;
import com.sait.peelin.repository.OrderRepository;
import com.sait.peelin.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryDto summary() {
        BigDecimal revenue = orderRepository.sumRevenueForStatuses(OrderStatus.completed, OrderStatus.delivered);
        long orders = orderRepository.countByOrderStatusIn(OrderStatus.completed, OrderStatus.delivered);
        long customers = customerRepository.count();
        long products = productRepository.count();
        List<Order> recent = orderRepository.findAll(
                PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "orderPlacedDatetime"))
        ).getContent();
        List<OrderDto> recentDtos = recent.stream().map(o -> OrderMapper.toDto(o, orderItemRepository)).toList();
        return new DashboardSummaryDto(revenue, orders, customers, products, recentDtos);
    }
}
