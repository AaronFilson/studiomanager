package io.github.aaronfilson.studiomanager.webhook;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A made-up payload of each type, shaped exactly as {@link WebhookFanOut} builds them, so someone
 * setting up a Zap can map fields before the first real event. WebhookApiTests checks a real
 * payload has the same keys.
 */
final class WebhookExamples {

    private WebhookExamples() {
    }

    static Map<String, Object> of(WebhookEventType type, String studioId, String studioName) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", "evt_0192f3a07c5e7d2a9b1c4e5f60718293");
        envelope.put("type", type.wireName());
        envelope.put("version", 1);
        envelope.put("created_at", "2026-11-04T18:04:00Z");
        envelope.put("studio", Map.of("id", studioId, "name", studioName));
        envelope.put("data", data(type, studioName));
        return envelope;
    }

    private static Map<String, Object> data(WebhookEventType type, String studioName) {
        Map<String, Object> member = new LinkedHashMap<>();
        member.put("id", "0192f39b-1d2e-7f30-8a41-b52c63d74e85");
        member.put("name", "Mona Potter");
        member.put("email", "mona@example.com");
        Map<String, Object> data = new LinkedHashMap<>();
        switch (type) {
            case PIECES_READY -> {
                data.put("firing", Map.of("id", "0192f3a0-0000-7000-8000-000000000001", "kind", "glaze", "kiln",
                        "Big Skutt", "unloaded_at", "2026-11-04T18:04:00Z"));
                Map<String, Object> piece = new LinkedHashMap<>();
                piece.put("id", "0192f3a0-0000-7000-8000-000000000002");
                piece.put("number", 412);
                piece.put("description", "Blue mug");
                piece.put("collect_by", "2026-11-18T06:00:00Z");
                data.put("makers", List.of(Map.of("member", member, "pieces", List.of(piece))));
            }
            case FEE_CHARGED -> {
                data.put("member", member);
                data.put("description", "Firing fee, piece 412");
                data.put("amount", "6.50");
                data.put("currency", "USD");
                data.put("tax_category", "FIRING_SERVICE");
                data.put("charged_at", "2026-11-04T18:04:00Z");
                data.put("source", Map.of("kind", "piece", "id", "0192f3a0-0000-7000-8000-000000000002", "number", 412));
            }
            case MEMBER_JOINED -> {
                member.put("role", "member");
                data.put("member", member);
                data.put("joined_at", "2026-11-04T18:04:00Z");
            }
            case PING -> data.put("message",
                    "A test from " + studioName + ". If you can see this, the webhook works.");
        }
        return data;
    }

}
