package io.github.aaronfilson.studiomanager.glaze;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.StatementCounts;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The glaze book on Hibernate (ADR 21): lists at a fixed number of statements, versions that move
 * only on a real change (the lines included), and the tenant applied by Hibernate.
 */
@IntegrationTest
class GlazePersistenceTests {

  @Autowired MockMvcTester mvc;
  @Autowired StatementCounts statements;
  @Autowired GlazeBook book;

  private final TestUser owner = TestUser.named("Gale");
  private UUID studioId;
  private UUID ownerAccountId;

  private static final String CELADON =
      """
      {"version": %d, "ownerKind": "STUDIO", "name": "%s", "cone": "10",
       "lines": [
         {"kind": "BASE", "material": "Feldspar", "amount": "40", "unit": "PARTS"},
         {"kind": "BASE", "material": "Silica", "amount": "%s", "unit": "PARTS"}%s
       ]}
      """;

  @BeforeEach
  void studio() throws Exception {
    studioId =
        UUID.fromString(
            read(
                mvc.post()
                    .uri("/api/v1/studios")
                    .with(owner.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name": "Glaze Room", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "GB"}
                        """)
                    .exchange(),
                "$.id"));
    ownerAccountId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
  }

  @Test
  void theStudioGlazesAreTheSameNumberOfQueriesForOneOrMany() throws Exception {
    create("Celadon");
    long forOne = countStudioList();

    create("Shino");
    create("Temmoku");
    StatementCounts.Counted<?> forThree =
        statements.of(
            () -> TenantContext.callAs(inStudio(), () -> book.list(GlazeBook.Filter.STUDIO)));

    assertThat(TenantContext.callAs(inStudio(), () -> book.list(GlazeBook.Filter.STUDIO)))
        .hasSize(3)
        .allSatisfy(recipe -> assertThat(recipe.lines()).hasSize(2));
    assertThat(forThree.statements()).isEqualTo(forOne);
  }

  @Test
  void aChangeToTheLinesAloneIsANewVersionAndSavingNoChangeIsNot() throws Exception {
    String id = create("Celadon");

    MvcTestResult sameAgain = put(id, CELADON.formatted(1, "Celadon", "30", ""));
    assertThat(sameAgain).hasStatusOk().bodyJson().extractingPath("$.version").isEqualTo(1);

    MvcTestResult newAmount = put(id, CELADON.formatted(1, "Celadon", "35", ""));
    assertThat(newAmount).hasStatusOk().bodyJson().extractingPath("$.version").isEqualTo(2);
    assertThat(newAmount).bodyJson().extractingPath("$.lines[1].amount").isEqualTo("35");

    String extraLine =
        ", {\"kind\": \"ADDITION\", \"material\": \"Iron\", \"amount\": \"1.5\", \"unit\": \"PERCENT\"}";
    MvcTestResult added = put(id, CELADON.formatted(2, "Celadon", "35", extraLine));
    assertThat(added).hasStatusOk().bodyJson().extractingPath("$.version").isEqualTo(3);
    assertThat(added).bodyJson().extractingPath("$.lines.length()").isEqualTo(3);

    MvcTestResult removed = put(id, CELADON.formatted(3, "Celadon", "35", ""));
    assertThat(removed).hasStatusOk().bodyJson().extractingPath("$.lines.length()").isEqualTo(2);
    assertThat(get(id)).bodyJson().extractingPath("$.version").isEqualTo(4);
  }

  @Test
  void anotherStudioSeesNoneOfTheseGlazes() throws Exception {
    String id = create("Celadon");
    TestUser stranger = TestUser.named("Sol");
    UUID theirStudio =
        UUID.fromString(
            read(
                mvc.post()
                    .uri("/api/v1/studios")
                    .with(stranger.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name": "Elsewhere", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "GB"}
                        """)
                    .exchange(),
                "$.id"));
    UUID theirAccount =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(stranger.token()).exchange(), "$.account.id"));
    TenantContext theirs = new TenantContext(theirAccount, theirStudio, StudioRole.OWNER);

    assertThat(TenantContext.callAs(theirs, () -> book.list(GlazeBook.Filter.STUDIO))).isEmpty();
    assertThat(
            mvc.get()
                .uri("/api/v1/studios/" + theirStudio + "/glazes/" + id)
                .with(stranger.token()))
        .hasStatus(HttpStatus.NOT_FOUND);
  }

  private long countStudioList() throws Exception {
    return statements
        .of(() -> TenantContext.callAs(inStudio(), () -> book.list(GlazeBook.Filter.STUDIO)))
        .statements();
  }

  private TenantContext inStudio() {
    return new TenantContext(ownerAccountId, studioId, StudioRole.OWNER);
  }

  private String create(String name) throws Exception {
    MvcTestResult created =
        mvc.post()
            .uri(path("/glazes"))
            .with(owner.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(CELADON.formatted(0, name, "30", ""))
            .exchange();
    assertThat(created).hasStatus(HttpStatus.CREATED);
    assertThat(created).bodyJson().extractingPath("$.version").isEqualTo(1);
    return read(created, "$.id");
  }

  private MvcTestResult get(String id) {
    return mvc.get().uri(path("/glazes/" + id)).with(owner.token()).exchange();
  }

  private MvcTestResult put(String id, String json) {
    return mvc.put()
        .uri(path("/glazes/" + id))
        .with(owner.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();
  }

  private String path(String suffix) {
    return "/api/v1/studios/" + studioId + suffix;
  }

  private static String read(MvcTestResult result, String jsonPath) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
  }
}
