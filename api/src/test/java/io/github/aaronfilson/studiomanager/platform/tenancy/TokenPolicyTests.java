package io.github.aaronfilson.studiomanager.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TokenPolicyTests {

  private static final Set<TokenScope> READ = Set.of(TokenScope.READ);

  @Test
  void everyAreaInTheContractIsDecidedOn() throws IOException {
    String spec = Files.readString(Path.of("src/main/openapi/studiomanager.yaml"));
    Matcher paths =
        Pattern.compile("(?m)^  /api/v1/studios/\\{studioId\\}(?:/([a-z-]+))?").matcher(spec);
    Set<String> areas = new TreeSet<>();
    while (paths.find()) {
      areas.add(paths.group(1) == null ? "" : paths.group(1));
    }
    assertThat(areas).isNotEmpty();
    // A new area must be added to TokenPolicy on purpose: read only, a write scope, or never
    assertThat(TokenPolicy.AREAS.keySet()).containsAll(areas);
  }

  @Test
  void readingNeedsTheReadScope() {
    assertThat(TokenPolicy.refusal(READ, "GET", "pieces")).isEmpty();
    assertThat(TokenPolicy.refusal(READ, "GET", "")).isEmpty();
    assertThat(TokenPolicy.refusal(Set.of(TokenScope.WRITE_PIECES), "GET", "pieces")).isPresent();
  }

  @Test
  void writingNeedsThatAreasScope() {
    assertThat(TokenPolicy.refusal(READ, "POST", "pieces"))
        .hasValue("This token needs the write:pieces scope.");
    assertThat(TokenPolicy.refusal(Set.of(TokenScope.WRITE_PIECES), "POST", "pieces")).isEmpty();
    assertThat(TokenPolicy.refusal(Set.of(TokenScope.WRITE_PIECES), "DELETE", "supplies"))
        .isPresent();
    assertThat(TokenPolicy.refusal(Set.of(TokenScope.values()), "PUT", "today")).isPresent();
  }

  @Test
  void moneyAndAdministrationAreNeverReachable() {
    Set<TokenScope> everything = Set.of(TokenScope.values());
    for (String area :
        new String[] {
          "ledger",
          "payouts",
          "payees",
          "balances",
          "export",
          "webhooks",
          "invites",
          "imports",
          "api-tokens"
        }) {
      assertThat(TokenPolicy.refusal(everything, "GET", area)).as(area).isPresent();
      assertThat(TokenPolicy.refusal(everything, "POST", area)).as(area).isPresent();
    }
  }

  @Test
  void anUnknownAreaOrNoStudioIsRefused() {
    Set<TokenScope> everything = Set.of(TokenScope.values());
    assertThat(TokenPolicy.refusal(everything, "GET", "something-new")).isPresent();
    assertThat(TokenPolicy.refusal(everything, "GET", null)).isPresent();
  }
}
