package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.ProductRecommendationDto;
import com.sait.peelin.model.Customer;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.service.CurrentUserService;
import com.sait.peelin.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final CurrentUserService currentUserService;
    private final CustomerRepository customerRepository;

    @GetMapping
    public List<ProductRecommendationDto> getRecommendations() {
        try {
            var user = currentUserService.requireUser();
            Customer customer = customerRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer profile required"));
            return recommendationService.listRecommendationsForCustomer(customer.getId());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return List.of();
        }
    }
}
