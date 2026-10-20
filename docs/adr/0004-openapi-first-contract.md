# 4. OpenAPI-first HTTP contract

- **Status:** accepted
- **Date:** 2026-10-19

## Context

Three clients consume the API: the React web app now, the Expo mobile app from M1, and possibly
integrations later. When server and clients each describe the API in their own code, they drift
apart, and the drift shows up as runtime errors.

## Decision

`api/src/main/openapi/studiomanager.yaml` (OpenAPI 3.1) is the single source of truth.

- **Server:** the Gradle `openApiGenerate` task (openapi-generator 7.26, `spring`, with
  `useSpringBoot4`, `useJackson3` and `useJspecify`) generates interfaces and DTOs into the
  `contract` package. Controllers implement the interfaces, so a spec change the server hasn't
  caught up with fails compilation. Generated code stays in `build/` and isn't committed.
- **Clients:** `packages/api-client` generates a typed fetch client plus TanStack Query options
  with `@hey-api/openapi-ts`. The output is committed, so mobile and web consume plain TypeScript.
  CI regenerates it and fails on `git diff`.
- **Errors** are RFC 9457 problem details everywhere, including 401 and 403 from the security
  layer.
- DTOs carry a `Dto` suffix and stay at the edge. Controllers map them to domain types, so the
  domain doesn't depend on the wire format.

## Consequences

- The contract is reviewed as a diff, before any code changes.
- Generator quirks are real. openapi-generator's OpenAPI 3.1 support is labeled beta, and
  TypeScript 7 (the native compiler) has no JS API yet, so the TypeScript tooling pins TypeScript
  6.0.

## In an interview

"The spec is the contract. The Spring interfaces and the TypeScript client are both generated from
it, so a breaking change fails the build on whichever side hasn't caught up, not in production."
