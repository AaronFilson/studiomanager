package io.github.aaronfilson.studiomanager.platform.tenancy;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Refuses to start if the application's database login would bypass row-level security.
 *
 * <p>Superusers, roles with {@code BYPASSRLS}, and table owners are exempt from RLS policies, so
 * one wrong connection setting would silently switch tenant isolation off. This turns that
 * mistake into a startup failure. Disabled only for the migration job, which must own the schema.
 */
@Component
@ConditionalOnBooleanProperty(name = "studiomanager.tenancy.rls-guard", matchIfMissing = true)
class RowLevelSecurityGuard implements ApplicationRunner {

    private final JdbcClient jdbc;

    RowLevelSecurityGuard(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        verify();
    }

    void verify() {
        Login login = jdbc.sql("""
                select current_user as name, r.rolsuper as superuser, r.rolbypassrls as bypass_rls,
                       exists (select 1
                               from pg_class c
                               join pg_namespace n on n.oid = c.relnamespace
                               where n.nspname = 'public'
                                 and c.relrowsecurity
                                 and pg_has_role(current_user, c.relowner, 'USAGE')) as owns_protected_table
                from pg_roles r
                where r.rolname = current_user
                """).query(Login.class).single();
        if (login.superuser() || login.bypassRls() || login.ownsProtectedTable()) {
            throw new IllegalStateException(("The database login '%s' would bypass row-level security "
                    + "(superuser, BYPASSRLS, or owner of a protected table). Connect as a member of app_rw instead.")
                    .formatted(login.name()));
        }
    }

    record Login(String name, boolean superuser, boolean bypassRls, boolean ownsProtectedTable) {
    }

}
