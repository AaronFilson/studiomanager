# 10. SECURITY DEFINER functions for the few cross-tenant operations

- **Status:** accepted
- **Date:** 2026-10-20

## Context

Row-level security binds every query to one studio (ADR 3). Two operations legitimately cross
that line:

- **Accepting an invite.** The invitee isn't a member yet, so RLS hides the invite from them.
- **The hourly sweep.** It marks ready pieces past their hold as unclaimed in every studio, and
  it runs with no tenant bound.

Giving the application role `BYPASSRLS` would switch isolation off everywhere, and the startup
guard refuses to run with it anyway.

## Decision

Each crossing is a `SECURITY DEFINER` function owned by the schema owner, written to do exactly
one thing:

- `app.accept_invite(code)`
  - Looks up the invite by the SHA-256 hash of its code. Only hashes are stored, so a leaked
    table can't be used to join studios.
  - Checks the invite is unexpired and unused.
  - Adds the membership for the account bound to the transaction, and returns that studio alone.
- `app.mark_unclaimed_pieces(now)` moves READY pieces past their hold to UNCLAIMED and writes
  their timeline entries. It changes nothing else.

Both functions pin `search_path` to `pg_catalog, pg_temp` and schema-qualify every table, so a
planted object can't hijack them. Both are `REVOKE`d from `PUBLIC` and granted only to `app_rw`.

## Consequences

- The application role still can't read across studios. The only way across is through two
  audited functions, and the migration is their single definition.
- A new cross-tenant need means a new function and a review, not a quiet `BYPASSRLS`.
- Because the API scales to zero, the sweep runs only while an instance is up. Pieces past their
  hold are also flagged `overdue` when read, so correctness doesn't depend on when the sweep runs.

## In an interview

"RLS is all-or-nothing per role, so I didn't weaken the role. The two places that must cross
tenants are security-definer functions with a pinned search path. Each does one thing and is
executable only by the app's role. Invite codes are stored only as hashes, so even the function
can't reveal them."
