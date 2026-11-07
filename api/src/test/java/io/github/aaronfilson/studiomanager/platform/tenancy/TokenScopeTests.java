package io.github.aaronfilson.studiomanager.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Each scope allows the permissions the operations it reaches name, so capping a token by its
 * scopes takes nothing away that {@link TokenPolicy} lets it reach; and no scope allows anything
 * from the areas a token never reaches (plan 21, lane R5).
 */
class TokenScopeTests {

  private static final String STUDIO_PATH = "/api/v1/studios/{studioId}";

  @Test
  void everyOperationATokenReachesIsAllowedByTheScopeItNeeds() throws IOException {
    for (PermissionContractTests.Operation operation : PermissionContractTests.studioOperations()) {
      TokenPolicy.Access access =
          TokenPolicy.AREAS.getOrDefault(
              areaOf(operation.path()), new TokenPolicy.Never("not listed"));
      boolean reading = operation.method().equals("get");
      Set<Permission> allowed =
          switch (access) {
            case TokenPolicy.Never unreachable -> {
              // A token refused here is told why
              assertThat(unreachable.why()).as("%s's refusal", operation.path()).isNotBlank();
              yield Set.of();
            }
            case TokenPolicy.ReadOnly() -> reading ? TokenScope.READ.permissions() : Set.of();
            case TokenPolicy.Writable(TokenScope writeScope) ->
                reading ? TokenScope.READ.permissions() : writeScope.permissions();
          };
      if (allowed.isEmpty() || needsAnOwnerOnlyPermission(operation)) {
        // An owner-only operation is never a token's, whatever its area: no scope allows one
        continue;
      }
      for (String key : operation.permissions()) {
        assertThat(allowed)
            .as("%s %s needs %s", operation.method(), operation.path(), key)
            .contains(Permission.byKey(key).orElseThrow());
      }
    }
  }

  @Test
  void noScopeAllowsWhatTokensNeverReach() {
    Set<Permission> anyScope = TokenScope.allowedBy(List.of(TokenScope.values()));
    Set<Permission> never =
        EnumSet.of(
            Permission.MONEY_READ,
            Permission.MONEY_TAKE_PAYMENT,
            Permission.MONEY_STORED_VALUE_MANAGE,
            Permission.MONEY_REFUND_AND_ADJUST,
            Permission.MONEY_PAYOUTS,
            Permission.MONEY_PAYABLES_MANAGE,
            Permission.MONEY_TAX_RATES_EDIT,
            Permission.MEMBERS_INVITE,
            Permission.MEMBERS_ROLE_ASSIGN,
            Permission.MEMBERS_REMOVE,
            Permission.STUDIO_SETTINGS_EDIT,
            Permission.STUDIO_EXPORT,
            Permission.STUDIO_AI_KEY_MANAGE,
            Permission.STUDIO_ROLES_MANAGE,
            Permission.IMPORTS_RUN,
            Permission.WEBHOOKS_MANAGE,
            Permission.API_TOKENS_OWN,
            Permission.API_TOKENS_MANAGE_ALL,
            Permission.INVOICES_MANAGE,
            Permission.PROPOSALS_REVIEW,
            Permission.OWNERS_MANAGE);
    assertThat(anyScope).doesNotContainAnyElementsOf(never);
  }

  @Test
  void onlyProposeAllowsProposingAndOnlyReadAllowsMcp() {
    Arrays.stream(TokenScope.values())
        .forEach(scope -> {
          assertThat(scope.permissions().contains(Permission.PROPOSALS_CREATE))
              .as("%s allows proposals.create", scope)
              .isEqualTo(scope == TokenScope.PROPOSE);
          assertThat(scope.permissions().contains(Permission.MCP_USE))
              .as("%s allows mcp.use", scope)
              .isEqualTo(scope == TokenScope.READ);
        });
  }

  private static boolean needsAnOwnerOnlyPermission(PermissionContractTests.Operation operation) {
    return operation.permissions().stream()
        .map(key -> Permission.byKey(key).orElseThrow())
        .anyMatch(Permission.ownerOnly()::contains);
  }

  private static String areaOf(String path) {
    String rest = path.substring(STUDIO_PATH.length());
    if (rest.isEmpty()) {
      return "";
    }
    String afterSlash = rest.substring(1);
    int slash = afterSlash.indexOf('/');
    return slash < 0 ? afterSlash : afterSlash.substring(0, slash);
  }
}
