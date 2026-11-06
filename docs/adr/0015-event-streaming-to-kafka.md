# 15. Streaming studio events to Kafka through the outbox, off by default

- **Status:** accepted
- **Date:** 2026-11-05

## Context

Webhooks (ADR 14) push a few events to a studio's own tools. An event stream serves a different
reader: other services of ours, such as analytics, reading every studio's events in bulk. No
studio has asked for one. It's built to show the pattern
([11](../planning/11-kafka-stream-plan.md)), so it mustn't cost anything while no one uses it.
The API scales to zero, and a Kafka endpoint on Azure Event Hubs needs the paid Standard tier.

It has the same delivery problem as email and webhooks:
- An event mustn't be sent for work that rolled back.
- An event mustn't be lost when the broker is down or the instance dies.

## Decision

- **Send through the existing outbox, never from inside the transaction.**
  - `StreamPublisher` listens to the domain events (`FiringCompleted`, `PieceReceived`,
    `SupplySold`, `MemberJoined`) inside the business transaction.
  - It publishes a stream event there too, so Spring Modulith's publication registry
    (`event_publication`, ADR 11) stores it in the same commit.
  - After commit, Modulith's Kafka externalizer (`spring-modulith-events-kafka`) sends it.
  - If the send fails, the publication stays failed and `OutboxRetry` resends it. The business
    code doesn't know Kafka exists.
- **A contract separate from the domain events.**
  - The records in `stream.StudioStreamEvent` (`firing.completed`, `fee.charged`,
    `member.joined`) each carry `id`, `type`, `version`, `studioId` and `occurredAt`.
  - Domain records can change freely; consumers see a change only when the version changes.
- **Ids only.** A topic is kept for days and read by any service with access, so the stream
  carries no names or email addresses. A consumer that needs one asks the API. Webhooks do carry
  member details, because each studio chooses where its own webhooks go.
- **One topic, keyed by studio.**
  - The topic is `studiomanager.studio-events`, with 3 partitions.
  - An `event-type` header lets a consumer skip types without parsing them.
  - The `__TypeId__` header that the externalizer adds, which holds a Java class name, is
    removed by a producer interceptor.
  - The value serializer is `ByteArraySerializer`, because the externalizer hands over JSON that
    is already serialized.
  - The producer uses `acks=all` and idempotence.
- **At least once, unordered, deduped by id.**
  - The id is derived from what happened (such as `fee.charged:piece:<id>`), so a resend has the
    same id.
  - Each event is sent asynchronously after its own commit, so two events from one studio can
    arrive reversed. The first test run showed exactly that. Consumers order by `occurredAt`.
- **The first consumer is an activity projection.**
  - `ActivityConsumer` writes weekly totals in its own consumer group.
  - It binds the studio from the message key, acting as no account, so it writes under
    row-level security like any request (ADR 3). The key must match the `studioId` in the body,
    or the record is refused.
  - It records each event id in the same transaction as the totals, so a duplicate or a full
    replay counts once. It records only events it counts. A type it doesn't read yet is left
    alone, so a later version can count it on replay.
  - Kiln totals are keyed by the kiln's id, not its name, so renaming a kiln doesn't split them.
  - **Failures.** A failing event is retried 6 times over about a minute, which rides out a
    database restart. Then it goes to `studiomanager.studio-events.dlt`. Bad data goes there
    straight away: unreadable JSON, a key that doesn't match the body, or a malformed id.
  - **The seen-id table stays bounded.** A daily job forgets ids older than 30 days. The topic
    keeps events for 7 days, so a replay never meets an event whose id has been forgotten.
  - The "system" account it acts as is a stand-in for now. A real system principal is planned in
    [12](../planning/12-system-principal-plan.md).
- **Off by default.** `studiomanager.stream.enabled` adds the publisher, the routing, the topic
  and the consumer, and turns on Modulith externalization. With the flag off nothing is sent, no
  rows are queued, and the activity endpoint says `streaming: false`. Locally, a compose profile
  runs one KRaft broker. Tests run against Testcontainers Kafka.

## Consequences

- No broker means no cost and no new runtime dependency. With the flag on, a broker outage only
  grows `event_publication` until the broker is back. It doesn't fail any user request. The one
  cost is at startup: creating the topics gives up after 5 seconds if the broker can't be
  reached. It logs the failure and doesn't stop startup, but a cold start can take up to 5
  seconds longer. At deploy the topics belong in Terraform, so the startup check can be turned
  off.
- **Per-studio order isn't promised,** although events share a partition. Getting order would
  need a single sender per studio, reading the registry in sequence, which costs more than the
  activity totals need.
- **The retry limit applies.** `OutboxRetry`'s cap (8 attempts) applies here too. An event that
  keeps failing stays `FAILED` in `event_publication` for someone to look at.
- **Dead letters wait for a person.** Nothing re-reads the dead-letter topic yet. Someone can
  inspect a parked event there and re-publish it to the main topic.
- **What a replay rebuilds.** To rebuild a studio's totals, delete its `weekly_activity` and
  `stream_event_seen` rows together, then replay. Only what the topic still holds (7 days) comes
  back, so the totals table is the record, not the topic.
- **Deploying it, if ever:** Event Hubs Standard, connected with the API's managed identity over
  OAUTHBEARER, not a connection string.

## In an interview

"Publishing to Kafka inside the transaction risks sending events for work that rolled back. Sending
after commit with nothing recorded loses events when the broker is down. So the stream event is
written to the same Postgres outbox as the business change, and Spring Modulith's externalizer
ships it after commit, retrying from the table. The topic carries a versioned contract with ids
only, not the internal events or anyone's email address. Delivery is at least once and not
ordered, so the consumer dedupes on a deterministic event id, in the same transaction as its
writes. The tests caught the ordering issue, so I documented it rather than promising order. The
whole thing is behind a flag, because the API scales to zero and a managed broker costs money."
