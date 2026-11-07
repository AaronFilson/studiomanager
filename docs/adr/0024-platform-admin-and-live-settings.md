# 24. A platform admin role, and settings changed live

- **Status:** accepted
- **Date:** 2026-11-06

## Context

The first hosted use will likely be a demo. For 15 to 30 minutes, an assistant mustn't hit a
rate limit, and afterwards the limits should come back gradually. That needs limits that change
while the app runs, a page to change them, and someone allowed to use it.

Nobody can do that today. Every role in the code is a studio role (owner, staff, member), and a
studio owner mustn't change limits for every studio. Configuration changes need a new revision.
The plan is [20, sections 6 and 7](../planning/20-pre-production-cleanup-plan.md).

## Decision

- **A platform admin is an Entra app role,** `Platform.Admin`, on the API's app registration
  (`infra/identity/main.tf`). Terraform assigns it to the user object ids in
  `platform_admin_object_ids`. It's not a studio role and isn't stored in the database.
- **The API reads it from the token.** The access token's `roles` claim becomes `APPROLE_...`
  authorities, next to the scopes' `SCOPE_...` ones. `/api/v1/admin/**` needs both
  `SCOPE_access_as_user` and `APPROLE_Platform.Admin`; the controller says so again with
  `@PreAuthorize`. An API token carries neither, so it never gets in, and `TokenPolicy` refuses
  it outside a studio anyway.
- **The database checks it too.** On those routes, `PlatformAdminFilter` binds `PlatformAdmin`,
  and `TenantAwareJpaTransactionManager` then sets `app.platform_admin` with each transaction,
  as it sets `app.principal_kind` (ADR 16). Row-level security on `platform_setting` refuses any
  insert or update without it, and any that doesn't name the signed-in account as `changed_by`.
- **Settings are rows of JSON (V29).** `platform_setting` holds a key, a JSON value, a version,
  and who changed it when. Configuration supplies each default, a row overrides it, and a null
  value means "back to the default". Rows are never deleted, so versions only go up.
- **Every change is kept.** A trigger writes `platform_setting_change` (old and new value, who,
  when) as the table's owner. The app can only read it, and only as an admin, so the history
  can't be skipped or forged.
- **Replicas pick changes up without timers.** Each one re-reads the settings on the first call
  after its copy is 15 seconds old, and at once after it saves a change. An idle app does no
  work, and a change reaches every replica within about 15 seconds.
- **The first settings are the rate limits.** `DatabaseRateLimitSettings` is the `@Primary`
  `RateLimitSettings` (the interface lane M's limiter reads):
  - the normal limits per channel (MCP, token) come from `studiomanager.rate-limit.*`, or the
    saved override;
  - a boost lifts them from a start time, now or scheduled: no limits at first, then 8×, 4×
    and 2× over the easing time, then normal. It covers one studio or all of them (the page
    picks the studio by name, from `app.studios_for_platform_admin()`: a cross-studio function
    in the ADR 10 style that returns only ids and names, and refuses anyone not bound as an
    admin), lasts at
    most 4 hours, and "End now" stops it at once;
  - the revision a bucket compares is built from the stored versions and the easing step, so a
    re-read that finds nothing new never rebuilds a bucket.
- **The web app** shows "Admin" to people whose `/me` says `platformAdmin`, and the `/admin` page
  has Limits and History. Usage follows once the call log exists; the demo studio comes later.

## Consequences

- **Two layers, as everywhere else.** A bug that let a non-admin reach the service still can't
  write a setting, and an admin's change is always in the history with their name.
- **Granting the role is an Entra change,** reviewed in Terraform, not something the app can do
  to itself.
- **A forgotten boost costs little.** It ends by itself within 4 hours, about four replica-hours.
- **Not verified yet:** that the External ID tenant puts `roles` in access tokens. Check it on the
  first deploy; until it does, nobody sees the admin page.
- **Not done:**
  - The demo preset picking the demo studio by itself, once it exists.
  - Optimistic locking on settings: two admins saving at once both win in turn, and both
    changes are in the history.

## In an interview

"Limits are tunable at run time from an audited operator page. The page is gated by an app role
from the identity provider, and the database enforces it too: a transaction setting says the
caller is an admin, and row-level security refuses writes without it. A trigger keeps every
change. Replicas re-read the settings at most every 15 seconds, on demand, so nothing polls.
Before a demo I schedule a boost that lifts the limits for half an hour and eases them back over
the next hour."
