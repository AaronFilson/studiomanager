package io.github.aaronfilson.studiomanager.studio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A new studio's stored roles hold the code defaults; an owner's change applies on the next request
 * and is in the access log (plan 21, lane R4).
 */
@IntegrationTest
class StudioRolesTests {

  @Autowired MockMvcTester mvc;
  @Autowired StudioRoles studioRoles;
  @Autowired JdbcClient jdbc;
  @Autowired TransactionTemplate transactions;

  private final TestUser owner = TestUser.named("Olive");
  private final TestUser staff = TestUser.named("Sam");
  private UUID studioId;
  private UUID ownerId;
  private UUID staffId;

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
    ownerId = accountOf(owner);
    staffId = accountOf(staff);
  }

  @Test
  void aNewStudioStoresTheCodeDefaults() {
    Map<String, Set<String>> byRole =
        asOwner(() ->
            transactions.execute(tx ->
                jdbc
                    .sql(
                        """
                        select r.key, p.permission
                        from studio_role r
                        left join studio_role_permission p on p.role_id = r.id
                        """)
                    .query((row, number) ->
                        Map.entry(
                            row.getString("key"), String.valueOf(row.getString("permission"))))
                    .list()
                    .stream()
                    .collect(
                        Collectors.groupingBy(
                            Map.Entry::getKey,
                            Collectors.mapping(Map.Entry::getValue, Collectors.toSet())))));
    assertThat(byRole).containsOnlyKeys("owner", "staff", "member");
    assertThat(byRole.get("owner")).containsExactly("null");
    assertThat(byRole.get("staff")).isEqualTo(storedKeys(StudioRole.STAFF));
    assertThat(byRole.get("member")).isEqualTo(storedKeys(StudioRole.MEMBER));
  }

  @Test
  void anOwnersChangeAppliesOnTheNextRequestAndIsLogged() {
    assertThat(members(staff)).hasStatusOk();

    Set<Permission> trimmed = EnumSet.copyOf(Permission.defaultsFor(StudioRole.STAFF));
    trimmed.remove(Permission.MEMBERS_READ);
    Set<Permission> held = asOwner(() -> studioRoles.setPermissions(StudioRole.STAFF, trimmed));

    assertThat(held).isEqualTo(trimmed);
    assertThat(members(staff)).hasStatus(HttpStatus.FORBIDDEN);
    assertThat(myPermissions(staff)).doesNotContain(Permission.MEMBERS_READ.key());

    Map<String, Object> logged =
        asOwner(() ->
            transactions.execute(tx ->
                jdbc.sql(
                        """
                        select actor_account_id, kind, target, before_value, after_value
                        from role_change_log where kind = 'ROLE_PERMISSIONS'
                        """)
                    .query()
                    .singleRow()));
    assertThat(logged).containsEntry("actor_account_id", ownerId).containsEntry("target", "staff");
    assertThat(logged.get("before_value").toString()).contains("members.read");
    assertThat(logged.get("after_value").toString())
        .doesNotContain("members.read")
        .contains("members.invite");

    // Giving it back restores the defaults
    asOwner(() ->
        studioRoles.setPermissions(StudioRole.STAFF, Permission.defaultsFor(StudioRole.STAFF)));
    assertThat(members(staff)).hasStatusOk();
  }

  @Test
  void onlyOwnersChangeRolesAndNeverTheOwnerRoleOrOwnerOnlyPermissions() {
    TenantContext asStaff = new TenantContext(staffId, studioId, StudioRole.STAFF);
    assertThatThrownBy(() ->
            TenantContext.callAs(
                asStaff, () -> studioRoles.setPermissions(StudioRole.MEMBER, Set.of())))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> asOwner(() -> studioRoles.setPermissions(StudioRole.OWNER, Set.of())))
        .isInstanceOf(InvalidInputException.class);
    assertThatThrownBy(() ->
            asOwner(() ->
                studioRoles.setPermissions(
                    StudioRole.STAFF, Set.of(Permission.SUPPLIES_PURCHASE_APPROVE))))
        .isInstanceOf(InvalidInputException.class);
    assertThat(logCount()).isZero();
  }

  @Test
  void aDutyChangeIsLogged() {
    assertThat(
            mvc.put()
                .uri("/api/v1/studios/{studioId}/members/{accountId}/duties", studioId, staffId)
                .with(owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duties\": [\"MONITOR\", \"TECHNICIAN\"]}"))
        .hasStatusOk();

    Map<String, Object> logged =
        asOwner(() ->
            transactions.execute(tx ->
                jdbc.sql(
                        """
                        select target, before_value, after_value
                        from role_change_log where kind = 'MEMBER_DUTIES'
                        """)
                    .query()
                    .singleRow()));
    assertThat(logged)
        .containsEntry("target", staffId.toString())
        .containsEntry("before_value", "")
        .containsEntry("after_value", "MONITOR,TECHNICIAN");
  }

  private long logCount() {
    Long count =
        asOwner(() ->
            transactions.execute(
                tx -> jdbc.sql("select count(*) from role_change_log").query(Long.class).single()));
    return count == null ? 0 : count;
  }

  private static Set<String> storedKeys(StudioRole role) {
    return Permission.defaultsFor(role).stream()
        .filter(permission -> !permission.isFloor())
        .map(Permission::key)
        .collect(Collectors.toSet());
  }

  private <T> T asOwner(ScopedValue.CallableOp<T, RuntimeException> operation) {
    return TenantContext.callAs(new TenantContext(ownerId, studioId, StudioRole.OWNER), operation);
  }

  private MvcTestResult members(TestUser user) {
    return mvc.get()
        .uri("/api/v1/studios/{studioId}/members", studioId)
        .with(user.token())
        .exchange();
  }

  private List<String> myPermissions(TestUser user) {
    MvcTestResult result =
        mvc.get()
            .uri("/api/v1/studios/{studioId}/me/permissions", studioId)
            .with(user.token())
            .exchange();
    try {
      return JsonPath.read(result.getResponse().getContentAsString(), "$.permissions");
    } catch (java.io.UnsupportedEncodingException unreadable) {
      throw new IllegalStateException(unreadable);
    }
  }

  private UUID accountOf(TestUser user) throws Exception {
    return UUID.fromString(
        read(mvc.get().uri("/api/v1/me").with(user.token()).exchange(), "$.account.id"));
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
