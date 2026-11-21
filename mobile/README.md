# Studio Manager mobile

The Expo (React Native) app for pottery studio members and staff. It uses the same generated API
client (`@studiomanager/api-client`) and fine-grained permission model (`useCan(studioId)`) as the
web app.

## What it does

**Bottom navigation (5 tabs):**

- **Pieces / My pieces:** members see their own pieces, stage, photos, and pickup dates. Staff
  (`pieces.move`) see the whole shelf, filter by stage, and advance pieces to their next stage.
- **Drop off:** take in a piece measured in the studio's units. Staff (`pieces.intake_for_others`
  and `members.read`) can pick the maker by name. A photo taken here is shrunk to JPEG and uploaded
  straight to Blob Storage ([ADR 12](../docs/adr/0012-direct-to-blob-photo-uploads.md)).
- **Tasks:** maintenance assigned to the caller by name or duty, with checklist ticks and notes,
  plus quick access to "Report a problem".
- **Notices:** the member's inbox with an unread badge on the tab. Opening a notice marks it read.
  Staff (`notices.send`) can send studio-wide notices directly from the top of the screen.
- **Studio (More hub):** permission-gated access to the rest of the studio, plus **Switch studio**
  and **Sign out**.

**Studio hub screens:**

- **Today (`today.read`):** staff handover dashboard with pull-to-refresh, uncovered shifts,
  out-of-service kilns, low stock, open problems, kiln states, today's roster/bookings, and
  tap-through shortcuts.
- **Kilns (`kilns.manage` / `firings.manage`):** kiln list, queue loading with `Fill from queue`
  and per-member cap indicators, firing transitions (start, unload & notify makers, cancel) and
  firing log (`firings.manage`), and out-of-service toggle (`kilns.manage`).
- **Supplies (`supplies.manage` / `supplies.sell` / `supplies.purchase.request`):** search and
  category filters, low-stock & substitute alerts, last purchase price & date badges, custom
  quantity stock movements (`Received`, `Used`, `Counted` via `supplies.manage`), member sales with
  a name picker (`supplies.sell` + `members.read`), one-tap `Request more` and open purchase
  requests (`supplies.purchase.request`, plus `supplies.purchase.approve` for Approve/Reject).
- **Glaze book:** browse Studio glazes, My recipes, and Shared recipes, view full recipes, scale
  dry batches (`g`, `kg`, `lb`, `oz`) with **Mix a batch**, adjust buckets with the offline
  **Slurry SG helper**, inspect offline **Chemistry & UMF** limit checks (`@studiomanager/glaze-chemistry`),
  and record studio bucket mixes & SG readings (`glazes.studio.edit`).
- **Roster & My hours:** upcoming shifts (take open shifts, cover offered shifts, ask for/retract
  cover, staff `Mark worked` via `schedule.hours.manage`) plus the caller's credited hours ledger.
- **Calendar:** week-by-week studio bookings, resource reservation with certification/availability
  checks, and cancellation.
- **Assistant suggestions (`proposals.review`):** review, approve, or reject MCP assistant
  proposals with optional rejection notes.
- **Maintenance (`maintenance.manage`):** staff view of open problem reports (with resolution) and
  maintained assets (with out-of-service toggles for non-kiln assets).
- **Members & Access (`members.read`):** billing contact picker (`owners.manage`), co-owner
  promotion/step-down with last-owner guard, built-in and custom role assignment
  (`members.role.assign`), duties checkboxes (`members.duties.manage`), equipment certifications
  (`members.certify`), invite link creation with native share sheet, bulk email invites, and open
  invite resend/withdraw (`members.invite`).
- **Money & Payables (`money.read`, `money.take_payment`, `money.stored_value.manage`,
  `money.payables.manage`):** member balances search (`money.read`), counter payment recording
  (`money.take_payment`), prepaid card redemption (`money.stored_value.manage`), and
  artist/instructor payables with W-9 indicators (`money.payables.manage`), all formatted in the
  studio's currency.
- **My statement / My statement & consignment:** member's own balance, prepaid cards, and
  consignment summary (when applicable).
- **Deep links & OAuth callbacks:** join a studio via `studiomanager://invites/<code>` or by pasting
  an invite code/link on the studio picker screen; Entra redirect callbacks (`studiomanager://auth`)
  and unknown deep links are handled cleanly via `src/app/auth.tsx` and `src/app/+not-found.tsx`.

**Deferred for v1:** push notifications (notices stay email + in-app inbox per owner decision) and
complex desktop-only admin workflows (CSV imports, custom role matrix editing, AI key setup,
bookkeeper exports).

## Settings

The app reads these at build time. Put them in `mobile/.env.local`, which git ignores:

```
EXPO_PUBLIC_ENTRA_CLIENT_ID=<the mobile app registration's client ID>
EXPO_PUBLIC_ENTRA_ISSUER=https://<subdomain>.ciamlogin.com/<tenant-id>/v2.0
EXPO_PUBLIC_API_SCOPES=api://<api client ID>/access_as_user
EXPO_PUBLIC_API_BASE_URL=http://10.0.2.2:8080
```

The first three are the `mobile_env` output of `infra/identity`. None of them is secret.

For the API base URL:

- **Android emulator:** `http://10.0.2.2:8080` reaches the API on your computer.
- **Phone on the same Wi-Fi:** use your computer's LAN address.
- **Dev environment:** use the `api_url` output of `infra/envs/dev`.

`localhost` doesn't work: on a phone it means the phone.

## Running it

Sign-in returns to the app through the `studiomanager://auth` scheme, which Expo Go doesn't
own. So use a development build:

```
pnpm --filter @studiomanager/mobile run android   # builds and installs on an emulator or a USB device
pnpm --filter @studiomanager/mobile run start     # later, to serve JavaScript changes to that build
```

`android/` and `ios/` are generated by those commands and aren't committed. Building for iOS needs a Mac.

## Checks

`typecheck`, `lint`, `test` (Jest with `jest-expo` and React Native Testing Library), and `build`
(`expo export`) run with the rest of the workspace in `bash scripts/web-linux.sh` and in CI.
