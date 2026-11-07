package io.github.aaronfilson.studiomanager.webhook;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Deliveries, always read with their event, since the log shows its type. */
interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

  @Query("select d from WebhookDelivery d join fetch d.event where d.id = :deliveryId")
  Optional<WebhookDelivery> findWithEvent(UUID deliveryId);

  /** An endpoint's deliveries since {@code since}, newest first. */
  @Query(
      """
      select d from WebhookDelivery d join fetch d.event
      where d.endpointId = :endpointId and d.createdAt > :since
      order by d.createdAt desc, d.id desc
      """)
  List<WebhookDelivery> findLog(UUID endpointId, Instant since, Limit limit);

  /** Every attempt at these deliveries, newest first: one query for a whole log. */
  @Query(
      """
      select a from WebhookAttempt a
      where a.deliveryId in :deliveryIds
      order by a.attemptedAt desc
      """)
  List<WebhookAttempt> findAttempts(Collection<UUID> deliveryIds);
}
