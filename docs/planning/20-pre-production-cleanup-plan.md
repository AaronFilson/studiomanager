# 20. Cleaning up before production

_Planned 2026-11-06, from the owner's notes on [19](19-java-practices-review.md). Azure costs
more than the owner's other hosting, so the planned work gets finished, and the code made clean,
before the first deploy. This plan records the decisions, answers the questions raised, and
orders the work._

_Reviewed the same day:_
- _the fixes are listed under "Review notes";_
- _the open work this plan didn't cover is in section 9;_
- _the order now runs in parallel lanes._

## Status (2026-11-06)

**Done: the foundation, wave 1, and the first deploy.** Dev runs on Azure in West US, with
sign-up through External ID, the admin page and email working.

| Step | PR | Notes |
|---|---|---|
| 0. Foundation | #20 | |
| 1. Wave 1: lanes C, P, M, S, A | #25 | Merged together in the planned order (C, P, M, S, A). ADRs 21–24 written. List caps use this plan's proposed numbers; real row sizes are still to be measured |
| First deploy | #21–#24, #26 | Below |

**What the first deploy changed:**
- **#21:** dev's Entra and database settings; the encryption keys Terraform never set (the API and every job now get them as Container App secrets); v2 tokens on the web and mobile registrations, which External ID requires; the `Microsoft.Communication` provider; bootstrap's GitHub federation now trusts GitHub's immutable OIDC subject (owner and repository by id).
- **#22:** the image build's AOT training run starts without a database: Spring Modulith's event registry, scheduling and eager initialization are off in that profile only. `AotTrainingStartTests` reproduces the build's start.
- **#23:** everything moved to West US (Static Web Apps to West US 2, where it's offered). Postgres was refused in East US 2 for this subscription. Bootstrap's region now defaults to `westus`, and the runbook says to choose it first.
- **#24:** the web app's authority carries the tenant id (`https://<subdomain>.ciamlogin.com/<tenant-id>/`); the bare host serves no OpenID metadata.
- **#26:** a custom "Studio Manager Email Sender" role (defined in bootstrap, assigned to the API identity on the ACS resource). A test invite arrived.
- **Branch protection on `main`:** the API, web and Terraform checks are required; admins can bypass.
- **External ID puts `roles` in access tokens:** confirmed. The admin role goes to the account that signs in to the site (a customer account), not the tenant administrator.

**In progress:** "via \<token name\>" attribution in histories (plan 17 step 2's remainder), migration V37.

**Next:**
- Wave 2 (lanes 2a–2d) and the lanes alongside it (D, F1, F1g, F2, F3, R). All can start, since P is merged.
- Before a real pilot: a custom email domain (the Azure-managed sender lands in junk and has low limits).
- Measure real row sizes for the list caps on the seeded demo studio.
- Check trace volume: the Azure Monitor starter may ignore `OTEL_TRACES_SAMPLER` (lane S's note).

## Decisions

- **No limits on a studio's own AI calls.** They use the studio's own key, so the studio pays.
  The first version of the app is free, and the platform spends no tokens of its own on studio
  work.
- **MCP and API tokens get rate limits,** sized so a normal user gets their work done quickly
  and moves on (section 6).
  - Token calls on REST get a higher limit than MCP.
  - Both are unlimited during a demo boost.
  - The numbers will be tuned by trying them, so they're settings, not code.
- **MCP runs on the official MCP Java SDK.** Building on top of it is fine, but the SDK must be
  what handles the protocol (section 5).
- **Hibernate fits the write side,** and plain SQL stays only where it has a performance or
  special-case reason (section 1).
- **One clock,** in the normal Spring form and no more (section 3).
- **NullAway goes in.** What it is and why: section 2.
- **List caps stay.** The user is told when a list is cut short, and the caps people browse are
  raised where the cost model allows (section 4). This replaces 19's keyset pagination.
- **The first hosted use is likely a demo for a prospective employer.** For 15 to 30 minutes, the
  MCP and token limits must not get in the way. Afterwards they come back gradually, if wanted.
  - Limits become live settings, changed from a new platform admin page with a timed "boost",
    instead of configuration that needs a new revision (section 7).
  - AI calls on the studio's own key already have no limit, so the demo needs nothing extra
    there.

## Review notes (2026-11-06)

What a second pass found, and where each one is now handled:

1. **Open work was missing.** "Finish the planned work before production" includes the open
   steps of plans 14, 16, 17 and 18, and this plan didn't list them. They're in section 9, and
   they need the owner's call.
2. **Plan 16's Google fallback broke a decision.** It runs on the platform's own GCP project, so
   the platform would pay for studio work. Resolved by the owner: Google stays, for the demo and
   the resume, but as a reader the admin page controls. It's off by default, and on only for
   chosen studios, under a monthly page cap (section 9).
3. **Scheduled work runs only while the API is awake.** ADRs 10, 11 and 14 accepted this for a
   pilot. In production it means maintenance reminders, the unclaimed-piece hold and retries
   fire only when someone happens to use the app. Fix: a Container Apps cron job runs the sweeps
   a few times a day, inside the free grant (section 8).
4. **jqwik may not work with Boot 4's JUnit 6.** Plan 14 found its support unverified, and 19 and
   this plan recommended it without saying so. Verify it first, or keep the seeded
   `@RepeatedTest`s (section 8).
5. **Error Prone would break the build on day one.** It brings a few hundred checks of its own,
   and with `-Werror` they'd fail on the existing code. NullAway runs alone at first; the
   default checks and `-Werror` come at the end (section 2).
6. **Spotless's first run reformats every file.** Done midstream, that conflicts with every lane.
   It happens in one commit before anything branches, listed in `.git-blame-ignore-revs`.
7. **Rate-limit property names said `mcp`,** but the limits now cover tokens too. They're
   `studiomanager.rate-limit.mcp.*` and `studiomanager.rate-limit.token.*`.
8. **Settings were re-read on a timer.** They're now re-read on the next request after 15
   seconds, so nothing runs while the app is idle.
9. **Number clashes.** ADRs 17, 18 and 20 are already claimed by plans 14, 16 and 18, so this
   plan's ADRs are 21 to 24. Migration numbers are reserved per lane (Order section).
10. **Cross-module entity links.** New rule: entities never map another module's table (section 1).
    That keeps Modulith's boundaries, and it lets modules be converted in parallel.
11. **The list envelopes and problem types were spread across modules.** They move out of the
    module conversions into one contract lane, so only one lane edits the contract.
12. **The test runners can only run one at a time.** `scripts/wsl/integration-inner.sh` and
    `web-linux-inner.sh` use fixed folders (`/tmp/sm-verify`, `/tmp/sm-web-ci`), so two runs
    would clobber each other. That's fixed before anything runs in parallel.
13. **Plans 19 and 20 are uncommitted on `main`.** Worktrees branch from commits, so they're
    committed first.

## Hosting cost, in brief

These are list prices for East US 2, checked on 2026-11-06 through the Azure Retail Prices API
and the pricing pages.

| Scenario | Monthly |
|---|---|
| Idle: scaled to zero, about 3 active hours a day | about $16, nearly all of it Postgres B1ms ($12.41 compute, $3.68 storage) |
| Pilot: 5 studios, about 6 active hours a day | about $21: Postgres $16.09, Container Apps $4.46, email about $0.50 |
| The same pilot, with logs at the 0.5 GB/day quota every day | about $50: 15.5 GB over the 5 GB free allowance, at $2.76/GB |
| The API awake 24/7 | adds about $34 |

### What drives the bill
- **Replica time drives Container Apps, not requests.** A replica costs $0.054 an hour, and the
  free grant covers 100 replica-hours a month. A replica stays up 5 minutes after the last
  request. Waking from zero costs about six replica-minutes, which is more than serving a
  5,000-row list.
- **Postgres is the only charge that's always on.** The new-account free offer doesn't apply to
  an existing account, so the bill can only drop meaningfully by moving the database. That's out
  of scope here.

### Cost guards that belong in this plan
- **Turn on response compression.** `server.compression` isn't set, so JSON goes out
  uncompressed. Turn it on for `application/json` and `application/problem+json`.
- **Keep the API asleep:**
  - The web Today page polls every 60 seconds (`web/src/routes/studios/$studioId/today.tsx:19`).
    A front-desk tablet left on Today keeps a replica up all day. Stop polling after a few idle
    minutes, or poll every 5 minutes.
  - Steady polling by a token shows up on the admin usage page, and the assistant guide points
    integrations at webhooks (section 6).
  - Don't set up external uptime pings.
- **Log volume:** lower `log_daily_quota_gb` from 0.5 to about 0.15, which stays inside the
  free 5 GB. Also set an OpenTelemetry trace sampling ratio, so the quota is a backstop rather
  than the thing that cuts logs off at midday.
- **Stay on the Consumption plan.** Don't turn on Container Apps' Dedicated profiles, private
  endpoints or planned maintenance. Since 2026-09-01 each of those costs $0.10 an hour (about
  $73 a month). Check the first invoice for those meters anyway.

## 1. Hibernate for the write side, SQL where it earns its place

**Today:**
- 229 `JdbcClient` statements across 34 classes, plus 3 JPA entities (`Account`, `Studio`,
  `StudioMembership`).
- 51 hand-written inserts, each passing `studioId` by hand.
- 19 hand-written optimistic-locking updates (`version = version + 1 where version = :version`),
  which is exactly what `@Version` does.
- Hand-written row mappers for each of them.

**Why Hibernate fits:**
- **Most writes are CRUD on one aggregate with a version.** That covers assets, tasks and
  records, resources, bookings and shifts, kilns and firings, pieces and photos, supplies,
  glazes, webhook endpoints, tokens, invites, payees and consignment items.
- **`@TenantId` (Hibernate's discriminator multi-tenancy) makes the application's tenant check
  automatic.** A `CurrentTenantIdentifierResolver` reads `TenantContext`. Hibernate then sets
  `studio_id` on every insert and adds `studio_id = ?` to every entity query, and row-level
  security still sits underneath. Both layers of "tenant isolation in two layers" become
  automatic, which is a better interview story than either one alone.
- **Hibernate sessions already get the RLS settings.** The tenant is bound in a
  `JpaTransactionManager` (`TenantAwareJpaTransactionManager`), so nothing changes there. The
  precondition is the same as for RLS today: the tenant is bound before the transaction starts.
- **Some of the plumbing is already in place:**
  - `ddl-auto: validate` already checks entities against Flyway's schema at startup.
  - `open-in-view` is already off.
  - Spring Data JPA (already a dependency) brings repositories and auditing for `created_at`
    and `created_by`.
  - Hibernate 7.4 (from Boot 4.1) maps records as embeddables, so `Money` and the measurements
    can be embedded.

**Where SQL stays, and why:**

| Kind | Examples | Why SQL |
|---|---|---|
| Reports and totals | ledger balances, statements, the 1099 list, hours totals, Today counts, weekly activity, the export | Set-based sums and joins in one round trip, with no entity to load |
| Claims and queues | webhook delivery claim (`update … returning`), reminder claim (`reminded_for is distinct from`) | An atomic test-and-set in one statement, safe across replicas |
| Counters and upserts | `piece_counter`, piece policy, AI key, activity projection (`on conflict …`) | Race-free in one statement |
| Set-based moves | `PieceService.advanceAll` after a firing | One statement for N pieces, all or nothing |
| Cross-tenant functions | the `app.*()` security-definer calls | By design (ADR 10) |
| Postgres-only features | the kiln queue's `lateral` join, the first photo per piece (`distinct on`), the RLS guard's catalog query | No JPQL equivalent worth the trade |
| The tenant binding | `set_config(...)` | It's the mechanism itself |

Roughly two thirds of today's statements become entity code (an estimate from sorting the
statements by kind). Each module keeps its SQL in one package-private `…Queries` class, so it's
easy to see where SQL is used and why.

**Rules to set in the first module:**
- **Flush before SQL in a write transaction.** `JdbcClient` doesn't see entity changes Hibernate
  hasn't flushed yet. In a write transaction, read through the `EntityManager` (its native
  queries flush first), or call `flush()` first. Plain `JdbcClient` is for read-only report
  transactions and for the claims and sweeps that don't load entities.
- **No N+1 queries.** Set `hibernate.default_batch_fetch_size`, use fetch joins or entity
  graphs for lists, and assert statement counts in tests with Hibernate's `Statistics`.
- **Check the client's version explicitly.** The client sends the version it saw. Compare it
  after loading, and `@Version` then catches a race between the load and the commit.
- **Calls with no studio bound.** "My studios" and the sweeps that find studios run with no
  studio bound. `Account`, `Studio` and `StudioMembership` don't carry `@TenantId`, and the
  resolver returns a value that matches nothing when no studio is bound. Prove this on the
  first module.
- **Measure startup** before and after. The AOT cache is trained with Hibernate's metamodel, so
  the cost should stay small, but measure it.
- **Entities never map another module's table.** Links across modules stay ids, as they are
  now. That keeps Modulith's boundaries, and it's what lets modules be converted in parallel.

**Order:**
- Kiln goes first, to set the patterns.
- The rest go in four parallel lanes (see "Order, and what runs in parallel").
- Within the money lane, the order is fixed: payables, then the ledger. Its postings become
  entities, and the balances stay SQL.

The HTTP contract doesn't change, so the 200-plus API integration tests are the regression net
for every step. The large services (`PayablesService`, `MaintenanceService`, `LedgerService`,
`ScheduleService`) get split by use case as they're converted, which takes care of 19's
finding 12.

ADR 21 records "Hibernate with `@TenantId` for the write side; SQL for the cases above".

## 2. NullAway: what it is, and what it does here

**The problem.** Java's types don't say whether a reference can be null, so the compiler can't
catch a `NullPointerException`; it only shows up at run time. Right now the code carries 253
`@Nullable` annotations in 76 files, and the generated contract classes carry them too. To the
compiler they're comments: nothing checks them.

**The two pieces:**
- **JSpecify** is the standard set of null annotations, agreed by Google, JetBrains, Spring,
  Oracle, Uber and others (1.0 in 2024). `@NullMarked` on a package means "nothing here is null
  unless it says `@Nullable`". The code already uses JSpecify's `@Nullable`, but no package is
  `@NullMarked` yet.
- **NullAway** (from Uber) is a compiler plugin, running on Google's Error Prone, that reads
  those annotations while `javac` runs. It fails the build where code could:
  - dereference something that may be null;
  - pass a maybe-null value where null isn't allowed;
  - return null from a method that promises not to.

  It's fast enough to run on every build. Spring Framework 7 and Boot 4 use exactly this pair on
  their own code. Spring's APIs are annotated, so NullAway also checks how this code calls into
  Spring.

**Examples from this code:**
- **A real runtime risk caught at compile time.** `FeeService.Row.toSchedule()` passes the
  `@Nullable String unit` to `VolumeUnit.valueOf(unit)`. A per-volume row with no unit would
  throw an NPE far from its cause. NullAway flags the line, and the fix is a
  `requireNonNull` with a message that names the bad row.
- **A design hint.** `PieceService.intake` calls `size.length().value()` after checking
  `size.hasDimensions()`. NullAway can't see that one implies the other, so it flags the call.
  The cleaner design gives `PieceSize` one nullable `Box` holding all three dimensions, so the
  check and the use can't drift apart. NullAway pushes the code toward types where illegal
  states can't be represented.
- **Entities.** Required columns become non-null fields, and nullable columns become
  `@Nullable` fields, so the Java says what the migration says.

**The benefit for this codebase:**
- Signatures can be trusted.
- Reviews stop asking "can this be null?".
- A whole class of production crashes becomes a compile error, which matters most in money and
  tenancy code.

**What it costs:**
- A few percent of compile time.
- A one-time annotation pass.
- Suppressions in a few known spots: JPA no-arg constructors get
  `@SuppressWarnings("NullAway.Init")`.

**Rollout:**
- Error Prone and NullAway in `build.gradle.kts`, with `JSpecifyMode=true` and
  `OnlyNullMarked=true`. Only `@NullMarked` packages are checked, so the build stays green
  throughout.
- Generated sources are excluded.
- **Error Prone's own checks stay off at first.** Its default checks are disabled and only
  NullAway is on, as an error. With `-Werror`, its few hundred default checks would break the
  build on the existing code. They're sorted out at the end, together with `-Werror`.
- Each module is marked `@NullMarked` in the same PR that converts it to Hibernate, so each
  module's code is only touched once. `platform` goes first.

## 3. One clock, the normal way

The host clock is fine; this isn't about accuracy. It's about tests being able to say what "now"
is. Injecting a `java.time.Clock` has been standard Spring practice since Java 8. It's neither
new nor clever, and the Hibernate work needs it anyway: Spring Data auditing fills `created_at`
from a `DateTimeProvider`, which should read the same clock as everything else.

**The whole change:**
- One bean, `Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000))`. Postgres stores
  microseconds and Java produces nanoseconds, so a value read back would otherwise differ from
  the one returned. That mismatch is why 22 classes each have their own `now()` today, and the
  bean replaces all of them.
- Services that work out dates (hold end, due dates, backoff, token expiry) take the `Clock`.
- Tests get a small settable clock in the test sources, instead of jumping 40 days with
  `plusSeconds`.

It's done module by module, alongside the Hibernate conversion.

## 4. List caps: keep them, say so, and raise the ones people browse

16 endpoints cut their results at a fixed number. The contract mentions a few of them ("Up to
500 pieces"), but the web and mobile apps never tell anyone.

**Cost.** At an assumed 1.5 KB of JSON a row, before compression:
- a 2,000-row response is about 3 MB;
- past the free 100 GB, it costs about $0.00026 in egress;
- the CPU is negligible;
- the free 100 GB covers about 33,000 such responses a month, and compression stretches that
  several times further.

So money isn't what limits list size. Phone payloads, render time and memory are. Lists people
browse can go to 2,000. The rest stay where they are, with a message.

| List | Now | Proposed |
|---|---|---|
| Pieces (stage and maker filters) | 500 | 2,000 |
| Kiln queue (pieces waiting in a stage) | 1,000 | 1,000 |
| Firings per kiln | 100 | 500 |
| Maintenance records per asset | 500 | 500 |
| Problem reports | 200 | 500 |
| My notifications | 100 | 100 |
| Bookings in a range (at most 62 days) | 2,000 | 2,000 |
| Shifts in a range (at most 62 days) | 2,000 | 2,000 |
| A person's hours entries (the total is computed separately) | 500 | 1,000 |
| Payouts | 500 | 1,000 |
| Stored values and gift cards | 500 | 2,000 |
| Consigned items | 500 | 2,000 |
| Consignment sales | 200 | 1,000 |
| Payees | 500 | 1,000 |
| Stock history per item | 200 | 1,000 |
| Webhook deliveries, last 30 days | 200 | 500 |

**The response changes.** Each of these lists becomes `{ items, truncated, limit }` in the
contract. Nothing is deployed yet, so changing the shape costs nothing beyond updating web and
mobile in the same PR. All 16 change together in the contract lane (C), not module by module,
along with a shared `Capped<T>` result type that the services return.
- The query asks for `limit + 1` rows. If it gets that many, it sets `truncated`. No count query
  is needed.
- When `truncated` is set, the apps say so, for example: "Showing the newest 2,000 pieces.
  Filter by stage or member to find older ones." Where a list has no filter, the message points
  to the studio export, which has no cap.
- MCP tool results carry the same flag.

Measure real row sizes on the seeded demo studio before settling on the numbers.

## 5. MCP on the official MCP Java SDK

The reason it was written by hand no longer holds. These findings were checked on 2026-11-06
against Maven Central and the SDK and Spring AI sources.

**Versions:**
- **MCP Java SDK 2.1.0** is the current GA (2026-10-15).
- The SDK's own README says the Spring integrations moved into **Spring AI 2.0**.
- **Spring AI 2.0.2** (2026-10-22) supports Spring Boot 4.0 and 4.1, and is built on SDK 2.1.0.
  Spring AI 2.1 moves to Boot 4.2.

**Dependencies:**
```kotlin
implementation("io.modelcontextprotocol.sdk:mcp:2.1.0")           // the protocol, and Jackson 3 binding
implementation("org.springframework.ai:mcp-spring-webmvc:2.0.2")  // WebMvcStatelessServerTransport
```
Not the full Spring AI starter. The SDK's server does all the protocol work, and the transport
only reads requests and writes responses. The starter would add `spring-ai-model`'s dependencies,
and it hides the one setting that matters inside an environment check.

**Why it works here:**
- **Threading.** `McpServer.sync(transport).immediateExecution(true)` runs each tool on the
  calling thread. Without it, tools go to Reactor's `boundedElastic`. The transport calls the
  server with `.block()` on the servlet thread, so tools run inside `TenantContext.callAs`, and
  row-level security sees the right studio. The flag was added because thread hops lost the
  security context (SDK PR #371, Spring AI PR #3794). Spring AI's own autoconfiguration turns it
  on for servlet apps. Here it's set explicitly, with a comment saying why.
- **The per-studio path.** The WebMVC transport is a `RouterFunction`, and router patterns take
  URI variables, so the endpoint stays `/api/v1/studios/{studioId}/mcp`. The security chain,
  `TenantContextFilter` and `TokenPolicy` run before it, exactly as now.
- **Jackson 3.** `mcp` 2.1.0 bundles `mcp-json-jackson3`. Pass the app's mapper through
  `new JacksonMcpJsonMapper(jsonMapper)`. No Jackson 2 databind comes in.
- **Protocol versions.** The stateless server accepts 2025-03-26, 2025-06-18 and 2025-11-25, the
  same three as today.

**What stays ours, built on top:**
- **Staff only.** A router function can't take `@PreAuthorize`. A filter on the router calls
  `StudioAuthorization.hasRole(studioId, STAFF)` and keeps the existing "bound to this studio"
  check.
- **`MCP-Protocol-Version`.** The transports don't check this header, so the router filter keeps
  today's check. It also adds the `Allow: POST` header to the 405, which the SDK leaves out.
- **Origin.** `DefaultServerTransportSecurityValidator` with the CORS origins. A missing Origin
  passes and a foreign one gets 403, as today. SDK 2.1.0 deprecates this type in favour of
  `ServerHttpHeaderValidator`, but Spring AI 2.0.2's transport only accepts the old one. So
  it needs a targeted `@SuppressWarnings("deprecation")`, since the build will run with
  `-Werror`.
- **Tools.** `McpTools` becomes a list of `SyncToolSpecification`s with the same names, schemas
  and annotations. Each handler maps only the app's own exceptions to `isError`, as now, and the
  rate limit (section 6) wraps every handler.

**Checks for the first commit**, since these come from reading the code, not running it:
- the `{studioId}` pattern matches in the transport's router;
- a malformed request still gets a 400.

Also, both transports refuse a POST whose `Accept` header doesn't list both `application/json`
and `text/event-stream`, as the spec requires. Real clients send both, but `McpApiTests.rpc()`
sends only JSON and needs updating.

**Later:** SDK 2.2.0-M2 previews a stateless server for the next spec revision. Move to it once
it's GA.

ADR 22 records it: "MCP on the official SDK; `immediateExecution` keeps tools on the request
thread". It replaces the "not the MCP Java SDK" note in plan 17.

## 6. Rate limits for MCP and API tokens

**The aim.** A person, or their assistant or script, gets a task done quickly and moves on. The
limits are generous during a burst of work and only slow a long session down a little. What they
mainly stop is runaway loops.

**What costs money is how long a replica stays awake, not the work per call.** A replica stays up
for 5 minutes after the last request. Past the free grant, that costs about $0.054 an hour.
- **Work per call.** A REST call often does less on the server than an MCP tool, because some
  tools gather several lists in one call (`studio_today`). Either way, the CPU per call is too
  small to matter.
- **Spread.** 500 calls in ten minutes cost one short wake-up. One call every 4 minutes keeps a
  replica up all day, which is about $34 a month.
- **Counts can't see spread.** A call count can't tell a busy person from a slow, steady poller.
  So the limits handle bursts and runaway loops, and steady polling is handled by watching for it
  (below).

| | MCP tool calls | API token calls (REST) |
|---|---|---|
| Burst | 30 | 200 |
| Refill | 1 every 10 s | 2 a second |
| Daily cap | 500 | 5,000 |
| Sized for | A session: a few questions, each taking several tool calls | A script's job: a sync across several lists, or a batch of updates |

- **What each window is for:**
  - The burst lets a task finish without waiting.
  - The refill keeps a long session moving.
  - The daily cap stops a stuck agent or script from looping for hours.
- **Steady polling:**
  - The usage page shows how many separate hours each token was active in the last day, so a
    poller stands out.
  - The assistant guide points integrations at webhooks: tokens are for doing work, and webhooks
    are for watching for changes.
  - If polling ever costs real money, a cap on active hours per token is the next step. It isn't
    built now.
- **Keys and counting:**
  - **One bucket per person, per studio, per channel.** A person has one bucket for MCP and one
    for REST in each studio. All of a person's tokens share them, so a second token doesn't
    double the allowance.
  - **What counts on MCP:** only `tools/call`, however the caller signed in. `initialize`, `ping`
    and `tools/list` are free.
  - **What counts on REST:** every token request, except those to the MCP endpoint, which count
    only against MCP.
  - **Not limited:** signed-in use of the web and mobile apps.
- **When a bucket is empty:**
  - **MCP:** the tool result comes back with `isError`, saying how long to wait, so the
    assistant can tell the person.
  - **REST:** the call gets `429 Too Many Requests` with `Retry-After`.
  - Both are logged and counted.
- **Library:** Bucket4j, which supports several windows on one bucket. Buckets live in memory on
  each replica, so with at most two replicas the real ceiling is up to twice the setting, which
  is acceptable and documented. Bucket4j's Postgres backend can make it exact if that ever
  matters.
- **Tuning.** Configuration (`studiomanager.rate-limit.mcp.*` and
  `studiomanager.rate-limit.token.*`) sets the defaults, and the admin page changes them live,
  with no restart (section 7).
  - A boost lifts them for a set time, then eases them back down.
  - Calls and refusals per token go to the call log and to metrics, so the tuning works from
    real sessions.
  - Start with the values above. Try a scripted "morning check-in" session (today, open
    firings, low supplies, maintenance due, problems) and a glaze-batch session, then adjust.
- **Changing limits on a running app.** Each bucket is rebuilt with the new configuration
  (Bucket4j's `replaceConfiguration`) only when the effective limit changes: a saved setting, or
  the next step of a boost. While a boost is unlimited, the bucket is skipped altogether.
- **Decided:** token calls on REST get their own, higher limit rather than sharing MCP's, and a
  boost lifts both (section 7).

## 7. The admin page: live limits, a demo boost, and the demo studio

**The need.** The first hosted use will likely be a demo for a prospective employer. For 15 to 30
minutes, assistants mustn't hit a limit. Afterwards the limits should come back gradually, not
all at once. That takes limits that change while the app runs, and a page to change them.

### Who is an admin

A platform admin runs the service. That's a different thing from a studio role, and the code has
no such role yet.
- **The role lives in Entra:** an app role, `Platform.Admin`, on the API's app registration
  (`azuread_application.api` in `infra/identity/main.tf`). It's assigned to the owner's account
  with `azuread_app_role_assignment`, taking the user's object id from a tfvar.
- **The API reads it from the token.** The role arrives in the access token's `roles` claim, and
  the API maps it to an authority. `/api/v1/admin/**` requires that authority.
- **The web app** shows "Admin" only to people who have it.
- **API tokens can never reach it.** `TokenPolicy` already refuses every path outside a studio.
- **Check on the first deploy** that the External ID tenant puts `roles` in access tokens. This
  plan hasn't verified it.

### Live settings

- **Storage.** A `platform_setting` table (V29) holds a key, a JSON value, a version, and who
  changed it and when. `platform_setting_change` keeps every change: the old and new values,
  who, and when.
- **Defaults.** Configuration still sets the defaults. A value saved on the admin page overrides
  its default, and "Back to defaults" removes the override.
- **Reaching both replicas.** A replica re-reads the settings on the first request after its
  copy is 15 seconds old, and at once after it saves them itself. A change applies everywhere
  within about 15 seconds, with no restart and no new revision. Nothing runs on a timer, so an
  idle app does no work.
- **Two layers, as everywhere else in the code.** The database refuses writes unless the
  transaction is bound as a platform admin. `app.platform_admin` is set per transaction, the same
  way as ADR 16's principal kind.

### The boost

A boost lifts the limits for a fixed time.

| Phase | Demo preset | Effect |
|---|---|---|
| Unlimited | 30 min | No MCP or token limits. Calls in this phase don't count toward the daily cap |
| Easing | 60 min, in three steps | 8×, then 4×, then 2× the normal limits |
| After | | Normal limits |

- **Presets:** "Demo: 30 minutes unlimited, then ease down over an hour", and "15 minutes
  unlimited, no easing". Custom values are allowed.
- **Scheduling:** it can start now, or at a set time, so it's ready before the interview.
- **Scope:** one studio (the demo preset picks the demo studio) or all studios.
- **It always ends on its own,** at most 4 hours after it starts, and "End now" goes back to
  normal at once. A forgotten boost costs at most about four replica-hours (about $0.22).

### The admin page (web, `/admin`), first version

1. **Limits.**
   - What applies right now, in words: "Unlimited for 23 more minutes, then easing to normal by
     15:40".
   - The normal values, the presets, a custom boost, and "End now".
2. **Usage.** MCP and token calls and refusals in the last hour and the last day, by studio and
   token, with how many separate hours each token was active. This is built from the call log
   in 17, step 2, which is built here. It's also what tuning reads.
3. **Demo studio.**
   - "Reset the demo studio" re-seeds "Demo Clay Co." (15, gap 1) with kilns, pieces, firings,
     glazes, maintenance and a ledger.
   - "Make a demo token" creates a read-scoped token, valid for a day, for connecting an
     assistant during the demo.
4. **History.** Every setting change, with who made it and when.

**Later controls, same mechanism:** log levels with an automatic revert, and a notice banner
across the site.

**Out of scope here:** how an interviewer signs in to the demo studio. 15's "try as staff" idea
is still open.

### Keeping the API warm is not on the admin page

Scaling is an Azure setting, and the API shouldn't hold rights over its own infrastructure. For a
demo, open the site a few minutes beforehand. Optionally, a `workflow_dispatch` GitHub workflow,
"Warm for an hour", sets the minimum replicas to 1 through the existing keyless deploy identity,
and back to 0 afterwards. That costs about $0.05 an hour.

**Interview story:** "Limits are tunable at run time from an audited operator page. The page is
gated by an app role from the identity provider, and the database enforces it too. Before a demo
I schedule a boost that lifts the limits for half an hour and eases them back over the next
hour."

## 8. Carried over from 19, unchanged

- **Merge `WebhookSecrets` and `AiKeyCipher`** into one cipher, with a key-version byte and a
  keyring, so keys can rotate (19, finding 5).
- **Problem `type` URIs and codes** in errors (19, finding 7).
- **`TokenPolicy` as an exhaustive record-pattern `switch`** (19, finding 8).
- **Logging fixes:**
  - pass exceptions to the logger, in the four warnings that drop the stack trace (19,
    finding 4);
  - SLF4J key-value fields for the ECS logs.
- **Build hygiene** (19, finding 9):
  - `-Xlint`, then `-Werror`;
  - Spotless;
  - a version catalog;
  - Gradle configuration cache and build cache;
  - a wrapper checksum.
- **Modulith tests:** `@ApplicationModuleTest` and `Scenario` tests for the event flows (19,
  finding 10). Plan 14 found jqwik's support for JUnit 6 (which Boot 4 brings) unverified, so
  jqwik is checked first; if it doesn't fit, the seeded `@RepeatedTest`s stay.
- **Scheduled work:**
  - **Runs while asleep.** The sweeps, retries and reminders only run while a replica is up.
    A Container Apps job on a cron trigger (every 6 hours) runs the same image with a `sweep`
    profile. The profile runs each sweep once and exits, like the migration job. That's about
    a minute a run, inside the free grant.
  - **Two replicas.** An advisory lock (`pg_try_advisory_xact_lock`) around each sweep, so the
    job and a replica never run the same sweep at once (19, finding 11).
  - **ADR 23** records both.
- **Metrics** (19, finding 4):
  - outbox lag;
  - webhook outcomes;
  - Kafka consumer lag;
  - sweep timings;
  - MCP calls and refusals.

  The code is built now. The SLO alerts get tuned after the deploy.
- **ADRs** (17, 18 and 20 are already claimed by plans 14, 16 and 18):
  - 21, data access (section 1);
  - 22, MCP on the SDK (section 5);
  - 23, scheduled work;
  - 24, operator controls and the platform admin role (section 7).

## Dropped

- **Rate limits on studio AI calls.** The studio's own key, the studio's own bill.
- **Keyset pagination.** Replaced by the caps and the `truncated` flag (section 4).

## 9. Other open plans

These are the open steps of earlier plans. "Finish the planned work" covers them, so each needs a
decision: before production, or after.

| Plan | Open steps | Recommendation |
|---|---|---|
| 16, invoice OCR | 2: the inbox and the Claude reader. 3: the Google fallback. 4: the matcher and vendor aliases. 5: the eval set | **All of it before**, with step 3 reworked as admin-controlled readers: Google Document AI and Azure AI Document Intelligence (below). |
| 17, tokens and MCP | 2: "via token" attribution and the revoke email (the call log and limits are already in this plan). 4: pending actions and propose tools. 5: OAuth for MCP. Also left from step 1: the last-used IP, and expiry after 90 days unused | **Step 2 and step 1's leftovers before.** **Step 4 before, if the demo should show safe writes.** **Step 5 after.** |
| 18, Google and GitHub sign-in | 1: Google through Entra (configuration). 2: a GitHub OIDC bridge, which is its own Container App | **Build step 2 now; turn steps 1 and 2 on at the first deploy.** |
| 14, glaze book | 3: the mix log. 4: import from glazecalc and glaze-vault. 5: photos and promote | **After.** |
| 15, resume review | README screenshots and a diagram, badges, a Playwright smoke test, k6 numbers, JaCoCo, a threat model | **README, diagram and badges before the demo.** **The rest right after the first deploy.** |
| 09, kiln log import | Waits for a real Genesis export | **After.** |

**The owner's decision (2026-11-06): all of it is in before production.**
- Plan 16, all steps, with the Google and Azure readers.
- Plan 17: steps 2 and 4, and step 1's leftovers.
- Plan 18: step 2, built now and turned on at the first deploy.
- Plan 15: the README work.
- The formatter is Google Java Format.

**Why each recommendation:**
- **Plan 16.**
  - **Steps 2, 4 and 5:** reading an invoice with Claude is the strongest AI feature to demo, and
    it runs on the studio's own key.
  - **Step 3** becomes the invoice readers below.
- **Plan 17.**
  - **Step 2 and step 1's leftovers** are small, and they're about auditing.
  - **Step 4** is the "an assistant proposes, a person approves" story.
  - **Step 5:** tokens already work with Claude's clients.
- **Plan 18.** Both steps need the real Entra tenant, so they can't be finished before the first
  deploy. A technical interviewer can sign in with GitHub, which answers section 7's open
  question.
- **Plan 14.** The owner already deferred these, and import needs sample exports from the
  owner.
- **Plan 15.** The README work is small. Playwright and k6 run against the deployed app.

### Invoice readers: Claude, Google and Azure, side by side

The owner wants a Google option in the demo, for the resume, controlled from the admin page.
It's in scope.

**Scope verdict: medium, and off the critical path.** It's about the size of the webhooks work.
It runs as its own lane alongside wave 2, so it doesn't push the deploy back. Most of the effort
is cloud plumbing, not reader code.

**The readers**, all behind one `InvoiceReader` interface:

| Reader | Who pays | Reaching it | Price (checked 2026-11-06) | Size |
|---|---|---|---|---|
| **Claude**, structured output | The studio's own key | Already built (plan 16 step 1) | The studio's own bill | In lane F1 |
| **Google Document AI**, Invoice Parser | The platform | Keyless, from Azure: workload identity federation | $0.10 per 10 pages, billed in blocks of 10 pages per document ([pricing](https://cloud.google.com/document-ai/pricing)) | Medium |
| **Azure AI Document Intelligence**, prebuilt invoice model | The platform | The API's managed identity. The same cloud, so no federation | F0 is free for 500 pages a month ([pricing](https://azure.microsoft.com/en-gb/pricing/details/ai-document-intelligence/)). The paid S0 rate wasn't confirmed | Small |

**Google, keyless from Azure:**
- **The GCP side.** A workload identity pool trusts the Entra tenant. Its provider accepts the
  API's managed-identity token. A service account with `roles/documentai.apiUser` is
  impersonated through it. No service-account key exists anywhere.
- **Getting the token.** Container Apps serves managed-identity tokens from `IDENTITY_ENDPOINT`,
  not from the VM metadata endpoint. `gcloud`'s ready-made `--azure` credential file reads the
  metadata endpoint, so it won't work here. Instead, a programmatic subject-token supplier in
  google-auth-library-java fetches the token through `azure-identity`, which is already a
  dependency. **Unverified:** the supplier's exact class name and signature. Check them in the
  first commit.
- **Calling Document AI.** Through its REST endpoint with Spring's `RestClient`, plus only
  `google-auth-library-oauth2-http`. The gRPC client library would pull in gRPC, protobuf and
  Guava, which adds startup time and weight to the AOT cache.
- **Terraform.** A new `infra/gcp` root module enables the API, creates the Invoice processor
  (`google_document_ai_processor`) and sets up the pool, provider and service account. The owner
  applies it once by hand, like `bootstrap` and `identity`, so CI needs no GCP identity.

**Azure.** An `azurerm_cognitive_account` of kind `FormRecognizer` on F0, with the API identity
holding `Cognitive Services User`. It's one module, and it uses the same managed identity as the
database login.

**Admin page controls** (on the section 7 settings store):
- **Each platform-paid reader:**
  - on or off;
  - which studios may use it (none, chosen studios, or all);
  - a monthly page cap per studio and for the whole platform.
- **Shown for each reader:** this month's pages and the estimated spend.
- **Order per studio:** for example, "Claude, then Azure", or "Google only" for the demo.
- **Defaults:** off for every studio except the demo studio. The platform pays nothing unless
  the admin turns a reader on.

**What the studio controls.** Invoices sent to Google or Microsoft go to another processor, so
the studio owner still has to consent. Step 1's `allow_google_fallback` becomes
`allow_platform_readers`, in F1's migration.

**For the demo: "Compare readers".** It runs one invoice through every reader allowed for the
studio, and shows the fields side by side, with the time and cost of each. It's a small screen
on top of the eval code.

**For the resume: the eval set (plan 16 step 5).** It runs every reader against the labelled
invoices in CI. That gives measured accuracy, time and cost per page, comparing an LLM with two
purpose-built document AI services. Those are README numbers.

**Other options, not in this plan:**
- **AWS Textract `AnalyzeExpense`:** a third cloud. It needs its own identity federation
  (`AssumeRoleWithWebIdentity` with the Entra token), which makes it medium scope for a
  repeated story.
- **Gemini on Vertex AI:** a second LLM reader on the same GCP federation. Small once Google is
  in, and an option for later.
- **Tesseract:** text only, with no invoice structure. It would still need an LLM to make sense
  of the text.

**What the owner needs to provide:**
- a GCP project with billing (a new account's credit covers far more than the demo);
- one manual apply of `infra/gcp`;
- 5 to 10 real supplier invoices for the eval set and the demo.

## Order, and what runs in parallel

Each lane is one branch and one PR, built by one agent in its own git worktree. Lanes run at the
same time wherever they don't share files. The elapsed time then becomes the longest chain of
lanes, not the sum of all of them.

### 0. Foundation: one PR, before anything branches

Everything else would conflict with these, because they touch the build file or every Java file:
1. Commit plans 19 and 20.
2. **Parallel test runs.** `integration-inner.sh` and `web-linux-inner.sh` get one work folder
   per checkout, under `/tmp/sm-verify/<checkout>` and `/tmp/sm-web-ci/<checkout>`. The JDK
   download, the Gradle home and the pnpm store stay shared. Gradle locks its shared caches, so
   builds can run side by side.
3. **One formatting commit.** Spotless with a formatter close to the current style, applied to
   every file in one commit. That commit is listed in `.git-blame-ignore-revs`.
4. **Build settings:**
   - the version catalog;
   - the configuration and build caches;
   - the wrapper checksum;
   - `-Xlint`, as warnings for now.
5. **Error Prone with only NullAway on,** and `OnlyNullMarked`.
6. **Small shared pieces:** the `Clock` bean and compression. Every lane needs them.

### 1. Wave 1: five lanes, once the foundation is merged

| Lane | What | Owns | Numbers |
|---|---|---|---|
| **C: contract and UI** | The 16 list envelopes with `Capped<T>`: the contract, controllers, `limit + 1` on today's queries, web and mobile messages, mobile fixtures. Problem types: the contract schema, `ApiExceptionHandler`, client errors. Today polling that stops when idle | The OpenAPI contract, the generated client, web, mobile | none |
| **P: Hibernate base** | The `@TenantId` resolver, auditing, batch fetch size, the statement-count helper, `@NullMarked` on `platform`. Then kiln, converted as the pattern for the rest | `platform/tenancy`, `kiln` | ADR 21 |
| **M: MCP and limits** | MCP on the SDK. Bucket4j limits for MCP and REST tokens, behind a `RateLimitSettings` interface backed by configuration at first. The call log, written with a plain insert: an append-only hot path. Plan 17 step 2 and step 1's leftovers | `mcp`, a new `ratelimit` package, the token filter | V30, ADR 22 |
| **A: admin** | The `Platform.Admin` app role (Terraform) and its mapping. The `app.platform_admin` binding. `platform_setting` with the database-backed `RateLimitSettings`. The admin API and page: Limits and History first, Usage once M's call log is merged | A new `admin` module, `infra/identity`, `web` admin routes | V29, ADR 24 |
| **S: security and operations** | The merged cipher with key rotation, the `TokenPolicy` switch, the logging fixes, metrics, the sweep job, advisory locks, the log quota and sampling | Webhook and AI-key crypto, `TokenPolicy`, `infra/modules`, the sweeps | ADR 23 |

- **Merge order:** C first, because it changes the return types of the 16 list services, and P,
  M and S touch some of them. The other lanes then rebase onto it. After that, any order.
- **Where the contract changes:** only C and A edit it in this wave.
- **A and M:** they meet at the `RateLimitSettings` interface, which is agreed up front. Neither
  waits for the other until A's Usage section.
- **No migration for key rotation.** S changes the stored cipher format, but nothing is deployed,
  so there are no stored secrets to convert.

### 2. Wave 2: the module conversions, once P is merged

| Lane | Modules | Numbers |
|---|---|---|
| **2a** | glaze, then supplies | V31 if needed |
| **2b** | maintenance, then schedule (resources point at assets) | V32 if needed |
| **2c** | piece and photos, then notices. Then webhooks, tokens and the AI key, once S is merged | V33 if needed |
| **2d** | studio, access and invites. Then payables, then the ledger, in that order | V34 if needed |

Each lane, for each of its modules:
- converts it to Hibernate, keeping SQL only for the reasons in section 1;
- marks it `@NullMarked` and moves it to the clock;
- splits its large service;
- adds its Modulith tests.

No lane in this wave edits the contract.

**Alongside wave 2,** once their prerequisites are merged:

| Lane | What | After | Numbers |
|---|---|---|---|
| **D: demo studio** | Seed data through the services. "Reset the demo studio" and "Make a demo token". The "Warm for an hour" workflow. A test that a reset succeeds, which also catches wave 2 breaking the seed | A | none |
| **F1: invoices** | Plan 16 steps 2, 4 and 5, written in the Hibernate style from the start: the inbox, the `InvoiceReader` interface, the Claude reader, the matcher, the eval set. Renames the studio opt-in to `allow_platform_readers` | P | V35, ADR 18 (already reserved) |
| **F1g: Google and Azure readers** | The two adapters, keyless access to Google through workload identity federation, `infra/gcp` and the Azure module, the admin page's reader controls, "Compare readers" | F1's interface, A's settings store | none |
| **F2: propose tools** | Plan 17 step 4, if wanted | M | V36 |
| **F3: GitHub bridge** | Plan 18 step 2, the `auth/` module, built and tested against a stub GitHub. Turned on at the first deploy | Foundation | its own schema, ADR 20 (already reserved) |
| **R: README** | Screenshots, the architecture diagram (Modulith's generated diagrams help), badges | D | none |

### 3. Finish

- Error Prone's default checks, sorted out.
- `-Werror` on.
- Any package not yet `@NullMarked`.
- The final status of plans 14 to 20.
- Then the first deploy, using the runbook:
  - turn on Google and GitHub sign-in;
  - rehearse the demo with a scheduled boost;
  - run Playwright and k6;
  - tune the limits and the SLO alerts from real use.

### Rules for working in parallel

- **Generated files.**
  - Only lanes C and A change the contract.
  - When the generated client or `routeTree.gen.ts` conflicts, regenerate it. Never merge
    generated code by hand.
- **Numbers are reserved,** as in the tables:
  - migrations V29 to V36;
  - ADRs 21 to 24 here, with 17, 18 and 20 already claimed.

  Nothing is deployed, so merging out of number order is harmless. A local database may need a
  reset.
- **Shared files.**
  - Dependencies go into the version catalog.
  - Each lane adds its own block to `application.yaml`.
  - Each lane marks only its own packages `@NullMarked`.
- **Status.** Each lane reports in its PR description, and plan 20's status is updated after
  each merge, so lanes don't conflict over this file.
- **Done means:**
  - `bash scripts/check.sh` is green;
  - `bash scripts/integration.sh` is green, with the per-class counts;
  - `bash scripts/web-linux.sh` is green if web or mobile changed;
  - nothing in the code matches the CodeQL patterns this repo has hit before.
- **Pull requests.** There's no `gh` CLI here, so lanes push their branches and the owner opens
  the PRs.

### Capacity, and the speed-up

- **Integration runs** are the tightest limit. Each one starts Postgres, Azurite and Kafka plus
  two JVMs inside WSL, so two or three at once is realistic. Lanes use `bash scripts/api.sh`
  (compile and unit tests, on Windows) for their inner loop, and run the integration tests
  before handing in.
- **Reviews** are the other limit. Every lane is a PR with CodeQL comments. Keeping three or four
  PRs open at a time, and merging in the stated order, keeps rebases small.
- **The critical path:**
  1. the foundation;
  2. P (the base, plus kiln);
  3. lane 2d (studio and access, payables, ledger);
  4. the finish;
  5. the deploy.

  Wave 1's other lanes, the demo studio and the feature lanes all fit inside it.
- **The speed-up.** Ten module conversions in a row become the longest of four lanes (three or
  four modules). Seven steps in sequence become four phases. The elapsed time should roughly
  halve, as long as reviews keep up.
