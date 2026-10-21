package io.github.aaronfilson.studiomanager.studio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The people in a studio, and inviting more of them. */
@Service
public class StudioMembers {

    static final Duration INVITE_LIFETIME = Duration.ofDays(14);
    static final int MAX_EMAILED_INVITES = 200;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcClient jdbc;
    private final AccountDirectory accounts;
    private final ApplicationEventPublisher events;

    StudioMembers(JdbcClient jdbc, AccountDirectory accounts, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.events = events;
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
        List<Row> rows = jdbc.sql("select account_id, role, created_at from studio_membership where studio_id = :studioId")
                .param("studioId", TenantContext.require().studioId())
                .query(Row.class).list();
        Map<UUID, String> names = accounts.displayNames(rows.stream().map(Row::accountId).toList());
        return rows.stream()
                .map(row -> new MemberView(row.accountId(), names.get(row.accountId()), row.role(),
                        row.createdAt().toInstant()))
                .sorted(Comparator.comparing(MemberView::role, Comparator.reverseOrder())
                        .thenComparing(MemberView::displayName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    /** The role of {@code accountId} in the bound studio, if they belong to it. */
    @Transactional(readOnly = true)
    public Optional<StudioRole> roleOf(UUID accountId) {
        return jdbc.sql("select role from studio_membership where account_id = :accountId and studio_id = :studioId")
                .param("accountId", accountId)
                .param("studioId", TenantContext.require().studioId())
                .query(StudioRole.class).optional();
    }

    /**
     * Creates an invite into the bound studio. Nobody can invite someone to a role above their
     * own, so staff can't mint owners.
     */
    @Transactional
    public InviteView invite(StudioRole role) {
        TenantContext caller = requireCanInvite(role);
        String code = newCode();
        Instant now = now();
        UUID id = Uuids.v7();
        insertInvite(id, caller, code, role, null, now);
        return new InviteView(id, code, role, now.plus(INVITE_LIFETIME));
    }

    /**
     * Emails an invite to each address found in {@code text}. Addresses of members, and those
     * with an unexpired invite, are skipped. The emails go out after this commits.
     */
    @Transactional
    public EmailInvitesResult emailInvites(String text, StudioRole role) {
        TenantContext caller = requireCanInvite(role);
        List<String> addresses = EmailAddresses.extract(text);
        if (addresses.isEmpty()) {
            throw new InvalidInputException("No email addresses found. Separate them with commas, spaces or new lines.");
        }
        if (addresses.size() > MAX_EMAILED_INVITES) {
            throw new InvalidInputException("That's %d addresses; send at most %d at once."
                    .formatted(addresses.size(), MAX_EMAILED_INVITES));
        }
        Set<String> memberAddresses = accounts.emails(list().stream().map(MemberView::accountId).toList())
                .values().stream().map(address -> address.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        Instant now = now();
        Set<String> invited = new HashSet<>(jdbc.sql("""
                select lower(email) from studio_invite
                where email is not null and accepted_at is null and expires_at > :now and lower(email) = any(:emails)
                """)
                .param("now", now.atOffset(ZoneOffset.UTC))
                .param("emails", addresses.toArray(String[]::new))
                .query(String.class).list());

        List<OpenInviteView> sent = new ArrayList<>();
        List<EmailInvitesResult.Skipped> skipped = new ArrayList<>();
        for (String address : addresses) {
            if (memberAddresses.contains(address)) {
                skipped.add(new EmailInvitesResult.Skipped(address, EmailInvitesResult.Reason.ALREADY_MEMBER));
            } else if (invited.contains(address)) {
                skipped.add(new EmailInvitesResult.Skipped(address, EmailInvitesResult.Reason.ALREADY_INVITED));
            } else {
                // A new invite replaces any expired one for the same address
                jdbc.sql("""
                        delete from studio_invite where lower(email) = :email and accepted_at is null and expires_at <= :now
                        """)
                        .param("email", address)
                        .param("now", now.atOffset(ZoneOffset.UTC))
                        .update();
                UUID id = Uuids.v7();
                // The hash of a code nobody knows: InviteEmails replaces it when it sends
                insertInvite(id, caller, newCode(), role, address, now);
                events.publishEvent(new InviteEmailRequested(id, caller.studioId(), caller.accountId()));
                sent.add(new OpenInviteView(id, address, role, now, now.plus(INVITE_LIFETIME), null));
            }
        }
        return new EmailInvitesResult(sent, skipped);
    }

    /** Invites in the bound studio that nobody has accepted, newest first. */
    @Transactional(readOnly = true)
    public List<OpenInviteView> openInvites() {
        return jdbc.sql("""
                select id, email, role, created_at, expires_at, last_sent_at from studio_invite
                where accepted_at is null order by created_at desc
                """)
                .query((rs, row) -> openInvite(rs))
                .list();
    }

    /** Withdraws an invite nobody has accepted, so its code stops working. */
    @Transactional
    public void revoke(UUID inviteId) {
        OpenInviteView invite = openInvite(inviteId);
        requireCanInvite(invite.role());
        jdbc.sql("delete from studio_invite where id = :id and accepted_at is null").param("id", inviteId).update();
    }

    /** Emails an invite again with a new code and a fresh lifetime. The old code stops working. */
    @Transactional
    public OpenInviteView resend(UUID inviteId) {
        OpenInviteView invite = openInvite(inviteId);
        TenantContext caller = requireCanInvite(invite.role());
        if (invite.email() == null) {
            throw new ConflictException("This is a link invite, so there's no address to email. Create a new one instead.");
        }
        events.publishEvent(new InviteEmailRequested(inviteId, caller.studioId(), caller.accountId()));
        return invite;
    }

    /** Accepts an invite for the signed-in account. Runs outside any studio. */
    @Transactional
    public MembershipView accept(String code) {
        return jdbc.sql("select studio_id, studio_name, role from app.accept_invite(:code)")
                .param("code", code)
                .query((rs, row) -> new MembershipView(rs.getObject("studio_id", UUID.class),
                        rs.getString("studio_name"), StudioRole.valueOf(rs.getString("role"))))
                .optional()
                .orElseThrow(() -> new NotFoundException("This invite doesn't exist, has been used, or has expired."));
    }

    private static TenantContext requireCanInvite(StudioRole role) {
        TenantContext caller = TenantContext.require();
        StudioRole callerRole = caller.role();
        if (callerRole == null || !callerRole.includes(role)) {
            throw new AccessDeniedException("You can't invite someone to a role above your own.");
        }
        return caller;
    }

    private void insertInvite(UUID id, TenantContext caller, String code, StudioRole role, @Nullable String email,
            Instant now) {
        jdbc.sql("""
                insert into studio_invite (id, studio_id, code_hash, role, created_by, created_at, expires_at, email)
                values (:id, :studioId, :codeHash, :role, :createdBy, :createdAt, :expiresAt, :email)
                """)
                .param("id", id)
                .param("studioId", caller.studioId())
                .param("codeHash", sha256(code))
                .param("role", role.name())
                .param("createdBy", caller.accountId())
                .param("createdAt", now.atOffset(ZoneOffset.UTC))
                .param("expiresAt", now.plus(INVITE_LIFETIME).atOffset(ZoneOffset.UTC))
                .param("email", email)
                .update();
    }

    private OpenInviteView openInvite(UUID inviteId) {
        return jdbc.sql("""
                select id, email, role, created_at, expires_at, last_sent_at from studio_invite
                where id = :id and accepted_at is null
                """)
                .param("id", inviteId)
                .query((rs, row) -> openInvite(rs))
                .optional()
                .orElseThrow(() -> new NotFoundException("No such invite, or it has been accepted."));
    }

    private static OpenInviteView openInvite(ResultSet rs) throws SQLException {
        OffsetDateTime lastSentAt = rs.getObject("last_sent_at", OffsetDateTime.class);
        return new OpenInviteView(rs.getObject("id", UUID.class), rs.getString("email"),
                StudioRole.valueOf(rs.getString("role")), rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("expires_at", OffsetDateTime.class).toInstant(),
                lastSentAt == null ? null : lastSentAt.toInstant());
    }

    static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    static String newCode() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Must match app.accept_invite: sha256(convert_to(code, 'UTF8'))
    static byte[] sha256(String code) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every JDK provides SHA-256", e);
        }
    }

    record Row(UUID accountId, StudioRole role, OffsetDateTime createdAt) {
    }

}
