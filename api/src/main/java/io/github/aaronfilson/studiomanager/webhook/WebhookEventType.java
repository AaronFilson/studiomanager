package io.github.aaronfilson.studiomanager.webhook;

import java.util.Arrays;
import java.util.Optional;

/** What a studio can subscribe an endpoint to. {@link #PING} is sent only by "Send test". */
public enum WebhookEventType {

    PIECES_READY("pieces.ready",
            "A kiln was unloaded. Once per firing, with each maker and the pieces of theirs it finished."),
    FEE_CHARGED("fee.charged",
            "Something was charged to a member's balance: a firing fee when a piece is taken in, or supplies sold."),
    MEMBER_JOINED("member.joined", "Someone accepted an invite and joined the studio."),
    PING("ping", "A test, sent from the Webhooks page.");

    private final String wireName;
    private final String description;

    WebhookEventType(String wireName, String description) {
        this.wireName = wireName;
        this.description = description;
    }

    /** As it appears in payloads and the API, such as {@code pieces.ready}. */
    public String wireName() {
        return wireName;
    }

    public String description() {
        return description;
    }

    public boolean subscribable() {
        return this != PING;
    }

    public static Optional<WebhookEventType> fromWireName(String name) {
        return Arrays.stream(values()).filter(type -> type.wireName.equals(name)).findFirst();
    }

}
