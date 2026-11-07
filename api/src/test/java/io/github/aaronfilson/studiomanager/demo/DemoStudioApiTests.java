package io.github.aaronfilson.studiomanager.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Resetting the demo studio and the token for it. The seed goes through every module's services, so
 * this is also what notices when a change to one of them breaks the seed.
 */
@IntegrationTest
class DemoStudioApiTests {

  @Autowired MockMvcTester mvc;

  private final TestUser admin = TestUser.named("Ada");
  private final TestUser owner = TestUser.named("Olive");

  @Test
  void aResetSucceedsTwiceInARowAndTheSeedReadsBackThroughTheApi() {
    UUID first = reset();
    UUID second = reset();

    assertThat(second).isNotEqualTo(first);
    // The first studio is gone, with the admin's membership in it
    List<String> ids = readList(get("/api/v1/me"), "$.memberships[*].studio.id");
    assertThat(ids).contains(second.toString()).doesNotContain(first.toString());

    String studio = "/api/v1/studios/" + second;
    assertThat(read(get(studio), "$.name")).isEqualTo(DemoStudio.NAME);
    // Five seeded people and the admin who reset it
    assertThat(readList(get(studio + "/members"), "$[*]")).hasSize(6);

    MvcTestResult pieces = get(studio + "/pieces");
    assertThat(readList(pieces, "$.items[*]")).hasSize(12);
    assertThat(stage(pieces, "PICKED_UP")).hasSize(2);
    assertThat(stage(pieces, "READY")).hasSize(2);
    assertThat(stage(pieces, "GLAZED")).hasSize(2);
    assertThat(stage(pieces, "BISQUE_FIRED")).hasSize(2);
    assertThat(stage(pieces, "RECEIVED")).hasSize(4);

    MvcTestResult kilns = get(studio + "/kilns");
    assertThat(readList(kilns, "$[*]")).hasSize(3);
    String bigKiln = readList(kilns, "$[?(@.name == 'Big Skutt')].id").getFirst();
    assertThat(readList(get(studio + "/kilns/" + bigKiln + "/firings"), "$.items[*]")).hasSize(2);
    assertThat(readList(kilns, "$[?(@.name == 'Test kiln')].downReason")).hasSize(1);

    assertThat(readList(get(studio + "/glazes?owner=STUDIO"), "$[*]")).hasSize(3);
    assertThat(readList(get(studio + "/glazes?owner=SHARED"), "$[*]")).hasSize(1);
    assertThat(readList(get(studio + "/maintenance/assets"), "$[*]")).hasSize(6);
    assertThat(readList(get(studio + "/supplies"), "$[*]")).hasSize(7);
    assertThat(readList(get(studio + "/purchase-requests"), "$[*]")).hasSize(3);
    // Fees and sales charged to members, less what they paid
    assertThat(readList(get(studio + "/balances"), "$[*]")).isNotEmpty();
  }

  @Test
  void aResetLeavesEveryOtherStudioAlone() {
    UUID other =
        UUID.fromString(
            read(
                mvc.post()
                    .uri("/api/v1/studios")
                    .with(owner.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name": "Demo Clay Co.", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                        """)
                    .exchange(),
                "$.id"));

    reset();
    reset();

    assertThat(mvc.get().uri("/api/v1/studios/" + other).with(owner.token())).hasStatusOk();
  }

  @Test
  void aDemoTokenReadsTheDemoStudioButCannotWrite() {
    UUID studioId = reset();

    MvcTestResult created =
        mvc.post().uri("/api/v1/admin/demo-studio/tokens").with(admin.adminToken()).exchange();
    assertThat(created).hasStatus(HttpStatus.CREATED);
    assertThat(read(created, "$.studioId")).isEqualTo(studioId.toString());
    String bearer = "Bearer " + read(created, "$.secret");

    String kilns = "/api/v1/studios/" + studioId + "/kilns";
    assertThat(mvc.get().uri(kilns).header("Authorization", bearer)).hasStatusOk();
    assertThat(
            mvc.post()
                .uri(kilns)
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Raku barrel\", \"kind\": \"RAKU\"}"))
        .hasStatus(HttpStatus.FORBIDDEN);

    // A reset deletes the studio, and its tokens with it
    reset();
    assertThat(mvc.get().uri(kilns).header("Authorization", bearer))
        .hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void onlyAPlatformAdminCanResetOrMakeAToken() {
    assertThat(mvc.post().uri("/api/v1/admin/demo-studio/reset").with(owner.token()))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(mvc.post().uri("/api/v1/admin/demo-studio/tokens").with(owner.token()))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  private UUID reset() {
    MvcTestResult reset =
        mvc.post().uri("/api/v1/admin/demo-studio/reset").with(admin.adminToken()).exchange();
    assertThat(reset).hasStatusOk();
    assertThat(read(reset, "$.name")).isEqualTo(DemoStudio.NAME);
    return UUID.fromString(read(reset, "$.id"));
  }

  private MvcTestResult get(String uri) {
    MvcTestResult result = mvc.get().uri(uri).with(admin.token()).exchange();
    assertThat(result).hasStatusOk();
    return result;
  }

  private static List<String> stage(MvcTestResult pieces, String stage) {
    return readList(pieces, "$.items[?(@.stage == '%s')].id".formatted(stage));
  }

  private static String read(MvcTestResult result, String jsonPath) {
    try {
      return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static List<String> readList(MvcTestResult result, String jsonPath) {
    try {
      List<Object> found = JsonPath.read(result.getResponse().getContentAsString(), jsonPath);
      return found.stream().map(String::valueOf).toList();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
