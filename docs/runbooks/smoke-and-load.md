# Smoke tests, load tests and dev measurements

Plan 22, lane D.
- The Playwright smoke test runs after every deploy to dev.
- The k6 load test runs by hand.
- The two measurements at the end are taken once, on dev, and their results go into plan 20 §4 and
  plan 22.

## Playwright smoke test (every deploy)

`deploy-dev.yml`'s `smoke` job runs `e2e/tests/smoke.spec.ts` after the web app is deployed. The
web and API URLs come from Terraform's outputs, so nothing needs setting for the first four tests:

| Test | Catches |
|---|---|
| The landing page loads without errors | A broken bundle, a runtime error on load |
| Sign in goes to the External ID authority | A wrong `VITE_ENTRA_AUTHORITY` or client id in the build |
| A deep link serves the app | `staticwebapp.config.json`'s navigation fallback |
| The API is ready and allows the web origin | A failed rollout; CORS no longer listing the web app |

The fifth test, **a read token reaches the studio**, is skipped until these are set on the `dev`
environment in GitHub (Settings > Environments > dev):
1. In the dev web app, sign in, open the demo studio, and go to **Settings > API tokens**.
2. Make a token with only the `read` scope, named "CI smoke", with the longest expiry offered.
3. Add it as the environment secret `SMOKE_TOKEN`.
4. Add the studio's id (from the URL) as the environment variable `SMOKE_STUDIO_ID`.

The token stops working after 90 days unused. The smoke job uses it on every deploy, so a
token stays alive as long as deploys keep happening. If the test fails with 401, make a new token.

A failed run uploads `playwright-report` (traces and screenshots) as an artifact.

Signing in isn't automated: External ID's sign-in page is interactive. Covering it would take a
test user and the password grant, which External ID doesn't offer for this kind of app.

## k6 load test (by hand)

The **Load test** workflow (Actions > Load test > Run workflow) runs `e2e/k6/token-limits.js`:
- 300 calls to the kilns list with the smoke token, ten at a time;
- expects the first ~200 to succeed and the rest to get `429` with `Retry-After`, never a 5xx;
- reports latency for the calls that succeeded (p95 under 800 ms).

It needs the same `SMOKE_TOKEN` and `SMOKE_STUDIO_ID`, plus an environment variable `API_URL`
(the API's URL, from the deploy summary).

It spends about 300 of the token's 5,000 daily calls, and keeps the API awake for about five
minutes (about $0.005). The summary is uploaded as `k6-summary.json`. Record the p95 and the
burst size seen in plan 22.

To test the MCP limit by hand, connect an assistant with the token
(`docs/guides/connect-an-ai-assistant.md`) and run the "morning check-in" from plan 20 §6.

## Measure row sizes (once)

Plan 20 §4's caps assumed 1.5 KB of JSON a row. To check on the seeded demo studio:
1. Admin page > Demo studio > **Reset the demo studio**.
2. For each capped list, with the smoke token:
   `curl -s -H "Authorization: Bearer $TOKEN" "$API_URL/api/v1/studios/$STUDIO/pieces" -o pieces.json`
   then compare the file's size with its item count.
3. Write the bytes per row next to each list in plan 20 §4's table, and change a cap only if a
   full page would go over about 3 MB.

## Check trace volume (once)

Lane S set `OTEL_TRACES_SAMPLER`. Whether the Azure Monitor starter honours it is unverified.
After a smoke run and a load-test run, in Application Insights > Logs:

```kusto
requests
| where timestamp > ago(1h)
| summarize requests = count(), sampled = sum(itemCount) by bin(timestamp, 5m)
```

If `sampled` is larger than `requests`, sampling is working: `itemCount` is how many requests each
stored row stands for. If the two are equal at the load test's rate, the sampler is being ignored.
In that case, set `APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE` on the container app instead, and note
it in ADR 13.
