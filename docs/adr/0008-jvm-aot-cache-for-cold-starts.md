# 8. JVM AOT cache for scale-to-zero cold starts

- **Status:** accepted
- **Date:** 2026-10-19

## Context

The API runs on Azure Container Apps with scale to zero, to stay inside the free grant. The first
request after idle pays for a JVM and Spring start, which can take seconds. GraalVM native images
would start fastest, but they add build complexity, need reflection configuration, and give up
JIT peak performance.

## Decision

Use the JDK's **AOT cache** (JEP 483 and JEP 514, Java 24–25). During the image build, a
*training run* starts the application context once, without a database (the `aot-training`
profile, with `spring.context.exit=onRefresh`), and records the classes it loads and links into
`app.aot`. Real starts run with `-XX:AOTCache=app.aot`. The JVM flags that affect the cache are
fixed in `JAVA_TOOL_OPTIONS`, so the training run and production match.

## Consequences

- Measured locally (WSL, Docker, the same image, against Postgres 18), application start was
  **5.1 s with the cache versus 9.7 s without it**. Repeated restarts ran the two containers at the
  same time, so they competed for CPU and the numbers were noisy (about 8.0 s versus 9.7–15.2 s),
  but the cache won every time. Measure on Container Apps before quoting a production number.
- The image is larger (about 825 MB in total). Shrinking it, for example with a jlink'd runtime,
  is a later optimization.
- The JDK logs a harmless AOT stub-linking warning at startup.
- Spring AOT processing and GraalVM native images are still options if cold starts matter more
  later.

## In an interview

"Scale to zero makes cold start a product concern. Rather than going native, I used Java 25's AOT
cache, trained during the Docker build, which roughly halved startup in my measurements with no
code changes."
