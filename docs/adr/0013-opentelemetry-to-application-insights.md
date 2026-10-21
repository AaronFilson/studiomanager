# 13. OpenTelemetry to Application Insights, from inside the app

- **Status:** accepted
- **Date:** 2026-10-21

## Context

P1 hardening asks for traces, metrics and logs in Application Insights before real studios use
the app. Terraform already creates Application Insights and hands the API its connection string
as the `APPLICATIONINSIGHTS_CONNECTION_STRING` secret. The app sent nothing.

Microsoft offers three ways:

- **The Application Insights Java agent**, recommended for JVM apps. It is a `-javaagent`, which
  changes class loading. The image starts from a JVM AOT cache (ADR 8) trained without it, so the
  two would have to be proven to work together, and only in Azure.
- **Container Apps' managed OpenTelemetry agent.** The app sends OTLP to an endpoint the
  environment provides. The azurerm provider doesn't configure it yet.
- **`spring-cloud-azure-starter-monitor`**: the OpenTelemetry Spring Boot starter plus the Azure
  Monitor exporter, as an ordinary dependency.

## Decision

Use `spring-cloud-azure-starter-monitor` (7.4.0, built for Spring Boot 4.1).

- It is **off by default** (`otel.sdk.disabled: true` in `application.yaml`), and on in the
  `azure` profile. Locally and in tests there is nowhere to send telemetry.
- **The SDK version is pinned** to what the starter was built on, by importing
  `opentelemetry-instrumentation-bom` 2.24.0. Spring Boot 4.1.1 would otherwise raise the SDK to
  1.62, and the application context then fails to start with an `UnsupportedOperationException`
  in the starter's component loader.
- `TelemetryTests` starts the whole application with telemetry on, against a dead ingestion
  endpoint, and checks that spans are real and requests still work. That is how the version skew
  was found, rather than at the first deploy.

## Consequences

- Traces for HTTP requests and JDBC, plus logs and metrics, reach Application Insights with the
  cloud role name `studiomanager-api`, with no agent and no change to the image.
- Upgrading the starter means upgrading the instrumentation BOM to the version it uses; Dependabot
  raises them separately, and `TelemetryTests` fails if they drift apart.
- The AOT training run has telemetry off, so its classes aren't in the cache. They load normally
  at startup, which costs a little cold-start time in Azure only.

## In an interview

"Our framework's BOM and the vendor starter disagreed about the OpenTelemetry SDK version. Both
compiled, and it only broke at startup. I wrote a test that boots the app with telemetry on
against a dead endpoint, which caught it, and pinned the SDK to the version the starter was built
on, with a note saying why."
