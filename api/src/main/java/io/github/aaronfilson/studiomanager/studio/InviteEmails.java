package io.github.aaronfilson.studiomanager.studio;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import io.github.aaronfilson.studiomanager.platform.mail.Email;
import io.github.aaronfilson.studiomanager.platform.mail.EmailSender;
import io.github.aaronfilson.studiomanager.platform.mail.MailProperties;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Sends emailed invites. Makes a new code, stores its hash and emails the code in one
 * transaction, so the plain code exists only in the email: if the send fails, the hash change
 * rolls back and the outbox retries with another new code.
 *
 * <p>Not an {@code @ApplicationModuleListener}: that would open a transaction before the tenant
 * is bound, and row-level security reads the tenant when a transaction begins. So this binds the
 * studio first and then starts its own. The outbox still records it, as it does any
 * {@code @TransactionalEventListener}.
 */
@Component
class InviteEmails {

    private static final Logger log = LoggerFactory.getLogger(InviteEmails.class);

    // "4 Nov 2026": unambiguous in every country the product serves
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final JdbcClient jdbc;
    private final TransactionOperations transactions;
    private final AccountDirectory accounts;
    private final EmailSender sender;
    private final URI webOrigin;

    InviteEmails(JdbcClient jdbc, TransactionOperations transactions, AccountDirectory accounts, EmailSender sender,
            MailProperties mail) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.accounts = accounts;
        this.sender = sender;
        this.webOrigin = mail.webOrigin();
    }

    @Async
    @TransactionalEventListener
    void on(InviteEmailRequested request) {
        // Row-level security reads only the studio and the account; the role here is the one the
        // requester needed to ask for the invite
        TenantContext requester = TenantContext.forAccount(request.requestedBy())
                .inStudio(request.studioId(), StudioRole.STAFF);
        TenantContext.callAs(requester, () -> transactions.execute(tx -> {
            send(request);
            return null;
        }));
    }

    private void send(InviteEmailRequested request) {
        String code = StudioMembers.newCode();
        Instant now = StudioMembers.now();
        Instant expiresAt = now.plus(StudioMembers.INVITE_LIFETIME);
        Sending invite = jdbc.sql("""
                update studio_invite i set code_hash = :codeHash, expires_at = :expiresAt, last_sent_at = :now
                from studio s
                where i.id = :id and i.accepted_at is null and i.email is not null and s.id = i.studio_id
                returning i.email, i.role, s.name as studio_name, s.time_zone
                """)
                .param("codeHash", StudioMembers.sha256(code))
                .param("expiresAt", expiresAt.atOffset(ZoneOffset.UTC))
                .param("now", now.atOffset(ZoneOffset.UTC))
                .param("id", request.inviteId())
                .query(Sending.class)
                .optional()
                .orElse(null);
        if (invite == null) {
            // Accepted or revoked since it was asked for: nothing to send
            log.debug("Invite {} is no longer open; not emailing it", request.inviteId());
            return;
        }
        String inviter = accounts.displayNames(List.of(request.requestedBy())).get(request.requestedBy());
        String link = webOrigin.resolve("/invites/" + code).toString();
        String until = DATE.format(OffsetDateTime.ofInstant(expiresAt, ZoneId.of(invite.timeZone())));
        String text = """
                %s invited you to join %s as %s.

                Accept the invite: %s

                The link works once, until %s. If you weren't expecting this, you can ignore it."""
                .formatted(inviter == null ? "Someone at the studio" : inviter, invite.studioName(),
                        roleName(StudioRole.valueOf(invite.role())), link, until);
        sender.send(new Email(invite.email(), "You're invited to join " + invite.studioName(), text));
    }

    private static String roleName(StudioRole role) {
        return switch (role) {
            case OWNER -> "an owner";
            case STAFF -> "a staff member";
            case MEMBER -> "a member";
        };
    }

    record Sending(String email, String role, String studioName, String timeZone) {
    }

}
