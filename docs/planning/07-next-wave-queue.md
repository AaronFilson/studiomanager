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

- **0. CI fix:** done 2026-10-21. PR #2 (`p1-photos`) went green and was merged into `main`; the
  branches below are rebased onto it.
- **1. Pilot kit:** written 2026-10-21.
- **2. Dependabot and CodeQL:** done 2026-10-21. The owner still applies
  [the ruleset](../runbooks/branch-protection.md) and turns on secret scanning.
- **3. Data export:** done 2026-10-21. `GET /studios/{id}/export`, owner only: a ZIP with one CSV
  per studio table (found from the catalog, so new tables are included), `members.csv` with names
  and addresses, and a README. Text cells are guarded against spreadsheet formulas; binary columns
  (an invite's code hash) are left out. "Download all data" is on Settings.
- **4. OpenTelemetry:** done 2026-10-21, see [ADR 13](../adr/0013-opentelemetry-to-application-insights.md).
  On in the `azure` profile only. First exercised for real at the first deploy.
- **5. Maintenance reminders:** done 2026-10-21 (V14). An hourly sweep sends one notice per task
  per cycle (by email through the outbox) when it first shows as due soon or due. It goes to the
  named assignee, else every holder of the duty, else the staff. Completing a task starts a new
  cycle. A new SECURITY DEFINER function finds the studios (ADR 10); the rest runs under RLS.
- **6. Suggested tasks and history CSV:** done 2026-10-21. Suggestions are offered, not created:
  each asset lists the published tasks for its kind that it doesn't have yet, each with its
  source (L&L for electric kilns, OSHA for extinguishers, ANSI for eyewash, NRCA for roofs, and
  studio practice for the clay trap). Wheels, pug mills and the like get none: nothing is
  published for them. The history export covers every asset between two dates.
- **7. Down assets block bookings:** done 2026-10-21 (V15). A calendar resource can be linked to
  an asset, kilns included. While the asset is down, only maintenance or a closure can be booked.
  Bookings made before it went down are left for staff to sort out; a pilot will show whether
  they want those flagged or cancelled.
- **8. Mobile tasks and problem reports:** done 2026-10-21 (V16). The phone has a Tasks tab
  (tick the checklist, mark done). Anyone can report a problem, on the phone or the studio's home
  page, with an asset or "something else". The staff and technicians get a notice. Open reports
  show on the Maintenance page, where staff resolve them. A report is its own table, not a
  maintenance record: it isn't work done. Still not run on a device.
