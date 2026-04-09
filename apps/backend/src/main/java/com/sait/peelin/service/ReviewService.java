package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ReviewCreateRequest;
import com.sait.peelin.dto.v1.ReviewDto;
import com.sait.peelin.dto.v1.ReviewStatusPatchRequest;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.*;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.EmployeeRepository;
import com.sait.peelin.repository.OrderRepository;
import com.sait.peelin.repository.OrderItemRepository;
import com.sait.peelin.repository.ProductRepository;
import com.sait.peelin.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    /** Short text for mobile/web toasts; full reason stays in {@code moderation_rejection_reason}. */
    private static final int MODERATION_MESSAGE_CLIENT_MAX_LEN = 100;

    private static final List<ReviewStatus> BLOCKING_REVIEW_STATUSES =
            List.of(ReviewStatus.approved, ReviewStatus.pending, ReviewStatus.rejected);

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;
    private final ReviewModerationService reviewModerationService;

    @Transactional(readOnly = true)
    @Cacheable(value = "reviews", key = "'product:' + #productId")
    public List<ReviewDto> forProduct(Integer productId) {
        return reviewRepository.findByProduct_IdAndReviewStatusAndOrderIsNull(productId, ReviewStatus.approved)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> forBakery(Integer bakeryId) {
        return reviewRepository
                .findByBakery_IdAndOrderIsNotNullAndReviewStatus(bakeryId, ReviewStatus.approved)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "reviews", key = "'product-avg:' + #productId")
    public Double averageForProduct(Integer productId) {
        return reviewRepository.averageRatingForProduct(productId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Double averageForBakery(Integer bakeryId) {
        return reviewRepository.averageRatingForBakery(bakeryId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> pending() {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return reviewRepository.findByReviewStatusOrderByReviewSubmittedDateDesc(ReviewStatus.pending)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "reviews", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true)
    })
    public ReviewDto create(Integer productId, ReviewCreateRequest req) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.customer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Customer customer = customerRepository.findByUser_UserId(u.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer profile required"));
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!orderItemRepository.existsPurchasedByCustomer(customer.getId(), productId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can only review products you have purchased");
        }
        if (reviewRepository.existsByCustomer_IdAndProduct_IdAndOrderIsNullAndReviewStatusIn(
                customer.getId(), productId, BLOCKING_REVIEW_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already submitted a review for this product");
        }

        Bakery bakery = resolveBakeryForProductReview(customer.getId(), productId);
        String comment = req.getComment();
        if (StringUtils.hasText(comment)) {
            var mod = reviewModerationService.moderateReview(comment, ReviewModerationService.ModerationKind.PRODUCT);
            if (!mod.approved()) {
                return persistModerationRejectedReview(
                        customer, product, bakery, null, req, moderationReasonOrDefault(mod.reason()));
            }
        }

        Review r = new Review();
        r.setCustomer(customer);
        r.setProduct(product);
        r.setReviewRating(req.getRating());
        r.setReviewComment(req.getComment());
        r.setReviewSubmittedDate(OffsetDateTime.now());
        r.setOrder(null);
        r.setBakery(bakery);
        r.setReviewStatus(ReviewStatus.approved);
        r.setReviewApprovalDate(OffsetDateTime.now());

        Review saved = saveReviewOrConflict(r, "You already submitted a review for this product");
        return toDto(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "reviews", allEntries = true),
            @CacheEvict(value = {"orders", "analytics", "dashboard"}, allEntries = true)
    })
    public ReviewDto createForOrder(ReviewCreateRequest req) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.customer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (req.getOrderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order id is required");
        }

        Customer customer = customerRepository.findByUser_UserId(u.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer profile required"));

        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to customer");
        }
        if (reviewRepository.existsByOrder_IdAndCustomer_IdAndReviewStatusIn(
                order.getId(), customer.getId(), BLOCKING_REVIEW_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already submitted a location review for this order");
        }
        if (!hasFullName(customer)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "First and last name are required in your profile before leaving a location review.");
        }

        List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order has no items");
        }

        Product product = items.get(0).getProduct();
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order item product required");
        }

        String comment = req.getComment();
        if (StringUtils.hasText(comment)) {
            var mod = reviewModerationService.moderateReview(comment, ReviewModerationService.ModerationKind.BAKERY_SERVICE);
            if (!mod.approved()) {
                return persistModerationRejectedReview(
                        customer, product, order.getBakery(), order, req, moderationReasonOrDefault(mod.reason()));
            }
        }

        Review r = new Review();
        r.setCustomer(customer);
        r.setProduct(product);
        r.setOrder(order);
        r.setBakery(order.getBakery());
        r.setReviewRating(req.getRating());
        r.setReviewComment(req.getComment());
        r.setReviewSubmittedDate(OffsetDateTime.now());
        r.setReviewStatus(ReviewStatus.approved);
        r.setReviewApprovalDate(OffsetDateTime.now());

        Review saved = saveReviewOrConflict(r, "You already submitted a location review for this order");
        completeDeliveredOrderAfterLocationReview(saved.getOrder());
        return toDto(saved);
    }

    private static String moderationReasonOrDefault(String reason) {
        return StringUtils.hasText(reason) ? reason.trim() : "This review does not meet our posting guidelines.";
    }

    /**
     * AI rejection is stored so each customer/order gets only one review attempt.
     * The rejection reason is persisted in {@code moderation_rejection_reason}.
     */
    private ReviewDto persistModerationRejectedReview(
            Customer customer,
            Product product,
            Bakery bakery,
            Order orderOrNull,
            ReviewCreateRequest req,
            String moderationReason) {
        Review rejected = new Review();
        rejected.setCustomer(customer);
        rejected.setProduct(product);
        rejected.setBakery(bakery);
        rejected.setOrder(orderOrNull);
        rejected.setReviewRating(req.getRating());
        rejected.setReviewComment(req.getComment());
        rejected.setReviewSubmittedDate(OffsetDateTime.now());
        rejected.setReviewStatus(ReviewStatus.rejected);
        rejected.setModerationRejectionReason(moderationReason);
        String conflictMessage = orderOrNull != null
                ? "You already submitted a location review for this order"
                : "You already submitted a review for this product";
        Review saved = saveReviewOrConflict(rejected, conflictMessage);
        if (orderOrNull != null) {
            completeDeliveredOrderAfterLocationReview(saved.getOrder());
        }
        return toDto(saved);
    }

    private Review saveReviewOrConflict(Review review, String message) {
        try {
            return reviewRepository.save(review);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    /**
     * After a location/service review is submitted (approved or rejected), move delivered orders to completed
     * so the customer cannot keep revisiting the review flow for this order.
     */
    private void completeDeliveredOrderAfterLocationReview(Order order) {
        if (order == null) {
            return;
        }
        Order fresh = orderRepository.findById(order.getId()).orElse(null);
        if (fresh == null) {
            return;
        }
        OrderStatus s = fresh.getOrderStatus();
        if (s == OrderStatus.delivered || s == OrderStatus.picked_up) {
            fresh.setOrderStatus(OrderStatus.completed);
            orderRepository.save(fresh);
        }
    }

    @Transactional
    @CacheEvict(value = "reviews", allEntries = true)
    public Optional<ReviewDto> patchStatus(UUID reviewId, ReviewStatusPatchRequest req) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Review r = reviewRepository.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (req.getStatus() == ReviewStatus.rejected) {
            reviewRepository.delete(r);
            return Optional.empty();
        }
        r.setReviewStatus(req.getStatus());
        if (req.getStatus() == ReviewStatus.approved) {
            r.setReviewApprovalDate(OffsetDateTime.now());
            Employee employee = employeeRepository.findByUser_UserId(u.getUserId()).orElse(null);
            r.setEmployee(employee);
            if (r.getOrder() != null) {
                OrderStatus status = r.getOrder().getOrderStatus();
                if (status == OrderStatus.delivered || status == OrderStatus.picked_up) {
                    r.getOrder().setOrderStatus(OrderStatus.completed);
                }
            }
        }
        return Optional.of(toDto(reviewRepository.save(r)));
    }

    private ReviewDto toDto(Review r) {
        Integer bakeryId = null;
        String bakeryName = null;
        if (r.getBakery() != null) {
            bakeryId = r.getBakery().getId();
            bakeryName = r.getBakery().getBakeryName();
        }
        String moderationMessage = null;
        if (r.getReviewStatus() == ReviewStatus.rejected
                && StringUtils.hasText(r.getModerationRejectionReason())) {
            moderationMessage = shortenForClientMessage(r.getModerationRejectionReason().trim());
        }

        return new ReviewDto(
                r.getId(),
                r.getCustomer().getId(),
                r.getOrder() != null ? r.getOrder().getId() : null,
                bakeryId,
                bakeryName,
                r.getProduct().getId(),
                r.getEmployee() != null ? r.getEmployee().getId() : null,
                r.getReviewRating(),
                r.getReviewComment(),
                r.getReviewStatus(),
                r.getReviewSubmittedDate(),
                r.getReviewApprovalDate(),
                reviewerDisplayName(r.getCustomer()),
                moderationMessage
        );
    }

    /**
     * First name plus last-name initial for public display (e.g. {@code James R.}).
     */
    static String reviewerDisplayName(Customer c) {
        if (c == null) {
            return "Customer";
        }
        String first = trimName(c.getCustomerFirstName());
        String last = trimName(c.getCustomerLastName());
        if (first.isEmpty()) {
            first = "Customer";
        }
        if (last.isEmpty()) {
            return first;
        }
        char initial = Character.toUpperCase(last.charAt(0));
        return first + " " + initial + ".";
    }

    private static String trimName(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> topReviews(int limit) {
        return reviewRepository
                .findByReviewStatusOrderByReviewRatingDescReviewSubmittedDateDesc(ReviewStatus.approved)
                .stream()
                .limit(limit)
                .map(this::toDto)
                .toList();
    }

    private Bakery resolveBakeryForProductReview(UUID customerId, Integer productId) {
        List<Order> purchasedOrders = orderRepository.findByCustomer_IdOrderByOrderPlacedDatetimeDesc(customerId);
        for (Order order : purchasedOrders) {
            boolean hasProduct = orderItemRepository.findByOrder_Id(order.getId()).stream()
                    .anyMatch(oi -> oi.getProduct() != null && oi.getProduct().getId().equals(productId));
            if (hasProduct && order.getBakery() != null) {
                return order.getBakery();
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No purchased bakery found for this product");
    }

    private static boolean hasFullName(Customer customer) {
        return StringUtils.hasText(customer.getCustomerFirstName())
                && StringUtils.hasText(customer.getCustomerLastName());
    }

    private static String shortenForClientMessage(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String t = text.trim();
        if (t.length() <= MODERATION_MESSAGE_CLIENT_MAX_LEN) {
            return t;
        }
        return t.substring(0, MODERATION_MESSAGE_CLIENT_MAX_LEN - 1).trim() + "…";
    }
}
