package io.github.aaronfilson.studiomanager.studio;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The owners' roles page: list, edit, reset and the access log, owners only (plan 21, lane R6). */
@IntegrationTest
@RecordApplicationEvents
class RolesApiTests {

  @Autowired MockMvcTester mvc;
  @Autowired ApplicationEvents events;

  private final TestUser owner = TestUser.named("Olive");
  private final TestUser staff = TestUser.named("Sam");
  private UUID studioId;

  @BeforeEach
  void studio() throws Exception {
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
    String code =
        read(
            post(owner, "/api/v1/studios/" + studioId + "/invites", "{\"role\": \"STAFF\"}"),
            "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(staff.token()))
        .hasStatusOk();
  }

  @Test
  void anOwnerSeesEveryRoleWithTheCatalogue() throws Exception {
    MvcTestResult listed = get(owner, roles(""));

    assertThat(listed).hasStatusOk();
    List<String> roleNames = JsonPath.read(body(listed), "$.roles[*].role");
    assertThat(roleNames).containsExactly("OWNER", "STAFF", "MEMBER");
    assertThat(JsonPath.<Boolean>read(body(listed), "$.roles[0].editable")).isFalse();
    assertThat(JsonPath.<Integer>read(body(listed), "$.roles[1].memberCount")).isEqualTo(1);
    List<String> staffHolds = JsonPath.read(body(listed), "$.roles[1].permissions");
    assertThat(staffHolds)
        .containsExactlyInAnyOrderElementsOf(
            Permission.defaultsFor(StudioRole.STAFF).stream().map(Permission::key).toList());
    List<String> locks =
        JsonPath.read(body(listed), "$.areas[*].permissions[?(@.key == 'owners.manage')].lock");
    assertThat(locks).containsExactly("OWNER_ONLY");
    List<String> floor =
        JsonPath.read(body(listed), "$.areas[*].permissions[?(@.key == 'money.own.read')].lock");
    assertThat(floor).containsExactly("FLOOR");
    List<String> labels =
        JsonPath.read(body(listed), "$.areas[*].permissions[?(@.key == 'pieces.move')].label");
    assertThat(labels).containsExactly("Move pieces");
  }

  @Test
  void anOwnerEditsAndResetsARoleAndTheChangeIsLoggedAndPublished() throws Exception {
    List<String> trimmed =
        Permission.defaultsFor(StudioRole.STAFF).stream()
            .filter(permission -> permission != Permission.MEMBERS_READ)
            .map(Permission::key)
            .toList();

    MvcTestResult saved = put(owner, roles("/STAFF/permissions"), permissionsJson(trimmed));

    assertThat(saved).hasStatusOk();
    List<String> held = JsonPath.read(body(saved), "$.permissions");
    assertThat(held).doesNotContain("members.read").contains("money.own.read");
    assertThat(get(staff, "/api/v1/studios/" + studioId + "/members"))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(
            events.stream(AccessChanged.class)
                .filter(change -> change.kind() == AccessChangeLog.Kind.ROLE_PERMISSIONS))
        .singleElement()
        .satisfies(change -> {
          assertThat(change.kind()).isEqualTo(AccessChangeLog.Kind.ROLE_PERMISSIONS);
          assertThat(change.target()).isEqualTo("staff");
          assertThat(change.removed()).containsExactly("members.read");
          assertThat(change.added()).isEmpty();
        });

    MvcTestResult reset = post(owner, roles("/STAFF/reset"), "");
    assertThat(reset).hasStatusOk();
    List<String> restored = JsonPath.read(body(reset), "$.permissions");
    assertThat(restored).contains("members.read");
    assertThat(get(staff, "/api/v1/studios/" + studioId + "/members")).hasStatusOk();

    MvcTestResult history = get(owner, roles("/history"));
    assertThat(history).hasStatusOk();
    String roleChanges = "$.items[?(@.kind == 'ROLE_PERMISSIONS' && @.target == 'staff')]";
    List<List<String>> added = JsonPath.read(body(history), roleChanges + ".added");
    List<List<String>> removed = JsonPath.read(body(history), roleChanges + ".removed");
    assertThat(added).containsExactlyInAnyOrder(List.of("members.read"), List.of());
    assertThat(removed).containsExactlyInAnyOrder(List.of(), List.of("members.read"));
    List<String> actors = JsonPath.read(body(history), roleChanges + ".actorName");
    assertThat(actors).containsExactly("Olive", "Olive");
    List<Boolean> viaToken = JsonPath.read(body(history), roleChanges + ".viaToken");
    assertThat(viaToken).containsExactly(false, false);
    assertThat(JsonPath.<Boolean>read(body(history), "$.truncated")).isFalse();
  }

  @Test
  void theOwnerRoleOwnerOnlyAndUnknownPermissionsAreRefused() {
    assertThat(put(owner, roles("/OWNER/permissions"), permissionsJson(List.of())))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(post(owner, roles("/OWNER/reset"), "")).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(put(owner, roles("/STAFF/permissions"), permissionsJson(List.of("owners.manage"))))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(put(owner, roles("/MEMBER/permissions"), permissionsJson(List.of("no.such"))))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(
            events.stream(AccessChanged.class)
                .filter(change -> change.kind() == AccessChangeLog.Kind.ROLE_PERMISSIONS))
        .isEmpty();
  }

  @Test
  void staffCanNeitherSeeNorEditRoles() {
    assertThat(get(staff, roles(""))).hasStatus(HttpStatus.FORBIDDEN);
    assertThat(get(staff, roles("/history"))).hasStatus(HttpStatus.FORBIDDEN);
    assertThat(put(staff, roles("/MEMBER/permissions"), permissionsJson(List.of())))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(post(staff, roles("/STAFF/reset"), "")).hasStatus(HttpStatus.FORBIDDEN);
  }

  private String roles(String rest) {
    return "/api/v1/studios/" + studioId + "/roles" + rest;
  }

  private static String permissionsJson(List<String> keys) {
    return "{\"permissions\": ["
        + String.join(",", keys.stream().map(key -> "\"" + key + "\"").toList())
        + "]}";
  }

  private MvcTestResult get(TestUser user, String uri) {
    return mvc.get().uri(uri).with(user.token()).exchange();
  }

  private MvcTestResult post(TestUser user, String uri, String json) {
    return mvc.post()
        .uri(uri)
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();
  }

  private MvcTestResult put(TestUser user, String uri, String json) {
    return mvc.put()
        .uri(uri)
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();
  }

  private static String body(MvcTestResult result) throws Exception {
    return result.getResponse().getContentAsString();
  }

  private static String read(MvcTestResult result, String jsonPath) throws Exception {
    return JsonPath.read(body(result), jsonPath).toString();
  }
}
