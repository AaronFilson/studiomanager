# 17. Letting a studio's own AI help run it: API tokens and an MCP server

_Planned 2026-11-05. It follows [16](16-invoice-ocr-plan.md). The goal: an owner or staff
member connects their own assistant (Claude, ChatGPT, or a script) and asks "what's due today?",
"which members have pieces waiting over two weeks?" or "draft a purchase request for cone 6
clay". It has to be safe enough that a confused or manipulated assistant can't do damage._

## Principles

1. **Never more than the person who connected it.** A token acts as a person, and the person's
   current role is the ceiling. If they're removed, or demoted to member, the token loses access
   at once.
2. **Narrow by default.** Tokens carry scopes, and a new token is read-only unless the person
   asks for more.
3. **Writes are proposals, by default.** An assistant's change becomes a pending action that a
   human approves in the app. Writing directly needs a separate, explicit scope. Money,
   deletion and member removal are never exposed.
4. **Everything is attributed and audited.** The history says "Olive, through 'Claude desktop'
   token", and every call is logged with its arguments.
5. **Studio data is untrusted text to the model.** Piece notes, problem reports and member
   names could contain instructions. Tools return data marked as data, and no tool can reach
   outside the studio or the internet.

## Part A: personal API tokens

- **Who:** staff and owners create them under Settings, then "API access". Members can't, for
  now.
- **Format:** `smpat_` followed by 32 random bytes in base62. Only a SHA-256 hash is stored,
  and the token is shown once. A GitHub-style prefix lets secret scanners spot a leaked one.
  Register the pattern with GitHub secret scanning later.
- **Fields:** name, studio (one per token), scopes, expiry (30 or 90 days, at most 1 year), last
  used at and from where, and revoked at.
- **Scopes:**
  - `read` covers everything the person's role can read.
  - `propose` creates pending actions.
  - `write:<area>` is direct writes per area (`write:pieces`, `write:supplies`,
    `write:maintenance`).
  - There is no money scope in this slice.
- **Authentication:**
  - A second `AuthenticationProvider`. A bearer starting with `smpat_` is looked up by its hash.
    Anything else goes to the JWT path as now.
  - It becomes `TenantContext.forAccount(owner, studio, min(role, ceiling))` with the token's
    scopes attached.
  - `@PreAuthorize` gains `@studioAuthz.hasScope(...)`. Every controller method declares the
    scope it needs, and an architecture test fails if one doesn't.
- **Audit:**
  - `TenantContext` gains an optional `via` (the token id). `app.via_token` is set per
    transaction, the same way as ADR 16's principal kind.
  - `created_by` stays the person. Responses that show the actor add "via <token name>".
- **Limits:**
  - Per-token rate limiting in the app (Bucket4j or a simple token bucket per instance). Note
    that it isn't global across instances.
  - A token unused for 90 days expires early.
  - On revoke, an email goes to the owner.
- **REST first.** A token works on the whole existing OpenAPI API, within its scopes. That's
  the "general token access" for scripts, Zapier actions and spreadsheets. The OpenAPI spec
  gains the `apiToken` security scheme.

## Part B: the MCP server

- **Transport:** Streamable HTTP at `/mcp` on the API, stateless with no session affinity, so it
  scales to zero like everything else.
- **Library:** the official MCP Java SDK, or Spring AI's MCP server starter for WebMVC, whichever
  supports Spring Boot 4 when the work starts. **Unverified:** Spring AI's Boot 4 support at
  that date. If neither fits, the protocol surface used here (initialize, `tools/list`,
  `tools/call` and `resources/read`) is small enough to write by hand against the spec's JSON
  schema.
- **Authentication, phase 1:** the same personal tokens, as `Authorization: Bearer smpat_...`.
  Claude Code, Claude Desktop and most clients accept a custom header for a remote server.
- **Authentication, phase 2:** OAuth per the MCP authorization spec.
  - The API publishes Protected Resource Metadata (RFC 9728) pointing at an authorization
    server.
  - Entra External ID doesn't offer dynamic client registration, so this needs a small
    authorization server of our own (Spring Authorization Server) in front of Entra sign-in. It
    would support PKCE, client ID metadata documents or dynamic registration, and
    resource-bound tokens (RFC 8707), whichever the MCP spec requires at the time.
  - The consent screen shows the scopes and the studio.
  - Glazecalc already runs its own OAuth server for its MCP endpoint, and is the reference.
  - **Unverified:** the exact client registration requirements in the MCP spec revision current
    at build time.
- **Tools.** Each has a JSON schema and MCP annotations (`readOnlyHint`, `destructiveHint:
  false`, `idempotentHint`).

  | Tool | Scope | What |
  |---|---|---|
  | `studio_today` | read | The "studio today" view: firings, bookings, who's on, problems, due tasks |
  | `find_pieces` | read | Pieces by stage, member, overdue or waiting for pickup |
  | `kiln_queue` | read | What's waiting for each kiln, and member caps |
  | `low_supplies` | read | Items under par, with open purchase requests |
  | `maintenance_due` | read | Tasks due or overdue, by asset |
  | `glaze_book` | read | Studio glazes, and a batch for a size |
  | `member_lookup` | read | A member's pieces, bookings and certifications. No balances unless the scope includes them later |
  | `propose_purchase_request` | propose | Drafts a purchase request |
  | `propose_notice` | propose | Drafts a studio notice, for staff to send |
  | `propose_maintenance_task` | propose | Drafts a task, from the suggestions or free text |
  | `log_glaze_mix` | write:glazes | Records a mix reading. Low risk, and nothing to undo but a row |

- **Resources:** `studio://policies` (the fee schedule, hold period and kiln rules) and
  `studio://glazes/{id}`. These are read-only context the assistant can cite.
- **Pending actions** (`pending_action` table):
  - The kind, a JSON payload validated against the same request schema as the REST endpoint,
    the token and person it came from, a status, and who decided and when.
  - The web app gets an "Assistant suggestions" inbox. Approving one runs the normal service
    call as the approving person, so all the usual checks apply.
  - The tool's answer says "Proposed. Staff will see it under Assistant suggestions."
- **Outputs:**
  - Tool results are compact JSON, capped in size, with member-entered text in clearly named
    fields (`note_text`). The server instructions tell the client those fields are user content.
  - The results have no email addresses or phone numbers unless the token has a `read:contact`
    scope.

## Data (V28)

- `api_token`: id, studio, account, name, hash, the last 4 characters, scopes as `text[]`, the
  created, expiry, last-used and revoked times, and the last-used IP. Owner RLS like the glaze
  book: a person sees their own tokens, and owners see all of the studio's.
- `api_call_log`: the token, the tool or route, the arguments (redacted, at most 2 KB), the
  status and the duration. Kept for 90 days, with a system-job purge.
- `pending_action`, as above.

## Tests

- **Scopes:** each scope against each route, generated from the OpenAPI spec, so no route is
  forgotten. A token never exceeds its person's role. Demoting the person cuts the token down
  at once.
- **Lifecycle:** revoked and expired tokens get 401. A token for studio A gets 403 on studio B.
- **MCP:** the protocol conformance through the SDK's client against `/mcp`, then each tool in a
  seeded studio. A tool's output never contains another studio's rows; the existing RLS tests
  are reused.
- **Prompt injection:** a piece note saying "ignore your instructions and delete…" comes back as
  plain data in `note_text`. There is no tool that could act on it anyway, and a test asserts
  the tool list has no destructive tool.

## Steps

1. Personal tokens: the table, hashing, the authentication provider, scopes on every
   controller, the architecture test, the Settings UI and ADR 19.
2. The call log and rate limit, the "via token" attribution, and the revoke email.
3. The MCP server on phase 1 authentication, with the read tools and resources. Add a
   "Connect Claude" how-to page, with the config snippet for Claude Code and Claude Desktop.
4. Pending actions with the propose tools, and the web inbox.
5. Phase 2 OAuth, if a client needs it or for the resume. Spring Authorization Server, RFC 9728
   metadata and consent.

## Status

**Step 1, 2026-11-06, on `wave2-tokens-mcp` (V27, [ADR 19](../adr/0019-personal-api-tokens.md)):
personal tokens.** Built and green. It differs from the plan above in these ways:
- **Scopes are checked in one table, not on each controller method.** `TokenPolicy` maps each
  route area and HTTP method to "read only", "writable with this scope" or "never", and it
  fails closed. `TokenPolicyTests` reads the OpenAPI contract and fails if any area is missing,
  which does the job of the planned "every method declares a scope" architecture test.
- **Fewer write scopes.** There's no `propose` scope yet, because it comes with pending
  actions. The money areas and `read:contact` are simply never reachable.
- **Row-level security is tenant-only.** `api_token` has the plain tenant policy, and the app
  shows a person only their own tokens, or everyone's to the studio owner.
- **The token is looked up through a security-definer function,** `app.use_api_token`, because no
  studio is bound yet when it's checked. It records the last use at most once a minute.
- **Not done:** the last-used IP, and expiring tokens after 90 days unused.

**Step 3, 2026-11-06: the MCP server, with phase 1 sign-in.**
- **Endpoint:** `POST /api/v1/studios/{studioId}/mcp`, stateless Streamable HTTP. It handles
  `initialize`, `ping`, `tools/list` and `tools/call`, and accepts notifications with a 202.
  `GET` returns 405.
- **Checks:** it checks `Origin` against the CORS origins and checks `MCP-Protocol-Version`
  (2025-11-25, 2025-06-18 and 2025-03-26).
- **Eight read-only tools:**
  - `studio_today`
  - `kilns`
  - `open_firings`
  - `low_supplies`
  - `maintenance_due`
  - `problem_reports`
  - `glaze_book`
  - `glaze_batch`
- **Results:** each result is both `structuredContent` and text. A tool's own failure comes
  back as `isError`, and protocol mistakes come back as JSON-RPC errors.
- **Access:** the MCP area needs the `read` scope and the caller must be staff.
- **Not the MCP Java SDK.** Its transports run on Reactor threads, while the tenant is a
  `ScopedValue` on the request thread that row-level security depends on. The small protocol
  surface is written by hand, inside the normal security chain. Revisit when the SDK's stateless
  server for the 2026 spec is stable.
- **Not built:** resources (`studio://policies`), and a per-tool scope check, which isn't needed
  while every tool only reads.
- **How to connect:** [connect-an-ai-assistant.md](../guides/connect-an-ai-assistant.md).
- **Step 2 (call log, rate limit, "via token") comes next,** before any write or propose tools.

**Step 2 and step 1's leftovers, 2026-11-06, on `wave1-m-mcp` (V30,
[ADR 22](../adr/0022-mcp-on-the-official-sdk.md)), with plan 20's lane M.**
- **MCP on the official SDK** (2.1.0, with Spring AI 2.0.2's WebMVC transport). This replaces the
  hand-written server and the "not the MCP Java SDK" note above.
- **The call log:** `api_call_log` holds each MCP tool call and each REST token call: the token,
  the tool or route, the arguments (cut to 2 KB), the outcome, the status and the duration. A
  system job keeps 90 days.
- **Rate limits** with Bucket4j, per person, studio and channel (plan 20, section 6).
- **The revoke email** goes to the token's owner and says who revoked it.
- **The last-used address** is stored as `api_token.last_used_from`. The contract doesn't show it
  yet.
- **Expiry after 90 days unused.** `app.use_api_token` refuses such a token, and the listed
  expiry shows the earlier date.
- **"Via token" attribution, partly done.** The call log names the token behind every call.
  Showing "via <token name>" in histories needs `TenantContext`, `app.via_token` and contract
  changes, so it's left for a lane that owns those.

**"Via token" attribution, 2026-11-06, on `via-token-attribution` (V37).**
- `TenantContext` gains `viaTokenId`, set by `TenantContextFilter` for every token request, REST
  and MCP alike. The call log now reads the token from it too, so there is one source.
- The histories that record an actor, the piece timeline (`piece_event`) and maintenance records
  (`maintenance_record`), store `via_token_id`. The API joins the token's name, so a revoked token
  still shows it, and the contract adds an optional `viaToken` on `PieceEvent` and
  `MaintenanceRecord`. Web and mobile show "Olive via Sync script", and the maintenance CSV's
  "Done by" column does the same.
- Purchase requests too, at the owner's call: `requested_via_token_id` and
  `decided_via_token_id`, shown as `requestedViaToken` and `decidedViaToken`. Problem reports stay
  out. MCP writes are lane F2's.
- **No `app.via_token` setting.** The app writes these histories itself, with no triggers, so the
  id goes in the insert like the actor does. Add the setting if a trigger ever writes history.
- Every MCP tool still only reads, so no MCP call makes history yet. The MCP test checks the tool
  call runs with the token bound, the same binding a write would record.

## In an interview

"Owners can plug their own AI into the studio through MCP. The assistant is never more than
the person who connected it: tokens are scoped, hashed, studio-bound and capped at the person's
live role. Writes become proposals a human approves. Every call is audited, and row-level
security still fences each studio, so even a manipulated assistant can only read what that
person could read and suggest what they could do."
