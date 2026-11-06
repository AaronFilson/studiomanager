# 18. Sign in with Google and GitHub

_Planned 2026-11-06, while building [17](17-mcp-and-api-tokens-plan.md) and [16](16-invoice-ocr-plan.md).
Sign-in today is Microsoft Entra External ID with email (ADR 7). Members of a pottery studio mostly
have Google accounts. Some, often the studio's technical helper, would rather use GitHub._

## Findings (2026-11-06)

- **Google is built in.** Entra External ID's external tenants list Google, Facebook and Apple as
  social identity providers. The sign-in page shows "Sign in with Google" and redirects to
  Google. <https://learn.microsoft.com/en-us/entra/external-id/customers/concept-authentication-methods-customers>
- **GitHub is not built in, and can't be federated directly.**
  - External tenants can federate a custom OpenID Connect provider (the docs once labelled it
    preview).
  - But GitHub's OAuth apps don't implement OpenID Connect and issue no ID tokens for users. The
    `/.well-known/openid-configuration` that GitHub publishes is for MCP clients of GitHub's own
    MCP server, and it's a public preview.
  - So Entra can't treat GitHub as an OIDC provider. A community answer routes GitHub through
    Auth0 as a bridge. That's unofficial, and it's a paid third party.

  Sources:
  - <https://docs.github.com/en/apps/github-authentication-discovery-endpoints>
  - <https://learn.microsoft.com/en-us/answers/questions/5904359/architecture-guidance-request-github-identity-for>
- **The API doesn't change.** The API trusts Entra's tokens (ADR 7), and accounts are keyed by
  issuer and subject. A person who signs in through Google or GitHub *via Entra* still gets an
  Entra token, so nothing downstream changes.
- **Unverified:**
  - whether custom OIDC in external tenants is generally available now;
  - which claims Entra passes through from a custom provider (email, `email_verified`);
  - the pricing of federated sign-ins on the free tier.

  Check all three on the first-deploy tenant before step 2.

## Options for GitHub

| Option | How | For | Against |
|---|---|---|---|
| **A. Our own small OIDC bridge** (recommended) | A Spring Authorization Server app, `auth/`, does the GitHub OAuth 2.0 dance. It reads `/user` and `/user/emails`, keeping only a verified primary email. It issues a standard OIDC ID token, and Entra federates it as a custom OIDC provider. | No third party. Entra stays the one issuer the API trusts. **The same authorization server is the one plan 17 step 5 needs for MCP OAuth**, so building it once covers both. | A second deployable to run and secure, with its own signing key. |
| B. Replace Entra with our own authorization server | Spring Authorization Server becomes the identity provider, with Google and GitHub as upstream logins. | One system that's fully ours, and the MCP OAuth fits it naturally. | It throws away ADR 7. Existing accounts change issuer and need a migration. We'd own password reset, MFA and abuse handling. Too much for now. |
| C. Auth0 or another broker in front of GitHub | Auth0 federates GitHub, and Entra federates Auth0. | Fastest. | A paid third party in the sign-in path, two hops, and a weaker interview story. |

## Plan

### Step 1: Google, through Entra (small, config only)
- **Terraform** (`infra/identity`): add Google as an identity provider on the external tenant,
  using the Google Cloud OAuth client's ID and secret. The secret goes in Key Vault. Add
  "Google" to the user flow.
  - **Unverified:** whether the azuread provider can manage external-tenant identity providers.
    If it can't, do it with a documented `az rest` call in the first-deploy runbook.
- **Google Cloud:** an OAuth consent screen (external, with only the `openid email profile`
  scopes) and a web client whose redirect URI is the Entra tenant's callback.
- **Web and mobile:** nothing changes. MSAL and the PKCE flow land on the Entra page, which now
  shows a Google button. Add a "Continue with Google" hint to the landing page with
  `domain_hint=google.com`, so the click goes straight to Google.
- **API:** nothing changes. Write a test that a token whose `idp` claim is `google.com` resolves
  to an account like any other, and that a second sign-in with the same subject finds the same
  account.

### Step 2: the OIDC bridge, with GitHub (`auth/`, ADR 20)
- **A new Gradle module, `auth/`:** Spring Boot with Spring Authorization Server, deployed as its
  own Container App. It's stateless apart from a small Postgres schema of its own for
  authorizations and the key set.
- **Upstream login:** Spring Security `oauth2Login` with a GitHub client.
  - On callback it calls `GET /user` and `GET /user/emails`.
  - It **refuses accounts with no verified email**, because invites are matched by email.
  - Subject: `github:<numeric id>`. Never the login name, because people can rename it.
- **Downstream it acts as an OIDC provider:**
  - Discovery, JWKS, `authorize`, `token` and `userinfo`.
  - One registered client, which is Entra's custom OIDC federation.
  - ID token claims: `sub`, `email`, `email_verified`, `name` and `picture`.
  - Signing keys are RS256, stored in Key Vault and rotated with an overlap.
- **Entra:** add the bridge as a custom OIDC identity provider, and add "GitHub" to the user flow.
- **Linking accounts:** someone who signed up with email and later uses GitHub with the same
  verified email ends up as two Entra users. Two choices:
  - Entra's automatic account linking by email, if the external tenant supports it (unverified);
  - otherwise, an explicit "link GitHub" in the app, as follow-up work.

  Note it in the ADR. Never link silently by unverified email.
- **Security:**
  - PKCE and an exact redirect URI;
  - `state` and `nonce` checks;
  - a short-lived authorization code, used once;
  - ID tokens valid for 5 minutes;
  - no GitHub token kept after the call; only the read scopes `read:user` and `user:email`;
  - rate limits on `/authorize`;
  - the bridge's own security headers.

### Step 3: reuse the bridge for MCP OAuth (plan 17 step 5)
- **The same authorization server issues access tokens for the API's `mcp` resource.**
  - It publishes Protected Resource Metadata (RFC 9728) from the API.
  - It supports dynamic client registration or client ID metadata documents, whichever the MCP
    spec requires then.
  - Tokens are audience-bound (RFC 8707).
  - Users sign in through Entra, and their tokens carry the studio and the same scopes as
    personal API tokens.
- **The API gains a second trusted issuer, for the `mcp` audience only.** That's a separate
  `JwtDecoder` chosen by audience. ADR 7's "one issuer" becomes one issuer per audience.

## Tests
- **Bridge (unit):** the GitHub user and emails mapping, refusing an unverified email, a stable
  subject after a rename, the ID token's claims and signature against its own JWKS.
- **Bridge (integration):** the whole code flow against a stub GitHub, with WireMock or the JDK
  `HttpServer` as in the webhook tests.
- **API:** tokens from each `idp` resolve to accounts, and a token from the bridge itself (not
  Entra) is refused on the normal API, so only Entra's tokens count.
- **Manual on the dev tenant:** sign in with Google, sign in with GitHub, and accept an invite
  sent to the same verified email.

## Needs from the owner
- A Google Cloud project, and an OAuth client for the Entra callback.
- A GitHub OAuth app whose callback is the bridge.
- The first deploy, because steps 1 and 2 need the real Entra tenant.

## In an interview
"Studios wanted Google and GitHub sign-in. Google was a configuration change in Entra External
ID. GitHub doesn't speak OpenID Connect, so I wrote a small OIDC bridge on Spring Authorization
Server: it logs in with GitHub's OAuth, keeps only a verified email, and issues standard ID
tokens that Entra federates. The API still trusts a single issuer. And that same authorization
server later became the OAuth server for my MCP endpoint, so one component solved two problems."
