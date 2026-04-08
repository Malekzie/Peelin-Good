package com.sait.peelin.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;

    @PostConstruct
    void init() {
        this.chatClient = chatClientBuilder
                .defaultSystem("You are a product recommendation engine. Respond ONLY with valid JSON, no markdown")
                .build();
    }

    @Cacheable(value = "recommendations", key = "#userId")
    public List<String> getRecommendations(UUID userId, List<String> previouslyOrderedProducts, Map<String, String> preferences, List<String> availableProducts) {
        String response = chatClient.prompt()
                .user("""
                        A customer has the following order history and preferences.
                                                                                Recommend up to 5 products from the AVAILABLE PRODUCTS list only.
                                                                                Do NOT suggest anything outside this list.
                                                                                Return ONLY a JSON array of strings using exact names from the list.
                        
                                                                                Available products: %s
                                                                                Order history: %s
                                                                                Preferences: %s
                        """.formatted(availableProducts ,previouslyOrderedProducts, preferences))
                .call().content();

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

    @CacheEvict(value = "recommendations", key = "#userId")
    public void evictRecommendations(UUID userId) {}
}
