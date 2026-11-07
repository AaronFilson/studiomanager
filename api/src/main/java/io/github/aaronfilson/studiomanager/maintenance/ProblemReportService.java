package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.access.AccessService;
import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Asset;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.ProblemReport;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.RolePermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.MemberView;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Problems members notice, such as a wheel that grinds or a vent that's stopped. Anyone in the
 * studio can report one; the staff and technicians hear about it, and staff resolve it.
 *
 * <p>The technician postings in the research name catching problems early as part of the job, and
 * members are usually the first to notice. A report from the phone at the wheel is the cheapest way
 * to get that to whoever fixes it.
 */
@Service
public class ProblemReportService {

  /** The most reports {@link #reports} returns. */
  static final int LIMIT = 500;

  private final ReportedProblemRepository problems;
  private final MaintenanceService maintenance;
  private final AccessService access;
  private final StudioMembers members;
  private final AccountDirectory accounts;
  private final ApplicationEventPublisher events;
  private final Clock clock;
  private final StudioPermissions permissions;
  private final RolePermissions rolePermissions;

  ProblemReportService(
      ReportedProblemRepository problems,
      MaintenanceService maintenance,
      AccessService access,
      StudioMembers members,
      AccountDirectory accounts,
      ApplicationEventPublisher events,
      Clock clock,
      StudioPermissions permissions,
      RolePermissions rolePermissions) {
    this.permissions = permissions;
    this.rolePermissions = rolePermissions;
    this.problems = problems;
    this.maintenance = maintenance;
    this.access = access;
    this.members = members;
    this.accounts = accounts;
    this.events = events;
    this.clock = clock;
  }

  /** Everything a member can report a problem with: every tracked asset, with whether it's down. */
  @Transactional(readOnly = true)
  public List<Asset> reportable() {
    return maintenance.assets(clock.instant());
  }

  @Transactional
  public ProblemReport report(@Nullable UUID assetId, String description) {
    String cleaned = description.strip();
    if (cleaned.isEmpty() || cleaned.length() > 1000) {
      throw new InvalidInputException("Describe the problem in up to 1,000 characters.");
    }
    String assetName = assetId == null ? null : maintenance.nameOf(maintenance.findAsset(assetId));
    TenantContext caller = TenantContext.require();
    ReportedProblem problem =
        new ReportedProblem(assetId, cleaned, caller.accountId(), clock.instant());
    // Written before the event, whose listeners write notices with SQL in this transaction
    problems.saveAndFlush(problem);
    String reporterName =
        accounts.displayNames(List.of(caller.accountId())).get(caller.accountId());
    events.publishEvent(
        new ProblemReported(
            caller.requireStudioId(),
            problem.id(),
            assetName,
            cleaned,
            reporterName,
            recipients(caller.accountId())));
    return views(List.of(problem)).getFirst();
  }

  /** Staff see every report; anyone else only their own. Newest first, at most {@value #LIMIT}. */
  @Transactional(readOnly = true)
  public Capped<ProblemReport> reports(boolean openOnly) {
    TenantContext caller = TenantContext.require();
    return Capped.fromFetched(
        views(
            problems.findNewest(
                seesEveryReport(caller),
                caller.accountId(),
                openOnly,
                Limit.of(Capped.fetchSize(LIMIT)))),
        LIMIT);
  }

  /** Closes a report, saying what was done or why nothing was. Staff only. */
  @Transactional
  public ProblemReport resolve(UUID reportId, @Nullable String resolution, long expectedVersion) {
    TenantContext caller =
        permissions.require(
            Permission.MAINTENANCE_REPORTS_RESOLVE, "Only staff can resolve a problem report.");
    ReportedProblem problem = find(caller, reportId);
    String cleaned = resolution == null || resolution.isBlank() ? null : resolution.strip();
    if (cleaned != null && cleaned.length() > 1000) {
      throw new InvalidInputException("Keep the resolution to 1,000 characters.");
    }
    if (problem.version() != expectedVersion) {
      throw MaintenanceText.staleVersionOfReport();
    }
    problem.resolve(caller.accountId(), clock.instant(), cleaned);
    problems.flush();
    return views(List.of(problem)).getFirst();
  }

  private ReportedProblem find(TenantContext caller, UUID reportId) {
    return problems
        .findVisible(reportId, seesEveryReport(caller), caller.accountId())
        .orElseThrow(() -> new NotFoundException("No such report."));
  }

  /** The reports as the API shows them: one lookup of asset names and one of people's names. */
  private List<ProblemReport> views(List<ReportedProblem> found) {
    if (found.isEmpty()) {
      return List.of();
    }
    Map<UUID, String> assetNames = maintenance.trackedNames();
    Map<UUID, String> names =
        accounts.displayNames(
            found.stream()
                .flatMap(problem ->
                    Stream.concat(
                        Stream.of(problem.reportedBy()), Stream.ofNullable(problem.resolvedBy())))
                .distinct()
                .toList());
    return found.stream()
        .map(problem -> {
          UUID assetId = problem.assetId();
          UUID resolvedBy = problem.resolvedBy();
          return problem.toView(
              assetId == null ? null : assetNames.get(assetId),
              names.get(problem.reportedBy()),
              resolvedBy == null ? null : names.get(resolvedBy));
        })
        .toList();
  }

  /** The staff and every technician, without the reporter: they already know. */
  private List<UUID> recipients(UUID reporter) {
    Stream<UUID> staff =
        members.list().stream()
            .filter(member ->
                rolePermissions.memberHolds(
                    member.memberRole(), Permission.MAINTENANCE_REPORTS_RESOLVE))
            .map(MemberView::accountId);
    Stream<UUID> technicians =
        access.allDuties().entrySet().stream()
            .filter(entry -> entry.getValue().contains(Duty.TECHNICIAN))
            .map(Map.Entry::getKey);
    return Stream.concat(staff, technicians)
        .distinct()
        .filter(id -> !id.equals(reporter))
        .sorted()
        .toList();
  }

  /** Whoever resolves reports sees every one; anyone else only their own. */
  private static boolean seesEveryReport(TenantContext caller) {
    return caller.holds(Permission.MAINTENANCE_REPORTS_RESOLVE);
  }
}
