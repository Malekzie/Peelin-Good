package com.sait.peelin.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.sait.peelin.model.Customer;
import com.sait.peelin.model.OrderItem;
import com.sait.peelin.repository.CustomerPreferenceRepository;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.OrderItemRepository;
import com.sait.peelin.repository.OrderRepository;
import com.sait.peelin.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI product-name list only — cached in Redis as {@code List<String>} (same shape as pre-2026 refactors).
 * Name→id resolution stays uncached in {@link RecommendationService}.
 */
@Service
@RequiredArgsConstructor
public class RecommendationAiCacheService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationAiCacheService.class);

    private final ChatClient.Builder chatClientBuilder;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerPreferenceRepository customerPreferenceRepository;
    private final ProductRepository productRepository;

    private ChatClient chatClient;

    @PostConstruct
    void init() {
        this.chatClient = chatClientBuilder
                .defaultSystem("You are a product recommendation engine. Respond ONLY with valid JSON, no markdown")
                .build();
    }

    @Cacheable(value = "recommendations", key = "#customerId")
    public List<String> cachedAiProductNames(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer profile required"));

        Map<String, String> preferences = customerPreferenceRepository
                .findByCustomer_IdWithTag(customer.getId())
                .stream()
                .collect(Collectors.toMap(
                        pref -> pref.getTag().getTagName(),
                        pref -> pref.getPreferenceType().name()
                                + (pref.getPreferenceStrength() != null
                                ? " (strength: " + pref.getPreferenceStrength() + ")"
                                : ""),
                        (a, b) -> a
                ));

        if (preferences.isEmpty()) {
            return List.of();
        }

        List<String> orderedProducts = orderRepository
                .findByCustomer_IdOrderByOrderPlacedDatetimeDesc(customer.getId())
                .stream()
                .flatMap(order -> orderItemRepository.findByOrder_IdWithProduct(order.getId()).stream())
                .map(OrderItem::getProduct)
                .filter(p -> p != null && p.getProductName() != null)
                .map(p -> p.getProductName())
                .distinct()
                .limit(20)
                .collect(Collectors.toList());

        List<String> availableProducts = productRepository.findAll()
                .stream()
                .map(p -> p.getProductName())
                .filter(name -> name != null)
                .toList();

        try {
            return generateRecommendedProductNames(orderedProducts, preferences, availableProducts);
        } catch (Exception e) {
            log.warn("AI recommendations call failed for customer {}: {}", customerId, e.toString());
            return List.of();
        }
    }

    private List<String> generateRecommendedProductNames(
            List<String> previouslyOrderedProducts,
            Map<String, String> preferences,
            List<String> availableProducts) {
        String response = chatClient.prompt()
                .user("""
                        A customer has the following order history and preferences.
                        Recommend up to 5 products from the AVAILABLE PRODUCTS list only.
                        Do NOT suggest anything outside this list.
                        Return ONLY a JSON array of strings using exact names from the list.

                        Available products: %s
                        Order history: %s
                        Preferences: %s
                        """.formatted(availableProducts, previouslyOrderedProducts, preferences))
                .call()
                .content();

        return parseRecommendations(response);
    }

    private List<String> parseRecommendations(String json) {
        try {
            json = json.replaceAll("```json|```", "").trim();
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            List<String> result = new ArrayList<>();

            arr.forEach(el -> result.add(el.getAsString()));
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }
}
