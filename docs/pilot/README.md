# Pilot kit

_Written 2026-10-21 for P1 item 6 in [04](../planning/04-pilot-readiness-plan.md). No pilot studio
has been chosen yet; the kit assumes a US studio._

The pilot should produce evidence, not just opinions. The [research report](../planning/reports/Pottery%20studio%20manager%20role.md)
says what public sources couldn't measure: admin hours (0 of 34 owner sources give any), how often
members ask "is it ready?", the money tools in use, and inventory and maintenance pain. One studio,
measured before and after, is the cheapest way to start filling those gaps.

## What is in the kit

| File | Who uses it | What for |
|---|---|---|
| [outreach.md](outreach.md) | Us | Ten US studios to approach first, why each fits, and the first email and follow-up |
| [studio-guide.md](studio-guide.md) | The studio's owner or manager | One printed page: what the app does, what we ask, how their data is handled, first-day setup |
| [interview-guide.md](interview-guide.md) | Us, with the owner or manager (and a technician or monitor if there is one) | The kickoff and closing interviews, with stable question IDs to record answers against |
| [diary.md](diary.md) | The owner or manager, daily | Counts for one week without the app and two weeks with it, under 3 minutes a day |

## Order of the pilot

| Step | When | What happens | Output |
|---|---|---|---|
| 0. Prerequisite | Before step 1 | The first deploy (P1 item 7) and the per-studio data export (P1 item 5). The guide promises both | A URL the studio can sign in to |
| 1. Recruit | Weeks −3 to −1 | Send the [first email](outreach.md#first-email), follow up once, agree dates | A studio and a named contact |
| 2. Kickoff interview | Day −7 or earlier | About an hour. [Q1–Q10, M1–M9 and the studio profile](interview-guide.md#kickoff-interview). Hand over the [studio guide](studio-guide.md) | Answers recorded by question ID |
| 3. Baseline diary | Week 0 (7 days) | The studio keeps working as it does today and fills in the [diary](diary.md) daily. No app yet | 7 baseline rows |
| 4. Setup | Day 1 of week 1 | The first-day steps in the guide, with us on a call | Kilns, members and fees in the app; setup minutes logged separately |
| 5. Two weeks using the app | Weeks 1–2 (14 days) | Normal use. Same diary every day. A 10-minute check-in at the end of week 1 | 14 rows |
| 6. Closing interview | Within 3 days of the end | About 45 minutes. [C1–C10](interview-guide.md#closing-interview), with the diary numbers on the table | Answers by question ID |
| 7. Write-up | Within a week | A short note in `docs/planning/` comparing week 0 with weeks 1–2, using the rules in [diary.md](diary.md#comparing-before-and-after), plus what to build next | A ranked Wave 2 list backed by the pilot's own words and counts |

## Ground rules

- Record answers against the question IDs and diary rows, with verbatim quotes where possible.
- Don't change the app mid-pilot except to fix bugs. A feature change in week 1 spoils the comparison.
- Write "don't know" rather than a guess, in interviews and in the diary.
- Keep the studio's name out of the repo unless they agree to be named. Use "the pilot studio".
