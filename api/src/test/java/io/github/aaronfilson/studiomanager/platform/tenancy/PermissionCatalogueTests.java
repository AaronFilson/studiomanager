package io.github.aaronfilson.studiomanager.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Every permission has words an owner can read, in one place (plan 21, lane R6). */
class PermissionCatalogueTests {

  @Test
  void everyPermissionHasALabelAndADescription() {
    for (Permission permission : Permission.values()) {
      PermissionCatalogue.Description description = PermissionCatalogue.describe(permission);
      assertThat(description.label()).as("%s label", permission).isNotBlank();
      assertThat(description.description()).as("%s description", permission).isNotBlank();
      assertThat(description.description()).as("%s description", permission).endsWith(".");
    }
  }

  @Test
  void labelsAreUnique() {
    Set<String> labels = new HashSet<>();
    for (Permission permission : Permission.values()) {
      assertThat(labels.add(PermissionCatalogue.describe(permission).label()))
          .as("%s has a label another permission uses", permission)
          .isTrue();
    }
  }

  @Test
  void everyAreaHasAPermission() {
    Set<PermissionCatalogue.Area> used = EnumSet.noneOf(PermissionCatalogue.Area.class);
    Arrays.stream(Permission.values())
        .forEach(permission -> used.add(PermissionCatalogue.describe(permission).area()));
    assertThat(used).containsExactlyInAnyOrder(PermissionCatalogue.Area.values());
  }
}
