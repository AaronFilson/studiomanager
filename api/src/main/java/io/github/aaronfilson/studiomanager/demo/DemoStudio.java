package io.github.aaronfilson.studiomanager.demo;

import io.github.aaronfilson.studiomanager.apitoken.ApiTokens;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.platform.tenancy.TokenScope;
import io.github.aaronfilson.studiomanager.studio.InviteView;
import io.github.aaronfilson.studiomanager.studio.MembershipView;
import io.github.aaronfilson.studiomanager.studio.StudioInvites;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.StudioView;
import io.github.aaronfilson.studiomanager.studio.UnitSystem;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.modulith.events.core.EventPublicationRegistry;
import org.springframework.stereotype.Service;

/**
 * Resetting the demo studio and making a token for it. Platform admins only; the controller checks.
 *
 * <p>The demo studio is whichever studio the seeded owner, {@link DemoPeople#MARA}, owns and that
 * is called {@link #NAME}. Nobody can sign in as her, so no other studio can match, and a reset
 * deletes only what matches.
 */
@Service
class DemoStudio {

  static final String NAME = "Demo Clay Co.";

  /** How long a demo token lasts. */
  static final int TOKEN_DAYS = 1;

  /** The longest a reset waits for listeners to finish with the seed's events. */
  static final Duration LISTENER_WAIT = Duration.ofSeconds(30);

  private static final Duration LISTENER_POLL = Duration.ofMillis(200);

  // One reset at a time on this replica, so two clicks don't make two studios. Across replicas a
  // race can still make two; the next reset deletes both.
  private final ReentrantLock resetting = new ReentrantLock();

  private final AccountResolver accounts;
  private final StudioService studios;
  private final StudioInvites invites;
  private final ApiTokens tokens;
  private final DemoSeed seed;
  private final EventPublicationRegistry publications;
  private final Clock clock;

  DemoStudio(
      AccountResolver accounts,
      StudioService studios,
      StudioInvites invites,
      ApiTokens tokens,
      DemoSeed seed,
      EventPublicationRegistry publications,
      Clock clock) {
    this.accounts = accounts;
    this.studios = studios;
    this.invites = invites;
    this.tokens = tokens;
    this.seed = seed;
    this.publications = publications;
    this.clock = clock;
  }

  /** The studio after a reset. */
  record Reset(UUID studioId, String studioName, DemoSeed.Seeded seeded) {}

  /** A demo token, with its secret shown once. */
  record DemoToken(
      UUID studioId, String studioName, String name, String secret, Instant expiresAt) {}

  /**
   * Deletes the demo studio, if there is one, and seeds a new one. {@code adminAccountId}, the
   * admin who asked, becomes an owner too, so the studio is in their list.
   */
  Reset reset(UUID adminAccountId) {
    resetting.lock();
    try {
      Map<DemoPeople, UUID> accountIds = new EnumMap<>(DemoPeople.class);
      for (DemoPeople person : DemoPeople.values()) {
        accountIds.put(person, person.accountId(accounts));
      }
      UUID ownerId = accountIds.get(DemoPeople.MARA);
      if (ownerId == null) {
        throw new IllegalStateException("The demo owner has an account");
      }
      TenantContext owner = TenantContext.forAccount(ownerId);
      for (UUID studioId : demoStudios(owner)) {
        TenantContext.callAs(
            owner.inStudio(studioId, StudioRole.OWNER),
            () -> {
              studios.deleteCurrent();
              return null;
            });
      }

      StudioView studio =
          TenantContext.callAs(
              owner,
              () -> studios.create(NAME, UnitSystem.US_CUSTOMARY, "America/Chicago", "US", "USD"));
      TenantContext inStudio = owner.inStudio(studio.id(), StudioRole.OWNER);
      Map<DemoPeople, TenantContext> people = new EnumMap<>(DemoPeople.class);
      people.put(DemoPeople.MARA, inStudio);
      for (DemoPeople person : DemoPeople.values()) {
        if (person == DemoPeople.MARA) {
          continue;
        }
        UUID accountId = accountIds.get(person);
        if (accountId == null) {
          throw new IllegalStateException("Every demo person has an account");
        }
        join(inStudio, accountId, person.role());
        people.put(
            person, TenantContext.forAccount(accountId).inStudio(studio.id(), person.role()));
      }
      Instant seeding = clock.instant();
      DemoSeed.Seeded seeded = seed.seed(people);
      // Last, so the admin isn't notified of everything the seed did
      if (!accountIds.containsValue(adminAccountId)) {
        join(inStudio, adminAccountId, StudioRole.OWNER);
      }
      awaitListeners(seeding);
      return new Reset(studio.id(), studio.name(), seeded);
    } finally {
      resetting.unlock();
    }
  }

  /**
   * Waits, at most {@link #LISTENER_WAIT}, for the other modules' asynchronous listeners to finish
   * with the events the seed published (the stream, notifications, webhooks). Otherwise the next
   * reset could delete the studio under them, and each would fail and be retried for nothing.
   */
  private void awaitListeners(Instant since) {
    Instant giveUp = clock.instant().plus(LISTENER_WAIT);
    while (publications.findIncompletePublications().stream()
        .anyMatch(publication -> !publication.getPublicationDate().isBefore(since))) {
      if (clock.instant().isAfter(giveUp)) {
        return;
      }
      try {
        Thread.sleep(LISTENER_POLL);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  /** A read-only token for the demo studio, belonging to its seeded owner, valid for a day. */
  DemoToken token() {
    UUID ownerId = DemoPeople.MARA.accountId(accounts);
    TenantContext owner = TenantContext.forAccount(ownerId);
    List<UUID> found = demoStudios(owner);
    if (found.isEmpty()) {
      throw new NotFoundException("There's no demo studio yet. Reset it first.");
    }
    UUID studioId = found.getFirst();
    ApiTokens.Created created =
        TenantContext.callAs(
            owner.inStudio(studioId, StudioRole.OWNER),
            () -> tokens.create("Demo assistant", Set.of(TokenScope.READ), TOKEN_DAYS));
    return new DemoToken(
        studioId, NAME, created.token().name(), created.secret(), created.token().expiresAt());
  }

  /** The studios the demo owner owns under the demo name: one, unless a reset raced another. */
  private List<UUID> demoStudios(TenantContext owner) {
    return TenantContext.callAs(owner, () -> studios.membershipsOf(owner.accountId())).stream()
        .filter(membership -> membership.role() == StudioRole.OWNER)
        .filter(membership -> NAME.equals(membership.studioName()))
        .map(MembershipView::studioId)
        .toList();
  }

  /** Brings {@code accountId} into the studio the way people join: by accepting an invite. */
  private void join(TenantContext inviter, UUID accountId, StudioRole role) {
    InviteView invite = TenantContext.callAs(inviter, () -> invites.invite(role));
    TenantContext.callAs(TenantContext.forAccount(accountId), () -> invites.accept(invite.code()));
  }
}
