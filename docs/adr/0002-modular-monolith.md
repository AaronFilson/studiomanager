# 2. A modular monolith with enforced boundaries

- **Status:** accepted
- **Date:** 2026-10-19

## Context

Wave 1 has seven areas (studio, member, piece, firing, kiln, supplies, notify) built by one
developer, deployed to a scale-to-zero platform on a free-tier budget. Microservices would multiply
deployments, databases, network failure modes and cost, for no scaling benefit at this size. A
plain monolith, though, tends to decay into a tangle that can never be split.

## Decision

One Spring Boot application, structured as [Spring Modulith](https://spring.io/projects/spring-modulith)
modules: each top-level package under `io.github.aaronfilson.studiomanager` is a module. Its
top-level types are its API; sub-packages and package-private types are internal.

- `ModularityTests` runs `ApplicationModules.verify()` in the build, so a forbidden dependency or a
  cycle fails CI. It also writes C4 component diagrams to `build/spring-modulith-docs`.
- `platform` is an *open* module (tenancy, security, errors) that everything may use. Where
  `platform` needs something from a feature module (for example, an account ID from a token), it
  declares an interface and the feature module implements it, so the dependency points one way.
- Modules will talk through application events with the transactional outbox (the Modulith event
  publication registry) from M2 on, starting with "kiln unloaded → notify owners".
- The OCR worker is the one planned separate deployable, because its scaling and failure
  profile differs: bursty, slow, and calling paid AI services. The 2026-10-20 plan revision
  moved it to Wave 2.

## Consequences

- One build, one deployment, one database, and in-process calls that are easy to debug.
- Boundaries are checked by a test rather than kept by discipline alone.
- Extracting a module later is mostly mechanical, because its API is already explicit.

## In an interview

"I split where the scaling and failure characteristics differ, not by default. The monolith is
modular, and a test enforces it: if `studio` reaches into `account`'s internals, the build fails. The
one planned extraction is the OCR worker, which is bursty and calls paid services."
