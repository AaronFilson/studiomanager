package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.MemberRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.RolePermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inviting people into the bound studio, and accepting an invite. Public: other modules' tests (and
 * the demo seed) bring members into a studio the way people do.
 */
@Service
public class StudioInvites {

  static final int MAX_EMAILED_INVITES = 200;

  private final StudioInviteRepository invites;
  private final StoredRoleRepository roles;
  private final RolePermissions rolePermissions;
  private final StudioQueries queries;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  StudioInvites(
      StudioInviteRepository invites,
      StoredRoleRepository roles,
      RolePermissions rolePermissions,
      StudioQueries queries,
      ApplicationEventPublisher events,
      Clock clock) {
    this.invites = invites;
    this.roles = roles;
    this.rolePermissions = rolePermissions;
    this.queries = queries;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Creates a link invite into the bound studio's system role {@code role}. Nobody can invite
   * someone to a role that holds more than they do, so staff can't mint owners.
   */
  @Transactional
  public InviteView invite(StudioRole role) {
    return invite(role, null);
  }

  /**
   * Creates a link invite into the role row {@code roleId} (a custom role, lane R7), whose base
   * must be {@code role}; without one, into the system role {@code role}.
   */
  @Transactional
  public InviteView invite(StudioRole role, @Nullable UUID roleId) {
    StoredRole target = roleNamed(role, roleId);
    TenantContext caller = requireCanInvite(target);
    String code = InviteCodes.newCode();
    StudioInvite invite = new StudioInvite(code, target, caller.accountId(), null, clock.instant());
    invites.saveAndFlush(invite);
    return new InviteView(
        invite.id(), code, target.base(), target.id(), target.name(), invite.expiresAt());
  }

  /**
   * Emails an invite to each address found in {@code text}, skipping {@code memberAddresses}
   * (lower-cased) and addresses with an unexpired invite, into the role row {@code roleId} when
   * given (its base must be {@code role}), else the system role {@code role}. The emails go out
   * after this commits.
   */
  @Transactional
  public EmailInvitesResult emailInvites(
      String text, StudioRole role, @Nullable UUID roleId, Set<String> memberAddresses) {
    StoredRole target = roleNamed(role, roleId);
    TenantContext caller = requireCanInvite(target);
    List<String> addresses = EmailAddresses.extract(text);
    if (addresses.isEmpty()) {
      throw new InvalidInputException(
          "No email addresses found. Separate them with commas, spaces or new lines.");
    }
    if (addresses.size() > MAX_EMAILED_INVITES) {
      throw new InvalidInputException(
          "That's %d addresses; send at most %d at once."
              .formatted(addresses.size(), MAX_EMAILED_INVITES));
    }
    Instant now = clock.instant();
    Set<String> invited = openlyInvited(addresses);

    List<OpenInviteView> sent = new ArrayList<>();
    List<EmailInvitesResult.Skipped> skipped = new ArrayList<>();
    for (String address : addresses) {
      if (memberAddresses.contains(address)) {
        skipped.add(
            new EmailInvitesResult.Skipped(address, EmailInvitesResult.Reason.ALREADY_MEMBER));
      } else if (invited.contains(address)) {
        skipped.add(
            new EmailInvitesResult.Skipped(address, EmailInvitesResult.Reason.ALREADY_INVITED));
      } else {
        // A new invite replaces any expired one for the same address
        invites.deleteAll(invites.findExpiredFor(address, now));
        // The hash of a code nobody knows: InviteEmails replaces it when it sends
        StudioInvite invite =
            new StudioInvite(InviteCodes.newCode(), target, caller.accountId(), address, now);
        // Written before the event, as ADR 21 asks of anything published
        invites.saveAndFlush(invite);
        events.publishEvent(
            new InviteEmailRequested(invite.id(), caller.requireStudioId(), caller.accountId()));
        sent.add(invite.toOpenView());
      }
    }
    return new EmailInvitesResult(sent, skipped);
  }

  /** Which of {@code addresses} (lower-cased) already have an open, unexpired invite. */
  @Transactional(readOnly = true)
  public Set<String> openlyInvited(List<String> addresses) {
    if (addresses.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(invites.findOpenlyInvited(addresses, clock.instant()));
  }

  /** Invites in the bound studio that nobody has accepted, newest first. */
  @Transactional(readOnly = true)
  public List<OpenInviteView> openInvites() {
    return invites.findOpenNewestFirst().stream().map(StudioInvite::toOpenView).toList();
  }

  /** Withdraws an invite nobody has accepted, so its code stops working. */
  @Transactional
  public void revoke(UUID inviteId) {
    StudioInvite invite = findOpen(inviteId);
    requireCanInvite(roleOf(invite));
    invites.delete(invite);
  }

  /** Emails an invite again with a new code and a fresh lifetime. The old code stops working. */
  @Transactional
  public OpenInviteView resend(UUID inviteId) {
    StudioInvite invite = findOpen(inviteId);
    TenantContext caller = requireCanInvite(roleOf(invite));
    if (invite.email() == null) {
      throw new ConflictException(
          "This is a link invite, so there's no address to email. Create a new one instead.");
    }
    events.publishEvent(
        new InviteEmailRequested(inviteId, caller.requireStudioId(), caller.accountId()));
    return invite.toOpenView();
  }

  /** Accepts an invite for the signed-in account. Runs outside any studio. */
  @Transactional
  public MembershipView accept(String code) {
    MembershipView membership =
        queries
            .acceptInvite(code)
            .orElseThrow(() ->
                new NotFoundException("This invite doesn't exist, has been used, or has expired."));
    events.publishEvent(
        new MemberJoined(membership.studioId(), TenantContext.require().accountId()));
    return membership;
  }

  /** The invites in the bound studio that {@code accountId} accepted. */
  @Transactional(readOnly = true)
  public List<UUID> acceptedBy(UUID accountId) {
    return invites.findIdsAcceptedBy(accountId);
  }

  private StudioInvite findOpen(UUID inviteId) {
    return invites
        .findOpen(inviteId)
        .orElseThrow(() -> new NotFoundException("No such invite, or it has been accepted."));
  }

  /**
   * The role row {@code roleId}, whose base must be {@code role}; without one, the system role
   * {@code role}.
   */
  private StoredRole roleNamed(StudioRole role, @Nullable UUID roleId) {
    if (roleId == null) {
      return roles
          .findSystemRole(role)
          .orElseThrow(() -> new NotFoundException("This studio has no such role."));
    }
    StoredRole named =
        roles.findById(roleId).orElseThrow(() -> new NotFoundException("No such role."));
    if (named.base() != role) {
      throw new InvalidInputException(
          "That role sits at %s, not %s.".formatted(named.base().name(), role.name()));
    }
    return named;
  }

  /** The role row an invite is into: its own, or the system role of its base on older invites. */
  private StoredRole roleOf(StudioInvite invite) {
    return roleNamed(invite.role(), invite.roleId());
  }

  /**
   * Nobody can invite someone to a role that holds more than they do (plan 21 §7): the role's
   * permissions, as the studio has set them, must all be the caller's own.
   */
  private TenantContext requireCanInvite(StoredRole role) {
    TenantContext caller = TenantContext.require();
    Set<Permission> given = rolePermissions.ofMemberRole(new MemberRole(role.base(), role.id()));
    if (!caller.permissions().containsAll(given)) {
      throw new AccessDeniedException("You can't invite someone to a role above your own.");
    }
    return caller;
  }
}
