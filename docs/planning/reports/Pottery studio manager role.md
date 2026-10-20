# Pottery studio manager role: research report

_A synthesis of the ten research notes and the verification log in the [notes folder](../research_notes/Pottery%20studio%20manager%20role/), building on [01], [02] and [03]. Written 2026-10-20 for the product owner._

The owner's principle: "The important principle is that this is to help a studio manager, or stand in for one, in a pottery studio." Supporting several payment providers ("is very normal") and QuickBooks Online ("QBO is another integration I will need") are stated requirements. For those two, this report describes what the evidence says about demand and shape; it does not overrule them.

> **Method**
>
> | Examined | Count | File |
> |---|---|---|
> | Manager and coordinator job postings, opened and coded | 39 (US 37, UK 1, CA 1); 28 from 2023–26 or live | [MGR] |
> | Ceramics-technician job postings | 40 from 35 employers (US 35, UK 3, CA 2) | [TECH] |
> | Owner and manager quotes | 97 coded entries (90 verbatim-verified) from 34 independent studios, plus one hired manager's 20 consecutive board posts | [VOICE] |
> | Studio websites audited on a 12-item checklist | 84 (US 42, CA 12, UK 12, AU 18); 80 reachable | [AUD-US], [AUD-INT] |
> | Filings | Form 990s of 24 US organisations; annual returns of 13 NZ potters' clubs | [ORG] |
> | First-person bookkeeping posts | 55 independent posters, 2022–26 (at most 2 in ceramics) | [MONEY] |
> | Vendors | 5 platforms (QBO, Xero, Square, Stripe, Shopify), 10 adjacent vertical-SaaS vendors, 7 accounting connectors | [INT] |
> | Scheduling and labour pages | ~53 pottery organisations; 26 labour programmes | [SCHED] |
> | Kiln, supply and regulation sources | 1 readable kiln maker, 5 controllers, 8 supplier pages, 8 equipment-failure events | [INV] |
> | **Verification** | **12 of 12 sampled quotes confirmed. Of 20 decision claims: 15 confirmed, 5 partly confirmed, 0 wrong** | [VER] |
>
> Sources were accessed 2026-10-20 (03's census on 2026-10-19). Where the verification log conflicts with a note, this report follows the log.

## Bottom line

- **Who manages depends on the model.** For-profit studios are mostly owner-run: only 31 of 84 audited sites name anyone else as running operations. Nonprofits pay a director (9 of 9 clay-centre 990s; median $110,235) above a studio manager and part-time technicians. Co-ops have no manager: 4 of 5 US guilds and 8 of 13 NZ clubs pay no staff.
- **The job is the physical studio and its people:** cleaning and safety (32 of 39 manager postings), inventory and ordering (30 of 39; 30 of 40 technician postings), supervision (28 of 39), equipment repair (27 of 39; 38 of 40), kiln firing (24 of 39; 32 of 40). Software is rarely named: 23 of 39 manager postings name none, and 0 of 80 reachable studio sites name an accounting tool.
- **What hurts, in owners' words:** money (19 of 34 sources) and admin overload (16), then equipment breakdowns (10) and staffing (9). Operationally, members can't see firing status (6 of 84 sites publish any), and a hired manager is a single point of failure.
- **Inventory and asset tracking:** near-universal work with little tool use or stated pain. Inference: build clay and supply sales with par levels and purchase requests, and a kiln maintenance log counted in firings, not general inventory or an asset register.
- **Several payment providers (requirement):** several *payout sources* are common (21 of 84 studios take money through two or more tools). Several *card processors* for the same sales are not: 0 of 10 vendor pages document it. Inference: one charging provider behind an interface, plus payout import and reconciliation for the rest.
- **QBO (requirement):** QuickBooks is the only ledger pottery sources name, but rarely (1 of 39 manager postings, from 2020). Intuit's rules favour webhook-driven, write-mostly, summary-level posting under a read cap shared by every connected studio. Xero is at least as likely outside the US, so build on a ledger-neutral export.
- **Scheduling:** strong, but it is four jobs. The unserved ones are the kiln queue and firing calendar, staff and volunteer rosters, and training gates. Class booking already runs on commodity tools at 55 of 84 studios.
- **Direction (inference):** keep 02's piece → kiln → pickup loop; add a studio calendar with rosters and a handover view, and a prepaid and payout ledger whose summary export feeds QBO; swap Wave 1's invoice OCR for a supplies ledger.

## Who they are

| Model | How common | Who manages | Pay and hours |
|---|---|---|---|
| Owner as manager | 4 US sites show only the owner; 3 international sites say the owner runs it; only 31 of 84 sites name anyone else as running operations (US 19 of 42, international 12 of 42) [AUD-US], [AUD-INT] | The owner or founders. The Digs' two directors work 40 h/wk each for about 100 artists | The Digs: $47,530 and $45,200 [ORG]. Elsewhere unknown: 0 of 34 owner sources give hours [VOICE] |
| Hired manager, for-profit | 2 of 39 postings, both PYOP, imply the owner as boss [MGR]; managers named at BKLYN CLAY, Gasworks, Mud Studio, Radius, The Kiln Rooms, Parkdale [AUD-US], [AUD-INT] | Studio, retail or general manager | thrown elements $49,040–$59,500; Clay With Me coordinator $19/h [MGR], [AUD-INT] |
| Nonprofit staff | 22 of 39 manager postings; a paid executive in 9 of 9 clay-centre 990s [MGR], [ORG] | Executive director over a studio manager, technicians and mostly part-time teachers; 7–38 W-2 staff, median 19 | Directors: median $110,235, range $55,869–$158,534 [ORG] |
| Co-op, guild, club | 4 of 5 US guilds and 8 of 13 NZ clubs pay no staff; 7 of 24 US organisations list no paid executive [ORG] | Volunteer chairs: kiln (2 of 5 guilds), purchasing (1), maintenance (1), membership (3), classes (4), treasurer (5) | Unpaid; treasurers log the most hours where officers differ (8 and 12 h/wk) [ORG] |
| Technician | 40 postings: education 20, municipal 11, studios and art centres 9 [TECH] | Kilns, materials, equipment | 28 of 40 part-time, median 19.5 h/wk (n=19); US hourly median $20.00 (n=26); 10 of 40 temporary or contract |

- **Manager pay and reporting (all 39 postings).** Full-time US salaries have a median midpoint of $52,135 (n=10) and hourly roles $22 (n=11); 14 of 39 are part-time, median 20 h/wk. 11 report to a programme, education, artistic or ceramics director, 10 to the top of the organisation, 7 to another manager, 11 don't say, and none to a board [MGR].
- **Co-op duties are rules.** Merrill Pottery Co-op requires five work hours a quarter or members are "asked to leave without a refund" [VER] #17. Stanford Ceramics caps membership at 45 partly because of "the limited number of volunteer hours we have available"; Hawaii Potters' Guild runs 12 volunteer work groups [SCHED], [VOICE]. At Ilium Ceramics, when nobody else would take on the lease, a newcomer "ended up falling into it" and is now "spending most of my time running the studio now, instead of actually doing ceramics!" [VER] S5.
- **Teams are small and partly unpaid.** 14 of 39 manager postings supervise volunteers, students or interns; paid teams, where counted, are 2–3 technicians [MGR].
- **Owners keep strategy.** One PYOP founder keeps "the ideas, relationships, and long-term vision" and wants help to become "less owner-dependent"; in 5 of 39 postings, purchases or repairs need someone else's approval or partnership [MGR].

**Single point of failure.** The Fitton Center's hired manager posted 20 consecutive board updates (2025-08 to 2026-06): 8 schedule or closure notices, 7 supply notices (4 reporting a stock-out or shortage), 4 firing updates, 3 sale sign-ups and 2 cover notices [VOICE]. Only one absence caused a stated firing delay: a forgotten medication on 2025-10-07 ("So, I was not able to load the kilns.") ([Fitton][fitton-delay]). A broken ankle (weeks, spring 2025) and a trip (2026-01-15 to 01-24) were covered by instructors with no delay attributed, and the June 2025 delay was a thermocouple fault [VER] #15. Inference: absences are survivable when someone covers; the risk is what the cover doesn't know.

## What they do and produce

| Duty | Manager postings (n=39) | Technician postings (n=40) |
|---|---|---|
| Cleaning, facility, safety | 32 (82%) | specific safety tasks 21 (53%) |
| Inventory, ordering, purchasing | 30 (77%) | 30 (75%) |
| Supervising staff, volunteers, students | 28 (72%) | 21 (53%) |
| Equipment maintenance and repair | 27 (69%) | 38 (95%) |
| Kiln firing | 24 (62%) | 32 (80%) |
| Scheduling of any kind | 32 (82%) | 15 (38%) |
| Classes and programmes | 23 (59%) | teaching 2 (5%) |
| Events, parties, rentals | 20 (51%) | — |
| Glaze mixing; clay and reclaim | 15 (38%); 13 (33%) | 29 (73%) combined |
| Finance | 17 (44%): budgets 13, bookkeeping 6 | budgets 6 (15%) |
| Retail and point of sale | 14 (36%) | 8 (20%) |
| Tracking members' pieces | 7 (18%) | 10 (25%) |
| Equipment records | 6 (15%) | maintenance logs 2 |

Sources: [MGR], [TECH].

| What the 84 studio sites state [AUD-US], [AUD-INT] | US /42 | International /42 | All /84 |
|---|---|---|---|
| Only staff load kilns | 8 | 21 | 29 |
| Firing turnaround or frequency | 14 | 25 | 39 |
| Firing queue or status published | 4 | 2 | 6 |
| Gift cards | 20 | 33 | 53 |
| Parties or private events | 21 | 31 | 52 |
| Space, kiln or equipment rental | 17 | 18 | 35 |
| Open-studio reservations (some users) | 9 | 16 | 25 |
| Waitlist | 11 | 6 | 17 |
| Accounting tool named | 0 | 0 | 0 of 80 reachable |

**Cadence.** Kilns run weekly to daily: Greenwich House fires "a minimum of once per week", The Clay Studio's technicians "load multiple kilns daily", and Morning Ceramics can "fire up to 13 times a week"; one glaze firing occupies a kiln for about 40 hours [SCHED], [AUD-US]. Only 1 of 39 manager postings states a firing cadence (electric kilns "daily", the gas kiln "bi weekly") [MGR]. Monitor shifts are fixed weekly slots set per 10–12-week session (VisArts) [SCHED].

**Outputs** are records and notices: inventory records (19 of 39 manager postings), budgets (13), firing schedules or logs (11, logs proper 3), calendars (10), newsletters (10), policies (9) and staff schedules (8) [MGR]. Only 18 of 40 technician postings name any document; one studio "uses lots of paper forms to track things like clay inventory, firing, orientations" [TECH].

**Software is barely named.** 23 of 39 manager postings name no product; the most-named are Microsoft Office (12) and Google Workspace (9). QuickBooks appears once (2020); Square, Stripe, Shopify, Xero, Mindbody and kiln-controller software never [MGR]. 4 of 40 technician postings name a product, none of them an inventory, maintenance, booking, payment or accounting tool [TECH]. Kiln Fire is the only pottery tool found in use, at 3 of 84 studios [03].

## What hurts

| Rank | Problem (n=34 independent studios) [VOICE] | Sources | Detail |
|---|---|---|---|
| 1 | Money | 19 | cash flow, financing or seasonality 9; rent and leases 7; energy and inputs 5; pricing 5; collecting payments 1; firing-fee fairness 0 |
| 2 | Burnout and admin load | 16 | admin crowds out the person's own making in 6 |
| 3 | Equipment breakdowns | 10 | e.g. a thermocouple; a ~CA$6,500 kiln panel |
| 4 | Staffing and coverage | 9 | |
| 5 | Inventory and supplies | 6 | mostly supplier stock-outs |
| 6= | Pieces; scheduling; bookkeeping and tax | 5 each | |
| 9= | Member communication; capacity | 4 each | |
| 11= | Kiln logistics; software; safety; cleanliness | 3 each | |

Elsewhere, of 8 equipment-failure events (costs $3,800–$50,000), 6 ended in fundraising and only 1 was blamed on missed maintenance [INV]. Among 55 general small sellers, the top bookkeeping problems are matching payouts to sales (15), keying entries by hand (9) and processor fees (8) [MONEY].

**Against 01's ranking.** 01 put piece tracking first (~8 sources), firing fees second and kiln scheduling fourth. Here pieces appear in 5 of 34 sources, kiln logistics in 3 and fee fairness in 0, while equipment upkeep, absent from 01's list, appears in 10. Source type explains much of it: 14 of the 34 are podcast show notes and 9 are news pieces, formats that favour money and burnout, while Reddit and the Ceramic Arts Daily forum, behind 01 and 03, were blocked [VOICE]. Studios also publish the piece problem themselves: "Do not call us to find out if your work has been fired." (Choplet, collective firing) and "You will not be emailed/notified" (Radius, one-day-workshop pieces) [VER] #14. Inference: trust both. Owners volunteer money and burnout; members and staff hit piece and kiln problems every week. Software fixes the second directly and the first only by saving time.

## The hypotheses

### H1. Inventory and asset tracking

**Verdict:** inventory is a near-universal duty with no tool and no loud complaint. Equipment matters as kilns (and, in the UK, extraction), not as a register.

**For.** Inventory or purchasing is a duty in 30 of 39 manager and 30 of 40 technician postings [MGR], [TECH]. Studios control clay to protect their kilns: 10 of 84 sites require studio clay and 22 allow outside clay, usually only with approval [AUD-US], [AUD-INT]; kiln damage from the wrong clay "can be upwards of $1,000" (Berkshire Art Center) [INV]. Stock-outs happen: 4 of the Fitton manager's 7 supply posts report a stock-out, shortage or discontinued line; 3 of 8 supplier pages were out of stock on 2026-10-20; lead times of 3–10 business days [VOICE], [INV]. Par levels exist on paper: "maintain 3-4 barrels of prepared clay" and "Notify the Ceramics Studio Manager when materials are low" [TECH]. For assets, maintenance is in 38 of 40 technician postings and breakdowns rank third (10 of 34). L&L schedules checks "after each firing", "after 10 firings" and "after 30 firings or annually" ([L&L][ll-maint]) [VER] #13. UK employers must have local exhaust ventilation examined at least every 14 months and keep records for 5 years; some processes need shorter intervals ([COSHH reg 9][coshh9]) [VER] #12.

**Against.** 3 of 39 manager postings mention inventory systems (two are production potteries); 0 of 40 technician postings name inventory or maintenance software; no studio was found using an inventory tool or maintenance software [MGR], [TECH], [INV]. No first-hand 2023–26 complaint about running out, theft or glaze-batch losses was found, though the forums where they would appear were blocked [INV]; 0 of 55 bookkeeping posters say stock levels don't update [MONEY]. Retail stock is small: 8 of 12 clay nonprofits report $0 on the 990's inventory-sales line, which may undercount shop sales [MONEY], and PYOP bisque stock already sits in POS systems [INV]. Asset counts or documented inspections appear in 6 of 39 manager postings; most failures are sudden or end-of-life, a capital problem; and US and UK tax rules don't force a register [MGR], [INV].

**Confidence:** high that the work exists (79 postings); low on how much it hurts.

**Shape.** *Clay and supply sales with reorder points and purchase requests, not general inventory:* a par level, pack size and supplier lead time per item; low-stock alerts; a purchase request that someone approves; "out of stock, use this instead" notices to members; clay sales recorded against the member who will fire the work. *A kiln maintenance log driven by firing counts, not a fixed-asset register:* a per-kiln counter fed by the firing log, with thresholds the studio sets (L&L's own pages disagree on relay life: ~173 firings against "many years"), resistance readings, a repair log with costs, "kiln down" on the calendar, and a 14-month extraction reminder for UK employers. Generic maintenance tools charge for count-based triggers (MaintainX Premium, $65 per user per month) [INV]. Inference: L&L's ~10-year kiln life, a fallback for when use can't be assessed ([L&L][ll-life]), could support a replacement-reserve view, which fits the capital problem better than reminders.

### H2. Sales integrations with several payment providers (stated requirement)

**Verdict:** the requirement matches the evidence when it means several *payout sources* per studio. Several *card processors* for the same sales aren't documented, and evidence for building multi-processor charging in phase 1 is weak.

**For.** 21 of 84 census studios (25%) take money through two or more tools (US 12 of 42, UK 6 of 12, AU 2 of 18, CA 1 of 12). All 7 Square studios also run another system, and in 5 of the 7 Square holds gift cards or a clay store; 5 studios add a marketplace that collects and pays out later (ClassBento, Eventbrite) [MONEY]. Matching payouts to sales is the top bookkeeping complaint (15 of 55) and grows with each platform [MONEY]. Vendors offer a choice of processor: Pike13 lists five, WellnessLiving assigns them by country, Rock Gym Pro offers Stripe or OpenEdge [INT].

**Against.** 0 of 10 vendor pages document a business running two processors at once. That is absence of evidence, not a ban: Rock Gym Pro says a facility "can use a standalone credit card terminal for POS transactions" beside its integrated Stripe processing ([Rock Gym Pro][rgp]) [VER] #7. No international audit site shows two card processors side by side; Square appears only as a gift-card link at 4 [AUD-INT]. Stripe and Square are named in 0 of 39 manager postings, 0 of 40 technician postings and 0 of 34 owner sources [MGR], [TECH], [VOICE]. The pottery incumbent is "100% tied into Stripe's functionality" ([Kiln Fire][kilnfire]) [INT]. Each provider brings its own tokens and webhooks: Square tokens expire after 30 days, Square retries webhooks for 24 hours and Stripe for 3 days, and both can arrive out of order or twice [INT].

**Confidence:** medium. Websites can't see card readers, cash or cheques.

**Shape.** Make the processor **payout** a first-class record from day one: Momence sends deposits "per batch, rather than per individual booking", and Amilia moved to gross payouts after customers cited "bank reconciliation" [INT]. Charge through one provider behind an interface; import payouts read-only from the others (Square gift cards and POS, Shopify, marketplaces); report each deposit as sales minus refunds and fees, plus or minus adjustments. Stripe Connect's "Stripe handles pricing" mode costs the platform nothing ([Stripe][stripe-connect]). Square OAuth is free, but writing Square bookings needs the studio to pay for Appointments Plus or Premium, gift-card loads cost sellers 2.5% (AU, CA, US), and "Developers cannot collect application fees for gift card payments" ([Square][square-giftcards]) [INT]. Read Shopify rather than write to it: rules 1.1.2 and 1.1.8 bar App Store apps that bypass Shopify checkout or register transactions "in connection with such activity", or that connect a non-Shopify POS. Treating 1.1.2 as a ban on writing counter sales into Shopify is the notes' reading ([Shopify][shopify-req]) [VER] #5. As for the first provider: Stripe is a processor at 6 of 10 adjacent vendors and Square at 0 [INT]; Square appears at 7 of 84 studios, mostly for gift cards [03]; 02 chose Square. Inference: let the pilot studio decide.

### H3. QuickBooks Online integration (stated requirement)

**Verdict:** direct demand from studios is thin and mostly unmeasured; where pottery sources name a ledger, it is QuickBooks. Platform rules shape the design more than demand does.

**For.** QuickBooks is the only ledger pottery sources name: 3 practitioners and 1 UNVERIFIED job posting, against 0 for Xero, Wave, Sage, MYOB and FreeAgent [MONEY]. Where it appears, the pain is concrete: 12 of 17 Square-forum posters with sync problems use QuickBooks, and a makerspace with a ceramics area writes "The portal and QuickBooks never speak; the PDF is the bridge and a human is the wire." ([Past Lives][past-lives-436]) [MONEY]. Businesses pay for it: WellnessLiving's "Integration with Intuit QuickBooks Online" costs $39/month ([WellnessLiving][wellnessliving]), and Momence's $85/month buys its "Automated Bookkeeping" add-on, which "also integrates with Quickbooks and Xero" ([Momence][momence]) [VER] #6.

**Against.** QuickBooks appears in 1 of 39 manager postings (2020; 0 of the 28 recent ones), 0 of 40 technician postings, 0 of 80 reachable sites, 1 of 34 owner sources (a QuickBooks-hosted story) and 0 of 24 filings. Managers hold budgets (13 of 39) more often than they keep books (6) [MGR], [TECH], [VOICE], [ORG]. 6 of 10 adjacent vendors stop at exports or built-in ledger reports [INT]. Outside the US, Xero is at least as likely: 2.7 million ANZ subscriptions at 30 September 2025 (a news report; [Capital Brief][xero-anz]) against ~2.8 million actively trading Australian businesses. Intuit's ~8% international revenue covers all of Intuit, so it shows Intuit is US-centric rather than QuickBooks' own split ([Intuit 10-K][intuit-10k]) [VER] #18. On QuickBooks Free, "third-party integrations not currently available"; classes, locations and inventory need Plus ($140/month list, $70 promotional for 3 months) or Advanced ([QBO pricing][qbo-pricing]) [VER] #2. Per-transaction sync is itself the complaint: Intuit's "Square Connector by QuickBooks" app (on Square's marketplace since 2025-05-30) "replaced the old integration and no longer offers the summary view", per QuickBooks staff in August 2025; the date of the change isn't stated. In February 2026 Square's product manager was recruiting testers by survey for a daily-summary journal-entry integration, which had not launched ([QuickBooks Community][qb-square]; [Square Community][square-pm]) [VER] #3.

**Platform rules.** On Intuit's free Builder tier, "Core API calls are unmetered and uncharged" and cover "most data-in operations" (the per-endpoint list wasn't read). 500,000 CorePlus (read) credits a month are pooled across all production apps in a developer workspace and shared by every connected company; "Any CorePlus calls made above the included monthly credit will be blocked." The programme has covered the UK, Australia and Canada (excluding Quebec) since 3 November 2025 ([Intuit developer blog][intuit-blog]) [VER] #1. Estuary's connector used up the read budget "as adoption grew", its token refreshes then failed with 429 errors "until the month rolled over", and it made every user register their own Intuit app ([Estuary][estuary]) [VER] #20. Refresh tokens now have a 100-day rolling window and a 5-year hard limit [INT].

**Confidence:** low on the size of demand; high on the shape.

**Shape.** Webhook-driven, write-mostly and summary-level: one journal entry, or a sales receipt and deposit, per day or per processor payout, split by revenue line (classes, memberships, firing, clay and retail, events), with tax by category, processor fees as a gross expense, prepaid movements against liability accounts, and bills for payees. Read QBO only to set up mappings, keep the IDs that writes return, and never poll. Ship the same summary first as a ledger-neutral CSV or journal export; it also serves Xero users and the 26% of UK below-threshold VAT businesses that file through bridging software [MONEY]. Native Xero is a poor early target for a free product: A$35/month (ex tax) for 6–50 connections and A$245 for 51–1,000, plus A$2.40/GB above the data allotment, and its terms say "API Data cannot be used to train, fine tune, adapt, or enhance any AI Models" ([Xero pricing][xero-pricing]; [Xero terms][xero-terms]) [VER] #4. Inference: reach Australian and New Zealand studios through the export first.

### H4. Scheduling

**Verdict:** strong, but it is four jobs, and two of them are unserved: the kiln queue with its firing calendar, and rosters with training gates. The evidence contradicts leading with class or wheel booking.

**For.** 32 of 39 manager postings schedule something: class and open-studio calendars 23, firing 11, staff rosters 8, bookings and rentals 8 [MGR]. In 2 of the 3 manager postings examined for scheduling, one person owns the kiln schedule, the open-studio schedule and the technician and volunteer roster [SCHED]. 14 studios publish kiln queue rules (11 verified), most often that class work goes first; 13 state a turnaround or firing timing, most commonly about two weeks per firing; kiln calendars draw 6 first-person complaints (5 from 03, 1 UNVERIFIED), more than any other scheduling type [SCHED]. 0 of 16 pottery labour programmes name a scheduling or hour-tracking tool. The Kiln Rooms runs 18–20 work-exchange places at 7 hours a week ([Kiln Rooms][kilnrooms-wx]); Sonoma rotates "12 studio monitors" across "40 of our 60 weekly hours" ([Sonoma][sonoma]) [SCHED]. 7 studios broadcast schedule changes by hand, and 17 of 84 sites describe an orientation, induction or certification step before independent use [SCHED], [AUD-US], [AUD-INT].

**Against.** Class booking is served: of 84 studios, 33 sell through website-builder commerce, 11 through schedulers and 11 through class-registration tools [03]. 12 of 84 sites say no booking is needed, and the wheel-booking pain comes from vendors (4 claims) rather than studios (0 complaints) [AUD-US], [AUD-INT], [SCHED]. No source measures the hours scheduling takes.

**Confidence:** high for the kiln queue; medium for rosters (common practice, little voiced pain).

**Shape.** One calendar per resource (each kiln, the wheel room, the glaze room, the whole studio) with booking types: firing, rental, training firing, maintenance or out of order, class block, event. A queue policy per kiln: oldest first, at most N pieces per member per load, class work first, whole-kiln rental. Broadcasts for closures and changes. A roster of fixed weekly slots rebuilt each session, with gaps flagged and an hours-to-credit ledger that separates nonprofit volunteers from paid staff with perks. Certification as an expiring permission on a resource. Class bookings read from the storefront, not replaced. One caution for for-profit US studios: the Labor Department's FLSA Advisor (undated) says "employees may not volunteer services to for-profit private sector employers"; applying it to work exchange is the notes' inference ([DOL][dol]) [VER] #8.

## Strong signals outside the hypotheses

| Signal | Evidence | Shape |
|---|---|---|
| Kiln queue, firing status, "ready" notice | 6 of 84 sites publish a queue or status; 13 of 42 international sites promise a ready email and 4 leave it to the customer ("We don't email to inform you to collect your items", Clay Studio Manchester) [AUD-US], [AUD-INT]; 03: ready emails go out "too early" when pieces span kiln loads | Per-piece status page; one notice per batch; stale-piece alerts |
| Members' pieces | Piece work in 10 of 40 technician postings [TECH]; 16 international sites publish a collection deadline (7 days to 3 months) [AUD-INT]; an "orphan shelf" each session, fines for shelf overflow [VOICE] | Hold period, unclaimed state, reminders before disposal |
| Rosters and training gates | See H4. 6 international work-exchange programmes, 3–7 h/week where stated [AUD-INT]; Northern Clay Center sells volunteer shifts as $0 shop items ([NCC][ncc-vol]) [VER] #16 | Roster, hours ledger, certifications |
| Prepaid money | Gift cards on 53 of 84 sites; Choplet's firing card is valid 5 years and charges a 16% "restocking fee" on credits back to it ([Choplet][choplet-card]); US Regulation E bars gift-card expiry under 5 years ([CFPB][regE]) [MONEY] | One stored-value ledger for gift cards, firing cards, class packs, memberships and deposits, with opening, sold, used, expired and closing balances |
| Payables | Consignment at 60/40 for Radius members ([Radius][radius-consign]) and 50/50 paid monthly via bill.com at The Clay Studio ([Clay Studio][clay-studio-consign]); instructor shares at Past Lives (a 70/10/20 split) run about $2,600 a month ([Past Lives #662][past-lives-662]); 1099-NEC from $2,000 for payments made in 2026, possibly inflation-adjusted from 2027 ([IRS][irs-1099]) [MONEY], [VER] #10 | Payee ledger with tax status, monthly statements, bill export |
| Sales tax by item, place and date | Washington made group pottery classes retail sales from 2025-10-01 ([WA DOR][wa-2025]) and excluded presentations by 501(c) nonprofits from "retail sale" from 2026-07-01 ([WA DOR][wa-2026]); California taxes firing of customers' ware under a 1950/1953 CDTFA annotation ([CDTFA][cdtfa]) [VER] #9; 5 of 55 posters struggle with tax categories [MONEY] | Tax category per item, with effective dates |
| Recurring membership billing | Dues are 99% of revenue at a pure co-op, 57% at The Digs, and 41% and 19% at two guilds [ORG]; a membership studio's CEO: "SUBSCRIPTIONS all day long!!!!" [MONEY] | Billing with pause and stop |
| Capacity and waitlists | 17 of 84 sites run waitlists (BKLYN CLAY: 15–18 months); 26 cap places or hours [AUD-US], [AUD-INT] | A waitlist that offers freed places |
| Handover and continuity | Fitton cover posts; co-ops spread the job across volunteer officers [VOICE], [ORG] | A "studio today" view: kilns and contents, queue, low stock, notices sent, who covers |

## Recommended direction for the first phase

Everything in this section is **Inference** from the evidence above.

| Rank | Option | Strength and main evidence |
|---|---|---|
| 1 | Piece → kiln → pickup loop with queue policy, status page, ready and hold notices | Strong: firing in 32 of 40 and 24 of 39 postings; status published by 6 of 84 sites; 14 published queue policies; studios building their own [TECH], [MGR], [SCHED], [03] |
| 2 | Studio calendar: kiln and equipment blocks, closures, rosters, training gates, handover view | Strong for kilns, medium for rosters: 32 of 39 postings schedule; 0 of 16 labour programmes use a tool [MGR], [SCHED] |
| 3 | Money ledger: balance owed, stored value, payout import and reconciliation, summary export feeding QBO | Medium, and it carries both stated requirements [MONEY], [INT] |
| 4 | Clay and supply sales with par levels and purchase requests | Medium: a duty in 30 of 39 and 30 of 40 postings; pain in 6 of 34 sources [MGR], [TECH], [VOICE] |
| 5 | Kiln maintenance log counted from the firing log | Medium-low, but cheap once firings are logged [INV] |

**Standing in for a manager, task by task.** Software can run these alone: ready, pickup and disposal notices; stale-piece alerts; closure and cover notices; low-stock alerts and reorder reminders; maintenance due by firing count or date; roster gaps and hour credits; membership billing and balance reminders; payout import, reconciliation and summary posting; certification expiry checks. Software can propose or remind, but a person must do: the kiln load (loading, firing, unloading), approving and placing supply orders (suppliers have no APIs [03]), repairs, inductions, approving outside clay and glaze, refunds and disputes, and the hard conversations ("we still dread those calls", a Color Me Mine co-owner on damaged pieces [VOICE]). The first list is the paperwork and messaging half of the job. Building it so whoever covers can see the studio's state answers the single-point-of-failure finding.

**Defer.** General SKU inventory (PYOP stock lives in POS; nonprofit retail is small). A fixed-asset register (no demand; tax doesn't require one). Class booking (served at 55 of 84). Charging through more than one processor at launch (undocumented, and each live provider is a standing maintenance cost; the interface keeps the door open). Native Xero (it costs money from the sixth studio; the export covers it). Writing to Shopify (App Store rules). Invoice OCR: nobody asked for it [01], and 0 of 34 owner sources mention supplier invoices [VOICE]. OCR can return later as the showcase 02 intended; the payout ledger and QBO sync offer comparable engineering stories (idempotent webhooks, the outbox, reconciliation, read budgets).

**Mapping onto 02's Wave 1:**

| 02 item | Verdict | Change |
|---|---|---|
| 1. Tenancy and roles | Keep, widen | Technician, coordinator, monitor, work-exchange and volunteer-officer roles; expiring per-resource permissions, as 03 asks |
| 2. Piece tracker | Keep, add | 03's hold period and unclaimed state; one ready notice per batch; a status page for members |
| 3. Firing-fee rules | Keep, add | 03's allowances and payment timing; a tax category with effective dates; draws on prepaid firing cards |
| 4. Kiln loads and firing log | Keep, add | 03's queue policy; a firing counter per kiln feeding maintenance thresholds; "kiln down"; the handover view |
| 5. Supplies inbox with OCR | Change | Wave 1 gets clay and supply items, par levels, purchase requests and stock notices; OCR moves later |
| Payments: none in Wave 1, Square premium in Wave 2 | Change | Add the stored-value ledger and read-only payout import with reconciliation in Wave 1; put charging behind a provider interface; choose the first provider with the pilot |
| QBO (absent from 02) | Add | Wave 1: a daily or per-payout summary export. Wave 2: write-mostly QBO posting (Wave 1 if the pilot runs QBO) |
| Wave 2 class booking and waitlists | Change | Keep waitlists; read class bookings from storefronts [03] |
| Wave 3 consignment ledger and 1099 export | Move to Wave 2 | The payables evidence |
| Wave 3 monitor shifts, work exchange, certification gating | Move to Wave 1–2 | The unserved scheduling jobs; 0 of 4 booking vendors market them [SCHED] |

If Wave 1 must stay small, the order is: piece and kiln loop (M1–M2), then calendar, rosters and handover, then ledger and export, then supplies.

## What the evidence can't tell us, and how to find out

- **Time.** No owner gives weekly hours (0 of 34), and nothing measures kiln-loading or scheduling time. The only figures are UNVERIFIED snippets (Lillstreet: one gas bisque firing ≈ 8 h/week; Phipps: "10 hours/week on average").
- **Money tools in use.** Card readers, cash and QBO plan tiers don't show on websites.
- **Inventory and maintenance pain.** The forums that would show it were blocked.
- **Segments.** No co-op or makerspace postings, 5 PYOP manager postings, no Australian or New Zealand postings, and no for-profit revenue mix beyond one UNVERIFIED sale listing.

**Interview questions** for owners, managers, technicians and co-op officers: (1) What did you do each day last week, and for how long? (2) When were you last away, what waited, and how did your cover know what to do? (3) Which tools took money last month, and who reconciles the payouts? (4) What does your bookkeeper use, on which plan, and what do they ask you for each month? (5) How do you know clay or glaze is low, and when did you last run out? (6) How do you track firings and element changes, and what happened at your last kiln failure? (7) How many "is it ready?" messages arrive each week? (8) How are shifts filled, swapped and credited? (9) What prepaid balances are outstanding? (10) Whom do you pay besides staff, and how?

**What a pilot studio should measure:** admin hours per week (a two-week diary before and after); "is it ready?" contacts per week; days from drop-off to ready notice; pieces past their hold date; stock-outs per month; firings per kiln against the controller log; maintenance due against done, and kiln-down days; minutes a month spent reconciling payouts; uncovered shifts; and QBO read calls per studio per month, to size the pooled 500,000 cap.

## Evidence quality and limits

- **Verification.** 12 of 12 sampled quotes matched their sources (a 95% interval of about 76–100%, so "high", not "perfect"). Of 20 decision claims, 15 were confirmed, 5 partly confirmed and 0 wrong; the partial verdicts came from attribution and scope, not misquotation [VER].
- **Where notes disagree.** [INT] calls the QBO cap "app-wide"; [MONEY] attributes the dropped summary view to "the Square connector" and says Square "has rebuilt" its integration; [VOICE] says an injury, a medication lapse and a trip all delayed firing. This report follows [VER], which re-opened the primary pages. QBO ratings differ ([MONEY]: "moderate" in the US; the others: "weak" or "none") because [MONEY] studies general small sellers while the others study studios, whose accounting is invisible in public; both can be right. Inventory is "strong" as a duty in the posting files and "weak" as a product anchor in [INV]: a duty is not demand.
- **Blocked sources.** Reddit (every host), the Ceramic Arts Daily forum (403), compressed PDFs, the Wayback Machine, review sites and some government and vendor docs.
- **Search budget.** A 200-search limit shared by all agents ran out early and repeatedly; later discovery followed links from pages already open.
- **Geography.** Postings are 72 of 79 US. The audits are half international (Canada 12, UK 12, Australia 18; no New Zealand studios). Owner voice is 23 of 34 US.
- **Segments and sampling.** 22 of 39 manager postings are nonprofit art centres, because archived job boards lean nonprofit; the 990 sample leans to large organisations; each file had one coder.
- **Public pages under-report back-office work.** "Not stated" isn't "no", which biases the inventory, maintenance and accounting counts down. Quotes passed through a tool that caps them at about 125 characters.

## Sources

**Notes files**, all in [Pottery studio manager role](../research_notes/Pottery%20studio%20manager%20role/):
- MGR: [job_postings_managers.md][MGR]
- TECH: [job_postings_technicians.md][TECH]
- VOICE: [owner_manager_voice.md][VOICE]
- ORG: [organizations_and_economics.md][ORG]
- MONEY: [money_and_bookkeeping.md][MONEY]
- INT: [integration_feasibility.md][INT]
- INV: [inventory_and_assets.md][INV]
- SCHED: [scheduling_and_staffing.md][SCHED]
- AUD-US: [studio_site_audit_us.md][AUD-US]
- AUD-INT: [studio_site_audit_intl.md][AUD-INT]
- VER: [verification_log.md][VER]

**Planning docs:** [01-market-survey.md][01] · [02-wave1-plan.md][02] · [03-tools-in-use.md][03]

**Key primary URLs**
- Intuit developer blog, App Partner Program posts: https://medium.com/feed/intuitdev/tagged/partner-program
- QuickBooks pricing: https://quickbooks.intuit.com/pricing/
- Estuary QuickBooks connector change: https://github.com/estuary/connectors/pull/4780
- Square connector thread, QuickBooks Community: https://quickbooks.intuit.com/learn-support/en-us/other-questions/the-new-square-connector-app-eliminated-my-summary-sales/00/1572343
- Square's daily-summary tester call: https://community.squareup.com/t5/Reports-Setup-Management/New-Square-to-QuickBooks-integration-for-daily-summaries-as/m-p/833698
- Intuit connector on Square's marketplace: https://squareup.com/us/en/app-marketplace/app/quickbooks-online
- Intuit 10-K, FY ended 2026-07-31: https://www.sec.gov/Archives/edgar/data/896878/000089687826000037/intu-20260731.htm
- Xero ANZ subscribers: https://www.capitalbrief.com/briefing/xero-first-half-revenue-lifts-20-to-1b-73f1b545-91ce-4eaf-b7dd-aad1b6bc98d4/
- Xero developer pricing and terms: https://developer.xero.com/pricing · https://developer.xero.com/xero-developer-platform-terms-conditions
- Momence and WellnessLiving accounting add-ons: https://intercom.help/momence/en/articles/12029948-automated-bookkeeping-faqs · https://www.wellnessliving.com/features/finance/
- Stripe Connect pricing; Square gift cards API; Shopify App Store requirements: https://stripe.com/connect/pricing · https://developer.squareup.com/docs/gift-cards/using-gift-cards-api · https://shopify.dev/docs/apps/launch/shopify-app-store/app-store-requirements
- Rock Gym Pro pricing; Kiln Fire: https://www.rockgympro.com/pricing · https://www.kilnfire.com/
- L&L maintenance and kiln life: https://hotkilns.com/support/pottery-kiln-knowledgebase/longevity-how-should-my-kiln-be-maintained-regular-basis · https://hotkilns.com/support/pottery-kiln-knowledgebase/longevity-how-long-does-kiln-last
- COSHH regulation 9: https://www.legislation.gov.uk/uksi/2002/2677/regulation/9
- Washington DOR notices; CDTFA annotation: https://dor.wa.gov/forms-publications/publications-subject/special-notices/live-presentations-now-subject-retail-sales-tax · https://dor.wa.gov/forms-publications/publications-subject/special-notices/live-presentations-new-exclusions-retail-sales · https://cdtfa.ca.gov/lawguides/vol2/suta/435-0860.html
- IRS 1099-NEC instructions; Regulation E; DOL volunteers: https://www.irs.gov/instructions/i1099mec · https://www.consumerfinance.gov/rules-policy/regulations/1005/20/ · https://webapps.dol.gov/elaws/whd/flsa/docs/volunteers.asp
- Fitton Center board ("Firing Delay!", "I'M BACK!", "AJ Out of Town"): https://fittoncenter.org/grouppage/pottery-studio/discussion/494a1d1b-ca21-4abb-bef3-89dbd5ec402e · https://www.fittoncenter.org/grouppage/pottery-studio/discussion/8ca36d35-0a3e-4d64-bd43-f84f816be3d7 · https://fittoncenter.org/grouppage/pottery-studio/discussion/6faee213-f0ef-415c-9d37-434a60887bca
- Studio policies: https://choplet.com/firing-services-kiln-rentals/ · https://choplet.com/product/firing-card/ · https://www.radiusstudio.org/contact-us/ · https://www.radiusstudio.org/sell-at-our-retail-shop/ · https://www.theclaystudio.org/apply/artist-program/shop-artist-program
- Labour programmes: https://www.nccshop.org/pages/volunteering-at-ncc · https://merrill.ucsc.edu/?p=302 · https://www.thekilnrooms.com/work-exchange-programme · https://sonomacommunitycenter.org/ceramics/ceramics-open-studio-program/
- Past Lives Makerspace payables issues: https://github.com/Past-Lives-Makerspace/plfog/issues/436 · https://github.com/Past-Lives-Makerspace/plfog/issues/662
- Ilium Ceramics takeover: http://koto.org/news/after-period-of-uncertainty-ilium-ceramics-studio-to-continue

[MGR]: ../research_notes/Pottery%20studio%20manager%20role/job_postings_managers.md
[TECH]: ../research_notes/Pottery%20studio%20manager%20role/job_postings_technicians.md
[VOICE]: ../research_notes/Pottery%20studio%20manager%20role/owner_manager_voice.md
[ORG]: ../research_notes/Pottery%20studio%20manager%20role/organizations_and_economics.md
[MONEY]: ../research_notes/Pottery%20studio%20manager%20role/money_and_bookkeeping.md
[INT]: ../research_notes/Pottery%20studio%20manager%20role/integration_feasibility.md
[INV]: ../research_notes/Pottery%20studio%20manager%20role/inventory_and_assets.md
[SCHED]: ../research_notes/Pottery%20studio%20manager%20role/scheduling_and_staffing.md
[AUD-US]: ../research_notes/Pottery%20studio%20manager%20role/studio_site_audit_us.md
[AUD-INT]: ../research_notes/Pottery%20studio%20manager%20role/studio_site_audit_intl.md
[VER]: ../research_notes/Pottery%20studio%20manager%20role/verification_log.md
[01]: ../01-market-survey.md
[02]: ../02-wave1-plan.md
[03]: ../03-tools-in-use.md
[intuit-blog]: https://medium.com/feed/intuitdev/tagged/partner-program
[qbo-pricing]: https://quickbooks.intuit.com/pricing/
[estuary]: https://github.com/estuary/connectors/pull/4780
[qb-square]: https://quickbooks.intuit.com/learn-support/en-us/other-questions/the-new-square-connector-app-eliminated-my-summary-sales/00/1572343
[square-pm]: https://community.squareup.com/t5/Reports-Setup-Management/New-Square-to-QuickBooks-integration-for-daily-summaries-as/m-p/833698
[intuit-10k]: https://www.sec.gov/Archives/edgar/data/896878/000089687826000037/intu-20260731.htm
[xero-anz]: https://www.capitalbrief.com/briefing/xero-first-half-revenue-lifts-20-to-1b-73f1b545-91ce-4eaf-b7dd-aad1b6bc98d4/
[xero-pricing]: https://developer.xero.com/pricing
[xero-terms]: https://developer.xero.com/xero-developer-platform-terms-conditions
[momence]: https://intercom.help/momence/en/articles/12029948-automated-bookkeeping-faqs
[wellnessliving]: https://www.wellnessliving.com/features/finance/
[stripe-connect]: https://stripe.com/connect/pricing
[square-giftcards]: https://developer.squareup.com/docs/gift-cards/using-gift-cards-api
[shopify-req]: https://shopify.dev/docs/apps/launch/shopify-app-store/app-store-requirements
[rgp]: https://www.rockgympro.com/pricing
[kilnfire]: https://www.kilnfire.com/
[ll-maint]: https://hotkilns.com/support/pottery-kiln-knowledgebase/longevity-how-should-my-kiln-be-maintained-regular-basis
[ll-life]: https://hotkilns.com/support/pottery-kiln-knowledgebase/longevity-how-long-does-kiln-last
[coshh9]: https://www.legislation.gov.uk/uksi/2002/2677/regulation/9
[wa-2025]: https://dor.wa.gov/forms-publications/publications-subject/special-notices/live-presentations-now-subject-retail-sales-tax
[wa-2026]: https://dor.wa.gov/forms-publications/publications-subject/special-notices/live-presentations-new-exclusions-retail-sales
[cdtfa]: https://cdtfa.ca.gov/lawguides/vol2/suta/435-0860.html
[irs-1099]: https://www.irs.gov/instructions/i1099mec
[regE]: https://www.consumerfinance.gov/rules-policy/regulations/1005/20/
[dol]: https://webapps.dol.gov/elaws/whd/flsa/docs/volunteers.asp
[fitton-delay]: https://fittoncenter.org/grouppage/pottery-studio/discussion/494a1d1b-ca21-4abb-bef3-89dbd5ec402e
[choplet-card]: https://choplet.com/product/firing-card/
[radius-consign]: https://www.radiusstudio.org/sell-at-our-retail-shop/
[clay-studio-consign]: https://www.theclaystudio.org/apply/artist-program/shop-artist-program
[ncc-vol]: https://www.nccshop.org/pages/volunteering-at-ncc
[kilnrooms-wx]: https://www.thekilnrooms.com/work-exchange-programme
[sonoma]: https://sonomacommunitycenter.org/ceramics/ceramics-open-studio-program/
[past-lives-436]: https://github.com/Past-Lives-Makerspace/plfog/issues/436
[past-lives-662]: https://github.com/Past-Lives-Makerspace/plfog/issues/662
