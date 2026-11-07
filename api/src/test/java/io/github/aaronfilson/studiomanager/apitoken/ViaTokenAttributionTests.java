package io.github.aaronfilson.studiomanager.apitoken;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Histories name the token a change came through: "Olive via Sync script". A change made in the app
 * names no token.
 */
@IntegrationTest
class ViaTokenAttributionTests {

  @Autowired MockMvcTester mvc;

  private final TestUser owner = TestUser.named("Olive");
  private UUID studioId;
  private String syncScript;

  @BeforeEach
  void setUp() {
    studioId =
        UUID.fromString(
            read(
                post(
                    owner,
                    "/api/v1/studios",
                    """
                    {"name": "Clay Club", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                    """),
                "$.id"));
    syncScript =
        read(
            post(
                owner,
                path("/api-tokens"),
                """
                {"name": "Sync script", "scopes": ["read", "write:pieces", "write:maintenance", "write:supplies"]}
                """),
            "$.secret");
  }

  @Test
  void aPieceMadeWithATokenShowsTheTokenInItsTimeline() {
    MvcTestResult created = postWithToken(syncScript, path("/pieces"), "{\"size\": {}}");
    assertThat(created).hasStatus(HttpStatus.CREATED);
    String pieceId = read(created, "$.id");

    MvcTestResult piece = mvc.get().uri(path("/pieces/" + pieceId)).with(owner.token()).exchange();
    assertThat(piece).bodyJson().extractingPath("$.timeline[0].actorName").isEqualTo("Olive");
    assertThat(piece).bodyJson().extractingPath("$.timeline[0].viaToken").isEqualTo("Sync script");
  }

  @Test
  void aPieceMadeInTheAppNamesNoToken() {
    String pieceId = read(post(owner, path("/pieces"), "{\"size\": {}}"), "$.id");

    String piece = body(mvc.get().uri(path("/pieces/" + pieceId)).with(owner.token()).exchange());
    Map<String, Object> received = JsonPath.read(piece, "$.timeline[0]");
    assertThat(received).containsEntry("actorName", "Olive");
    assertThat(received.get("viaToken")).isNull();
  }

  @Test
  void workLoggedWithATokenShowsTheTokenEvenAfterItIsRevoked() {
    String booth =
        read(
            post(
                owner,
                path("/maintenance/assets"),
                "{\"kind\": \"SPRAY_BOOTH\", \"name\": \"Booth\"}"),
            "$.id");
    String records = path("/maintenance/assets/" + booth + "/records");
    assertThat(postWithToken(syncScript, records, "{\"note\": \"New filter\"}"))
        .hasStatus(HttpStatus.CREATED);
    assertThat(post(owner, records, "{\"note\": \"Wiped down\"}")).hasStatus(HttpStatus.CREATED);

    String tokenId =
        JsonPath.read(
            body(mvc.get().uri(path("/api-tokens")).with(owner.token()).exchange()), "$[0].id");
    assertThat(mvc.delete().uri(path("/api-tokens/" + tokenId)).with(owner.token()))
        .hasStatus(HttpStatus.NO_CONTENT);

    String history = body(mvc.get().uri(records).with(owner.token()).exchange());
    // Newest first
    List<String> notes = JsonPath.read(history, "$.items[*].note");
    assertThat(notes).containsExactly("Wiped down", "New filter");
    Map<String, Object> byApp = JsonPath.read(history, "$.items[0]");
    assertThat(byApp.get("viaToken")).isNull();
    assertThat(JsonPath.<String>read(history, "$.items[1].doneByName")).isEqualTo("Olive");
    assertThat(JsonPath.<String>read(history, "$.items[1].viaToken")).isEqualTo("Sync script");
  }

  @Test
  void aPurchaseRequestNamesTheTokenItWasAskedAndDecidedThrough() {
    String clay =
        read(
            post(
                owner,
                path("/supplies"),
                """
                {"name": "B-Mix 5", "category": "CLAY", "unit": "25 lb bag", "supplier": "Laguna",
                 "leadTimeDays": 7, "parLevel": "5", "reorderQuantity": "10"}
                """),
            "$.id");
    MvcTestResult asked =
        postWithToken(
            syncScript, path("/purchase-requests"), "{\"itemId\": \"%s\"}".formatted(clay));
    assertThat(asked).hasStatus(HttpStatus.CREATED);
    assertThat(asked).bodyJson().extractingPath("$.requestedByName").isEqualTo("Olive");
    assertThat(asked).bodyJson().extractingPath("$.requestedViaToken").isEqualTo("Sync script");
    String requestId = read(asked, "$.id");

    MvcTestResult approved =
        mvc.put()
            .uri(path("/purchase-requests/" + requestId + "/status"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + syncScript)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"to\": \"APPROVED\", \"expectedVersion\": 0}")
            .exchange();
    assertThat(approved).hasStatusOk();
    assertThat(approved).bodyJson().extractingPath("$.decidedByName").isEqualTo("Olive");
    assertThat(approved).bodyJson().extractingPath("$.decidedViaToken").isEqualTo("Sync script");

    // Asked for in the app: no token on either side
    MvcTestResult inApp =
        post(owner, path("/purchase-requests"), "{\"itemId\": \"%s\"}".formatted(clay));
    Map<String, Object> made = JsonPath.read(body(inApp), "$");
    assertThat(made).containsEntry("requestedByName", "Olive");
    assertThat(made.get("requestedViaToken")).isNull();
    assertThat(made.get("decidedViaToken")).isNull();
  }

  private MvcTestResult postWithToken(String secret, String uri, String body) {
    return mvc.post()
        .uri(uri)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secret)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .exchange();
  }

  private MvcTestResult post(TestUser user, String uri, String body) {
    return mvc.post()
        .uri(uri)
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .exchange();
  }

  private String path(String suffix) {
    return "/api/v1/studios/" + studioId + suffix;
  }

  private static String body(MvcTestResult result) {
    try {
      return result.getResponse().getContentAsString();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String read(MvcTestResult result, String jsonPath) {
    return JsonPath.read(body(result), jsonPath).toString();
  }
}
