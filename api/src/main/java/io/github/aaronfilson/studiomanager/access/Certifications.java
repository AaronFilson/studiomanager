package io.github.aaronfilson.studiomanager.access;

import io.github.aaronfilson.studiomanager.access.AccessService.Certification;
import io.github.aaronfilson.studiomanager.access.AccessService.CertificationType;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.UniqueViolations;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Certification types, and granting and revoking certifications. */
@Service
class Certifications {

  private final AccessService access;
  private final CertificationTypeRepository types;
  private final CertificationRepository certifications;
  private final AccessQueries queries;
  private final StudioService studios;
  private final AccountDirectory accounts;
  private final Clock clock;

  Certifications(
      AccessService access,
      CertificationTypeRepository types,
      CertificationRepository certifications,
      AccessQueries queries,
      StudioService studios,
      AccountDirectory accounts,
      Clock clock) {
    this.access = access;
    this.types = types;
    this.certifications = certifications;
    this.queries = queries;
    this.studios = studios;
    this.accounts = accounts;
    this.clock = clock;
  }

  @Transactional
  public CertificationType createType(String name, @Nullable Integer validMonths) {
    AccessService.requireStaff();
    String cleaned = name.strip();
    if (cleaned.isEmpty() || cleaned.length() > 60) {
      throw new InvalidInputException("Name the certification in 1 to 60 characters.");
    }
    if (validMonths != null && (validMonths < 1 || validMonths > 120)) {
      throw new InvalidInputException("A certification can last 1 to 120 months, or not expire.");
    }
    CertificationTypeEntity type = new CertificationTypeEntity(cleaned, validMonths);
    try {
      types.saveAndFlush(type);
    } catch (DataIntegrityViolationException e) {
      if (UniqueViolations.isUniqueViolation(e)) {
        throw new InvalidInputException(
            "There's already a certification called '%s'.".formatted(cleaned));
      }
      throw e;
    }
    return type.toView();
  }

  /**
   * Grants (or renews) a certification. It expires {@code validMonths} after today, at the end of
   * the studio's day.
   */
  @Transactional
  public Certification grant(UUID typeId, UUID accountId) {
    TenantContext caller = AccessService.requireStaff();
    access.requireMember(accountId);
    CertificationType type = access.type(typeId);
    Instant now = clock.instant();
    Integer validMonths = type.validMonths();
    Instant expiresAt =
        validMonths == null
            ? null
            : AccessService.expiry(now, validMonths, studios.current().timeZone());
    queries.grant(caller.requireStudioId(), typeId, accountId, caller.accountId(), now, expiresAt);
    return certifications(accountId).stream()
        .filter(c -> c.typeId().equals(typeId))
        .findFirst()
        .orElseThrow();
  }

  @Transactional
  public void revoke(UUID typeId, UUID accountId) {
    AccessService.requireStaff();
    certifications
        .findById(new CertificationEntity.Key(typeId, accountId))
        .ifPresent(certifications::delete);
  }

  /** One person's certifications. Members may look only at their own. */
  @Transactional(readOnly = true)
  public List<Certification> certifications(UUID accountId) {
    TenantContext caller = TenantContext.require();
    if (!accountId.equals(caller.accountId()) && !AccessService.isStaff(caller)) {
      throw new AccessDeniedException("You can see only your own certifications.");
    }
    return withNames(certifications.findRowsFor(accountId));
  }

  /** Every certification in the studio, for staff. */
  @Transactional(readOnly = true)
  public List<Certification> allCertifications() {
    AccessService.requireStaff();
    return withNames(certifications.findAllRows());
  }

  private List<Certification> withNames(List<CertificationRow> rows) {
    Instant now = clock.instant();
    Map<UUID, String> names =
        accounts.displayNames(rows.stream().map(CertificationRow::accountId).toList());
    return rows.stream()
        .map(row -> {
          Instant expires = row.expiresAt();
          return new Certification(
              row.typeId(),
              row.name(),
              row.accountId(),
              names.get(row.accountId()),
              row.grantedAt(),
              expires,
              expires == null || expires.isAfter(now));
        })
        .toList();
  }
}
