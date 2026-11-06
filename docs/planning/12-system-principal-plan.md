# A system principal for background work

_Planned 2026-11-05, while reviewing `wave2-kafka`. It's built on its own branch after that one
merges. Follows [11](11-kafka-stream-plan.md) and ADR 15._

## Why

Four background jobs act inside a studio, and none of them is anyone:
- `MaintenanceReminderSweep` and `InviteEmails` act as the studio owner.
- `WebhookSweep` acts as whoever created the endpoint.
- `ActivityConsumer` acts as a made-up all-zero account, with the role `STAFF`.

That causes three problems:
- **The audit trail lies.** A row written by the sweep says the owner wrote it. If the owner
  leaves, the job breaks or acts as someone who's gone.
- **Jobs get more than they need.** A job that impersonates an owner can do whatever an owner
  can, and the code only stops it by convention.
- **The zero account is a trick.** It works because no row belongs to it, but nothing records
  that it is meant to be a machine, and a `created_by` written under it points at no account.

The fix is a first-class system principal: a named, non-human actor that is bound the same way a
person is, and that shows up as "system: activity-consumer" wherever a person's name would.

## Research findings

- **Use a distinct identity per workload, not a borrowed user.** Cloud IAM separates service
  principals from users, and gives each workload its own identity with only the permissions it
  needs. The same idea applies inside the app. Practitioner RLS guides name "a background job
  that quietly bypasses the policy" as a common failure, and say to set the tenant context for
  jobs the same way requests do.
  <https://hashnode.com/posts/postgres-row-level-security-for-multi-tenant-saas-a-setup-that-holds-up/6ac9181e52c978b433ae915b>,
  <https://stackharbor.com/en/knowledge-base/pg-row-level-security-multi-tenant/>
- **Keep RLS on; don't hand out BYPASSRLS.** AWS's SaaS guidance treats RLS as the isolation
  boundary in the pooled model, set per transaction with a runtime variable, and warns that
  owners and superusers skip it. BYPASSRLS belongs to migrations and incident response, never to
  the app's role.
  <https://docs.aws.amazon.com/prescriptive-guidance/latest/saas-multitenant-managed-postgresql/rls.html>,
  <https://aws.amazon.com/blogs/database/multi-tenant-data-isolation-with-postgresql-row-level-security>
- **Separate DB role, or a setting?** A second login role (or `SET ROLE` per transaction) gives
  a hard database boundary, but needs a second pool or a role reset on every checkout. A
  transaction-local setting (`set_config(..., true)`) costs nothing and already works here.
  Cross-tenant work is already handled by security-definer functions (ADR 10), which is the
  narrow, reviewed hole a separate role would otherwise be for.
  <https://leapcell.io/blog/achieving-robust-multi-tenant-data-isolation-with-postgresql-row-level-security>
- **Spring Security on background threads.** There is no request, so nothing is
  authenticated. Spring's answer is to put an explicit `SecurityContext` on the thread
  (`DelegatingSecurityContext*` executors and schedulers), not to borrow the caller's.
  <https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/concurrent/package-summary.html>,
  <https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/scheduling/DelegatingSecurityContextSchedulingTaskExecutor.html>
- **Audit: say what kind of actor it was.** Audit designs carry an actor type (`user`,
  `service`, `system`) next to the actor id, and Ashby added exactly that split this year.
  Some keep a nullable person FK plus a type; others give services their own rows.
  <https://alivedise.github.io/backend-engineering-essentials/distributed-systems/audit-logging-architecture>,
  <https://developers.ashbyhq.com/changelog/2026-09-02-audit-log-actor-type-candidate-operations-system>,
  <https://git.coopcloud.tech/wiki-cafe/member-console/src/commit/8065d4837aff/design/audit/model.md>

## Design

One choice: **seeded system accounts, a principal kind on the context, and the same RLS.**

### Data (V25)
- `account` gets `kind text NOT NULL DEFAULT 'PERSON' CHECK (kind IN ('PERSON','SYSTEM'))`.
- One seeded row per job, with a fixed UUID, `issuer = 'urn:studiomanager:system'`, and
  `subject` the job's name: `activity-consumer`, `maintenance-reminders`, `webhook-sweep`,
  `invite-emails`. `display_name` is "System: activity consumer".
- System accounts have no memberships and no login. `AccountResolver` refuses any token whose
  issuer is the system URN, so no JWT can become one.
- **Why seeded rows, not nullable `created_by` plus an `actor_kind`:** every existing
  `created_by` stays `NOT NULL REFERENCES account`, no query needs a `CASE`, and "who did it"
  is one join that already works. The cost is a few rows that aren't people; `kind` says so.

### The context
- `TenantContext` gains a `Principal kind` (`PERSON`, `SYSTEM`). `forAccount` stays for people.
  `TenantContext.system(SystemActor actor, UUID studioId)` builds the job context.
- `SystemActor` is an enum in `platform.tenancy`: one constant per job, holding its fixed UUID
  and the `StudioRole` ceiling it needs. Most get `STAFF`; none get `OWNER`.
- A new role isn't added to `StudioRole`. The ceiling is enough, and an owner-only action from a
  job would be a design smell to find in review.

### The database
- `TenantAwareJpaTransactionManager` also sets `app.principal_kind` (`person` or `system`).
  `app.current_principal_kind()` reads it, like the other two.
- **Policies stay as they are.** A system job is bound to one studio, so tenant RLS limits it
  exactly as it limits a person. No BYPASSRLS, no second role.
- Cross-tenant claiming stays in the ADR 10 functions. Those that only jobs should call check
  `app.current_principal_kind() = 'system'` and raise otherwise, so a request can't call them.
- `RowLevelSecurityGuard` keeps checking that the app role isn't an owner and has no BYPASSRLS.

### Spring Security
- A `SystemAuthentication` token (principal = the `SystemActor`, authority
  `ROLE_SYSTEM` plus `SCOPE_<job>`), set by one helper,
  `SystemWork.run(SystemActor, UUID studioId, op)`, that binds both the `TenantContext` and the
  `SecurityContext` for the call and clears them after.
- `@Scheduled`, `@ApplicationModuleListener` and `@KafkaListener` methods call that helper,
  rather than relying on an executor wrapper. Context never crosses threads implicitly, which
  matches the ScopedValue design.
- No RunAs: there is no human to run as. (Whether `RunAsManager` is deprecated in Spring Security 7
  is unchecked. It doesn't matter here, because nothing uses it.)

### Audit
- Anything that shows "by" (the webhook log, notices, the activity page) renders a system
  account's `display_name` with a "system" badge. API responses add `actorKind` next to
  `createdBy`.

## Migration of existing jobs

| Job | Today | After |
|---|---|---|
| `ActivityConsumer` | zero account, `STAFF` | `SystemActor.ACTIVITY_CONSUMER`, `STAFF` ceiling. `NO_ACCOUNT` goes. |
| `MaintenanceReminderSweep` | studio owner | `MAINTENANCE_REMINDERS`. Stops failing when a studio has no owner row. |
| `WebhookSweep` | endpoint creator | `WEBHOOK_SWEEP`. Keeps working after the creator leaves. |
| `InviteEmails` | studio owner | `INVITE_EMAILS`. The invite still names the inviting person in its body; only the actor changes. |

One commit per job, each with its test, after the V25 and context commit.

## Tests
- **Unit.** `TenantContext.system` refuses `OWNER` and a missing studio. `AccountResolver`
  refuses the system issuer.
- **Integration** (Testcontainers):
  - each job, run as its actor, can't read another studio's rows;
  - a row a job writes has `created_by` = the actor's account, and the API shows `actorKind`;
  - a job-only definer function raises when called from a request context;
  - the guard still fails startup if the app role gains BYPASSRLS;
  - deleting a studio's owner doesn't stop the maintenance sweep.
- **Architecture.** An ArchUnit rule: nothing outside `SystemWork` calls `TenantContext.callAs`
  with a context it didn't get from a request, so new jobs can't impersonate people again.

## Not in this slice
- A separate Postgres login role for jobs. Revisit if jobs move to their own process.
- Per-job DB grants. The `STAFF` ceiling and tenant RLS cover it for now.
- An append-only audit table. This slice fixes attribution on existing rows only.
- Machine API keys for outside callers. They'd reuse `kind = 'SYSTEM'` later.

## Status

**Step 1, 2026-11-05, on `wave2-system-principal` (V25).** Built and green: all 221 API tests pass
in WSL. It differs from the plan above in four ways:
- **Three jobs, not four.** `InviteEmails` already acts as the person who asked for the invite, not
  as the owner, and that's the right actor, so it stays. `OpeningBalances` and `WebhookFanOut` act
  as the member who just joined, for the same reason.
- **No `SystemAuthentication` or `SystemWork` helper.** Method security is only on controllers,
  and no job passes through one, so nothing would read a Spring `SecurityContext`. Jobs bind
  `TenantContext.system(actor, studioId)` with `callAs`, the same way requests bind theirs.
- **The checks are in `TenantContext` itself.** A `SYSTEM` context must use a `SystemActor`
  account and stay within its ceiling. A `PERSON` context can't use a system account.
- **More guards in the database.** A trigger stops a system account from joining a studio.
  `AccountService` refuses a token issued by `urn:studiomanager:system`.

What changed:
- `ActivityConsumer` and `SeenEventPurge` act as `ACTIVITY_CONSUMER`, and `NO_ACCOUNT` is gone.
- `MaintenanceReminderSweep` acts as `MAINTENANCE_REMINDERS`, so a studio with no owner is still
  reminded.
- `WebhookSweep` acts as `WEBHOOK_SWEEP`.
- `studios_with_maintenance_to_remind` and `due_webhook_deliveries` now return ids only.
- The definer functions those jobs call now raise unless a system principal is bound:
  `studios_with_maintenance_to_remind`, `due_webhook_deliveries`, `purge_webhook_history` and
  `purge_stream_event_seen`.
- `SystemPrincipalTests` covers the seeded rows, the refused issuer, the job-only functions, the
  membership trigger, and a job seeing only its own studio.

**Left for later:**
- The photo-upload sweeps and `mark_unclaimed_pieces` still run their definer functions with no
  principal. They should get actors too.
- `actorKind` in API responses and the "system" badge in the web app.
- The ArchUnit rule. The checks in `TenantContext` cover most of what it would catch.
- ADR 16.

## Decisions to record
**ADR 16: a system principal for background work.** Seeded system accounts with a `kind`, a
principal kind on `TenantContext` and in `app.principal_kind`, the same tenant RLS for jobs, no
BYPASSRLS, and job-only definer functions that check the kind.

**In an interview:** "My background jobs used to borrow the studio owner's identity. Now each
job is its own named principal. It's bound to one studio the same way a request is, so row-level
security still fences it in, and every row it writes says 'system: webhook sweep' instead of
blaming a person."
