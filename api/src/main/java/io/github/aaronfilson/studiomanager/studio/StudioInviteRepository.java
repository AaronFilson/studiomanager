package io.github.aaronfilson.studiomanager.studio;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's invites; Hibernate adds the studio to every query ({@code @TenantId}). */
interface StudioInviteRepository extends JpaRepository<StudioInvite, UUID> {

  @Query("select i from StudioInvite i where i.id = :inviteId and i.acceptedAt is null")
  Optional<StudioInvite> findOpen(UUID inviteId);

  /** An open emailed invite, which is all the sender sends. */
  @Query(
      """
      select i from StudioInvite i
      where i.id = :inviteId and i.acceptedAt is null and i.email is not null
      """)
  Optional<StudioInvite> findOpenEmailed(UUID inviteId);

  @Query("select i from StudioInvite i where i.acceptedAt is null order by i.createdAt desc")
  List<StudioInvite> findOpenNewestFirst();

  /** Which of {@code emails} (lower-cased) have an open, unexpired invite. */
  @Query(
      """
      select lower(i.email) from StudioInvite i
      where i.email is not null and i.acceptedAt is null and i.expiresAt > :now
        and lower(i.email) in :emails
      """)
  List<String> findOpenlyInvited(Collection<String> emails, Instant now);

  /** Expired, unaccepted invites for one address, which a new invite replaces. */
  @Query(
      """
      select i from StudioInvite i
      where lower(i.email) = :email and i.acceptedAt is null and i.expiresAt <= :now
      """)
  List<StudioInvite> findExpiredFor(String email, Instant now);

  @Query("select i.id from StudioInvite i where i.acceptedBy = :accountId")
  List<UUID> findIdsAcceptedBy(UUID accountId);
}
