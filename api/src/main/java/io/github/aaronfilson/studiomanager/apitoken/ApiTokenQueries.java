package io.github.aaronfilson.studiomanager.apitoken;

import io.github.aaronfilson.studiomanager.platform.tenancy.ApiTokenResolver.ResolvedToken;
import io.github.aaronfilson.studiomanager.platform.tenancy.TokenScope;
import java.sql.Array;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** The API token module's SQL, for one of ADR 21's reasons. Everything else is Hibernate. */
@Component
class ApiTokenQueries {

  private final JdbcClient jdbc;

  ApiTokenQueries(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Cross-tenant functions: signing in with a token happens before any studio is bound, so the
   * lookup is {@code app.use_api_token} (ADR 10). It returns only a live token, and notes when and
   * where it was used.
   */
  Optional<ResolvedToken> use(byte[] hash, Instant now, String fromAddress) {
    return jdbc.sql(
            """
            select token_id, account_id, studio_id, scopes from app.use_api_token(:hash, :now, :from)
            """)
        .param("hash", hash)
        .param("now", now.atOffset(ZoneOffset.UTC))
        .param("from", fromAddress)
        .query((rs, row) ->
            new ResolvedToken(
                rs.getObject("token_id", UUID.class),
                rs.getObject("account_id", UUID.class),
                rs.getObject("studio_id", UUID.class),
                scopes(rs.getArray("scopes"))))
        .optional();
  }

  private static Set<TokenScope> scopes(Array array) throws SQLException {
    Set<TokenScope> scopes = EnumSet.noneOf(TokenScope.class);
    Arrays.stream((String[]) array.getArray())
        .map(TokenScope::fromWireName)
        .flatMap(Optional::stream)
        .forEach(scopes::add);
    return scopes;
  }
}
