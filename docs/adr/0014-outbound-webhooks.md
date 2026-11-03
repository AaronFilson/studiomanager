# 14. Outbound webhooks: signed, retried from their own table, and behind an SSRF filter

- **Status:** accepted
- **Date:** 2026-11-02

## Context

Owners ask to "connect Zapier" ([03](../planning/03-tools-in-use.md)). Webhooks give a studio's
own tools a push when something happens, and Zapier, Make and n8n all accept them without an app
listing. They raise three problems:

- **SSRF.** Staff type the URL, so the server POSTs wherever it points: the cloud metadata
  endpoint, the database, anything on the private network.
- **Delivery.** It must survive a rollback (nothing sent), an instance dying (nothing lost) and a
  receiver that's down for a day (caught up later).
- **Trust.** The receiver must be able to tell the POST came from us.

## Decision

- **Events are written in the transaction that caused them.**
  - A listener in the `webhook` module writes one `webhook_event`, and one `webhook_delivery`
    per subscribed endpoint.
  - The body is stored exactly as it will be signed. A dedupe key such as
    `pieces.ready:<firing>` makes a second run add nothing.
  - After commit, the first attempt runs at once on another thread.
  - `member.joined` is raised outside any studio, so it's an async after-commit listener instead,
    and the Modulith outbox retries it (ADR 11).
- **The delivery row owns its retries, not the outbox.**
  - Webhooks need a growing backoff (1 min, 5 min, 30 min, 2 h, 6 h, 12 h, then daily, about 4
    days in all), with a log of each attempt for staff to read.
  - A sweep runs every minute and finds due rows across studios through
    `app.due_webhook_deliveries` (ADR 10).
  - Each attempt claims its row with a conditional update, so two senders never send it at once.
    A claim lapses after 2 minutes, so a sender that died is covered.
- **When an endpoint stops working.**
  - A 410 disables it at once. So do 3 days of failures, or 50 in a row.
  - Staff get a `WEBHOOK_DISABLED` notice, which is also emailed.
  - Turning it back on doesn't replay what it missed. Staff can resend from the log.
- **Signing follows Standard Webhooks.**
  - The headers are `webhook-id`, `webhook-timestamp` and `webhook-signature`
    (`v1,` HMAC-SHA256), so receivers can use an existing library.
  - It's tested against the spec's published vector.
  - The secret is shown once. At rest it's AES-GCM encrypted under a configured key, with the
    endpoint id as associated data.
  - A rotation signs with both secrets for 24 hours.
- **The SSRF filter is `platform.http.SafeHttpClient`.** It's built on Apache HttpClient 5,
  because its `DnsResolver` hook lets the filter sit inside the connection's own lookup. The JDK
  client has no such hook.
  - Only https, on port 443 or 8443.
  - Every resolved address must be public. That excludes private, loopback, link-local, CGNAT,
    reserved and multicast addresses, and the IPv6 forms that carry an IPv4 address.
  - The check runs inside the connection's DNS lookup, so the address that was checked is the
    address that's connected to, and DNS rebinding gets nowhere.
  - No redirects, cookies, proxy or retries. 5 seconds to connect, 10 to answer, and at most
    64 KB of the response read.
  - `allow-private-networks` turns the filter off for local work and tests. The azure profile
    sets it to false.

## Consequences

- **Delivery is at least once**, and the docs say so. Receivers drop duplicates by `webhook-id`,
  which stays the same on every retry and resend.
- **Order isn't guaranteed.**
- **Payloads carry members' names and emails**, which a Zap needs. Only staff can add an
  endpoint, and the page says so.
- **Scale to zero.** Retries happen only while an instance is up (as for email). The first attempt
  runs during the request, so the usual case doesn't wait.
- **The sweep sends one at a time.** That's fine at pilot volume. Parallelise it if a studio's
  receiver is slow and the backlog grows.
- **Open at first deploy.** `STUDIOMANAGER_WEBHOOK_SECRET_KEY` has to come from Key Vault. The
  azure profile has no default, so the API won't start without it.

## In an interview

"Staff type the URL and the server POSTs to it, so it's an SSRF hole unless the server resolves
the name, checks every address, and connects to the address it checked. I put the check inside the
HTTP client's DNS resolver for that reason. Deliveries are written in the business transaction, so
a rollback sends nothing, and each delivery row carries its own backoff and attempt log. They're
signed the Standard Webhooks way, and an endpoint that keeps failing is turned off, with staff
told."
