package com.sait.peelin.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewModerationService {
    private final ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;

    @PostConstruct
    void init() {
        this.chatClient = chatClientBuilder.defaultSystem("""
                You are a content moderation assistant.
                Respond ONLY with valid JSON, no markdown, no explanation.
                """).build();
    }

    public ModerationResult moderateReview(String reviewText) {
        String response = chatClient.prompt()
                .user("""
                        Analyze this product review for harmful content.
                        Return JSON in exactly this format: 
                        {"approved": true/false, "reason": "brief reason or null if approved"}
                        
                        Flag if it contains: slurs, hate speech, gibberish/spam,
                        personal attacks, or completely off-topic content.
                        
                        Review: "%s"
                        """.formatted(reviewText))
                .call()
                .content();

        return parseModeration(response);
    }

    private ModerationResult parseModeration(String json) {
        try {
            json = json.replaceAll("```json|```", "").trim();
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            boolean approved = obj.get("approved").getAsBoolean();
            String reason = obj.has("reason") && !obj.get("reason").isJsonNull() ? obj.get("reason").getAsString() : null;

            return new ModerationResult(approved, reason);
        } catch (Exception e) {
            return new ModerationResult(false, "Moderation check failed, manual review required.");
        }
    }

    public record ModerationResult(boolean approved, String reason) {}
}
