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

## Editing roles (lane R6)

Owners edit roles on the roles page, through `GET /roles`, `PUT /roles/{role}/permissions`,
`POST /roles/{role}/reset` and `GET /roles/history`, all `studio.roles.manage`. The rules:

- **Only owners edit, and only STAFF and MEMBER.** `studio.roles.manage` is owner-only, so no one
  else can change any role, including their own. The OWNER role always holds everything and is
  shown, not edited.
- **Owner-only permissions are locked off** for every other role (decision 5). The API refuses
  them with 400, and the database check on `studio_role_permission` refuses them too.
- **The floor is locked on.** Every role always holds the self-service floor (`money.own.read`);
  it isn't stored, and the page shows it checked and disabled.
- **Everything else is editable,** including `DUTY_ONLY` permissions and `api_tokens.own` and
  `mcp.use` (decision 8). Unknown keys are refused, not ignored, on the way in.
- **A save names the whole set** and the role then holds exactly that (plus the floor). "Reset to
  defaults" sets `Permission.defaultsFor(role)`. Both write one `role_change_log` row with the
  sorted before and after, and publish `AccessChanged`, the Modulith event every access change
  (role permissions, duties, member roles, billing contact) now publishes from `AccessChangeLog`.
- **No version check.** The plan doesn't call for one: two owners saving at once is last write
  wins, and both saves are in the log. Before saving, the page says how many people hold the role
  and which permissions they lose.
- **No per-person overrides** (decision 7); duty grants stay fixed in code.
- **Changes apply at once on the instance that made them, everywhere within a minute** (the
  cache), and the page says so.
- **Words live with the API.** `PermissionCatalogue` gives every permission an area, a label and a
  description, with a case per permission and no default, so a new permission doesn't compile
  until it's described. Web and mobile read them from `GET /roles`.
- **Tokens never reach it.** `TokenPolicy` lists `roles` as an admin area, and no scope allows
  `studio.roles.manage`.

## Custom roles (lane R7)

Owners make named roles such as "Kiln tech" or "Board" (`POST`, `PUT` and `DELETE
/roles/custom[/{roleId}]`, all `studio.roles.manage`), and anyone with `members.role.assign` puts
members in them (`PUT /members/{accountId}/role`). The rules:

- **A custom role has a base of STAFF or MEMBER, never OWNER.** The base is what everything still
  keyed on a `StudioRole` sees: `TenantContext.role`, jobs' ceilings, `studio_membership.role`. What
  its people hold is the role's own set, plus the floor and their duties, never an owner-only
  permission (the API, `TenantContext` and V39's database check each refuse one; V41 adds a check
  that only the system OWNER role is OWNER-based). Its base can't change later; make another role.
- **Memberships point at a role row.** `studio_membership.role_id` names the system or custom role;
  `role` stays and always equals that row's base, kept in step by a V41 trigger, so R8's last-owner
  trigger and every query on `role` still work. The last-owner trigger now also fires on a change
  to `role_id` alone.
- **Resolved per request, cached per role row.** `TenantContextFilter` reads the membership's role
  row and base; `RolePermissions` holds each row's set for a minute, keyed by studio and row, and
  drops the studio's entries when any role changes on this instance.
- **Nobody gives more than they hold.** Assigning, and inviting, need the role's permissions (as
  the studio has set them, not code defaults) to be all the caller's own, and moving someone needs
  their current role to be too. Owners are made and unmade only with R8's owner endpoints, so
  neither side of an assignment may be OWNER. `GET /roles/assignable` lists what the caller may
  give.
- **Invites can name a custom role** (`roleId` with `role` as its base); `app.accept_invite` puts
  the new member in that row.
- **A role in use can't be deleted.** The API refuses with 409 ("give them another role first"),
  and a deferred foreign key from `studio_membership.role_id` refuses at commit whatever made the
  change. Deleting withdraws the role's open invites. We chose refusing over moving members as
  part of the delete, so nobody's access changes as a side effect of tidying roles.
- **Everything is logged:** `ROLE_CREATED`, `ROLE_RENAMED`, `ROLE_DELETED` (target the role's key,
  values its names), `ROLE_PERMISSIONS` for a custom role's set, and `MEMBER_ROLE` naming built-in
  roles as `STAFF` and custom ones by key.

## After the lanes (plan 22, lane C)

- **Access changes go out.** `studio.AccessChanged` becomes an `access.changed` webhook, sent in
  the changing transaction like `fee.charged`, and an `access.changed` event on the Kafka stream.
  The stream event carries ids only; the webhook also carries who made the change, by name.
- **Export downloads are logged** (V42) as `STUDIO_EXPORTED` in the same log, target `studio`,
  value the file's name. It isn't a change of access, but it is the owner-only action co-owners
  most need to see, and this log already has the account, the token, the roles page and the
  webhook. A separate audit log would duplicate all four.
- **`studio_membership.role` stays.** Plan 21 §6 meant to drop it once nothing read it. It is
  still read by the JPQL that finds owners and roles, by the last-owner trigger (V40), by
  `app.accept_invite` and by the export. V41's trigger keeps it equal to the base of `role_id`, so
  it can't drift. It is kept on purpose, as the role's rank, beside `role_id`, which names the role.

## Consequences

- One readable list says what can be granted, and the owner-only list is explicit.
- `TenantContext` grows a field; every way of building it still works, with code defaults.
- R2 replaced every `hasRole` with `can`. The ArchUnit allow-list and the contract ratchet keep it
  that way.
- Rejected: per-person grants and denies (hard to reason about); a policy or relationship engine
  such as Cedar, OPA or OpenFGA (too much machinery for studios of three to twenty people);
  permissions in RLS (splits each rule between Java and SQL).
