package io.github.aaronfilson.studiomanager.notify;

import java.util.UUID;

/**
 * A notification was saved to a member's inbox. It carries everything needed to deliver it
 * elsewhere, because its listeners run after commit with no tenant bound, where row-level
 * security hides the notification table.
 */
public record NotificationCreated(UUID notificationId, UUID studioId, String studioName, UUID accountId,
        NotificationView.Kind kind, String title, String body) {
}
