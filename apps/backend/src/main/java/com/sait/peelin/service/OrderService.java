package com.sait.peelin.service;

import com.sait.peelin.dto.v1.*;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.*;
import com.sait.peelin.repository.*;
import com.sait.peelin.support.PhoneNumberFormatter;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

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
    private final TaxRateRepository taxRateRepository;
    private final CustomerService customerService;
    private final CurrentUserService currentUserService;
    private final StripeService stripeService;
    private final StripePaymentFulfillmentService stripePaymentFulfillmentService;
    private final RewardAccrualService rewardAccrualService;
    private final RewardTierService rewardTierService;
    private final RecommendationService recommendationService;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "orders", keyGenerator = "userIdKeyGenerator")
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
    @Cacheable(value = "orders", key = "'order:' + #orderId + ':' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public OrderDto get(UUID orderId) {
        Order o = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        assertCanView(o);
        return toDto(o);
    }

    @Transactional
    @CacheEvict(value = {"orders", "analytics", "dashboard"}, allEntries = true)
    public CheckoutSessionResponse checkout(CheckoutRequest req) {
        User user = currentUserService.currentUserOrNull();
        Customer customer;
        boolean guestCheckout = false;
        if (user == null) {
            if (req.getGuest() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication or guest details are required");
            }
            if (req.getCustomerId() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guest checkout cannot target an existing customer id");
            }
            customer = customerService.resolveOrCreateGuestCustomer(req.getGuest());
            guestCheckout = true;
        } else if (user.getUserRole() == UserRole.customer) {
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

        if (req.getOrderMethod() == OrderMethod.delivery
                && req.getAddressId() == null
                && customer.getAddress() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery requires address information");
        }
        Address address = null;
        if (req.getAddressId() != null) {
            address = addressRepository.findById(req.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        } else if (req.getOrderMethod() == OrderMethod.delivery) {
            address = customer.getAddress();
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
        } else if (!guestCheckout) {
            int pts = customer.getCustomerRewardBalance() != null ? customer.getCustomerRewardBalance() : 0;
            RewardTier tierForDiscount = rewardTierService.tierForBalance(pts).orElse(customer.getRewardTier());
            if (tierForDiscount != null && tierForDiscount.getRewardTierDiscountRate() != null) {
                discount = subtotal.multiply(tierForDiscount.getRewardTierDiscountRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        BigDecimal total = subtotal.subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        BigDecimal taxRatePercent = resolveTaxRatePercent(
                resolveTaxProvinceForCheckout(customer, address, bakery));
        BigDecimal taxAmount = total.multiply(taxRatePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = total.add(taxAmount);

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
        order.setOrderTaxRate(taxRatePercent);
        order.setOrderTaxAmount(taxAmount);
        if (guestCheckout && req.getGuest() != null) {
            order.setGuestName(buildGuestName(req.getGuest().getFirstName(), req.getGuest().getLastName()));
            String ge = req.getGuest().getEmail();
            if (ge != null && !ge.trim().isEmpty()) {
                order.setGuestEmail(ge.trim().toLowerCase());
            } else {
                order.setGuestEmail(null);
            }
            String gp = req.getGuest().getPhone();
            if (gp != null && !gp.trim().isEmpty()) {
                order.setGuestPhone(PhoneNumberFormatter.formatStoredPhone(gp));
            } else {
                order.setGuestPhone(null);
            }
        }
        order.setOrderStatus(OrderStatus.pending_payment);
        order = orderRepository.save(order);

        List<OrderItem> savedItems = new ArrayList<>();
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
            savedItems.add(orderItemRepository.save(oi));
        }

        Payment pay = new Payment();
        pay.setOrder(order);
        pay.setPaymentAmount(grandTotal);
        pay.setPaymentMethod(req.getPaymentMethod());
        pay.setPaymentStatus(PaymentStatus.pending);

        String clientSecret;
        String paymentIntentId;
        if (!stripeService.isConfigured()) {
            log.warn("Stripe is not configured; using dev payment placeholder for order {}", order.getId());
            paymentIntentId = "dev_pi_" + order.getId();
            clientSecret = paymentIntentId + "_secret_dev";
            pay.setStripeSessionId(paymentIntentId);
        } else {
            try {
                PaymentIntent intent = stripeService.createPaymentIntent(order.getId(), grandTotal);
                pay.setStripeSessionId(intent.getId());
                clientSecret = intent.getClientSecret();
                paymentIntentId = intent.getId();
            } catch (Exception e) {
                log.error("Failed to create Stripe payment intent for order {}", order.getId(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment provider unavailable");
            }
        }

        paymentRepository.save(pay);

        if (!guestCheckout && customer.getId() != null) {
            recommendationService.evictRecommendations(customer.getId());
        }

        return new CheckoutSessionResponse(order.getId(), order.getOrderNumber(), clientSecret, paymentIntentId);
    }

    /**
     * After Payment Sheet completes, verifies the PaymentIntent with Stripe and marks the order paid.
     * Use when webhooks are unavailable (e.g. local dev without {@code stripe listen}); production may still rely on webhooks.
     */
    @Transactional
    public OrderDto confirmStripePayment(UUID orderId, ConfirmStripePaymentRequest req) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User u = currentUserService.currentUserOrNull();
        if (order.getCustomer() != null && order.getCustomer().getUser() != null
                && u != null && u.getUserRole() == UserRole.customer
                && !order.getCustomer().getUser().getUserId().equals(u.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        }

        Payment payment = paymentRepository.findByOrder_Id(orderId).stream()
                .filter(p -> req.paymentIntentId().equals(p.getStripeSessionId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment intent does not match this order"));

        if (payment.getPaymentStatus() == PaymentStatus.completed) {
            return toDto(orderRepository.findById(orderId).orElseThrow());
        }

        if (!stripeService.isConfigured()) {
            if (req.paymentIntentId().startsWith("dev_pi_")) {
                stripePaymentFulfillmentService.fulfillOrderByPaymentIntentId(req.paymentIntentId());
                return toDto(orderRepository.findById(orderId).orElseThrow());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe is not configured");
        }

        try {
            PaymentIntent pi = stripeService.retrievePaymentIntent(req.paymentIntentId());
            if (!"succeeded".equals(pi.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment not completed");
            }
            String metaOrderId = pi.getMetadata() != null ? pi.getMetadata().get("orderId") : null;
            if (metaOrderId == null || !orderId.toString().equals(metaOrderId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment does not match order");
            }
        } catch (StripeException e) {
            log.error("Stripe retrieve failed for order {}", orderId, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to verify payment with provider");
        }

        stripePaymentFulfillmentService.fulfillOrderByPaymentIntentId(req.paymentIntentId());
        return toDto(orderRepository.findById(orderId).orElseThrow());
    }

    /**
     * Returns a PaymentIntent client secret to resume the sheet for {@link OrderStatus#pending_payment},
     * or fulfills immediately if Stripe already shows the intent succeeded.
     */
    @Transactional
    public ResumePaymentSessionResponse resumeStripePayment(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        assertCanView(order);

        if (order.getOrderStatus() != OrderStatus.pending_payment) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not awaiting payment");
        }

        Payment payment = paymentRepository.findByOrder_Id(orderId).stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.pending)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending payment for this order"));

        String existingPi = payment.getStripeSessionId();
        if (existingPi == null || existingPi.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment session missing");
        }

        if (!stripeService.isConfigured()) {
            if (existingPi.startsWith("dev_pi_")) {
                String clientSecret = existingPi + "_secret_dev";
                return new ResumePaymentSessionResponse(order.getId(), order.getOrderNumber(), clientSecret, existingPi, false);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe is not configured");
        }

        try {
            PaymentIntent intent = stripeService.retrievePaymentIntent(existingPi);
            String status = intent.getStatus();

            if ("succeeded".equals(status)) {
                stripePaymentFulfillmentService.fulfillOrderByPaymentIntentId(existingPi);
                Order refreshed = orderRepository.findById(orderId).orElseThrow();
                return new ResumePaymentSessionResponse(refreshed.getId(), refreshed.getOrderNumber(), null, existingPi, true);
            }

            if ("canceled".equals(status)) {
                PaymentIntent created = stripeService.createPaymentIntent(order.getId(), payment.getPaymentAmount());
                payment.setStripeSessionId(created.getId());
                paymentRepository.save(payment);
                return new ResumePaymentSessionResponse(
                        order.getId(),
                        order.getOrderNumber(),
                        created.getClientSecret(),
                        created.getId(),
                        false);
            }

            String clientSecret = intent.getClientSecret();
            if (clientSecret == null || clientSecret.isBlank()) {
                PaymentIntent created = stripeService.createPaymentIntent(order.getId(), payment.getPaymentAmount());
                payment.setStripeSessionId(created.getId());
                paymentRepository.save(payment);
                return new ResumePaymentSessionResponse(
                        order.getId(),
                        order.getOrderNumber(),
                        created.getClientSecret(),
                        created.getId(),
                        false);
            }

            return new ResumePaymentSessionResponse(
                    order.getId(),
                    order.getOrderNumber(),
                    clientSecret,
                    existingPi,
                    false);
        } catch (StripeException e) {
            log.warn("Stripe retrieve failed for resume on order {}, creating new PaymentIntent", orderId, e);
            try {
                PaymentIntent created = stripeService.createPaymentIntent(order.getId(), payment.getPaymentAmount());
                payment.setStripeSessionId(created.getId());
                paymentRepository.save(payment);
                return new ResumePaymentSessionResponse(
                        order.getId(),
                        order.getOrderNumber(),
                        created.getClientSecret(),
                        created.getId(),
                        false);
            } catch (StripeException e2) {
                log.error("Could not create replacement PaymentIntent for order {}", orderId, e2);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment provider unavailable");
            }
        }
    }

    /**
     * Prefer the order/delivery address, then the customer profile, then the bakery (e.g. guest pickup without profile address).
     */
    private String resolveTaxProvinceForCheckout(Customer customer, Address orderAddress, Bakery bakery) {
        if (orderAddress != null && hasProvince(orderAddress)) {
            return orderAddress.getAddressProvince();
        }
        Address custAddr = customer.getAddress();
        if (custAddr != null && hasProvince(custAddr)) {
            return custAddr.getAddressProvince();
        }
        Address bakeryAddr = bakery.getAddress();
        if (bakeryAddr != null && hasProvince(bakeryAddr)) {
            return bakeryAddr.getAddressProvince();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Province is required for tax calculation");
    }

    private static boolean hasProvince(Address address) {
        String p = address.getAddressProvince();
        return p != null && !p.trim().isEmpty() && !p.trim().equalsIgnoreCase("unknown");
    }

    private static String buildGuestName(String firstName, String lastName) {
        return ((firstName != null ? firstName.trim() : "") + " " + (lastName != null ? lastName.trim() : "")).trim();
    }

    private BigDecimal resolveTaxRatePercent(String provinceRaw) {
        String normalizedProvince = normalizeProvince(provinceRaw);
        return taxRateRepository.findByProvinceNameIgnoreCase(normalizedProvince)
                .map(TaxRate::getTaxPercent)
                .orElseGet(() -> {
                    log.warn("No tax rate for province '{}'; falling back to Alberta rate", normalizedProvince);
                    return taxRateRepository.findByProvinceNameIgnoreCase("Alberta")
                            .map(TaxRate::getTaxPercent)
                            .orElse(BigDecimal.valueOf(5));
                });
    }

    private String normalizeProvince(String provinceRaw) {
        String province = provinceRaw == null ? "" : provinceRaw.trim();
        String upper = province.toUpperCase();
        return switch (upper) {
            case "AB" -> "Alberta";
            case "BC" -> "British Columbia";
            case "MB" -> "Manitoba";
            case "NB" -> "New Brunswick";
            case "NL", "NF" -> "Newfoundland and Labrador";
            case "NT" -> "Northwest Territories";
            case "NS" -> "Nova Scotia";
            case "NU" -> "Nunavut";
            case "ON" -> "Ontario";
            case "PE", "PEI" -> "Prince Edward Island";
            case "QC", "PQ" -> "Quebec";
            case "SK" -> "Saskatchewan";
            case "YT", "YK" -> "Yukon";
            default -> province;
        };
    }

    @Transactional
    @CacheEvict(value = {"orders", "analytics", "dashboard"}, allEntries = true)
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
        OrderStatus previous = o.getOrderStatus();
        o.setOrderStatus(req.getStatus());
        Order saved = orderRepository.save(o);
        if (req.getStatus() == OrderStatus.cancelled && previous != OrderStatus.cancelled) {
            rewardAccrualService.reverseEarnedPointsForOrder(saved);
        }
        return toDto(saved);
    }

    @Transactional
    @CacheEvict(value = {"orders", "analytics", "dashboard"}, allEntries = true)
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
        OrderStatus previousStatus = o.getOrderStatus();
        o.setOrderDeliveredDatetime(req.getDeliveredAt() != null ? req.getDeliveredAt() : OffsetDateTime.now());
        if (o.getOrderStatus() != OrderStatus.cancelled) {
            o.setOrderStatus(OrderStatus.completed);
        }
        Order saved = orderRepository.save(o);
        if (saved.getOrderStatus() == OrderStatus.completed && previousStatus != OrderStatus.completed) {
            awardRewardPoints(saved);
        }
        return toDto(saved);
    }

    @Transactional
    @CacheEvict(value = {"orders", "analytics", "dashboard"}, allEntries = true)
    public OrderDto acceptDelivery(UUID orderId) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.customer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Order o = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (o.getCustomer() == null || o.getCustomer().getUser() == null
                || !o.getCustomer().getUser().getUserId().equals(u.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (o.getOrderStatus() != OrderStatus.delivered && o.getOrderStatus() != OrderStatus.picked_up) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only delivered or picked_up orders can be accepted");
        }
        o.setOrderStatus(OrderStatus.completed);
        Order saved = orderRepository.save(o);
        awardRewardPoints(saved);
        return toDto(saved);
    }

    private void awardRewardPoints(Order order) {
        if (order.getCustomer() == null) return;
        if (rewardRepository.existsByOrder_Id(order.getId())) return;

        int points = order.getOrderTotal() != null ? order.getOrderTotal().intValue() : 0;
        if (points <= 0) return;

        Reward reward = new Reward();
        reward.setCustomer(order.getCustomer());
        reward.setOrder(order);
        reward.setRewardPointsEarned(points);
        reward.setRewardTransactionDate(OffsetDateTime.now());
        rewardRepository.save(reward);

        Customer customer = order.getCustomer();
        int newBalance = (customer.getCustomerRewardBalance() != null ? customer.getCustomerRewardBalance() : 0) + points;
        customer.setCustomerRewardBalance(newBalance);
        recalculateCustomerTier(customer);
        customerRepository.save(customer);
        log.info("Awarded {} points to customer {} for order {}", points, customer.getId(), order.getId());
    }

    private void recalculateCustomerTier(Customer customer) {
        int balance = customer.getCustomerRewardBalance() != null ? customer.getCustomerRewardBalance() : 0;
        rewardTierService.tierForBalance(balance).ifPresent(customer::setRewardTier);
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
        return OrderMapper.toDto(o, orderItemRepository, reviewRepository);
    }
}
