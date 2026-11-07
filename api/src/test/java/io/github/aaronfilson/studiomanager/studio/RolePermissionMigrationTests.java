package io.github.aaronfilson.studiomanager.studio;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.RolePermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * V39 gives every existing studio its three roles, holding exactly the code defaults, so nothing
 * changes on day one. Migrates a scratch database in the shared container to V38, adds a studio,
 * then applies V39.
 */
@IntegrationTest
class RolePermissionMigrationTests {

  @Autowired PostgreSQLContainer postgres;

  @Test
  void everyStudioIsSeededWithTheCodeDefaults() throws SQLException {
    String database = "v39_" + UUID.randomUUID().toString().replace("-", "");
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
      jdbc.sql(
              """
              insert into studio (id, name, unit_system, time_zone, country, currency, created_at, version)
              values (:studio, 'Clay Club', 'METRIC', 'Europe/London', 'GB', 'GBP', now(), 0)
              """)
          .param("studio", studio)
          .update();

      migrate(dataSource, "39");

      assertThat(
              jdbc.sql("select key from studio_role where studio_id = :studio and is_system")
                  .param("studio", studio)
                  .query(String.class)
                  .set())
          .containsExactlyInAnyOrder("owner", "staff", "member");
      for (StudioRole role : StudioRole.values()) {
        Set<String> seeded =
            jdbc.sql(
                    """
                    select p.permission from studio_role_permission p
                    join studio_role r on r.id = p.role_id
                    where r.studio_id = :studio and r.base = :base
                    """)
                .param("studio", studio)
                .param("base", role.name())
                .query(String.class)
                .set();
        assertThat(seeded)
            .as("%s", role)
            .isEqualTo(
                RolePermissions.storedDefaults().get(role).stream()
                    .map(Permission::key)
                    .collect(Collectors.toSet()));
      }
    }
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
