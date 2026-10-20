# Integration feasibility: QBO, Xero, Square, Stripe, Shopify + adjacent vertical SaaS

_Working notes. Started 2026-10-20. All "accessed" dates are 2026-10-20 unless stated._

## Plan

1. Primary docs per platform (program terms, auth, objects, webhooks, rate limits, inventory, terms that bind a free multi-tenant app):
   QBO (Intuit App Partner Program) -> Xero (developer pricing, certification, API terms) -> Square -> Stripe -> Shopify.
2. Adjacent vertical SaaS help-centre pages: Mindbody, Pike13, Momence, Jackrabbit, Rock Gym Pro, Fabman, Kiln Fire, CourseStorm, Amilia, WellnessLiving.
3. Third-party connectors and prices: Synder, A2X, Bookkeep, Zapier, Make.
4. User voice: QuickBooks Community, Reddit RSS (few queries, ~65 s apart), marketplace reviews (Intuit/Xero app stores) where reachable.
5. Closing: Hypothesis evidence / Other strong signals / Gaps and limits.

Rules followed: WebSearch/WebFetch/Read/Write/Edit/Grep/Glob only. Quotes are only text WebFetch returned verbatim. Anything not re-opened = UNVERIFIED. Country tag on every data point.

Already covered elsewhere (not redone): 84-studio tool census, Square at 7/84 studios (mostly gift cards), QuickBooks named once by a potter (pre-2024), no Xero/Wave in any studio source, owners' "nothing connects" complaints ([03](../../03-tools-in-use.md)); competitor list and pain points ([01](../../01-market-survey.md)).

## Bottom line (read first)

- **Payments: start with one provider, Stripe, connected as the studio's own account.**
  - Connect in "Stripe handles pricing" mode costs the platform $0.
  - The pottery incumbent (Kiln Fire) works this way.
  - Square appears as a processor at 0 of 10 adjacent vendors.
  - Square remains cheap to add later ($0, listing optional). Its Bookings writes need the studio to pay for Appointments Plus/Premium.
- **Accounting: don't build native QBO sync in phase 1.**
  - Build a **payout-level ledger and export**: one record per processor payout, broken down by revenue line and fees.
  - QBO's free Builder tier makes writes free, but the read cap is shared by every customer and has already broken one multi-tenant app (2026).
  - QBO's free plan can't connect apps, and class/location tracking needs Plus.
  - 6 of 10 adjacent vendors stop at export or GL reports. The 2 that built native sync charge $39–85/month for it.
  - Stripe lists embedded QBO/Xero partner apps for Connect platforms (preview).
- **Xero is a poor early target for a free product.** It costs A$35–245/month once you pass 5 studios, and its terms ban AI/ML training on API data.
- **Shopify: read, don't write.** App Store rules forbid recording outside transactions in Shopify or connecting a non-Shopify POS.
- **Inventory:** the platforms only model sellable stock. Consumables and equipment (kilns, wheels) have to live in our own model.
- **Scheduling:** kiln, wheel and equipment reservations tied to pieces are supported. Class booking is not, because studios already book classes elsewhere (03).

---

## Findings log

### QBO 1 — App Partner Program (US first; UK/AU/CA later)

Access problem: every developer.intuit.com docs page (incl. the Partner FAQ), help.developer.intuit.com (Salesforce), Intuit's blog (now redirects to medium.com/intuitdev, 403), the Business Wire release (403) and the investors.intuit.com PDF (undecodable) failed through WebFetch. **Medium's RSS feed `https://medium.com/feed/intuitdev` works** but only carries the 10 newest posts (2026-05 to 2026-10). So program numbers below come from secondary sources, labelled as such.

| Claim | Source (type) | Date | Verbatim |
|---|---|---|---|
| Program announced 2025-05-15 | Truto blog (below) | 2026-03-30 | "On May 15, 2025, Intuit announced the App Partner Program, which introduces platform service fees for developers" |
| Live 2025-07-28 for US partners | Woodard Report (trade press) https://report.woodard.com/articles/intuits-app-partner-program-marks-new-phase-in-developer-ecosystem-fpwr | 2025-05-20 | "Set to go live on July 28, 2025," |
| Builder free: unlimited Core + 500k CorePlus | Woodard | 2025-05-20 | "Includes unlimited calls to Core API endpoints and up to 500,000 CorePlus API credits per month" … "with limited support and no overage flexibility." |
| Silver $300/mo | Woodard | 2025-05-20 | "Silver ($300/month): Offers 1 million API credits with overage charges of $3.50 per 1,000 additional credits." |
| Gold $1,700/mo | Woodard | 2025-05-20 | "Gold ($1,700/month): Includes 10 million API credits, lower overage fees, and enhanced visibility and support." |
| Platinum $4,500/mo | Woodard | 2025-05-20 | "Platinum ($4,500/month): Provides 75 million API credits with top-tier partner support and access to premium APIs." |
| Metered fees from 2025-11-01 | Woodard | 2025-05-20 | "Developers who exceed their tier's API limits will be subject to metered usage fees starting November 1, 2025." |
| Woodard's own analysis (not a developer quote) | Woodard | 2025-05-20 | "The move represents a pivot from a fully open ecosystem to one that introduces economic gatekeeping." |
| Builder over cap = blocked | Apideck blog (unified-API vendor, marketing) https://www.apideck.com/blog/quickbooks-api-pricing-and-the-intuit-app-partner-program | pub 2025-09-20, updated 2026-10-06 | "Overage: API calls are blocked after exceeding the 500,000 limit" |
| Core = writes, CorePlus = reads | Apideck | same | Core "Examples: POST requests to create/update invoices, bills, customers, vendors, accounts, payments, etc." CorePlus "Examples: GET requests to retrieve accounts, query company information, fetch reports, read invoices, etc." |
| Classification rule; only 2xx counted; workspace-level billing | Apideck | same | "API calls are now classified into two categories based on the endpoint and HTTP method used." / "Metered Calls: Only successful API calls (2xx HTTP status codes) are counted" / "Billing Scope: Charges apply at the Workspace level" |
| Transition discount | Apideck | same | "July 28 - October 31, 2025: 100% discount on variable API fees (transition period)" |
| Platinum overage | Apideck | same | "Overage Rate: $0.25 per 1,000 API calls (best available rate)" |
| Premium APIs (Projects, Custom Fields, Sales Tax, Time) Silver+ only | Apideck | same | "As of July 17, 2025, Intuit has launched Premium APIs exclusively available to Silver, Gold and Platinum tier partners." |
| Min. active connections Gold 500 / Platinum 3,000 | Truto blog (integration vendor, marketing) https://truto.one/blog/how-much-does-the-quickbooks-api-cost-2026-pricing-rate-limits/ | 2026-03-30 | "To be eligible for the Gold Tier, you'll need to have a minimum of 500 Minimum Active Connections;" (Platinum 3,000 from its table) |
| Sandbox not metered | Truto | 2026-03-30 | "API calls executed within the sandbox environment are not included in the new fee structure." |
| Program is QBO-only | Truto | 2026-03-30 | "The Intuit App Partner Program and its associated pricing structure only applies to QuickBooks Online (QBO) integrations." |
| Expansion to UK, AU, CA (excl. Quebec) from 2025-11-03 | **UNVERIFIED** (search-engine summary of blogs.a.intuit.com/?p=35224; page unreachable) | — | — |
| Silver+ gating continues in 2026 | Intuit Developer blog via RSS (primary) https://medium.com/intuitdev/launch-alert-new-project-management-apis-and-custom-dimensions-enhancements-f9610448fbe7 | 2026-05-13 | "Project Budget and Change Order APIs are available to Silver Tier and above within the Intuit App Partner Program." |

**What this means for a free studio app (US):** most writes (Apideck: "Most data-in operations such as creating or updating records"; Truto: "Core API calls are unmetered and uncharged.") are Core = unmetered on Builder; the exact per-endpoint classification table (help.developer.intuit.com) could not be opened. Reads (queries, reports, webhook follow-up GETs; CDC probably, but its classification wasn't verified) are CorePlus, capped at 500k/month across all connected companies on Builder, then **blocked**, not billed. A write-mostly sync (push daily summaries; read back only IDs from write responses) fits Builder comfortably: e.g. 1,000 studios × 30 reads/day ≈ 900k/month would NOT fit; 1,000 studios × 10 reads/day ≈ 300k would. (Arithmetic mine.)

### QBO 2 — OAuth tokens (primary, 2026)

| Claim | Source | Date | Verbatim |
|---|---|---|---|
| Refresh tokens: 100-day rolling + 5-year hard limit; new opt-in field | Intuit Developer blog via RSS https://medium.com/intuitdev/track-your-refresh-tokens-hard-expiry-with-a-new-response-field-7a95ed3ab67f | 2026-06-05 | "Earlier this year, we shared that refresh tokens now have a hard five-year limit." / "That's your rolling 100-day window, which resets every time you refresh." / "Unlike the 100-day rolling window that resets every time you refresh, this new five-year clock never restarts." Header `x-include-refresh-token-hard-expires-in: true` returns `x_refresh_token_hard_expires_in` |
| Access token 1 hour | same post's sample response shows `"expires_in": 3600` (no prose); Truto 2026-03-30: "QuickBooks Online designs the access token to be valid for only one hour." | | |
| 429 handling | Intuit blog via RSS "Retrying the Right Way: 4XX vs 5XX" | 2026-09-01 | "Retry, but only after honoring the Retry-After header or cooldown period the server gives you." (no numbers given) |

Rate limits (secondary only so far, Truto 2026-03-30): "QuickBooks Online enforces 500 requests per minute per company (realm ID) with a maximum of 10 concurrent requests per app." / "Batch operations are limited to 40 requests per minute, while resource-intensive endpoints drop to 200 requests per minute."

### QBO 3 — Plans and prices (US)

Source: https://quickbooks.intuit.com/pricing/ (accessed 2026-10-20, Intuit, primary).

| Plan (US) | List / promo | Users | Notes (verbatim where quoted) |
|---|---|---|---|
| Free | "$0/mo" | "1 user" | "Send 2 invoices per month"; **"Mobile app access and third-party integrations not currently available with QuickBooks Free."** |
| Simple Start | "$38" list; "$19/mo" with "50% off for 3 months*" | "1 user" | no class/location/inventory bullets |
| Essentials | "$85"; "$42.50/mo" promo | "3 users" | |
| Plus | "$140"; "$70/mo" promo | "5 users" | "Track and manage inventory"; "Track classes and locations (40)" |
| Advanced | "$340"; "$170/mo" promo | "25 users" | "unlimited classes, locations, and custom KPIs" |

**Implication:** a studio on QBO Free cannot connect any third-party app at all (US). Class and Location (Department) tracking, which a studio would use to split "classes vs memberships vs firing vs retail", needs Plus ($140/mo list). Inventory (quantity-on-hand Items) needs Plus.

### QBO 4 — Plans outside the US (accessed 2026-10-20)

| Country | Source | What loaded | Prices |
|---|---|---|---|
| CA | https://quickbooks.intuit.com/ca/pricing/ | Lite, EasyStart, Plus, Advanced (FAQ also mentions Essentials) | EasyStart "Was$30" → "$3/mo"; Plus "Was$110" → "$11/mo"; Advanced "Was$220" → "$22/mo"; Lite "Was$20" → "$2/mo" ("Save 90% for 6 months*"). CAD. Inventory + "Up to 40" classes/locations only on Plus/Advanced. No permanent free plan |
| UK | https://quickbooks.intuit.com/uk/pricing/ | Plans: Sole Trader Plus, Simple Start, Essentials, Plus, Advanced; prices did not load ("Loading live prices…") | **not captured** |
| AU | https://quickbooks.intuit.com/au/pricing/ | Simple Start, Essentials, Plus, Advanced; "Get 70% off QuickBooks for 6 months." | **not captured** |

UK and AU: inventory and classes/locations ("Up to 40") only on Plus and Advanced; every listed plan says "Integrate with hundreds of apps" (no Free-plan equivalent with integrations excluded was shown in CA/UK/AU).

How common QBO is outside the US: Intuit publishes no per-country subscriber split that I could open. Proxy = Xero's own counts:
- Xero FY25 (to 2025-03-31): "The company's subscriber base grew by 6% to 4.41 million" … ANZ "total subscribers reaching 2.6 million" … international "subscribers totalled 1.8 million" (CFOtech, 2025-05-19, trade press reporting Xero results) https://cfotech.news/story/xero-reports-nzd-2-1-billion-revenue-23-annual-growth
- Xero H1 FY26: "Total subscribers reached 4.6 million, up 10%, with 176,000 net additions in H1 FY26." (FilingReader, 2025-11-17) https://filingreader.com/news-wire/sydney/2025-11-17/xero-reports-strong-first-half-fy26-results-reiterates-fy28-aspirations
- Xero H1 FY26 regional split UK 1.21M, North America 419k, ANZ 2.7M: **UNVERIFIED** (search summary of investor-deck PDF; PDF undecodable).

### Xero 1 — Developer pricing (new model from 2026-03-02)

Source: https://developer.xero.com/pricing (Xero, primary; accessed 2026-10-20; AUD ex-tax).

| Plan | Max connections | Monthly fee (AUD) | Egress allotment | Overage | Daily calls per org |
|---|---|---|---|---|---|
| Starter | 5 | No charge | n/a | n/a | 1,000 |
| Core | 50 | $35 | 10 GB | $2.40/GB | 5,000 |
| Plus | 1,000 | $245 | 50 GB | $2.40/GB | 5,000 |
| Advanced | 10,000 | $1,445 | 250 GB | $2.40/GB | 5,000 |
| Enterprise | No limit | Price on Application | Volume on Application | POA | 5,000 |

Verbatim (pricing page): "App pricing is a flat monthly fee based on plan and includes a set allotment of data consumption via the Xero APIs." / "Additional data consumption is available at additional cost." / "We exclude the organisation endpoint from this usage allotment." / "Apps cannot share connection counts or API volume used, even if they are from the same developer." Ingress "Unlimited" on every plan. App Store listing "Not Applicable" for Starter and Core, "Optional" for Plus/Advanced, "Required" for Enterprise.

Verbatim (FAQ https://developer.xero.com/faq/pricing-and-policy-updates, undated, © 2026): "Xero will no longer be pricing based on a revenue-share model." / "Beginning on 2 March, 2026, we will be moving to a commercial tiering model based on connections and Xero API usage." / "The Starter tier has unlimited data access to enable early innovation but is capped at 5 connections." / "In order to add a 6th connection, you will need to first add your payment details." / "The Journals endpoint, Xero Practice Manager API, and Bulk Connections require a security assessment (initial and annual) and use case approval." / "Ensure all of your customers are transitioned by 1 July, 2026."

**Implication (arithmetic mine):** a free studio app connecting 51–1,000 Xero orgs pays A$245/mo (~A$2,940/yr) regardless of revenue; 1,001–10,000 orgs A$1,445/mo. Xero charges **per connection count** and **per GB read**, so cost scales with adoption, not revenue. For a free product, Xero is a fixed cost from the 6th studio. Contrast QBO Builder: $0 until 500k reads/month.

### Xero 2 — Developer terms (AI/ML, data use)

Source: https://developer.xero.com/xero-developer-platform-terms-conditions — "Last updated: 4 December 2025." Applies to pre-Dec-2025 developers "from 2 March 2026".

- 7(b)(i): "API Data cannot be used to train, fine tune, adapt, or enhance any AI Models."
- Intro: "You must not use API Data sourced from the Xero developer platform to train or fine tune any artificial intelligence" (AI Models defined to include ML tools, LLMs and predictive analytics tools; paraphrase of the definition).
- 7(b)(ii): "API Data shall not be passed to another third party without consent from the associated user."
- 29: "Aggregate and supply user data (or any other API Data accessed through the developer platform)" … "to another app or any third party." Also: "Download or scrape any of our websites, data or content in any way (including with bots or browser automation)."
- 7(a): "If you join, or we request that you transition into, a different app partner tier," … "different usage limits (and fees) might apply to you."
- 31: "We may terminate, suspend or limit your access to the developer platform" (immediate in listed cases incl. "we decide that we need to in order to prevent harm, material risk or liability to us or any other person,").
- 15: "take reasonable measures (which are at least industry standard) to protect user data." (incident reporting within 24 hours — paraphrase).

**Implication:** cross-studio benchmarking built from Xero data (e.g. "median firing revenue per member across studios") would need care under 7(b) and 29; any ML on Xero-sourced data is out. Using our own studio data (which we hold first-hand) is unaffected.

### Xero 3 — Auth, limits, webhooks (mostly secondary — docs pages are client-rendered and returned only titles)

- Tokens (Xero's own SDK README, GitHub, primary): "Bearer token with a 30 minute expiration required for all API calls"; refresh token "60 day expiry". https://github.com/XeroAPI/xero-node
- Daily limit per org: 1,000 (Starter) / 5,000 (others) — pricing page (primary).
- Per-minute and concurrency limits: **not captured from a primary page** (developer.xero.com/documentation/guides/oauth2/limits/ returned only its title).
- Webhooks (Truto, 2026-03-30, vendor blog): "Xero webhooks deliver thin payloads—they send you the resource ID and event type, not the full resource." / "Xero supports webhooks for Contacts, Invoices, and (in beta) Credit Notes," — **secondary**.
- Egress definition (Truto): "Data egress is defined as the total volume of data transferred out of Xero's systems via API GET responses."

### Xero 4 — Changelog (primary, https://developer.xero.com/changelog, accessed 2026-10-20)

- Granular scopes, 2026-03-04: "You can now request smaller, more specific scopes instead of the broad accounting.transactions" … "accounting.journals.read is no longer granted by default." (needs Advanced tier + certification).
- 2026-08-06: "Apps created on or after 2 March 2026 already use granular scopes." … "The accounting.transactions scope is replaced by accounting.invoices, accounting.payments," (+ accounting.banktransactions, accounting.manualjournals). Existing apps must migrate by 2027-09-13.
- Webhooks, 2026-03-04: "Xero has made the decision to introduce a new webhook schema model, with each event type having its own schema." New events: prepayments (2026-08-14), overpayments (2026-09-03).
- Inventory, 2026-08-10: "The Invoices endpoint now supports backorders for tracked inventory items." / "Item responses now include QuantityOnBackorder and QuantityAvailable" (backorders not supported in Canada or Singapore).
- Rate limits, 2024-09 (pre-2025 entry): "We will be enforcing high volume threshold limits to key endpoints in the Accounting API".
- 2026-10-15: remote MCP server "available to commercial apps on Core or above".

### QBO/Xero maintenance churn 2025–26 (one vendor article, with primary links; partly re-verified)

Apideck, "AI-built integrations: what broke in QuickBooks and Xero", 2026-10-16 (vendor blog) https://www.apideck.com/blog/ai-built-accounting-integrations-maintenance — lists, with links:
- QBO "since 1 August 2025, QuickBooks Online ignores any minor version below 75"
- QBO webhooks moved to CloudEvents: "first due by 15 May 2026 and then extended to 31 July 2026"; Intuit staff called intermittent delivery "a known active issue" (help.developer.intuit.com question — not reachable, **UNVERIFIED**)
- QBO refresh tokens: "The first hard expiries arrive in February 2027 for apps on granular or restricted scopes" (cites blogs.intuit.com/2025/11/12/important-changes-to-refresh-token-policy — not reachable, **UNVERIFIED** beyond the 2026-06-05 RSS post above)
- QBO "The Reports API moved to a new service by 31 August 2026" (**UNVERIFIED**)
- Xero scopes change — **verified** against the Xero changelog above.

**Practitioner, first-person, dated (verified):** Estuary (data-integration vendor) PR "source-quickbooks: switch to "bring your own prod app" model", merged 2026-07-08, https://github.com/estuary/connectors/pull/4780 — "the API queries we run use a metered CorePlus endpoint that consumed our requests budget as adoption grew." / "Once the monthly budget was exhausted, access token refreshes would 429 until the month rolled over." / "This commit switches to a new approach where users provide a client id, secret and refresh token for their own app." / "A single capture's API usage fits comfortably within the free tier." / "This is a breaking change: all users will be required to apply for a new app before they can re-enable their tasks."
→ **The QBO Builder cap is app-wide, not per company. A read-heavy multi-tenant app hit it and broke for every customer (US, 2026).** Bring-your-own-app is not a realistic answer for pottery-studio owners.

### QBO 5 — App assessment (production access)

- Codat integration docs (third-party, undated, accessed 2026-10-20) https://docs-integration.codat.io/integrations/accounting/quickbooksonline/qbo-app-assessment-questionnaire: "All apps that intend to access QuickBooks Online production data must fill in a questionnaire as part of Intuit's app assessment process." Sections: App details; Compliance (General, App information, Authorization and authentication, API usage, Accounting API, Error handling, Security). Example question: "Are the client ID and client secret for your app stored securely".
- Intuit's own statement that this applies "whether or not they are listed in the QuickBooks app store", that listed apps and apps with "over 500 connections" are reviewed annually, and that critical/high/medium findings must be fixed before publishing: **UNVERIFIED** (search summary of blogs.intuit.com/?p=33093, unreachable).
- Not found: any fee for the assessment; pen-test or SOC 2 requirement.

### QBO 6 — Webhooks and change data capture (secondary only)

- Truto (2026-03-30, vendor blog): a webhook tells you something changed, but "you must still make a CorePlus API call to fetch the updated invoice details." / "So webhooks alone aren't sufficient — you need a fallback polling mechanism, which means even more CorePlus API calls."
- Apideck (2026-10-16, vendor blog, fetched): QBO webhook payloads moved to CloudEvents, "first due by 15 May 2026 and then extended to 31 July 2026"; apps that don't migrate "will no longer be able to process webhooks notifications from Intuit". Not confirmed from an Intuit page.
- **UNVERIFIED (search summaries only):** Intuit's wording "The migration is mandatory and must be completed by May 15, 2026."; a sandbox/production format switch on the webhooks page; "One notification can now carry events for several companies."
- Webhook entity list, payload fields, signature header and the CDC lookback window: **not captured** (Intuit docs client-rendered; the Intuit webhooks sample README on GitHub doesn't state them — https://github.com/IntuitDeveloper/SampleApp-Webhooks-Java says only "Sample code using QuickBooks Online SDK to call CDC API to sync data between the app and the QuickBooks Online company.").
- **Implication:** on Builder, every webhook-triggered fetch spends the shared 500k read cap. Design the sync so QBO is written to and almost never read: keep our own mapping of IDs returned by writes.

### Square 1 — OAuth, webhooks, Bookings, Inventory (primary docs, undated pages "© 2026 Square, Inc.", accessed 2026-10-20)

- OAuth https://developer.squareup.com/docs/oauth-api/overview: "Square OAuth access tokens expire after 30 days." / "Refresh tokens obtained using the code flow don't expire." / "Refresh tokens obtained using the PKCE flow are single-use tokens and expire after 90 days." / "Webhook Subscriptions API and Events API endpoints require the developer's personal access token."
- Webhooks https://developer.squareup.com/docs/webhooks/overview: "Square resends the event notification for up to 24 hours after the originating event." / "After 24 hours, the notification is discarded and no further retry attempts are made." / "There's no guarantee of the delivery order of event notices." / "Webhooks can be sent more than once." (dedupe on `event_id`) / "In most cases, event notifications arrive in well under 60 seconds of the associated event."
- Bookings https://developer.squareup.com/docs/bookings-api/what-it-is: "A seller must have a subscription to Appointments Plus or Premium to use this feature." (seller-level writes; otherwise 403 "Merchant subscription does not support write operations."). No class/group-booking feature documented on that page. → **A studio app cannot write a Square booking unless the studio pays for Appointments Plus/Premium.**
- Inventory https://developer.squareup.com/docs/inventory-api/what-it-does: InventoryCount "Defines the calculated quantity of an item variation at a specific location with a specific inventory state." / "Inventory quantities can only be tracked on items of the CatalogItemVariation type." / "The Inventory API doesn't support the tracking of subcomponents, ingredients, or product bundling." → Square inventory covers **sellable retail stock** (pots, clay bags, tools for sale), not consumables recipes (glaze materials) or equipment/assets (kilns, wheels).
- Rate limits: no numbers found; the old URL developer.squareup.com/docs/build-basics/rate-limits returns 404.
- Orders, Payments, Catalog, Customers and Invoices APIs: their pages are linked from the docs index (https://developer.squareup.com/docs), but I did **not examine them in detail**. The only requirement text captured is from the 2024 listing changelog below, e.g. "Your app deducts refunds to accurately display daily sales amounts." (Orders), and "Upon initial sync, customer data is imported accurately into your app." (Customers).

### Square 2 — App Marketplace (partially unreachable)

- **All developer.squareup.com/docs/app-marketplace/* pages now return 404** (requirements, faq, rev-share, app-subscriptions/billing) — they appear in search indexes but were removed/moved by 2026-10-20.
- Requirements changelog (primary, 2024-08-23 — pre-2025 label: 2024) https://developer.squareup.com/docs/changelog/requirements/change-log/2024-08-23: "Square reviewers will test your app as if they're real Square sellers." / "Confirm that your app uses exponential backoff to manage rate-limit errors." / "Confirm that Square access tokens are AES-encrypted in your database." / "Confirm that your app has implemented logic to refresh OAuth tokens asynchronously every 7-14 days." / "Your app deducts refunds to accurately display daily sales amounts."
- Partner page (primary, undated, © 2026) https://squareup.com/us/en/partnerships/app-partner: "If you're building an app or integration for more than one seller, you could be a great fit for our partner program." / "We also want to ensure that there is a strong enough demand for your app." / "Create new revenue streams by sharing in profit generated through the app partnership." No % stated. Markets listed in the form: AU, CA, FR, IE, JP, GB, ES, US.
- Square staff on the developer forum, 2023-03-13 (pre-2025: 2023): "There isn't any documentation that describes revenue share agreements." / 2023-03-14: "Everything App Marketplace related will be discussed with the Square rep that reaches out." https://developer.squareup.com/forums/t/what-is-squares-cut-of-subscriptions-when-a-seller-buys-it-through-their-app-store/8840
- 20% revenue share on App Subscriptions, first year waived, US-only: **UNVERIFIED** (search summary of a now-404 page).
- **Listing is optional**: nothing found says an unlisted app can't use OAuth with any seller; the partner page says build first and "reach out once it's live".

### Square 3 — Square ↔ QuickBooks (who builds it, what users say)

- Square App Marketplace "QuickBooks Online" entry → "Square Connector by QuickBooks app", published by **Intuit**, US, "Listed since 2025-05-30, last updated 2026-08-04" (as summarised by WebFetch from https://squareup.com/us/en/app-marketplace/app/quickbooks-online; price not stated).
- QuickBooks Community (CA), thread "Square Connect set up does not allow daily sync with QB" https://quickbooks.intuit.com/learn-support/en-ca/other-questions/square-connect-set-up-does-not-allow-daily-sync-with-qb/01/1354490/highlight/true:
  - 2023-12-08, user (pre-2025): "I have too many individual transaction for QB/me to cope with."
  - 2024-05-14, user (pre-2025: 2024): "the Canadian version of this app (created by Intuit) does not allow for 'Daily Summary' integration"
  - 2024-05-14, original poster: "QBO froze up presumably because of an excess of data." (then switched to Commerce-Sync; HST problems)
  - 2024-05-14: "I'm gathering that there is NO free/cheap version of this app"
  - Intuit staff reply (2023-12-09) suggested clearing "browser cache and cookies"; no staff answer on the daily-summary limit.
- Square ↔ Xero: Square App Marketplace (GB) listing "Xero", links to xero.com (builder unclear there; the Xero App Store listing says "By Square", added 2020 — https://apps.xero.com/au/app/square); "Your daily transactions will be automatically imported, summarised, and populated to the app." Countries AU, CA, UK, IE, US; listed 2020-11-23, updated 2026-08-24 (WebFetch summary of https://squareup.com/gb/en/app-marketplace/app/xero).
- Subscriptions API (primary, https://developer.squareup.com/docs/subscriptions-api/overview): "The minimum amount you can charge for a subscription is $1." / "ACH payments aren't available through the Subscriptions API." / "Catalog items sold through subscriptions must be shipped to the customer."
- Gift Cards API (primary, https://developer.squareup.com/docs/gift-cards/using-gift-cards-api) — relevant because gift cards are the main Square use in the census (03): "Sellers in the following countries pay 2.5% of the amount added to a Square gift card" (AU, CA, US). / "There are no fees when a gift card is redeemed or refunded." / **"Developers cannot collect application fees for gift card payments."** / third-party cards need "a one-time process with Square Support to import". Webhooks `gift_card.activity.created`, `gift_card.updated`.

### Stripe 1 — Pricing (US, primary, https://stripe.com/pricing, accessed 2026-10-20)

Online domestic cards 2.9% + 30¢; in-person (Terminal) 2.7% + 5¢; Billing pay-as-you-go "0.7% of Billing volume" with "No recurring fees."; Invoicing Starter 0.4% per paid invoice; ACH Direct Debit 0.8% capped at $5.00. FAQ: "Stripe does not charge setup fees, monthly fees, or any other hidden fees like closure fees."

### Stripe 2 — Connect (primary)

- Pricing (US, https://stripe.com/connect/pricing): **"Stripe handles pricing for your users"** = "No fees for your platform"; Stripe collects processing fees from the connected account. **"You handle pricing"** = $2 per monthly active account + 0.25% + 25¢ per payout; 1099s $2.99/form. In both: "Stripe covers loss liability for these connected accounts." (for direct charges).
- Account types (https://docs.stripe.com/connect/accounts): Standard/Express/Custom are now marked **"Deprecated feature"** for new platforms ("applies only to platforms that already use legacy connected account types"); new platforms use Accounts v2 configurations. Table: Standard = "Full Dashboard", fraud/dispute liability "Connected account for direct charges", "Integration method: API or OAuth", support "Platform and Stripe"; Custom = no Dashboard, platform liable, support by "Platform". "There's an additional cost for using Express or Custom connected accounts." / "Extensions building on Connect must use OAuth to connect to Standard connected accounts." Standard is described as for "store builders, such as Shopify, and Software as a Service (SaaS) platforms".
- Accounts v2 SaaS guide (https://docs.stripe.com/connect/accounts-v2/saas-platform-payments-billing): "You can collect application fees from an `Account` with the Merchant configuration." Accounts v1 is still required for "Using OAuth to authenticate connected accounts".
- **Who owns the merchant relationship:** with a full-Dashboard (Standard-style) account and direct charges, the studio has its own Stripe account, contract, Dashboard and payouts, pays Stripe's fees directly, and carries its own disputes; the platform pays nothing and may add an application fee. That is the cheapest model for a free product.

### Stripe 3 — Accounting sync Stripe offers (primary)

- https://docs.stripe.com/accounting-integrations: Stripe's own export is CSV reports + a **QuickBooks Desktop** IIF file; "You can't import IIF-formatted files into QuickBooks Online. Use a third-party accounting integration that supports one-click imports of your Stripe activity." Stripe's first-party connector is for **NetSuite** ("Stripe Connector for NetSuite"), not QBO/Xero.
- https://docs.stripe.com/stripe-apps/embedded-apps/accounting-integrations: "Stripe provides integrations to Quickbooks Online and Xero accounting software through Connect embedded components for Stripe Apps built by 3rd party developers." QBO app = **Quickbooks Sync by Acodei** (syncs "Sales (payment) transactions, Fees, Payouts, Refunds, Invoices, Customers, Products, Taxes"; "Real-time (per transaction) or daily aggregate (for large volumes)"; off-Stripe cash/cheque via invoices paid out-of-band; "supports all Connect types and funds-flows"). Pricing per Stripe: "Free user tier", "Direct (self-serve) user billing or platform billing options which retroactively charge the Connect platform based on user activity". Xero app: "Xero is free for all users." Page still offers a **preview** sign-up ("Interested in the preview of embedded components for Apps?").
- **Conflict:** Acodei's own pricing page (https://www.acodei.com/pricing, © 2026) lists **no free plan**: Scale $12–100/mo for up to 100–5k transactions/month, Enterprise $150–350/mo, 14-day trial, card required.
- **Implication:** a Stripe-Connect-based studio app could embed QBO/Xero sync from a partner instead of building it — but the feature is preview-gated and the QBO side costs the studio (or us) $12+/mo.
- Stripe Apps review rules (https://docs.stripe.com/stripe-apps/review-requirements): "Clearly state pricing without hidden costs"; "You can't resell or publish any data obtained from Stripe users through your app." No revenue share mentioned.

### Stripe 4 — Terminal (primary, https://docs.stripe.com/terminal/overview)

"Use Terminal with Connect to support connected accounts." Options: readers, "Tap to Pay" on "compatible iPhone or Android devices", "Standalone mode (Public preview)", "The Stripe Terminal app (Private preview)". Country list not captured.

### Stripe 5 — Rate limits, webhooks, inventory (primary, accessed 2026-10-20)

- Rate limits (https://docs.stripe.com/rate-limits): global "100 requests per second" live / 25 sandbox, per Stripe account; individual endpoints 25/s; global concurrency "50 in-progress requests". **Read allocation:** "Your account's read API requests must not exceed an average of 500 per transaction." Connect platforms get "a separate allocation" of "500 requests per transaction based on the aggregate transaction count across its connected accounts"; "Every account, regardless of transaction count, has a minimum allocation of 10,000 read requests per month." / "Write API requests have no allocation limit." → like QBO, **reads are the scarce resource; webhooks + local copy is the intended pattern.**
- Webhooks (https://docs.stripe.com/webhooks): "Stripe attempts to deliver events to your destination for up to three days with an exponential back off in live mode." / "Stripe doesn't guarantee the delivery of events in the order that they're generated." / "Webhook endpoints might occasionally receive the same event more than once." Signature in `Stripe-Signature` (HMAC-SHA256). Connect: an event destination scoped to "Connected accounts" receives events "from resources that belong to your connected accounts" (e.g. direct charges, payout failures). Up to 16 event destinations.
- Inventory: the Product object (https://docs.stripe.com/api/products/object) has `id, active, default_price, description, images, marketing_features, metadata, name, package_dimensions, shippable, statement_descriptor, tax_code, tax_details, unit_label, url…` — **no stock or quantity field. Stripe has no inventory.**

### Shopify 1 — Distribution and revenue share (primary, shopify.dev, undated pages, accessed 2026-10-20)

- Distribution (https://shopify.dev/docs/apps/launch/distribution): Public = "Can be installed on multiple Shopify stores", Shopify review required. Custom = "Installed on a single Shopify store, on multiple stores that belong to the same Plus organization", no review, and "Can't charge merchants through Shopify's app billing system." / "You can't change the distribution method after you select it." Unpublished/unlisted public apps were deprecated 2019-12-09. → **A multi-tenant studio app must be a public App Store app** (custom distribution = one app per studio).
- Revenue share (https://shopify.dev/docs/apps/launch/distribution/revenue-share): "You keep 100% of your first $1,000,000 USD in lifetime gross app revenue, and 85% of earnings above that." / "The $1,000,000 USD threshold is a lifetime amount. It doesn't reset each year." One-time $19 USD registration fee. Only revenue from 2025-01-01 counts. (A free app pays nothing.)

### Shopify 2 — App Store requirements that bind a studio app (primary, https://shopify.dev/docs/apps/launch/shopify-app-store/app-store-requirements)

- 1.2.1: "Your app must use Shopify App Pricing or the Shopify Billing API for any app charges." 1.2: "Apps that use off-platform billing cannot be distributed through the Shopify App store"
- 2.2.4: "As of April 1, 2025 all new public apps must be built exclusively with the GraphQL Admin API." / "As of October 1, 2024 the REST Admin API is considered a legacy API and should no longer be used."
- **1.1.2:** "Apps that bypass checkout or payment processing, or register any transactions through the Shopify API" … "in connection with such activity, are prohibited."
- **1.1.8:** "Shopify is not currently accepting apps that connect to a POS system outside of Shopify."
- 3.2.1: "Request read_all_orders access scope only if it provides necessary app functionality." (That default order access covers only recent orders is my background knowledge — **UNVERIFIED** this run.)
- 1.1.1: "Use session tokens for authentication." / "Your embedded app must function properly without relying on third-party cookies or local storage."
- **Implication:** reading Shopify orders (classes/memberships sold as products) into the studio app and pushing retail stock levels back is allowed. **Writing studio-counter sales (taken via Square/Stripe) into Shopify as orders, or acting as a bridge to a non-Shopify POS, is what 1.1.2 and 1.1.8 prohibit for App Store apps.** Shopify should be a read-mostly source, not a sales ledger we write into.
- Protected customer data (https://shopify.dev/docs/apps/launch/protected-customer-data): Level 2 = "Customer data including name, address, phone, or email fields." Public apps must request it in the Partner Dashboard and "Participate in data protection reviews"; requirements include "Encrypt data at rest and in transit.", retention periods, "Limit staff access to protected customer data.", "Keep an access log to protected customer data." Custom apps: "Always available".

### Shopify 3 — API, webhooks, inventory (primary)

- GraphQL Admin API (https://shopify.dev/docs/api/admin-graphql, version 2026-10): "The GraphQL Admin API is rate-limited using calculated query costs, measured in cost points." Example shows `"maximumAvailable": 1000.0`, `"restoreRate": 50.0`; "single query max cost limit (1000)". Per-plan figures not on that page.
- Limits (https://shopify.dev/docs/api/usage/limits): "Input arguments that accept an array have a maximum size of 250, on every Shopify API." / "Pagination of arrays of objects caps at 25,000 objects" / "The recommended backoff time is one second."
- Webhooks (https://shopify.dev/docs/apps/build/webhooks): destinations "a URL, Google Pub/Sub URI, or Amazon EventBridge ARN"; "Shopify doesn't guarantee ordering within a topic, or across different topics for the same resource."; dedupe with `X-Shopify-Webhook-Id`; HMAC in `X-Shopify-Hmac-Sha256`.
- Inventory (https://shopify.dev/docs/apps/build/orders-fulfillment/inventory-management-apps): quantities per location by state (available, on_hand, committed, incoming, reserved, damaged, safety_stock, quality_control); "The `inventoryAdjustQuantities` mutation adjusts the inventory quantity"; "The `inventorySetQuantities` mutation explicitly sets the quantity of inventory"; audit via `referenceDocumentUri`; "You can't use the Admin API to adjust or move inventory quantities in the `committed` state." → Shopify inventory, like Square's, is **sellable product stock per location**, not equipment or consumables.

---

**Note (2026-10-20, mid-run):** the session's shared WebSearch budget ran out after ~17 searches by this agent. Everything below was found by fetching known or linked URLs directly (vendor sites, their help centres and marketplaces). No search-engine workaround was used.

### Adjacent vertical SaaS

#### Mindbody (fitness/wellness; US-first, global)

- Payments: **optional embedded**. Pricing FAQ (https://www.mindbodyonline.com/business/pricing, accessed 2026-10-20): "No, Mindbody does not require you to use its payment processing, Mindbody Payments, to run your business on the software." / "You can use Mindbody with a third-party payment processor of your choice." / "Some promotional offers include Mindbody Payments as part of the package." Starter "Starting at $79 USD/month per location". Fixed fee "$0.10 to $0.30" per transaction plus a rate (rate not stated).
- Accounting: **no native sync found; third-party marketplace.** The Financial Services category of Mindbody's marketplace (https://integrations.mindbodyonline.com/t/categories/financial-services) lists 16 partners, 3 of which sync to accounting: FlexBooks (QBO), Quote Stock Sell (QBO or Xero), Bookkeep (QBO, Xero, Zoho Books, NetSuite, Sage Intacct).
  - FlexBooks (https://integrations.mindbodyonline.com/partners/flexbooks): "automatically imports and categorizes your Mindbody data into QuickBooks Online each night while you sleep."; $65/mo (1 location), $120 (2), $150 (3); pricing "last updated" 2023-09-01 (pre-2025).
  - Quote Stock Sell (https://integrations.mindbodyonline.com/partners/quote-stock-sell): "From $33.00/ Month"; uploads "throughout the day"; splits income by "the QuickBooks Online classes and locations options" or Xero tracking categories; pricing last updated 2025-12-11.
- Limitation: the marketplace page does not say whether Mindbody has a native QBO feature; Mindbody's help centre (Salesforce) was not fetchable. Gap.

#### Pike13 (fitness, swim, music, enrichment; US)

- Payments: **embedded, choose from Pike13's processor partners.** https://www.pike13.com/features/billing-and-payments: "Pike13 integrates with Stripe, EVO, Paysafe, IntegraPay, and Celero for secure, automated payment processing." / "Select from multiple different merchant processors to fit your needs or switch as your business grows". Rates and countries not stated.
- Accounting: **no native QBO/Xero.** The integrations page (https://www.pike13.com/connections-and-integrations) lists 15 integrations; QuickBooks appears only via Zapier ("connect with platforms like Zoom, QuickBooks, and HubSpot"). The help centre's Reporting category (https://help.pike13.com/knowledge/reporting?hsLang=en) has 18 articles; 0 titles mention QuickBooks, Xero or accounting; exports are CSV ("How to Export Your Reporting Data").

#### Momence (boutique fitness, yoga, dance; now part of Xplor; US/UK/AU)

- Payments: **embedded Stripe** (help article "Add Multiple Stripe Accounts to Your Quickbooks Integration"; QBO transactions "are based on Stripe deposits"). Pricing page lists no prices ("Every business is different.") https://www.momence.com/pricing/
- Accounting: **native sync to QBO and Xero, sold as a paid add-on.** "Automated Booking keeping costs $85/month, is cheaper than a bookkeeper, and comes with a 30-day free trial period." (sic) / "It also integrates with Quickbooks and Xero if you use this as a system for accounting purposes." / it "reconciles each deposit that hits your bank account" — https://intercom.help/momence/en/articles/12029948-automated-bookkeeping-faqs ("Updated this week", accessed 2026-10-20).
- QBO article (2026-08-09) https://intercom.help/momence/en/articles/12996556-momence-quickbooks-integration: "Deposits are sent to QuickBooks per batch, rather than per individual booking." / transactions "are based on Stripe deposits and typically sync daily." / "a new QuickBooks customer must be created before the deposit is uploaded." / "Revenue categories can be mapped to QuickBooks accounts or classes for precise bookkeeping." / Limitation: "These account types are not currently supported in the integration." (holding/liability accounts such as "Payments in Transit").
- **Design lesson:** Momence syncs at the **payout/deposit** level (one deposit per Stripe payout, split by revenue category), not per booking — the same grain accountants reconcile against the bank.

#### Jackrabbit Class (youth classes: dance, gymnastics, swim; US/CA)

- Payments: **embedded, own brand.** "Jackrabbit Pay is built into Jackrabbit's class management software." (https://www.jackrabbitclass.com/features/billing-payment-processing/); "Process credit card payments and bank drafts for tuition and other associated fees from one secure platform" (https://www.jackrabbitclass.com/features/billing-payment-processing/epayments/). Underlying processor not named; no BYO option described.
- Accounting: **export to QuickBooks**, listed under *Payroll* in the partner marketplace: "Manage finances and staff payroll by integrating Jackrabbit with QuickBooks" (https://www.jackrabbitclass.com/partner-marketplace/). No Xero. Detail pages 404; Zendesk help centre 403. Mechanism (IIF/CSV, Online vs Desktop) not captured — gap.

#### Rock Gym Pro (climbing gyms; US/CA + intl)

- Payments: **embedded Stripe (Connect Custom) recommended, or OpenEdge.** Pricing FAQ (https://www.rockgympro.com/pricing, USD): "we recommend using RGP Stripe Custom, our integrated payment processing solution powered by Stripe." Footnotes: "Requires a Merchant Account with OpenEdge or Stripe for CC and ACH" / "OpenEdge is for US and Canadian facilities only." Plans Standard/Plus/Premium; cloud "$180 / $275 / $385" per month per facility (one of several price sets on the page — mapping unclear).
- Accounting: **export file** — table row "Export Sales/Payment Information to Quickbooks Desktop" (all plans). Integrations rows "QuickBooks *" and "Zapier *" with footnote "Integration requires an account with a third party application. Fees vary." → QBO = third-party connector. No Xero. Help centre (Zendesk) search 403.

#### Fabman (makerspaces; EU/AT-based, global)

- Payments: **bring your own Stripe account (or SEPA Direct Debit).** "To accept payments, you'll have to either connect your space to a Stripe account or enable SEPA Direct Debit payments." / "If you don't have a Stripe account, you can create one and connect it to Fabman in one go." (https://help.fabman.io/article/30-payment-setup, "Last updated on March 26, 2020" — pre-2025). Fabman also charges its own transaction fee: Basic "2.0%", Standard "1.0%", Plus "0.5%" on top of €49/99/199 per month (https://fabman.io/pricing).
- Accounting: **CSV export only.** "download them as a CSV (comma-separated values) file" (https://help.fabman.io/article/82-exporting-member-invoices, "Last updated on February 15, 2023" — pre-2025). 0 of 18 billing articles and 0 of 5 integration articles name Xero, QuickBooks, DATEV or Lexoffice. Open REST API on all plans.

#### Kiln Fire (pottery; US)

- Payments: **bring your own Stripe, Stripe only.** Home page (https://www.kilnfire.com/, accessed 2026-10-20): "We use Stripe to process credit cards and you will start and connect your own Stripe account with our app." / "Yes, our tool is 100% tied into Stripe's functionality and we cannot use any other billing providers." / "These prices do not include credit card processing fees." POS: "Using our external card reader hardware you can quickly collect in person credit card payments with the touch of a card." Kisi door integration.
- Accounting: **none found.** Docs index (https://kilnfire.com/docs) has ~70 entries; 0 mention QuickBooks, Xero, accounting or export. FAQ: membership discount codes "Yes, you can, but only in Stripe." (Stripe Dashboard is the de facto ledger.)
- Inventory (https://kilnfire.com/docs/inventory): "Keep track of inventory for your studio." Supply items; instructors can log class usage ("Instructors Can Take Inventory For Classes"). No retail stock/POS link described.

#### CourseStorm (class registration: community ed, arts centres; US)

- Payments: **embedded Stripe.** "Standard Stripe credit card processing costs and a $2.49 per registration fee apply to all plans." (https://www.coursestorm.com/pricing). Payments page: "built-in payment processor for simple setup and maintenance." / "No merchant accounts, weeks of paperwork, or tech teams needed." / payouts "daily, weekly, biweekly or monthly" (https://www.coursestorm.com/payments).
- Accounting: **GL mapping + payout breakdown export, no native QBO/Xero.** "Direct revenue from classes, materials, and add-ons to the right General Ledger accounts automatically." / each payout's breakdown is "organized to flow cleanly into your accounting software." "Accounting rules" only on Pro ($269–329/mo) and All Access ($399–499/mo); Zapier/API only on All Access. Integrations page names 10 apps (CRMs, email, WordPress, Zapier) — no accounting app.

#### Amilia SmartRec (recreation, municipalities, clubs; CA/US)

- Payments: **embedded, now Adyen.** "Adyen (the back-end processor supporting our new Amilia Payments solution)." Legacy merchant accounts on Paysafe (help collection "Merchant Account (Paysafe)"). **Gross payouts from 2026-10-01:** "an overwhelming number of you asked to move to Gross Payouts" citing "bank reconciliation, cash flow management"; "Processing fees are billed once a month on your Amilia invoice." (https://help.amilia.com/en/articles/16400170-amilia-payments-is-switching-from-net-to-gross-payouts, 2026-09-18 — vendor reporting customer feedback, not first-person).
- Accounting: **built-in general ledger + journal-entry reports, no named QBO/Xero connector.** Collection "Accounting and Finance Tab" has 23 articles incl. "Chart of accounts", "Cash accounting & journal entries", "Journal entries report (for accrual)", "How Revenue is Deferred and Recognized in SmartRec". 2026-08-26 article: "Through 2025 and 2026, we're reshaping SmartRec's accounting tools to give you more control, clarity, and flexibility." / "Journal entries were only available for accrual accounting." / "Ledger account imports are also in the works." 0 of 23 titles name QuickBooks/Xero/Sage/Acomba.

#### WellnessLiving (fitness, spa, salon; US/CA/UK/AU)

- Payments: **embedded via assigned processor partners** (https://help.wellnessliving.com/en/articles/8976663-merchant-processing, 2025-07-11): "WellnessLiving is associated with the following merchant processors and ACH providers:" US Paragon, Paysafe, Nuvei, PayArc; CA Paysafe, Nuvei; UK Barclays, Paysafe; AU/NZ Payrix, PayChoice. "Using your merchant processing integration with WellnessLiving requires paying fees to your processor." No Stripe/Square; BYO not described.
- Accounting: **native QBO sync as a paid add-on; no Xero.**
  - New "Finance by QuickBooks" (https://www.wellnessliving.com/features/finance/): "$39/month · Cancel anytime · Set up in 10 minutes." / "every sale, payment, and refund posts to QuickBooks each night, already categorized" / "Client account top-ups sit as liabilities until spent" / "Built for single-location and multi-location businesses, with QuickBooks Class/Location support" / "Anything without a specific mapping routes to a default account you pick during setup" / "Your QuickBooks Online subscription is separate and stays exactly as it is."
  - Legacy "Finance" (https://help.wellnessliving.com/en/articles/9274581-finance, "Updated over a month ago"): "WellnessLiving offers Finance at $19 USD per location per month" / "To use Finance, you'll need a QuickBooks Online Essentials, Plus, or Advanced subscription plan." / "Finance is only available to US and Canada businesses that are already connected to it." / "Daily auty-mations sync your All Sales Report directly to QuickBooks daily."

#### Summary table (10 vendors)

| Vendor | Segment | Payments model | Processors | Accounting | Price of accounting |
|---|---|---|---|---|---|
| Mindbody | fitness/wellness | Embedded optional; BYO allowed | Mindbody Payments or "third-party payment processor of your choice" | Third-party marketplace (FlexBooks, QSS, Bookkeep) | $33–65+/mo (partner) |
| Pike13 | fitness/enrichment | Embedded, pick a partner | Stripe, EVO, Paysafe, IntegraPay, Celero | CSV reports + Zapier | Zapier cost |
| Momence | boutique fitness | Embedded | Stripe | **Native** QBO + Xero, deposit-level, daily | $85/mo add-on |
| Jackrabbit | youth classes | Embedded, own brand | Jackrabbit Pay (processor unnamed) | QuickBooks export (listed under Payroll) | included? (not stated) |
| Rock Gym Pro | climbing | Embedded recommended | Stripe Custom; OpenEdge (US/CA) | QB **Desktop** export; QBO via third party | third-party fees |
| Fabman | makerspace | **BYO Stripe** or SEPA | Stripe | CSV invoice export; REST API | — |
| Kiln Fire | pottery | **BYO Stripe**, Stripe only | Stripe | none found | — |
| CourseStorm | class registration | Embedded | Stripe (+$2.49/registration) | GL rules + payout breakdown export (Pro+) | plan upgrade |
| Amilia | recreation | Embedded | Adyen (new), Paysafe (legacy) | Built-in GL / journal-entry reports | included |
| WellnessLiving | fitness/spa/salon | Embedded, assigned partner | Paragon, Paysafe, Nuvei, PayArc (US); Paysafe, Nuvei (CA); Barclays, Paysafe (UK); Payrix, PayChoice (AU/NZ) | **Native** QBO nightly; no Xero | $39/mo (new) or $19/location/mo (legacy) |

Counts (n = 10): payments embedded/platform-chosen 8, BYO Stripe 2 (Fabman, Kiln Fire). Stripe is a processor option at 6 (Pike13, Momence, RGP, Fabman, Kiln Fire, CourseStorm). Square is a processor at **0 of 10**. Accounting, one bucket per vendor (n = 10): native QBO sync 2 (Momence, WellnessLiving), **both sold as paid add-ons ($39–85/mo)**, and only Momence also does Xero; export file, CSV or built-in GL reports only 6 (Pike13, Jackrabbit, Rock Gym Pro, Fabman, CourseStorm, Amilia); third-party marketplace 1 (Mindbody); nothing found 1 (Kiln Fire).

### Third-party connectors a studio can already buy (vendor pricing pages, accessed 2026-10-20; vendor marketing)

| Connector | Price (USD) | What it does | Source |
|---|---|---|---|
| Synder | Basic $65/mo monthly ($52/mo yearly), 500 sales transactions/mo, 2 integration slots, daily import; Essential from $129/mo; Pro from $299/mo | Stripe, Square, Shopify, PayPal and "30+ integrations" → QBO, Xero; "Summary Sync" and "Per Transaction Sync" | https://synder.com/pricing/ (dateModified 2026-08-19) |
| A2X | from $29/mo per channel (Shopify, Amazon, Etsy, eBay, PayPal, TikTok Shop); Walmart $79 | Payout-level summaries to QBO, Xero, NetSuite. **Square and Stripe not listed** | https://www.a2xaccounting.com/pricing |
| Bookkeep | Lite $19, Starter $49, Growth $99, Pro $199, Advanced $499, Unlimited $1,199 per month; "All plans are priced per entity in USD or CAD currency only." | "Summarized journal entries for gross sales, discounts, refunds and sales tax." Sources incl. Square, Shopify, Stripe, Mindbody, Clover, Toast → QBO, Xero, Zoho Books (+ NetSuite, Sage Intacct at higher tiers). Historical entries $0.35 each | https://www.bookkeep.com/pricing |
| Zapier | Free: "Free includes 100 tasks/month.", "two-step Zap workflows", no premium apps; Pro $19.99/mo annual ($29.99 monthly) for 750 tasks; Team $69/mo annual | Event-by-event glue; whether QBO is "premium" not stated | https://zapier.com/pricing (last_updated 2026-10-20) |
| Make | Free: "If you are on the Free plan, you have 1000 credits per month." Paid prices not captured | Same role as Zapier | https://www.make.com/en/pricing |
| Acodei (Stripe → QBO) | Scale $12–100/mo by volume (100–5k tx/mo); Enterprise $150–350/mo | Real-time or daily summary | https://www.acodei.com/pricing |
| FlexBooks / QSS (Mindbody → QBO/Xero) | $65/mo; from $33/mo | nightly / intraday | Mindbody marketplace (above) |

**Read-across:** the cheapest automated sales→QBO path a US studio can buy today is ~$12–19/mo (Acodei for Stripe-only low volume; Bookkeep Lite) and the common one is $29–65/mo. Square→QBO via Intuit's own connector is free but users report it posts every transaction (CA, 2023–24).

### User voice on payment/accounting integrations

Access: **Reddit is blocked for WebFetch in this session** ("unable to fetch from www.reddit.com", also old.reddit.com), so no Reddit RSS this run. QuickBooks Community search URL 404; Intuit App Store pages are client-rendered ("Compiling and pre-filling your Intuit info..."); the Square Seller Community thread redirected to a login. Shopify App Store review pages and Xero App Store pages were readable; Xero App Store reviews showed **no dates**, so they are counted but not quoted as dated evidence.

**Dated, first-person (verified by re-opening):**

| Date | Country | Who | Product | Quote | Source |
|---|---|---|---|---|---|
| 2026-10-19 (edited) | not shown | Shopify merchant | Synder (Shopify/Stripe → QBO/Xero) | "Basic $65 plan has many essential features like full tax code mapping behind paywalls"; "God forbid you screw up and end up duplicating syncs" | https://apps.shopify.com/synder/reviews?sort_by=newest |
| 2026-07-03 | US | Shopify merchant (4 months) | Synder | "The setup to be too rigid, lacking the necessary flexibility for a smooth implementation" (sic) | same |
| 2026-06-15 | US | Shopify merchant (7+ yrs) | Bold QuickBooks Sync Pro | "My transfers were not working/not transferring into Quickbooks" | https://apps.shopify.com/quickbooks-online/reviews?sort_by=newest |
| 2026-05-06 | CA | Shopify merchant (~11 yrs) | Bold QuickBooks Sync Pro | "need assistance with syncing transactions. they are not syncing" | same |
| 2024-09-24 | US | Shopify merchant (8+ yrs) | Bold QuickBooks Sync Pro | 183 of 1,629 orders (11%) missing, multi-line orders cut to the first line, no processing fees captured (paraphrase); "Overall, I would not recommend the app until Bold makes the necessary improvements in reliability and accuracy." | same |
| 2024-05-14 | CA | QBO user | Square Connector (Intuit) | "the Canadian version of this app (created by Intuit) does not allow for 'Daily Summary' integration" | QuickBooks Community (above) |
| 2023-12-08 (pre-2024) | CA | QBO user | Square Connector (Intuit) | "I have too many individual transaction for QB/me to cope with." | same |
| 2026-07-08 | US | Developer (Estuary) | QBO API program | "the API queries we run use a metered CorePlus endpoint that consumed our requests budget as adoption grew." | https://github.com/estuary/connectors/pull/4780 |

**Counts:**
- Bold QuickBooks Sync Pro: 3.2 from 168 reviews; 1★ = 53 of 168 (32%). Newest 10 reviews: 3 report sync failures, 1 support non-response, 6 positive (5 of the 6 praise support for fixing a problem).
- Synder: 4.7 from 221 reviews; 1★ = 6 of 221 (3%). Newest 11 reviews: 1 pricing/paywall + duplicate syncs, 1 rigid setup, 1 mentions an extra marketplace fee, 8 positive.
- Square ↔ Xero (built by Square) on the Xero App Store: 4.2 from 108 reviews; 17% 1★, 74% 5★ (https://apps.xero.com/au/app/square/reviews). Of 6 undated reviews shown: 3 negative (can't find/add the integration; repeated connection problems), 3 positive (thanking a support agent for fixing integration issues).
- **No dated first-person complaints about the adjacent vertical SaaS products' own accounting integrations were reachable** (Capterra/G2/Trustpilot blocked; Reddit blocked; Intuit App Store client-rendered).
- Vendor-reported customer feedback (not first-person): Amilia, 2026-09-18, "an overwhelming number of you asked to move to Gross Payouts" because of "bank reconciliation, cash flow management".

---

## Synthesis

### Cost and gates for a free, multi-tenant studio app (US unless noted)

| Platform | What it costs us | Gate before real customers | What it costs the studio | Binding constraints |
|---|---|---|---|---|
| **Stripe Connect** | $0 in "Stripe handles pricing" mode; may add an application fee | Connect platform setup. Whether Stripe reviews the platform itself was **not checked**; App Marketplace review applies only to Stripe Apps | 2.9% + 30¢ online, 2.7% + 5¢ Terminal; Billing 0.7% if used | Reads ≤ 500 per transaction (min 10k/month per account); legacy Standard/Express/Custom deprecated for new platforms, but OAuth still needs Accounts v1; no inventory |
| **Square** | $0; listing optional | Partner approval + review only if listed on App Marketplace (docs now 404) | Square fees; **Appointments Plus/Premium for any booking writes**; 2.5% gift-card load fee (AU/CA/US) | Access token 30 days; listing rules ask for refresh every 7–14 days and AES-encrypted tokens; no app fee on gift-card payments; webhooks retried 24 h, unordered, duplicated |
| **QuickBooks Online** | $0 on Builder for writes; reads capped at 500k/month **across all studios**, then blocked; next step Silver $300/month | App assessment questionnaire for production access | Paid QBO plan (US list $38+; QBO Free can't connect apps); **Plus ($140 list) for classes, locations, inventory** | 1-hour access tokens, 100-day rolling refresh, 5-year hard expiry; webhook format migration and minor-version floor in 2025–26; one free-tier app (Estuary) broke at scale |
| **Xero** | $0 up to 5 orgs; then A$35 (≤50), A$245 (≤1,000), A$1,445 (≤10,000) per month + A$2.40/GB read over allotment | Security assessment + certification only for Advanced-tier endpoints (journals, bulk connections) | Studio's own Xero plan | **No AI/ML training on API data**; no passing data to third parties without consent; 1,000 (Starter) / 5,000 calls per org per day; granular scopes for apps created after 2026-03-02 |
| **Shopify** | $0 (free app; $19 one-time partner fee) | Public App Store review; protected-customer-data review for names/emails | none | GraphQL only; **can't register external transactions (1.1.2) or connect a non-Shopify POS (1.1.8)**; custom distribution = one store |

### Country tags for the platform data above

| Data point | US | CA | UK | AU | NZ |
|---|---|---|---|---|---|
| QBO plan prices | captured (USD) | captured (CAD, promo) | plans only, prices didn't load | plans only, prices didn't load | not checked |
| QBO App Partner Program | live 2025-07-28 | from 2025-11-03 (UNVERIFIED), excluding Quebec | same (UNVERIFIED) | same (UNVERIFIED) | not stated |
| Xero developer pricing | priced in AUD ex-tax; no country split shown | ← | ← | ← | ← |
| Square gift-card load fee 2.5% | yes | yes | not listed | yes | n/a |
| Square partner-form markets | yes | yes | yes (GB) | yes | not listed |
| Stripe and Connect prices | captured (USD) | not captured | not captured | not captured | not captured |
| Stripe Express/Custom account countries | listed | listed | listed (GB) | listed | listed |
| Shopify App Store rules and revenue share | no country split shown (thresholds in USD) | ← | ← | ← | ← |
| WellnessLiving processors | Paragon, Paysafe, Nuvei, PayArc | Paysafe, Nuvei | Barclays, Paysafe | Payrix, PayChoice | Payrix, PayChoice |
| Rock Gym Pro OpenEdge | yes | yes | no | no | no |
| WellnessLiving legacy QBO "Finance" | yes | yes | no | no | no |

### QBO entities a studio sync would touch (analysis; plan limits verified only where marked)

| Entity | Studio use | Write? | Plan limit |
|---|---|---|---|
| SalesReceipt | Paid-at-the-time sales (firing at drop-off/pickup, retail, drop-in) — per payout summary or per sale | Yes (Core) | none found |
| Invoice + Payment | Monthly member statements (firing billed monthly at Berkeley, Kilnfolk; 03) | Yes (Core) | none found |
| RefundReceipt | Refunds | Yes | none found |
| Deposit | One per processor payout: receipts from Undeposited Funds, minus processor fees | Yes | none found |
| JournalEntry | Alternative to receipts: one summarised entry per day or payout (Bookkeep's model) | Optional | none found |
| Item | Service items: membership, class, firing fee, studio time; non-inventory retail | Create once, then reference | **Inventory-type items need Plus/Advanced** (verified) |
| Account | Map income, clearing and fee accounts | Read once (CorePlus) | — |
| Class | Revenue line: classes / memberships / firing / retail | Reference | **Plus/Advanced only** (verified) |
| Department (Location) | Multi-site studios | Reference | **Plus/Advanced only** (verified) |
| TaxCode | Sales tax / GST / VAT per line | Read | country-specific; US automated sales tax behaviour not verified |
| Customer | Per-member, or one "Studio sales" placeholder for summaries | Optional | — |
| Vendor/Bill | Supplier invoices (clay, glaze) | Not in phase 1 | not verified |

Other evidence: WellnessLiving's legacy sync "need[s] a QuickBooks Online Essentials, Plus, or Advanced subscription plan", which excludes Simple Start. It doesn't say why. Momence posts "per batch, rather than per individual booking" and has to create a customer before uploading a deposit.

**Xero equivalents** (scope names verified in the Xero changelog; object names are standard Xero API names, not re-verified one by one): Invoices (ACCREC = sales invoice, ACCPAY = bill) · Payments · BankTransactions (RECEIVE ≈ SalesReceipt) · CreditNotes (≈ RefundReceipt) · ManualJournals (≈ JournalEntry) · Items (tracked inventory optional; QuantityAvailable added 2026-08) · Accounts · TrackingCategories (≈ Class/Location) · TaxRates · Contacts. Scopes for new apps: `accounting.invoices`, `accounting.payments`, `accounting.banktransactions`, `accounting.manualjournals`.

### Inventory capability by platform

| Platform | Inventory model | Fit for a studio |
|---|---|---|
| QBO | Inventory items with quantity on hand, **Plus/Advanced only** | Retail stock for studios on Plus |
| Xero | Tracked inventory items; backorders + QuantityAvailable (2026-08; not CA/SG) | Retail stock |
| Square | Count per item variation per location per state; "doesn't support the tracking of subcomponents, ingredients, or product bundling" | Retail pots, clay bags for sale; not glaze recipes or equipment |
| Shopify | Quantities per location in named states; set/adjust/move mutations; `committed` not adjustable | Retail stock (online shop) |
| Stripe | None (no quantity field on Product) | — |

No platform models **equipment or assets** (kilns, wheels) or **consumables by recipe**. Those stay in our own data model whatever we integrate.

---

## Hypothesis evidence

Ratings refer to whether the evidence supports doing this **as part of the first phase**. Where a count draws on the earlier census, it says so (03 = [03-tools-in-use.md](../../03-tools-in-use.md)).

### 1. Inventory tracking — **weak**

- **For:** 4 of 5 platforms have APIs for stock you sell (QBO, Xero, Square, Shopify; Stripe has none), so syncing retail stock is technically possible. The only pottery tool studios actually use, Kiln Fire (3 of 84 studios in 03), has a supplies inventory that lets instructors log what a class uses (1 of 1 pottery vendor checked).
- **Against:**
  - None of the 5 platforms models consumables by recipe. Square says outright that it "doesn't support the tracking of subcomponents, ingredients, or product bundling".
  - QBO inventory needs Plus ($140/month list, US).
  - Shopify's 1.1.2 and 1.1.8 rules block recording studio-counter sales in Shopify, so a two-way stock sync with a Shopify shop is constrained.
  - This run found 0 dated statements from studios asking for inventory sync (Reddit was blocked). In 03, Sortly and Craftybase were recommended by a 2024 magazine article, but no studio in 03's census or back-office sweep was found using them.
- **Not assessed:** inventory features at 9 of the 10 adjacent vendors.

### 2. Asset and equipment tracking — **weak**

- **For:** 2 of 10 adjacent vendors are built around equipment:
  - Fabman prices per connected machine ("€14 / mo." on Basic) and charges for machine time.
  - Kiln Fire has "Kiln Firings", "Reservable Items" and "Bookable Hours".
- **Integration angle:** 0 of 5 platforms track equipment. This is a feature to build ourselves, not an integration, so there's no platform risk.
- **Against or missing:** this run gathered no demand evidence (no reachable user voice). 03 has one member quote about reserving wheels.

### 3. Sales integrations with several payment providers — **contradicts**

The evidence contradicts *several providers in phase 1*. It moderately supports *one provider behind an interface*.

- **Adjacent vendors (n = 10):**
  - 0 of 10 document a business running two processors at once.
  - 8 of 10 embed or assign a processor. Pike13 offers a choice of 5 but expects you to "switch as your business grows". WellnessLiving assigns processors by country. Rock Gym Pro offers Stripe or OpenEdge.
  - 2 of 10 are bring-your-own-Stripe only: Fabman, and Kiln Fire ("we cannot use any other billing providers").
  - **Square is a processor at 0 of 10; Stripe at 6 of 10.**
- **Studios (03):** Square appears at 7 of 84, mostly for gift cards. Stripe sits behind Kiln Fire, BKLYN CLAY's custom app and several WooCommerce sites. A prospective owner asked Kiln Fire's founder "Does it integrate with other POS?" (2025-05-24, in 03). That is a request to **connect** an existing POS, not to support many processors.
- **Cost of each extra provider (primary docs):**
  - Square: tokens last 30 days; listing rules ask for a refresh every 7–14 days and AES-encrypted tokens; webhooks are unordered, can duplicate and retry for 24 hours; Bookings writes need a paid Appointments plan; no app fees on gift-card payments.
  - Stripe: webhooks retry for 3 days, unordered and possibly duplicated; reads are capped at 500 per transaction.
  - Both need payout reconciliation logic of their own.
- **Cost to us:** Stripe Connect in "Stripe handles pricing" mode costs the platform $0. Square OAuth costs $0. Neither is a blocker; the cost is engineering and maintenance.

### 4. QuickBooks Online integration — **weak**

The evidence is weak for a native two-way sync in phase 1. It is moderate for a payout-level export the accountant can import, or for an embedded partner app.

- **For:**
  - On the free Builder tier, writes are unmetered, so a write-only sync costs $0.
  - 2 of 10 adjacent vendors built native QBO sync, Momence and WellnessLiving. **Both charge for it** ($85/month and $39/month), which shows customers will pay.
  - QBO sells integration-capable plans in the US, CA, UK and AU.
- **Against:**
  - **6 of 10 adjacent vendors offer only an export or built-in GL reports**, and 1 of 10 has nothing: Kiln Fire, the pottery incumbent.
  - 03 found QuickBooks named by 1 potter (pre-2024) and Xero by 0.
  - The **Builder read cap is shared across all customers**. Estuary's connector hit it "as adoption grew" and had to make every user register their own Intuit app (2026-07-08).
  - **QBO Free (US) blocks all third-party integrations.** Class and location tracking need Plus.
  - **Sync complaints:**
    - Bold QuickBooks Sync Pro: 53 of 168 reviews are 1★ (32%), and 3 of its 10 newest reviews report failed syncs (2024-09 to 2026-06, US/CA).
    - Intuit's Square connector: 2 Canadian users (2023-12, 2024-05) were swamped by per-transaction posting. One wrote "QBO froze up".
  - **2025–26 churn:**
    - Minor versions below 75 are ignored from 2025-08-01 (per Apideck; Intuit page unreachable).
    - Webhooks moved to CloudEvents (deadline 2026-05-15, later 2026-07-31, per Apideck; not confirmed from Intuit).
    - Refresh tokens now have a 5-year hard expiry (Intuit, primary via RSS).
    - The Reports API moved service (per Apideck only; UNVERIFIED).
  - Studios can already buy the sync: Square→QBO free from Intuit; Stripe→QBO from $12/month (Acodei); Bookkeep from $19/month; Synder $65/month.

### 5. Scheduling — **moderate**

The split matters: support is moderate for **kiln, wheel and equipment reservations tied to pieces**, and weak for **class booking**.

- **For:**
  - 8 of 10 adjacent vendors' pages fetched in this run show booking, registration or reservations as core: Momence, Pike13, Jackrabbit, CourseStorm, Amilia, WellnessLiving, Fabman, Kiln Fire. Mindbody's and Rock Gym Pro's pages that I read didn't cover it, so they aren't counted.
  - Kiln Fire treats kiln firings and reservable items as first-class.
- **Against class booking:**
  - Through integrations it costs the studio extra: Square's Bookings API writes need Appointments Plus/Premium.
  - 03 found studios already book classes through storefronts (33 of 84), schedulers (11) and registration tools (11), and recommended not replacing them.
- **Not assessed:** Shopify booking apps.

## Other strong signals

1. **Every platform now meters reads, not writes.**
   - QBO: CorePlus reads are capped or billed; Core writes are free.
   - Xero: billed by connections plus GB read; ingress is "Unlimited".
   - Stripe: "read API requests must not exceed an average of 500 per transaction"; "Write API requests have no allocation limit."

   Design implication: push records out, react to webhooks, keep our own ledger, and never poll.
2. **The reconciliation grain the industry settles on is the payout or deposit.**
   - Momence: "Deposits are sent to QuickBooks per batch, rather than per individual booking."
   - Amilia moved to gross payouts on 2026-10-01 after customers asked about "bank reconciliation".
   - Bookkeep posts "Summarized journal entries".
   - Square→Xero posts daily summaries.
   - Canadian QBO users were overwhelmed by per-transaction posting.

   Model each payment-processor payout as a first-class record from day one, whatever accounting target comes later.
3. **Buy-not-build exists for accounting on Stripe.** Stripe lists embedded QuickBooks (Acodei) and Xero ("free for all users") apps that a Connect platform can surface to its connected accounts. Caveats:
   - the page still offers a **preview** sign-up;
   - Acodei's own page shows no free plan ($12+/month), which contradicts Stripe's "Free user tier".
4. **Bring-your-own Stripe is what the pottery incumbent does, and it costs a platform $0.** Kiln Fire (pottery) and Fabman (makerspaces) both have the studio connect its own Stripe account, and 03 found Stripe behind BKLYN CLAY's custom firing app. Connect's "Stripe handles pricing" mode charges the platform nothing, and the studio keeps its Dashboard, contract and disputes. Stripe has deprecated legacy account types for new platforms, so new builds should use Accounts v2, which can still take application fees. OAuth still requires Accounts v1.
5. **Xero is hostile to a free product beyond 5 studios.** It costs A$35/month for up to 50 orgs and A$245/month for up to 1,000, regardless of revenue. Its terms also ban using API data to "train, fine tune, adapt, or enhance any AI Models", and ban passing data to third parties without the user's consent. This matters most in AU and NZ (Xero reported 2.6 million ANZ subscribers at FY25) and the UK (1.21 million at H1 FY26, UNVERIFIED).
6. **Shopify can be read from, not written into.** Reading orders (classes and memberships sold as products) and setting stock levels is fine. Recording studio-counter sales in Shopify (1.1.2) or bridging to a non-Shopify POS (1.1.8) is not allowed for App Store apps. Reading customer names or emails triggers Level 2 protected-data requirements and review.
7. **Square specifics that matter for studios.**
   - The census shows gift cards are the main Square use. Studios in AU, CA and the US pay 2.5% on gift-card loads, and "Developers cannot collect application fees for gift card payments."
   - The App Marketplace documentation now returns 404.
   - Revenue-share terms are opaque: in 2023 a Square staff member said it was negotiated with a rep, and the 20% figure is UNVERIFIED.
8. **Integration maintenance is real and recurring.** In the last ~15 months:
   - QBO: refresh-token hard expiry (primary); minor-version floor, webhook format change, Reports API move (per Apideck).
   - Xero: new pricing model, new terms, granular scopes (existing apps must migrate by 2027-09-13).
   - Stripe: account types deprecated for new platforms.
   - Square: marketplace docs moved.

   Each integration is a standing cost for a free product.
9. **Accounting sync is a paid add-on wherever vendors built it natively** (2 of 2: $39 and $85/month). That suggests a real support cost. It also means a free, reliable sync would undercut those vendors (my inference). Only attempt it once the payout ledger is solid.

## Gaps and limits

- **Search budget:** the session's shared WebSearch budget ran out after about 17 searches by this agent. Everything after that came from direct fetches of known or linked URLs. Some discovery (Mindbody's native QBO status, Jackrabbit's export format, Shopify booking apps, more QuickBooks Community threads) was therefore not possible.
- **Reddit was blocked** for WebFetch this session (www and old.reddit.com), so there was no Reddit RSS voice. Capterra, G2 and Trustpilot were not attempted, as instructed.
- **Intuit primary sources were unreachable:**
  - developer.intuit.com docs and the Partner FAQ are client-rendered;
  - help.developer.intuit.com (Salesforce) showed "CSS Error";
  - the Intuit blog redirects to Medium (403);
  - Business Wire returned 403; the investors.intuit.com PDF couldn't be decoded.
  - **App Partner Program tiers, fees and caps therefore rest on secondary sources:** Woodard (trade press), Apideck and Truto (vendor blogs). Gold's 10M credits and the 500/3,000 minimum active connections come from secondary sources only. The UK/AU/CA expansion date (2025-11-03) is UNVERIFIED.
  - Medium's RSS feed was readable but covers only the 10 newest posts (2026-05 to 2026-10).
- **Not verified from primary sources:**
  - QBO: webhook entity list and payload, CDC lookback window, rate and concurrency limits (secondary: 500 requests/min per realm, 10 concurrent), plan limits on Bills and Vendors, and US automated sales tax behaviour through the API.
  - Xero: per-minute and concurrency limits, webhook coverage (secondary: Contacts, Invoices, Credit Notes), and the certification checklist. Those docs pages returned titles only.
- **Square:** App Marketplace requirements, FAQ, revenue share and App Subscriptions pages return 404, so listing fees are unknown (20% UNVERIFIED). No rate-limit numbers were found.
- **Prices not captured:** QBO in the UK and AU (prices load live); Make's paid plans; Stripe Terminal country list. Xero's regional subscriber split is only in an undecodable PDF (UNVERIFIED: UK 1.21M, North America 419k). **How common QBO is in UK, CA and AU remains unquantified.**
- **User voice is thin and skewed to e-commerce.** Dated first-person complaints came from Shopify App Store reviews of QBO connectors (Bold, Synder), one QuickBooks Community thread from Canada, and one developer PR. Xero App Store reviews had no dates. Intuit App Store reviews were unreachable. **No dated first-person complaints about the 10 adjacent vendors' own payment or accounting integrations were reachable.**
- **Vendor pages are marketing:** Mindbody, Pike13, Momence, Jackrabbit, Rock Gym Pro, Fabman, Kiln Fire, CourseStorm, Amilia and WellnessLiving pages describe features, not limitations. Two help articles are pre-2025: Fabman's payment setup (2020) and invoice export (2023). FlexBooks pricing was last updated 2023-09-01.
- **Two conflicts flagged, not resolved:**
  - Stripe says Acodei has a "Free user tier"; Acodei's page lists none.
  - Square's GB marketplace page suggests Xero builds the Square↔Xero app; the Xero App Store says "By Square".
- **Currency and country:** Xero developer prices are AUD ex-tax. QBO, Stripe, Shopify and connector prices are USD (US pages) unless marked. All prices are list prices on 2026-10-20 and may be promotional; QBO US shows "50% off for 3 months" and CA "Save 90% for 6 months".
