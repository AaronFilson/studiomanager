# 19. Review of the Java against current practice, for the resume

_The owner's decisions on this review are in
[20](20-pre-production-cleanup-plan.md), which is the plan of record. It drops the AI-call
limits and keyset pagination, and adds Hibernate, the MCP SDK and the cost guards._

_Written 2026-11-06, after PR #19. The question: is the Java API up to date with current Java and
Spring practice, and what would make it a stronger resume piece? It builds on
[15](15-resume-review.md), which covers deploying, screenshots, numbers, Playwright, coverage,
mutation tests and the AI feature. None of that is repeated here, and 15's first point still
comes first: deploy it with a demo studio._

## Verdict

The Java is already modern. It uses the Java 25 and Spring Boot 4 idioms a reviewer looks for,
and uses them for real reasons, not decoration. The gaps are in **enforcement and operations,
not language features**:
- null annotations that nothing checks;
- time that tests can't control;
- lists that silently stop at 500;
- no metrics of its own;
- secret encryption that can't rotate its key;
- no rate limits on the new token and AI surfaces.

Each fix is small to medium, and each one makes a better interview story than another feature.

## Already current: say these in interviews

| Practice | Where |
|---|---|
| `ScopedValue` instead of `ThreadLocal` for the tenant, safe on virtual threads | `platform/tenancy/TenantContext.java` |
| Sealed interfaces with exhaustive pattern-matching `switch` | `firing/FeeRule.java`, `FeeCalculator.java`, `stream/StudioStreamEvent.java` |
| Records for values and views, with invariants in compact constructors | `platform/Money.java`, `TenantContext`, `FeeRule` |
| Virtual threads on; `@Async` work takes the tenant explicitly instead of inheriting it | `application.yaml`, `webhook/WebhookSender.java` |
| JVM AOT cache trained in the image build (JEP 483/514/515), same flags for training and runs | `api/Dockerfile`, ADR 8 |
| `JdbcClient` with text-block SQL; conditional-update claims for at-least-once delivery | `piece/PieceService.java`, `WebhookSender.claim` |
| Optimistic locking with version columns, mapped to 409 | `PieceService.move`, `platform/web/ApiExceptionHandler.java` |
| RFC 9457 problem details, including from the security filters | `ApiExceptionHandler`, `ProblemJsonSecurityHandler` |
| Jackson 3, JSpecify annotations, Spring Boot 4 generator options | `build.gradle.kts` |
| Constructor injection, package-private classes, `proxyBeanMethods = false`, `@ConfigurationProperties` records | throughout |
| Spring Modulith: boundary test, event publication registry as the outbox, Kafka externalization | `ModularityTests`, ADRs 11 and 15 |
| `MockMvcTester` (AssertJ), Testcontainers 2 with real Postgres 18, Azurite and Kafka | `IntegrationTest`, `TestcontainersConfiguration` |
| A test that fails the build if any `studio_id` table lacks RLS or a policy | `TenantIsolationTests.everyStudioOwnedTableHasRowLevelSecurityAndAPolicy` |
| SBOM and provenance on the image, SHA-pinned third-party actions, `permissions: {}` by default | `.github/workflows/` |

## Findings, ranked by payoff for the effort

### 1. The null annotations aren't enforced

`@Nullable` (JSpecify) is used throughout, and the generator emits it too (`useJspecify`). But
no package is `@NullMarked`, so the default is "unspecified", and no checker runs. The
annotations document intent, but nothing fails when code breaks it. Spring Framework 7 made the
same move to JSpecify and enforces it with NullAway, so this is exactly what "current" means for
Spring code.

**Fix:**
- Add `@NullMarked` to each module's `package-info.java`.
- Add Error Prone with NullAway (`net.ltgt.errorprone`) using
  `-XepOpt:NullAway:JSpecifyMode=true` and `-XepOpt:NullAway:OnlyNullMarked=true`, so modules
  can be marked one at a time.
- Make NullAway an error, and exclude `build/generated`.
- Expect work in the three JPA entities, whose no-arg constructors leave fields unset; suppress
  `NullAway.Init` there.

**Interview story:** "Null safety is checked at compile time, with the same tooling Spring itself
uses."
**Effort:** small to medium, one sweep.

### 2. Time comes from the wall clock in 39 files

There are 57 `Instant.now()` and `LocalDate.now()` calls. 22 classes each have their own private
`now()` that truncates to microseconds, and about 22 more helpers convert between `Instant` and
`OffsetDateTime` (`utc`, `instant`, `instantOrNull`). The sweeps work around it with
`runAt(Instant)`, and tests jump ahead with `Instant.now().plusSeconds(60L * 60 * 24 * 40)`
(`PieceApiTests`).

**Fix:**
- Inject one `InstantSource` (or `Clock`) bean.
- Move truncation and conversion into one `platform` helper.
- Give tests a settable clock.

Hold periods, backoff, reminders, token expiry and shift cover then become deterministic tests.
**Effort:** medium, but mechanical.

### 3. Lists silently stop at 500 or 1,000

19 hard-coded `limit N` clauses across 12 services feed endpoints that return plain arrays. For
example, `PieceService.list` orders newest first with `limit 500`. Once a studio passes 500
pieces, the oldest ones drop out of the list, and nothing tells the client.

**Fix:** keyset (cursor) pagination.
- Ids are UUIDv7, so they're time-ordered: `where id < :cursor order by id desc limit :size + 1`.
- Define an opaque `cursor` parameter and a `next` field once, as reusable components in the
  OpenAPI contract. The TypeScript client then gets them for free.

**Interview story:** keyset versus offset pagination is a standard question, and UUIDv7 makes the
answer concrete.
**Effort:** medium (contract, client, web).

### 4. No metrics of its own, and some logs drop the stack trace

The Azure starter instruments HTTP and JDBC, but the application records nothing itself: no
`MeterRegistry` and no `Observation`. In production you couldn't answer:
- how far behind is the outbox?
- how many webhook deliveries are failing?
- what is the Kafka consumer lag?
- how long does a sweep take?

Four warnings log `e.getMessage()` or `e.toString()` instead of the exception, so the stack trace
is lost:
- `WebhookSender.java:67`
- `MaintenanceReminderSweep.java:62`
- `PhotoUploadSweep.java:70`
- `RecordPhotoUploadSweep.java:67`

**Fix:**
- Wrap sweeps and webhook sends in a Micrometer `Observation`, which gives a span and a timer
  together.
- Add gauges for incomplete `event_publication` rows and pending deliveries.
- Pass the exception as the logger's last argument.
- Use SLF4J 2's `log.atWarn().addKeyValue("studioId", id)`, so the ECS logs that
  `application-azure.yaml` already turns on carry real fields.
- Then define two or three SLOs, with alerts in Terraform.

**Interview story:** "I can tell you the outbox lag right now," which is an SRE answer, not just
a developer's.
**Effort:** small to medium. Best done right after the first deploy.

### 5. Secret encryption can't rotate its key, and the cipher is written twice

`webhook/WebhookSecrets.java` and `ai/AiKeyCipher.java` are the same AES-GCM code. The stored
format is nonce plus ciphertext, with no key id, and config holds one key. Rotating either key
would make every stored secret unreadable. `WebhookSecrets` also still has
`nonce.length + sealed.length`, a pattern CodeQL flags, and spells out `java.nio.charset.StandardCharsets`
even though it imports it.

**Fix:**
- Write one `platform/crypto` class.
- Prefix the ciphertext with a key-version byte.
- Hold a keyring: the current key plus previous ones, from Key Vault.
- Re-encrypt under the current key when a secret is read, or in a job. Google Tink keysets do
  the same thing, if you'd rather use a library.

**Interview story:** envelope encryption with key rotation.
**Effort:** small.

### 6. No rate limits on tokens, MCP or AI calls

Personal API tokens and the MCP endpoint open the API to scripts and agents. Testing an AI key,
and soon reading invoices, spends the studio's own Anthropic money. Nothing limits any of it.

**Fix:**
- Per-token and per-account buckets with Bucket4j (in memory is enough at two replicas, or use
  its Postgres backend), returning 429 with `Retry-After`.
- Spring Framework 7's `@ConcurrencyLimit` on outbound AI calls.
- Its `@Retryable` for email and blob calls.

**Interview story:** protecting a public API, and a customer's bill.
**Effort:** small.

### 7. Errors carry prose only

`ApiExceptionHandler` sets a status and a detail message, with no `type` and no code. Web and
mobile can't tell a stale-version 409 from a duplicate-name 409 except by matching text, so the
wording can never change safely.

**Fix:**
- Give each problem a `type` URI (for example `/problems/stale-version`) and a `code` property.
- Let each exception carry its type.
- Describe the problem schema in the OpenAPI contract.

**Effort:** small.

### 8. A sealed `switch` was traded away for CodeQL

`TokenPolicy.refusal` walks the sealed `Access` interface with `instanceof` checks. A fourth kind
of access would compile and fall through to "Tokens can only read this." It was written that way
to avoid CodeQL's "unread local variable" alert on `_`. Record patterns that bind only what they
use avoid the alert and keep the compiler's exhaustiveness check:

```java
boolean reading = method.equals("GET") || method.equals("HEAD");
return switch (access) {
    case Never(String why) -> Optional.of(why);
    case ReadOnly() -> reading ? needsRead(scopes) : Optional.of("Tokens can only read this.");
    case Writable(TokenScope scope) -> reading ? needsRead(scopes)
            : scopes.contains(scope) ? Optional.empty()
            : Optional.of("This token needs the %s scope.".formatted(scope.wireName()));
};
```

`case FeeRule.Flat _` in `FeeService` is correct Java 22+. If CodeQL flags it, dismiss the alert
as a false positive, or filter that query in a `.github/codeql/codeql-config.yml`. Don't rewrite
code into an older idiom to please a scanner.

**Effort:** tiny.

### 9. The build doesn't enforce quality

The build has:
- no compiler lint flags;
- no formatter;
- no version catalog;
- no Gradle configuration cache or build cache;
- no `distributionSha256Sum` in the wrapper properties.

**Fix:**
- Turn on `-Xlint:all`, see what the generated code trips, then make it `-Werror`.
- Add Spotless with a formatter close to the current style (Palantir Java Format is 4-space,
  120 columns).
- Move versions into `gradle/libs.versions.toml`.
- Create `api/gradle.properties` with `org.gradle.configuration-cache=true` and
  `org.gradle.caching=true`, and check that the OpenAPI generator plugin supports the
  configuration cache.
- Pin the wrapper's checksum.

JaCoCo is already gap 5 in 15.
**Effort:** small.

### 10. Tests don't use Modulith's own test support

Every integration test starts the whole application. Modulith's `@ApplicationModuleTest` starts
one module, and its `Scenario` API tests event flows ("when a firing completes, a notice is
published") without Awaitility timing. The property tests are hand-rolled with a seeded
`java.util.Random` (`FeeCalculatorTests:123`, `BatchCalculatorTests:69`). jqwik adds shrinking:
a failure comes back as the smallest input that still fails.

**Effort:** small per test. Start with the kiln, piece and notify event flow.

### 11. Undocumented decisions an interviewer will ask about

- **Data access.** Three JPA entities (`Account`, `Studio`, `StudioMembership`) use Hibernate.
  Every other module uses `JdbcClient`, under a JPA transaction manager that binds the tenant. No
  ADR says why. Either "SQL-first, JPA only for X" or "drop Hibernate" is defensible: measure
  startup time and the AOT cache size before deciding. Leaving it undocumented is the only weak
  choice.
- **The hand-written MCP server.** The reason (the SDK's transports run tools on Reactor threads,
  while the tenant is bound to the request thread) is only in `McpController`'s Javadoc. An ADR
  makes it easy to find, and it's one of the best stories in the repo.
- **Scheduled jobs on two replicas.** `max_replicas` is 2, and there are nine `@Scheduled`
  methods with no lock. Webhook claims are conditional updates, so they're safe. The other sweeps
  depend on each being idempotent, which this review didn't check one by one. Either wrap them in
  `pg_try_advisory_xact_lock` (or ShedLock) with a test that two concurrent runs don't
  double-send, or record in an ADR why each is safe. "How do you run cron in a scaled-out
  service?" is a common question.
- **Ambient tenant.** Services call `TenantContext.require()` rather than taking the tenant as a
  parameter. Have a ready answer for why: RLS needs the tenant bound per transaction, and a
  `ScopedValue` can't be forgotten or leak.

**Effort:** small, writing only.

### 12. Large service classes

`PayablesService` is 774 lines, `MaintenanceService` 745, `LedgerService` 645 and
`ScheduleService` 564, mostly inline SQL. They work, but a reviewer who opens one sees a wall of
SQL.
- Split them by use case, for example `ScheduleService` into bookings, shifts and rosters, the
  way `FiringBookings` already is.
- `PieceService.withDetails` rebuilds a 19-component record by hand; a `withNamesAndPhotos(...)`
  method on `PieceView` keeps that in one place.

**Effort:** medium. Do it when you next touch each class.

## Tech worth adding for the resume (beyond 15)

- **AsyncAPI for the Kafka stream.** It's contract-first for events, matching OpenAPI-first for
  HTTP. `StudioStreamEvent` already has an id, type, version and time, so a CloudEvents envelope
  is a short step.
- **Vendor-neutral telemetry.** Spring Boot 4's own OpenTelemetry starter exports OTLP, with
  Application Insights as one backend. That would remove the BOM pinning that ADR 13 and
  `build.gradle.kts` work around. First check that Application Insights accepts OTLP in the way
  you'd need.
- **Image scanning and a Scorecard badge.** Trivy or Grype in the deploy job, and the OpenSSF
  Scorecard. Both are small and visible.
- **Skip:**
  - GraalVM native image: the AOT cache is the better story, and ADR 8 already tells it.
  - WebFlux: virtual threads made it unnecessary, and saying so is a good answer in itself.
  - Microservices, Lombok, or a Kotlin rewrite.

## Resume bullets

15's bullets are still true. Add this one, true today:
- Exposed studio data to AI assistants through a hand-written, stateless **MCP server**, reached
  with **scoped personal API tokens** whose live role is the ceiling, and still isolated down to
  PostgreSQL RLS.

These become true after the work above:
- Enforced **compile-time null safety** (JSpecify and NullAway) across a Spring Boot 4 / Java 25
  codebase.
- **Keyset pagination** on UUIDv7 keys across the API contract and its generated TypeScript
  client.
- **Envelope encryption with key rotation** for third-party secrets.
- Instrumented outbox lag, webhook delivery and Kafka consumer lag with **Micrometer and
  OpenTelemetry**, with SLO alerts in Terraform.

## Suggested order

1. Deploy with a demo studio (15, gap 1). It still beats everything below.
2. NullAway, `@NullMarked` and the compiler flags (1, 9): one PR, the strongest "modern Java"
   signal.
3. The clock bean and time helpers (2): one mechanical PR.
4. Problem types, the `TokenPolicy` switch and the logging fixes (4, 7, 8): one small PR.
5. Keyset pagination (3): a contract change, in its own PR.
6. Key rotation with the merged cipher, and rate limits (5, 6): one security PR.
7. Metrics and SLO alerts (4), once it's deployed.
8. ADRs for data access, MCP and scheduled jobs (11).
