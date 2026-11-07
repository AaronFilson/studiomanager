package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionOperations;

/** Studios and membership in them. The studio module's API to the rest of the application. */
@Service
public class StudioService {

  private static final Set<String> ISO_COUNTRIES =
      Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2);

  private final StudioRepository studios;
  private final StudioMembershipRepository memberships;
  private final TransactionOperations transactions;
  private final Clock clock;
  private final StudioPermissions permissions;
  private final StudioRoles studioRoles;
  private final AccessChangeLog changeLog;
  private final ApplicationEventPublisher events;

  StudioService(
      StudioRepository studios,
      StudioMembershipRepository memberships,
      TransactionOperations transactions,
      Clock clock,
      StudioPermissions permissions,
      StudioRoles studioRoles,
      AccessChangeLog changeLog,
      ApplicationEventPublisher events) {
    this.studios = studios;
    this.memberships = memberships;
    this.transactions = transactions;
    this.clock = clock;
    this.permissions = permissions;
    this.studioRoles = studioRoles;
    this.changeLog = changeLog;
    this.events = events;
  }

  /**
   * Creates a studio with the calling account as its owner. Without a {@code currency}, the studio
   * uses its country's.
   */
  public StudioView create(
      String name,
      UnitSystem unitSystem,
      String timeZone,
      String country,
      @Nullable String currency) {
    TenantContext caller = TenantContext.require();
    String trimmedName = name.strip();
    if (trimmedName.isEmpty()) {
      throw new InvalidInputException("A studio needs a name.");
    }
    ZoneId zone = parseTimeZone(timeZone);
    String countryCode = parseCountry(country);
    Currency studioCurrency = currency == null ? currencyOf(countryCode) : parseCurrency(currency);
    UUID studioId = Uuids.v7();
    // Bind the new studio before the transaction begins, so row-level security sees these
    // inserts as happening inside the studio they create.
    StudioView created =
        TenantContext.callAs(
            caller.inStudio(studioId, StudioRole.OWNER),
            () ->
                transactions.execute(tx -> {
                  Instant now = clock.instant();
                  Studio made =
                      new Studio(
                          studioId,
                          trimmedName,
                          unitSystem,
                          zone,
                          countryCode,
                          studioCurrency,
                          now);
                  // The creator is the first owner, so the billing contact too
                  made.billingContact(caller.accountId());
                  Studio studio = studios.save(made);
                  memberships.save(
                      new StudioMembership(
                          Uuids.v7(), studioId, caller.accountId(), StudioRole.OWNER, now));
                  studioRoles.seed(now);
                  return StudioView.of(studio, StudioRole.OWNER, 1);
                }));
    return Objects.requireNonNull(created, "Creating a studio returns it");
  }

  /** The studio the current request is bound to. Membership is checked before this runs. */
  @Transactional(readOnly = true)
  public StudioView get(UUID studioId) {
    TenantContext context = TenantContext.require();
    StudioRole role = context.role();
    if (role == null || !studioId.equals(context.studioId())) {
      throw new IllegalStateException("The request is not bound to studio " + studioId);
    }
    Studio studio =
        studios.findById(studioId).orElseThrow(() -> new NotFoundException("No such studio."));
    return StudioView.of(studio, role, memberships.countByStudioId(studioId));
  }

  /** The studio the current request is bound to. */
  @Transactional(readOnly = true)
  public StudioView current() {
    UUID studioId = TenantContext.require().studioId();
    if (studioId == null) {
      throw new IllegalStateException("The request is not bound to a studio");
    }
    return get(studioId);
  }

  /**
   * Deletes the bound studio and everything in it: every studio-owned table cascades from {@code
   * studio}. Only an owner can, and nothing in the app offers it yet; the demo studio's reset is
   * the one caller.
   */
  @Transactional
  public void deleteCurrent() {
    UUID studioId = TenantContext.require().requireStudioId();
    // Deleting the studio removes every owner with it, so it takes the owners' own permission
    if (!permissions.has(Permission.OWNERS_MANAGE)) {
      throw new AccessDeniedException("Only an owner can delete a studio.");
    }
    Studio studio =
        studios.findById(studioId).orElseThrow(() -> new NotFoundException("No such studio."));
    studios.delete(studio);
    studios.flush();
  }

  @Transactional(readOnly = true)
  public List<MembershipView> membershipsOf(UUID accountId) {
    return memberships.findMembershipsOf(accountId);
  }

  /**
   * Makes a member of the bound studio one of its owners (plan 21, lane R8). Any owner can ({@code
   * owners.manage}); every owner is told. Making an owner an owner again changes nothing.
   */
  @Transactional
  public void addOwner(UUID studioId, UUID accountId) {
    requireStillAnOwner(studioId, "Only an owner can add an owner.");
    StudioMembership member =
        memberships
            .findByStudioIdAndAccountId(studioId, accountId)
            .orElseThrow(() -> new NotFoundException("They aren't a member of this studio."));
    StudioRole before = member.role();
    if (before == StudioRole.OWNER) {
      return;
    }
    member.changeRole(StudioRole.OWNER);
    memberships.flush();
    changeLog.record(
        AccessChangeLog.Kind.MEMBER_ROLE,
        accountId.toString(),
        List.of(before.name()),
        List.of(StudioRole.OWNER.name()));
    events.publishEvent(new OwnersChanged(studioId, accountId, OwnersChanged.Change.ADDED, false));
  }

  /**
   * Makes an owner of the bound studio staff instead. Any owner can, including of themselves
   * (stepping down), as long as another owner remains: the last owner is refused with "Add another
   * owner first", and so is a race that would leave none, because the owner rows are locked first.
   * If they were the billing contact, the studio has none until the remaining owners pick one, and
   * every owner is told.
   *
   * @throws ConflictException when they are the last owner
   */
  @Transactional
  public void removeOwner(UUID studioId, UUID accountId) {
    List<StudioMembership> owners =
        requireStillAnOwner(studioId, "Only an owner can remove an owner.");
    StudioMembership owner =
        owners.stream()
            .filter(membership -> membership.accountId().equals(accountId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("They aren't an owner of this studio."));
    if (owners.size() == 1) {
      throw new ConflictException(
          "A studio always needs an owner, and they're the only one. Add another owner first.");
    }
    owner.changeRole(StudioRole.STAFF);
    Studio studio = currentStudio(studioId);
    boolean contactCleared = accountId.equals(studio.billingContactAccountId());
    if (contactCleared) {
      studio.billingContact(null);
      changeLog.record(
          AccessChangeLog.Kind.BILLING_CONTACT, "studio", List.of(accountId.toString()), List.of());
    }
    memberships.flush();
    changeLog.record(
        AccessChangeLog.Kind.MEMBER_ROLE,
        accountId.toString(),
        List.of(StudioRole.OWNER.name()),
        List.of(StudioRole.STAFF.name()));
    events.publishEvent(
        new OwnersChanged(studioId, accountId, OwnersChanged.Change.REMOVED, contactCleared));
  }

  /** The bound studio's billing contact, an owner; empty while the owners choose one. */
  @Transactional(readOnly = true)
  public Optional<UUID> billingContact(UUID studioId) {
    TenantContext caller =
        permissions.require(Permission.OWNERS_MANAGE, "Only an owner can see the billing contact.");
    requireBoundTo(caller, studioId);
    return Optional.ofNullable(currentStudio(studioId).billingContactAccountId());
  }

  /**
   * Makes {@code accountId}, who must be an owner, the bound studio's billing contact.
   *
   * @throws ConflictException when they aren't an owner
   */
  @Transactional
  public void setBillingContact(UUID studioId, UUID accountId) {
    boolean isOwner =
        requireStillAnOwner(studioId, "Only an owner can choose the billing contact.").stream()
            .anyMatch(owner -> owner.accountId().equals(accountId));
    if (!isOwner) {
      throw new ConflictException("The billing contact must be an owner. Make them one first.");
    }
    Studio studio = currentStudio(studioId);
    UUID before = studio.billingContactAccountId();
    if (accountId.equals(before)) {
      return;
    }
    studio.billingContact(accountId);
    studios.flush();
    changeLog.record(
        AccessChangeLog.Kind.BILLING_CONTACT,
        "studio",
        before == null ? List.of() : List.of(before.toString()),
        List.of(accountId.toString()));
  }

  /**
   * Someone who accepts an owner invite is added without any owner acting in the studio, so the
   * owners are told here: after the acceptance commits, bound to the studio it joined. Accepting
   * happens outside any studio, so this can't run in that transaction.
   */
  @Async
  @TransactionalEventListener
  void onMemberJoined(MemberJoined joined) {
    TenantContext member =
        TenantContext.forAccount(joined.accountId()).inStudio(joined.studioId(), StudioRole.MEMBER);
    TenantContext.callAs(
        member,
        () -> {
          transactions.executeWithoutResult(tx -> {
            boolean joinedAsOwner =
                memberships
                    .findRole(joined.studioId(), joined.accountId())
                    .map(role -> role == StudioRole.OWNER)
                    .orElse(false);
            if (joinedAsOwner) {
              events.publishEvent(
                  new OwnersChanged(
                      joined.studioId(), joined.accountId(), OwnersChanged.Change.ADDED, false));
            }
          });
          return joined;
        });
  }

  /**
   * Refuses the caller unless they hold {@code owners.manage} and are still an owner now, under the
   * lock on the owner rows: an owner demoted a moment ago by another owner may have had their
   * permissions resolved before that committed. Returns the studio's owners, locked until the
   * transaction ends.
   */
  private List<StudioMembership> requireStillAnOwner(UUID studioId, String message) {
    TenantContext caller = permissions.require(Permission.OWNERS_MANAGE, message);
    requireBoundTo(caller, studioId);
    List<StudioMembership> owners = memberships.lockByRole(studioId, StudioRole.OWNER);
    boolean stillOwner =
        owners.stream().anyMatch(owner -> owner.accountId().equals(caller.accountId()));
    if (!stillOwner) {
      throw new AccessDeniedException("You're no longer an owner of this studio.");
    }
    return owners;
  }

  private static void requireBoundTo(TenantContext caller, UUID studioId) {
    if (!studioId.equals(caller.studioId())) {
      throw new IllegalStateException("The request is not bound to studio " + studioId);
    }
  }

  private Studio currentStudio(UUID studioId) {
    return studios.findById(studioId).orElseThrow(() -> new NotFoundException("No such studio."));
  }

  private static ZoneId parseTimeZone(String timeZone) {
    // Region IDs only: "America/Chicago" yes, "+05:00" or "EST" no
    if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
      throw new InvalidInputException(
          "Unknown time zone '%s'. Use an IANA name such as America/Chicago.".formatted(timeZone));
    }
    return ZoneId.of(timeZone);
  }

  private static String parseCountry(String country) {
    if (!ISO_COUNTRIES.contains(country)) {
      throw new InvalidInputException(
          "Unknown country '%s'. Use an ISO 3166-1 alpha-2 code such as US or GB."
              .formatted(country));
    }
    return country;
  }

  private static Currency parseCurrency(String currency) {
    try {
      return Currency.getInstance(currency);
    } catch (IllegalArgumentException e) {
      throw new InvalidInputException(
          "Unknown currency '%s'. Use an ISO 4217 code such as USD or GBP.".formatted(currency));
    }
  }

  private static Currency currencyOf(String country) {
    try {
      // Null for places with no currency of their own, such as Antarctica
      Currency currency = Currency.getInstance(Locale.of("", country));
      if (currency != null) {
        return currency;
      }
    } catch (IllegalArgumentException e) {
      // Fall through: the JDK knows the country but not its currency
    }
    throw new InvalidInputException(
        "Country '%s' has no default currency, so name one.".formatted(country));
  }
}
