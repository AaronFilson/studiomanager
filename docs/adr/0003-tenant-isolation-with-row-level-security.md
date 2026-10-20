# 3. Tenant isolation: shared schema plus PostgreSQL row-level security

- **Status:** accepted
- **Date:** 2026-10-19

## Context

Every studio is a tenant. One studio must never see another's members, pieces, fees or invoices.
Isolating with a database per tenant or a schema per tenant would cost too much, and make
migrations too slow, for a free product with many small studios. A shared schema filtered only by
application code means one forgotten `WHERE studio_id = ?` is a data leak.

## Decision

A shared schema with `studio_id` on every studio-owned row, and **PostgreSQL row-level security
(RLS)** underneath the application's own checks:

1. **Context.** For each request, `TenantContextFilter` resolves the caller's account from the
   token. For routes under `/api/v1/studios/{studioId}/...`, it binds the studio only *after*
   confirming membership. The result is a `TenantContext` held in a Java 25 `ScopedValue`:
   immutable, bound for exactly the request's extent, and unable to leak across reused (virtual)
   threads the way a `ThreadLocal` can.
2. **Into the database.** `TenantAwareJpaTransactionManager` begins every transaction with
   `set_config('app.account_id', …, true)` and `set_config('app.studio_id', …, true)`. These are
   transaction-scoped (`SET LOCAL`), so a pooled connection can't carry one request's tenant into
   the next.
3. **Policies.** `V1__tenancy_foundation.sql` enables RLS and adds policies that compare rows
   with `app.current_studio_id()` and `app.current_account_id()`. The functions are wrapped in
   `(SELECT …)` so Postgres evaluates them once per statement, not once per row. Writes must stay
   inside the bound studio (`WITH CHECK`). The "my studios" list works outside any one studio
   through read-only policies keyed on the account.
4. **Roles.** The application logs in as a member of `app_rw`, which owns nothing. Flyway (or
   the migration job on Azure) runs as the owner. `RowLevelSecurityGuard` refuses to start the
   application if its login is a superuser, has `BYPASSRLS`, or owns a protected table, because any
   of those silently disables RLS.
5. **Method security** (`@PreAuthorize("@studioAuthz.isMember(#studioId)")`) states each
   endpoint's requirement where it's declared.

`account` is the global identity directory (one person, many studios) and deliberately has no RLS.
It's reached only by issuer and subject.

## Consequences

- `TenantIsolationTests` proves isolation at the HTTP layer *and* at the SQL layer. An
  unfiltered `select id from studio` inside Alice's studio returns only Alice's studio. A write
  into Bob's studio fails with `new row violates row-level security policy`. With no tenant bound,
  no rows come back at all.
- Every new studio-owned table needs a `studio_id`, an `ENABLE ROW LEVEL SECURITY`, policies and
  explicit grants to `app_rw` in its migration. That's a review checklist item.
- Creating a studio binds the new studio's ID *before* its transaction begins, so the inserts pass
  the policies without a special case.
- Every transaction acquires its connection at the start, to run `set_config`. That's a small
  cost.

## In an interview

"Application filters are the first line, and Postgres RLS is the backstop. The tenant is bound to
each transaction with `SET LOCAL` semantics, so pooled connections can't leak it, and the app
refuses to start if its login could bypass RLS. I have a test that runs an unfiltered query inside
one tenant and proves the database hides the others."
