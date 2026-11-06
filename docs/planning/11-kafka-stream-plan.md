# Wave 2: streaming studio events to Kafka

_Planned 2026-11-05, on `wave2-kafka`. It follows the sketch at the end of [10](10-webhooks-plan.md)._

## Why

No studio has asked for this. It's resume work: an event stream that other services can read.
The domain events that webhooks fan out (`FiringCompleted`, `PieceReceived`, `SupplySold`,
`MemberJoined`) can also go to Kafka through the same outbox (ADR 11), so the business code doesn't
change. It stays off unless a flag is set, so the API still scales to zero with no broker to pay
for.

## Design

- **A stream contract, separate from the domain events.** These events are published:
  `firing.completed`, `fee.charged` and `member.joined`.
  - Each is a record in `stream.StudioStreamEvent`, with `id`, `type`, `version`, `studioId` and
    `occurredAt`.
  - Changing a domain record doesn't change what consumers see unless the version changes.
- **Ids only, no names or email addresses.** A topic is kept for days and read by anything with
  access, so it doesn't carry member details. A consumer that needs a name asks the API. Webhooks
  are different: a studio chooses where they go, so their payloads do carry member details.
- **Mapped inside the transaction.** `StreamPublisher` maps each domain event and publishes the
  stream event in the same transaction, so the tenant is still bound for lookups such as the
  studio's currency.
  - Modulith's publication registry stores the stream event in the same commit.
  - After commit, the Kafka externalizer sends it.
  - If the work rolls back, nothing is sent. If the broker is down, the event waits, and
    `OutboxRetry` resends it.
- **Topic and key.**
  - One topic, `studiomanager.studio-events`, with 3 partitions.
  - The key is the studio id, so each studio's events share a partition.
  - An `event-type` header carries the type, so a consumer can skip types it doesn't read.
  - There are no Java type headers on the messages.
- **The producer** sends with `acks=all` and idempotence on.
- **Guarantees, stated plainly.**
  - Delivery is at least once.
  - `id` is derived from what happened (`type:source:id`), so a repeat has the same id and
    consumers dedupe on it.
  - **Order isn't guaranteed.** Each event is sent asynchronously after its own commit. The
    first test run caught this: a member joining and a sale straight after it arrived reversed.
    Consumers order by `occurredAt`.
- **Off by default.** `studiomanager.stream.enabled` (env `STUDIOMANAGER_STREAM_ENABLED`) adds the
  publisher, the routing and the topic, and turns on Modulith externalization.
  - Locally, run `docker compose --profile stream up -d` for a single KRaft broker
    (`apache/kafka-native`).
  - Tests use Testcontainers.

## Steps

1. **Producer** (done 2026-11-05): the contract, the mapper, the routing, the compose profile, and
   `StreamTests` against a real broker. The tests cover the events arriving keyed by studio, ids
   only, a rollback queuing nothing, and stable ids. All 205 API tests pass in WSL.
   - **Found while building.** Modulith's Kafka externalizer hands the producer JSON that is
     already serialized, as bytes, so the value serializer is `ByteArraySerializer`. A JSON
     serializer encoded those bytes again, as a base64 string.
   - The externalizer also adds a `__TypeId__` header holding the Java class name. A producer
     interceptor, `DropTypeIdHeader`, removes it, so consumers read `event-type` instead.
2. **A consumer worth showing** (done 2026-11-05, V24). These are the changes from the plan
   below:
   - `ActivityConsumer` is in group `studiomanager-activity`. It binds the studio from the message
     key, acting as no account (an all-zero id), so it can reach only that studio's rows under
     RLS. `ActivityProjection` records the event id in `stream_event_seen` in the same
     transaction as the `weekly_activity` upsert, so a duplicate or a replay counts once.
   - Weeks start on Monday in the studio's time zone. A type it doesn't know is marked as seen
     and not counted, so a newer producer doesn't break it.
   - `GET /studios/{id}/activity/weekly` (staff only) returns `streaming: false` when the flag is
     off. The web "Activity" page shows a table, not a chart. A chart can follow if anyone wants
     one.
   - The tests cover the full path from the API through Kafka back to the endpoint, a duplicate
     counting once, other studios staying apart, and members getting a 403. All 207 API tests
     pass in WSL, and the Linux web job passes.
   - **Review fixes, 2026-11-05.** A deep review before the PR found the following, and all of
     it is fixed:
     - The endpoint failed for staff who belong to more than one studio. RLS let them read every
       studio's time zone, and the code expected one row. The week is now worked out in SQL from
       the studio's own row.
     - Failures were retried without pauses, then skipped. They now back off over about a minute,
       then go to `.dlt`.
     - Types it didn't read were marked as seen, so they could never be counted later.
     - The kiln was keyed by name, so a rename split its totals. Events now carry `kilnId`.
     - The record key wasn't checked against the body.
     - The locale wasn't fixed when lowercasing the kind.
     - The seen-id table grew forever. It's now purged after 30 days, and topic retention is set
       to 7 days.
     - A startup could stall for 30 seconds without a broker. It now waits 5 seconds at most.
     - The endpoint returned stale totals while streaming was off.
     - `type` and `version` could be set wrong. They're now fixed accessors, written to JSON but
       never read back.
     - A test was flaky. It assumed an order, and assumed everything landed in one week.
   - **Not taken:** the consumer's stand-in account. A real system principal is planned in
     [12](12-system-principal-plan.md).

   The original plan for this step: an analytics projection, with firings per kiln per week and
   fees charged per week.
   - A `@KafkaListener` in its own consumer group, deduping on `id`, writes to a projection table
     under RLS. That means binding the studio from the event before the transaction, the way the
     invite emails do.
   - A read endpoint and a small chart on the dashboard.
   - It is idempotent and safe to replay from the earliest offset. A test replays the topic twice
     and gets the same totals.
3. **ADR 15: event streaming** (done 2026-11-05:
   [ADR 15](../adr/0015-event-streaming-to-kafka.md)).
   - Why a separate contract and ids only.
   - Why go through the outbox and not send to Kafka inside the transaction.
   - At least once, unordered, and dedupe by id.
   - Kept off at deploy.
4. **Deployed (optional).** The Kafka endpoint on Azure Event Hubs needs the Standard tier, which
   is paid. Leave the flag off until there's a reason to pay for it. If it's turned on, connect
   with the managed identity (OAUTHBEARER), not a connection string.

## Not in this slice

- A schema registry or Avro. JSON with a `version` field is enough for one producer.
- Moving webhook fan-out onto the stream. That would make the stream a hard dependency, which
  conflicts with keeping it off by default.
- More event types. They're cheap to add: a record, a mapping, and a test.
