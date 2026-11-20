package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * A request authenticated with an OAuth 2.1 access token issued by the MCP authorization server
 * (ADR 26). Accepted only on {@code /api/v1/studios/{studioId}/mcp}, never on regular REST routes.
 */
public final class McpOAuthAuthentication extends AbstractAuthenticationToken {

  /**
   * The sole authority an MCP OAuth access token carries. Regular API routes require {@code
   * SCOPE_access_as_user} or {@code API_TOKEN} and therefore refuse this authority.
   */
  public static final String AUTHORITY = "MCP_OAUTH";

  /**
   * Validated claims from an MCP OAuth access token.
   *
   * @param issuer the trusted MCP authorization server issuer ({@code iss})
   * @param subject the authenticated person's subject or account ID ({@code sub})
   * @param studioId the single studio this token is bound to ({@code studio_id})
   * @param scopes the parsed {@link TokenScope}s granted to this token ({@code scope})
   * @param clientId the OAuth client that requested this token, if present
   */
  public record ResolvedMcpToken(
      String issuer,
      String subject,
      UUID studioId,
      Set<TokenScope> scopes,
      @Nullable String clientId) {}

  private final ResolvedMcpToken token;

  public McpOAuthAuthentication(ResolvedMcpToken token) {
    super(List.of(new SimpleGrantedAuthority(AUTHORITY)));
    this.token = token;
    setAuthenticated(true);
  }

  public ResolvedMcpToken token() {
    return token;
  }

  @Override
  public Object getCredentials() {
    return "";
  }

  @Override
  public Object getPrincipal() {
    return token.subject();
  }
}
