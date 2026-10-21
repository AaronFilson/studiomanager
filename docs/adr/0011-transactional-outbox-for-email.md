# 11. A transactional outbox for email, sent through Azure Communication Services

- **Status:** accepted
- **Date:** 2026-10-20

## Context

Members don't open an app to check whether their pieces are out, so the batch "ready" notice
and staff notices have to arrive by email. An email is a side effect outside the database:

- Sent inside the request's transaction, an email goes out even when the transaction rolls back,
  and a slow provider holds up the response.
- Sent after commit with nothing recorded, an email is lost if the provider is down or the
  instance stops before sending.

## Decision

- **The outbox is Spring Modulith's event publication registry, kept in Postgres** (V10,
  `event_publication`). `NotificationService` publishes `NotificationCreated` for each inbox row
  it writes. The registry stores the publication in the same transaction. After commit,
  `NotificationEmails`, an `@ApplicationModuleListener`, runs asynchronously in its own
  transaction and sends the email. When it returns, the publication is marked completed.
- **Failures are retried on a schedule.**
  - A send that throws leaves the publication `FAILED`.
  - Every 5 minutes, `OutboxRetry` resubmits failures at least 5 minutes old, up to 8 attempts.
    Past that, a publication stays `FAILED` for someone to look at.
  - A publication stuck for 15 minutes is marked failed by Modulith's staleness monitor, so it
    gets retried too. That covers an instance that died mid-send.
  - Completed publications are kept for 30 days, so "did that email go out?" has an answer.
- **The event carries everything the email needs:** studio name, title and body. The listener
  runs with no tenant bound, where row-level security hides the notification table. The only
  other thing it reads is the member's address, from `account`, which isn't tenant data.
  `event_publication` has no RLS of its own, because only the registry reads it.
- **Sending goes through an `EmailSender` interface in `platform.mail`.**
  - `log` writes emails to the log, for local work.
  - `acs` sends with Azure Communication Services. It authenticates as the API's managed identity
    through `DefaultAzureCredential`, so there's no connection string or key.
  - Integration tests swap in a recording sender.

## Consequences

- Nothing is emailed for work that rolled back, and a provider outage delays email instead of
  losing it. Integration tests cover both.
- **Delivery is at least once.** A send that reached ACS but timed out before confirming is
  retried, and can arrive twice. For notices that's acceptable; payment receipts would need an
  idempotency key.
- The API scales to zero, so retries happen only while an instance is up. The first request
  after idle wakes it.
- Later push notifications use the same path: a new event and a new listener.
- **Emailed invites (P1 item 2) use it too, but keep the outbox free of secrets.**
  - Event payloads sit in `event_publication` for 30 days, so `InviteEmailRequested` carries only
    the invite's id.
  - The listener makes the code when it sends. It stores the code's hash and emails the code in
    one transaction, so a failed send rolls the hash back.
  - It binds the studio before starting that transaction, because RLS reads the tenant when a
    transaction begins. So it's a plain `@Async @TransactionalEventListener` instead of an
    `@ApplicationModuleListener`, and the registry still tracks it.
- **Open at first deploy:**
  - The API identity needs a role on the Communication Services resource to send. Microsoft's
    built-in role list has no Communication Services entry, and reports conflict on custom roles.
    Find the role then, assign it in `infra/envs/dev`, and add it to the deploy identity's
    allow-list in `infra/bootstrap`.
  - The Azure-managed sender domain has low limits and an unfamiliar address. Before a real pilot,
    verify a custom domain.
- Members can't yet opt out of studio-notice emails. Add a preference if the pilot asks for it.

## In an interview

"The email can't go inside the transaction, and it can't just run after commit either. So I used
Spring Modulith's event publication registry as an outbox. The event is written in the same
transaction as the notice, delivered by an async listener after commit, and a scheduled job
retries failures with a cap. Delivery is at least once, and I say so. The integration tests
prove that a rollback leaves nothing queued and that a failed send is retried."
