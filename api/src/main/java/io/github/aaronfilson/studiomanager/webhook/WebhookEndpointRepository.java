package io.github.aaronfilson.studiomanager.webhook;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's webhook endpoints ({@code @TenantId}); a studio has at most ten. */
interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

  @Query("select e from WebhookEndpoint e order by e.createdAt, e.id")
  List<WebhookEndpoint> findAllInOrder();

  List<WebhookEndpoint> findByEnabledTrue();

  /** Locks the endpoint until the transaction ends, so two rotations can't lose a secret. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from WebhookEndpoint e where e.id = :endpointId")
  Optional<WebhookEndpoint> findAndLock(UUID endpointId);
}
