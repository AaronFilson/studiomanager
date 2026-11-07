package io.github.aaronfilson.studiomanager.notify;

import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.StudioView;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Writes notices to people's inboxes, in the caller's transaction, and records each one in the
 * outbox ({@link NotificationCreated}) to be emailed after that commits.
 */
@Component
class Notices {

  private final NotificationRepository notifications;
  private final EntityManager entityManager;
  private final StudioService studios;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  Notices(
      NotificationRepository notifications,
      EntityManager entityManager,
      StudioService studios,
      ApplicationEventPublisher events,
      Clock clock) {
    this.notifications = notifications;
    this.entityManager = entityManager;
    this.studios = studios;
    this.events = events;
    this.clock = clock;
  }

  /** One notice to each of {@code accountIds}, written as a batch of inserts. */
  void send(Collection<UUID> accountIds, NotificationView.Kind kind, String title, String body) {
    if (accountIds.isEmpty()) {
      return;
    }
    StudioView studio = studios.current();
    Instant now = clock.instant();
    List<Notification> written = new ArrayList<>(accountIds.size());
    for (UUID accountId : accountIds) {
      // Persisted, not saved: a notification has no version, so save would look it up first
      Notification notification = new Notification(accountId, kind, title, body, now);
      entityManager.persist(notification);
      written.add(notification);
    }
    // Written before the events: the publisher may go on to run SQL (ADR 21)
    notifications.flush();
    for (Notification notification : written) {
      // Recorded in the outbox in this transaction, emailed after it commits
      events.publishEvent(
          new NotificationCreated(
              notification.id(),
              studio.id(),
              studio.name(),
              notification.accountId(),
              kind,
              title,
              body));
    }
  }
}
