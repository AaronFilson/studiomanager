# Threat model

Plan 22, lane E (plan 15's "threat model"). Written 2026-11-07 against what is built and deployed
to dev.
- It names what's worth protecting, who could go after it, and the control in place for each threat.
- It also lists what's still open.
- Review it whenever a new way in is added: an endpoint family, an integration, or a new identity.

## What's worth protecting

| Asset | Why it matters |
|---|---|
| One studio's data from another studio | The core promise. Members' names, emails, balances, pieces |
| Money records (ledger, payables, stored value) | Wrong balances cost a studio money and trust |
| Members' personal data | Names, emails, consignment payouts (1099 data in the US) |
| Secrets the studio gives us | Its Claude API key, webhook signing secrets |
| Platform settings and paid readers | A runaway reader or lifted limit costs the platform money |
| Availability | A studio runs its front desk on it |

## Who

- **A member of one studio** trying to see or change another studio's data, or to act above their
  role in their own.
- **A staff member** trying to do what their role doesn't allow (refunds, roles, export).
- **A leaked API token or an assistant (MCP)**, used by someone else or by a model that was
  prompt-injected.
- **An outsider on the internet:** no account, or a free account in a studio of their own.
- **A compromised dependency or CI step.**

## Boundaries and controls

### Tenant isolation (spoofing, information disclosure)

- **Two layers** (ADR 3, ADR 21):
  - Postgres row-level security filters every studio table by the studio bound to the
    transaction;
  - Hibernate's `@TenantId` adds the same filter to every entity query.
  - A bug in one layer is caught by the other.
- The studio is bound per request by `TenantContextFilter`, only after the caller's membership is
  found. A path's studio id with no membership is refused, so guessing ids gets nothing.
- Cross-studio work (accepting an invite, sweeps) goes through security-definer functions that
  check the principal kind (ADR 10, ADR 16).
- **Tests and guards:**
  - `TenantIsolationTests` checks that one studio can't reach another's rows, through the API and
    in the database alone. It also checks that every table with a `studio_id` has row-level
    security and a policy, so a new table can't skip it.
  - `RowLevelSecurityGuard` refuses to start the app if its database login could bypass row-level
    security (superuser, `BYPASSRLS`, or owner of a protected table).

### Authorization inside a studio (elevation of privilege)

- Every studio endpoint checks one named permission (ADR 25). Two tests keep it that way: one
  fails on an operation without `x-permission`, the other on an endpoint without
  `@PreAuthorize`.
- Owner-only permissions can't be given to another role. The API refuses it, and a database check
  refuses it too.
- **Nobody gives more than they hold:** roles, invites and assignments are checked against the
  caller's own permissions.
- **A studio always has an owner:** a deferred trigger, plus a row lock against two owners
  demoting each other at once.
- Every access change is logged with actor and "via token", and sent as an `access.changed`
  webhook and stream event.

### API tokens and MCP (spoofing, repudiation, elevation)

- **Storage:** a token is stored only as a hash and shown once. It expires, and it stops working
  after 90 days unused (ADR 19).
- **Never more than its person:** a token's permissions are its person's live permissions ∩ its
  scopes, recomputed on every call.
  - Money, access, tokens, webhooks, imports, invoices and the export are never reachable by a
    token (`TokenPolicy`).
- **MCP:**
  - tools check the same permission as their REST twin;
  - writes go only through `propose`, which a person must approve (plan 17 step 4).
  - That approval step is the defence against a prompt-injected assistant: it can suggest, not
    do.
- Work done with a token is attributed "via <token name>" in histories (repudiation).
- Rate limits per person, per studio and per channel stop runaway loops (plan 20 §6).

### Sign-in

- **Entra External ID** (ADR 7):
  - The API validates issuer, audience and signature on every request.
  - The web app uses MSAL with PKCE.
- **The GitHub bridge** (ADR 20):
  - Its own OpenID provider in front of GitHub's OAuth, rate-limited on `/oauth2/authorize`.
  - It keeps no GitHub tokens after sign-in.
  - Off in dev until its OAuth app is set up.
- **Platform admin** is an Entra app role. Its pages are refused to every studio role and to
  every token.
  - Its settings writes are also refused by the database unless the transaction is bound as an
    admin (ADR 24).

### Outbound requests (SSRF)

- Webhook URLs are resolved, and the connection goes only to the addresses that were checked.
  Private, loopback, link-local (including the metadata endpoint), multicast and reserved ranges
  are refused, including IPv6 forms that carry an IPv4 address (`PublicAddresses`, ADR 14).
- Webhooks are signed (Standard Webhooks). Each endpoint's secret is encrypted at rest and can be
  rotated with an overlap window.

### Secrets at rest

- **Encrypted:** studio AI keys and webhook secrets, with AES-GCM under a versioned keyring, so
  keys can be rotated (`SecretCipher`).
- **Where keys live:** the keyring comes from Container App secrets set by Terraform, never from
  the repository.
- **No passwords anywhere:**
  - the database login is the API's managed identity;
  - email, blob storage and Document Intelligence use the same identity;
  - Google is reached through workload identity federation (ADR 5, ADR 18).

### Files

- **Photos** go straight to Blob Storage on short-lived, single-blob upload URLs (ADR 12). The API
  never handles the bytes.
- **Invoices:**
  - the type is checked by magic number, not by the name or the declared type;
  - size is capped;
  - only the file goes to a reader, with no member data (plan 16, "Data minimization").

### Data leaving the platform

- The studio export is owner-only. Downloads aren't logged yet (see Open).
- The Kafka stream carries ids only, never names or emails.
- Webhook payloads do carry names and emails. The Webhooks page says so before an endpoint is
  saved.

### Supply chain and CI

- **Actions** are pinned by commit SHA where third-party.
- **Deploys** are keyless (GitHub OIDC to Azure, ADR 6). Production identity is scoped to the
  resource group, with a condition on role assignments.
- **Dependencies:**
  - Dependabot is on;
  - pnpm enforces supply-chain checks on the lockfile;
  - images carry SBOM and provenance attestations.
- CodeQL runs on every PR. Error Prone and NullAway run on every compile, with `-Werror`.

### Availability and cost

- Rate limits on tokens and MCP.
- List caps with a `truncated` flag (plan 20 §4).
- Log quota and trace sampling, so the logs can't run up the bill.
- Platform-paid readers have per-studio and platform page caps, and are off by default.

## Open

| Item | Risk | Plan |
|---|---|---|
| Export downloads aren't recorded | With several co-owners, nobody can see who took the studio's data | Plan 21 §7 asks for it; log each download with its account |
| Rate limits are per replica, in memory | With two replicas the ceiling is up to twice the setting | Accepted (plan 20 §6); Bucket4j's Postgres backend if it matters |
| No limit on signed-in web use | A signed-in user could script the API with their session | Watch usage; add a per-person limit if seen |
| Email from the Azure-managed domain | Phishing look-alikes are easier; mail lands in junk | `docs/runbooks/email-domain.md`, before the pilot |
| No WAF in front of the API | Only the app's own checks and Azure's platform defences | Revisit at the pilot; Front Door's WAF costs money even when idle |
| Smoke tests don't sign in | A broken sign-in flow is caught by people, not CI | External ID has no non-interactive test sign-in for this app type |
| Trace sampling unverified | A traffic spike could hit the log quota and hide logs | `docs/runbooks/smoke-and-load.md`, "Check trace volume" |
