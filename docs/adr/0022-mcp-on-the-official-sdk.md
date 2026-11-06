# 22. MCP on the official SDK; `immediateExecution` keeps tools on the request thread

- **Status:** accepted
- **Date:** 2026-11-06
- **Replaces:** the "not the MCP Java SDK" note in [plan 17](../planning/17-mcp-and-api-tokens-plan.md)

## Context

The MCP server ([17](../planning/17-mcp-and-api-tokens-plan.md), step 3) was written by hand. The
SDK's transports seemed to run tools on Reactor threads, and the tenant is a `ScopedValue` bound
on the servlet thread. Row-level security depends on that binding. Since then:

- **MCP Java SDK 2.1.0** is GA, and the Spring integrations moved into **Spring AI 2.0**.
- **Spring AI 2.0.2** supports Spring Boot 4.1 and is built on SDK 2.1.0.
- The SDK's `immediateExecution` flag runs a tool on the calling thread. It was added because
  thread hops lost the security context (SDK PR #371, Spring AI PR #3794).

A hand-written protocol is something to maintain against every spec revision. Plan 20 decided
that the SDK handles the protocol, and this app builds on top of it.

## Decision

- **Dependencies:** `io.modelcontextprotocol.sdk:mcp` 2.1.0 and
  `org.springframework.ai:mcp-spring-webmvc` 2.0.2, through the version catalog. This is not the
  Spring AI starter, which would bring `spring-ai-model` and hide the threading setting behind an
  environment check.
- **The server:** `McpServer.sync(WebMvcStatelessServerTransport)`, stateless, with the app's own
  Jackson 3 mapper (`JacksonMcpJsonMapper`).
- **`immediateExecution(true)`, set explicitly with a comment.** The transport blocks the servlet
  thread for the answer. With the flag, the tool runs on that same thread, inside
  `TenantContext.callAs`, so RLS sees the right studio and Spring Security's context is there.
- **The per-studio path stays:** `/api/v1/studios/{studioId}/mcp`. The transport is a
  `RouterFunction`, and router patterns take URI variables. The security chain,
  `TenantContextFilter` and `TokenPolicy` run before it, as before.
- **Ours, on top of the SDK:**
  - `McpRequestFilter`, a filter on the router. A router function can't take `@PreAuthorize`,
    so it checks the caller is staff in the studio in the URL. It also checks
    `MCP-Protocol-Version` (2025-11-25, 2025-06-18, 2025-03-26), which the transport doesn't, and
    answers GET with 405 and `Allow: POST`.
  - Origin: `DefaultServerTransportSecurityValidator` with the CORS origins. A missing Origin
    passes, a foreign one gets 403.
  - `McpToolCalls` turns the eight tools into `SyncToolSpecification`s, with the same names,
    schemas and annotations. Each handler maps only the app's own exceptions to `isError`,
    applies the rate limit, and writes the call log.
- **Rate limits (plan 20, section 6)** sit in a `ratelimit` package. Bucket4j keeps one bucket per
  person, per studio, per channel (MCP and REST tokens), in memory on each replica.
  - MCP counts only `tools/call`, however the caller signed in. A refusal is an `isError` result
    that says how long to wait.
  - REST counts every token request except the MCP endpoint. A refusal is `429` with
    `Retry-After`.
  - The limits come from a `RateLimitSettings` interface. At first, configuration backs it
    (`studiomanager.rate-limit.mcp.*` and `.token.*`). The admin module's live settings replace
    that bean.
  - A bucket is rebuilt (`replaceConfiguration`, keeping what's been used) only when the
    effective limit's revision changes. While a boost is unlimited, the bucket is skipped, and
    those calls don't count.
- **The call log** (`api_call_log`, V30). Each MCP tool call and each REST token call is written
  with a plain insert, because it's an append-only hot path. A failed insert is logged, and never
  fails the call. A system job keeps 90 days.

## Consequences

- The protocol (parsing, JSON-RPC errors, `initialize`, `tools/list`) is the SDK's. A new spec
  revision becomes a dependency bump. SDK 2.2's stateless server for the next revision is the
  next step once it's GA.
- **Behaviour the SDK brings:**
  - A POST whose `Accept` header doesn't list both `application/json` and `text/event-stream`
    gets 400, as the spec requires. Real clients send both.
  - A body that isn't JSON-RPC gets 400.
- With two replicas, the real ceiling is up to twice each limit. Bucket4j's Postgres backend can
  make it exact if that ever matters.
- **Deprecation, checked against 2.1.0:** plan 20 expected
  `DefaultServerTransportSecurityValidator` to be deprecated as a type. It isn't: only its
  `validateHeaders` method is, and Spring AI's transport is what calls it. So no
  `@SuppressWarnings` is needed in this code, and `-Werror` won't trip on it.

## In an interview

"MCP runs on the official Java SDK. The one subtle part is threading. Row-level security depends
on a tenant bound to the request thread, so the server runs with `immediateExecution`, and tools
never leave that thread. Staff checks, protocol-version checks and per-person rate limits sit on
top of the SDK, and every tool call lands in an append-only call log."
