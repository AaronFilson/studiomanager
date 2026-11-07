package io.github.aaronfilson.studiomanager.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Each role sees exactly its default permissions, by key, plus what its duties grant, and a
 * stranger sees nothing.
 */
@IntegrationTest
class MyPermissionsApiTests {

  @Autowired MockMvcTester mvc;

  private final TestUser owner = TestUser.named("Olive");
  private final TestUser staff = TestUser.named("Sam");
  private final TestUser member = TestUser.named("Mo");
  private final TestUser stranger = TestUser.named("Nia");
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
    join(staff, "STAFF");
    join(member, "MEMBER");
  }

  @Test
  void anOwnerHoldsEveryPermission() {
    assertPermissions(owner, StudioRole.OWNER, "Owner");
  }

  @Test
  void staffHoldTheStaffDefaults() {
    assertPermissions(staff, StudioRole.STAFF, "Staff");
  }

  @Test
  void aMemberHoldsTheMemberDefaults() {
    assertPermissions(member, StudioRole.MEMBER, "Member");
  }

  @Test
  void listsTheCallersDuties() throws Exception {
    assertThat(
            mvc.put()
                .uri(
                    "/api/v1/studios/{studioId}/members/{accountId}/duties",
                    studioId,
                    accountOf(member))
                .with(owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duties\": [\"TECHNICIAN\"]}"))
        .hasStatusOk();

    assertThat(myPermissions(member))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.duties")
        .asArray()
        .containsExactly("TECHNICIAN");
  }

  @Test
  void aDutyAddsWhatItGrants() throws Exception {
    assertThat(
            mvc.put()
                .uri(
                    "/api/v1/studios/{studioId}/members/{accountId}/duties",
                    studioId,
                    accountOf(member))
                .with(owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duties\": [\"MONITOR\"]}"))
        .hasStatusOk();

    Set<Permission> held = EnumSet.noneOf(Permission.class);
    held.addAll(Permission.defaultsFor(StudioRole.MEMBER));
    held.addAll(Duty.MONITOR.grants());
    List<String> expected = held.stream().map(Permission::key).sorted().toList();
    assertThat(myPermissions(member))
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying(
            "$.permissions",
            value -> assertThat(value).asArray().containsExactlyElementsOf(expected));
    // Monitors take in pieces for others, which plain members can't
    assertThat(expected).contains(Permission.PIECES_INTAKE_FOR_OTHERS.key());
  }

  @Test
  void aStrangerIsRefused() {
    assertThat(myPermissions(stranger)).hasStatus(HttpStatus.FORBIDDEN);
  }

  private void assertPermissions(TestUser user, StudioRole role, String roleName) {
    List<String> expected =
        Permission.defaultsFor(role).stream().map(Permission::key).sorted().toList();
    assertThat(myPermissions(user))
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$.role", value -> assertThat(value).isEqualTo(role.name()))
        .hasPathSatisfying("$.roleName", value -> assertThat(value).isEqualTo(roleName))
        .hasPathSatisfying(
            "$.permissions",
            value -> assertThat(value).asArray().containsExactlyElementsOf(expected))
        .hasPathSatisfying("$.duties", value -> assertThat(value).asArray().isEmpty());
  }

  private MvcTestResult myPermissions(TestUser user) {
    return mvc.get()
        .uri("/api/v1/studios/{studioId}/me/permissions", studioId)
        .with(user.token())
        .exchange();
  }

  private void join(TestUser user, String role) throws Exception {
    String code =
        read(
            post(
                owner,
                "/api/v1/studios/" + studioId + "/invites",
                "{\"role\": \"%s\"}".formatted(role)),
            "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(user.token()))
        .hasStatusOk();
  }

  private String accountOf(TestUser user) throws Exception {
    return read(mvc.get().uri("/api/v1/me").with(user.token()).exchange(), "$.account.id");
  }

  private MvcTestResult post(TestUser user, String uri, String json) {
    return mvc.post()
        .uri(uri)
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();
  }

  private static String read(MvcTestResult result, String jsonPath) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
  }
}
