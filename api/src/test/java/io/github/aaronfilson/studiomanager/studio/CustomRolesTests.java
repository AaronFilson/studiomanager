package io.github.aaronfilson.studiomanager.studio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Custom roles (plan 21, lane R7): owners make, rename, edit and delete them; staff put members in
 * them, never above their own; a member on one holds exactly its set; invites and tokens carry it.
 */
@IntegrationTest
@RecordApplicationEvents
class CustomRolesTests {

  private static final String KILN_TECH =
      """
      {"name": "Kiln tech", "base": "MEMBER",
       "permissions": ["studio.read", "kilns.read", "kilns.manage", "firings.manage"]}
      """;

  @Autowired MockMvcTester mvc;
  @Autowired JdbcClient jdbc;
  @Autowired TransactionTemplate transactions;
  @Autowired ApplicationEvents events;

  private final TestUser olive = TestUser.named("Olive");
  private final TestUser sam = TestUser.named("Sam");
  private final TestUser mia = TestUser.named("Mia");
  private UUID studioId;
  private UUID oliveId;
  private UUID miaId;

  @BeforeEach
  void studio() throws Exception {
    studioId =
        UUID.fromString(
            read(
                post(
                    olive,
                    "/api/v1/studios",
                    """
                    {"name": "Clay Club", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                    """),
                "$.id"));
    join(sam, "{\"role\": \"STAFF\"}");
    join(mia, "{\"role\": \"MEMBER\"}");
    oliveId = accountOf(olive);
    miaId = accountOf(mia);
  }

  @Test
  void anOwnerMakesARoleAndAMemberOnItHoldsExactlyItsSet() throws Exception {
    MvcTestResult made = post(olive, path("/roles/custom"), KILN_TECH);
    assertThat(made).hasStatus(HttpStatus.CREATED);
    String roleId = read(made, "$.id");
    assertThat(read(made, "$.key")).isEqualTo("kiln_tech");
    assertThat(JsonPath.<Boolean>read(body(made), "$.custom")).isTrue();

    assertThat(setRole(sam, miaId, roleId)).hasStatus(HttpStatus.NO_CONTENT);

    MvcTestResult mine = get(mia, path("/me/permissions"));
    List<String> held = JsonPath.read(body(mine), "$.permissions");
    assertThat(held)
        .containsExactlyInAnyOrder(
            "studio.read", "kilns.read", "kilns.manage", "firings.manage", "money.own.read");
    assertThat(read(mine, "$.role")).isEqualTo("MEMBER");
    assertThat(read(mine, "$.roleName")).isEqualTo("Kiln tech");
    // Members can read pieces; this role can't, and can manage kilns
    assertThat(get(mia, path("/pieces"))).hasStatus(HttpStatus.FORBIDDEN);
    assertThat(post(mia, path("/kilns"), "{\"name\": \"Skutt\", \"kind\": \"ELECTRIC\"}"))
        .hasStatus2xxSuccessful();

    // The members page and the roles page show it
    List<String> roleNames = JsonPath.read(body(get(olive, path("/members"))), "$[*].roleName");
    assertThat(roleNames).contains("Kiln tech");
    List<Integer> counts =
        JsonPath.read(
            body(get(olive, path("/roles"))), "$.roles[?(@.key == 'kiln_tech')].memberCount");
    assertThat(counts).containsExactly(1);
    assertThat(roleOf(miaId))
        .containsEntry("role", "MEMBER")
        .containsEntry("role_id", UUID.fromString(roleId));
  }

  @Test
  void editingARoleAppliesToItsMembersAndRenamingIsLogged() throws Exception {
    String roleId = read(post(olive, path("/roles/custom"), KILN_TECH), "$.id");
    assertThat(setRole(olive, miaId, roleId)).hasStatus(HttpStatus.NO_CONTENT);

    MvcTestResult edited =
        put(
            olive,
            path("/roles/custom/" + roleId),
            "{\"name\": \"Kiln crew\", \"permissions\": [\"studio.read\", \"kilns.read\"]}");

    assertThat(edited).hasStatusOk();
    assertThat(read(edited, "$.name")).isEqualTo("Kiln crew");
    List<String> held = JsonPath.read(body(get(mia, path("/me/permissions"))), "$.permissions");
    assertThat(held).containsExactlyInAnyOrder("studio.read", "kilns.read", "money.own.read");
    List<String> kinds = JsonPath.read(body(get(olive, path("/roles/history"))), "$.items[*].kind");
    assertThat(kinds).contains("ROLE_CREATED", "ROLE_RENAMED", "ROLE_PERMISSIONS", "MEMBER_ROLE");
    assertThat(
            events.stream(AccessChanged.class)
                .filter(change -> change.kind() == AccessChangeLog.Kind.ROLE_RENAMED))
        .singleElement()
        .satisfies(change -> {
          assertThat(change.added()).containsExactly("Kiln crew");
          assertThat(change.removed()).containsExactly("Kiln tech");
        });
  }

  @Test
  void ownerOnlyPermissionsAnOwnerBaseAndDuplicatesAreRefused() {
    assertThat(
            post(
                olive,
                path("/roles/custom"),
                "{\"name\": \"Board\", \"base\": \"STAFF\", \"permissions\": [\"studio.export\"]}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(
            post(
                olive,
                path("/roles/custom"),
                "{\"name\": \"Board\", \"base\": \"OWNER\", \"permissions\": []}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(post(olive, path("/roles/custom"), KILN_TECH)).hasStatus(HttpStatus.CREATED);
    assertThat(
            post(
                olive,
                path("/roles/custom"),
                "{\"name\": \"KILN TECH\", \"base\": \"STAFF\", \"permissions\": []}"))
        .hasStatus(HttpStatus.CONFLICT);
    // Staff can't make roles at all
    assertThat(
            post(
                sam,
                path("/roles/custom"),
                "{\"name\": \"Mine\", \"base\": \"MEMBER\", \"permissions\": []}"))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void theDatabaseRefusesAnOwnerBasedCustomRole() {
    assertThatThrownBy(() ->
            asOwner(() ->
                transactions.execute(tx ->
                    jdbc.sql(
                            """
                            insert into studio_role (id, studio_id, key, name, base, is_system, created_at, updated_at)
                            values (gen_random_uuid(), :studio, 'board', 'Board', 'OWNER', false, now(), now())
                            """)
                        .param("studio", studioId)
                        .update())))
        .hasStackTraceContaining("studio_role_custom_not_owner");
  }

  @Test
  void staffCantGiveARoleAboveTheirOwnOrTouchAnOwner() throws Exception {
    String board =
        read(
            post(
                olive,
                path("/roles/custom"),
                "{\"name\": \"Board\", \"base\": \"STAFF\", \"permissions\": [\"money.read\", \"studio.read\"]}"),
            "$.id");
    // Staff lose money.read: Board now holds something they don't
    List<String> staffWithoutMoney =
        JsonPath.<List<String>>read(body(get(olive, path("/roles"))), "$.roles[1].permissions")
            .stream()
            .filter(key -> !key.equals("money.read") && !key.equals("money.own.read"))
            .toList();
    assertThat(put(olive, path("/roles/STAFF/permissions"), permissionsJson(staffWithoutMoney)))
        .hasStatusOk();

    assertThat(setRole(sam, miaId, board)).hasStatus(HttpStatus.FORBIDDEN);
    List<String> assignable = JsonPath.read(body(get(sam, path("/roles/assignable"))), "$[*].name");
    assertThat(assignable).contains("Member").doesNotContain("Board", "Owner");

    // Nobody moves an owner, or makes one, here
    String member = systemRoleId("member");
    String owner = systemRoleId("owner");
    assertThat(setRole(olive, oliveId, member)).hasStatus(HttpStatus.CONFLICT);
    assertThat(setRole(olive, miaId, owner)).hasStatus(HttpStatus.CONFLICT);
  }

  @Test
  void theLastOwnerTriggerStillHoldsWithRoleIds() throws Exception {
    String staffRole = systemRoleId("staff");
    // Moving the only owner's role row, not the role column, is still refused at commit
    assertThatThrownBy(() ->
            asOwner(() ->
                transactions.execute(tx ->
                    jdbc.sql(
                            "update studio_membership set role_id = :role where studio_id = :studio and account_id = :account")
                        .param("role", UUID.fromString(staffRole))
                        .param("studio", studioId)
                        .param("account", oliveId)
                        .update())))
        .hasStackTraceContaining("Add another owner first");
    assertThat(roleOf(oliveId)).containsEntry("role", "OWNER");

    // Making an owner keeps role and role_id together
    assertThat(mvc.put().uri(path("/members/" + miaId + "/owner")).with(olive.token()))
        .hasStatus(HttpStatus.NO_CONTENT);
    assertThat(roleOf(miaId))
        .containsEntry("role", "OWNER")
        .containsEntry("role_id", UUID.fromString(systemRoleId("owner")));
  }

  @Test
  void anInviteCanNameACustomRole() throws Exception {
    String roleId = read(post(olive, path("/roles/custom"), KILN_TECH), "$.id");
    TestUser nia = TestUser.named("Nia");

    MvcTestResult invited =
        post(olive, path("/invites"), "{\"role\": \"MEMBER\", \"roleId\": \"" + roleId + "\"}");
    assertThat(invited).hasStatus(HttpStatus.CREATED);
    assertThat(read(invited, "$.roleName")).isEqualTo("Kiln tech");
    List<String> open = JsonPath.read(body(get(olive, path("/invites"))), "$[*].roleName");
    assertThat(open).contains("Kiln tech");
    // The base must match
    assertThat(
            post(olive, path("/invites"), "{\"role\": \"STAFF\", \"roleId\": \"" + roleId + "\"}"))
        .hasStatus(HttpStatus.BAD_REQUEST);

    assertThat(
            mvc.post()
                .uri("/api/v1/invites/{code}/accept", read(invited, "$.code"))
                .with(nia.token()))
        .hasStatusOk();
    assertThat(read(get(nia, path("/me/permissions")), "$.roleName")).isEqualTo("Kiln tech");
    assertThat(roleOf(accountOf(nia))).containsEntry("role_id", UUID.fromString(roleId));
  }

  @Test
  void aRoleInUseCantBeDeletedAndAnUnusedOneWithdrawsItsInvites() throws Exception {
    String roleId = read(post(olive, path("/roles/custom"), KILN_TECH), "$.id");
    assertThat(setRole(olive, miaId, roleId)).hasStatus(HttpStatus.NO_CONTENT);
    post(olive, path("/invites"), "{\"role\": \"MEMBER\", \"roleId\": \"" + roleId + "\"}");

    MvcTestResult refused =
        mvc.delete().uri(path("/roles/custom/" + roleId)).with(olive.token()).exchange();
    assertThat(refused).hasStatus(HttpStatus.CONFLICT);
    assertThat(body(refused)).contains("another role first");

    assertThat(setRole(olive, miaId, systemRoleId("member"))).hasStatus(HttpStatus.NO_CONTENT);
    assertThat(mvc.delete().uri(path("/roles/custom/" + roleId)).with(olive.token()))
        .hasStatus(HttpStatus.NO_CONTENT);

    List<String> keys = JsonPath.read(body(get(olive, path("/roles"))), "$.roles[*].key");
    assertThat(keys).containsExactly("owner", "staff", "member");
    assertThat(JsonPath.<List<Object>>read(body(get(olive, path("/invites"))), "$")).isEmpty();
    List<String> kinds = JsonPath.read(body(get(olive, path("/roles/history"))), "$.items[*].kind");
    assertThat(kinds).contains("ROLE_DELETED");
    // The built-in roles can't be deleted
    assertThat(mvc.delete().uri(path("/roles/custom/" + systemRoleId("staff"))).with(olive.token()))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void aTokenOnACustomRoleHoldsThatRoleWithinItsScopes() throws Exception {
    String roleId =
        read(
            post(
                olive,
                path("/roles/custom"),
                """
                {"name": "Assistant user", "base": "MEMBER",
                 "permissions": ["studio.read", "kilns.read", "api_tokens.own", "mcp.use"]}
                """),
            "$.id");
    assertThat(setRole(olive, miaId, roleId)).hasStatus(HttpStatus.NO_CONTENT);

    MvcTestResult created =
        post(mia, path("/api-tokens"), "{\"name\": \"Mine\", \"scopes\": [\"read\"]}");
    assertThat(created).hasStatus2xxSuccessful();
    String token = read(created, "$.secret");

    MvcTestResult mine =
        mvc.get()
            .uri(path("/me/permissions"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .exchange();
    List<String> held = JsonPath.read(body(mine), "$.permissions");
    assertThat(held)
        .contains("kilns.read", "mcp.use")
        .doesNotContain("pieces.read", "api_tokens.own");

    // Moved back to the plain member role, which lacks api_tokens.own: the token stops working
    assertThat(setRole(olive, miaId, systemRoleId("member"))).hasStatus(HttpStatus.NO_CONTENT);
    assertThat(mvc.get().uri(path("/kilns")).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  private void join(TestUser user, String invite) throws Exception {
    String code = read(post(olive, path("/invites"), invite), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(user.token()))
        .hasStatusOk();
  }

  private MvcTestResult setRole(TestUser user, UUID accountId, String roleId) {
    return put(user, path("/members/" + accountId + "/role"), "{\"roleId\": \"" + roleId + "\"}");
  }

  private String systemRoleId(String key) throws Exception {
    List<String> ids =
        JsonPath.read(body(get(olive, path("/roles"))), "$.roles[?(@.key == '" + key + "')].id");
    return ids.getFirst();
  }

  private Map<String, Object> roleOf(UUID accountId) {
    return asOwner(() ->
        transactions.execute(tx ->
            jdbc.sql(
                    "select role, role_id from studio_membership where studio_id = :studio and account_id = :account")
                .param("studio", studioId)
                .param("account", accountId)
                .query()
                .singleRow()));
  }

  private <T> T asOwner(ScopedValue.CallableOp<T, RuntimeException> operation) {
    return TenantContext.callAs(new TenantContext(oliveId, studioId, StudioRole.OWNER), operation);
  }

  private UUID accountOf(TestUser user) throws Exception {
    return UUID.fromString(read(get(user, "/api/v1/me"), "$.account.id"));
  }

  private static String permissionsJson(List<String> keys) {
    return "{\"permissions\": ["
        + String.join(",", keys.stream().map(key -> "\"" + key + "\"").toList())
        + "]}";
  }

  private String path(String suffix) {
    return "/api/v1/studios/" + studioId + suffix;
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
