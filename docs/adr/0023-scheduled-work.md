# 23. Scheduled work: one copy at a time, and a job that runs while the API sleeps

- **Status:** accepted
- **Date:** 2026-11-06

## Context

Nine `@Scheduled` methods do the background work: the webhook retries and their history purge,
the outbox retry and purge, the maintenance reminders, unclaiming pieces, the two photo-upload
cleanups, and forgetting old stream event ids. Two things were wrong with them
([19](../planning/19-java-practices-review.md), finding 11; [20](../planning/20-pre-production-cleanup-plan.md)):

- **They only ran while a replica was up.** The API scales to zero, so a quiet studio could go a
  day without a webhook retry or a maintenance reminder.
- **Two replicas ran them twice.** `max_replicas` is 2 and nothing coordinated them. Webhook
  claims are conditional updates, so those were safe; the rest relied on each sweep being
  idempotent, which nobody had checked sweep by sweep.

## Decision

- **Each sweep implements `Sweep`** (a name and one pass) and always runs through `SweepRunner`.
  Classes that did two jobs were split, so each sweep is one bean: `WebhookHistoryPurge` and
  `OutboxPurge` came out of `WebhookSweep` and `OutboxRetry`.
- **One copy at a time, by an advisory lock.** `SweepRunner` opens a transaction on a connection
  of its own, calls `pg_try_advisory_xact_lock(hashtext('studiomanager.sweep.' || name))`, and
  holds it until the sweep returns. If another replica, or the job, holds it, this run is skipped.
  - The sweep's own work uses other connections and transactions, so a sweep that commits studio
    by studio still does, and the tenant binding (ADR 3, ADR 16) is unchanged.
  - Ending the transaction frees the lock, and so does a crash: Postgres ends the session. There
    is no lock table to clean up, which is why this beats ShedLock here: Postgres is already the
    one shared thing.
  - "Try" rather than wait: a skipped run is harmless, because the next one is minutes away.
- **A Container Apps job runs every sweep once, every 6 hours.** It's the same image and the same
  managed identity as the API, with the `sweep` profile:
  - no web server, so the schedule is off (`SchedulingConfiguration` is web-only);
  - `SweepJob` runs each `Sweep` bean once on `ApplicationReadyEvent`, and the application exits
    like the migration job does, with exit code 1 if any sweep failed;
  - async work a sweep starts (a resubmitted email) gets 60 seconds to finish before exit.

  A run takes about a minute, which is inside the Container Apps free grant.
- **Each run is observed.** `SweepRunner` wraps it in a Micrometer `Observation` named
  `studiomanager.sweep`, tagged with the sweep and its outcome (`ran`, `skipped`, `failed`): a
  timer, and a span where tracing is on. Failures log with the stack trace and a `sweep` field.

## Consequences

- **Work happens while the API sleeps,** at worst 6 hours late, without paying for a replica that
  stays up. A replica that is up still runs the sweeps on their own schedules.
- **No sweep runs twice at once,** across replicas and the job, without each one having to prove
  it's idempotent. They still should be, since a sweep can run again right after another finishes.
- **A lock costs a connection** for the length of the sweep. With nine sweeps, staggered, that's
  well inside the pool.
- **A new sweep is one class:** implement `Sweep`, call `SweepRunner.run(this)` from its
  `@Scheduled` method, and the job picks it up.
- **Not done:**
  - Re-encrypting secrets under the newest key (the keyring in `SecretCipher`) as a sweep. Add it
    when a key is first rotated.
  - SLO alerts on the sweep timer. They get tuned after the first deploy.

## In an interview

"How do you run cron in a scaled-out service that scales to zero? Two parts. Each sweep takes a
Postgres advisory lock, transaction-scoped on its own connection, so two replicas never run the
same sweep at once and a crash frees it for free. And because no replica may be up at all, a
Container Apps job on a 6-hour cron starts the same image with a profile that runs every sweep once
and exits. The lock means the job and a live replica can't collide either."
