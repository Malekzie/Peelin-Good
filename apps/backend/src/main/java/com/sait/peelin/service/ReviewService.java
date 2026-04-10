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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;
    private final CustomerLookupCacheService customerLookupCacheService;

    @Transactional(readOnly = true)
    @Cacheable(value = "reviews", key = "'product:' + #productId")
    public List<ReviewDto> forProduct(Integer productId) {
        return reviewRepository.findByProduct_IdAndReviewStatus(productId, ReviewStatus.approved).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> forBakery(Integer bakeryId) {
        return reviewRepository.findByBakery_Id(bakeryId).stream().map(this::toDto).toList();
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
        if (u.getUserRole() != UserRole.admin && u.getUserRole() != UserRole.employee) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (u.getUserRole() == UserRole.employee) {
            Employee employee = employeeRepository.findByUser_UserId(u.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee profile required"));
            return reviewRepository.findByReviewStatusAndBakery_IdOrderByReviewSubmittedDateDesc(
                            ReviewStatus.pending,
                            employee.getBakery().getId()
                    )
                    .stream()
                    .map(this::toDto)
                    .toList();
        }
        return reviewRepository.findByReviewStatusOrderByReviewSubmittedDateDesc(ReviewStatus.pending)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ReviewDto create(Integer productId, ReviewCreateRequest req) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.customer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Customer customer = customerLookupCacheService.findByUserId(u.getUserId());
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer profile required");
        }
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Review r = new Review();
        r.setCustomer(customer);
        r.setProduct(product);
        r.setReviewRating(req.getRating());
        r.setReviewComment(req.getComment());
        r.setReviewSubmittedDate(OffsetDateTime.now());
        r.setReviewStatus(ReviewStatus.pending);
        if (req.getOrderId() != null) {
            Order order = orderRepository.findById(req.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            if (!order.getCustomer().getId().equals(customer.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to customer");
            }
            boolean hasProduct = orderItemRepository.findByOrder_Id(order.getId()).stream()
                    .anyMatch(oi -> oi.getProduct() != null && oi.getProduct().getId().equals(productId));
            if (!hasProduct) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order does not contain this product");
            }
            r.setOrder(order);
            r.setBakery(order.getBakery());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order id is required for reviews");
        }
        return toDto(reviewRepository.save(r));
    }

    @Transactional
    public ReviewDto createForOrder(ReviewCreateRequest req) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.customer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (req.getOrderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order id is required");
        }

        Customer customer = customerLookupCacheService.findByUserId(u.getUserId());
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer profile required");
        }

        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to customer");
        }

        List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order has no items");
        }

        Product product = items.get(0).getProduct();
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order item product required");
        }

        Review r = new Review();
        r.setCustomer(customer);
        r.setProduct(product);
        r.setOrder(order);
        r.setBakery(order.getBakery());
        r.setReviewRating(req.getRating());
        r.setReviewComment(req.getComment());
        r.setReviewSubmittedDate(OffsetDateTime.now());
        r.setReviewStatus(ReviewStatus.pending);

        return toDto(reviewRepository.save(r));
    }

    @Transactional
    @CacheEvict(value = "reviews", allEntries = true)
    public ReviewDto patchStatus(UUID reviewId, ReviewStatusPatchRequest req) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.admin && u.getUserRole() != UserRole.employee) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Review r = reviewRepository.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (u.getUserRole() == UserRole.employee) {
            Employee employee = employeeRepository.findByUser_UserId(u.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee profile required"));
            Integer employeeBakeryId = employee.getBakery() != null ? employee.getBakery().getId() : null;
            Integer reviewBakeryId = r.getBakery() != null ? r.getBakery().getId() : null;
            if (employeeBakeryId == null || reviewBakeryId == null || !employeeBakeryId.equals(reviewBakeryId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot moderate reviews from another bakery");
            }
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
        return toDto(reviewRepository.save(r));
    }

    private ReviewDto toDto(Review r) {
        Integer bakeryId = null;
        String bakeryName = null;
        if (r.getBakery() != null) {
            bakeryId = r.getBakery().getId();
            bakeryName = r.getBakery().getBakeryName();
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
                reviewerDisplayName(r.getCustomer())
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
}
