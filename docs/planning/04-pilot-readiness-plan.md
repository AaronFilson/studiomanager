# Next Phase: Pilot Readiness

_Drafted 2026-10-20, after Wave 1's first versions ([02](02-wave1-plan.md)). This is a draft for
discussion; the decisions at the end are open._

## Where we are

Wave 1's five milestones work end to end: 81 API tests against real Postgres and 25 web tests.
But no studio has used any of it, nothing is deployed, and every feature rests on public
evidence rather than a studio's own words. The research report says plainly what it couldn't
measure: hours spent, the money tools in use, and how often members ask "is it ready?".

**So the next phase isn't Wave 2. It is getting one real studio using Wave 1**, then letting what
it does decide Wave 2.

## Proposed phase: P1, pilot-ready (about 3–4 weeks of evenings)

Ordered by what a pilot can't start without.

| # | Work | Why now |
|---|---|---|
| 1 | **First deploy to Azure dev**, following [the runbook](../runbooks/first-deploy.md) | Nothing can be piloted from a laptop. The steps need your Azure, Entra and GitHub credentials. |
| 2 | **Email delivery of notices** through the transactional outbox (Spring Modulith event registry), by Azure Communication Services or another provider | Members don't open an app to check. The batch "ready" notice only helps if it arrives by email. |
| 3 | **Bulk invite**: paste a list of emails, send each an invite link | A 60-member studio can't invite people one at a time. This is the first thing a pilot does. |
| 4 | **Piece photos**: direct-to-Blob uploads with SAS tokens, and a thumbnail on the shelf view | The research found studios identify pieces by maker's mark plus a photo (none use QR labels). |
| 5 | **Hardening**: branch protection with required checks, Dependabot and CodeQL, a Playwright smoke test of the main flows, OpenTelemetry to App Insights, Postgres backups checked, a per-studio data export | Real people's data and money. Also resume-grade engineering. |
| 6 | **Pilot kit**: a one-page guide for the studio, the ten interview questions from [the report](reports/Pottery%20studio%20manager%20role.md), and a two-week before-and-after diary | So the pilot produces evidence, not just opinions. |

**Deliberately not in P1:**
- Live QuickBooks, unless the pilot studio runs it. The CSV export covers the first month.
- Card charging and the mobile app. The web app already works on phones.
- Waiting on the pilot to decide: kilns on the calendar, shift swaps, automatic substitute notices.

## After the pilot: Wave 2 candidates, to be ranked by what the pilot shows

- Charging cards through the pilot's provider (Stripe Connect or Square OAuth) and membership
  billing with pause and stop
- Live QuickBooks posting (OAuth, account mapping, daily journal), or Xero if the pilot is in
  Australia or New Zealand
- Payout import straight from the provider's API instead of typed in
- Consignment and instructor payables, with a 1099-NEC export
- Reading class bookings from the studio's storefront, and waitlists
- The Expo app, if members ask for it
- Invoice OCR into supplies (the AI-pipeline showcase)

## Decisions (2026-10-20)

1. **Pilot-ready first.** Wave 2 waits for what a real studio shows.
2. **No pilot studio yet.** Build for a US studio by default (US units, sales tax, QuickBooks
   export), and add finding one to this phase.
3. **Email through Azure Communication Services.** It stays in one cloud, is managed in
   Terraform, and the managed identity sends it, so there is no API key to keep.
4. **The first deploy waits** until most features are complete and the mobile app is going
   well. Locally, the WSL integration suite stands in for an environment until then.

## Revised order for P1

Deploying moved to the end and the mobile app moved in, so P1 becomes:

| # | Work | Notes |
|---|---|---|
| 1 | **Email delivery** through the transactional outbox (Spring Modulith event registry), sending with ACS | Locally, a dev sender writes the emails to the log, and integration tests assert on the outbox. ACS itself is wired in Terraform but first exercised at deploy. |
| 2 | **Bulk invite** by pasting a list of emails | Sent through the same outbox. |
| 3 | **Mobile app (Expo)**: sign in, my pieces and their status, notices, piece intake from the camera, and staff moving pieces | Shares the generated API client. Members use phones in the studio. |
| 4 | **Piece photos**: direct-to-Blob uploads with SAS tokens, and thumbnails | Azurite locally, so it is tested before Azure exists. The mobile camera intake depends on it. |
| 5 | **Hardening**: branch protection, Dependabot and CodeQL, a Playwright smoke test, OpenTelemetry, a per-studio data export | Branch protection is a setting on your GitHub repo. |
| 6 | **Pilot kit and outreach**: the guide, the interview questions, the diary, and a short list of US studios to approach | The [census](03-tools-in-use.md) already lists 42 US studios, a starting point. |
| 7 | **First deploy** | Your call on when, once 1–6 are in hand. |

## Status

- **1. Email delivery: first version done (2026-10-20).** See [ADR 11](../adr/0011-transactional-outbox-for-email.md).
  - Every inbox notice (batch "ready" and staff notices) is emailed through the Spring Modulith
    outbox (V10), with scheduled retries.
  - Locally, emails are written to the log. ACS and its email domain are in Terraform
    (`infra/modules/email`).
  - Left for the first deploy: the API identity's role on ACS, and a custom sender domain.
  - Left for the pilot to decide: an email opt-out for studio notices.
- **2. Bulk invite: first version done (2026-10-20).**
  - Staff paste a list (a spreadsheet column, or an email's To line) on the Members page. Each
    new address gets its own emailed invite; members and addresses already invited are skipped
    and reported.
  - "Waiting to join" lists open invites, with resend (a new code and a fresh 14 days) and withdraw.
  - The plain code is made when the email is sent, and never stored (V11, `InviteEmails`).
- **3. Mobile app: first version done (2026-10-20).** See [mobile/README.md](../../mobile/README.md).
  - Expo SDK 58 (React Native 0.88) with Expo Router, in the pnpm workspace, on the shared API client.
  - Covers sign-in (Entra External ID, PKCE, refresh token in the keychain), my pieces, the
    shelf for staff with stage moves, drop-off, and notices.
  - A new Entra registration for the app is in `infra/identity`.
  - CI typechecks, lints, tests it and bundles it with Metro. It hasn't been run on a device yet:
    that needs a development build on an emulator or phone.
  - Camera intake and photos came with item 4. Waiting on a decision: push notifications.
- **4. Piece photos: first version done (2026-10-20).** See [ADR 12](../adr/0012-direct-to-blob-photo-uploads.md).
  - Up to 6 photos a piece. The web and the phone shrink each image to a JPEG photo and
    thumbnail, then upload both straight to Blob Storage with create-only SAS URLs. The API checks
    them before they show (V13).
  - The shelf shows each piece's first photo as a thumbnail; the piece page shows them all.
  - The phone can photograph a piece at drop-off; the photo is added once the piece is in.
  - Azurite runs in `compose.yaml` and in the integration tests. Azure signs with a
    user-delegation key from the API's managed identity; the storage account, container, CORS
    and role were already in Terraform.
  - Not yet run on a device or against real Azure storage.
- **5. Hardening:** next in P1.

**Wave 2 started early (2026-10-20)** with maintenance of equipment and the building, the top
candidate from the [maintenance research](05-maintenance-research.md). See
[06](06-wave2-maintenance-plan.md). The nine questions in 05 belong in the pilot kit (item 6).
