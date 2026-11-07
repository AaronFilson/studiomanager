package io.github.aaronfilson.studiomanager.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Every studio operation in the contract names the permission it needs, as {@code x-permission:
 * pieces.move}, the same idea as {@link TokenPolicyTests}' every-area test (plan 21 §5).
 */
class PermissionContractTests {

  /** Studio operations not naming a permission: none since lane R2, and it stays that way. */
  private static final int STILL_UNNAMED = 0;

  private static final Pattern PATH = Pattern.compile("^  (/\\S+):\\s*$");
  private static final Pattern OPERATION =
      Pattern.compile("^    (get|put|post|patch|delete):\\s*$");
  private static final Pattern NAMED = Pattern.compile("^      x-permission:\\s*(\\S+)\\s*$");

  record Operation(String path, String method, List<String> permissions) {}

  @Test
  void everyNamedPermissionIsInTheCatalogue() throws IOException {
    for (Operation operation : studioOperations()) {
      for (String key : operation.permissions()) {
        assertThat(Permission.byKey(key))
            .as("%s %s names %s", operation.method(), operation.path(), key)
            .isPresent();
      }
    }
  }

  @Test
  void studioOperationsNameTheirPermission() throws IOException {
    List<Operation> operations = studioOperations();
    assertThat(operations).isNotEmpty();
    long unnamed =
        operations.stream().filter(operation -> operation.permissions().isEmpty()).count();
    // Equal, not at most, so naming one also lowers the count and it can't creep back up
    assertThat(unnamed).as("studio operations without x-permission").isEqualTo(STILL_UNNAMED);
  }

  /** The contract's studio operations, each with the permissions it names. */
  static List<Operation> studioOperations() throws IOException {
    List<String> lines = Files.readAllLines(Path.of("src/main/openapi/studiomanager.yaml"));
    List<Operation> operations = new ArrayList<>();
    String path = "";
    @Nullable String method = null;
    List<String> permissions = new ArrayList<>();
    for (String line : lines) {
      Matcher pathLine = PATH.matcher(line);
      Matcher operationLine = OPERATION.matcher(line);
      Matcher namedLine = NAMED.matcher(line);
      if (pathLine.matches() || operationLine.matches()) {
        if (method != null && path.startsWith("/api/v1/studios/{studioId}")) {
          operations.add(new Operation(path, method, List.copyOf(permissions)));
        }
        method = null;
        permissions.clear();
        if (pathLine.matches()) {
          path = pathLine.group(1);
        } else {
          method = operationLine.group(1);
        }
      } else if (method != null && namedLine.matches()) {
        permissions.add(namedLine.group(1));
      } else if (!line.isEmpty() && !line.startsWith(" ")) {
        // A new top-level section, such as components, ends the paths
        if (method != null && path.startsWith("/api/v1/studios/{studioId}")) {
          operations.add(new Operation(path, method, List.copyOf(permissions)));
        }
        method = null;
        permissions.clear();
        path = "";
      }
    }
    return operations;
  }
}
