# 15. Review of the project as a resume piece

_Written 2026-11-05, after PR #17. The question: what would make this project as strong as
possible on a resume and in an interview, while it stays something a pottery studio would really
use?_

## Verdict

The engineering is already above what most portfolio projects show. It has:
- multi-tenant Postgres RLS with a startup guard;
- a double-entry ledger;
- a transactional outbox;
- signed webhooks with an SSRF filter;
- Kafka through Modulith externalization;
- a system principal for background jobs;
- OIDC with keyless CI;
- Terraform;
- a JVM AOT cache;
- OpenTelemetry;
- 16 ADRs.

The domain is real and specific: kilns, consignment, 1099-NEC, glaze buckets.

**The biggest gap isn't technical. Nobody can see it.** It isn't deployed, the README has no
screenshots, and there's no demo studio. A reviewer gives a project about 60 seconds. Right now
those 60 seconds go to reading a feature list. They should go to clicking a live link.

## Resume bullets that are true today

Pick three to five. Each one is backed by an ADR, so you can go deep on any of them in an
interview.

- Built a multi-tenant SaaS for pottery studios on **Spring Boot 4 / Java 25** (a Spring Modulith
  modular monolith) and **React 19 + Expo**, with an **OpenAPI-first** contract that generates the
  server interfaces and a shared TypeScript client.
- Enforced tenant isolation in **two layers**: application checks, plus **PostgreSQL row-level
  security** bound per transaction from a Java `ScopedValue`. The app refuses to start if its
  database role could bypass RLS. Cross-tenant work goes only through audited
  security-definer functions.
- Designed a **double-entry ledger** for consignment, instructor payouts and stored value. The
  database rejects unbalanced entries, and the app produces 1099-NEC reports that follow the 2026
  IRS rules.
- Delivered reliable side effects through a **transactional outbox**: email, **Standard Webhooks**
  signed outbound webhooks with retries, and an SSRF-safe sender. It also streams domain events to
  **Kafka**.
- Replaced borrowed user identities in background jobs with a **first-class system principal**.
  Each job has a role ceiling, Postgres knows a job from a person, and the audit history is
  accurate.
- Ran infrastructure as **Terraform on Azure Container Apps**, with **keyless GitHub Actions
  (OIDC)**, a passwordless managed-identity database login, a separate migration identity,
  **CodeQL**, Dependabot, and **OpenTelemetry** to Application Insights.
- Cut cold starts on a scale-to-zero container with a **JVM AOT cache** trained during the image
  build. _(Add a measured number. See gap 3.)_

## Gaps, ranked by payoff for the effort

| # | Gap | Why it matters | Effort |
|---|---|---|---|
| 1 | **Deploy it, with a seeded demo studio** | A live link beats every bullet. Seed "Demo Clay Co." with kilns, pieces, a ledger, glazes and photos, and add a one-click "try as staff" or "try as member" button with a read-only reset every night. | Medium. The runbook exists, and it needs your Azure account |
| 2 | **README screenshots, a GIF and an architecture diagram** | Reviewers skim. Show the "studio today" view, piece tracking, the ledger and mobile, plus one diagram: web and mobile, then the API, its modules, Postgres RLS, the outbox, webhooks and Kafka. | Small |
| 3 | **Numbers** | Resume bullets need metrics: cold start with and without the AOT cache, p95 latency under a **k6** load test, test counts, coverage. | Small to medium |
| 4 | **Playwright end-to-end smoke test in CI** | It's the one missing test layer, and it's already on the to-do list. Run it against the deployed dev environment after every deploy. | Small, once deployed |
| 5 | **Coverage and quality gates** | Add **JaCoCo** with a badge, maybe **PIT mutation testing** on the ledger and the batch math, and **jqwik** property tests (plan 14 already uses it). Mutation testing on money code makes a strong interview story. | Small |
| 6 | **ArchUnit or Modulith documentation output** | Modulith can generate C4/PlantUML module diagrams. Commit them, and that fills part of gap 2 for free. | Small |
| 7 | **The AI feature (08 item 6, invoice OCR)** | Hiring in 2026 asks about LLM integration. A supplies inbox that reads an invoice PDF into purchase lines, with structured output, an eval set and human confirmation, fits the product and the market. Build it after the glaze book. | Medium |
| 8 | **Security story, written up** | A `SECURITY.md` and a short threat model (STRIDE over tenancy, uploads, webhooks and imports). You've done the work, so this just makes it visible. | Small |
| 9 | **Repo hygiene** | Badges (CI, CodeQL, coverage), a CHANGELOG or releases with tags, and closing or merging the six Dependabot branches. Stale bot PRs look abandoned. | Small |

## Keep it a real studio tool

What makes it believable is that it's specific to pottery, so don't trade that for more
technology:
- **The glaze book (plan 14)** is the most "potter" feature yet, and it ties to two of your other
  repos. That makes a good portfolio thread: "I built the calculator, the encrypted vault and the
  studio system, and they talk to each other."
- **Run the kiln log import** once a real Genesis file exists. Hardware data is a good story.
- **Get one real pilot studio,** even a friendly one. "Used by one studio, which logged N firings
  a month" is worth more than any feature.

## Suggested order
1. Merge PR #17.
2. Build the glaze book (plan 14).
3. **Deploy, with the demo studio and screenshots** (gaps 1 and 2).
4. Add Playwright, k6 and the numbers (gaps 3 and 4).
5. Add the coverage, mutation and property tests, and the badges (gaps 5, 6 and 9).
6. Build the invoice OCR with an LLM (gap 7).
7. Write the threat model (gap 8).

If you only do one thing, deploy it with a demo studio before you add more features.
