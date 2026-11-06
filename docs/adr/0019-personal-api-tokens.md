# 19. Personal API tokens, never more than their person

- **Status:** accepted
- **Date:** 2026-11-06

## Context

Owners want their own scripts and AI assistants to help run the studio. The assistants include
Claude through MCP ([17](../planning/17-mcp-and-api-tokens-plan.md)). A person's sign-in is an
interactive Entra token that lasts an hour, which a script can't use. A credential for a machine
has to be easy to revoke, and it must not reach further than the person who made it.

## Decision

- **Opaque tokens, hashed.** A token is `smpat_` and 43 base62 characters (256 random bits).
  Only its SHA-256 is stored, with the last four characters for telling tokens apart. It's shown
  once. The prefix lets secret scanners recognise a leaked token.
- **One studio, staff only, the person's live role as the ceiling.**
  - A token belongs to one person in one studio.
  - On every request the person's membership is looked up again. A token whose person is no
    longer staff gets a 403, and one whose person has left stops at once.
  - A token can't reach any route outside its studio, including `/me`.
- **Scopes, decided in one place.**
  - `read`, plus one `write:<area>` scope per area: pieces, kilns, maintenance, schedule,
    supplies, glazes and notices.
  - `TokenPolicy` maps each route area, together with the HTTP method, to "read only",
    "writable with this scope" or "never". This is one table, not an annotation on each of
    about 150 controller methods.
  - The table fails closed: an area not in it is refused. A unit test reads the OpenAPI
    contract and fails if any area isn't listed, so a new area is a deliberate decision.
- **Never reachable with a token,** whatever its scopes: money (the ledger, payments, payouts,
  payees, consignment, balances), imports, the data export, webhooks, invites and tokens
  themselves.
- **Sign-in.**
  - `ApiTokenAuthenticationFilter` runs before the JWT filter, and only for `smpat_` bearers. An
    invalid one gets a 401 there.
  - The JWT resource server's bearer resolver skips `smpat_` values, so the two paths never
    meet.
  - The lookup happens before any studio is bound, so it's an ADR 10 security-definer function,
    `app.use_api_token`. It's keyed by the hash, returns only a live token, and records the last
    use at most once a minute.
- **Row-level security is unchanged.** Once signed in, a token request binds the same
  `TenantContext` as the person's own sign-in would, so RLS fences it the same way.

## Consequences

- A leaked token is bounded by its studio, its scopes and its expiry (365 days at most), and
  the person or the studio owner can revoke it at once.
- **Not yet:**
  - Attribution says the person, not "via token"; the plan's step 2 adds that.
  - There's no per-token rate limit.
  - Tokens can't propose changes for approval; that's step 4, pending actions.
- MCP (step 3) reuses these tokens as its first sign-in method.

## In an interview

"Tokens are opaque and hashed, and each is bound to one studio. Every request re-checks the
person's live role, so a token is never more than its person, even after they're demoted. One
policy table maps route areas to scopes, it fails closed, and a test reads the OpenAPI contract
so no new endpoint is left out. Money is never reachable with a token."
