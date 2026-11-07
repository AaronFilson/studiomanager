# 25. Studio permissions behind the three roles

- **Status:** accepted
- **Date:** 2026-11-07

## Context

A studio owner needs to decide who can do what: let a monitor check people in without letting
them refund payments, let a bookkeeper see the ledger without managing kilns. Today every check
compares a ranked role (`MEMBER < STAFF < OWNER`), in about 155 annotations and a dozen private
`requireStaff()` copies, so nothing between "staff" and "owner" can be said, and the owner-only
actions are scattered literals. The plan, and the owner's answers to its questions, are
[plan 21](../planning/21-studio-rbac-plan.md).

## Decision

- **A fixed catalogue of named permissions.** `platform.tenancy.Permission` is a Java enum, one
  constant per action (`PIECES_MOVE`), each with a stable key (`pieces.move`) for the database
  and the apps, and a default grant: `FLOOR`, `MEMBER`, `STAFF`, `DUTY_ONLY` or `OWNER_ONLY`. The
  catalogue is code, not data: adding one is a code change plus, once roles are stored, a
  migration. Owners choose among permissions; they don't invent them.
- **The defaults reproduce today's checks one for one.** `Permission.defaultsFor(role)` gives
  each role exactly what its rank allowed, so seeding studios and switching checks over changes
  nothing until an owner edits a role. Background jobs keep the STAFF ceiling, resolved from these
  code defaults, never from a studio's edited STAFF set.
- **An OWNER holds every permission.** Owner-only permissions (export, AI key, roles, all
  tokens, purchase approval, managing owners) can never be held by another role. `TenantContext`
  refuses to be built otherwise, and the role tables will have a database check too.
- **Resolved once per request, checked in one place.** `TenantContext` carries the caller's
  `permissions`. `StudioPermissions.has` and `require` read it; controllers use
  `@PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_MOVE')")`, Spring Security's supported
  method-security pattern. Services check only rules that depend on the row (your own piece, the
  shift's duty). No action checks twice.
- **Duties add fixed permissions.** `Duty.grants()` is code; owners can't edit it. A person's
  permissions are their role's set plus their duties' grants.
- **No per-person overrides.** A person who needs something different gets a role.
- **RLS stays tenant-only** (ADR 3). Postgres never sees permissions.
- **Guard rails in the build.** An ArchUnit test fails on new code that branches on `StudioRole`
  outside `platform.tenancy`, and on a studio endpoint without `@PreAuthorize`. A contract test
  requires each studio operation in `studiomanager.yaml` to name its permission as
  `x-permission`, ratcheting down to zero as checks move over.

## Built in lanes

Lane R1 adds the catalogue, the defaults, `StudioPermissions`, `@studioAuthz.can`, permissions on
`TenantContext` (from code defaults) and the guard-rail tests, and changes no behaviour. Later
lanes replace the role checks in shadow mode (R2), expose `GET /me/permissions` and `can()` in the
apps (R3), store per-studio role sets with a one-minute cache, an audit log and enforcement (R4),
cap tokens and MCP by permissions (R5), and open the owner's roles page, custom roles and
co-owners (R6 to R8).

## Consequences

- One readable list says what can be granted, and the owner-only list is explicit.
- `TenantContext` grows a field; every way of building it still works, with code defaults.
- Until R2 lands, two vocabularies coexist (`hasRole` and `can`). The ArchUnit allow-list and the
  contract ratchet make the remaining work visible and stop it growing.
- Rejected: per-person grants and denies (hard to reason about); a policy or relationship engine
  such as Cedar, OPA or OpenFGA (too much machinery for studios of three to twenty people);
  permissions in RLS (splits each rule between Java and SQL).
