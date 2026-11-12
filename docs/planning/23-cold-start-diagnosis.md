# 23. Cold start: why the API takes 2–3 minutes after idle, and the queue after it

_Written 2026-11-11. The owner reports the deployed API taking 2–3 minutes, sometimes longer, to
answer the first request after idle. ADR 8 measured 5.1 s locally, so most of that time is
spent somewhere else._

## 1. Suspects, most likely first

| # | Suspect | Why it fits | How to confirm |
|---|---|---|---|
| 1 | **The startup probe restarts a slow start** | The probe gives up after 5 s + 10 × 10 s ≈ **105 s**, kills the container and starts again. A 60–100 s start that sometimes runs over would then take 2–3+ minutes. | System logs: `Startup probe failed` then `Container ... restarted` (§2, query A) |
| 2 | **0.5 vCPU** (`infra/modules/api/variables.tf`) | The 5 s local figure came from a multi-core laptop. Spring, Hibernate (51 entities), Modulith, OTel, the Azure SDKs and the MCP SDK starting on half a core with serial GC can take 10–20 times longer. | The new `Started:` log line: JVM uptime, CPUs, slowest steps |
| 3 | **Pulling the ~825 MB image** | A cold node pulls the whole image from GHCR before the JVM starts. | System logs: the time between `Pulling image` and `Successfully pulled` |
| 4 | **The AOT cache isn't loading** | The cache is invalid if the classpath or flags differ between the training run and the real run. The JVM then prints a warning and starts without it. | Console logs: search for `AOT cache` / `AOTCache` warnings |
| 5 | **First token and DB connection** | Managed identity → Entra token → Postgres connection, plus the JWKS fetch from `ciamlogin.com`, happen on the first request after the app is ready. | Request duration of the first call in App Insights versus JVM uptime |

A standby cache (Solr, Redis) doesn't help with any of these: the API is still asleep behind it.

## 2. Diagnostics added

- **`StartupReport`** (`api/.../StartupReport.java`) logs one line when the app is ready:
  `Started: JVM uptime … ms, Spring … ms, … CPUs, max heap … MiB, AOT cache …. Slowest steps: …`.
  Spring's `BufferingApplicationStartup` records the steps, and the line lists the 15 slowest
  bean creations. The difference between JVM uptime and the time to first response is the
  probe, ingress and pull time.
- **The startup probe allows ~5 minutes** (30 failures instead of 10), so a slow start isn't
  killed partway through. This stops the restart loop while we measure. It doesn't make the
  start itself faster.

### What the owner runs after the next deploy

Let the API scale to zero (about 5 minutes with no requests), call it once, then:

```
az containerapp logs show -n ca-studiomanager-dev-api -g <rg> --type system --tail 100
az containerapp logs show -n ca-studiomanager-dev-api -g <rg> --type console --tail 300
```

Or, in Log Analytics:

```kusto
// A: pull, probe failures, restarts
ContainerAppSystemLogs_CL
| where ContainerAppName_s == "ca-studiomanager-dev-api" and TimeGenerated > ago(1h)
| project TimeGenerated, Reason_s, Log_s
| order by TimeGenerated asc

// B: the startup report and any AOT warning
ContainerAppConsoleLogs_CL
| where ContainerAppName_s == "ca-studiomanager-dev-api" and TimeGenerated > ago(1h)
| where Log_s has_any ("Started:", "AOT", "AOTCache")
| project TimeGenerated, Log_s
```

Paste the results into §4.

## 3. Likely fixes, chosen from what §2 shows

| Finding | Fix | Cost |
|---|---|---|
| Restarts (1) | The longer probe (done). Then fix the slow start itself | $0 |
| CPU-bound (2) | 1 vCPU / 2 GiB. The consumption plan bills per second only while a replica runs, so the free grant still covers low traffic | Small |
| Pull time (3) | A smaller image: jlink'd runtime, drop unused starters. Or an ACR in the same region | Low |
| No AOT cache (4) | Fix the mismatch so the training run's classpath and flags match the real run's | $0 |
| Slow beans (2) | Make the slowest beans lazy (Kafka admin when streaming is off, the MCP server, invoice readers) | $0 |
| Still slow / pilot hours | A scheduled `min_replicas = 1` during studio hours, or always during the pilot | ~$10–30/mo |
| Perceived wait | The web wakes the API when the shell loads and shows saved query data while it waits (from the 2026-11-11 caching discussion) | $0 |

## 4. Findings (2026-11-12, revision 0000013, image `8b11051`)

**About 100 s from the waking request to ready; 78 s of it is the JVM, and 74 s of that is
Spring's context refresh.**

| Step | Time |
|---|---|
| Request → replica on a node | 1 s |
| Node → container started | 16 s |
| Image pull (already cached on the node, 327 MB) | 0.4 s |
| JVM launch → Tomcat initialized | 12 s |
| JVM launch → ready (`StartupReport`) | **78 s** (`spring.context.refresh` 74 s) |

`StartupReport`'s line: `1 CPUs` (0.5 vCPU rounds up, but the container is capped at half a
core), max heap 742 MiB, `-XX:AOTCache=app.aot` set and no rejection warning in the log.

Slowest steps (they nest, so they overlap rather than add up):
- `entityManagerFactory`: 10.6 s;
- bean post-processing, configuration scanning and parsing: 5–7 s each;
- the demo chain (`demoController` → `demoStudio` → `demoSeed`): 5.3 s;
- the access chain: 4.9 s;
- the web server: 4.0 s;
- firing, piece and OTel beans: 2–2.4 s each.

Beyond those, the time is spread thinly across the rest of the beans. **The start is CPU-bound,
and spread out.**

What it rules out:
- **Image pull (3):** 358 ms.
- **The AOT cache not loading (4):** no warning. It speeds up class loading, not bean creation.
- **A probe restart on this start (1):** it came in under the old 105 s limit, but only just.
  Slower starts were restarting, which explains the 2–3+ minute waits. The 5-minute limit from
  #40 stops that.

### Decisions (owner, 2026-11-12)

- **1 vCPU and 2 GiB: done** (`infra/modules/api/variables.tf`). The migrate and sweep jobs share
  the size.
- **No lazy initialization.** Beans that fail on their first request rather than at startup
  aren't acceptable while the app is being demoed to employers.
- **Spring AOT processing: not this round.** On the JVM it fixes `@Profile` and `@ConditionalOn…`
  at build time. This app has 18 of them and three run modes from one image (web, `migrate`,
  `sweep`), and Kafka streaming switches on a property. Each mode would need its own build, or AOT
  only for the web mode. That's the same "works differently than it looks" risk as lazy init, at
  a bigger size. It needs its own plan, with a test that starts every mode.
- **Demo beans behind a profile: no gain on dev.** Dev is where the demo runs, so the profile
  would be on there anyway.
- **For a demo, warm the API first:** Actions > **Warm for an hour** (`warm-api.yml`), per
  `docs/runbooks/demo-rehearsal.md`. A demo then never sees a cold start.

### After the CPU change (2026-11-12, #41 and #42 deployed)

**JVM start: 78 s → 41 s.** Max heap 742 → 1484 MiB, which confirms the new size is live.

| Step | 0.5 vCPU / 1 GiB | 1 vCPU / 2 GiB |
|---|---|---|
| JVM uptime at ready | 78.2 s | **40.9 s** |
| `spring.context.refresh` | 73.7 s | 38.4 s |
| `entityManagerFactory` (Hibernate) | 10.6 s | 7.4 s |
| Bean post-processing | 6.8 s | 3.5 s |
| Configuration scan and parse | 5.2 s each | 2.7 s each |
| Access chain | 4.9 s | 2.2 s |
| Demo chain | 5.3 s | 1.8 s |
| Web server | 4.0 s | 2.0 s |
| OpenTelemetry SDK | not in the top 15 | 0.9 s |

Almost everything halved, as expected for a CPU-bound start. Hibernate gained least (−30%), so it's
now the largest single step, at 18% of the refresh. Adding the ~17 s Azure spends placing and
starting the container, a cold request should now take about **60 s** to answer, from about 100 s.
The web asks for the API as soon as the page loads (#42), so part of that is hidden behind sign-in.

**What's left, if 60 s is still too slow,** none of it changes how the app behaves:
- **A scheduled warm window** (`min_replicas = 1` at set times, the same switch as `warm-api.yml`):
  no cold start then, at a known cost.
- **Hibernate's 7 s:** check how much of it is metadata building and schema validation
  (`ddl-auto: validate` reads the whole catalog over the network on every start). Flyway already
  guards the schema, so validation could run in the migrate job only. Measure first.
- **Spring AOT:** still its own plan (see the decisions above).

For a demo, run **Warm for an hour** first, as the demo runbook already says.

## 5. The queue

1. **Cold start (this plan).** Deploy, measure (§2), apply the fixes from §3, record the numbers here
   and in ADR 8's Consequences.
2. **Mobile parity (plan 22, §3, decision 1).** First the members screen: invites, roles including
   custom roles, co-owners, billing contact. Then the other owner and staff pages the web has and
   mobile lacks. Start with a gap list made from the web routes and the mobile screens.
3. **Plan 22 tail, waiting on the owner:** `SMOKE_TOKEN` and `SMOKE_STUDIO_ID`, then the k6 run,
   row sizes and trace volume; the email domain; Google and GitHub sign-in in dev; the demo
   rehearsal.
4. **Deferred, each needs the owner's go-ahead:** plan 14 steps 3–5 (glaze book), plan 17 step 5
   (OAuth for MCP), plan 09 (Genesis kiln log).

## 6. Tooling note

`scripts/lib.sh` finds a JDK 25 only at `STUDIOMANAGER_JDK`, `JAVA_HOME` or the Windows IntelliJ
paths. On the owner's Mac none is found, so this change wasn't compiled locally. CI builds it.
Add a macOS path (for example `/Library/Java/JavaVirtualMachines/*/Contents/Home`) so local
checks work there.
