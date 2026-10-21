# Recurring Maintenance for Studio Equipment and the Building

_Research note, 2026-10-20. Question from the owner: would studios value help managing regular,
recurring maintenance across kilns, pug mills, clay traps and the building itself? Web and file
tools only. It builds on [the main report](reports/Pottery%20studio%20manager%20role.md) and
[inventory_and_assets.md](research_notes/Pottery%20studio%20manager%20role/inventory_and_assets.md),
which already cover kiln maintenance in depth; this note doesn't repeat their tables but cites them as
[INV], [TECH] (technician postings), [MGR] (manager postings), [VOICE] (owner voice) and [SCHED] (scheduling and staffing)._

**Labels.** **[MFR]** manufacturer · **[REG]** regulation or standard · **[STUDIO]** a studio's own
page or posting · **[PRAC]** practitioner, trade article or forum · **[VENDOR]** a vendor's claim ·
**UNVERIFIED** means the page couldn't be opened and the claim comes from a search summary only.
**Inference** marks my reasoning, not a source.

## Summary

- **The work is real and recurring.** Equipment maintenance is a duty in 38 of 40 technician
  postings and 27 of 39 manager postings [TECH], [MGR]. Postings and studio pages name the same
  set this note was asked about: kilns, pug mills, clay traps, wheels, slab rollers, extruders, spray
  booths, floors, and deep cleans run weekly, monthly or quarterly ([20], [23], [24], [TECH]).
  **Well supported.**
- **Kilns are the only equipment with a published schedule.** L&L's checks run every firing, every 10
  firings and every 30 firings or yearly; component-life figures disagree by 2–3x between sources (a
  thermocouple lasts 30–50 firings or 150) ([1], [4], [5]). Other studio machines (wheels, pug mills,
  slab rollers, clay traps) have no maker's interval found: they are cleaned by use and repaired when
  they fail. **Well supported.**
- **The building and safety items have the hardest dates.** US employers must check fire extinguishers
  visually every month and service them every year, keeping the record ([34]). Eyewash stations
  under ANSI Z358.1 are activated weekly ([35]); NRCA advises roof inspections twice a year ([36]);
  UK law requires extraction (LEV) to be tested every 14 months with 5 years of records ([33]). These
  are generic small-building rules, not pottery-specific pain. **Well supported as rules; untested as
  demand.**
- **Pain is mostly sudden failure, not a missed checklist.** A relay stuck closed at Harvard-Westlake
  overheated a load, "some of her pieces exploded" and the kiln was out for about two weeks ([10]);
  only 1 of 8 earlier failure events was blamed on skipped maintenance [INV]. Clogged drains are a
  known risk but no studio clog story was found. **Moderate.**
- **Nobody uses software for it.** 0 of 40 technician postings and 0 studios found use maintenance
  software; tracking is a paper kiln log, a wiki or a whiteboard of chores ([12], [INV], [TECH]).
  Pottery tools don't cover it: Kiln Fire's docs list no maintenance topic ([40]); Ceramispace records
  only "when you last did maintenance" for one user ([42]). Generic maintenance tools (CMMS) charge
  per user and put usage triggers in paid tiers ($55–65 per user per month) ([43]–[46]).
- **The app already has the kiln half.** Each kiln has a firing count, an "every N firings" threshold,
  a maintenance log with cost, and a "down" reason that blocks loading. Its limits: one threshold per
  kiln, no calendar trigger, no non-kiln equipment, no assignment, checklist, photo or reminder, and
  logging any repair resets the count.
- **Recommendation (Inference).** Build a small generalization rather than a CMMS: **assets with
  maintenance tasks**, each due by days or by firings, assigned to a duty or person, with a checklist,
  a log entry with cost, photo and parts used, due and overdue on Today, a reminder email through the
  existing outbox, and an optional "down" that blocks loading and booking. Don't displace the P1
  pilot work; add it as a top Wave 2 candidate and put three maintenance questions in the pilot kit.
  The kiln-specific fixes (several tasks per kiln, not resetting the count on every repair) are worth
  doing whenever kilns are next touched.

## What exists in the app today

| Piece | What it does | Where |
|---|---|---|
| Kiln counters | `total_firings`, `firings_since_maintenance`, and `maintenance_every_firings` (1–10,000, null if untracked). Completing a firing adds one to both counters | `V6__kilns_and_firings.sql`; `FiringService` |
| Due flag | `maintenanceDue` is true when firings since maintenance ≥ the threshold | `KilnView.maintenanceDue()` |
| Maintenance log | Insert-only rows: note (≤500 chars), cost, `at_firing` (total firings at the time), who, when. Listed newest first, up to 200 | `kiln_maintenance`; `KilnService.recordMaintenance`, `maintenanceLog` |
| Kiln down | `down_reason` (≤200 chars). Building a load on a down kiln is refused | `KilnService.setDown`; `FiringService` line 81 |
| Today | Staff see `maintenanceDue` kilns and `outOfService` kilns | `TodayService` |
| Duties | TECHNICIAN, MONITOR, WORK_EXCHANGE, VOLUNTEER_OFFICER, INSTRUCTOR per member; shifts can name a duty; hours are credited | `V7__duties_calendar_rosters.sql` |
| Resources and bookings | Bookable things (KILN, WHEEL_ROOM, GLAZE_ROOM, STUDIO, EQUIPMENT, OTHER), with a `MAINTENANCE` booking kind that blocks the whole resource | `V7`; `BookingRules` |
| Supplies | Items with category `KILN_SUPPLY`, par levels, stock movements including `USED`, purchase requests | `V9__supplies.sql` |
| Notices and email | Inbox notifications (`PIECES_READY`, `STUDIO_NOTICE`) emailed through the Spring Modulith outbox; a scheduled sweep exists for unclaimed pieces | `notify`, `platform/mail`, `UnclaimedPieceSweep` |

**Gaps and limits:**

1. **One threshold per kiln.** Elements, thermocouples, relays, kiln wash and the yearly wiring check
   fall due at different counts and dates ([1], [4], [5]). One number can't hold them.
2. **Any log entry resets the count.** `recordMaintenance` sets `firings_since_maintenance = 0` on
   every entry, so logging a small repair restarts the element countdown.
3. **No calendar trigger.** "Every 30 firings or annually" ([1]) and every building item are by date.
4. **Kilns only.** Wheels, the pug mill, clay traps, the spray booth, vents, extinguishers and the roof
   have nowhere to live. `resource` has kinds for equipment but no maintenance, and kilns and resources
   aren't linked, so a down kiln still accepts calendar bookings.
5. **No assignment, checklist, photo, reminder or parts.** Due work shows only on Today for staff; no
   one is emailed; the cost is a single number with no link to the spare element taken from stock.
6. **Good foundations.** The log is insert-only (useful as a compliance record), records the firing
   count at the time, and the outbox, sweep pattern, duties and photo pipeline (P1 item 4) already
   exist.

## Evidence, by equipment type

### Electric kilns (the best documented)

| Task | Frequency found | Label | Source |
|---|---|---|---|
| Vacuum floor and element grooves; check for glaze or clay chips; kiln-wash shelf tops | Every firing | MFR, PRAC | L&L [1]; "Clean the floor and elements before each firing" (Ceramics Monthly 2025) [6] |
| Element resistance, cord and breaker temperature at peak, case tightness, brick repair | Every 10 firings | MFR | L&L [1] |
| Wiring and terminal tightness | Every 30 firings or yearly | MFR | L&L [1] |
| Full check-up | Start of the school year and "periodically" | PRAC | Ceramics Monthly, Jun/Jul/Aug 2025 [6]; Jim Skutt's checklist says "Back to school" is "ideal" (UNVERIFIED, page returned 522 and 404) [7] |
| Inspection by a kiln engineer | Yearly, "even if it is working" | VENDOR (UK supplier) | Potterycrafts, 2025-01-07 [5] |
| Replace elements | ~80 firings at cone 5–6 [4]; ~100 firings bisque and cone 6 [5]; when resistance is up 8–10% [3] | PRAC, VENDOR, MFR | |
| Replace thermocouple | 30–50 firings [4]; ~150 firings or about once a year [5] | PRAC, VENDOR | |
| Replace relays | ~173 firings or "18+ months" [2]; another L&L page says "many years" [INV]; yearly if firing daily, every 2 years otherwise (UNVERIFIED) [7] | MFR, PRAC | |
| Keep a firing log and note element changes | Ongoing | MFR, PRAC | "Keep a kiln log of firings." [1]; make "a note or record of when this was done" [6] |
| Keep spare elements, relays and a thermocouple | — | PRAC | [6] |

What goes wrong: a relay failed closed at Harvard-Westlake (2024-10-25), the kiln heated too fast,
pieces cracked or exploded, and it was out about two weeks; the relay was replaced and test-fired
[10]. The Fitton Center's manager posted that a load was "just waiting for a thermocouple replacement
on Friday" [11]. 8 earlier events cost $3,800–$50,000 [INV]. Takeaway (Inference): the spread of life
figures means a studio must set its own thresholds per component and learn them per kiln; the
controller rarely helps (only TAP counts component use) [9].

### Gas, wood and raku kilns

No maker's schedule found (also a gap in [INV]). General gas-appliance practice is an annual service
covering burners, safety devices and venting (Technical Safety BC, UNVERIFIED analog) [58]. A raku
hood cable snapped on burners "already on their last legs" [INV]. **Weak.** Inference: calendar
tasks ("annual burner service", "check hood cable") fit; firing counts matter less.

### Kiln vents

Orton's Vent Master FAQ: keep holes unblocked and the blower motor "should be vacuumed often", with
no interval [31]. L&L: a downdraft vent "can last for many years"; the flexible duct is the weak part
[32]. **Weak on frequency.** Inference: a quarterly "vacuum vent motor, check duct" task is a
reasonable default for a studio to edit.

### Clay and sink traps

| Finding | Label | Source |
|---|---|---|
| No maker's interval; clear bottles show when to empty; rinse buckets stretch the time between empties | VENDOR (UNVERIFIED) | The Ceramic Shop, Clay-King [14] |
| Gleco trap: rinse pool chlorine into the trap "once per week" against odour | VENDOR (UNVERIFIED) | [15] |
| Makerspace studio cleans "the entire system once a month" | STUDIO | TinkerMill Muddy Makers [13] |
| "empty sink buckets" weekly | STUDIO | technician posting T39 [TECH] |
| "Kiln (and kiln furniture), slab roller, extruder, wheel, pugmill, and clay trap maintenance and troubleshooting" | STUDIO | NW Pottery Collective, 2025-01-06 [20] |
| Sink-trap clay disposal (Vancouver 2026), "emptying sink trap particulate" (Ann Arbor 2024) | STUDIO (UNVERIFIED; PDFs unreadable) | [21], [22] |
| "Clean sink Traps" on a member deep-clean day | STUDIO | Delores Pottery, 2026-05-31 [23] |
| Clay "will build up in your drain and harden, causing clogs" | VENDOR | Diamond Core Tools [17]; Lakeside [16] |
| Chicago's code requires interceptors for clay and similar solids; an Australian utility requires plaster arrestors for clay work | REG (UNVERIFIED) | [18], [19] |
| Glaze dumped in a storm drain; studio paying "fines and the cleanup" | PRAC (news, 2024) | Color Me Mine Santa Monica [INV] |

**Moderate.** The task is common and recurring; the interval is set by use, not by the calendar.
Plumber costs found ($175–$300+, $241 average) are forum and vendor figures (UNVERIFIED) [17].
Inference: a weekly "check and empty trap" task with a checklist line "level below the fill line?"
fits; a usage meter doesn't.

### Pug mills and clay reclaim

Pug mills are named in 10+ technician postings, usually as "clay reclaim and pugging" [TECH], [20].
Makers publish no interval: Shimpo has troubleshooting PDFs for vacuum-pump oil and gear grease;
Peter Pugger's FAQ has nothing on maintenance [INV]. The Shimpo manual couldn't be read (compressed
PDF) [29]. **Weak on frequency.** Inference: calendar tasks (monthly "check vacuum pump oil",
yearly "gearbox grease") and condition notes; pounds pugged as a meter isn't supported by any source.

### Wheels, slab rollers, extruders

Members clean wheels after each use under studio rules (UCSD, Leland, Berkeley's Potters' Studio;
UNVERIFIED snippets for the last two). Brent's manual covers belts, motor brushes and fuses; Shimpo's
videos cover belts, pedal potentiometers and dust-cover grease; no interval in either (UNVERIFIED)
[51], [52]. North Star markets its slab roller as "virtually maintenance free", with canvases as the
consumable [INV]; another maker suggests gear inspection about every 300 hours (UNVERIFIED) [53].
**Weak.** These are repair-on-failure items. Inference: the useful features are "down" (out of order)
and a repair log, not a schedule.

### Spray booths and extraction

UNL: "Maintain the spray booth according to the manufacturers directions." [25]. Industrial booth
guidance changes filters by pressure gauge or about monthly (paint industry, UNVERIFIED) [30]. UK:
LEV "at least once every 14 months", records kept 5 years [33]. **Strong in the UK as law; weak
elsewhere.**

### Floors, silica dust and deep cleans

"Wet-mop floors to control dust; do not sweep." (UNL) [25]; "We mop almost every day in the studio."
(Hudgens Center manager) [28]. Technician postings give daily cleaning, weekly and monthly deep
cleans, and a quarterly deep clean [TECH]. Studios run group deep cleans: MAxT's December 2024 week
with "general cleaning and maintenance to check off as a group effort" [24], Delores Pottery's 13-item
list [23], Canberra Potters' "quarterly cleaning bees" [VOICE], Stanford's 6 hours of tasks per
quarter [SCHED]. **Strong.** Inference: this overlaps rosters. A recurring task with a checklist, done
on a shift and credited as hours, links maintenance to the existing hours ledger.

### The building and safety equipment

| Item | Frequency | Label | Source |
|---|---|---|---|
| Portable fire extinguishers | Visual check monthly; annual maintenance; record kept 1 year (US employers) | REG | OSHA 1910.157(e) [34] |
| Extinguisher near the kiln | "regularly inspected" | MFR | L&L install checklist [39] |
| Plumbed eyewash | Activated weekly; serviced yearly | REG (standard) | ANSI Z358.1 via Justrite [35] |
| Roof | Twice a year, spring and fall, and after severe weather | PRAC (trade body) | NRCA, 2013 [36] |
| HVAC | Filter checked monthly, changed at least every 3 months; contractor check-ups spring and fall | REG (agency guidance) | ENERGY STAR [37] |
| Kiln circuit | Cord, receptacle and breaker temperature every 10 firings | MFR | L&L [1] |
| Audit items for a painting studio | "Kiln maintenance and servicing records are logged and current"; extinguishers "within their service date" | VENDOR (template) | GoAudits [38] |

No insurer's published maintenance requirement was found; one search returned only general advice
and a forum anecdote [INV]. **Rules well supported; studio demand unmeasured.** Many studios rent,
and the landlord may own the roof and HVAC (St. Pete Ceramics' building was insured by the landlord
association [INV]). Inference: building tasks are cheap to support with the same calendar trigger and
matter most to studios that own their building or answer to a landlord's lease terms.

## How studios track it today

| Practice | Example | Source |
|---|---|---|
| Paper kiln log of firings and element changes | L&L, Ceramics Monthly; printed kiln log books; Skutt's printable maintenance log | [1], [6], [8] |
| Wiki page per kiln with resistances and procedures | ENTS makerspace (BookStack), with "TODO" gaps | [12] |
| Approximate counts from memory | John Bryan's wood kiln "logged an estimated 130 firings" | [INV] |
| Member notices when equipment is down | Fitton Center manager's post | [11] |
| Controller counters | TAP only, among 5 controllers checked | [9] |
| Single-user app | Ceramispace: last maintenance date, firing log, iCloud sync | [42] |
| Event pages for cleaning days | Corsizio (Delores), Pike13 (MAxT), TicketTailor (TinkerMill) | [23], [24], [13] |
| Maintenance or asset software | **0 studios found**; 0 of 40 technician postings name one | [TECH], [INV] |

Forums (Reddit, the Ceramic Arts Daily forum) were blocked again ("unable to fetch from
www.reddit.com"; CAD returned 403), so first-hand tracking habits remain the biggest gap.

## What other tools do

| Tool | Maintenance features | Price | Fit for a small studio |
|---|---|---|---|
| Kiln Fire (pottery) | None in its docs index; "Marking Firing Issues" on pieces | Private quote | No maintenance [40] |
| MudBuddy (pottery) | Firing by weight, revenue per kiln load; no maintenance described | From $29/mo | UNVERIFIED [41] |
| Ceramispace (pottery app) | "recording when you last did maintenance on your kiln" | Free; Pro subscription | One user, no schedule [42] |
| TAP controller | Component-life counters with user thresholds | Free tier; $4.99/mo premium | One kiln, one brand [9] |
| Fabman (makerspace) | Claims to "track required maintenance"; its forum (Nov–Dec 2025) discusses adding intervals by busy time, usage time or calendar days | €49–199/mo + per machine | Shows the same three triggers this note proposes [49] |
| MaintainX (CMMS) | Free: "2 active repeating Work Orders"; meter-based maintenance and parts only on Premium | $0 / $20 / $65 per user/mo | Free tier too small; Premium is overkill [43] |
| UpKeep (CMMS) | PM scheduling, checklists and parts from Premium | $24 / $55 per user/mo | Overkill [44] |
| Limble (CMMS) | Recurring work orders on all plans; meter-based scheduling and spare parts on Premium+ | Not published | Overkill [45] |
| Fiix (CMMS) | Free: "25 active PMs", unlimited assets, limited users | $0 / $45 / $75 per user/mo | Free tier could work, but knows nothing of firings, duties or members [46] |
| Maintenance Care (CMMS) | Free tier reported as 2 users, 3 PM schedules, 5 assets | Free / quote | UNVERIFIED [47] |
| Sortly (inventory) | "Date-based Alerts" for maintenance | Free–$149/mo | Dates only [INV] |
| Trello | Recurring cards by Power-Up or automation; built-in repeat reuses one card, so no history | Free tier | Generic; no evidence studios use it [50] |

Takeaway (Inference): generic tools charge per user (a studio's techs, monitors and work-exchange
members would each need a seat), keep usage triggers in paid tiers, and can't read the firing log the
studio already keeps. That is the opening: maintenance counted from firings the app already records,
assigned to the duties it already tracks, at no per-user cost.

## Proposed feature: assets with maintenance tasks

Everything here is **Inference** from the evidence above.

### First version

| Part | What it does | Builds on |
|---|---|---|
| **Asset** | Anything that needs upkeep: kiln, wheel, pug mill, slab roller, extruder, clay trap, spray booth, vent, building, safety equipment, other. Name, location, notes; optional link to a kiln and to a bookable resource | New table; links `kiln` and `resource` |
| **Maintenance task** | A recurring job on an asset: title, checklist lines, and a trigger of **every N days**, **every N firings** (kiln assets only), or both, whichever comes first. A "due soon" margin. Each kiln can have several tasks: "Vacuum and inspect" every firing, "Check element resistance" every 10, "Replace thermocouple" every 40, "Wiring check" every 30 firings or 365 days | Firing counter from `FiringService`; the `kiln.maintenance_every_firings` value becomes one task |
| **Assignment** | To a duty (any technician, any monitor, work exchange) or a named member | `member_duty` |
| **Log entry** | Done against a task (or ad hoc, for a repair): note, checklist ticks, cost, photos, parts used, who, when, firing count at the time. Completing a task restarts **that task's** clock only | `kiln_maintenance` generalized; P1 photo pipeline; a `USED` stock movement per part |
| **Due and overdue** | Today lists due-soon, due and overdue tasks for staff, and "my tasks" for whoever holds the duty | `TodayService` |
| **Reminder** | A daily sweep emails the assignee once per due cycle, and again when overdue | `UnclaimedPieceSweep` pattern; outbox and email |
| **Down** | An asset can be marked down with a reason. A down kiln already refuses loads; a down asset linked to a resource also refuses bookings and shows on the calendar | `down_reason`; `BookingRules` |
| **History export** | Per-asset log as CSV, for an insurer, a landlord or a UK LEV record | The ledger's export pattern |
| **Starter templates** | Suggested tasks when an asset is added (L&L's 1/10/30 firings, weekly trap check, monthly extinguisher check), all editable | Seed data only |

Optional, if cheap: credit hours when a work-exchange member logs a task done on a shift
(`hours_entry`), since deep cleans and trap duty are often traded for membership [SCHED].

### Leave out of the first version

- Work-order queues, priorities, vendor management, and purchase orders beyond the existing purchase
  requests.
- Meters other than firings (kiln hours, pounds pugged, wheel hours): no source uses them.
- Kiln-controller import (already Wave 3), structured resistance readings (a note field covers the
  ENTS-style table for now), and predictive maintenance.
- A fixed-asset register with depreciation (no demand, and tax doesn't require it [INV]).
- QR labels on equipment, and members reporting problems from their phone. The second is a strong
  follow-on candidate: it is how most "down" states would start.

### Data and migration sketch

- `asset`, `maintenance_task` (trigger days and/or firings, last done at and at firing, duty or
  assignee, checklist as JSONB) and `maintenance_record` (insert-only, like `kiln_maintenance`),
  each with the same row-level security policy as the other tables.
- A migration creates an asset per kiln, turns each non-null `maintenance_every_firings` into a task,
  and copies `kiln_maintenance` rows into records with their `at_firing`. The kiln API keeps
  `maintenanceDue` for now, computed from the kiln's tasks.
- The OpenAPI contract gains asset, task and record schemas; the generated client is regenerated.

### Where it fits the plan

P1 is pilot readiness, and its order shouldn't change for this. The kiln part of Wave 1 already
answers "maintenance due by firing count"; this generalizes it. It belongs at the top of the Wave 2
candidates in [04](04-pilot-readiness-plan.md), ranked by what the pilot studio says. It also adds an
engineering story: a scheduler with two kinds of trigger, idempotent reminders through the outbox, and
an availability rule that crosses two modules (kilns and the calendar).

## Open questions to ask a pilot studio

1. Which equipment and building items does someone look after on a schedule? Who does each one: you,
   a technician, monitors, work exchange, the landlord?
2. How do you know a kiln needs elements, a thermocouple or relays? Do you count firings, watch firing
   times, or wait for an error code? When did one last fail mid-firing, and what did it cost?
3. How often is the clay trap emptied, and by whom? Has a drain ever clogged?
4. Where is maintenance written down today (paper log, whiteboard, spreadsheet, controller, memory)?
   Would your cover know what was done last?
5. Does anyone ask you for maintenance records: an insurer, a landlord, a fire inspector, a school or
   city, the HSE?
6. Do you keep spare parts? Did you ever wait on one with a kiln down?
7. When equipment is down, how do members find out, and does anything stop them booking or loading it?
8. Would you trust a reminder email to a monitor or work-exchange member, or should tasks always go
   through a manager?
9. Is the building yours to maintain (roof, HVAC, extinguishers), or the landlord's?

## Evidence quality and limits

- **Strong:** that the work exists and recurs (postings, handbooks, deep-clean events); kiln schedules
  from one maker plus trade articles; legal and standard intervals for extinguishers, eyewash and UK
  extraction.
- **Weak or missing:** first-hand pain about missed maintenance (forums blocked); intervals for pug
  mills, wheels, slab rollers, clay traps and gas kilns; insurer requirements; whether studios would
  enter data at all.
- **Unreadable sources:** several PDFs (Shimpo pug mill manual, The Potters' Studio handbook, the Ann
  Arbor and West Point Grey postings, the Yale silica guide) returned compressed text; Jim Skutt's
  ArtLink article returned 522 and 404; the Ceramic Arts Daily forum returned 403; Reddit is blocked;
  Icon Water's guideline timed out. Claims from them are marked UNVERIFIED.
- **Bias:** postings over-represent nonprofits and colleges; most building rules apply to employers,
  so a volunteer co-op may sit outside OSHA's extinguisher rule while still wanting the reminder.

## Sources

All accessed 2026-10-20. "Notes" means the source was read in an earlier research pass and is
recorded in [inventory_and_assets.md](research_notes/Pottery%20studio%20manager%20role/inventory_and_assets.md)
or the other notes files.

1. L&L Kiln Mfg, "How should my kiln be maintained on a regular basis" (notes). https://hotkilns.com/support/pottery-kiln-knowledgebase/longevity-how-should-my-kiln-be-maintained-regular-basis
2. L&L, "How long do relays last" (notes). https://hotkilns.com/support/pottery-kiln-knowledgebase/relays-how-long-do-relays-last
3. L&L, "How can I extend ceramic kiln element life" (notes). https://hotkilns.com/support/pottery-kiln-knowledgebase/elements-how-can-i-extend-ceramic-kiln-element-life
4. Lakeside Pottery, computerized electric kiln troubleshooting guide (notes). https://lakesidepottery.com/Pages/Pottery-tips/computerized-electric-kiln-troubleshooting-reference-guide.htm
5. Potterycrafts (UK), Angela Armstrong, "New Year Studio Refresh!", 2025-01-07. https://potterycrafts.co.uk/blogs/blog/new-year-studio-refresh-a-short-guide-of-things-to-consider-this-month-in-your-studio
6. Ceramics Monthly, "Tips and Tools: Kiln Care", Jeni Hansen Gard, Jun/Jul/Aug 2025. https://ceramicartsnetwork.org/ceramics-monthly/ceramics-monthly-article/tips-and-tools-kiln-care
7. Mayco ArtLink archive, Jim Skutt, "Kiln Maintenance" (UNVERIFIED: 522 and 404). https://mail.maycocolors.com/index.php/art-link/artlink-archives/203-kiln-maintenance
8. Skutt, Basic Maintenance Checklist page (intro and log description only). https://skutt.com/skutt-resources/repairs/kiln-maintenance/
9. TAP / SDS Industries, controller life-expectancy counters (notes). https://help.kilncontrol.com/knowledge/life-expectancy
10. Harvard-Westlake Chronicle, kiln breakdown destroys student projects, 2024-10-25. https://hwchronicle.com/110319/ae/ceramics-kiln-in-feldman-horn-breaks-down-destroys-student-projects-in-progress/
11. Fitton Center pottery studio discussion, 2025-06-03 (notes). https://www.fittoncenter.org/grouppage/pottery-studio/discussion/8ca36d35-0a3e-4d64-bd43-f84f816be3d7
12. ENTS makerspace pottery wiki, troubleshooting (notes). https://docum.ents.ca/books/pottery/page/troubleshooting/export/html
13. TinkerMill Muddy Makers, water system workday. https://www.tickettailor.com/events/tinkermillmakerspaceevents/1619228
14. The Ceramic Shop, sink traps (UNVERIFIED). https://www.theceramicshop.com/store/category/17/134/Sink-Traps/ · Clay-King, sink traps (UNVERIFIED). https://www.clay-king.com/product-category/other-equipment/sink-traps/
15. Gleco Trap guide via The Ceramic Shop (UNVERIFIED). https://www.theceramicshop.com/downloads/glecotrap.pdf
16. Lakeside Pottery, preventing clay in drainage. https://lakesidepottery.com/HTML%20Text/Tips/Preventing-clay-in-drainage.htm
17. Diamond Core Tools, "The Dangers of Clay-Clogged Drains", 2021, updated 2025-03-24. https://diamondcoretools.com/blogs/resources/clay-clogged-drains
18. Chicago Municipal Code, interceptors (UNVERIFIED). https://dev-codelibrary.amlegal.com/codes/chicago/latest/chicago_il/0-0-0-2503245
19. Icon Water, TW-GN-106 Craft Activities (UNVERIFIED; timed out). https://www.iconwater.com.au/sites/default/files/2025-10/TW-GN-106%20Craft%20Activities.pdf
20. NW Pottery Collective, Studio Technicians posting, 2025-01-06. https://depts.washington.edu/advis104/2025/01/06/nw-pottery-collective-seeks-studio-technicians
21. West Point Grey Community Centre, Pottery Studio Technician 2026 (UNVERIFIED; PDF unreadable). https://westpointgrey.org/wp-content/uploads/2026/02/Pottery-Studio-Technician-Job-Posting-2026.pdf
22. Ann Arbor Art Center, Ceramics Studio Tech 2024 (UNVERIFIED; PDF unreadable). https://www.annarborartcenter.org/wp-content/uploads/2024/04/Job-Description-Ceramics-Studio-Tech-4-24.docx.pdf
23. Delores Pottery & Studios, studio cleaning event, 2026-05-31. https://site.corsizio.com/event/6a07499d609670070c540bfd
24. MAxT Makerspace, Ceramics Center Deep Clean, December 2024. https://maxt.pike13.com/events/14862266
25. University of Nebraska–Lincoln, ceramics safety policies. https://arts.unl.edu/art/ceramics-safety-policies
26. UNC Greensboro, ceramics EHS policy (UNVERIFIED). https://vpa.uncg.edu/wp-content/uploads/2024/05/ceramics.pdf
27. Yale EHS, respirable crystalline silica in the art school (UNVERIFIED). https://ehs.yale.edu/sites/default/files/files/respirable-crystalline-silica-art-school.pdf
28. The Potters Cast #1102, Yesha Panchal, Hudgens Center, 2025-01-30 (notes, [VOICE] Q43). https://thepotterscast.com/1102/
29. Shimpo pugmill instructions (unreadable PDF). https://potterycrafts.co.uk/cdn/shop/files/Shimpo_Pugmill_Instructions.pdf · Shimpo troubleshooting (notes). https://www.shimpoceramics.com/troubleshooting/
30. Products Finishing, "How to Know When to Change Spray Booth Filters" (UNVERIFIED). https://pfonline.com/articles/how-to-know-when-to-change-spray-booth-filters
31. Orton, Vent Master FAQ. https://www.ortonceramic.com/ventmaster-faq
32. L&L, "How long does the Vent-Sure downdraft vent last" (notes). https://hotkilns.com/support/pottery-kiln-knowledgebase/how-long-does-vent-sure-downdraft-vent-last
33. UK COSHH Regulations 2002, regulation 9 (notes). https://www.legislation.gov.uk/uksi/2002/2677/regulation/9
34. OSHA 29 CFR 1910.157, portable fire extinguishers. https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.157
35. Justrite, weekly inspections for emergency eyewash (ANSI Z358.1). https://www.justrite.com/news/weekly-inspections-for-emergency-eyewash-equipment/
36. Professional Roofing, "Tech Today", Mark S. Graham (NRCA), October 2013. https://www.professionalroofing.net/Articles/Tech-Today--10-01-2013/2338
37. ENERGY STAR, heating and cooling maintenance checklist. https://www.energystar.gov/saveathome/heating-cooling/maintenance-checklist
38. GoAudits, Pottery Studio Audit Checklist (template). https://goaudits.com/checklist/pottery-studio-audit-checklist/916/19/
39. L&L installation checklist (notes). https://hotkilns.com/node/5388
40. Kiln Fire documentation index. https://kilnfire.com/docs
41. MudBuddy on Capterra (UNVERIFIED). https://www.capterra.ca/software/1107549/MudBuddy
42. Ceramispace, App Store listing. https://apps.apple.com/us/app/-/id1526050930
43. MaintainX pricing. https://www.getmaintainx.com/pricing
44. UpKeep pricing. https://upkeep.com/pricing/
45. Limble pricing. https://limble.com/pricing
46. Fiix pricing. https://fiixsoftware.com/pricing/
47. Rework, "Best CMMS Software in 2026" (Maintenance Care free tier; UNVERIFIED). https://resources.rework.com/tools/asset-management/best-cmms-software-2026
48. Sortly pricing (notes). https://www.sortly.com/pricing/
49. Fabman forum, "Maintenance reminders for equipment", Oct–Dec 2025. https://forum.fabman.io/t/maintenance-reminders-for-equipment/1270 · Fabman product page (notes). https://fabman.io/product
50. Atlassian Community, "Creating Recurring Cards in Trello" (UNVERIFIED). https://community.atlassian.com/forums/Trello-articles/Creating-Recurring-Cards-in-Trello/ba-p/2645742
51. Potclays, Shimpo troubleshooting videos (UNVERIFIED). https://www.potclays.co.uk/Shimpo-troubleshooting-videos
52. Brent wheel manual via Bath Potters (UNVERIFIED). https://www.bathpotters.co.uk/userfiles/file/brent-wheel-manual.pdf
53. Relyef slab roller manual (UNVERIFIED). https://cdn.shopify.com/s/files/1/0602/4095/3531/files/Navod_k_pouziti_SVS_Relyef_Eng.pdf · AMACO slab roller note (UNVERIFIED). https://amaco.com/asset/671a4c1ee7377
54. Stanford Ceramics membership (notes, [SCHED]). https://ceramics.sites.stanford.edu/membership
55. Canberra Potters Society, volunteers (notes, [VOICE] Q93). https://canberrapotters.com.au/volunteer/
56. UC San Diego Craft Center, ceramics (UNVERIFIED). https://craftcenter.ucsd.edu/studios/ceramics.html
57. Clay-King, North Star slab roller (notes). https://www.clay-king.com/product/north-star-551-slab-roller-30/
58. Technical Safety BC, annual servicing of gas appliances (UNVERIFIED analog). https://www.technicalsafetybc.ca/regulatory-resources/regulatory-notices/information-bulletin-annual-servicing-gas-appliances
59. Ceramic Arts Daily forum, "Kiln maintenance: what spare parts should I have on hand" (403, not read). https://community.ceramicartsdaily.org/topic/23718-kiln-maintenance-what-spare-parts-should-i-have-on-hand/
