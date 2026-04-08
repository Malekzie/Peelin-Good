package com.sait.peelin.controller.v1;

import com.sait.peelin.model.Customer;
import com.sait.peelin.model.OrderItem;
import com.sait.peelin.repository.*;
import com.sait.peelin.service.CurrentUserService;
import com.sait.peelin.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;
    private final CurrentUserService currentUserService;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerPreferenceRepository customerPreferenceRepository;
    private final ProductRepository productRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public List<String> getRecommendations() {
        try {
            var user = currentUserService.requireUser();

            Customer customer = customerRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer profile required"));

            List<String> orderedProducts = orderRepository
                    .findByCustomer_IdOrderByOrderPlacedDatetimeDesc(customer.getId())
                    .stream()
                    .flatMap(order -> orderItemRepository.findByOrder_Id(order.getId()).stream())
                    .map(OrderItem::getProduct)
                    .filter(p -> p != null && p.getProductName() != null)
                    .map(p -> p.getProductName())
                    .distinct()
                    .limit(20)
                    .collect(Collectors.toList());

            Map<String, String> preferences = customerPreferenceRepository
                    .findByCustomer_Id(customer.getId())
                    .stream()
                    .collect(Collectors.toMap(
                            pref -> pref.getTag().getTagName(),
                            pref -> pref.getPreferenceType().name()
                                    + (pref.getPreferenceStrength() != null
                                    ? " (strength: " + pref.getPreferenceStrength() + ")"
                                    : ""),
                            (a, b) -> a
                    ));

            List<String> availableProducts = productRepository.findAll()
                    .stream()
                    .map(p -> p.getProductName())
                    .filter(name -> name != null)
                    .toList();

            return recommendationService.getRecommendations(customer.getId(), orderedProducts, preferences, availableProducts);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return List.of();
        }
    }
}
