package io.github.aaronfilson.studiomanager.notify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Members' notifications in the bound studio ({@code @TenantId}). */
interface NotificationRepository extends JpaRepository<Notification, UUID> {

  /** One member's inbox, newest first. */
  @Query(
      "select n from Notification n where n.accountId = :accountId order by n.createdAt desc, n.id desc")
  List<Notification> findInbox(UUID accountId, Limit limit);

  Optional<Notification> findByIdAndAccountId(UUID id, UUID accountId);
}
