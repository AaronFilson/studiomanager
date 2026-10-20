package io.github.aaronfilson.studiomanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionOperations;

/**
 * One studio's data is invisible to another studio's members: first through the API, then
 * at the database, where row-level security holds even if application code forgets a filter.
 */
@IntegrationTest
class TenantIsolationTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    TransactionOperations transactions;

    private final TestUser alice = TestUser.named("Alice");
    private final TestUser bob = TestUser.named("Bob");

    @Test
    void membersCanOpenOnlyTheirOwnStudios() throws Exception {
        UUID alicesStudio = createStudio(alice, "Alice's Clay");
        UUID bobsStudio = createStudio(bob, "Bob's Kiln");

        assertThat(mvc.get().uri("/api/v1/studios/{id}", alicesStudio).with(alice.token()))
                .hasStatusOk()
                .bodyJson().extractingPath("$.name").isEqualTo("Alice's Clay");
        assertThat(mvc.get().uri("/api/v1/studios/{id}", bobsStudio).with(alice.token()))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void myStudiosListsOnlyTheStudiosIBelongTo() throws Exception {
        createStudio(alice, "Alice's Clay");
        createStudio(bob, "Bob's Kiln");

        assertThat(mvc.get().uri("/api/v1/me").with(alice.token()))
                .hasStatusOk()
                .bodyJson().extractingPath("$.memberships[*].studio.name").asArray().containsExactly("Alice's Clay");
    }

    @Test
    void theDatabaseEnforcesIsolationOnItsOwn() throws Exception {
        UUID alicesStudio = createStudio(alice, "Alice's Clay");
        UUID bobsStudio = createStudio(bob, "Bob's Kiln");
        UUID aliceId = accountIdOf(alice);
        TenantContext inAlicesStudio = TenantContext.forAccount(aliceId).inStudio(alicesStudio, StudioRole.OWNER);

        // No WHERE clause, and still only Alice's studio comes back
        assertThat(inTenant(inAlicesStudio, () -> jdbc.sql("select id from studio").query(UUID.class).list()))
                .containsExactly(alicesStudio);

        // Writing into Bob's studio fails the policy's WITH CHECK
        assertThatThrownBy(() -> inTenant(inAlicesStudio, () -> jdbc.sql("""
                        insert into studio_membership (id, studio_id, account_id, role, created_at, version)
                        values (:id, :studioId, :accountId, 'OWNER', now(), 0)
                        """)
                .param("id", Uuids.v7())
                .param("studioId", bobsStudio)
                .param("accountId", aliceId)
                .update()))
                // SQLSTATE 42501, which Spring files under BadSqlGrammarException; the cause says why
                .rootCause()
                .hasMessageContaining("new row violates row-level security policy for table \"studio_membership\"");

        // Outside a transaction no tenant is bound, so no studio is visible at all
        assertThat(jdbc.sql("select count(*) from studio").query(Long.class).single()).isZero();
    }

    @Test
    void piecesFeesAndInvitesAreIsolatedToo() throws Exception {
        UUID alicesStudio = createStudio(alice, "Alice's Clay");
        UUID bobsStudio = createStudio(bob, "Bob's Kiln");
        for (UUID studio : List.of(alicesStudio, bobsStudio)) {
            TestUser owner = studio.equals(alicesStudio) ? alice : bob;
            assertThat(mvc.post().uri("/api/v1/studios/{id}/pieces", studio).with(owner.token())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"size\": {}}")).hasStatus(HttpStatus.CREATED);
            assertThat(mvc.put().uri("/api/v1/studios/{id}/fee-schedule", studio).with(owner.token())
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"ruleType": "FLAT", "rate": "5", "rounding": "NONE", "minimum": "0"}
                            """)).hasStatusOk();
            assertThat(mvc.post().uri("/api/v1/studios/{id}/invites", studio).with(owner.token())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"role\": \"MEMBER\"}"))
                    .hasStatus(HttpStatus.CREATED);
        }
        TenantContext inAlicesStudio = TenantContext.forAccount(accountIdOf(alice)).inStudio(alicesStudio, StudioRole.OWNER);

        for (String table : List.of("piece", "piece_event", "piece_counter", "fee_schedule", "studio_invite")) {
            assertThat(inTenant(inAlicesStudio, () -> jdbc.sql("select distinct studio_id from " + table)
                    .query(UUID.class).list()))
                    .as(table)
                    .containsExactly(alicesStudio);
        }
    }

    @Test
    void everyStudioOwnedTableHasRowLevelSecurityAndAPolicy() {
        // A table with a studio_id but no RLS would silently leak across tenants; fail the build instead
        List<String> unprotected = jdbc.sql("""
                select c.relname
                from pg_class c
                join pg_namespace n on n.oid = c.relnamespace
                join pg_attribute a on a.attrelid = c.oid and a.attname in ('studio_id') and not a.attisdropped
                where n.nspname = 'public' and c.relkind = 'r'
                  and (not c.relrowsecurity
                       or not exists (select 1 from pg_policy p where p.polrelid = c.oid))
                order by c.relname
                """).query(String.class).list();
        int protectedTables = jdbc.sql("""
                select count(*) from pg_class c join pg_namespace n on n.oid = c.relnamespace
                where n.nspname = 'public' and c.relkind = 'r' and c.relrowsecurity
                """).query(Integer.class).single();

        assertThat(unprotected).isEmpty();
        // The guard itself works: the schema really has its protected tables
        assertThat(protectedTables).isGreaterThanOrEqualTo(25);
    }

    private <T> T inTenant(TenantContext context, Supplier<T> work) {
        return TenantContext.callAs(context, () -> transactions.execute(tx -> work.get()));
    }

    private UUID createStudio(TestUser owner, String name) throws Exception {
        MvcTestResult result = mvc.post().uri("/api/v1/studios")
                .with(owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                        """.formatted(name))
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private UUID accountIdOf(TestUser user) throws Exception {
        MvcTestResult result = mvc.get().uri("/api/v1/me").with(user.token()).exchange();
        assertThat(result).hasStatusOk();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.account.id"));
    }

}
