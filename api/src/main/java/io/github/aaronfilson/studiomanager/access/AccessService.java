package io.github.aaronfilson.studiomanager.access;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import io.github.aaronfilson.studiomanager.studio.StudioService;

import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Duties and certifications in the bound studio. Changing them is for staff. */
@Service
public class AccessService {

    private final JdbcClient jdbc;
    private final StudioMembers members;
    private final StudioService studios;
    private final AccountDirectory accounts;

    AccessService(JdbcClient jdbc, StudioMembers members, StudioService studios, AccountDirectory accounts) {
        this.jdbc = jdbc;
        this.members = members;
        this.studios = studios;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public Set<Duty> dutiesOf(UUID accountId) {
        List<Duty> duties = jdbc.sql("select duty from member_duty where account_id = :id")
                .param("id", accountId).query(Duty.class).list();
        return duties.isEmpty() ? EnumSet.noneOf(Duty.class) : EnumSet.copyOf(duties);
    }

    /** Everyone's duties, for rosters and the members page. */
    @Transactional(readOnly = true)
    public Map<UUID, Set<Duty>> allDuties() {
        return jdbc.sql("select account_id, duty from member_duty")
                .query((rs, row) -> Map.entry(rs.getObject("account_id", UUID.class), Duty.valueOf(rs.getString("duty"))))
                .list().stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toCollection(() -> EnumSet.noneOf(Duty.class)))));
    }

    @Transactional
    public Set<Duty> setDuties(UUID accountId, Set<Duty> duties) {
        TenantContext caller = requireStaff();
        requireMember(accountId);
        jdbc.sql("delete from member_duty where account_id = :id").param("id", accountId).update();
        Instant now = now();
        for (Duty duty : duties) {
            jdbc.sql("""
                    insert into member_duty (studio_id, account_id, duty, granted_at) values (:studioId, :id, :duty, :now)
                    """)
                    .param("studioId", caller.studioId())
                    .param("id", accountId)
                    .param("duty", duty.name())
                    .param("now", utc(now))
                    .update();
        }
        return dutiesOf(accountId);
    }

    @Transactional(readOnly = true)
    public List<CertificationType> types() {
        return jdbc.sql("select id, name, valid_months from certification_type order by name")
                .query((rs, row) -> new CertificationType(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getObject("valid_months", Integer.class)))
                .list();
    }

    @Transactional
    public CertificationType createType(String name, @Nullable Integer validMonths) {
        TenantContext caller = requireStaff();
        String cleaned = name.strip();
        if (cleaned.isEmpty() || cleaned.length() > 60) {
            throw new InvalidInputException("Name the certification in 1 to 60 characters.");
        }
        if (validMonths != null && (validMonths < 1 || validMonths > 120)) {
            throw new InvalidInputException("A certification can last 1 to 120 months, or not expire.");
        }
        UUID id = Uuids.v7();
        try {
            jdbc.sql("insert into certification_type (id, studio_id, name, valid_months) values (:id, :studioId, :name, :months)")
                    .param("id", id)
                    .param("studioId", caller.studioId())
                    .param("name", cleaned)
                    .param("months", validMonths)
                    .update();
        } catch (DuplicateKeyException e) {
            throw new InvalidInputException("There's already a certification called '%s'.".formatted(cleaned));
        }
        return new CertificationType(id, cleaned, validMonths);
    }

    /** Grants (or renews) a certification. It expires {@code validMonths} after today, at the end of the studio's day. */
    @Transactional
    public Certification grant(UUID typeId, UUID accountId) {
        TenantContext caller = requireStaff();
        requireMember(accountId);
        CertificationType type = type(typeId);
        Instant now = now();
        Instant expiresAt = type.validMonths() == null ? null
                : expiry(now, type.validMonths(), studios.current().timeZone());
        jdbc.sql("""
                insert into certification (studio_id, type_id, account_id, granted_by, granted_at, expires_at)
                values (:studioId, :typeId, :accountId, :by, :now, :expires)
                on conflict (type_id, account_id) do update
                    set granted_by = excluded.granted_by, granted_at = excluded.granted_at, expires_at = excluded.expires_at
                """)
                .param("studioId", caller.studioId())
                .param("typeId", typeId)
                .param("accountId", accountId)
                .param("by", caller.accountId())
                .param("now", utc(now))
                .param("expires", expiresAt == null ? null : utc(expiresAt))
                .update();
        return certifications(accountId).stream().filter(c -> c.typeId().equals(typeId)).findFirst().orElseThrow();
    }

    @Transactional
    public void revoke(UUID typeId, UUID accountId) {
        requireStaff();
        jdbc.sql("delete from certification where type_id = :typeId and account_id = :accountId")
                .param("typeId", typeId).param("accountId", accountId).update();
    }

    /** One person's certifications. Members may look only at their own. */
    @Transactional(readOnly = true)
    public List<Certification> certifications(UUID accountId) {
        TenantContext caller = TenantContext.require();
        if (!accountId.equals(caller.accountId()) && !isStaff(caller)) {
            throw new AccessDeniedException("You can see only your own certifications.");
        }
        return query("where c.account_id = :accountId", Map.of("accountId", accountId));
    }

    /** Every certification in the studio, for staff. */
    @Transactional(readOnly = true)
    public List<Certification> allCertifications() {
        requireStaff();
        return query("", Map.of());
    }

    /** Whether {@code accountId} holds an unexpired {@code typeId} certification at {@code at}. */
    @Transactional(readOnly = true)
    public boolean isCertified(UUID accountId, UUID typeId, Instant at) {
        return jdbc.sql("""
                select exists (select 1 from certification
                               where account_id = :accountId and type_id = :typeId
                                 and (expires_at is null or expires_at > :at))
                """)
                .param("accountId", accountId)
                .param("typeId", typeId)
                .param("at", utc(at))
                .query(Boolean.class).single();
    }

    @Transactional(readOnly = true)
    public CertificationType type(UUID typeId) {
        return types().stream().filter(t -> t.id().equals(typeId)).findFirst()
                .orElseThrow(() -> new NotFoundException("No such certification."));
    }

    /** The end of the studio's local day, {@code months} months from {@code from}. */
    static Instant expiry(Instant from, int months, ZoneId zone) {
        return from.atZone(zone).toLocalDate().plusMonths(months).plusDays(1).atStartOfDay(zone).toInstant();
    }

    private List<Certification> query(String where, Map<String, Object> params) {
        Instant now = Instant.now();
        var spec = jdbc.sql("""
                select c.type_id, t.name, c.account_id, c.granted_at, c.expires_at
                from certification c join certification_type t on t.id = c.type_id
                %s order by t.name
                """.formatted(where));
        for (var param : params.entrySet()) {
            spec = spec.param(param.getKey(), param.getValue());
        }
        List<Certification> found = spec.query((rs, row) -> {
            OffsetDateTime expires = rs.getObject("expires_at", OffsetDateTime.class);
            return new Certification(rs.getObject("type_id", UUID.class), rs.getString("name"),
                    rs.getObject("account_id", UUID.class), null,
                    rs.getObject("granted_at", OffsetDateTime.class).toInstant(),
                    expires == null ? null : expires.toInstant(),
                    expires == null || expires.toInstant().isAfter(now));
        }).list();
        Map<UUID, String> names = accounts.displayNames(found.stream().map(Certification::accountId).toList());
        return found.stream()
                .map(c -> new Certification(c.typeId(), c.name(), c.accountId(), names.get(c.accountId()),
                        c.grantedAt(), c.expiresAt(), c.valid()))
                .toList();
    }

    private void requireMember(UUID accountId) {
        if (members.roleOf(accountId).isEmpty()) {
            throw new InvalidInputException("That person isn't a member of this studio.");
        }
    }

    private static TenantContext requireStaff() {
        TenantContext caller = TenantContext.require();
        if (!isStaff(caller)) {
            throw new AccessDeniedException("Only staff can do this.");
        }
        return caller;
    }

    private static boolean isStaff(TenantContext caller) {
        return caller.role() != null && caller.role().includes(StudioRole.STAFF);
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public record CertificationType(UUID id, String name, @Nullable Integer validMonths) {
    }

    public record Certification(UUID typeId, String name, UUID accountId, @Nullable String accountName,
            Instant grantedAt, @Nullable Instant expiresAt, boolean valid) {
    }

}
