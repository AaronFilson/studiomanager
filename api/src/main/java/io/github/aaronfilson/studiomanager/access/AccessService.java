package io.github.aaronfilson.studiomanager.access;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.studio.AccessChangeLog;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Duties and certifications in the bound studio: what other modules ask. Changing duties is here;
 * certification types and grants are in {@link Certifications}. Changing anything is for staff.
 */
@Service
public class AccessService {

  private final MemberDutyRepository duties;
  private final CertificationTypeRepository types;
  private final CertificationRepository certifications;
  private final StudioMembers members;
  private final Clock clock;
  private final StudioPermissions permissions;
  private final AccessChangeLog changeLog;

  AccessService(
      MemberDutyRepository duties,
      CertificationTypeRepository types,
      CertificationRepository certifications,
      StudioMembers members,
      Clock clock,
      StudioPermissions permissions,
      AccessChangeLog changeLog) {
    this.duties = duties;
    this.types = types;
    this.certifications = certifications;
    this.members = members;
    this.clock = clock;
    this.permissions = permissions;
    this.changeLog = changeLog;
  }

  @Transactional(readOnly = true)
  public Set<Duty> dutiesOf(UUID accountId) {
    Set<Duty> held = EnumSet.noneOf(Duty.class);
    duties.findByAccount(accountId).forEach(duty -> held.add(duty.duty()));
    return held;
  }

  /** Everyone's duties, for rosters and the members page. */
  @Transactional(readOnly = true)
  public Map<UUID, Set<Duty>> allDuties() {
    return duties.findAll().stream()
        .collect(
            Collectors.groupingBy(
                MemberDuty::accountId,
                Collectors.mapping(
                    MemberDuty::duty, Collectors.toCollection(() -> EnumSet.noneOf(Duty.class)))));
  }

  /**
   * Gives a member exactly these duties: the ones they lose are removed and the new ones granted
   * now. Duties they keep keep the date they were first granted.
   */
  @Transactional
  public Set<Duty> setDuties(UUID accountId, Set<Duty> wanted) {
    permissions.require(Permission.MEMBERS_DUTIES_MANAGE, "Only staff can do this.");
    requireMember(accountId);
    List<MemberDuty> held = duties.findByAccount(accountId);
    Set<Duty> kept = EnumSet.noneOf(Duty.class);
    for (MemberDuty duty : held) {
      if (wanted.contains(duty.duty())) {
        kept.add(duty.duty());
      } else {
        duties.delete(duty);
      }
    }
    Instant now = clock.instant();
    for (Duty duty : wanted) {
      if (!kept.contains(duty)) {
        duties.save(new MemberDuty(accountId, duty, now));
      }
    }
    duties.flush();
    Set<Duty> heldNow = dutiesOf(accountId);
    changeLog.record(
        AccessChangeLog.Kind.MEMBER_DUTIES,
        accountId.toString(),
        held.stream().map(duty -> duty.duty().name()).toList(),
        heldNow.stream().map(Duty::name).toList());
    return heldNow;
  }

  @Transactional(readOnly = true)
  public List<CertificationType> types() {
    return types.findAllByName().stream().map(CertificationTypeEntity::toView).toList();
  }

  @Transactional(readOnly = true)
  public CertificationType type(UUID typeId) {
    return types
        .findById(typeId)
        .map(CertificationTypeEntity::toView)
        .orElseThrow(() -> new NotFoundException("No such certification."));
  }

  /** Whether {@code accountId} holds an unexpired {@code typeId} certification at {@code at}. */
  @Transactional(readOnly = true)
  public boolean isCertified(UUID accountId, UUID typeId, Instant at) {
    return certifications.isCertified(accountId, typeId, at);
  }

  /** The end of the studio's local day, {@code months} months from {@code from}. */
  static Instant expiry(Instant from, int months, ZoneId zone) {
    return from.atZone(zone)
        .toLocalDate()
        .plusMonths(months)
        .plusDays(1)
        .atStartOfDay(zone)
        .toInstant();
  }

  void requireMember(UUID accountId) {
    if (members.roleOf(accountId).isEmpty()) {
      throw new InvalidInputException("That person isn't a member of this studio.");
    }
  }

  public record CertificationType(UUID id, String name, @Nullable Integer validMonths) {}

  public record Certification(
      UUID typeId,
      String name,
      UUID accountId,
      @Nullable String accountName,
      Instant grantedAt,
      @Nullable Instant expiresAt,
      boolean valid) {}
}
