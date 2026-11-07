package io.github.aaronfilson.studiomanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

  private static final Pattern CAN =
      Pattern.compile("^@studioAuthz\\.can\\(#studioId, '(\\w+)'\\)$");
  private static final Pattern OPERATION_ID = Pattern.compile("^      operationId:\\s*(\\S+)\\s*$");
  private static final Pattern X_PERMISSION =
      Pattern.compile("^      x-permission:\\s*(\\S+)\\s*$");

  /**
   * Classes outside {@code platform.tenancy} that name a {@link StudioRole} constant only to give
   * someone that role, never to decide what the caller may do. ArchUnit can't tell reading a
   * constant to assign it from reading it to compare, so they are listed by hand (plan 21, lane
   * R2):
   *
   * <ul>
   *   <li>{@code studio.StudioService}: a studio's creator becomes its owner, and co-owners are
   *       made and unmade (lane R8; who may do that is {@code owners.manage}, a Permission).
   *   <li>{@code importer.ImportService}: imported people are invited as members.
   *   <li>{@code studio.InviteEmails}, {@code webhook.WebhookFanOut}, {@code
   *       ledger.OpeningBalances}: bind a context in a role to do work after a commit.
   *   <li>{@code demo.DemoPeople}, {@code demo.DemoStudio}: the demo seed's cast, and finding the
   *       studio its owner owns.
   * </ul>
   *
   * <p>Every access decision checks a {@code Permission}. This list may only shrink.
   */
  private static final Set<String> ASSIGNS_A_ROLE =
      Set.of(
          "demo.DemoPeople",
          "demo.DemoStudio",
          "importer.ImportService",
          "ledger.OpeningBalances",
          "studio.InviteEmails",
          "studio.StudioService",
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
        .isSubsetOf(ASSIGNS_A_ROLE);
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

  @Test
  void everyStudioEndpointChecksAPermission() {
    Set<String> byRole = new TreeSet<>();
    for (JavaClass javaClass : classes) {
      if (!javaClass.isAnnotatedWith(RestController.class)) {
        continue;
      }
      for (JavaMethod method : javaClass.getMethods()) {
        Method handler = method.reflect();
        PreAuthorize check = handler.getAnnotation(PreAuthorize.class);
        if (check != null
            && takesStudioId(handler)
            && !check.value().startsWith("@studioAuthz.can(#studioId, '")) {
          byRole.add(shortName(javaClass) + "." + handler.getName());
        }
      }
    }
    assertThat(byRole).as("Studio endpoints not checking @studioAuthz.can").isEmpty();
  }

  @Test
  void theContractNamesThePermissionEachEndpointChecks() throws IOException {
    Map<String, String> named = contractPermissions();
    Map<String, String> different = new TreeMap<>();
    for (JavaClass javaClass : classes) {
      if (!javaClass.isAnnotatedWith(RestController.class)) {
        continue;
      }
      for (JavaMethod method : javaClass.getMethods()) {
        Method handler = method.reflect();
        PreAuthorize check = handler.getAnnotation(PreAuthorize.class);
        Matcher can = check == null ? null : CAN.matcher(check.value());
        if (can != null && can.matches()) {
          String checked = Permission.valueOf(can.group(1)).key();
          String inContract = named.get(handler.getName());
          if (!checked.equals(inContract)) {
            different.put(handler.getName(), checked + " checked, " + inContract + " named");
          }
        }
      }
    }
    assertThat(different).as("x-permission in the contract against @studioAuthz.can").isEmpty();
  }

  /** Each operation's x-permission in the contract, by operationId. */
  private static Map<String, String> contractPermissions() throws IOException {
    Map<String, String> named = new HashMap<>();
    String operationId = "";
    for (String line : Files.readAllLines(Path.of("src/main/openapi/studiomanager.yaml"))) {
      Matcher operation = OPERATION_ID.matcher(line);
      Matcher permission = X_PERMISSION.matcher(line);
      if (operation.matches()) {
        operationId = operation.group(1);
      } else if (permission.matches()) {
        named.put(operationId, permission.group(1));
      }
    }
    return named;
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
