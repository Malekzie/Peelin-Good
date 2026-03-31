package com.sait.peelin.service;

import com.sait.peelin.dto.v1.*;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.*;
import com.sait.peelin.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final RewardRepository rewardRepository;
    private final ProductRepository productRepository;
    private final BakeryRepository bakeryRepository;
    private final AddressRepository addressRepository;
    private final BatchRepository batchRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<OrderDto> listForCurrentUser() {
        User u = currentUserService.requireUser();
        return switch (u.getUserRole()) {
            case admin -> orderRepository.findAll().stream().map(this::toDto).toList();
            case customer -> orderRepository.findByCustomer_User_UserIdOrderByOrderPlacedDatetimeDesc(u.getUserId())
                    .stream().map(this::toDto).toList();
            case employee -> {
                Employee e = employeeRepository.findByUser_UserId(u.getUserId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
                yield orderRepository.findByBakery_IdOrderByOrderPlacedDatetimeDesc(e.getBakery().getId())
                        .stream().map(this::toDto).toList();
            }
        };
    }

    @Transactional(readOnly = true)
    public OrderDto get(UUID orderId) {
        Order o = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        assertCanView(o);
        return toDto(o);
    }

    @Transactional
    public OrderDto checkout(CheckoutRequest req) {
        User user = currentUserService.requireUser();
        Customer customer;
        if (user.getUserRole() == UserRole.customer) {
            customer = customerRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer profile not found"));
        } else if (user.getUserRole() == UserRole.admin || user.getUserRole() == UserRole.employee) {
            if (req.getCustomerId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerId is required for staff checkout");
            }
            customer = customerRepository.findById(req.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            if (user.getUserRole() == UserRole.employee) {
                Employee staff = employeeRepository.findByUser_UserId(user.getUserId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
                if (!staff.getBakery().getId().equals(req.getBakeryId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bakery does not match your assignment");
                }
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot place order with this role");
        }

        Bakery bakery = bakeryRepository.findById(req.getBakeryId())
                .orElseThrow(() -> new ResourceNotFoundException("Bakery not found"));

        if (req.getOrderMethod() == OrderMethod.delivery && req.getAddressId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery requires addressId");
        }
        Address address = null;
        if (req.getAddressId() != null) {
            address = addressRepository.findById(req.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CheckoutRequest.CheckoutLineRequest line : req.getItems()) {
            Product p = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.getProductId()));
            BigDecimal unit = p.getProductBasePrice();
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(line.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (req.getManualDiscount() != null) {
            discount = req.getManualDiscount().max(BigDecimal.ZERO);
        } else if (customer.getRewardTier().getRewardTierDiscountRate() != null) {
            discount = subtotal.multiply(customer.getRewardTier().getRewardTierDiscountRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        BigDecimal total = subtotal.subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        Order order = new Order();
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomer(customer);
        order.setBakery(bakery);
        order.setAddress(address);
        order.setOrderMethod(req.getOrderMethod());
        order.setOrderComment(req.getComment());
        order.setOrderScheduledDatetime(req.getScheduledAt());
        order.setOrderPlacedDatetime(OffsetDateTime.now());
        order.setOrderTotal(total);
        order.setOrderDiscount(discount);
        order.setOrderStatus(OrderStatus.completed);
        order = orderRepository.save(order);

        for (CheckoutRequest.CheckoutLineRequest line : req.getItems()) {
            Product p = productRepository.findById(line.getProductId()).orElseThrow();
            Batch batch = null;
            if (line.getBatchId() != null) {
                batch = batchRepository.findById(line.getBatchId())
                        .orElseThrow(() -> new ResourceNotFoundException("Batch not found"));
            }
            BigDecimal unit = p.getProductBasePrice();
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(line.getQuantity()));
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(p);
            oi.setBatch(batch);
            oi.setOrderItemQuantity(line.getQuantity());
            oi.setOrderItemUnitPriceAtTime(unit);
            oi.setOrderItemLineTotal(lineTotal);
            orderItemRepository.save(oi);
        }

        Payment pay = new Payment();
        pay.setOrder(order);
        pay.setPaymentAmount(total);
        pay.setPaymentMethod(req.getPaymentMethod());
        pay.setPaymentStatus(PaymentStatus.completed);
        pay.setPaymentPaidAt(OffsetDateTime.now());
        pay.setPaymentTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        paymentRepository.save(pay);

        int points = total.setScale(0, RoundingMode.DOWN).intValue();
        Reward reward = new Reward();
        reward.setCustomer(customer);
        reward.setOrder(order);
        reward.setRewardPointsEarned(Math.max(points, 1));
        reward.setRewardTransactionDate(OffsetDateTime.now());
        rewardRepository.save(reward);

        customer.setCustomerRewardBalance(customer.getCustomerRewardBalance() + reward.getRewardPointsEarned());
        customerRepository.save(customer);

        return toDto(orderRepository.findById(order.getId()).orElseThrow());
    }

    @Transactional
    public OrderDto updateStatus(UUID orderId, OrderStatusPatchRequest req) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.admin && u.getUserRole() != UserRole.employee) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Order o = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (u.getUserRole() == UserRole.employee) {
            Employee e = employeeRepository.findByUser_UserId(u.getUserId()).orElseThrow();
            if (!o.getBakery().getId().equals(e.getBakery().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        o.setOrderStatus(req.getStatus());
        return toDto(orderRepository.save(o));
    }

    @Transactional
    public OrderDto markDelivered(UUID orderId, OrderDeliveredPatchRequest req) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.admin && u.getUserRole() != UserRole.employee) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Order o = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (u.getUserRole() == UserRole.employee) {
            Employee e = employeeRepository.findByUser_UserId(u.getUserId()).orElseThrow();
            if (!o.getBakery().getId().equals(e.getBakery().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        o.setOrderDeliveredDatetime(req.getDeliveredAt() != null ? req.getDeliveredAt() : OffsetDateTime.now());
        if (o.getOrderStatus() != OrderStatus.cancelled) {
            o.setOrderStatus(OrderStatus.completed);
        }
        return toDto(orderRepository.save(o));
    }

    private void assertCanView(Order o) {
        User u = currentUserService.requireUser();
        switch (u.getUserRole()) {
            case admin -> { }
            case customer -> {
                if (o.getCustomer() == null || o.getCustomer().getUser() == null
                        || !o.getCustomer().getUser().getUserId().equals(u.getUserId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }
            case employee -> {
                Employee e = employeeRepository.findByUser_UserId(u.getUserId()).orElseThrow();
                if (!o.getBakery().getId().equals(e.getBakery().getId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }
        }
    }

    private OrderDto toDto(Order o) {
        return OrderMapper.toDto(o, orderItemRepository);
    }
}
