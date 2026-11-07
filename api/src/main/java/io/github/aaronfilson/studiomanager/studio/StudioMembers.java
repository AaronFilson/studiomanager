package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The people in a studio. The studio module's API for them; inviting is in {@link StudioInvites},
 * and the invite methods other modules call are kept here.
 */
@Service
public class StudioMembers {

  private final StudioMembershipRepository memberships;
  private final StoredRoleRepository roles;
  private final StudioInvites invites;
  private final AccountDirectory accounts;

  StudioMembers(
      StudioMembershipRepository memberships,
      StoredRoleRepository roles,
      StudioInvites invites,
      AccountDirectory accounts) {
    this.memberships = memberships;
    this.roles = roles;
    this.invites = invites;
    this.accounts = accounts;
  }

  /**
   * Everyone in the bound studio, ordered by role and then name.
   *
   * <p>Filtered by studio explicitly, not left to RLS: a second policy lets anyone read their own
   * memberships in every studio (for "my studios"), so RLS alone would add the caller's rows from
   * their other studios. The same goes for {@link #roleOf}.
   */
  @Transactional(readOnly = true)
  public List<MemberView> list() {
    List<StudioMembership> rows = memberships.findByStudioId(boundStudio());
    Map<UUID, String> names =
        accounts.displayNames(rows.stream().map(StudioMembership::accountId).toList());
    Map<UUID, String> roleNames =
        roles.findAll().stream().collect(Collectors.toMap(StoredRole::id, StoredRole::name));
    return rows.stream()
        .map(row ->
            new MemberView(
                row.accountId(),
                names.get(row.accountId()),
                row.role(),
                row.roleId(),
                roleNames.getOrDefault(row.roleId(), row.role().name()),
                row.createdAt()))
        .sorted(
            Comparator.comparing(MemberView::role, Comparator.reverseOrder())
                .thenComparing(
                    MemberView::displayName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
        .toList();
  }

  /** The role of {@code accountId} in the bound studio, if they belong to it. */
  @Transactional(readOnly = true)
  public Optional<StudioRole> roleOf(UUID accountId) {
    return memberships.findRole(boundStudio(), accountId);
  }

  /**
   * The name of the role {@code accountId} holds in the bound studio, such as {@code Staff} or a
   * custom role's {@code Kiln tech}, if they belong to it.
   */
  @Transactional(readOnly = true)
  public Optional<String> roleNameOf(UUID accountId) {
    return memberships
        .findMemberRole(boundStudio(), accountId)
        .flatMap(role -> roles.findById(role.roleId()))
        .map(StoredRole::name);
  }

  /**
   * Emails an invite to each address found in {@code text}. Addresses of members, and those with an
   * unexpired invite, are skipped. The emails go out after this commits.
   */
  @Transactional
  public EmailInvitesResult emailInvites(String text, StudioRole role) {
    return emailInvites(text, role, null);
  }

  /**
   * As {@link #emailInvites(String, StudioRole)}, into the role row {@code roleId} (a custom role)
   * when it is given; {@code role} must then be its base.
   */
  @Transactional
  public EmailInvitesResult emailInvites(String text, StudioRole role, @Nullable UUID roleId) {
    Set<String> memberAddresses =
        accounts.emails(list().stream().map(MemberView::accountId).toList()).values().stream()
            .map(address -> address.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
    return invites.emailInvites(text, role, roleId, memberAddresses);
  }

  /** Each member's email address, lower-cased, to their account. For matching imported rows. */
  @Transactional(readOnly = true)
  public Map<String, UUID> membersByEmail() {
    Map<String, UUID> byEmail = new HashMap<>();
    accounts
        .emails(list().stream().map(MemberView::accountId).toList())
        .forEach((account, address) -> byEmail.put(address.toLowerCase(Locale.ROOT), account));
    return byEmail;
  }

  /** Which of {@code addresses} (lower-cased) already have an open, unexpired invite. */
  @Transactional(readOnly = true)
  public Set<String> openlyInvited(List<String> addresses) {
    return invites.openlyInvited(addresses);
  }

  /**
   * The invites in the bound studio that {@code accountId} accepted, so a module holding something
   * against an invite can find it once they join.
   */
  @Transactional(readOnly = true)
  public List<UUID> invitesAcceptedBy(UUID accountId) {
    return invites.acceptedBy(accountId);
  }

  private static UUID boundStudio() {
    return TenantContext.require().requireStudioId();
  }
}
