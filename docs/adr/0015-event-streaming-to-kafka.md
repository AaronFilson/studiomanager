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
    row-level security like any request (ADR 3).
  - It records each event id in the same transaction as the totals, so a duplicate or a full
    replay counts once.
- **Off by default.** `studiomanager.stream.enabled` adds the publisher, the routing, the topic
  and the consumer, and turns on Modulith externalization. With the flag off nothing is sent, no
  rows are queued, and the activity endpoint says `streaming: false`. Locally, a compose profile
  runs one KRaft broker. Tests run against Testcontainers Kafka.

## Consequences

- No broker means no cost and no new runtime dependency. Turning the flag on adds no new failure
  to user requests. A broker outage only grows `event_publication` until the broker is back.
- **Per-studio order isn't promised,** although events share a partition. Getting order would
  need a single sender per studio, reading the registry in sequence, which costs more than the
  activity totals need.
- **The retry limit applies.** `OutboxRetry`'s cap (8 attempts) applies here too. An event that
  keeps failing stays `FAILED` in `event_publication` for someone to look at.
- **The consumer has no dead-letter topic.** Spring Kafka retries an event that keeps failing,
  then logs and skips it. A dead-letter topic comes when there's a consumer that can't afford to
  skip.
- **What a replay rebuilds.** A new consumer group rebuilds its totals from what the topic still
  holds. Anything older than the topic's retention is gone, so the totals table is the record,
  not the topic.
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
