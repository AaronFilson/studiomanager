# 21. Owner-configurable roles and permissions

_Planned 2026-11-06, from the owner's note: "I will need RBAC system for the future so a studio
owner can set who and how things work." This is a plan only. No code changes with it._

_Revised 2026-11-07 with the owner's answers: several co-owners, money split, MEMBER editable,
every lane before the pilot. See "Decisions (2026-11-07)"._

## Decisions (2026-11-07)

1. **Several co-owners.** A studio has one or more owners. It always has at least one (§7).
2. **Money is split** into `money.take_payment` and `money.refund_and_adjust` / `money.payouts`.
   STAFF get both by default, so nothing changes on day one.
3. **MEMBER is an editable set,** like STAFF, above a fixed self-service floor: a member can
   always read their own pieces, balance, consignment and bookings.
4. **All lanes, R1 to R8, ship before the pilot** (§8).
5. **Owner-only permissions stay with owners.** They can never be given to any other role.
6. **Duties grant fixed permissions in code.** MONITOR's proposed grants are kept.
7. **No per-person overrides.** Make a role instead.
8. **Tokens and MCP:** staff-only by default, as `api_tokens.own` and `mcp.use`, which the
   owners may give to any role.
9. **Changes apply within about a minute** (cache).

## Summary

- Keep the three roles (OWNER, STAFF, MEMBER). Behind them, put a catalogue of named permissions.
- Each studio gets one editable permission set per role, seeded so day one behaves exactly as
  today. The owner can then add custom roles built from the same catalogue.
- A studio can have several co-owners, and always has at least one.
- Duties stay what they are: jobs that grant a few narrow permissions, on top of a role.
- Every action checks one permission in one place in the API. RLS stays tenant-only.
- API tokens are capped by the person's live permissions and their own scopes. The web and mobile
  apps ask `GET /me/permissions` instead of comparing role names.

## 1. Today

### How it works

- `StudioRole` is a ranked enum: MEMBER(1) < STAFF(2) < OWNER(3). `includes(minimum)` compares
  ranks. One role per person per studio, stored on `studio_membership.role`.
- `TenantContext` carries `accountId`, `studioId`, `role`, `kind` (person or system) and
  `viaTokenId`. It's bound in a `ScopedValue` by `TenantContextFilter`.
- Controllers check with `@PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")` or
  `isMember(...)` (155 annotations across 24 controllers).
- Most services check again with a private `requireStaff()` that throws `AccessDeniedException`.
  There are at least nine copies (`AccessService`, `LedgerService`, `PayablesChecks`,
  `ScheduleService`, `SupplyService`, `WebhookEndpoints`, `ApiTokens`, `Proposals`,
  `PendingActionReview`, `ImportService`, `PieceService`, plus `isStaff` helpers in
  `GlazeBook`, `ProblemReportService`).
- **Duties** (`access.Duty`: TECHNICIAN, MONITOR, WORK_EXCHANGE, VOLUNTEER_OFFICER, INSTRUCTOR)
  are held per member in `member_duty`. They gate a few member actions: taking a duty shift,
  completing a maintenance task assigned to a duty, and who gets problem-report alerts.
- **Certifications** gate booking a resource that needs one (`BookableResource.requiredCertification`).
  They are a fact about a person, not a permission.
- **System actors** (ADR 16) run at a role ceiling, all STAFF today.
- **API tokens** (ADR 19): `TokenPolicy` maps each route area to read-only, writable with a scope,
  or never. `TokenScope` has `read`, seven `write:*` scopes and `propose`. A token also needs its
  person to be staff, checked live on each call.
- **MCP** (ADR 22): `McpRequestFilter` requires staff; propose tools also need the `propose` scope.
- **Platform admin** (ADR 24): the Entra app role `Platform.Admin`, seen as
  `APPROLE_Platform.Admin`. It's outside any studio and out of scope here.
- **RLS** (ADR 3) is by studio only: policies compare `studio_id` to `app.current_studio_id()`.
  Postgres never sees the role. The only role use in SQL is V14, which finds a studio's OWNER for
  reminders. `app.principal_kind` and `app.platform_admin` are the other session settings.
- **Web and mobile** hide things with `isStaff(myRole)` (`web/src/labels.ts`,
  `mobile/src/labels.ts`) and `myRole === 'OWNER'` in four places.

### Inventory of checks

"Member" means any role in the studio. "Self" means the row must be the caller's own.

| Area | Where | What it guards | Role |
|---|---|---|---|
| Studio | `StudioController` | Read the studio | Member |
| Studio | `StudioService` | Creator becomes owner | (sets OWNER) |
| Members | `MemberController` (6) | List members, invite, email invites, change role, remove | Staff |
| Members | `StudioInvites` | Can't invite above your own role | Caller's role ≥ invited role |
| Members | web `members.tsx` | Only an owner offers "Owner" in the role picker | Owner |
| Duties, certifications | `AccessController`, `AccessService`, `Certifications` | Grant duties, certification types, certify members | Staff |
| Duties, certifications | `AccessController`, `Certifications` | Read your own duties and certifications | Member, self |
| Pieces | `PieceController`, `PieceService` | Take in your own piece, read pieces | Member |
| Pieces | `PieceService` | Take in for someone else, move stages, kiln queue, load contents, hold policy | Staff |
| Kilns, firings | `KilnController` (9) | Read kilns; create and edit kilns, firings, loads | Member read; staff write |
| Fees | `FeeController` | Read fee schedule, quote; edit fee schedule | Member; staff |
| Maintenance | `MaintenanceController` (20) | Tasks, records, parts | Staff; member reads and reports |
| Maintenance | `MaintenanceRecords.requireMayComplete` | Complete a task | Staff, assignee, or holder of the task's duty |
| Maintenance | `ProblemReportService` | Resolve a problem report; who is alerted | Staff; staff + TECHNICIAN duty alerted |
| Schedule | `ScheduleController` (12), `ScheduleService` | Resources, shifts, credited hours | Staff write; member read |
| Schedule | `BookingChanges` | Book a resource | Member, self only; certification if required |
| Schedule | `ShiftChanges.assign` | Take an open or offered shift | Member, self, must hold the shift's duty |
| Schedule | `ShiftChanges` | Offer your shift for cover | Holder of the shift |
| Schedule | `ScheduleService` | Read hours | Staff, or self |
| Supplies | `SupplyController` (9), `SupplyService`, `SupplyItems`, `SupplySales` | Catalogue, stock, sales | Member read; staff write |
| Supplies | `PurchaseRequests`, `PurchaseRequestEntity` | Request a purchase; approve one | Staff; **owner only** to approve |
| Invoices | `InvoiceController` (7) | Read and confirm supplier invoices | Staff |
| Glazes | `GlazeController`, `GlazeEdits`, `GlazeBook` | Studio glazes; members' own recipes | Staff for studio; owner of a member recipe |
| Money | `LedgerController` (16), `LedgerService`, `StoredValues`, `Payouts`, `OpeningBalances` | Payments, balances, gift cards, payouts, tax rates | Staff; member reads own balance |
| Money | `PayablesController` (16), `PayablesChecks`, `Consignments`, `PayeePayments`, `PayablesReports` | Payees, consignment, payment runs | Staff; member reads own consignment |
| Notices | `NotificationController`, `NotificationService` | Read notifications; send a studio notice | Member; staff |
| Today | `TodayController`, `TodayService` | Studio overview | Staff |
| Activity | `ActivityController` | Activity stream | Staff |
| Import | `ImportController`, `ImportService` | Import members and data | Staff |
| Export | `ExportController` | Download the studio's data | **Owner** |
| Webhooks | `WebhookController` (9), `WebhookEndpoints` | Manage webhooks | Staff |
| API tokens | `ApiTokenController`, `ApiTokens` | Make, list, revoke tokens | Staff; owner lists and revokes anyone's |
| AI key | `AiKeyController`, `AiKeys` | The studio's AI key | **Owner** |
| Proposals | `Proposals`, `PendingActionReview`, `PendingActionController` | Propose changes; approve suggestions | Staff |
| MCP | `McpRequestFilter`, `McpToolCalls` | Use the MCP server | Staff; `propose` scope for propose tools |
| Tokens | `TenantContextFilter.tokenContext` | Token's person must still be staff | Staff (live) |
| Jobs | `SystemActor` | Background jobs' ceiling | Staff |
| Platform | `AdminController` (7), `PlatformSettings` | Platform admin page | `Platform.Admin` app role |

Web and mobile: `isStaff` gates the notice form, glaze editing, calendar editing, supplies,
roster, piece stage moves, intake for others, and the staff home. `myRole === 'OWNER'` gates
purchase approval, the AI key card and the export card.

### What this means

- Two layers check the same thing (annotation and service). They agree today only because both
  say "staff".
- Nothing between "staff" and "owner" can be expressed. A studio can't let a monitor check people
  in without also letting them refund payments.
- The owner-only actions are scattered literals, not a list anyone can read.

## 2. Goals and non-goals

**Goals**
- The owner decides who can do what in their studio, from the app.
- Day one changes nothing: every studio behaves exactly as now until the owner edits something.
- One check per action, in one place, named for the action.
- A token, MCP session or job can never do more than its person (or its ceiling).
- The apps show what the person can do, without knowing role names.
- Every change to access is recorded.
- The owner can't lock themselves, or the studio, out.

**Non-goals**
- Per-row sharing (this piece, that kiln). Ownership rules like "your own recipe" stay in code.
- Attribute-based policy languages (Cedar, OPA, Rego) or a relationship engine (Zanzibar,
  OpenFGA). Too much machinery for three to twenty people per studio.
- Changing RLS to know roles.
- Platform admin roles. ADR 24 stands.
- Cross-studio roles (one person, many studios, already works: one membership each).

## 3. Options for the model

| Option | What the owner edits | Fits a small studio | Cost |
|---|---|---|---|
| A. Fixed roles, fixed permissions (today) | Who has which role | Yes, but too coarse | None |
| B. Fixed roles, owner-editable permission sets | Which permissions STAFF and MEMBER have | Yes | Small |
| C. B plus custom roles | Also new roles ("Monitor", "Bookkeeper") from the catalogue | Yes | Medium |
| D. Per-member overrides (grant/deny per person) | Single permissions per person | Hard to reason about | Medium, plus confusion |
| E. Relationship or policy engine | Rules and relations | No | Large |

How comparable products do it:
- **GitHub** organizations: fixed base roles, plus custom roles built from a list of
  fine-grained permissions on top of a base role
  ([docs](https://docs.github.com/en/organizations/managing-peoples-access-to-your-organization-with-roles/about-custom-organization-roles)).
- **Shopify** staff: roles bundle the permissions a job needs; the store creates and assigns them
  under Settings > Users > Roles ([docs](https://help.shopify.com/en/manual/your-account/users/roles)).
- **Square** Team: the account owner, and team members with team-management permission, create
  and edit permission sets and assign them to team members
  ([docs](https://squareup.com/help/us/en/article/5822-employee-permissions)).

_Links checked 2026-11-07._

The common pattern is named roles built from a fixed permission list, with an owner who always
has everything. Per-person overrides are rare and mostly replaced by "make another role".

**Recommendation: C, built in two steps (B first).** Why:
- B alone answers most studios: "let staff do X but not Y". It needs no new UI concept.
- C adds roles for the real jobs the research found (monitor, technician, bookkeeper) without
  per-person exceptions.
- No D. If a person needs something special, the owner makes a role for it. One place to look.
- Duties stay separate from roles. A duty says what job someone does on a shift or task; it
  also grants a few narrow permissions, fixed in code (below). Owners can't edit what a duty
  grants (decision 6).
- Certifications stay facts, checked by the action (booking), not permissions.

### How roles, duties and certifications combine

- A person's permissions = their role's permission set ∪ what their duties grant.
- Duty grants are small and fixed:

| Duty | Grants |
|---|---|
| TECHNICIAN | `maintenance.complete_assigned`, `maintenance.reports.resolve`, gets problem alerts |
| MONITOR | `schedule.shifts.take_duty`, `pieces.intake_for_others` |
| WORK_EXCHANGE | `schedule.shifts.take_duty`, `schedule.hours.log_own` |
| VOLUNTEER_OFFICER | `schedule.shifts.take_duty` |
| INSTRUCTOR | `schedule.shifts.take_duty` |

- "Take duty shift" still checks the shift's specific duty, as now.

## 4. Permission catalogue

Names are `area.action`. "Own" variants mean the row must be the caller's. Each lists the
default (S = staff, M = member, O = owner-only). Every owner always has every permission.

| Area | Permission | Default |
|---|---|---|
| Studio | `studio.read` | M |
| Studio | `studio.settings.edit` (name, units, piece policy) | S |
| Studio | `studio.export` | O |
| Studio | `studio.ai_key.manage` | O |
| Studio | `studio.roles.manage` (edit roles and permission sets) | O |
| Members | `members.read` | S |
| Members | `members.invite` (up to roles the caller can grant; see §6) | S |
| Members | `members.role.assign` | S |
| Members | `members.remove` | S |
| Members | `members.duties.manage`, `members.certify` | S |
| Pieces | `pieces.intake_own`, `pieces.read` | M |
| Pieces | `pieces.intake_for_others`, `pieces.move`, `pieces.policy.edit` | S |
| Kilns | `kilns.read` | M |
| Kilns | `kilns.manage`, `firings.manage` | S |
| Fees | `fees.read` | M |
| Fees | `fees.edit` | S |
| Maintenance | `maintenance.read`, `maintenance.report_problem` | M |
| Maintenance | `maintenance.manage`, `maintenance.complete_any`, `maintenance.reports.resolve` | S |
| Schedule | `schedule.read`, `schedule.book_own`, `schedule.shifts.take_open` | M |
| Schedule | `schedule.manage` (resources, shifts), `schedule.hours.manage`, `schedule.book_for_others` | S |
| Supplies | `supplies.read` | M |
| Supplies | `supplies.manage`, `supplies.sell`, `supplies.purchase.request`, `invoices.manage` | S |
| Supplies | `supplies.purchase.approve` | O |
| Glazes | `glazes.read`, `glazes.own.edit` | M |
| Glazes | `glazes.studio.edit` | S |
| Money | `money.own.read` (own balance, own consignment) | M |
| Money | `money.read` (ledger, reports) | S |
| Money | `money.take_payment`, `money.stored_value.manage` | S |
| Money | `money.refund_and_adjust`, `money.payouts`, `money.payables.manage`, `money.tax_rates.edit` | S (split from take-payment, decision 2) |
| Notices | `notices.read` | M |
| Notices | `notices.send` | S |
| Overview | `today.read`, `activity.read` | S |
| Integrations | `imports.run`, `webhooks.manage` | S |
| Integrations | `api_tokens.own` (make and revoke your own) | S |
| Integrations | `api_tokens.manage_all` | O |
| Integrations | `mcp.use`, `proposals.create`, `proposals.review` | S |
| Owners | `owners.manage` (add or remove co-owners) | O |

**Self-service floor.** These can't be removed from MEMBER or any role: read your own pieces,
balance, consignment, bookings, certifications and notifications. They aren't in the editable
set; the check is "it's yours".

**Owner-only** (O) permissions belong to the OWNER role alone and can never be added to another
role (decision 5): `studio.export`, `studio.ai_key.manage`, `studio.roles.manage`,
`api_tokens.manage_all`, `supplies.purchase.approve`, `owners.manage`.

About 55 permissions. Defaults reproduce today's checks one for one, so seeding is mechanical.
The catalogue is code (a Java enum), not data: adding one is a migration plus a code change, and
the owner only chooses among them.

## 5. Enforcement

### One check per action

- A `Permission` enum in `platform.tenancy`, and a `StudioPermissions` bean:
  `require(Permission)` and `has(Permission)`, reading the bound `TenantContext`.
- Controllers keep `@PreAuthorize`, with a permission instead of a role:
  `@PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_MOVE')")`. That's Spring Security's
  supported method-security pattern
  ([docs](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)).
- Services drop their private `requireStaff()` copies and call `permissions.require(...)` only
  where the check depends on the row (own vs other's, duty match, purchase approval). The
  annotation is the gate; the service check is for row rules. No action checks twice.
- An ArchUnit test fails the build on a new `StudioRole.` comparison outside `platform.tenancy`
  and on a studio controller method with no `@PreAuthorize`.
- A test walks every operation in `studiomanager.yaml` and asserts it names a permission (the
  same idea as `TokenPolicy`'s every-area test).

### TenantContext

- Add `Set<Permission> permissions` (an `EnumSet`) resolved once per request in
  `TenantContextFilter`: role set ∪ duty grants, capped for tokens (below).
- Keep `role` for display and for the jobs' ceiling. Code stops branching on it.

### RLS stays studio-coarse

- RLS answers "which studio's rows", and should keep doing only that. Permission checks are
  per action and often per row rule; putting them in policies would split each rule between Java
  and SQL, slow every query, and make tests harder.
- The tenant boundary, the risk RLS was chosen for (ADR 3), doesn't change.
- Exception kept as is: SECURITY DEFINER functions (ADR 10) check `app.principal_kind`. If one
  later needs a permission, set `app.permissions` as a text array in the same `set_config` call
  and check it there. Not needed now.
- V14's reminder query joins `studio_membership` on `role = 'OWNER'` and was written for one
  owner. With co-owners it must notify **every** owner: a new migration replaces it to return all
  owners (by the OWNER system role's `role_id`), and the reminder sender sends to each. A test
  covers a studio with two owners.

### Caching

- Resolve permissions once per request (one query joining membership, role permission set and
  duties). That's the same cost as today's role lookup plus one indexed join.
- A Caffeine cache keyed by `(studioId, roleId)` holds role permission sets for 60 seconds, and
  is evicted on any change made by this instance. With one API replica that's instant; with more,
  a 60-second delay is acceptable and stated in the UI ("changes apply within a minute").
- Duties are per person and read with the membership row. No cache.

### API tokens

- A token's effective permissions = (person's live permissions) ∩ (what its scopes allow).
  Token ≤ person, always, checked on every call as now.
- `TokenPolicy` stays the scope gate by area. Its `Never` areas (money, admin, invites, export,
  tokens, invoices, pending actions) stay never, whatever the role.
- Drop "the person must be STAFF" and replace it with "the person must have `api_tokens.own`".
  Same result on day one.
- Scopes don't become permissions. Scopes are coarse on purpose, so a person can reason about a
  token in one screen.

### MCP

- `McpRequestFilter` checks `mcp.use` instead of STAFF.
- Each tool declares the permission its REST twin needs. `McpToolCalls` checks it before
  running, so a tool can't reach what the person couldn't reach by REST. Propose tools need
  `proposals.create` and the `propose` scope.
- Tool listing hides tools the caller can't run.

### How the apps know what to show

- New `GET /api/v1/studios/{studioId}/me/permissions` returns
  `{ role, roleName, permissions: ["pieces.move", ...], duties: [...] }`.
- The web and mobile apps replace `isStaff(myRole)` and `myRole === 'OWNER'` with
  `can('pieces.move')`, from one React Query hook cached for the session and refetched on focus.
- Hiding is convenience only; the API is the authority.
- `StudioView.myRole` stays for display.

## 6. Data model and migration

### Tables

```
studio_role            (id, studio_id, key, name, base, is_system, created_at, updated_at)
  key: 'owner' | 'staff' | 'member' | custom slug; base: OWNER | STAFF | MEMBER
studio_role_permission (studio_id, role_id, permission)        -- PK all three
studio_membership      + role_id  (FK studio_role)             -- role column kept for a release
role_change_log        (id, studio_id, actor_account_id, via_token_id, kind, target, before, after, at)
```

- All tenant tables, with the usual `studio_id` RLS policy and `@TenantId`.
- `base` is the rank a custom role sits at, for two rules: who may assign it, and the job
  ceiling. Owner-only permissions (§4) can't be given to any role except OWNER (decision 5); the
  API refuses them and the database has a check against the owner-only list.
- The OWNER role is a system role with every permission, not editable. Any number of members may
  hold it.

### Migration, no behaviour change

1. **Migration V38:** create the tables. For every studio, insert the three system roles and
   seed STAFF and MEMBER with exactly the defaults in §4. Set `role_id` from `role`.
2. **Dual-write release:** membership writes set both `role` and `role_id`. Permission checks run
   in **shadow mode**: the old role check decides, the new permission check runs too, and a
   mismatch logs a warning and a metric. Tests assert zero mismatches for every endpoint and
   role.
3. **Switch:** the permission check decides. The old checks are deleted.
4. **Owner UI** opens: editing STAFF and MEMBER sets.
5. **Custom roles** open.
6. A later migration drops `studio_membership.role` once nothing reads it.

New studios get the same seed from `StudioService` when created.

## 7. Audit, lock-out, safe defaults

### Audit

- Every change to a role, a permission set, a membership's role or a duty writes a
  `role_change_log` row in the same transaction, with before and after, and "via token" when
  relevant.
- Each also publishes a Modulith event, so the activity stream and webhooks (plan 10) can show it.
- Every owner sees the log on the roles page, with the acting account on each row. Kept for the life of the studio.

### Can't lock out

- **A studio always has at least one owner.** The last owner can't leave, be removed, or be
  demoted. The API refuses it with a plain message ("Add another owner first"), and a deferred
  database trigger on `studio_membership` refuses any transaction that would leave a studio with
  no OWNER, so a bug or a race can't get past it either.
- **Any owner can add or remove other owners** (`owners.manage`). Promoting someone to OWNER, or
  demoting an owner, needs that permission; no other role can hold it. An owner may demote
  themselves while another owner remains.
- Two owners demoting each other at once: both transactions take a lock on the studio's owner
  rows (`SELECT ... FOR UPDATE` on the OWNER memberships), so the second sees the first's result
  and is refused if it would leave none.
- The OWNER role always has every permission; no UI or API can edit it.
- `studio.roles.manage` is owner-only, so no one else can change permissions, including their own.

### Accountability with several owners

- **Every owner holds every owner-only permission.** There's no "primary" owner inside a studio.
  Each can manage the AI key, export, approve purchases, manage roles and add or remove owners.
- **Who did what is recorded, not who is "the" owner.** Every owner-only action (AI key set or
  removed, export downloaded, owner added or removed, role edited, purchase approved) writes the
  acting account to `role_change_log` or the area's own history, as tokens already do with "via".
- **The AI key** stays the studio's key. The log records which owner set it and when. Any owner
  can replace or remove it.
- **Billing** (when the platform charges): the studio has one billing contact, an account that
  must be an owner. If that owner stops being an owner, the API asks the remaining owners to pick
  one; until then notices go to every owner.
- **Notices meant for "the owner"** (maintenance reminders through V14, purchase requests
  waiting for approval, owner-added alerts) go to every owner.
- Adding or removing an owner notifies every owner, so a co-owner can't be added quietly.
- Nobody can assign a role they couldn't hold themselves (today's "can't invite above your own
  role", generalised: the assigned role's permissions must be a subset of the caller's).
- A role in use can't be deleted until its members are moved.
- Removing a permission from STAFF that the caller is using takes effect on the next request;
  the UI warns which members lose what before saving ("3 people lose: refund payments").

### Safe defaults

- New permissions added in later releases default to **off** for every non-owner role, unless
  the migration that adds them says otherwise and says why (usually: splitting an existing
  permission, which copies the old one's grants).
- Unknown permission in the database is ignored and logged, not granted.
- A request with no resolvable role is refused, as now.
- Jobs keep the STAFF ceiling, resolved against the code defaults, not the studio's edited
  STAFF set, so an owner can't break reminders by trimming staff.

## 8. Build plan

| Lane | What | Size | Depends on |
|---|---|---|---|
| R1 | `Permission` enum, `StudioPermissions`, `@studioAuthz.can`, TenantContext gets permissions resolved from role defaults in code (no tables yet). ArchUnit and contract-coverage tests | M | none |
| R2 | Replace every `hasRole`, `requireStaff`, `isStaff` and `== OWNER` with permissions, in shadow mode, module by module (can split per module) | L | R1 |
| R3 | `GET /me/permissions`, generated client, web and mobile `can()` hook; remove `isStaff` | M | R1 |
| R4 | Tables, V38 seed, dual-write, cache, role_change_log, switch from shadow to enforcing | M | R2 |
| R5 | Tokens and MCP: token = person ∩ scopes; `mcp.use`; per-tool permissions; tool listing filter | S | R2 |
| R6 | Owner UI: roles page, edit STAFF and MEMBER sets, diff warning, audit log view; extends ADR 25 | M | R3, R4 |
| R7 | Custom roles: create, rename, delete, assign; invites by role | M | R6 |
| R8 | Co-owners: add and remove owners (`owners.manage`), last-owner trigger and lock, V14 replacement notifying every owner, owner-meant notices to all owners, billing contact, web and mobile owner picker | M | R4 |

**Every lane ships before the pilot** (decision 4). Order:

1. R1.
2. R2 and R3 in parallel (R2 can split per module).
3. R4, then R5 and R8 in parallel.
4. R6, then R7.
5. Pilot.

### Status

**R1 done (2026-11-07), branch `rbac-r1`.** No behaviour change, no migration.
- `platform.tenancy.Permission`: the §4 catalogue, 64 constants with keys and a default grant
  (`FLOOR`, `MEMBER`, `STAFF`, `DUTY_ONLY`, `OWNER_ONLY`); `defaultsFor(role)`, `ownerOnly()`,
  `byKey(key)` (unknown keys are empty, for R4's "ignore, don't grant").
- `TenantContext.permissions` (resolved from code defaults; the old constructors still work). It
  refuses owner-only permissions on a non-owner and anything above a job's ceiling defaults.
  R4 plugs in by building the context with the stored set plus duty grants instead of defaults.
- `StudioPermissions.has/require` and `@studioAuthz.can(#studioId, 'PIECES_MOVE')`, for R2.
- `Duty.grants()` (the table in §3), not yet added to a caller's permissions; R4 does that.
- ADR 25. Tests: `PermissionTests`, `StudioPermissionsTests` (`can` equals `hasRole` for every
  role and permission), `DutyTests`, `PermissionArchitectureTests` (role-branching allow-list
  that R2 empties; every studio endpoint has `@PreAuthorize`), `PermissionContractTests`
  (`x-permission` on operations, ratchet at 148 unnamed, which R2 brings to 0).

Choices R1 made that the Decisions don't cover:
- `maintenance.complete_assigned`, `schedule.shifts.take_duty` and `schedule.hours.log_own`
  are in the catalogue as `DUTY_ONLY`: no role but OWNER holds them by default; duties grant them.
- `money.own.read` is the `FLOOR` grant (every role, not editable), per decision 3, rather than an
  editable MEMBER permission as the §4 table shows.
- The contract names permissions with an `x-permission` extension on each operation.
- The ADR for the model is 25 (R1); R6 extends it rather than writing it.

**R3 done (2026-11-07), branch `rbac-r3`.** No behaviour change.
- `GET /api/v1/studios/{studioId}/me/permissions` (`getMyPermissions`, tag `permissions`,
  `x-permission: studio.read`) returns `{ role, roleName, permissions, duties }`: the bound
  context's permissions by `Permission.key()`, sorted, and the caller's duties. `roleName` is the
  role's name in title case until R6 names roles. `MyPermissionsController` lives in `access`,
  next to the duties it reads. `TokenPolicy` lists the `me` area as read-only.
- `useCan(studioId)` in `@studiomanager/api-client`, shared by web and mobile: one React Query
  query, cached and refetched on focus like the studio. `PermissionKey` names the keys the apps
  check, so a typo doesn't compile. The web studio layout waits for it, so every page has the
  answer on first render.
- `isStaff` is gone from both apps, and so is every `myRole === 'OWNER'`. Nav links, the notice
  form, calendar and roster editing (`schedule.manage`), glaze editing, supplies (and
  `supplies.purchase.approve` for approving), piece moves (`pieces.move`), intake for others,
  inviting owners (`owners.manage`), the AI key and export cards each check their own key.
  `myRole` stays for display.
- Web test fixtures serve permissions for the role the test's studio names (`test/permissions.ts`).
  Test: `MyPermissionsApiTests` (each role gets exactly `defaultsFor`, duties listed, stranger 403).
- Not yet: duty grants in the response's `permissions` (R4 adds them to the context). Until then,
  a duty doesn't change what the apps show.

**R2 done (2026-11-07), branch `rbac-r2`.** No behaviour change, no migration.
- Every studio endpoint checks `@studioAuthz.can(#studioId, '...')`; `hasRole` and `isMember`
  are left only as the reference `StudioPermissionsTests` compares against.
- Every service check uses `StudioPermissions.require(permission, message)` (new overload: keeps
  each area's old message and returns the caller) or `TenantContext.holds` for row rules ("your
  own", "staff see everyone's"). The private `requireStaff`/`isStaff`/`requireOwner` copies are
  gone. Service checks stay alongside the annotations, since MCP tools, proposals and jobs call
  the services without one.
- Choosing other people by role (problem and webhook alerts, maintenance reminders, the token
  owner's live check) uses `StudioPermissions.roleHolds(role, permission)`, from code defaults;
  R4 changes it in that one place.
- "Can't invite above your own role" is now "the invited role's permissions must all be yours",
  the §7 rule, equal to the rank rule on day one.
- `mcp.use` in `McpRequestFilter` and `api_tokens.own` in `TenantContextFilter` (R5's two
  swaps) are done here too, as one-for-one replacements; R5 keeps the token ∩ scopes work.
- Contract: `x-permission` on all 148 studio operations; `PermissionContractTests` ratchet at 0.
  A new test checks each endpoint's `can` matches its operation's `x-permission`, and another
  that every studio endpoint checks `can`.
- `PermissionArchitectureTests`' list is now `ASSIGNS_A_ROLE`: classes that name a role only to
  give it (creator becomes owner, imports invite members, contexts bound for after-commit work,
  the demo seed). Code that only assigns a role is exempt; nothing on the list decides access.
- Shadow mode was skipped: the default sets equal the role checks by construction (and by
  `StudioPermissionsTests`), so the switch is direct. R4 still runs shadow mode when stored sets
  arrive.

Mappings that weren't one-for-one (all equal to the old role on day one):
- Seeing everyone's pieces: `pieces.move` (no read-all permission in the catalogue).
- Kiln queue and load contents: `firings.manage`. Staff's view of reservations: `schedule.manage`.
- Member endpoints whose row rule is in the service (complete a task, resolve a report) use
  `maintenance.read`; offering a shift uses `schedule.shifts.take_open`; piece photos use
  `pieces.intake_own`.
- Certification types, certifications and tax rates for members: `studio.read`.
- Own notifications: `notices.read` (MEMBER, not the floor); own statement and consignment:
  `money.own.read`.
- Comparing invoice readers (owner only): `studio.ai_key.manage`. Deleting a studio (owner only,
  demo reset): `owners.manage`. Opening balances: `money.refund_and_adjust`.
- Consignment sale: `money.take_payment`; its refund: `money.refund_and_adjust`.

**R4 done (2026-11-07), branch `rbac-r4`.** Migration V39; no change for any role on day one.
- Tables `studio_role` (three system roles per studio: `owner`, `staff`, `member`),
  `studio_role_permission` (by `Permission` key) and `role_change_log`, each with the usual
  `studio_id` RLS policy and `@TenantId` (studio module: `StoredRole`, `StoredRolePermission`,
  `AccessChange`). V39 seeds every existing studio with the code defaults; `StudioService.create`
  seeds new ones from `RolePermissions.storedDefaults()`; `RolePermissionMigrationTests` checks
  the SQL lists against the code.
- Only the editable part is stored: OWNER holds everything in code and has no rows, and the floor
  (`money.own.read`) is added in code. A check on `studio_role_permission` refuses the owner-only
  keys and the floor, so the database can't give an owner-only permission to another role.
- `platform.tenancy.RolePermissions` builds each request's context in `TenantContextFilter`: the
  role's stored set (plus the floor, minus anything owner-only), plus what the person's duties
  grant (`access.DutyGrants`), or everything for an owner. A studio with no stored role of that
  rank falls back to the code defaults. Jobs keep their ceiling's code defaults. The token check
  (`api_tokens.own`) now reads the resolved person, and `roleHolds` moved from a static on
  `StudioPermissions` to `RolePermissions`, reading the stored sets.
- Cache: Caffeine, keyed by studio and role, one minute, dropped after commit on the instance that
  made the change (`RolePermissionsChanged`). Duties are read per request, not cached.
- Shadow mode: `studiomanager.permissions.mode` (`STUDIOMANAGER_PERMISSIONS_MODE`), `enforcing` by
  default. In `shadow` the code defaults decide and a differing stored set logs a warning with the
  role name and how many permissions it adds and drops, and counts
  `studiomanager.permissions.shadow_mismatch` by role.
- Write path for R6: `StudioRoles.setPermissions(role, permissions)` (`studio.roles.manage`;
  refuses OWNER and owner-only permissions). It and `AccessService.setDuties` write a
  `role_change_log` row in the same transaction (actor, via token, sorted before and after).
- Behaviour that does change: duty grants are now held (decision 6). A MONITOR member can take in
  pieces for others, and a TECHNICIAN member can resolve problem reports and sees every report.
- Tests: `RolePermissionsTests` (unit), `StudioRolesTests`, `RolePermissionMigrationTests`, and
  `MyPermissionsApiTests.aDutyAddsWhatItGrants`.

Left for later lanes:
- `studio_membership.role_id` and dual-writing it: with only system roles a membership's role row
  is found by `(studio_id, base)`. R7 adds the column when custom roles need it; memberships are
  also written by `app.accept_invite`, which R7 changes with it.
- The last-owner trigger and lock (R8). Modulith events for access changes, and showing the log
  (R6).

**R8 done (2026-11-07), branch `rbac-r8`.** Migration V40; several co-owners per studio.
- Endpoints (tag `members`, all `x-permission: owners.manage`, under `members` so `TokenPolicy`'s
  read-only `members` area covers them): `PUT` and `DELETE /members/{accountId}/owner`
  (`addOwner`, `removeOwner`), `GET` and `PUT /members/billing-contact` (`getBillingContact`,
  `setBillingContact`). Removing an owner makes them STAFF; an owner may remove themselves
  (stepping down) while another owner remains.
- Last owner: `StudioService.removeOwner` locks the studio's OWNER memberships
  (`SELECT ... FOR UPDATE` through `@Lock(PESSIMISTIC_WRITE)`) before deciding, refuses the last
  one with 409 "... Add another owner first.", and refuses a caller who stopped being an owner
  since their request began (403). Of two owners demoting each other at once, the second waits,
  sees the first's result and is refused. V40's deferred constraint trigger
  `studio_membership_keeps_an_owner` refuses at commit any transaction that leaves a studio with
  no OWNER, past the API too; a deleted studio is skipped.
- Billing contact: `studio.billing_contact_account_id`, set to the creator for new studios and to
  the earliest owner for existing ones. It must be an owner (API 409, and the same trigger, also on
  `studio`). Demoting the contact clears it; `GET` then answers `chooseNeeded: true` and the
  owners' notice asks them to pick one.
- Notices: a new `OWNERS_CHANGED` kind goes to every owner (found by `roleHolds(..., owners.manage)`,
  which only owners hold) and the person concerned, when an owner is added or removed, including
  someone accepting an OWNER invite (after commit, from `StudioService.onMemberJoined`).
  `role_change_log` gains `MEMBER_ROLE` and `BILLING_CONTACT` rows.
- V14: its single-owner lookup was already replaced by V25 (studio ids only), and the reminder
  goes to every `maintenance.manage` holder, so every owner. V40 changes no reminder SQL;
  `MaintenanceReminderTests.aStudioWithTwoOwnersRemindsBoth` covers a two-owner studio. No other
  SQL or notice picks "the" owner; purchase requests have no notice yet.
- Web: the members page shows owners "Make … an owner", "Remove … as owner" / "Step down as
  owner" (not offered to the only owner) and a Billing contact card, all behind
  `can('owners.manage')`.
- Tests: `CoOwnerTests` (add and notices, staff refused, last owner refused at the API and at the
  trigger, concurrent mutual demotion leaves one owner, billing contact handover and trigger, owner
  invite notice), the two-owner reminder test, and web `members.test.tsx`.
- Not done: the mobile owner picker (the mobile app has no members screen yet).

R1 to R5 change no behaviour. R6, R7 and R8 are the features the owner asked for. Sizes: S ≈ a
day, M ≈ two to four days, L ≈ a week. In plan 20's terms, these lanes join the pre-production
queue.

## 9. Open questions for the owner

None open. All nine were answered on 2026-11-07; see "Decisions (2026-11-07)" at the top.

## References

- ADR 3 (RLS), ADR 10 (security definer), ADR 16 (system principal), ADR 19 (API tokens),
  ADR 22 (MCP), ADR 24 (platform admin).
- Plans 17 (MCP and tokens) and 20 (pre-production cleanup).
- GitHub custom organization roles, Shopify staff roles, Square team permissions (links above).
