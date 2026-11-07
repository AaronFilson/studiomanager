package io.github.aaronfilson.studiomanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Keeps permission checks in one place (plan 21 §5, ADR 25): no new code branches on a role, and
 * every studio endpoint declares its check.
 */
class PermissionArchitectureTests {

  private static final String TENANCY = "io.github.aaronfilson.studiomanager.platform.tenancy";

  /**
   * Classes that still compare or switch on {@link StudioRole} outside {@code platform.tenancy} (or
   * name a role to assign it). Lane R2 replaces them with permissions and empties this list; it may
   * only shrink.
   */
  private static final Set<String> STILL_BRANCHING_ON_ROLE =
      Set.of(
          "access.AccessController",
          "access.AccessService",
          "ai.AiKeys",
          "apitoken.ApiTokens",
          "demo.DemoPeople",
          "demo.DemoStudio",
          "glaze.GlazeBook",
          "importer.ImportService",
          "ledger.LedgerService",
          "ledger.OpeningBalances",
          "ledger.PayablesChecks",
          "maintenance.MaintenanceRecords",
          "maintenance.MaintenanceReminders",
          "maintenance.ProblemReportService",
          "mcp.McpRequestFilter",
          "notify.NotificationListeners",
          "notify.NotificationService",
          "piece.PieceService",
          "proposal.PendingActionReview",
          "proposal.Proposals",
          "schedule.ScheduleService",
          "studio.InviteEmails",
          "studio.StudioInvites",
          "studio.StudioService",
          "supplies.PurchaseRequests",
          "supplies.SupplyService",
          "today.TodayService",
          "webhook.WebhookEndpoints",
          "webhook.WebhookFanOut");

  private final JavaClasses classes =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("io.github.aaronfilson.studiomanager");

  @Test
  void noNewCodeBranchesOnARole() {
    Set<String> branching = new TreeSet<>();
    for (JavaClass javaClass : classes) {
      if (javaClass.getPackageName().startsWith(TENANCY)) {
        continue;
      }
      for (JavaAccess<?> access : javaClass.getAccessesFromSelf()) {
        if (access.getTargetOwner().isEquivalentTo(StudioRole.class)
            && branchesOnRole(access.getName())) {
          branching.add(shortName(javaClass));
        }
      }
    }
    assertThat(branching)
        .as("Check a Permission through StudioPermissions instead of comparing roles")
        .isSubsetOf(STILL_BRANCHING_ON_ROLE);
  }

  @Test
  void everyStudioEndpointDeclaresItsCheck() {
    Set<String> unchecked = new TreeSet<>();
    for (JavaClass javaClass : classes) {
      if (!javaClass.isAnnotatedWith(RestController.class)) {
        continue;
      }
      for (JavaMethod method : javaClass.getMethods()) {
        Method handler = method.reflect();
        // The mapping is usually on the generated *Api interface the controller implements
        if (MergedAnnotations.from(handler, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
                .isPresent(RequestMapping.class)
            && takesStudioId(handler)
            && !handler.isAnnotationPresent(PreAuthorize.class)) {
          unchecked.add(shortName(javaClass) + "." + handler.getName());
        }
      }
    }
    assertThat(unchecked).as("Studio endpoints without @PreAuthorize").isEmpty();
  }

  private static boolean branchesOnRole(String member) {
    // Reading a constant or calling includes(); name() and valueOf() only convert for display
    return !member.equals("name")
        && !member.equals("valueOf")
        && !member.equals("toString")
        && !member.equals("<init>");
  }

  private static boolean takesStudioId(Method handler) {
    for (Parameter parameter : handler.getParameters()) {
      PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
      if (pathVariable != null
          && (pathVariable.value().equals("studioId")
              || pathVariable.name().equals("studioId")
              || parameter.getName().equals("studioId"))) {
        return true;
      }
    }
    return false;
  }

  private static String shortName(JavaClass javaClass) {
    String outermost = javaClass.getName();
    int nested = outermost.indexOf('$');
    if (nested >= 0) {
      outermost = outermost.substring(0, nested);
    }
    return outermost.substring("io.github.aaronfilson.studiomanager.".length());
  }
}
