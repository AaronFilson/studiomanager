package io.github.aaronfilson.studiomanager.platform.tenancy;

/**
 * Verifies a signed MCP OAuth 2.1 JWT issued by the configured MCP authorization server (ADR 26),
 * so an already-authenticated Entra user can securely link their MCP OAuth identity to their Studio
 * Manager account without trusting unverified claims.
 */
public interface McpOAuthTokenVerifier {

  /** A cryptographically verified {@code (issuer, subject)} pair from an MCP OAuth token. */
  record VerifiedIdentity(String issuer, String subject) {}

  /** Whether MCP OAuth is configured on this server. */
  boolean enabled();

  /**
   * Verifies the RS256 signature, issuer and timestamps of {@code token} and returns its verified
   * {@code (issuer, subject)}. Throws {@link
   * io.github.aaronfilson.studiomanager.platform.InvalidInputException} if MCP OAuth is disabled or
   * the token is invalid.
   */
  VerifiedIdentity verify(String token);
}
