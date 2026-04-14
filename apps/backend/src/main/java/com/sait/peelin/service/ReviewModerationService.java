package com.sait.peelin.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewModerationService {

    /** When rejecting, the model must keep "reason" at most this many characters (concise for mobile UI). */
    public static final int MAX_REJECTION_REASON_CHARS = 80;

    /** Which review flow is being moderated (prompts differ for on-topic expectations). */
    public enum ModerationKind {
        PRODUCT,
        BAKERY_SERVICE
    }

    private final ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;

    @PostConstruct
    void init() {
        this.chatClient = chatClientBuilder.defaultSystem("""
                You validate user-generated reviews for a bakery app.
                Reply with a single JSON object only—no markdown fences, no prose before or after.
                """).build();
    }

    public ModerationResult moderateReview(String reviewText, ModerationKind kind) {
        try {
            String userPrompt = switch (kind) {
                case PRODUCT -> productModerationPrompt(reviewText);
                case BAKERY_SERVICE -> bakeryServiceModerationPrompt(reviewText);
            };
            String response = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();

            return parseModeration(response);
        } catch (Exception e) {
            return new ModerationResult(false,
                    "We couldn't verify your review right now. Please try again later.");
        }
    }

    private static String productModerationPrompt(String reviewText) {
        return """
                You are a content moderator for product-only customer reviews in a bakery marketplace.

                Decide whether this text should be published. Set "approved" to true only if the review is acceptable in substance and tone.

                Approve when the review stays on topic for the product: taste, texture, freshness, quality, portion size, price/value, or packaging of the baked item itself. Brief background is fine.

                Reject ("approved": false) when:
                - It contains slurs, hate speech, threats, graphic content, obvious spam/gibberish, harassment, or personal attacks; or
                - It is mostly about bakery service—staff attitude, pickup or delivery experience, wait times, store cleanliness, order mistakes, or location vibe—rather than the product. Short mentions of context are OK; reject when service dominates the message.
                - It is not written in English.

                Return JSON in exactly this format:
                {"approved": true/false, "reason": "if rejected: one concise phrase, at most %d characters, plain English; if approved: null"}

                Review: "%s"
                """.formatted(MAX_REJECTION_REASON_CHARS, escapeForPrompt(reviewText));
    }

    private static String bakeryServiceModerationPrompt(String reviewText) {
        return """
                You are a content moderator for bakery location and service feedback (pickup, delivery, or in-store).

                Decide whether this text should be published. Set "approved" to true only if the review is acceptable in substance and tone.

                Approve when the review stays on topic for service: staff helpfulness, communication, timeliness, how the order was handed off, packaging for transport, cleanliness, atmosphere, or overall experience at that bakery.

                Reject ("approved": false) when:
                - It contains slurs, hate speech, threats, obvious spam/gibberish, harassment, or personal attacks; or
                - It reads mainly like a product critique (deep flavor notes, comparing specific items, ingredient focus) without centering the service or location experience. Naming what you ordered in passing is OK; reject when product opinion clearly dominates.
                - It is not written in English.

                Return JSON in exactly this format:
                {"approved": true/false, "reason": "if rejected: one concise phrase, at most %d characters, plain English; if approved: null"}

                Review: "%s"
                """.formatted(MAX_REJECTION_REASON_CHARS, escapeForPrompt(reviewText));
    }

    private static String clampRejectionReason(String reason) {
        if (reason == null) {
            return null;
        }
        String t = reason.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() <= MAX_REJECTION_REASON_CHARS) {
            return t;
        }
        return t.substring(0, MAX_REJECTION_REASON_CHARS - 1).trim() + "…";
    }

    /** Avoid breaking the prompt string if the review contains quotes or newlines. */
    private static String escapeForPrompt(String reviewText) {
        if (reviewText == null) {
            return "";
        }
        return reviewText.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
    }

    private ModerationResult parseModeration(String json) {
        try {
            json = json.replaceAll("```json|```", "").trim();
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            boolean approved = obj.get("approved").getAsBoolean();
            String reason = obj.has("reason") && !obj.get("reason").isJsonNull() ? obj.get("reason").getAsString() : null;
            reason = clampRejectionReason(reason);

            return new ModerationResult(approved, reason);
        } catch (Exception e) {
            return new ModerationResult(false,
                    "We couldn't verify your review. Try different wording.");
        }
    }

    public record ModerationResult(boolean approved, String reason) {}
}
