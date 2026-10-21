package io.github.aaronfilson.studiomanager.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import io.github.aaronfilson.studiomanager.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * V12 moves kiln maintenance into the maintenance tables. Migrates a scratch database in the
 * shared container to V11, seeds kilns the old way, then applies V12.
 */
@IntegrationTest
class MaintenanceMigrationTests {

    @Autowired
    PostgreSQLContainer postgres;

    @Test
    void eachKilnBecomesAnAssetItsThresholdATaskAndItsLogRecords() throws SQLException {
        String database = "v12_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword())) {
            admin.createStatement().execute("CREATE DATABASE " + database);
        }
        String url = postgres.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/" + database + "$1");
        try (var dataSource = new SingleConnectionDataSource(url, postgres.getUsername(), postgres.getPassword(), true)) {
            migrate(dataSource, "11");
            JdbcClient jdbc = JdbcClient.create(dataSource);
            UUID studio = UUID.randomUUID();
            UUID account = UUID.randomUUID();
            UUID tracked = UUID.randomUUID();
            UUID untracked = UUID.randomUUID();
            jdbc.sql("""
                    insert into account (id, issuer, subject) values (:account, 'https://issuer.test/', 'olive');
                    insert into studio (id, name, unit_system, time_zone, country, currency, created_at, version)
                    values (:studio, 'Clay Club', 'METRIC', 'Europe/London', 'GB', 'GBP', now(), 0);
                    insert into kiln (id, studio_id, name, kind, maintenance_every_firings, firings_since_maintenance,
                                      total_firings, created_at, version)
                    values (:tracked, :studio, 'Skutt', 'ELECTRIC', 10, 5, 25, now() - interval '1 year', 0),
                           (:untracked, :studio, 'Raku', 'RAKU', null, 3, 3, now(), 0);
                    insert into kiln_maintenance (id, studio_id, kiln_id, note, cost, at_firing, performed_by, performed_at)
                    values (gen_random_uuid(), :studio, :tracked, 'New elements', 180.00, 20, :account,
                            now() - interval '1 month');
                    """)
                    .param("studio", studio).param("account", account)
                    .param("tracked", tracked).param("untracked", untracked)
                    .update();

            migrate(dataSource, "12");

            assertThat(jdbc.sql("select kiln_id from asset where kind = 'KILN' and name is null").query(UUID.class).set())
                    .containsExactlyInAnyOrder(tracked, untracked);
            Map<String, Object> task = jdbc.sql("""
                    select t.title, t.every_firings, t.every_days, t.last_done_firings,
                           t.last_done_at > now() - interval '2 months' as recent
                    from maintenance_task t join asset a on a.id = t.asset_id
                    """).query().singleRow();
            // The clock stands where the old counter stood: 25 firings, 5 since maintenance
            assertThat(task).containsEntry("title", "Kiln maintenance").containsEntry("every_firings", 10)
                    .containsEntry("last_done_firings", 20).containsEntry("recent", true);
            assertThat(task.get("every_days")).isNull();
            List<Map<String, Object>> records = jdbc.sql("""
                    select r.note, r.at_firing, r.task_id is not null as on_task from maintenance_record r
                    """).query().listOfRows();
            assertThat(records).singleElement().satisfies(r -> assertThat(r)
                    .containsEntry("note", "New elements").containsEntry("at_firing", 20).containsEntry("on_task", true));
            assertThat(jdbc.sql("""
                    select count(*) from information_schema.columns
                    where table_name = 'kiln' and column_name in ('maintenance_every_firings', 'firings_since_maintenance')
                    """).query(Integer.class).single()).isZero();
        }
    }

    private static void migrate(DataSource dataSource, String target) {
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target(target).load().migrate();
    }

}
