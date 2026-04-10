package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ProductRecommendationDto;
import com.sait.peelin.model.Customer;
import com.sait.peelin.repository.CustomerPreferenceRepository;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Resolves AI-suggested product names to DTOs. AI output is cached in {@link RecommendationAiCacheService}.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CustomerRepository customerRepository;
    private final CustomerPreferenceRepository customerPreferenceRepository;
    private final ProductRepository productRepository;
    private final RecommendationAiCacheService recommendationAiCacheService;

    public List<ProductRecommendationDto> listRecommendationsForCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer profile required"));

        boolean hasPrefs = !customerPreferenceRepository.findByCustomer_IdWithTag(customer.getId()).isEmpty();
        if (!hasPrefs) {
            return List.of();
        }

        List<String> names = recommendationAiCacheService.cachedAiProductNames(customerId);

        LinkedHashSet<Integer> seen = new LinkedHashSet<>();
        List<ProductRecommendationDto> out = new ArrayList<>();
        for (String raw : names) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            productRepository.findFirstByProductNameIgnoreCase(raw.trim()).ifPresent(p -> {
                if (seen.add(p.getId())) {
                    out.add(new ProductRecommendationDto(p.getId(), p.getProductName()));
                }
            });
            if (out.size() >= 5) {
                break;
            }
        }
        return out;
    }

    @CacheEvict(value = "recommendations", key = "#customerId")
    public void evictRecommendations(UUID customerId) {}
}
