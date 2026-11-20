# Plan 25 — Mobile Parity & Pre-Store Readiness

## Goal

Bring the Expo React Native app (`mobile/`) to full day-to-day operational parity with the web app
for pottery studio members and staff, aligned with Plan 21 (R8 permission gating) and Plan 22 (§3
Decision 1 co-owners, billing contact, custom roles, and invites), while keeping the bottom bar
focused and preparing pre-store polish prior to Apple Developer / Google Play enrollment.

## Navigation & Account UX

- **5-tab bottom bar** (`mobile/src/app/studios/[studioId]/_layout.tsx`):
  1. `index` — **Pieces** (`My pieces` when `!can('pieces.move')`)
  2. `intake` — **Drop off**
  3. `tasks` — **Tasks**
  4. `notices` — **Notices** (with unread badge)
  5. `more` — **Studio** hub
- All secondary and detail screens (`today`, `kilns/index`, `kilns/[kilnId]`, `supplies`,
  `glazes/index`, `glazes/[glazeId]`, `roster`, `calendar`, `maintenance`, `members`, `money`,
  `suggestions`, `statement`, `pieces/[pieceId]`, `report`) use `href: null` with history back
  navigation and a header back button.
- **Studio hub** (`StudioMenu.tsx`): strictly permission-gated via `useCan(studioId)`, with
  **Switch studio** (`/?choose=1`) and **Sign out**.
- **Invite acceptance** (`invites/[code].tsx`, `AcceptInvite.tsx`, and manual code/link entry on
  `app/index.tsx`): supports `studiomanager://invites/<code>` deep links and pasted invite codes.

## Shipped Feature Lanes

| Lane | Scope | Feature Module(s) | Route(s) | Status |
|---|---|---|---|---|
| **M0** | Navigation, Studio hub, Invite acceptance | `StudioMenu.tsx`, `AcceptInvite.tsx` | `_layout.tsx`, `more.tsx`, `index.tsx`, `invites/[code].tsx` | Shipped |
| **M1** | Members, co-owners, billing contact, custom roles, duties, certs, invites | `MemberList.tsx` | `members.tsx` | Shipped |
| **M2** | Today handover & kiln operations (load, cap, transitions, availability, log) | `TodayDashboard.tsx`, `KilnList.tsx`, `KilnDetail.tsx` | `today.tsx`, `kilns/index.tsx`, `kilns/[kilnId].tsx` | Shipped |
| **M3** | Supplies on-the-go (search, categories, stock moves, sales, purchase requests) | `SupplyList.tsx` | `supplies.tsx` | Shipped |
| **M4** | Glaze book & interactive dry-batch calculator | `GlazeList.tsx`, `GlazeDetail.tsx` | `glazes/index.tsx`, `glazes/[glazeId].tsx` | Shipped |
| **M5** | Roster, My hours & Calendar bookings | `RosterSchedule.tsx`, `CalendarView.tsx` | `roster.tsx`, `calendar.tsx` | Shipped |
| **M6** | Assistant suggestions, Staff maintenance & Studio notice broadcast | `SuggestionList.tsx`, `MaintenanceStaff.tsx`, `NoticeList.tsx` | `suggestions.tsx`, `maintenance.tsx`, `notices.tsx` | Shipped |
| **M7** | Financials (balances, prepaid cards, payables) & My statement / consignment | `Financials.tsx`, `MyStatement.tsx` | `money.tsx`, `statement.tsx` | Shipped |

## Post-Merge Follow-ups (Cross-Branch)

After Plans 14 and 24 merge into `main`, surface the newly generated API fields in mobile:
- Glaze chemistry / UMF analysis readouts on `GlazeDetail.tsx` (Plan 14).
- Supply `lastPurchaseUnitPrice` and `lastPurchasedAt` badges on `SupplyList.tsx` (Plan 24).

## Pre-Store Readiness Checklist

### Completed (Free, Pre-Enrollment)
- Safe-area & keyboard-avoiding `Screen` wrapper (`SafeAreaProvider` + `SafeAreaInsetsContext` + `KeyboardAvoidingView`) and selectable text support on `Body` (`mobile/src/ui.tsx`).
- App icon, Android adaptive icon, and splash icon branding assets (`mobile/assets/icon.png`, `mobile/assets/adaptive-icon.png`, `mobile/assets/splash-icon.png`) wired into `mobile/app.config.ts`.
- Full Jest test coverage across all feature lanes using exact OpenAPI SDK endpoints and methods.
- Verified clean `typecheck`, `lint`, `test`, and `expo export` build in Linux CI container (`scripts/web-linux.sh`).

### Remaining (Requires Developer Accounts & Real Devices)
- [ ] Enroll in Apple Developer Program and Google Play Console.
- [ ] Configure EAS Build profiles (`eas.json`) for preview (TestFlight / internal track) and production releases.
- [ ] Smoke-test Entra PKCE sign-in, `studiomanager://invites/<code>` deep linking, and camera/photo uploads on physical iOS and Android devices.
- [ ] Prepare store listing screenshots, privacy policy URL, and Entra account deletion / support URLs.
- [ ] Evaluate push notifications (APNs / FCM) for v2 (explicitly deferred for v1; email + in-app notices only).
