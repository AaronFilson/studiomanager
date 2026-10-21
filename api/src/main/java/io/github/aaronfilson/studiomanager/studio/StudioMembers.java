package io.github.aaronfilson.studiomanager.studio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The people in a studio, and inviting more of them. */
@Service
public class StudioMembers {

    static final Duration INVITE_LIFETIME = Duration.ofDays(14);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcClient jdbc;
    private final AccountDirectory accounts;

    StudioMembers(JdbcClient jdbc, AccountDirectory accounts) {
        this.jdbc = jdbc;
        this.accounts = accounts;
    }

    /** Everyone in the bound studio, ordered by role and then name. */
    @Transactional(readOnly = true)
    public List<MemberView> list() {
        List<Row> rows = jdbc.sql("select account_id, role, created_at from studio_membership")
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
        return jdbc.sql("select role from studio_membership where account_id = :accountId")
                .param("accountId", accountId)
                .query(StudioRole.class).optional();
    }

    /**
     * Creates an invite into the bound studio. Nobody can invite someone to a role above their
     * own, so staff can't mint owners.
     */
    @Transactional
    public InviteView invite(StudioRole role) {
        TenantContext caller = TenantContext.require();
        StudioRole callerRole = caller.role();
        if (callerRole == null || !callerRole.includes(role)) {
            throw new AccessDeniedException("You can't invite someone to a role above your own.");
        }
        String code = newCode();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Instant expiresAt = now.plus(INVITE_LIFETIME);
        UUID id = Uuids.v7();
        jdbc.sql("""
                insert into studio_invite (id, studio_id, code_hash, role, created_by, created_at, expires_at)
                values (:id, :studioId, :codeHash, :role, :createdBy, :createdAt, :expiresAt)
                """)
                .param("id", id)
                .param("studioId", caller.studioId())
                .param("codeHash", sha256(code))
                .param("role", role.name())
                .param("createdBy", caller.accountId())
                .param("createdAt", now.atOffset(ZoneOffset.UTC))
                .param("expiresAt", expiresAt.atOffset(ZoneOffset.UTC))
                .update();
        return new InviteView(id, code, role, expiresAt);
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

    private static String newCode() {
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
