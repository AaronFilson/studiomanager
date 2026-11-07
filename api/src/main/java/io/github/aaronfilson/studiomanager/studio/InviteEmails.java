package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.mail.Email;
import io.github.aaronfilson.studiomanager.platform.mail.EmailSender;
import io.github.aaronfilson.studiomanager.platform.mail.MailProperties;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Sends emailed invites. Makes a new code, stores its hash and emails the code in one transaction,
 * so the plain code exists only in the email: if the send fails, the hash change rolls back and the
 * outbox retries with another new code.
 *
 * <p>Not an {@code @ApplicationModuleListener}: that would open a transaction before the tenant is
 * bound, and row-level security reads the tenant when a transaction begins. So this binds the
 * studio first and then starts its own. The outbox still records it, as it does any
 * {@code @TransactionalEventListener}.
 */
@Component
class InviteEmails {

  private static final Logger log = LoggerFactory.getLogger(InviteEmails.class);

  // "4 Nov 2026": unambiguous in every country the product serves
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

  private final StudioInviteRepository invites;
  private final StudioRepository studios;
  private final TransactionOperations transactions;
  private final AccountDirectory accounts;
  private final EmailSender sender;
  private final URI webOrigin;
  private final Clock clock;

  InviteEmails(
      StudioInviteRepository invites,
      StudioRepository studios,
      TransactionOperations transactions,
      AccountDirectory accounts,
      EmailSender sender,
      MailProperties mail,
      Clock clock) {
    this.invites = invites;
    this.studios = studios;
    this.transactions = transactions;
    this.accounts = accounts;
    this.sender = sender;
    this.webOrigin = mail.webOrigin();
    this.clock = clock;
  }

  @Async
  @TransactionalEventListener
  void on(InviteEmailRequested request) {
    // Row-level security reads only the studio and the account; the role here is the one the
    // requester needed to ask for the invite
    TenantContext requester =
        TenantContext.forAccount(request.requestedBy())
            .inStudio(request.studioId(), StudioRole.STAFF);
    TenantContext.callAs(
        requester,
        () -> {
          transactions.executeWithoutResult(tx -> send(request));
          return request;
        });
  }

  private void send(InviteEmailRequested request) {
    Optional<StudioInvite> open = invites.findOpenEmailed(request.inviteId());
    if (open.isEmpty()) {
      // Accepted or revoked since it was asked for: nothing to send
      log.debug("Invite {} is no longer open; not emailing it", request.inviteId());
      return;
    }
    StudioInvite invite = open.get();
    Studio studio =
        studios
            .findById(request.studioId())
            .orElseThrow(() -> new IllegalStateException("The invite's studio is gone"));
    String code = InviteCodes.newCode();
    Instant now = clock.instant();
    invite.resend(code, now);
    // Written before the email goes out, so a failed write never leaves a code nobody can use
    invites.flush();

    String address = Objects.requireNonNull(invite.email(), "Only emailed invites are found");
    String inviter =
        accounts.displayNames(List.of(request.requestedBy())).get(request.requestedBy());
    String link = webOrigin.resolve("/invites/" + code).toString();
    String until = DATE.format(OffsetDateTime.ofInstant(invite.expiresAt(), studio.timeZone()));
    String text =
        """
        %s invited you to join %s as %s.

        Accept the invite: %s

        The link works once, until %s. If you weren't expecting this, you can ignore it.\
        """
            .formatted(
                inviter == null ? "Someone at the studio" : inviter,
                studio.name(),
                roleName(invite.role()),
                link,
                until);
    sender.send(new Email(address, "You're invited to join " + studio.name(), text));
  }

  private static String roleName(StudioRole role) {
    return switch (role) {
      case OWNER -> "an owner";
      case STAFF -> "a staff member";
      case MEMBER -> "a member";
    };
  }
}
