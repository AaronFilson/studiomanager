package io.github.aaronfilson.studiomanager.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import io.github.aaronfilson.studiomanager.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@IntegrationTest
class RowLevelSecurityGuardTests {

    @Autowired
    RowLevelSecurityGuard guard;

    @Autowired
    PostgreSQLContainer postgres;

    @Test
    void acceptsTheApplicationLogin() {
        assertThatNoException().isThrownBy(guard::verify);
    }

    @Test
    void rejectsALoginThatWouldBypassRowLevelSecurity() {
        // The container's superuser, which also owns the schema
        DriverManagerDataSource owner =
                new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        assertThatIllegalStateException()
                .isThrownBy(() -> new RowLevelSecurityGuard(JdbcClient.create(owner)).verify())
                .withMessageContaining("would bypass row-level security");
    }

}
