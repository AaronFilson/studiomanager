package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** The studio module's SQL, each statement with its reason (ADR 21). */
@Component
class StudioQueries {

  private final JdbcClient jdbc;

  StudioQueries(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Accepts an invite for the account bound to the transaction. Cross-tenant function: the invitee
   * isn't a member yet, so row-level security hides the invite from them, and {@code
   * app.accept_invite} reads it as the schema owner (ADR 10).
   */
  Optional<MembershipView> acceptInvite(String code) {
    return jdbc.sql("select studio_id, studio_name, role from app.accept_invite(:code)")
        .param("code", code)
        .query((rs, row) ->
            new MembershipView(
                rs.getObject("studio_id", UUID.class),
                rs.getString("studio_name"),
                StudioRole.valueOf(rs.getString("role"))))
        .optional();
  }
}
