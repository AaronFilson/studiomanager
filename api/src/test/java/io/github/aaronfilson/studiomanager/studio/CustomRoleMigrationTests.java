package io.github.aaronfilson.studiomanager.studio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aaronfilson.studiomanager.IntegrationTest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * V41 points every existing membership at its system role row and keeps {@code role} and {@code
 * role_id} in step from then on. Migrates a scratch database in the shared container to V40, adds a
 * studio with an owner and a member, then applies V41.
 */
@IntegrationTest
class CustomRoleMigrationTests {

  @Autowired PostgreSQLContainer postgres;

  @Test
  void membershipsGetTheirRoleRowAndStayInStep() throws SQLException {
    String database = "v41_" + UUID.randomUUID().toString().replace("-", "");
    try (Connection admin =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var statement = admin.createStatement()) {
      statement.execute("CREATE DATABASE " + database);
    }
    String url = postgres.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/" + database + "$1");
    try (var dataSource =
        new SingleConnectionDataSource(url, postgres.getUsername(), postgres.getPassword(), true)) {
      migrate(dataSource, "38");
      JdbcClient jdbc = JdbcClient.create(dataSource);
      UUID studio = UUID.randomUUID();
      UUID owner = UUID.randomUUID();
      UUID member = UUID.randomUUID();
      jdbc.sql(
              """
              insert into account (id, issuer, subject) values (:owner, 'https://issuer.test/', 'olive'),
                                                               (:member, 'https://issuer.test/', 'mia');
              insert into studio (id, name, unit_system, time_zone, country, currency, created_at, version)
              values (:studio, 'Clay Club', 'METRIC', 'Europe/London', 'GB', 'GBP', now(), 0);
              insert into studio_membership (id, studio_id, account_id, role, created_at, version)
              values (gen_random_uuid(), :studio, :owner, 'OWNER', now(), 0),
                     (gen_random_uuid(), :studio, :member, 'MEMBER', now(), 0);
              """)
          .param("studio", studio)
          .param("owner", owner)
          .param("member", member)
          .update();

      migrate(dataSource, "41");

      assertThat(roleKeyOf(jdbc, studio, owner))
          .containsEntry("role", "OWNER")
          .containsEntry("key", "owner");
      assertThat(roleKeyOf(jdbc, studio, member))
          .containsEntry("role", "MEMBER")
          .containsEntry("key", "member");

      // A custom role at STAFF: pointing at it sets the role column to its base
      UUID crew = UUID.randomUUID();
      jdbc.sql(
              """
              insert into studio_role (id, studio_id, key, name, base, is_system, created_at, updated_at)
              values (:crew, :studio, 'kiln_crew', 'Kiln crew', 'STAFF', false, now(), now())
              """)
          .param("crew", crew)
          .param("studio", studio)
          .update();
      jdbc.sql("update studio_membership set role_id = :crew where account_id = :member")
          .param("crew", crew)
          .param("member", member)
          .update();
      assertThat(roleKeyOf(jdbc, studio, member))
          .containsEntry("role", "STAFF")
          .containsEntry("key", "kiln_crew");

      // Changing the role column alone moves the row to that base's system role
      jdbc.sql("update studio_membership set role = 'MEMBER' where account_id = :member")
          .param("member", member)
          .update();
      assertThat(roleKeyOf(jdbc, studio, member))
          .containsEntry("role", "MEMBER")
          .containsEntry("key", "member");

      // An owner-based custom role is refused
      assertThatThrownBy(() ->
              jdbc.sql(
                      """
                      insert into studio_role (id, studio_id, key, name, base, is_system, created_at, updated_at)
                      values (gen_random_uuid(), :studio, 'board', 'Board', 'OWNER', false, now(), now())
                      """)
                  .param("studio", studio)
                  .update())
          .hasMessageContaining("studio_role_custom_not_owner");
    }
  }

  private static Map<String, Object> roleKeyOf(JdbcClient jdbc, UUID studio, UUID account) {
    return jdbc.sql(
            """
            select m.role, r.key from studio_membership m
            join studio_role r on r.id = m.role_id and r.studio_id = m.studio_id
            where m.studio_id = :studio and m.account_id = :account
            """)
        .param("studio", studio)
        .param("account", account)
        .query()
        .singleRow();
  }

  private static void migrate(DataSource dataSource, String target) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .target(target)
        .load()
        .migrate();
  }
}
