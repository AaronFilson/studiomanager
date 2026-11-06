# 16. Background jobs act as their own system principals

- **Status:** accepted
- **Date:** 2026-11-05

## Context

Seven background jobs touch studio data with nobody signed in. Until now, each one borrowed an
identity:
- **The maintenance reminders** acted as the studio's first owner. A studio with no owner was
  skipped, and anything the job did looked like the owner's work.
- **The webhook sweep** acted as whoever made the endpoint. That person could leave the studio.
- **The Kafka activity consumer** acted as a made-up all-zero account that no row belonged to.
  Nothing said it was a machine.
- **Unclaiming pieces, the photo-upload cleanup and the purges** called their cross-studio
  functions (ADR 10) with nothing bound at all. A piece's history showed a blank actor.

So the history could be wrong, a job could do whatever its borrowed person could, and nothing
stopped ordinary request code from calling a function meant only for jobs. The plan and research
are in [12](../planning/12-system-principal-plan.md).

## Decision

- **Each job has its own account.**
  - `account.kind` is `PERSON` or `SYSTEM`. V25 seeds one `SYSTEM` row per job, each with a fixed
    id, the issuer `urn:studiomanager:system`, and a name such as "System: webhook sweep".
  - The `SystemActor` enum holds the same ids, and gives each job a role ceiling. Today every
    ceiling is `STAFF`, and none may be `OWNER`.
- **No person can become a job, and a job is never a member.**
  - `AccountService` refuses any token with the system issuer.
  - A trigger stops a system account from joining a studio.
  - A `TenantContext` of kind `SYSTEM` must name a `SystemActor` and stay within its ceiling.
  - A `PERSON` context can't use a system account.
- **Jobs are bound the way requests are.**
  - `TenantContext.system(actor, studioId)` binds a job to one studio, and row-level security
    (ADR 3) limits it exactly as it limits a person.
  - `TenantContext.system(actor)` binds a job to no studio, and then it can see no studio data.
- **Postgres knows who is acting.**
  - `TenantAwareJpaTransactionManager` also sets `app.principal_kind` (`person` or `system`).
  - Every cross-studio security-definer function that jobs call checks it first, through
    `app.require_system_principal()`, and raises otherwise. A bug in request code can't reach
    them.
- **Kept:**
  - The app's database role stays as it is: no `BYPASSRLS` and no second login. The ADR 10
    functions remain the only way across studios.
  - Listeners that act for the person who caused the event keep acting as that person. These are
    the invite emails, the opening balances and the webhook fan-out.
- **Shown:** a piece's history now names the job and carries `actorKind`, and the web app marks
  the step "automatic".

## Consequences

- **The history is accurate.** Unclaimed pieces show "System: unclaimed pieces" instead of a blank,
  and rows a job writes point to a real account row.
- **Losing a person doesn't break a job.** The maintenance reminders still run in a studio whose
  owner left, and webhooks keep sending after their creator leaves.
- **A job's powers are written down.** Its ceiling lives in one enum, so a review sees it there,
  instead of finding an owner's identity borrowed somewhere in the code.
- **A new job is three steps:** a `SystemActor` constant, a seeded row in a migration, and a
  `require_system_principal()` call in any cross-studio function it uses. Forgetting the actor
  makes the function raise, so the mistake shows up in tests.
- **The Spring Security context isn't involved.** Method security sits only on controllers, and no
  job goes through a controller, so jobs bind only `TenantContext`. If a job ever calls a secured
  method, it will need an authentication of its own.
- **Not done:**
  - A separate Postgres login role for jobs. Revisit it if jobs move to their own process.
  - Per-job database grants.
  - An append-only audit table.

## In an interview

"My background jobs used to borrow a person's identity, usually the studio owner. Now each job is
its own principal: a seeded account marked SYSTEM, with a role ceiling that can never be owner. It's
bound to one studio the same way a request is, so row-level security still fences it in. Postgres
also knows whether a person or a job is acting, so the few cross-tenant functions refuse anyone but
a job. And the history says 'System: unclaimed pieces' instead of blaming a person or showing a
blank."
