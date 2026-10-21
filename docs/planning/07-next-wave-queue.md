# The next wave: work queue

_Queued 2026-10-21 from [04](04-pilot-readiness-plan.md) (P1 status), [06](06-wave2-maintenance-plan.md)
(maintenance's next slices) and [02](02-wave1-plan.md) (what's left in Wave 1). The decisions in
04 stand: pilot-ready first, a US studio by default, and the first deploy waits._

## How it's ordered

What a pilot can't start without comes first. The pilot kit's README makes two things
prerequisites: the first deploy (the owner's call) and a per-studio data export, which the studio
guide promises. Next comes hardening that guards real data. Then the maintenance slices, which
take the most work off a manager (maintenance is in 38 of 40 technician postings, per 02), and
then the smaller Wave 1 leftovers.

Each group is a branch stacked on the one before, like P1 so far. `p1-photos` is PR #2.

| # | Work | Branch | Notes |
|---|---|---|---|
| 0 | **CI fix:** the web job failed on PR #2 | `p1-photos` | UI tests gave up after 1 s while code-split routes loaded on a cold runner. `scripts/web-linux.sh` now runs the job on Linux locally. |
| 1 | **Pilot kit** (P1 item 6): guide, interviews, diary, outreach | `p1-hardening` | In [docs/pilot](../pilot/README.md). Contact details are TBD. |
| 2 | **Dependabot and CodeQL**, and the branch-protection settings to apply | `p1-hardening` | Branch protection is a repo setting for the owner; the doc lists the required checks. |
| 3 | **Per-studio data export:** the owner downloads a ZIP of CSVs of everything in the studio | `p1-hardening` | A prerequisite for the pilot. Read inside the studio's own RLS scope, so it can't leak another studio's rows. |
| 4 | **OpenTelemetry to App Insights** | `p1-hardening` | Off locally. On in Azure via the connection string in Terraform. |
| 5 | **Maintenance reminder emails:** daily, to the assignee, once per due cycle | `wave2-maintenance-2` | Through the outbox. Cross-studio, so a new SECURITY DEFINER function (ADR 10). |
| 6 | **Starter tasks and history as CSV** | `wave2-maintenance-2` | L&L's 1/10/30 firings for kilns, a weekly clay-trap check, a monthly extinguisher check. |
| 7 | **A down asset blocks calendar bookings** of it | `wave2-maintenance-2` | Needs a link from a calendar resource to an asset. |
| 8 | **Mobile "my tasks"**, and members reporting a problem | `wave2-maintenance-2` | A report becomes a record for staff, and a notice. |
| 9 | **Photos on maintenance records, and parts used from supplies** | `wave2-maintenance-2` | Reuses `BlobStore` and the upload flow from piece photos. |
| 10 | **Playwright smoke test** of the main web flows | `p1-hardening` or later | Against the built web app with a stubbed API. Browsers install into the repo. |
| 11 | **Wave 1 leftovers:** firings on the calendar, automatic "use this instead" notices, shift swaps | `wave1-leftovers` | Small, and each is independent. |
| 12 | **First deploy** | — | The owner's call, through [the runbook](../runbooks/first-deploy.md). |

## Status

Updated as each item lands.

- **0. CI fix:** done 2026-10-21, pushed to `p1-photos`.
- **1. Pilot kit:** written 2026-10-21.
