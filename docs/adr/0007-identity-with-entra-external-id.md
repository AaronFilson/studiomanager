# 7. Customer identity with Microsoft Entra External ID

- **Status:** accepted
- **Date:** 2026-10-19

## Context

Studio owners, staff and members need sign-up and sign-in, without the project storing passwords
or building account recovery and MFA. The platform is Azure, and the budget is free tier.

## Decision

- **Microsoft Entra External ID** (the successor to Azure AD B2C) issues tokens. The SPA uses
  MSAL with authorization code + PKCE, and requests the API's `access_as_user` scope.
- **The API is a plain OAuth 2.0 resource server.** It validates issuer, audience and signature
  from configuration, and requires `SCOPE_access_as_user`. Nothing in the API is specific to
  Entra beyond those settings, so switching providers is a configuration change.
- **No sign-up endpoint.** The first call after sign-in (`GET /api/v1/me`) creates the account,
  keyed by the token's issuer and subject. The upsert is race-safe, and the common path is one
  indexed read.

## Consequences

- External ID's free tier covers far more monthly active users than this project needs.
- Creating the tenant and the user flow are portal steps; the app registrations are Terraform
  (`infra/identity`).
- Tests never call the identity provider: they use Spring Security's `jwt()` post-processor.

## In an interview

"The API only knows OAuth: issuer, audience, scope. Entra External ID handles passwords, MFA and
recovery, and the account record is created just in time from the token's claims."
