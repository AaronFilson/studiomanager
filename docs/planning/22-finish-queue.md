# 22. What's next after RBAC: plan 20's finish, and the pilot

_Written 2026-11-07, after PR #33 (RBAC, plan 21) merged and deployed. It reviews the
work against plans 20 and 21 and lists the next lanes in order. It replaces plan 20's "Status"
block, which stopped at wave 1 and is out of date._

## 1. Review against the plans

### Plan 21 (RBAC): complete

All eight lanes shipped in #33 (R1 to R8). The behaviour matches the decisions:
- day one is unchanged;
- each action has one named check (`@studioAuthz.can` plus `x-permission` on all 148 operations, with
  ratchet tests at 0);
- tokens are capped at person ∩ scopes;
- co-owners have the last-owner trigger and lock;
- custom roles, the owners' roles page and the access history are all in.

Where the build differs from the plan, each choice is recorded in plan 21 §8:
- **Shadow mode skipped in R2.** The default sets equal the role checks by construction. R4 has the
  switch (`STUDIOMANAGER_PERMISSIONS_MODE`), `enforcing` by default.
- **`studio_membership.role` kept.** A trigger keeps it equal to the base of `role_id`. Plan 21
  §6 step 6 (drop the column) is **still open**.

Still open from plan 21:
1. **Mobile owner picker (R8).** The mobile app has no members screen. Small. It can wait until
   mobile gets a members screen, unless the pilot owners manage from phones.
2. **Per-caller MCP `tools/list` (R5).** Blocked by the SDK. A refused call is clear. Leave it.
3. **Drop `studio_membership.role`** (§6 step 6). Find every reader first: `MembershipLookup`, V25's
   reminder SQL, `app.accept_invite`, the demo seed and the export.
4. **`AccessChanged` has no subscriber.** R6 publishes it, but neither the activity stream nor
   webhooks list it yet. §7 says both should.
5. **ADR 25's status.** Check that it says "accepted", with R6 and R7's sections.

### Plan 20 (pre-production cleanup): wave work done; the Finish section is open

Merged:
- foundation (#20);
- wave 1 (#25);
- first deploy (#21–#26);
- wave 2 (#28);
- via-token attribution (#29);
- the D, F1g and F3 lanes (#31);
- README (#32).

Not done, from plan 20's "3. Finish" and "Status → Next":

| Item | Where it stands |
|---|---|
| Error Prone default checks | Still `disableAllChecks = true` (`api/build.gradle.kts:156`) |
| `-Werror` | Not on. `-Xlint:all` is warnings only (`api/build.gradle.kts:152`) |
| `@NullMarked` everywhere | 27 packages marked. **Not marked:** `account`, `export`, `firing`, `importer`, `mcp`, `ratelimit`, `stream`, `today`, and the root package |
| Final status of plans 14 to 21 | Plan 20's status block stops at wave 1 (this doc replaces it) |
| Google and GitHub sign-in turned on (plan 18) | GitHub bridge built (#31). Check that it's switched on in dev |
| Demo rehearsal with a scheduled boost | Not recorded |
| Playwright smoke test and k6 numbers (plan 15) | Not built |
| JaCoCo and a threat model (plan 15) | Not built |
| Limits and SLO alerts tuned from real use | Not done |
| Real row sizes for the list caps | Not measured |
| Trace volume (`OTEL_TRACES_SAMPLER` honoured?) | Not checked |
| Custom email domain (before a real pilot) | Not done |

## 2. The queue

Lanes in order. A–C can run in parallel; D needs the deployed app.

| # | Lane | What | Size | Notes |
|---|---|---|---|---|
| A | **NullMarked finish** | Mark the 8 packages above and the root package. Fix what NullAway finds | S–M | Do it before B, so B's checks see final signatures |
| B | **Error Prone and `-Werror`** | Turn on the default checks. Fix or narrowly suppress each finding, with a reason. Then `-Werror` for javac and Error Prone | M | Big diff. Merge it alone, with nothing else open on `api/` |
| C | **RBAC tail** | Subscribe `AccessChanged` to the activity stream and webhooks (a new event type in the contract). Drop `studio_membership.role` in a migration once nothing reads it. Check ADR 25 | S–M | The contract changes, so regenerate the client |
| D | **Prove it on dev** | A Playwright smoke test against dev (sign in, studio, piece intake, roles page) as a manual workflow. k6 for the token and MCP limits. Measure row sizes on the demo studio, then settle the caps. Check trace volume | M | Feeds plan 15's README numbers and plan 20 §6's tuning |
| E | **Pilot readiness** | Custom email domain (ACS plus DNS). Turn on Google and GitHub sign-in in dev if not done. Rehearse the demo with a scheduled boost. JaCoCo badge. A threat model doc | M | The owner supplies the domain and DNS access |

After the queue, the deferred plans are next. Each needs an owner's go-ahead:
- plan 14, steps 3 to 5 (glaze book);
- plan 17, step 5 (OAuth for MCP);
- plan 09 (Genesis kiln log; the owner has two Genesis controllers);
- the mobile members screen, with the owner picker.

## 3. Decisions (owner, 2026-11-07)

1. **Mobile owner picker:** later, probably before the pilot, with a mobile members screen.
2. **Playwright against dev:** on every deploy. The assurance is worth waking the API.
3. **Email domain:** not chosen yet; the owner is searching. Lane E builds everything that doesn't
   need the domain, and the runbook lists the steps for when it's known.

All five lanes go in one PR (branch `finish-queue`), lane by lane, so CI checks the whole.
