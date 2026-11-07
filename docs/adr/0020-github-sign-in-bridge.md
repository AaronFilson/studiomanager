# 20. GitHub sign-in through our own OpenID Connect bridge

- **Status:** accepted (built; turned on at the first deploy)
- **Date:** 2026-11-06

## Context

Sign-in is Entra External ID (ADR 7), and the API trusts only Entra's tokens. Some members, often
a studio's technical helper, want to sign in with GitHub. Entra External ID can federate a custom
OpenID Connect provider, but GitHub's OAuth apps aren't one: they issue no ID token. Plan 18
compares three ways round that and picks our own small bridge
([18](../planning/18-google-and-github-sign-in-plan.md), option A).

## Decision

- **A bridge, `auth/`, that is an OpenID Connect provider in front of GitHub's OAuth.**
  - Spring Authorization Server (in Spring Security 7) serves discovery, JWKS, `authorize`,
    `token` and `userinfo`. Its one registered client is Entra's custom OIDC identity provider.
  - Signing in to the bridge is Spring Security's `oauth2Login` with GitHub. An unauthenticated
    `/oauth2/authorize` sends the browser to GitHub, and the callback resumes the authorize
    request, which redirects back to Entra with a code.
- **Its own Gradle module and Container App.** `auth/` builds with the API's Gradle wrapper and
  version catalog: `api/settings.gradle.kts` includes `../auth`, so `scripts/api.sh` and CI build
  and test it with the API. It's deployed as `ca-<env>-auth`, with its own identity that can only
  read Key Vault secrets. The API's image is built from `api/` alone, so the include is skipped
  when `../auth` isn't there.
- **Who the user is.** On the callback the bridge reads `GET /user` and `GET /user/emails`.
  - It **refuses an account without a verified primary email**, because invites are matched by
    email. Only that address is issued, with `email_verified: true`.
  - The subject is `github:<numeric id>`, never the login name, which people can change.
  - ID token claims: `sub`, `email`, `email_verified`, `name` (the GitHub name, or the login when
    there's none) and `picture`.
  - GitHub's access token is used for those two reads and dropped: the authorized-client
    repository saves nothing. The scopes are `read:user user:email`.
- **Protocol checks.**
  - Exact redirect URIs: Entra's two federation callbacks, built by Terraform from the tenant.
  - The client authenticates with `client_secret_post`, the method Entra supports
    (`client_secret_basic` it doesn't). The secret is held only as a bcrypt hash.
  - PKCE: checked whenever Entra sends a challenge, not required, because whether Entra's
    federation sends one is unverified. Change `requireProofKey` to true once a real sign-in
    shows the challenge. PKCE is always used on the GitHub leg, on top of `state`.
  - `state` and `nonce`: Entra's are echoed and put in the ID token by Spring Authorization
    Server; GitHub's `state` is checked by Spring Security, and a wrong one ends the sign-in with
    a 403 and no code.
  - Codes live 1 minute and are used once; a replay is `invalid_grant`. ID and access tokens live
    5 minutes.
  - `/oauth2/authorize` is rate limited (60 a minute per replica, one shared bucket, because a
    per-address limit would mean trusting a forwarded header).
  - Its own headers: `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`,
    `Referrer-Policy: no-referrer`, plus Spring Security's defaults.
  - Errors are fixed messages; nothing a caller sent is echoed or logged.
- **Signing keys: RS256, in Key Vault, reached as a Container App secret.**
  - One Key Vault secret, `github-bridge-signing-keys`, holds one or more PKCS#8 PEM RSA keys
    (2048 bits or more). The first signs. Any after it are published in the JWKS only, so
    rotation is: put the new key first, keep the old one after it for a day (well past Entra's
    key cache and the 5-minute tokens), then drop it.
  - Key IDs are RFC 7638 thumbprints, so they're stable across restarts.
  - The Container App reads the secret with a Key Vault reference and its own identity. The key
    never passes through Terraform or its state, unlike the API's `random_bytes` keys, because the
    owner generates it with `openssl` (the runbook). The GitHub client secret and the secret
    Entra presents are two more vault secrets, handled the same way.
- **State in memory, one replica.** Plan 18 suggested a small Postgres schema for authorizations
  and the key set. The bridge keeps both elsewhere instead:
  - Keys come from Key Vault, as above.
  - Authorizations (codes and the tokens issued for them) and the sign-in session are in memory.
    A code is redeemed within seconds of being issued, and the session only carries one sign-in
    through GitHub and back. The Container App is pinned to `max_replicas = 1`, and scales to zero.
    A restart mid-sign-in costs that one person a retry.
  - That avoids a database identity, a migration job and a schema for data that lives a minute.
  - **Revisit when** the bridge needs more than one replica, or when plan 17 step 5 makes it the
    MCP authorization server with longer-lived tokens. Then: `JdbcOAuth2AuthorizationService` and
    Spring Session JDBC in an `auth` schema, with the bridge's own Postgres role.
- **Off by default.** Terraform creates nothing until `github_sign_in.enabled` is true in
  `infra/envs/<env>`. The deploy workflow always builds and pushes the image, and rolls it out
  only when the app exists.
- **The Entra side is not in Terraform.** Custom OIDC identity providers are a Microsoft Graph
  beta resource (`oidcIdentityProvider`) that the `azuread` provider doesn't manage, and its body
  carries the client secret, which `infra/identity` keeps out of its local state. The runbook
  creates it with `az rest`, or the admin center.
- **Linking accounts.** Someone who signed up with email and later uses GitHub with the same
  verified address gets a second Entra user, so a second Studio Manager account. Whether External
  ID links them by email automatically is unverified. If it doesn't, an explicit "link GitHub"
  in the app is follow-up work. Nothing links silently by an unverified email: the bridge only
  ever issues verified ones.

## Consequences

- The API is unchanged: it still trusts only Entra. A token signed by the bridge itself has the
  bridge's issuer and Entra's client ID as audience, so the API's decoder refuses it.
- A second deployable with its own secrets to run. It's small (0.25 vCPU, 0.5 GiB) and scales to
  zero; a cold start delays a GitHub sign-in by a JVM start.
- Plan 17 step 5 can reuse the same authorization server for MCP OAuth, after moving its state to
  Postgres as above.

## Unverified

- Whether Entra's federation sends PKCE, and which claims it maps by default (`email_verified`
  in particular). The runbook sets the claim mapping explicitly.
- Whether External ID links a GitHub user to an existing email user.
- The pricing of federated sign-ins on the free tier.
