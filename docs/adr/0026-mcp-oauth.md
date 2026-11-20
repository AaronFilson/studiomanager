# 26. Phase 2 OAuth 2.1 for MCP

- **Status:** accepted
- **Date:** 2026-11-20
- **Extends:** [ADR 7](0007-identity-with-entra-external-id.md), [ADR 19](0019-personal-api-tokens.md),
  [ADR 20](0020-github-sign-in-bridge.md), [ADR 22](0022-mcp-on-the-official-sdk.md),
  [ADR 25](0025-studio-permissions.md)

## Context

Phase 1 of Studio Manager's MCP server ([plan 17](../planning/17-mcp-and-api-tokens-plan.md))
authenticated assistants with personal API tokens (`Authorization: Bearer smpat_...`). Some remote
MCP clients only connect through the MCP Authorization specification (OAuth 2.1 with RFC 9728
Protected Resource Metadata, PKCE S256, RFC 7591 dynamic client registration, and RFC 8707 resource
indicators).

Microsoft Entra External ID does not offer RFC 7591 dynamic client registration. We already operate
a small Spring Authorization Server (`auth/`, ADR 20) as our GitHub-to-Entra OpenID Connect
bridge. Plan 18 step 3 designed that same authorization server to serve as the MCP OAuth 2.1
authorization server so one component solves both problems.

## Decision

### 1. API — RFC 9728 Protected Resource Metadata and `WWW-Authenticate` discovery

- When `studiomanager.mcp.oauth.issuer` is configured, the API serves unauthenticated RFC 9728
  metadata on:
  - `GET /.well-known/oauth-protected-resource`
  - `GET /.well-known/oauth-protected-resource/api/v1/studios/{studioId}/mcp`
- Both return `resource`, `authorization_servers`, `scopes_supported` (`["read", "propose"]`), and
  `bearer_methods_supported` (`["header"]`). Neither endpoint queries the database or leaks whether
  a studio ID exists.
- When `studiomanager.mcp.oauth.issuer` is blank (the default), both endpoints return `404`.
- When MCP OAuth is enabled, `401` responses on `/api/v1/studios/{studioId}/mcp` include
  `WWW-Authenticate: Bearer resource_metadata="<origin>/.well-known/oauth-protected-resource/api/v1/studios/{studioId}/mcp"`
  so MCP clients auto-discover the authorization server.

### 2. API — Second trusted JWT issuer, restricted strictly to `/api/v1/studios/{studioId}/mcp`

- ADR 7's "one issuer" rule becomes **one issuer per resource/audience**:
  - Microsoft Entra External ID remains the sole trusted JWT issuer for the REST API (`/api/**`),
    `/api/v1/me`, and `/api/v1/admin/**`.
  - The MCP OAuth authorization server (`studiomanager.mcp.oauth.issuer` + `jwk-set-uri` +
    `audience`) is trusted **only** on `/api/v1/studios/{studioId}/mcp`.
- `McpOAuthAuthenticationFilter` verifies RS256 signatures lazily against `jwk-set-uri`, checks
  `exp`/`nbf`, `iss`, required `sub` and `studio_id`, and validates `aud` against the configured
  RFC 8707 resource URI (or the per-studio MCP resource URL for `studio_id`).
- Any JWT whose `iss` names the MCP authorization server is refused with `401` on every non-MCP
  route before reaching Entra's decoder, and `McpOAuthAuthentication` carries only the `MCP_OAUTH`
  authority (never `SCOPE_access_as_user` or `API_TOKEN`). Regular Entra tokens and `smpat_`
  personal tokens keep working unchanged everywhere, including on `/mcp`.
- **Never more than the person:** on every MCP request, `TenantContextFilter`:
  1. Verifies the path `{studioId}` matches the token's `studio_id` (403 otherwise).
  2. Checks `TokenPolicy` for the `mcp` area (requiring `read` scope).
  3. Looks up an existing non-system account for `sub` (without auto-creating accounts).
  4. Resolves the person's live studio membership and live permissions, requires `mcp.use`, and
     intersects live permissions with `TokenScope.allowedBy(scopes)`.
  5. Binds `TenantContext`, so PostgreSQL row-level security and Hibernate `@TenantId` enforce
     tenant isolation identically to every other call.
- Demoting a user, removing them from a studio, or revoking `mcp.use` cuts off active MCP OAuth
  tokens on the next request. Money, member administration, tokens, webhooks, imports, invoices,
  exports, and pending-action approvals remain unreachable.

### 3. Auth Bridge (`auth/`) — MCP OAuth 2.1 Authorization Server

- Enabled via `studiomanager.bridge.mcp.enabled=true` (off by default, leaving existing
  GitHub-to-Entra federation unchanged).
- **Dynamic Client Registration (`POST /oauth2/register`):**
  - Stateless, rate-limited with Bucket4j (`register-requests-per-minute`, default 20/min), backed
    by a bounded in-memory repository (max 1,000 dynamic clients; fixed clients like Entra and any
    optional pre-registered MCP client are never evicted or overwritten).
  - Accepts only public PKCE clients (`token_endpoint_auth_method: "none"`, `authorization_code`
    grant, `code` response type, `requireProofKey(true)`, `requireAuthorizationConsent(true)`).
  - Restricts `redirect_uris` to `https://` or loopback `http://` (`localhost`, `127.0.0.1`,
    `[::1]`) with no fragments or userinfo (OAuth 2.1 / RFC 8252).
  - Restricts scopes to the configured MCP scopes (`read`, `propose`).
- **Discovery metadata:** `/.well-known/oauth-authorization-server` and
  `/.well-known/openid-configuration` advertise `code_challenge_methods_supported: ["S256"]`,
  `scopes_supported`, `token_endpoint_auth_methods_supported` including `"none"`, and
  `registration_endpoint`.
- **Explicit per-request consent (`/oauth2/consent`):**
  - Displays the client name, signed-in identity, target studio ID, resource URI, and requested
    scopes with clear descriptions, rendered with strict CSP
    (`default-src 'none'; style-src 'self'; form-action 'self'; frame-ancestors 'none'`) and full
    HTML escaping.
  - Consent is deliberately not cached across authorizations (`McpAuthorizationConsentService`),
    because Spring Authorization Server keys consent by client and principal rather than studio;
    re-prompting guarantees a client approved for Studio A can never silently obtain a token for
    Studio B.
- **Resource-bound tokens (RFC 8707):**
  - Validates `resource` against `studiomanager.bridge.mcp.allowed-resources` at `/oauth2/authorize`
    and verifies any `resource` sent at `/oauth2/token` matches (returning `invalid_target`
    otherwise).
  - Resolves `studio_id` from `/api/v1/studios/{studioId}/mcp` in `resource` or an explicit
    `studio_id` parameter, refusing mismatches.
  - Signs RS256 JWT access tokens with `SigningKeys`, carrying `sub`, `studio_id`, space-delimited
    `scope`, `client_id`, and `aud` equal to the authorized `resource`.

## Consequences

- MCP clients that require OAuth 2.1 auto-discovery (RFC 9728 + RFC 7591 + PKCE S256 + RFC 8707)
  can connect directly to `/api/v1/studios/{studioId}/mcp` without manually copying `smpat_`
  tokens.
- Personal API tokens (`smpat_...`) remain supported for scripts and clients that prefer static
  bearer headers.
- Both `api/` and `auth/` keep MCP OAuth off by default until configured.
- Dynamic clients and authorizations stay in memory on the single-replica `auth/` Container App
  (matching ADR 20). Moving `RegisteredClientRepository` and `OAuth2AuthorizationService` to
  Postgres (`JdbcRegisteredClientRepository` + `JdbcOAuth2AuthorizationService`) is the follow-up
  if `auth/` scales past one replica or needs longer-lived refresh tokens.

## In an interview

"MCP clients that require OAuth discover our authorization server from RFC 9728 metadata advertised
in the 401 `WWW-Authenticate` header. Rather than adding a third service, we reused our Spring
Authorization Server bridge to handle RFC 7591 dynamic client registration for public PKCE clients,
per-studio consent, and RFC 8707 resource-bound RS256 JWTs. On the API side, that second issuer is
fenced strictly to `/mcp`—normal REST endpoints reject its tokens with 401—and every MCP call still
re-evaluates the user's live studio permissions intersected with the token's scopes under
PostgreSQL row-level security."
