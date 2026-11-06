# 21. Hibernate with `@TenantId` for the write side; SQL for the listed cases

- **Status:** accepted
- **Date:** 2026-11-06

## Context

Before this, three entities (`Account`, `Studio`, `StudioMembership`) used Hibernate, and every
other module used `JdbcClient`: 229 statements in 34 classes, 51 hand-written inserts each passing
`studio_id`, 19 hand-written `version = version + 1 where version = :version` updates, and a row
mapper per table. Most of those writes are CRUD on one aggregate with a version, which is the job
an ORM does. Plan [20](../planning/20-pre-production-cleanup-plan.md), section 1, has the counts
and the sorting; [19](../planning/19-java-practices-review.md), finding 11, asked for the decision
to be written down.

Kiln was converted first, to set the pattern. This ADR is the template the other modules follow.

## Decision

**Hibernate (through Spring Data JPA) for writes and for reads of one module's own aggregates.
Plain SQL only for the kinds below, each with its reason.**

| Kind | Examples | Why SQL |
|---|---|---|
| Reports and totals | ledger balances, statements, the 1099 list, hours totals, Today counts, weekly activity, the export | Set-based sums and joins in one round trip, with no entity to load |
| Claims and queues | webhook delivery claim (`update … returning`), reminder claim | An atomic test-and-set in one statement, safe across replicas |
| Counters and upserts | `piece_counter`, piece policy, AI key, activity projection (`on conflict …`) | Race-free in one statement |
| Set-based moves | `PieceService.advanceAll` after a firing | One statement for N rows, all or nothing |
| Cross-tenant functions | the `app.*()` security-definer calls | By design (ADR 10) |
| Postgres-only features | the kiln queue's `lateral` join, the first photo per piece (`distinct on`), the RLS guard's catalog query | No JPQL equivalent worth the trade |
| The tenant binding | `set_config(...)` | It's the mechanism itself |

**The tenant, in two layers.**
- Studio-owned entities mark `studio_id` with Hibernate's `@TenantId`. `StudioTenantResolver`
  reads `TenantContext`, so Hibernate sets `studio_id` on every insert and adds `studio_id = ?`
  to every query of those entities. (A load by id of another studio's row also comes back empty;
  `KilnPersistenceTests` checks it, though RLS alone would give the same result.)
- Row-level security (ADR 3) still sits underneath, unchanged. The application check and the
  database check are now both automatic.
- With no studio bound ("my studios", the sweeps that find studios), the resolver returns the nil
  UUID, which matches no row. `Account`, `Studio` and `StudioMembership` have no `@TenantId`, so
  they work there as before. `KilnPersistenceTests` proves both.
- Like RLS, Hibernate reads the tenant when the transaction's session opens, so **the studio is
  bound before the transaction starts**. That was already the rule for RLS.

**Shared settings** (`platform/tenancy/TenancyConfiguration`, `application.yaml`):
- Spring Data auditing: `@CreatedDate` reads the application's one `Clock` (a `DateTimeProvider`),
  and `@CreatedBy` is the bound account, a person's or a job's.
- `hibernate.default_batch_fetch_size: 32`, JDBC batching of 32, ordered inserts and updates.
- `ddl-auto: validate` (already set) checks every entity against Flyway's schema at startup.

## Rules for each module conversion (wave 2 copies these)

Kiln (`api/src/main/java/.../kiln`) is the worked example of every rule.

1. **Entities.**
   - Package-private, in the module's own package, with `@Table(name = …)` and
     `@EntityListeners(AuditingEntityListener.class)` when it has audit columns.
   - Id: `UUID`, assigned in the constructor with `Uuids.v7()`.
   - `studio_id`: `@TenantId private @Nullable UUID studioId;`. Never set it by hand.
   - `@Version private @Nullable Long version;` (a wrapper, so Spring Data sees a new entity and
     persists instead of merging), read through `long version()`, which
     `requireNonNull`s.
   - `created_at` / `created_by`: `@CreatedDate` / `@CreatedBy`, `@Nullable` until saved.
   - Required columns are non-null fields set in the constructor; nullable columns are
     `@Nullable` fields. The Java says what the migration says.
   - Enums as `@Enumerated(EnumType.STRING)`.
   - The JPA no-arg constructor is `protected` and carries `@SuppressWarnings("NullAway.Init")`.
     That is the only NullAway suppression allowed.
   - Behaviour lives on the entity (`Firing.start`, `complete`, `cancel`), with the same
     messages the service used to throw. No public setters.
   - Records map as embeddables, including composite ids (`FiringPiece.Key`). Name every column
     of a composite id that a `@JoinColumn` also names, or Hibernate refuses the duplicate.
2. **Entities never map another module's table.** A link across modules stays a `UUID` field
   (`FiringPiece.Key.pieceId`). Associations (`@ManyToOne`, `@OneToMany`) only within a module.
   That keeps Modulith's boundaries and lets modules convert in parallel.
3. **Repositories.** Package-private Spring Data interfaces. JPQL in `@Query` for anything past a
   derived query. DTO projections use a top-level record (`FiringPieceCount`).
4. **SQL that stays** goes in one package-private `…Queries` class per module, each statement with
   a comment naming its row in the table above. Kiln needs none: its only Postgres-only query is
   the queue's `lateral` join, which lives in the piece module.
5. **Flush before SQL in a write transaction.** `JdbcClient` can't see changes Hibernate hasn't
   flushed. Call `repository.flush()` (or `saveAndFlush`) before:
   - running SQL, your own or another module's (`PieceService.advanceAll`);
   - publishing an event, because synchronous `@EventListener`s run SQL in the same
     transaction (maintenance inserts a kiln's asset on `KilnCreated`, with a foreign key to the
     kiln).
   Plain `JdbcClient` reads are for read-only transactions, and for claims and sweeps that load
   no entities.
6. **Constraint errors are flushed inside the `try`.** A unique index only fails at flush, which
   is otherwise at commit, outside the service. Use `saveAndFlush` inside the `try`, catch
   `DataIntegrityViolationException`, and map it only if `UniqueViolations.isUniqueViolation(e)`;
   rethrow anything else.
7. **Check the client's version explicitly.** Load, compare `entity.version()` with the
   `expectedVersion` the client sent, and throw the module's `ConflictException` message if they
   differ. `@Version` then catches a race between the load and the commit, which
   `ApiExceptionHandler` already maps to 409. Flush before building the response, so it carries
   the new version.
8. **No N+1.**
   - Fetch-join the associations a list shows (`join fetch f.kiln`).
   - Count or aggregate for a whole list in one query, not per row.
   - Ask another module for many rows at once (`PieceService.listByIds`), never one call per id.
   - Pin each list with a test: `StatementCounts.of(…)` (Hibernate `Statistics`) must give the
     same count for one row and for several. It counts only Hibernate's statements.
9. **Time** comes from the injected `Clock` (`clock.instant()`), never `Instant.now()`.
10. **`@NullMarked`** on the module's `package-info.java`, in the same PR. Fix what NullAway
    finds in code: an explicit null check, a local copy before a lambda, a non-null accessor.
11. **Split the large service by use case** as it's converted: reads (public, for other modules),
    and package-private services per use case (`FiringLoads`, `FiringTransitions`). Keep the
    public methods other modules call where they are, with the same signatures.
12. **Modulith tests for the module's events**: an `@ApplicationModuleTest(mode =
    ALL_DEPENDENCIES)` with `Scenario` (`KilnModuleTests`). `Scenario` runs the stimulus in its own
    transaction, so bind the tenant around the whole scenario with `TenantContext.callAs`. Add
    `extraIncludes = {"account", "apitoken"}`: they implement platform interfaces the security
    filters need.
13. **The HTTP contract doesn't change.** The API integration tests are the regression net; they
    pass unchanged.

## Consequences

- **The tenant check is automatic in both layers.** A new query can't forget `studio_id`, and a
  new insert can't set the wrong one.
- **Less code.** Kiln lost its row mappers, its hand-written inserts and its version-bump updates.
  It has no SQL left.
- **Behaviour changes, all deliberate:**
  - An update that changes nothing no longer bumps the version (Hibernate only writes dirty
    entities).
  - A firing's pieces come back in piece-number order, as lists show them: each piece's first
    photo and no timeline. Before, each was a full `PieceService.get`, one call per piece.
- **The flush rule is the new thing to review for.** A missing flush shows up as a foreign-key
  error, or a stale read, in the SQL that follows.
- **Startup** (`StartupTimeTests`, `bash scripts/integration.sh test --tests '*StartupTimeTests'
  -PstartupRuns=12`, WSL, Postgres in Testcontainers, the same JVM restarting the context; the
  median of 11 warm starts, plus the first, cold start, which also migrates):

  | | Before (main) | After (kiln on Hibernate) |
  |---|---|---|
  | Warm restart, median of 11 | 1,561 ms and 2,695 ms (two runs) | 2,533 ms |
  | First start, cold, with migrations | 24,012 ms | 26,071 ms |

  The machine was shared with other lanes' builds, so runs of the same code differ by over a
  second. Within that noise, three more entities made no measurable difference.

  The AOT cache in the image (ADR 8) is trained with Hibernate's metamodel, so the deployed
  difference should be smaller still. Measure it again on the first deploy.

## In an interview

"Writes go through Hibernate, and tenant isolation is automatic twice over: Hibernate's
`@TenantId` adds the studio to every insert and query from a resolver that reads the request's
scoped tenant, and Postgres row-level security still checks every row underneath. SQL stays where
it earns its place, such as atomic claims, set-based moves, report sums and Postgres-only joins,
each in one class per module with its reason. The two worlds meet at one rule: flush before SQL.
And every list has a test that pins its query count, so an N+1 fails the build."
