package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inviting people into the bound studio, and accepting an invite. */
@Service
class StudioInvites {

  static final int MAX_EMAILED_INVITES = 200;

  private final StudioInviteRepository invites;
  private final StudioQueries queries;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  StudioInvites(
      StudioInviteRepository invites,
      StudioQueries queries,
      ApplicationEventPublisher events,
      Clock clock) {
    this.invites = invites;
    this.queries = queries;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Creates a link invite into the bound studio. Nobody can invite someone to a role above their
   * own, so staff can't mint owners.
   */
  @Transactional
  public InviteView invite(StudioRole role) {
    TenantContext caller = requireCanInvite(role);
    String code = InviteCodes.newCode();
    StudioInvite invite = new StudioInvite(code, role, caller.accountId(), null, clock.instant());
    invites.saveAndFlush(invite);
    return new InviteView(invite.id(), code, role, invite.expiresAt());
  }

  /**
   * Emails an invite to each address found in {@code text}, skipping {@code memberAddresses}
   * (lower-cased) and addresses with an unexpired invite. The emails go out after this commits.
   */
  @Transactional
  public EmailInvitesResult emailInvites(String text, StudioRole role, Set<String> memberAddresses) {
    TenantContext caller = requireCanInvite(role);
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
            new StudioInvite(InviteCodes.newCode(), role, caller.accountId(), address, now);
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
    requireCanInvite(invite.role());
    invites.delete(invite);
  }

  /** Emails an invite again with a new code and a fresh lifetime. The old code stops working. */
  @Transactional
  public OpenInviteView resend(UUID inviteId) {
    StudioInvite invite = findOpen(inviteId);
    TenantContext caller = requireCanInvite(invite.role());
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

  private static TenantContext requireCanInvite(StudioRole role) {
    TenantContext caller = TenantContext.require();
    StudioRole callerRole = caller.role();
    if (callerRole == null || !callerRole.includes(role)) {
      throw new AccessDeniedException("You can't invite someone to a role above your own.");
    }
    return caller;
  }
}
