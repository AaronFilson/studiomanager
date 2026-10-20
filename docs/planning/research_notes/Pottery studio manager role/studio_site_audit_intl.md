# Studio site audit: Canada, UK and Australia (42 studios)

_Completed 2026-10-20 (all pages accessed that day). Scope: census rows 43–84 of the per-studio table in [03-tools-in-use.md](../../03-tools-in-use.md#per-studio-table) (Canada 12, UK 12, Australia 18), in table order. The 42 US rows (1–42) are audited separately with the same checklist; the two halves are meant to be merged. 40 of the 42 sites were opened; UBC Pottery Club and Twelve80 are unreachable and coded NS throughout._

**Headline (out of 42):** staff load the kilns at 21 (members load at 2, one of them only for some membership tiers); 25 state a firing turnaround and 13 promise a "ready" email; 31 run private or team events and 18 rent space, kilns or equipment; 22 sell clay, but only 4 make members buy it from the studio and 12 let them bring their own; 33 sell gift cards and 23 sell goods; 16 require open-studio reservations and 17 cap hours or places; 12 name someone other than the owner as running operations; 6 run work-exchange programmes; 4 publish a clay price; none mentions an accounting tool. Hypothesis ratings: scheduling **strong** (for resources, events and staff), inventory **moderate**, asset tracking **weak**, multiple payment providers **weak**, QuickBooks Online **none** (see [Hypothesis evidence](#hypothesis-evidence)).

## Method and legend

- **Sources:** each studio's own site (home page plus up to eight linked pages: membership, classes, firing or kiln policy, FAQ, about or team, jobs, volunteer or work exchange, shop, policy or handbook), plus the studio's own booking or shop pages. Wayback Machine snapshots were planned for dead sites, but web.archive.org is blocked for WebFetch, so none were used. Third-party listings, press and reviews are **not** counted as evidence.
- **Tools:** WebSearch (to locate a studio's own site) and WebFetch. WebFetch returns a model-processed view of a page, so each fetch asked for exact sentences; only text returned verbatim is quoted. Booking and commerce platforms and firing fees are already in 03 and were not re-collected.
- **Values:** **Y** = Yes (stated on the site) · **N** = No (the site says it is not offered / not allowed) · **NS** = Not stated (nothing found on the pages checked; never inferred as No) · **UNREACHABLE** = the page or site could not be opened · **UNVERIFIED** = seen only in a search snippet or other processed source that could not be opened directly.
- **Denominator:** every total is out of the fixed 42 studios, including unreachable rows.
- **Segments (one per studio):** membership/community studio · teaching studio · cooperative · nonprofit art centre · paint-your-own-pottery (PYOP) · production pottery · university/college/school · makerspace · municipal/recreation.

### Checklist sub-fields used for totals

| Item | Sub-fields (each Y / N / NS) |
|---|---|
| 1 Size | 1a member count or cap · 1b waitlist · 1c number of kilns · 1d number of wheels · 1e studio area |
| 2 Staff | titles shown (owner/founder · studio manager/coordinator · operations · technician/kiln tech · instructors · front desk · volunteers · board · other) · 2x someone other than the owner named as running operations |
| 3 Work exchange / assistant / monitor / volunteer program | offered · hours per week · trade or pay · duties listed · how to apply |
| 4 Required member duties | required · what they are |
| 5 Clay | 5a must buy from studio · 5b included in membership or classes · 5c price per bag/box · 5d how bought (front desk / online shop / honour log) |
| 6 Glazes | 6a studio glazes provided or included · 6b outside glazes allowed · 6c members mix glazes |
| 7 Retail | 7a shop selling clay, tools or supplies · 7b finished work for sale or consignment · 7c gift cards · 7d online store |
| 8 Open studio | 8a reservations required (and tool) · 8b capacity limits · 8c staffed or monitored hours · 8d key card / 24-hour access |
| 9 Kilns | 9a who loads (staff only / members) · 9b firing frequency or turnaround promise · 9c how firing is requested (form, slip, app) · 9d firing queue or calendar published |
| 10 Events and rentals | 10a parties, private or team events · 10b studio or kiln rental · how booked |
| 11 Equipment list | published (kiln makes/types, wheels, slab roller, pug mill, extruder, spray booth) |
| 12 Member communications visible | 12a newsletter · 12b member portal · 12c Slack/Discord/WhatsApp/Facebook group · 12d rules or handbook document (a page or file of studio rules a member can read) |

**Coding rules used in the summary table** (so the US half can be coded the same way):
- Codes are written `id:value` (sub-field id, a colon, then the value), so totals can be counted by searching only the summary-table rows (lines that start with `| <number> |`). An `N` is matched as `N` followed by a word boundary so it does not also match `NS`.
- **2a–2i** record which titles the site shows (2i = any other title, listed in brackets). **2x** = someone other than the owner is named, by name or by a management title, as running day-to-day operations.
- **4a** = a required cleaning or upkeep duty for members or open-studio users, including cleaning up after yourself. Clean-up inside a taught class, and rules such as "collect work weekly" or "keep pot bottoms clean", are not counted. **4b** = a chore rotation, studio jobs or co-op work hours beyond cleaning your own space.
- **8b** = a stated cap on people in the studio, seats or places, **or** a cap on hours or sessions per member (brackets say which).
- **12b** = any member, student or customer login, account page, booking dashboard or password-protected members' page.
- **PYOP rows:** clay and member items (4, 5) are NS by nature; item 8 is read as the painting floor (seat booking, seat limits, staffed floor).
- **5b** = clay included in membership **or** in classes (which one is noted in brackets).
- **7a** = a shop (physical or online) selling clay, tools or supplies to the public or members; clay sold only as a member add-on is recorded under item 5. **7d** = an online store selling goods (supplies, finished work, merchandise). Online class booking or gift cards alone do not count as 7d; gift cards are 7c.
- **8c** = the site says open-studio hours are staffed, supervised or monitored. **8d** = keys, fobs, codes or 24-hour access.
- **9a** values: `staff` (only staff load, or the site says staff fire the work, including class work), `members` (members load or fire their own), `both`, or `NS`. **9c** values: `form`, `slip`, `app`, `shelf` (place labelled work on a firing shelf), `drop-off`, `booking`, or `NS`.
- **11** values: `Y` (a list of named equipment), `Partial` (one or two items named), `NS`.

## Per-studio summary (one row per studio)

| # | Studio | City | Country | Segment | URL | 1 Size | 2 Staff | 3 Work-exchange | 4 Member duties | 5 Clay | 6 Glazes | 7 Retail | 8 Open studio | 9 Kilns | 10 Events/rentals | 11 Equipment list | 12 Member comms |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 43 | Create Art Studio | Toronto | Canada | teaching studio | https://createartstudio.ca/ | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS (membership "space permitting"; "our electric kiln") | 2a:NS 2b:Y 2c:NS 2d:NS 2e:Y 2f:NS 2g:NS 2h:NS 2x:Y (Pottery Studio Manager; instructors) | 3:NS | 4a:Y 4b:NS (clean own space; cleaning time inside booked hours) | 5a:N 5b:Y (classes; membership NS) 5c:NS 5d:NS | 6a:Y 6b:NS 6c:NS | 7a:Y (art supplies, no clay listed) 7b:NS 7c:Y 7d:Y | 8a:Y (portal, hourly) 8b:NS 8c:Y 8d:NS | 9a:NS 9b:NS 9c:NS 9d:NS | 10a:Y 10b:NS (team events, parties; phone/email) | 11:Partial (Shimpo wheels, electric kiln) | 12a:NS 12b:Y 12c:NS 12d:NS (rules referenced, no document) |
| 44 | Zei Pottery | Toronto | Canada | teaching studio | https://zeipottery.com/membership/ | 1a:NS 1b:Y 1c:NS 1d:NS 1e:NS (2 sites; waitlist for returning members) | 2a:Y 2b:NS 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2x:NS (owners Zey and John; "studio technicians and instructors") | 3:NS | 4a:Y 4b:NS (clean up; remove finished work weekly) | 5a:Y 5b:Y (classes/events; membership N) 5c:Y ($30 / 10 kg) 5d:NS | 6a:Y 6b:N (unless approved) 6c:N | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:NS 8b:NS 8c:NS 8d:Y (24/7) | 9a:staff 9b:Y (classes 2–3 wks) 9c:shelf+initials 9d:NS | 10a:Y 10b:NS (private events 3–20 guests; web form/email, 30% deposit) | 11:NS | 12a:NS 12b:NS 12c:NS 12d:Y (studio policies page; email when ready) |
| 45 | Clay With Me | Toronto | Canada | teaching studio | https://claywithme.ca/ | 1a:NS 1b:NS 1c:NS 1d:NS 1e:Y (Richmond site three floors, up to 150 guests; wheel room "fits 10–12 pottery wheels") | 2a:Y 2b:Y 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (Studio General Manager) 2x:Y (Studio Coordinator job: "Oversee the daily operations") | 3:Y (Energy Exchange: 4 h shift for 4 h studio time/wk; 6-month min; interview) | 4a:Y 4b:NS (clean own station) | 5a:N (approved cone 6 clay allowed) 5b:N (membership extra; classes NS) 5c:Y ($45 / 10 kg) 5d:in-studio | 6a:Y 6b:Y (tile test + approval) 6c:NS | 7a:N ("we don't offer supplies for purchase") 7b:Y (community shop of local makers) 7c:Y 7d:NS | 8a:NS (fixed weekly blocks per package) 8b:NS 8c:NS 8d:Y (lockbox code) | 9a:NS 9b:Y (≤3 wks; priority 2–3 wks) 9c:form 9d:NS | 10a:Y 10b:Y (parties, corporate, bridal; hourly studio rental; quote forms) | 11:Partial (wheels, kilns, slab rollers, extruders named in job ad) | 12a:Y 12b:Y (Appstle membership dashboard) 12c:NS 12d:Y (Studio Policy; membership T&C) |
| 46 | Parkdale Pottery | Toronto | Canada | teaching studio | https://parkdalepottery.ca/ | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS (2 sites) | 2a:Y 2b:Y 2c:NS 2d:NS 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (Studio Assistant) 2x:Y (Bella, Studio Manager) | 3:Y (energy exchange, 3–4 h/wk for equal studio time; currently full; apply NS) | 4a:NS 4b:NS (class cleanup only) | 5a:N (exchangees may bring own) 5b:Y (classes; membership N) 5c:NS 5d:self-serve | 6a:Y 6b:NS 6c:NS | 7a:NS 7b:NS 7c:Y 7d:Y (merch) | 8a:NS 8b:NS 8c:N (Dupont "may not be staff on-site") 8d:Y (keyless, 6am–12am) | 9a:NS 9b:Y (class work ≥2 wks) 9c:NS 9d:NS | 10a:Y 10b:NS (birthdays, corporate; request form; min 6) | 11:Y (Bailey extruder and slab roller, spray booth, slip casting) | 12a:Y 12b:Y (account login) 12c:NS 12d:Y (Studio Policies page; access codes by email) |
| 47 | Pottery Lab | Toronto | Canada | teaching studio | https://www.potterylab.ca/service-page/studio-membership | 1a:NS 1b:NS 1c:NS 1d:NS 1e:Y (1,300 sq ft; memberships "subject to availability") | 2a:NS 2b:NS 2c:NS 2d:NS 2e:NS 2f:NS 2g:NS 2h:NS 2i:Y (cleaning staff) 2x:NS | 3:NS | 4a:NS 4b:NS (rules only: clean bottoms, collect weekly) | 5a:N (bring own cone 6) 5b:Y (classes; membership N) 5c:NS 5d:NS | 6a:Y 6b:Y (test tile) 6c:NS | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:NS 8b:Y (≤10 sessions/month, ≤4 h each) 8c:NS 8d:NS | 9a:staff (class work) 9b:Y ("a few weeks") 9c:NS 9d:NS | 10a:Y 10b:NS (team, birthday, bridal; 8–30 guests; email, 40% deposit) | 11:Partial | 12a:NS 12b:NS 12c:NS 12d:Y (membership rules; policies page) |
| 48 | City of Pickering Pottery Studio | Pickering ON | Canada | municipal/recreation | https://www.pickering.ca/potterystudio | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS | 2a:NS 2b:NS 2c:NS 2d:Y 2e:NS 2f:NS 2g:NS 2h:NS 2x:NS ("studio technicians"; "staff") | 3:NS | 4a:NS 4b:NS | 5a:Y 5b:N (min. one bag per class) 5c:Y ($46.34 block) 5d:NS | 6a:NS 6b:NS 6c:NS | 7a:Y (clay, tool kit $29.35) 7b:NS 7c:NS 7d:NS | 8a:NS 8b:NS 8c:NS 8d:NS (fixed open-studio hours; membership required) | 9a:NS 9b:Y (≥2 wks) 9c:slip (paper tickets, 20+20 per bag) 9d:NS | 10a:NS 10b:NS | 11:NS | 12a:NS 12b:NS 12c:NS 12d:Y (firing policy; size chart posted in studio) |
| 49 | Old Ottawa South Community Association | Ottawa | Canada | municipal/recreation | https://oldottawasouth.ca/programs/ | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS | 2a:NS 2b:NS 2c:NS 2d:NS 2e:NS 2f:NS 2g:Y 2h:Y 2i:Y (association-wide: Executive Director, Program Director, Program Coordinator, Bookkeeper, Customer Service Supervisor; none pottery-specific) 2x:NS | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:NS (UNVERIFIED snippet: clay extra, $50/25 lb) 5c:NS 5d:online shop | 6a:NS (UNVERIFIED: glaze materials included) 6b:NS 6c:NS | 7a:Y (pottery clay in online store) 7b:NS 7c:NS 7d:Y | 8a:NS 8b:NS 8c:NS 8d:NS | 9a:NS 9b:NS 9c:NS 9d:NS | 10a:NS 10b:NS | 11:NS | 12a:NS 12b:NS 12c:NS 12d:NS |
| 50 | The Mud Oven | Ottawa | Canada | paint-your-own-pottery (PYOP) | https://www.mudoven.com/ | 1a:NS 1b:Y (seat waitlist) 1c:NS ("one of our kilns") 1d:NS 1e:NS | 2a:NS 2b:NS 2c:NS 2d:NS 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (generic "staff"; bilingual staff) 2x:NS | 3:NS | 4a:NS 4b:NS (PYOP, no members) | 5a:NS 5b:NS 5c:NS 5d:NS (PYOP, no clay) | 6a:Y (all-inclusive; staff glaze) 6b:NS 6c:NS | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:N (recommended; walk-ins welcome; Tock) 8b:Y (table seats) 8c:Y 8d:NS | 9a:staff 9b:Y (5–8 days; email when ready; 60-day hold) 9c:drop-off 9d:NS | 10a:Y 10b:NS (groups, birthdays, team building, mobile events; Tock or email) | 11:NS | 12a:NS 12b:NS 12c:NS 12d:NS (customer policies only) |
| 51 | UBC Pottery Club | Vancouver | Canada | university/college/school | https://amscampusbase.ubc.ca/ubcpottery/ | UNREACHABLE: 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS | 2a:NS 2b:NS 2c:NS 2d:NS 2e:NS 2f:NS 2g:NS 2h:NS 2x:NS | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:NS 5c:NS 5d:NS | 6a:NS 6b:NS 6c:NS | 7a:NS 7b:NS 7c:NS 7d:NS | 8a:NS 8b:NS 8c:NS 8d:NS | 9a:NS 9b:NS 9c:NS 9d:NS | 10a:NS 10b:NS | 11:NS | 12a:NS 12b:NS 12c:NS 12d:NS |
| 52 | Tendre Atelier | Montréal | Canada | teaching studio | https://www.tendreatelier.com/en/pages/pottery-studio-membership | 1a:NS 1b:NS 1c:NS 1d:Y (6 wheels) 1e:NS | 2a:Y 2b:NS 2c:NS 2d:NS 2e:Y 2f:NS 2g:NS 2h:NS 2x:NS (founder-teacher Marie Bourguignon) | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:N (membership: buy on site; classes NS) 5c:NS 5d:in-studio | 6a:Y (glazes and firing bundled with clay purchase) 6b:NS 6c:NS | 7a:NS 7b:Y (founder's work in studio boutique) 7c:Y 7d:NS (online shop "coming soon") | 8a:NS 8b:Y (9 h/week) 8c:NS 8d:NS | 9a:NS 9b:Y (painting: "a few weeks") 9c:NS 9d:NS | 10a:Y 10b:Y (studio reserved for corporate/creative events, up to 30; contact form) | 11:Partial (6 wheels) | 12a:Y 12b:NS 12c:NS 12d:NS |
| 53 | Atelier Forma | Montréal | Canada | teaching studio | https://atelierforma.ca/pages/firing-services | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS (2 sites; membership "Sold out") | 2a:Y 2b:NS 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (partner; Studio Assistant; "supervisor") 2x:NS | 3:Y (Studio Assistant work-trade: greet, log information, supervise open studio, clean; online form; hours/trade NS) | 4a:Y 4b:NS (mop own area, wash tools) | 5a:Y (members; firing-service customers may use approved outside clay) 5b:N (membership; classes NS) 5c:NS 5d:staff ("ask one of our team members") | 6a:Y 6b:N (members) 6c:NS | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:Y (3 h reserved slots at registration; 2 h slots sold online) 8b:Y (34 or 60 h per 4 wks) 8c:Y (assistants supervise) 8d:NS | 9a:staff 9b:NS 9c:drop-off (pieces checked by teacher/supervisor; pickup email) 9d:NS | 10a:Y 10b:Y (private events 5–23 via request form; $35 1-h glazing "studio rental") | 11:NS | 12a:Y 12b:Y (customer portal) 12c:NS 12d:Y (Studio Policies) |
| 54 | Centred Clay Studios | Calgary | Canada | membership/community studio | https://centredclaystudios.com/ | 1a:NS 1b:NS 1c:Y (2 kilns) 1d:Y (15 wheels) 1e:NS | 2a:Y 2b:Y 2c:Y 2d:NS 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (Master Potter, Mentor) 2x:N (owners hold "Studio Manager" and "Operations Manager" titles) | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:Y (bag of clay in first block) 5c:NS 5d:NS | 6a:Y (Ravenscrag glaze program) 6b:NS 6c:NS | 7a:NS 7b:NS 7c:NS 7d:NS | 8a:NS 8b:NS 8c:NS 8d:Y (fob + PIN; 24/7 on approval) | 9a:NS 9b:NS 9c:NS 9d:NS | 10a:NS 10b:NS | 11:Y (Shimpo RK Whisper wheels, L&L Equad Pro kilns, Frema slab roller) | 12a:NS 12b:NS 12c:NS 12d:NS |
| 55 | Turning Earth | London (4 sites) | UK | membership/community studio | https://www.turningearth.org/membership | 1a:Y (In Production: 25 makers) 1b:NS 1c:Y (In Production: 4 L&L + gas + test kiln) 1d:Y (In Production: 10 wheels) 1e:NS | 2a:NS 2b:Y 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (office; marketing and management roles) 2x:Y (Studio Manager at In Production; technicians "keep the studio going") | 3:NS (paid technicians also get full membership) | 4a:Y 4b:NS (clean-up time in sessions; clean apron) | 5a:NS 5b:N 5c:NS 5d:in-studio (8 clays, members only) | 6a:Y (20+ ready-mixed) 6b:NS 6c:NS (technicians mix) | 7a:Y (Leyton shop, clay and glaze materials at cost) 7b:Y (studio sales) 7c:Y 7d:NS | 8a:N (members; pass sessions booked) 8b:Y (15 or 22 h/week; ~3 h wheel limit) 8c:Y (technicians on hand) 8d:NS | 9a:staff 9b:Y (fire daily, to a schedule) 9c:NS 9d:NS | 10a:N (private bookings paused) 10b:Y (studio hire for shoots, Mondays; no kiln rental) | 11:Y | 12a:Y 12b:NS 12c:Y (members' Facebook page) 12d:Y (member handbook; contract) |
| 56 | The Kiln Rooms | London | UK | membership/community studio | https://www.thekilnrooms.com/drop-in | 1a:NS 1b:Y (2–5 month waits) 1c:NS 1d:NS 1e:NS (5 studios; exchange cohort 18–20) | 2a:Y 2b:Y 2c:Y 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (Bookings Manager, Technical Manager, Admin Support) 2x:Y (General Manager; site Studio Managers) | 3:Y (work exchange: 7 h/wk with a technician for free membership, firings, glazes, training; CV by email; full) | 4a:Y 4b:NS (clean own and communal areas after use) | 5a:N (personal materials with staff approval) 5b:Y (classes/private sessions; membership N) 5c:NS 5d:in-studio (materials store) | 6a:Y 6b:Y (staff approval) 6c:Y (glaze materials sold; exchange trains mixing) | 7a:Y (materials store, clay and glaze materials) 7b:Y (Christmas Sale, Summer Show) 7c:Y 7d:NS | 8a:Y (drop-ins via Bookwhen; members sign in/out) 8b:Y (drop-in numbers capped; 18 h/wk part-time) 8c:Y (technician always present at drop-ins) 8d:Y (Copeland key holders 7am–11pm) | 9a:members (Copeland induction includes kiln packing; others NS) 9b:Y (2–3 wks, aim 5–10 days) 9c:NS 9d:NS | 10a:Y 10b:Y (team, corporate, parties, photoshoots 5–22 by email/phone; wheel hire) | 11:Y (Copeland list) | 12a:Y 12b:NS 12c:NS 12d:Y (T&Cs; email when ready) |
| 57 | Studio Pottery London | London | UK | teaching studio | https://www.studio-pottery-london.com/ | 1a:NS 1b:NS 1c:NS 1d:Y (14 wheels in classroom) 1e:NS | 2a:NS 2b:Y 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (Director, Artistic Director, resident potter) 2x:Y (Studio Manager) | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:N (membership; clay "purchased via the studio") 5c:NS 5d:NS | 6a:Y 6b:NS 6c:NS | 7a:NS 7b:Y (artists' and members' work, in studio and online) 7c:NS 7d:Y (online shop of ceramics) | 8a:NS 8b:Y (≤18 h/week) 8c:Y (full-time technician on site) 8d:NS | 9a:NS 9b:NS 9c:NS 9d:NS | 10a:Y 10b:Y (corporate sessions up to 60 by email; exclusive hire £3,500–5,000) | 11:Partial (14 wheels; kiln and glazing room) | 12a:NS 12b:Y (Mindbody client login) 12c:NS 12d:Y (membership T&Cs) |
| 58 | Trace Ceramics | London | UK | membership/community studio | https://www.traceceramics.co.uk/monthly-membership | 1a:NS ("limited members per month", no number) 1b:NS 1c:Y (2 Rohde kilns) 1d:NS 1e:NS | 2a:NS 2b:NS 2c:NS 2d:Y 2e:NS 2f:NS 2g:NS 2h:NS 2x:NS (team names without roles; technicians) | 3:NS | 4a:Y 4b:NS (leave area clean; hours include cleaning) | 5a:N (outside clay with approval) 5b:N (membership) 5c:NS 5d:NS | 6a:Y 6b:Y (with approval) 6c:NS | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:N (members after induction) 8b:Y (≤16 h/wk; "subject to capacity") 8c:Y (technicians on duty) 8d:NS | 9a:staff 9b:Y (firing service ~1 wk after drop-off; email when ready) 9c:drop-off (email booking) 9d:NS | 10a:Y 10b:Y (birthdays, team, corporate; whole-kiln hire £90–120) | 11:Partial (Rohde Ecotop 60L, TE-80S) | 12a:Y 12b:Y (account login) 12c:NS 12d:Y (policy page per membership) |
| 59 | Art Craft Studios | London | UK | teaching studio | https://www.artcraftstudios.co.uk/ | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS | 2a:Y 2b:Y 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (Studio Assistants, Social Media Manager) 2x:Y (Clare, "Pottery Teacher & Studio Manager") | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:Y (freestyle sessions include clay and glazes) 5c:NS 5d:NS | 6a:Y 6b:NS 6c:NS | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:Y (freestyle session packs; drop-in booking; Acuity meet & greet) 8b:NS 8c:Y (technician on hand) 8d:NS | 9a:NS 9b:NS 9c:NS 9d:NS | 10a:Y 10b:NS (team building on site or at your venue; parties page; contact us) | 11:NS | 12a:Y 12b:NS 12c:NS 12d:Y (T&Cs) |
| 60 | Bonbon Pottery | London | UK | paint-your-own-pottery (PYOP) | https://www.bonbonpottery.co.uk/faq | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS (5 studios; franchising) | 2a:NS 2b:NS 2c:NS 2d:NS 2e:Y 2f:Y 2g:NS 2h:NS 2i:Y (Pottery Worker – Night Shift; Pottery Café Assistant; Beverage Development & Training Specialist) 2x:NS | 3:NS | 4a:NS 4b:NS (PYOP) | 5a:NS 5b:NS 5c:NS 5d:NS (PYOP) | 6a:Y (all-inclusive) 6b:NS 6c:NS | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:N (walk-ins if space; booking recommended) 8b:Y (limited seating) 8c:Y 8d:NS | 9a:staff 9b:Y (~2 wks) 9c:drop-off 9d:Y (online order tracker per customer) | 10a:Y 10b:Y (kids, adults, hen, corporate; half or whole studio for 2.5 h; request form/email) | 11:NS | 12a:NS 12b:NS 12c:NS 12d:NS (customer terms only) |
| 61 | Pitter Potter / Pottery Café | London | UK | paint-your-own-pottery (PYOP) | https://pottery-cafe.com/ | 1a:NS 1b:NS 1c:NS 1d:NS 1e:Y (Fulham seats 50+, suite 20–26; 2 studios) | 2a:NS 2b:NS 2c:NS 2d:NS 2e:NS 2f:NS 2g:NS 2h:NS 2i:Y (studio assistants) 2x:NS | 3:NS | 4a:NS 4b:NS (PYOP) | 5a:NS 5b:NS 5c:NS 5d:NS (PYOP) | 6a:Y (studio fee covers materials, glazing, firing) 6b:N (no outside pottery or paints fired) 6c:NS | 7a:Y (webshop: bisque, underglazes, brushes) 7b:Y (Emma Bridgewater pottery, toys) 7c:Y 7d:Y (click & collect webshop) | 8a:N (booking recommended) 8b:Y (seating) 8c:Y (studio assistants always on hand) 8d:NS | 9a:staff 9b:Y (7–10 days) 9c:drop-off (receipt/reference) 9d:NS | 10a:Y 10b:Y (kids, adult, hen, corporate parties by phone; £100 Studio Suite hire) | 11:NS | 12a:Y 12b:NS 12c:NS 12d:NS |
| 62 | Social Pottery | London | UK | paint-your-own-pottery (PYOP) | https://socialpottery.com/memberships/ | 1a:NS 1b:NS 1c:NS 1d:NS 1e:Y (Studio 136 120-person; Studio 221 20-person; 5 studios + franchise) | 2a:Y 2b:Y 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (Assistant Manager, Studio Assistant, Barista, Freelancer) 2x:Y (Assistant Manager job: "day-to-day running") | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:Y (membership plans include clay) 5c:NS 5d:NS | 6a:Y 6b:NS 6c:NS | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:Y (members' calendar on bsport) 8b:Y (10 or 60 h/month) 8c:NS 8d:NS | 9a:staff 9b:Y (2–3 wks painted; 3–4 wks clay) 9c:drop-off 9d:NS | 10a:Y 10b:NS (hen, birthdays, kids, corporate 4–150 in studio, mobile up to 1,000; enquiry) | 11:NS | 12a:NS 12b:Y (members' calendar/members area) 12c:NS 12d:NS |
| 63 | Clay Time | London | UK | teaching studio | https://www.claytime.london/booking | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS (2 sites; not-for-profit CIC) | 2a:NS 2b:NS 2c:NS 2d:NS 2e:Y 2f:NS 2g:Y 2h:NS 2i:Y (generic "staff") 2x:NS | 3:Y (volunteers "to help us run well"; hours/trade/duties NS; get in touch) | 4a:NS 4b:NS | 5a:NS 5b:Y (course fee includes 6 glazed and fired pots) 5c:NS 5d:NS | 6a:Y (course glazes) 6b:NS 6c:NS | 7a:NS 7b:Y (studio pots sold in studio and at markets) 7c:Y 7d:NS | 8a:NS 8b:NS 8c:NS 8d:NS | 9a:staff 9b:Y (7–10 days painting; 3 wks make; 10 days after course) 9c:slip (pink or blue ticket) 9d:NS | 10a:NS 10b:NS (school and charity workshops only) | 11:NS | 12a:Y 12b:NS 12c:NS 12d:NS |
| 64 | Creative Bloc | Glasgow | UK | membership/community studio | https://creativeblocglasgow.com/ | 1a:NS 1b:Y (long waiting list, prioritised by need) 1c:NS ("our electric kiln", 80 L) 1d:Y (3 wheels + 1 tabletop) 1e:Y (main studio 16 people + front room 8) | 2a:NS 2b:NS 2c:NS 2d:NS 2e:Y 2f:NS 2g:Y 2h:NS 2i:Y (staff; "experienced potter") 2x:NS | 3:NS (volunteers supervise sessions; no scheme described) | 4a:NS 4b:NS | 5a:NS 5b:Y (practice clay included; pay per fired weight) 5c:NS 5d:NS | 6a:Y 6b:NS 6c:NS | 7a:NS 7b:NS 7c:NS 7d:NS | 8a:Y (Eventbrite; reserve wheel or table) 8b:Y (4 wheels + 4 hand-building places; 4 member sessions/month) 8c:Y (always supervised by staff or volunteer) 8d:NS | 9a:NS 9b:NS 9c:NS 9d:NS | 10a:Y 10b:Y (private group workshops up to 16+8 via form; kiln hire from £45) | 11:Partial | 12a:Y 12b:NS 12c:NS 12d:NS |
| 65 | Clay Shed | Bristol | UK | membership/community studio | https://www.clayshed.co.uk/ceramics-membership-bristol | 1a:NS 1b:NS 1c:Y (4 electric kilns) 1d:Y (7 Shimpo wheels) 1e:NS | 2a:Y 2b:NS 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (visiting tutors) 2x:NS (three founder-directors; technicians) | 3:Y (Tech Exchange: 4 h technical support/wk for membership; spec + application form) | 4a:NS 4b:NS | 5a:NS 5b:Y (courses; "extra clay" charged) 5c:NS 5d:NS | 6a:NS 6b:NS 6c:Y (glaze-mixing induction and table) | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:Y (book wheels, kilns, equipment in app) 8b:NS 8c:NS 8d:Y (24/7) | 9a:both (team fires for Flexi 4/15; Unlimited members fire own) 9b:NS 9c:app 9d:NS | 10a:Y 10b:NS (group events 4–10, from £60 pp; email) | 11:Y (kilns with makes and sizes, wheels, extruder, spray booth, slab roller) | 12a:Y 12b:Y (password-protected Members Area; app) 12c:NS 12d:NS |
| 66 | Clay Studio (Community Clay Studio Manchester CIC) | Manchester | UK | membership/community studio | https://www.claystudio.co.uk/membership | 1a:NS 1b:Y (waiting list emailed mid-month) 1c:Y (5 electric kilns) 1d:Y (8 teaching + 10 members' wheels) 1e:NS | 2a:Y 2b:NS 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (Directors; Production Potter; Administration) 2x:NS | 3:NS | 4a:Y 4b:NS (clean "thoroughly and co-operatively"; 20% surcharge pays staff to clean if not) | 5a:N (may source independently, staff-approved) 5b:N (Pathway: clay discount) 5c:NS 5d:in-studio (shop; collection only) | 6a:NS 6b:Y (with staff approval) 6c:Y (self-made glazes with approval) | 7a:Y (clay, tools) 7b:Y (members' and local makers' work; in-house production potter) 7c:Y 7d:NS | 8a:Y (Setmore; 3-h sessions, first come first served) 8b:Y (up to 4 sessions/wk) 8c:NS 8d:NS | 9a:NS 9b:Y (kiln use ~2 wks; classes 1–8 wks) 9c:drop-off (prepaid online by litre) 9d:NS | 10a:Y 10b:Y (private workshops; members' kiln hire) | 11:Y (5 kilns, 18 wheels) | 12a:Y 12b:Y (Setmore login) 12c:NS 12d:Y (T&Cs; Membership Guide) |
| 67 | Slow Clay Centre | Melbourne | Australia | teaching studio | https://www.slowclay.com/ | 1a:NS 1b:NS 1c:NS 1d:Y (up to 12 on wheels) 1e:NS | 2a:NS 2b:NS 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (director; office manager) 2x:Y (office manager) | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:Y (unlimited clay in course fees) 5c:NS 5d:NS | 6a:Y 6b:NS 6c:Y (students taught to make own glazes) | 7a:Y (trimming tool, journals) 7b:NS 7c:Y 7d:Y (online shop) | 8a:Y (SATs for enrolled students, booking required, $30) 8b:NS 8c:NS 8d:NS | 9a:staff 9b:NS 9c:NS 9d:NS | 10a:Y 10b:NS (private parties, team building, hens; up to 12 wheel + 10 hand-building) | 11:NS | 12a:Y 12b:Y (password-protected student SAT page) 12c:NS 12d:NS |
| 68 | Twelve80 | Melbourne | Australia | membership/community studio | https://twelve80.com.au/ | UNREACHABLE (script-rendered): 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS | 2a:NS 2b:NS 2c:NS 2d:NS 2e:NS 2f:NS 2g:NS 2h:NS 2x:NS | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:NS 5c:NS 5d:NS | 6a:NS 6b:NS 6c:NS | 7a:NS 7b:NS 7c:NS 7d:NS | 8a:NS 8b:NS 8c:NS 8d:NS | 9a:NS 9b:NS 9c:NS 9d:NS | 10a:NS 10b:NS | 11:NS | 12a:NS 12b:NS 12c:NS 12d:NS |
| 69 | Sunroom Studio | Melbourne | Australia | teaching studio | https://www.sunroomstudio.store/ | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS | 2a:Y 2b:NS 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2x:Y ("managed by a glorious handful of talented local artists and teachers") | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:Y (open-studio booking includes free recycled clay) 5c:NS 5d:NS | 6a:NS 6b:NS 6c:NS | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:Y (studio time by the hour or day) 8b:NS 8c:NS 8d:NS | 9a:staff (technician fires student pots) 9b:NS 9c:drop-off (prepaid by kg) 9d:NS | 10a:Y 10b:Y (birthdays, hens, team building by email/phone; full kiln hire) | 11:NS | 12a:NS 12b:NS 12c:NS 12d:Y (open-studio policies and agreements form; firing T&Cs) |
| 70 | Urth Clay Studio | Melbourne & Adelaide | Australia | teaching studio | https://urthclaystudio.com.au/pottery-studio-club-membership-adelaide/ | 1a:NS 1b:NS 1c:NS 1d:Y (15 Shimpo wheels) 1e:NS (3 sites) | 2a:NS 2b:NS 2c:NS 2d:NS 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y ("studio staff") 2x:NS | 3:NS | 4a:Y 4b:NS (leave equipment clean) | 5a:NS 5b:N (membership: clay extra) 5c:NS 5d:NS | 6a:NS 6b:NS 6c:NS | 7a:Y (wheel, tool kit, apron, sink trap) 7b:NS 7c:Y 7d:Y | 8a:NS 8b:Y (4 or 8 h/week) 8c:NS 8d:Y (24/7, passcode) | 9a:staff 9b:Y (class work 4 wks) 9c:form (firing form + photo, quote) 9d:NS | 10a:Y 10b:Y (team building, hens, kids parties; home wheel hire $145/month) | 11:Partial (15 Shimpo wheels; kiln room) | 12a:NS 12b:NS 12c:NS 12d:Y (membership rules; T&Cs) |
| 71 | Pot Dispensary | Melbourne | Australia | teaching studio | https://www.potdispensary.com.au/ | 1a:NS 1b:Y (class waitlist by email) 1c:NS 1d:Y (8 wheels) 1e:NS | 2a:Y 2b:Y 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (studio hands; glaze tech) 2x:Y (Tiarn, "studio manager & teacher") | 3:NS | 4a:Y 4b:NS (keep own area clean; leave space clean) | 5a:N (purchase or BYO, T&Cs apply) 5b:N (membership) 5c:NS (wholesale prices) 5d:WhatsApp (clay orders via team WhatsApp) | 6a:Y 6b:Y (lead/wood-ash need glaze tech permission) 6c:N (glaze lab "not included") | 7a:Y (at-home clay and painting kits) 7b:Y (own ceramics, storefront) 7c:Y 7d:Y | 8a:NS 8b:NS 8c:NS ("studio hands are not available to help") 8d:Y (24/7, key) | 9a:NS 9b:Y (classes 2–8 wks) 9c:booking (drop-off time) 9d:NS | 10a:Y 10b:Y (private workshops 1–20; whole kiln hire $180–350) | 11:Partial (8 wheels by make) | 12a:Y 12b:Y (account login) 12c:Y (team WhatsApp) 12d:Y (membership rules; firing T&Cs; safety data sheets) |
| 72 | Bisque Studios | Melbourne | Australia | teaching studio | https://bisquestudios.com/membership/ | 1a:NS ("limited spaces") 1b:NS 1c:NS 1d:NS 1e:NS | 2a:Y 2b:NS 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (Administrative Assistant; Glaze Tech; Workshop Host) 2x:N (owner "co-ordinating ... & driving our team") | 3:N ("prefer to pay employees rather than seek out volunteers") | 4a:NS 4b:NS | 5a:NS 5b:Y (classes; membership N) 5c:NS 5d:in-studio | 6a:Y (glaze library) 6b:NS 6c:NS | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:Y (student access timeslots booked; members 24 h) 8b:NS 8c:N (student open studio "unsupervised") 8d:Y (24 h except Tue evening) | 9a:staff (in-house firing service) 9b:Y (4–6 wks after workshops) 9c:NS 9d:NS | 10a:Y 10b:Y (birthdays, team building 6–20+; whole-warehouse venue hire; email) | 11:NS | 12a:Y 12b:Y (password-protected student page; login) 12c:NS 12d:Y (FAQs and policies) |
| 73 | Terrapotter Studios | Melbourne | Australia | teaching studio | https://www.terrapotterstudios.com/publicfiring | 1a:NS 1b:NS 1c:NS 1d:Y (18 + 20 wheels) 1e:Y (capacity 36 and 40 people; 2 sites) | 2a:NS 2b:NS 2c:NS 2d:NS 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y ("TPS Team"; staff with teaching masters) 2x:NS | 3:NS | 4a:Y 4b:NS (clean up equipment used) | 5a:N (any midfire clay) 5b:Y (classes/events; open studio N) 5c:Y ($2/kg recycled; $30–60/bag) 5d:in-studio | 6a:Y (+$5/kg) 6b:Y 6c:NS | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:Y (reserve in advance, online scheduler) 8b:NS 8c:NS 8d:NS | 9a:staff 9b:Y (public ~2 wks; classes 2–10 wks; pickup email) 9c:form (public firing agreement form; weigh; firing shelf) 9d:Y (firing updates on Instagram) | 10a:Y 10b:NS (birthdays, hens, corporate; sliding scale; payment link) | 11:Partial (wheel counts) | 12a:Y 12b:NS 12c:NS 12d:Y (studio policies; firing agreement) |
| 74 | Urban Clay | Melbourne | Australia | teaching studio | https://www.urbanclay.com.au/ | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS | 2a:Y 2b:NS 2c:NS 2d:NS 2e:Y 2f:NS 2g:NS 2h:NS 2x:NS (founder; "experienced teachers") | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:NS 5c:NS 5d:NS | 6a:NS 6b:NS 6c:NS | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:NS 8b:NS 8c:NS 8d:NS (classes only) | 9a:staff 9b:NS 9c:NS 9d:NS (work held until firing fees paid; disposed after 3 months) | 10a:NS 10b:NS | 11:NS | 12a:Y 12b:Y (My Account) 12c:NS 12d:Y (T&Cs incl. conduct rules) |
| 75 | Neo Ceramics | Melbourne | Australia | teaching studio | https://neoceramics.com.au/studio-access | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS | 2a:NS 2b:NS 2c:NS 2d:NS 2e:NS 2f:NS 2g:NS 2h:NS 2x:NS (about page 404) | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:Y (free reclaim clay for access and subscription) 5c:NS 5d:NS | 6a:Y (house glazes) 6b:NS 6c:NS | 7a:Y (studio equipment it fabricates: glaze tables, spray booth, stirrers) 7b:NS 7c:Y 7d:NS (equipment by email enquiry) | 8a:Y (sessions booked with a code after purchase) 8b:NS 8c:NS 8d:NS | 9a:NS 9b:Y (collection 3–4 wks after course) 9c:NS 9d:NS | 10a:NS 10b:NS | 11:NS | 12a:NS 12b:NS 12c:NS 12d:NS |
| 76 | Lygon Street Pottery | Melbourne | Australia | teaching studio | https://lygonstreetpottery.com.au/memberships | 1a:NS 1b:NS 1c:Y (2 electric + 1 gas kiln) 1d:NS 1e:Y (5 rentable 13 m² studios) | 2a:Y 2b:NS 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2x:NS (two founder-teachers; studio tech/teacher) | 3:NS | 4a:NS 4b:NS (students wipe surfaces) | 5a:N (members supply own materials) 5b:Y (courses include clay; membership N) 5c:NS 5d:NS | 6a:Y (house glazes, added to firing charge) 6b:NS 6c:NS | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:Y (casual sessions booked; members unlimited) 8b:NS 8c:NS 8d:Y (studio renters 24-hour) | 9a:staff 9b:Y (members ~fortnightly; public 10–14 days) 9c:NS 9d:NS | 10a:Y 10b:Y (private workshops/work parties; kiln hire $230–550; studios $480/month) | 11:Y (kilns with makes and sizes; wheels; pug mill; slab roller; glaze lab) | 12a:NS 12b:NS 12c:NS 12d:NS |
| 77 | Robert Gordon Australia | Melbourne | Australia | production pottery | https://www.robertgordonaustralia.com/products/paint-your-own-pottery-booking | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS (35 craftspeople; 300,000+ pieces a year) | 2a:NS 2b:NS 2c:NS 2d:NS 2e:NS 2f:Y 2g:NS 2h:NS 2i:Y (retail, production, warehouse, office teams; craftspeople) 2x:NS | 3:NS | 4a:NS 4b:NS (PYOP) | 5a:NS 5b:NS 5c:NS 5d:NS (PYOP) | 6a:Y (glaze and firing in piece price) 6b:NS 6c:NS | 7a:NS 7b:Y (own production stoneware; outlet) 7c:Y 7d:Y | 8a:Y (PYOP bookings essential) 8b:Y (limited seating) 8c:Y (front desk) 8d:NS | 9a:staff 9b:Y (2–3 wks; notified when ready) 9c:drop-off (staff add order number and date) 9d:NS | 10a:Y 10b:NS (large groups, parties, team building on request) | 11:Partial (solar-assisted electric kilns, jigger-jolly, press) | 12a:NS 12b:NS 12c:NS 12d:NS |
| 78 | Everyday Clay | Healesville VIC | Australia | teaching studio | https://www.everydayclay.com.au/glazing-firing-booking | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS (mobile studio) | 2a:Y 2b:NS 2c:NS 2d:NS 2e:NS 2f:NS 2g:NS 2h:NS 2x:N ("a one-person show") | 3:NS | 4a:NS 4b:NS | 5a:Y (firing service only for its own kit clay) 5b:NS 5c:NS 5d:online shop (DIY kits) | 6a:Y (studio's custom glazes) 6b:NS 6c:NS | 7a:Y (DIY clay kits) 7b:Y (own pottery) 7c:NS 7d:Y | 8a:NS 8b:NS 8c:NS 8d:NS (studio by appointment only) | 9a:staff (owner) 9b:Y (4–6 wks; email when ready) 9c:booking (calendar drop-off + invoice) 9d:NS | 10a:Y 10b:NS (mobile team-building, hens, birthdays, conferences 5–150; enquiry + signed agreement) | 11:NS | 12a:Y 12b:NS 12c:NS 12d:Y (studio policies) |
| 79 | Maker's Loft | Sydney | Australia | teaching studio | https://www.makersloft.com.au/pages/pottery-ceramics-studio | 1a:NS ("subject to availability") 1b:NS 1c:Y (4 electric kilns) 1d:Y (14 wheels) 1e:NS | 2a:Y 2b:NS 2c:NS 2d:NS 2e:NS 2f:NS 2g:NS 2h:NS 2i:Y (generic staff; guest "Makers") 2x:NS | 3:NS | 4a:Y 4b:NS (clean equipment and tools every visit; last out turns off lights) | 5a:N ("Own clay", shop optional) 5b:N (membership) 5c:NS 5d:online shop | 6a:Y 6b:Y ("Own Grazes" [sic]) 6c:NS | 7a:Y (clays, materials, tools online) 7b:NS 7c:Y 7d:Y | 8a:N ("no booking required" for wheels) 8b:NS 8c:NS ("not led by a teacher") 8d:Y (24/7) | 9a:staff 9b:NS 9c:form (online firing order + order sheet; request form for large work) 9d:NS | 10a:Y 10b:Y (corporate, hens, birthdays via enquiry form; front-studio hire $440–750; kiln bookings from $60) | 11:Partial (Venco & Shimpo wheels; 4 kilns) | 12a:Y 12b:Y (account login) 12c:NS 12d:Y (membership rules; firing terms) |
| 80 | Studio P | Perth | Australia | teaching studio | https://www.studiop.net.au/memberships | 1a:NS 1b:NS 1c:Y (4 electric kilns on one page, 3 on another) 1d:Y (22 wheels) 1e:NS | 2a:Y 2b:NS 2c:NS 2d:NS 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y ("Studio helper") 2x:NS | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:N (membership) 5c:NS 5d:in-studio | 6a:Y (10 studio glazes) 6b:NS 6c:NS | 7a:Y (tools, toolboxes, underglazes online) 7b:Y (handmade items in studio store) 7c:Y 7d:Y | 8a:NS 8b:NS 8c:NS 8d:Y (24/7) | 9a:NS 9b:Y (class work fired within 2 wks; kilns run several times a week; email) 9c:NS 9d:NS | 10a:Y 10b:NS (bridal, family, birthday, Christmas functions) | 11:Y (22 wheels, kilns, pug mill, extruder, slab roller) | 12a:Y 12b:NS 12c:NS 12d:NS |
| 81 | ClayMake Studio | Perth | Australia | teaching studio | https://www.claymakestudio.com.au/firing-tcs.html | 1a:NS 1b:NS 1c:Y (3 electric kilns) 1d:Y (15 wheels) 1e:NS | 2a:NS 2b:NS 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (arts facilitator) 2x:NS ("run by mother - daughter team"; ownership not stated) | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:N (drop-ins exclude clay) 5c:NS 5d:NS | 6a:Y (14 glazes and engobes) 6b:NS 6c:NS | 7a:NS 7b:NS 7c:Y 7d:NS | 8a:N (drop-in, no pre-booking) 8b:Y (Essential ≤40 h/month) 8c:NS 8d:NS | 9a:NS 9b:NS 9c:form 9d:NS | 10a:Y 10b:NS (birthdays, bridal, hens, fundraisers, corporate; contact) | 11:Y (kilns, wheels, slab roller, 3 extruders, glazes) | 12a:Y 12b:NS 12c:NS 12d:Y (T&Cs; OHS page; firing T&Cs) |
| 82 | Not Yet Perfect | Perth | Australia | teaching studio | https://www.notyetperfect.com/workshops | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS | 2a:NS 2b:NS 2c:NS 2d:NS 2e:NS 2f:NS 2g:NS 2h:NS 2x:NS | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:NS 5c:NS 5d:NS | 6a:NS 6b:NS 6c:NS | 7a:Y (At Home Pottery Box) 7b:Y (own vases, bowls, tumblers online) 7c:Y 7d:Y | 8a:NS 8b:NS 8c:NS 8d:NS (workshops only) | 9a:NS 9b:NS 9c:NS 9d:NS (firing service listed, details NS) | 10a:Y 10b:Y (corporate, social clubs, birthdays, bridal, hens; $700 minimum; booking form; wheel hire) | 11:NS | 12a:NS 12b:NS 12c:NS 12d:NS |
| 83 | Walyalup Fremantle Arts Centre | Fremantle | Australia | municipal/recreation | https://wfac.org.au/art-classes/ | 1a:NS 1b:NS 1c:NS 1d:NS 1e:NS | 2a:NS 2b:Y 2c:NS 2d:Y 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (Manager of Arts, Creative Learning Manager, Shop Manager, coordinators) 2x:Y (Studio Coordinators; City staff) | 3:NS | 4a:NS 4b:NS | 5a:N (extra clay bought locally) 5b:Y (one bag in course fee) 5c:NS 5d:NS | 6a:NS 6b:NS 6c:NS | 7a:NS 7b:Y (shop of local WA makers, online and in store) 7c:NS 7d:Y | 8a:Y (open studio is a term course, enrol online) 8b:Y (limited spots) 8c:Y (tutor present) 8d:NS | 9a:NS 9b:Y (fires weekly, best effort; email with collection times) 9c:NS 9d:NS | 10a:NS 10b:NS | 11:NS | 12a:NS 12b:NS 12c:NS 12d:Y (enrolment T&Cs, code of conduct) |
| 84 | Earth & KLĀ | Perth | Australia | teaching studio | https://earthandkla.com.au/ | 1a:NS 1b:NS 1c:Y (2 kilns, "Frank and Pietro") 1d:NS 1e:NS | 2a:Y 2b:NS 2c:NS 2d:NS 2e:Y 2f:NS 2g:NS 2h:NS 2i:Y (resident potter; "studio staff") 2x:NS | 3:NS | 4a:NS 4b:NS | 5a:NS 5b:NS 5c:NS 5d:in-studio | 6a:NS 6b:NS 6c:NS | 7a:NS 7b:Y (resident potter's tableware online) 7c:Y 7d:Y | 8a:NS 8b:NS 8c:NS 8d:NS (self-guided 2-h sessions; 10-session pass) | 9a:staff 9b:NS 9c:NS 9d:NS | 10a:NS 10b:NS | 11:Partial (two kilns) | 12a:NS 12b:NS 12c:NS 12d:Y (T&Cs) |

## Per-studio evidence

Each block lists the 12 items with the value, the page URL and a verbatim quote. A quote is only given where WebFetch returned the text verbatim. "NS" items list the pages that were checked.

<!-- STUDIO BLOCKS BELOW, IN TABLE ORDER -->

### 43 · Create Art Studio · Toronto, Canada · teaching studio

Pages fetched 2026-10-20: https://createartstudio.ca/ · https://createartstudio.ca/pottery-studio/ · https://createartstudio.ca/about-us/ · https://createartstudio.ca/careers/ · https://createartstudio.ca/art-classes-for-adults · https://createartstudio.ca/classes/clay-pottery-and-handbuilding-classes-for-adults/ · https://createartstudio.ca/creative-corporate-team-building-events/ · https://createartstudio.ca/product-category/art-and-craft-supplies/

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | No count, cap, waitlist, kiln/wheel number or area. Membership limited by space; one kiln implied | https://createartstudio.ca/pottery-studio/ | "Potters and ceramic artists with experience are welcome to apply to join our studio as a member, space permitting." · "You’ll enjoy access to create in our pottery studio and use of our electric kiln on a pay-as-you-go basis" |
| 2 Staff | Pottery Studio Manager (Thomas); pottery instructors (Thomas, Claire); "experienced staff". Non-owner running operations: **Y** (Pottery Studio Manager). No owner named | https://createartstudio.ca/about-us/ · https://createartstudio.ca/pottery-studio/ | "Thomas, Pottery Studio Manager" (image caption) · "Create Art Studio Pottery and Hand building Instructor Claire V" (alt text) · "Experienced staff to answer questions and a helpful community of members" |
| 3 Work exchange | NS. Careers page has no openings and no assistant or volunteer scheme | https://createartstudio.ca/careers/ | "We currently have no opportunities available" |
| 4 Member duties | Y: clean own space, inside the booked time. No rotation | https://createartstudio.ca/pottery-studio/ | "Leave the studio in a clean and tidy condition" · "Account for cleaning time in your booking" |
| 5 Clay | Outside clay allowed (bisque fee applies); clay sold to members at a member price; clay included in classes | https://createartstudio.ca/pottery-studio/ · https://createartstudio.ca/classes/clay-pottery-and-handbuilding-classes-for-adults/ | "Firing fees apply for bisque when clay is purchased outside our studio and for all glaze firings." · "Preferred member pricing on kiln firing, clay and glazes" · "All materials and firings are included – as always!" |
| 6 Glazes | Studio glazes available to members; high-fire only; outside glazes and mixing not stated | https://createartstudio.ca/pottery-studio/ | "Access to tools, glazes, and on-site storage for personal supplies" · "Only use high fire clay and glazes" |
| 7 Retail | Online and in-studio shop (art and craft supplies, ready-to-paint ceramics; no clay products listed); Square gift cards | https://createartstudio.ca/ · https://createartstudio.ca/product-category/art-and-craft-supplies/ | "Visit our shop online or in studio to pick up creative activities and gifts for yourself or someone you love." · "Get our gift cards in studio or online" |
| 8 Open studio | Booked in the member portal by the hour; hourly fee; staff present to answer questions | https://createartstudio.ca/pottery-studio/ | "Studio time can be booked through our booking portal for an hour or more" · "members are charged an hourly fee for each hour they’re using the studio." |
| 9 Kilns | Pay-as-you-go kiln use; loading, turnaround, request method and schedule NS | https://createartstudio.ca/pottery-studio/ | (see item 1 quote on the electric kiln) |
| 10 Events/rentals | Team events (pottery wheel, ceramic painting) in studio or on location; clay birthday parties; phone/email. Rental NS | https://createartstudio.ca/creative-corporate-team-building-events/ | "Host your next successful event at your office location, in our Toronto studio, or online" · "Learn the pottery wheel or sculpt beautiful clay creations with our inspiring instructors." |
| 11 Equipment list | Partial: wheel model and an electric kiln | https://createartstudio.ca/pottery-studio/ | "Shimpo Whisper VL Lite pottery wheels. So quiet you’d never know they’re here" |
| 12 Member comms | Member portal (Jackrabbit login). Studio rules referenced but no document visible; newsletter/group NS | https://createartstudio.ca/pottery-studio/ | "If you’re an existing member, login to our portal to sign up for time in our studio." · "Follow all studio rules" |

### 44 · Zei Pottery · Toronto, Canada · teaching studio

Pages fetched 2026-10-20: https://zeipottery.com/ · https://zeipottery.com/membership/ · https://zeipottery.com/about-us · https://zeipottery.com/refund_returns/ · https://zeipottery.com/privateevent/ · https://zeipottery.com/classes/4-weeks-classes/

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Waitlist for returning members; two sites (Queen St W, College St). No count, cap, kiln/wheel number or area | https://zeipottery.com/membership/ | "After cancellation, returning members will be added to the waitlist and accepted as space becomes available." |
| 2 Staff | Owners/founders Zey and John; "studio technicians and instructors"; separate "Instructor Login". No one else named as running operations | https://zeipottery.com/about-us · https://zeipottery.com/membership/ | "We’re Zey and John. Together, we are ZEI Pottery." · "For safety and consistency, only studio technicians and instructors may handle kiln loading and firing." |
| 3 Work exchange | NS (no jobs, volunteer or assistant page linked) | — | — |
| 4 Member duties | Y: clean up after yourself; remove finished work weekly. No rotation | https://zeipottery.com/membership/ | "Please respect other members, clean up after yourself, and do not touch other people's work." · "Please remove your finished pieces weekly to avoid congestion." |
| 5 Clay | Studio clay only; $30 per 10 kg bag, bought as needed (method NS); clay not in membership; classes use a "designated amount of clay"; events include clay | https://zeipottery.com/membership/ · https://zeipottery.com/classes/4-weeks-classes/ · https://zeipottery.com/privateevent/ | "Only clay provided by Zei Pottery Studio may be used." · "Clay is available for purchase in 10kg bags at $30 each, and can be purchased when needed." · "Throughout the program, you’ll work with a designated amount of clay and are free to create as many pieces as you can." |
| 6 Glazes | Unlimited studio glazes; outside glazes only with approval; no mixing into buckets | https://zeipottery.com/membership/ | "Studio glazes are available for unlimited use." · "Outside glazes are not allowed unless approved by the studio, to ensure safety and kiln compatibility." · "Please do not alter glaze buckets or mix in outside materials." |
| 7 Retail | Gift cards (online, emailed). No supply shop or finished-work sales seen | https://zeipottery.com/ | "Pottery class gift cards, delivered instantly by email." |
| 8 Open studio | 24/7 member access outside class times; lock the door when alone. Reservation and capacity NS. Hours listed under "Get In Touch" are not labelled staffed | https://zeipottery.com/membership/ | "Members have 24/7 access to the studio, excluding scheduled class times." · "For safety reasons, the door must be locked while using the studio alone." |
| 9 Kilns | Staff-only loading; work placed on the firing shelf with initials; class work ready 2–3 weeks after the course, by email; oversize/urgent work at staff discretion "if the kiln schedule allows" | https://zeipottery.com/membership/ · https://zeipottery.com/classes/4-weeks-classes/ · https://zeipottery.com/refund_returns/ | "The kiln may not be touched by members." · "All work must be clearly labelled with your initials before going on the firing shelf." · "You’ll receive an email notification once everything is ready for pickup, usually within 2-3 weeks after the course ends." |
| 10 Events/rentals | Private events (birthday, team/corporate, family), 3–20 guests by site; enquiry form or email, 30% deposit; no rental | https://zeipottery.com/privateevent/ · https://zeipottery.com/refund_returns/ | "Fill out the form below and we’ll get back to you with availability and next steps." · "Private events are not confirmed automatically through the website." · "A 30% non-refundable deposit is required to reserve a private event." |
| 11 Equipment list | NS (inclusions only: "Use of wheels, tools, and worktables") | https://zeipottery.com/membership/ | "Use of wheels, tools, and worktables" |
| 12 Member comms | Studio policies page that applicants must accept; email when work is ready. Newsletter/portal/group NS | https://zeipottery.com/membership/ · https://zeipottery.com/refund_returns/ | "You must read and agree to the studio policies prior to applying." · "We will email you when your work is ready for pickup." |

### 45 · Clay With Me · Toronto, Canada · teaching studio

Pages fetched 2026-10-20: https://claywithme.ca/ · https://claywithme.ca/pages/membership · https://claywithme.ca/pages/open-studio-membership-terms-conditions · https://claywithme.ca/pages/studio-policy · https://claywithme.ca/pages/energy-exchange-program-1 · https://claywithme.ca/pages/community-shop · https://claywithme.ca/pages/workwithus · https://claywithme.ca/pages/studio-rental · https://claywithme.ca/pages/faq

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Two sites (Richmond St W, Roncesvalles). Richmond is three floors plus a backyard; the wheel room fits 10–12 wheels. No member count, cap, waitlist or kiln count | https://claywithme.ca/pages/studio-rental | "This unique three-floor space plus backyard is available for hourly rentals and can accommodate up to 150 guests" · "The studio comfortably fits 10–12 pottery wheels" |
| 2 Staff | Owner (Gabby / Gabriela Angulo); Studio General Manager; Studio Coordinator (open job) who oversees daily operations; studio technician (firing requests); junior, senior and master instructors. Non-owner running operations: **Y** (by title) | https://claywithme.ca/ · https://claywithme.ca/pages/workwithus · https://claywithme.ca/pages/open-studio-membership-terms-conditions · https://claywithme.ca/pages/membership | "Gabby the owner of clay with me in toronto" (image caption) · "Communicate feedback and updates to the Studio General Manager." · "Oversee the daily operations at our studios" · "Other clay brands must be approved by the studio technician" · "Exclusive 1:1 Assistance: Access to Junior or Senior instructors based on skill level" |
| 3 Work exchange | **Y. Energy Exchange Program:** a 4-hour weekly shift for 4 hours of studio time; 6-month minimum; 16 weeks of classes required; duties: reclaim, wedging, cleaning, greeting students; apply by reaching out for an interview. Paid Studio Coordinator job: $19/h, Wed–Sun, 5–8 h a day | https://claywithme.ca/pages/energy-exchange-program-1 · https://claywithme.ca/pages/workwithus | "In exchange for 4 hours of personal studio time each week, participants commit to a 4-hour weekly shift" · "(scheduled together week by week)." · "This role requires a minimum 6-month commitment, and you’ll be given clear duties and support along the way." · "reclaiming and wedging clay, cleaning, greeting students, and other studio care responsibilities" · "Many of our participants go on to teach at Clay with Me, gaining valuable experience while continuing their energy exchange." · "Please reach out to set up an interview and explore if this is the right fit." · "$19 / hour" |
| 4 Member duties | Y: clean own wheel, table, tools and station after every session; $10 fee for glaze drips. No rotation | https://claywithme.ca/pages/open-studio-membership-terms-conditions | "Members are responsible for cleaning their wheels, tables, tools, splash pans, and workstations after every session." · "Keep bottoms of all pieces clean — $10 cleaning fee applies for glaze drips." |
| 5 Clay | Clay sold in studio at $45 per 10 kg sleeve, extra to membership; approved outside cone 6 clay allowed (one policy line says outside clay is not permitted, so the rules conflict). Class inclusion NS | https://claywithme.ca/pages/membership · https://claywithme.ca/pages/open-studio-membership-terms-conditions | "Clay: $45 per sleeve (10 kg) Additional Cost" · "Clay sold in-studio: $45 per 10 kg sleeve" · "Members may bring their own mid-fire (cone 6) clay — Tuckers or PSH recommended upon approval" · "Outside clay and unapproved glazes are not permitted." |
| 6 Glazes | House glazes included; own glazes allowed after a tested tile is approved; studio glazes must not be altered. Mixing NS | https://claywithme.ca/pages/membership · https://claywithme.ca/pages/open-studio-membership-terms-conditions | "Use of pottery wheels, handbuilding space, studio tools, house glazes, brushes, etc." · "Members may bring their own glazes; however, all glazes must be tested with a tile before firing." · "Test tiles must be approved by the studio team." |
| 7 Retail | Community Shop of local makers' work; gift cards and e-cards. No supply sales | https://claywithme.ca/pages/community-shop · https://claywithme.ca/pages/faq | "a curated shop that celebrates the work of local Canadian makers." · "we don't offer supplies for purchase and use outside of the studio." |
| 8 Open studio | Fixed weekly membership blocks (20–54 h/month); orientation required; lockbox access code; access only in package hours. Reservation tool, capacity and supervision NS | https://claywithme.ca/pages/membership · https://claywithme.ca/pages/open-studio-membership-terms-conditions | "20 hours per month \| Every Sunday 4:00pm–9:00pm" · "Upon completing the required orientation, members will be provided with a lockbox access code for studio entry." · "Studio access is only permitted during the Open Studio hours outlined in your membership package." |
| 9 Kilns | Firing request form to the studio technician; up to 3 weeks (priority 2–3 weeks); one glaze shelf per month included; pickup within 1 week, discard after 4 weeks. Who loads NS; schedule not published | https://claywithme.ca/pages/studio-policy · https://claywithme.ca/pages/membership | "A firing request form must be submitted to the studio technician prior to firing any piece." · "Firing turnaround time may take up to 3 weeks and may be longer during peak periods." · "One glaze-firing shelf per month (up to 8" tall)" |
| 10 Events/rentals | Adult, kids', bridal and corporate parties ("Get a quote"); hourly rental of the whole Richmond studio, loft, boardroom, painting room, terrace and backyard via "Request a quote" forms | https://claywithme.ca/ · https://claywithme.ca/pages/studio-rental | "From birthdays and bachelorette parties to corporate team building, bridal showers, holiday celebrations" · "Our entire Richmond studio is available for hourly rental" |
| 11 Equipment list | Partial: equipment named only in the job ad and rental page | https://claywithme.ca/pages/workwithus · https://claywithme.ca/pages/studio-rental | "Coordinate routine maintenance of pottery wheels, kilns, slab rollers, extruders, and studio equipment" · "The space includes a sink, wedging table, water cooler, and organized tool shelf" |
| 12 Member comms | Mailing list; Appstle membership dashboard; Studio Policy and membership terms; FAQ says staff cannot check individual pieces | https://claywithme.ca/pages/membership · https://claywithme.ca/pages/faq | "Join our mailing list for updates on new classes, specialty events and more from our community!" · "Access your Appstle membership dashboard to manage your subscription and preferences." · "By purchasing you are agreeing to our Studio Policy & Open Studio Membership Policy." · "Please mark your calendar, as we are unable to check the status of individual pieces due to volume." |

**Hypothesis note (inventory, assets, scheduling):** the Studio Coordinator posting lists "Monitor inventory of clay, glazes, tools, packaging, cleaning supplies, and retail products" · "Order materials and supplies before inventory runs low" · "Schedule equipment servicing and repairs" · "Help coordinate instructor schedules" · "Monitor attendance and staffing" (https://claywithme.ca/pages/workwithus).

### 46 · Parkdale Pottery · Toronto, Canada · teaching studio

Pages fetched 2026-10-20: https://parkdalepottery.ca/ · https://parkdalepottery.ca/pages/membership · https://parkdalepottery.ca/pages/energy-exchange · https://parkdalepottery.ca/pages/team · https://parkdalepottery.ca/pages/studio-policies · https://parkdalepottery.ca/pages/f-a-q · https://parkdalepottery.ca/pages/private-event-bookings · https://parkdalepottery.ca/pages/the-parkdale-pottery-collection · https://parkdalepottery.ca/pages/dupont-studio · (https://parkdalepottery.ca/pages/holiday-market returned 404)

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Two sites (Queen St W, Dupont). No count, cap, waitlist, kiln/wheel number or area | https://parkdalepottery.ca/pages/dupont-studio | "Our second location in a beautiful heritage building on Campbell Ave, just off Dupont Street in Toronto" |
| 2 Staff | Founder & Instructor; Studio Manager (Bella); Studio Assistant (Oliver); six instructors. Non-owner running operations: **Y** | https://parkdalepottery.ca/pages/team | "Bella is the Studio Manager of Parkdale Pottery." · Oliver, "Studio Assistant": "Helping to keep the studio running smoothly." · Dana Dallal, "Founder & Instructor" |
| 3 Work exchange | **Y. Time-for-time energy exchange:** a 3–4 hour weekly shift for equal studio time; clay at cost or bring your own; duties listed; "currently full"; how to apply NS | https://parkdalepottery.ca/pages/energy-exchange | "Ideally, we recommend committing to a 3 or 4 hour shift per week" · "which will grant you an equal amount of time in our studio." · "your responsibilities will encompass various studio maintenance tasks, such as clay collection, reclamation, and wedging" · "creating test tiles for new glazes, preparing slabs for workshops and ensuring general studio upkeep and cleanliness" |
| 4 Member duties | NS for members (keep belongings on own shelf). Class students clean up at the end of class | https://parkdalepottery.ca/pages/studio-policies | "The final 20–30 minutes of each class are for cleanup." |
| 5 Clay | Self-serve purchase of house clay by bag or box (price NS); not in membership; provided in classes; exchangees may bring their own | https://parkdalepottery.ca/pages/membership · https://parkdalepottery.ca/pages/f-a-q · https://parkdalepottery.ca/pages/energy-exchange | "Clay (we have a self serve option for in house stock clay for purchase by the bag or box)" · "Clay and tools for classes are provided and tasters typically include glazing and firing of one piece" · "You have the option to purchase clays from our studio at cost or bring your own." |
| 6 Glazes | Studio glazes included (Dupont, 8 rotating). Outside glazes and mixing NS | https://parkdalepottery.ca/pages/membership | "Studio glazes (8 rotating glazes)" · "Studio underglazes" |
| 7 Retail | Online merch collection (apparel and accessories) and gift cards; no clay/tool or finished-work sales seen | https://parkdalepottery.ca/pages/the-parkdale-pottery-collection · https://parkdalepottery.ca/pages/f-a-q | "We've cooked up some pottery goodness! Unveiling our new Parkdale Pottery Collection to compliment your studio practice." · "Gift cards are a popular choice for first-time guests." |
| 8 Open studio | Queen St: set members' hours (closed Thursday), firings included. Dupont: keyless entry 6am–12am, sometimes unstaffed. Orientation required. Reservations/capacity NS | https://parkdalepottery.ca/pages/membership · https://parkdalepottery.ca/pages/f-a-q | "The studio is open 7 days a week, daily from 6am - 12am with a keyless entry system." · "At the Dupont location, there may not be staff on-site when you are in the studio" · "The Queen Street location has set open studio hours and all of your firings are included in your membership price." · "Dupont offers 24/7 member access with allocated kiln shelf space." · "all members will be required to sign-up for and attend a studio orientation before starting their membership." |
| 9 Kilns | Monthly shelf allowance (2–3 half-round shelves at Dupont); class work at least two weeks; outside firing-only jobs refused when kilns are full. Loading and request method NS | https://parkdalepottery.ca/pages/membership · https://parkdalepottery.ca/pages/f-a-q | "Part Time Potters: 2 kiln shelf (half-round) included each month" · "you should expect at least **two weeks** before pickup in many cases" · "we do not accept external “firing-only” jobs when kilns are full." |
| 10 Events/rentals | Birthday and corporate workshops, minimum 6 people, $70 per person; "Private Event Booking Request" form. Rental NS | https://parkdalepottery.ca/pages/private-event-bookings | "For a minimum of 6 people, private handbuilding and wheel throwing workshops are $70 (+HST) per person." |
| 11 Equipment list | Y (Dupont) | https://parkdalepottery.ca/pages/membership | "Equipment: Wheels and Hand building space, Large Bailey Clay Extruder, Bailey Slab Roller, Spray booth" · "Slip casting table and plaster casting space." |
| 12 Member comms | Newsletter ("studio updates"); customer account "Log in"; access codes and schedules sent by email; Studio Policies page | https://parkdalepottery.ca/ · https://parkdalepottery.ca/pages/studio-policies | "We'll send you a 10% discount code for your first class, plus studio updates, new workshops and more." · "you might miss important class info like studio access codes, schedules, and important updates" |

### 47 · Pottery Lab · Toronto, Canada · teaching studio

Pages fetched 2026-10-20: https://www.potterylab.ca/ · https://www.potterylab.ca/service-page/studio-membership · https://www.potterylab.ca/about-us · https://www.potterylab.ca/policies · https://www.potterylab.ca/service-page/private-events · https://www.potterylab.ca/pottery-classes-toronto · https://www.potterylab.ca/service-page/date-night-2 · https://www.potterylab.ca/gift-card

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | 1,300 sq ft; membership subject to availability (no number). Kiln and wheel counts NS | https://www.potterylab.ca/service-page/studio-membership | "Welcome to our 1300 sq. ft. pottery studio in the creative heart of West Queen West, Toronto." · "All memberships are subject to availability." |
| 2 Staff | No names or titles. About page is an unnamed founder's first-person bio; "our team"; cleaning staff after events | https://www.potterylab.ca/about-us · https://www.potterylab.ca/policies | "Guests must exit promptly at the end of the event; cleaning staff will arrive immediately afterward." |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS (rules about bottoms and collection, no cleaning duty stated) | https://www.potterylab.ca/service-page/studio-membership | "Keep pot bottoms clean for firing" · "Pick up finished pieces weekly" |
| 5 Clay | Members bring their own cone 6 clay; classes and date nights include clay | https://www.potterylab.ca/service-page/studio-membership · https://www.potterylab.ca/service-page/date-night-2 | "Bring your own cone-6 clay (Tuckers / PSH recommended)" · "All clay, tools, and materials included" |
| 6 Glazes | House glazes included; personal glazes allowed with a test tile. Mixing NS | https://www.potterylab.ca/service-page/studio-membership | "Use of wheels, tools, tables, wax, and house glazes" · "Personal glazes allowed (test tile required)" |
| 7 Retail | eGift cards ($50–200) only | https://www.potterylab.ca/gift-card | (amounts listed; no sentence returned) |
| 8 Open studio | Plan-based access windows (e.g. 9 AM–9 PM excluding classes) and session caps. Reservation tool, supervision and keys NS | https://www.potterylab.ca/service-page/studio-membership | "Access 9 AM – 9 PM (excluding classes & events)" · "Up to 10 sessions per month (maximum 4 hours per session)" |
| 9 Kilns | Monthly kiln-shelf allowance (1–3 shelves); class work fired by the team, ready "within a few weeks"; work left over 4 weeks may be discarded. Request method and schedule NS | https://www.potterylab.ca/service-page/studio-membership · https://www.potterylab.ca/service-page/date-night-2 | "1 kiln shelf per month for glaze firing (up to 8” tall)" · "After the class, your pottery will be glazed and kiln-fired by our team." · "Typically within a few weeks after glazing and firing." |
| 10 Events/rentals | Corporate, birthday and bridal events for 8–30 guests, by email with a 40% deposit. Rental NS | https://www.potterylab.ca/service-page/private-events · https://www.potterylab.ca/policies | "Pottery Lab offers hands-on pottery experiences for corporate team building, birthdays, bridal showers" · "A 40% non-refundable deposit is required to secure your booking." |
| 11 Equipment list | Partial (wheels, tables, drying area, kiln; no makes) | https://www.potterylab.ca/service-page/studio-membership | "Dedicated drying areas and kiln" |
| 12 Member comms | Membership rules on the page; booking policies page. Newsletter/portal/group NS | https://www.potterylab.ca/service-page/studio-membership | "1-month written notice required for cancellation" |

### 48 · City of Pickering Pottery Studio · Pickering ON, Canada · municipal/recreation

Pages fetched 2026-10-20: https://www.pickering.ca/potterystudio (the city site has one pottery page; registration is on ActiveNet, already recorded in 03)

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | NS | https://www.pickering.ca/potterystudio | — |
| 2 Staff | "Studio technicians" and "staff"; no names. Non-owner running operations NS | https://www.pickering.ca/potterystudio | "Final decisions on size classification will be made by the studio technicians to ensure optimal kiln usage." · "If you have any questions, please speak with staff." |
| 3 Work exchange | NS (only the city-wide volunteer page) | — | — |
| 4 Member duties | NS | — | — |
| 5 Clay | Studio clay only; at least one bag per class; max 2 bags per session; $46.34 per block; no sales after week 7 | https://www.pickering.ca/potterystudio | "All clay must be purchased through the City of Pickering Pottery Studio." · "Other/outside clay is not permitted in the studio." · "A minimum of one bag must be purchased per class." · "Block of Clay" $46.34 (Studio Rates) |
| 6 Glazes | NS | — | — |
| 7 Retail | Clay and a tool kit sold at studio rates | https://www.pickering.ca/potterystudio | "Tool Kit" $29.35 (Studio Rates) |
| 8 Open studio | Membership required; fixed open-studio hours (Wed 4–6, Fri 4–6, Sun 2–6); independent work. Supervision, capacity, keys NS | https://www.pickering.ca/potterystudio | "Open Studio is an opportunity to practice, or improve established skills, and work independently on projects." · "Membership is required." |
| 9 Kilns | Paper firing tickets (20 bisque + 20 glaze per bag of clay), number per piece set by a posted size chart; max 6 pieces a week; at least 2 weeks; technicians classify sizes | https://www.pickering.ca/potterystudio | "A new ticketing system will be implemented to manage kiln space and maintain a steady flow of work through the studio." · "Upon purchasing a bag of clay, students will receive 20 Bisque and 20 Glaze firing tickets." · "Tickets must be submitted with each piece turned in for firing." · "Students may submit a maximum of 6 pieces per week for firing." · "Please allow at least 2 weeks for a piece to be fired." |
| 10 Events/rentals | NS for pottery (city-wide party and venue rentals are not pottery-specific) | — | — |
| 11 Equipment list | NS | — | — |
| 12 Member comms | Firing policy published on the page (effective June 27, 2025); size chart posted in the studio | https://www.pickering.ca/potterystudio | "Effective – June 27, 2025" · "Please refer to the size chart posted in the studio for guidance." · "Be familiar with the policies and procedures of the pottery studio." |

### 49 · Old Ottawa South Community Association (OSCA) · Ottawa, Canada · municipal/recreation

Pages fetched 2026-10-20: https://oldottawasouth.ca/programs/ · https://oldottawasouth.ca/program-page/adult-programs/ · https://oldottawasouth.ca/governance/staff/ · Amilia store pages (https://app.amilia.com/store/en/old-ottawa-south-community-association, …/shop/programs, …/shop/programs/134473, …/shop/products, and the 2024 activity URLs 5008325, 5008327, 5071904, which now redirect to the category index). The Fall 2026 Adult Programs page has an "Art Programs → Pottery" heading, but its listings load by script and returned only "Loading...". A Wayback snapshot could not be fetched (web.archive.org is blocked for WebFetch). **Thin evidence: the association site has almost no pottery content.**

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | NS | — | — |
| 2 Staff | Association staff only, none pottery-specific: Executive Director, Program Director, Program Coordinator, Bookkeeper, Marketing & Communications, Customer Service Supervisor, Web Support; a board and volunteers | https://oldottawasouth.ca/governance/staff/ | "Katherine Boisvert": "Program Director" · "Laura Barclay": "Program Coordinator" · "OSCA Volunteers are always looking for help." |
| 3 Work exchange | NS (no pottery volunteer scheme) | — | — |
| 4 Member duties | NS | — | — |
| 5 Clay | Pottery clay is sold through the online store. **UNVERIFIED** (search summaries of 2024 Amilia listings, not openable; wording not verbatim): clay not included in course fees, bought at the centre on the first day, $50 per 25 lb bag | https://oldottawasouth.ca/program-page/adult-programs/ | "For purchasing items like pottery clay, summer camp t-shirts and hats, and water bottles, please visit the online store." |
| 6 Glazes | NS. **UNVERIFIED** (search summary): membership includes a storage shelf, facilities, equipment, glaze materials and all firing | — | — |
| 7 Retail | Online store (Amilia) sells pottery clay and merchandise | https://oldottawasouth.ca/program-page/adult-programs/ | (see item 5 quote) |
| 8 Open studio | NS. **UNVERIFIED** (search summary): members use the studio when no classes are running; previous experience required | — | — |
| 9 Kilns | NS | — | — |
| 10 Events/rentals | NS for pottery | — | — |
| 11 Equipment list | NS | — | — |
| 12 Member comms | NS | — | — |

### 50 · The Mud Oven · Ottawa, Canada · paint-your-own-pottery (PYOP)

Own site found at https://www.mudoven.com/ (the census URL was its Tock page, which still returns 403). Pages fetched 2026-10-20: https://www.mudoven.com/ · https://www.mudoven.com/group-bookings · https://www.mudoven.com/birthday-parties · https://www.mudoven.com/paintathome · https://www.mudoven.com/about-5 · (https://www.exploretock.com/the-mud-oven-ottawa/faq/ returned 403)

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Seat waitlist through the reservation page; several kilns ("one of our kilns"); a large table seats about 12 | https://www.mudoven.com/ · https://www.mudoven.com/birthday-parties | "We also have a wait list. Accessible through our reservation page, we'll contact you in the event a spot opens up." · "Our large painting table seats around 12 kids max (shoulder to shoulder)." |
| 2 Staff | No names or titles; generic staff, bilingual staff, two staff sent for mobile sessions, extra staff for large groups | https://www.mudoven.com/about-5 · https://www.mudoven.com/group-bookings | "Mobile options below refer to instructor led activities, where we send two staff members to your location" · "Additionally, we often bring in extra staff to ensure large groups receive excellent service." |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS (PYOP; no members) | — | — |
| 5 Clay | NS (PYOP; customers paint bisque) | — | — |
| 6 Glazes | All-inclusive per-item price; staff glaze and fire | https://www.mudoven.com/ · https://www.mudoven.com/paintathome | "Pricing is per item, and is ALL INCLUSIVE." · "Return your items to us, and we'll glaze and fire your pieces." |
| 7 Retail | Gift cards online and in store; "hundreds of items" of bisque in stock; paint-at-home kit rental ($20) | https://www.mudoven.com/ · https://www.mudoven.com/paintathome | "Gift Cards are available for purchase online and in store." · "We have hundreds of items in stock at all time, with a wide range of prices to suit every budget." · "A non-refundable rental cost of $20 per kit includes brushes, paint pallets and 10 large paint bottle colours." |
| 8 Open studio (PYOP seating) | Reservations recommended (Tock), walk-ins when possible; $10 card hold per seat for some bookings; last seating 90 minutes before close | https://www.mudoven.com/ | "Reservations are highly recommended." · "Walk ins are welcome when possible." · "Last seating time is 90 minutes prior to close." · "we will require a credit card hold of $10 per seat for bookings with the following criteria:" |
| 9 Kilns | Staff glaze and fire; 5–8 days (kits ~7–10 days); email when ready; 60-day hold | https://www.mudoven.com/ · https://www.mudoven.com/paintathome | "Leave the item with us for 5-8 days to fire the piece in one of our kilns." · "You'll receive a notification by email when your piece is ready for pick up." · "We keep items for 60 days, so please ensure you pick up your items promptly!" |
| 10 Events/rentals | Groups (12+ Mon–Thu, $30 minimum spend, $10/person deposit), birthday parties (no separate room), team building, mobile sessions for 20+ ($150 instructor fee). Booked on Tock or by email. No rental | https://www.mudoven.com/group-bookings · https://www.mudoven.com/birthday-parties · https://www.mudoven.com/about-5 | "Please email us at mudgirls@mudoven.com or use our contact page HERE in order to book a group of 12 or more people." · "We do not have a separate party room. All parties are held in the main part of our studio." · "A $150 instructor fee applies to groups of 20-30 participants per 2 hour painting session." |
| 11 Equipment list | NS | — | — |
| 12 Member comms | No members. Customer policies on the home page; email notification when work is ready | https://www.mudoven.com/ | "Children under the age of 12 must be accompanied by an adult at all times." |

**Hypothesis note (inventory):** mobile bookings need "2-4 weeks advance notice to ensure we can stock the desired items" (https://www.mudoven.com/about-5), and stock "changes every week" (https://www.mudoven.com/).

### 51 · UBC Pottery Club · Vancouver, Canada · university/college/school · UNREACHABLE

Tried 2026-10-20: https://amscampusbase.ubc.ca/ubcpottery/ (TLS certificate error: the host is served by CampusGroups) · https://ams.campusgroups.com/ubcpottery/ (302 to the CampusGroups vendor home page) · guessed domains ubcpottery.com, ubcpottery.ca, ubcpotteryclub.com (none resolve). A later WebSearch showed the club's own pages are on the same host (https://amscampusbase.ubc.ca/ubcpottery/faqs/ and …/membership/); a retry of the FAQ URL failed with the same certificate error. All 12 items are recorded as NS (unreachable). Segment assigned from the census (student club).

**UNVERIFIED (search-result summaries of the club's own pages; not opened, not counted):** a 2023–24 membership page listing $60 per term for AMS members and $120 for non-AMS members, with students prioritised and community spots full; an online safety orientation because the space is used largely without supervision (paraphrase); 6 electric wheels; clay as the main extra cost, bought at cost or recycled free; about 155 members; club sales in October, December, February and April.

### 52 · Tendre Atelier · Montréal, Canada · teaching studio

Pages fetched 2026-10-20: https://www.tendreatelier.com/en/pages/pottery-studio-membership · https://www.tendreatelier.com/en/pages/faq · https://www.tendreatelier.com/en/pages/contact-ceramics-studio · https://www.tendreatelier.com/en/pages/studio-events · https://www.tendreatelier.com/en/products/wheel-throwing-pottery-introduction

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | 6 wheels; up to 6 wheel participants (classes up to 7); events up to 30. Members, kilns, area NS | https://www.tendreatelier.com/en/pages/pottery-studio-membership · https://www.tendreatelier.com/en/pages/faq | "The studio includes 6 pottery wheels" · "If you are booking a creative activity, the studio can accommodate up to 6 participants for wheel-throwing" |
| 2 Staff | Founder and teacher Marie Bourguignon; "instructors". No one else named | https://www.tendreatelier.com/en/pages/contact-ceramics-studio · https://www.tendreatelier.com/en/products/wheel-throwing-pottery-introduction | "Tendre Atelier was born from Marie's desire to create a place where imperfection is embraced and perfected." · "Guided by ceramic artist Marie Bourguignon" |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS | — | — |
| 5 Clay | Bought on site; glazes and firing bundled into each clay purchase. Must-buy and price NS | https://www.tendreatelier.com/en/pages/faq | "Clay is available for purchase on-site, with glazes and firings included with each clay purchase." |
| 6 Glazes | Included with clay purchase | https://www.tendreatelier.com/en/pages/faq | (see item 5) |
| 7 Retail | Founder's pieces sold in the studio boutique (by appointment) and a partner boutique; online shop "coming soon"; gift card | https://www.tendreatelier.com/en/pages/faq · https://www.tendreatelier.com/en/pages/contact-ceramics-studio | "Marie’s pieces are always on display and available for purchase at the studio boutique." · "Online shop coming soon" |
| 8 Open studio | Members get 9 hours a week; extended weekday hours | https://www.tendreatelier.com/en/pages/pottery-studio-membership · https://www.tendreatelier.com/en/pages/faq | "You'll have access to 9 hours per week to throw, shape, develop your projects, and grow without pressure." · "Members have access to extended studio hours during weekdays, as well as select evenings and weekends." |
| 9 Kilns | Painted pieces ready "a few weeks" after the session; loading/request NS | https://www.tendreatelier.com/en/pages/faq | "They are generally ready for pickup a few weeks after the session." |
| 10 Events/rentals | Studio reserved for corporate, team and celebration events up to 30; packages; contact form | https://www.tendreatelier.com/en/pages/faq · https://www.tendreatelier.com/en/pages/studio-events | "The studio can be reserved for a variety of creative or corporate events" · "Several packages are available depending on the type of event and the experience you’re looking to create." |
| 11 Equipment list | Partial (6 wheels) | https://www.tendreatelier.com/en/pages/pottery-studio-membership | (see item 1) |
| 12 Member comms | Newsletter signup; cancellations by email | https://www.tendreatelier.com/en/pages/pottery-studio-membership · https://www.tendreatelier.com/en/pages/faq | "Subscribe to our newsletter" (heading) · "Any request for cancellation or modification must be sent by email." |

### 53 · Atelier Forma · Montréal, Canada · teaching studio

Pages fetched 2026-10-20: https://atelierforma.ca/ · https://atelierforma.ca/pages/firing-services · https://atelierforma.ca/pages/team · https://atelierforma.ca/pages/work-with-us · https://atelierforma.ca/pages/atelier-forma-studio-policies · https://atelierforma.ca/products/membership · https://atelierforma.ca/pages/avada-faqs (FAQ loads by script; no entries returned) · https://atelierforma.ca/pages/private-events · https://atelierforma.ca/products/1-hour-studio-rental-glazing-time

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Two sites (Fabre, St-Laurent); 4-week membership shown "Sold out". No counts or area | https://atelierforma.ca/products/membership | "Sold out" (product status) |
| 2 Staff | Founder (Bojana Kolarevic) and a partner (Nicolas Mory) who built internal systems; teachers; a client-support team member; open jobs for ceramics teacher, studio technician, and Studio Assistant (work-trade). No one else named as running operations | https://atelierforma.ca/pages/team · https://atelierforma.ca/pages/work-with-us | "She is the founder of Atelier Forma" · "joined Atelier Forma as a partner in 2022" · Nico "has created and implemented multiple human-scale and computer-level systems which have helped streamline our services" · "As a studio technician, you'll be unloading kilns, selling clay, glazing pieces, cleaning up the space," |
| 3 Work exchange | **Y. Studio Assistant (work-trade):** greeting, logging information, supervising open studio, cleaning; no experience needed; apply through the online form. Hours and trade terms NS; "Free open studio time" is a listed staff perk | https://atelierforma.ca/pages/work-with-us | "Tasks include greeting customers, logging information, supervising open studio sessions, and maintaining cleanliness." · "No prior experience needed just a friendly attitude and a willingness to learn." · "Free open studio time" |
| 4 Member duties | Y: clean and mop own area, wash and store tools; start cleaning 15 minutes before the end | https://atelierforma.ca/pages/atelier-forma-studio-policies · https://atelierforma.ca/products/membership | "are expected to mop around their wheel/work area, wash and store all tools and equipment used," · "Start cleaning up at least 15 min before the end of the session to finish on time." |
| 5 Clay | Members: studio clay only, not included, bought from a team member (price NS). Firing-service customers may bring approved outside clay | https://atelierforma.ca/products/membership · https://atelierforma.ca/pages/firing-services | "Clay is not included in the fee." · "Please ask one of our team members to purchase clay." · "Clay and galzes from outside of studio is not allowed." [sic] · "Clay and glaze from outside of the studio are allowed, but you must first confirm with us..." |
| 6 Glazes | In-house glazes and all firings included for members; no outside glazes for members; mixing NS | https://atelierforma.ca/products/membership | "Included in the price: access to studio, tools, equipment and variety of our in-house glazes," · "plus firings of all work you make." |
| 7 Retail | Gift cards. No supply or finished-work shop seen | https://atelierforma.ca/ | ("GIFT CARDS" collection link) |
| 8 Open studio | Members choose three reserved hours at registration plus "flex time"; extra 2-hour open-studio slots sold to current members; 34 h (Fabre) or 60 h (St-Laurent) per 4-week session; no instruction during open time; assistants supervise | https://atelierforma.ca/products/membership · https://atelierforma.ca/pages/work-with-us | "3 reserved hours which you will choose at the day of registration." · "Extra Practice Time: Additional 2-hour open studio slots are available exclusively for members currently registered in an ongoing session." · "No instructions are available during this time." · "Please note if it is your first time at our studio, you must book a 30-minute glaze demonstration" |
| 9 Kilns | Staff-only kiln room; teacher or supervisor checks pieces; firing service by drop-off; pickup email, then 30 days; damage fee | https://atelierforma.ca/pages/atelier-forma-studio-policies · https://atelierforma.ca/pages/firing-services | "Access to kiln room is reserved for staff only." · "All pieces that enter the kiln room must be checked beforehand by the teacher or supervisor." · "Once your pieces are ready, you will receive an email to come to the studio to pick up your work." · "You will be charged a fee for any damage to the kiln caused by your work." |
| 10 Events/rentals | Private group events (birthdays, bachelorettes, teams), 5–23 people, priced by activity, size and site, via a booking-request form; 1-hour glazing "studio rental" at $35 | https://atelierforma.ca/pages/private-events · https://atelierforma.ca/products/1-hour-studio-rental-glazing-time | "Atelier Forma curates excellent events for teams and groups. Perfect for birthdays, bachelor(ette) parties, and more." · "This is a one-hour studio rental time for anyone who has already taken a full 4 or 6-week class with us" |
| 11 Equipment list | NS | — | — |
| 12 Member comms | Newsletter; customer portal (account icon); Studio Policies page; pickup email | https://atelierforma.ca/ · https://atelierforma.ca/products/membership | "Stay up to date with our latest classes and workshops. Plus get $10 off on your first purchase!" · "Your work must be picked up within 30 days after you receive our pick-up email." |

### 54 · Centred Clay Studios · Calgary, Canada · membership/community studio

Pages fetched 2026-10-20: https://centredclaystudios.com/ · https://www.centredclaystudios.com/page/page0.html (Workshops & Fees) · https://www.centredclaystudios.com/page-2/ (About) · https://www.centredclaystudios.com/page-4/ (People). The site has only these four pages plus a contact form.

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | 15 wheels, 2 kilns. Members, waitlist, area NS | https://centredclaystudios.com/ | "Studio equipment includes 15 Shimpo/Nidac RK Whisper wheels and two L&L Equad Pro production kilns" |
| 2 Staff | Two owners who are also the Studio Manager and the Operations Manager; a master potter mentor; instructors. Non-owner running operations: **N** (the owners hold both management titles) | https://www.centredclaystudios.com/page-4/ | Ashley Morrow: "Owner, Studio Manager" · Julie Vincent: "Owner, Operations Manager." · Dave Settles: "Master Potter, Mentor" |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS | — | — |
| 5 Clay | First 4-week block includes tools and a bag of clay; later price NS | https://www.centredclaystudios.com/page/page0.html | "New students: First 4-week block, a set of tools, and a bag of clay: $210 plus GST." |
| 6 Glazes | Studio glaze programme (Ravenscrag) | https://centredclaystudios.com/ | "Frema 30" slab roller and Ravenscrag glaze program." |
| 7 Retail | NS | — | — |
| 8 Open studio | Membership includes all open-studio time; fob and PIN; 24/7 on approval. Reservations and capacity NS | https://www.centredclaystudios.com/page/page0.html · https://centredclaystudios.com/ | "Membership includes access to all open studio time." · "Each student is provided with their own fob and pin code, granting secure access to the building" · "24/7 access is available on approval." |
| 9 Kilns | NS | — | — |
| 10 Events/rentals | NS | — | — |
| 11 Equipment list | Y: wheels, kilns, slab roller, glaze programme, with makes | https://centredclaystudios.com/ | (see items 1 and 6) |
| 12 Member comms | NS (sign-up through a contact form) | — | — |

### 55 · Turning Earth · London (Hoxton, Leyton, Highgate, In Production), UK · membership/community studio

Pages fetched 2026-10-20: https://www.turningearth.org/membership · https://www.turningearth.org/faq · https://www.turningearth.org/about · https://www.turningearth.org/work-for-us · https://www.turningearth.org/venue-hire · https://www.turningearth.org/in-production · https://www.turningearth.org/open-studio-pass · https://www.turningearth.org/contracts

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Four sites. In Production holds 25 makers, with 4 L&L kilns, a gas kiln, a test kiln and 10 Shimpo wheels. No member total, waitlist or area for the main studios | https://www.turningearth.org/in-production · https://www.turningearth.org/membership | "Accommodating 25 makers at a time." · "4 x L&L top-loading kilns" · "In practice, equipment has always been sufficient for the number of members." |
| 2 Staff | Technicians, tutors, an office, a Studio Manager at In Production; jobs page mentions marketing and management roles. No founder or names. Non-owner running operations: **Y** (by title) | https://www.turningearth.org/faq · https://www.turningearth.org/in-production · https://www.turningearth.org/work-for-us | "Technicians do the work that keeps the studio going: packing and unloading kilns, reclaiming and pugging clay," · "Studio Manager on-site to provide inductions in work processes." · "including marketing and management roles, especially as the studio grows." |
| 3 Work exchange | NS. Paid studio technicians also receive full membership; staffing currently full | https://www.turningearth.org/work-for-us | "Studio technicians are paid to do the tasks that keep the studio running." · "They also receive full studio membership in addition to their hourly pay." · "at the moment we have full staff levels" |
| 4 Member duties | Y: clean-up time built into sessions; apron cleaning. No rotation | https://www.turningearth.org/open-studio-pass · https://www.turningearth.org/faq | "Clean your apron after every use" (fragment) · sessions are "Three hours" plus up to 30 minutes of clean-up time |
| 5 Clay | Eight stoneware clays sold on site to members only (from Potclays); not included in membership or the pass. Must-buy and price NS | https://www.turningearth.org/membership · https://www.turningearth.org/faq · https://www.turningearth.org/open-studio-pass | "Clay you buy from us on-site, at a competitive price, since everyone gets through different amounts" · "Our clay is sold at discounted prices to our members only, so if you're not a member we can't sell you clay." · "Clay is bought separately at the studio, the same as it is for our full members." |
| 6 Glazes | 20+ ready-mixed studio glazes; technicians mix and develop glazes. Outside glazes NS | https://www.turningearth.org/membership | "We provide over 20 ready-mixed glazes developed by our technical team" · "No need to develop your own." |
| 7 Retail | Shop at Leyton selling clay and glaze materials at cost (In Production page); regular studio sales of members' work; gift vouchers | https://www.turningearth.org/in-production · https://www.turningearth.org/faq | "Shop around the corner (Turning Earth Leyton) with clay and glaze materials at cost" · "We also hold regular studio sales, where hundreds of Turning Earth artists exhibit their work." |
| 8 Open studio | Members: no booking, any time in opening hours (66 h/week), capped at 15 h/week (22 on Plus); technicians always on hand. Open-Studio Pass: sessions booked when buying; limited places | https://www.turningearth.org/membership · https://www.turningearth.org/faq · https://www.turningearth.org/open-studio-pass | "You can visit any time during opening hours with no booking required." · "Members can come in for a maximum of 15 hours a week, or 22 on Studio Membership Plus." · "There are always technicians on hand to answer questions and give advice, so you're not on your own." · "Our booking system asks you to pick a session as part of buying the pass." |
| 9 Kilns | Technicians load; communal firings run daily to a schedule; 30 L of firing a month included (50 L on Plus) | https://www.turningearth.org/membership · https://www.turningearth.org/faq | "Kiln-loading handled by technicians." · "Firings run to a schedule and can't be brought forward for one person's deadline." · "We fire kilns constantly every day and, unfortunately, we don't have the capacity to rent them out at the moment." |
| 10 Events/rentals | Studio hire for photoshoots and filming (mostly Mondays), by email; private group bookings paused; no kiln rental | https://www.turningearth.org/venue-hire · https://www.turningearth.org/faq | "They are available for hire when they’re not being used by our members (mostly on Mondays)." · "We're not taking private bookings at the moment." |
| 11 Equipment list | Y | https://www.turningearth.org/membership · https://www.turningearth.org/in-production | "Every studio has Shimpo Whisper pottery wheels, L&L electric kilns, a slab roller, extruder, and pugmill." |
| 12 Member comms | Mailing list; members' Facebook page; member handbook; contract and induction form | https://www.turningearth.org/faq | "The Turning Earth Members Facebook page is where members organise things between themselves." · "You'll also get a member handbook with the important information in it." · "When you come in for your induction you'll sign your contract and induction form." |

### 56 · The Kiln Rooms · London (Peckham Levels, Copeland Park, Bellenden Rd, Farringdon, Islington), UK · membership/community studio

Pages fetched 2026-10-20: https://www.thekilnrooms.com/ · https://www.thekilnrooms.com/drop-in · https://www.thekilnrooms.com/work-exchange-programme · https://www.thekilnrooms.com/the-team · https://www.thekilnrooms.com/faq · https://www.thekilnrooms.com/copeland-park-studio · https://www.thekilnrooms.com/collection-of-work · https://www.thekilnrooms.com/private-sessions-studio-hire · https://www.thekilnrooms.com/terms

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Five studios; membership waitlists (about 4–5 months at Bellenden Rd, 2–3 at Copeland Park); work-exchange cohort of 18–20. Counts of kilns, wheels and area NS | https://www.thekilnrooms.com/faq · https://www.thekilnrooms.com/copeland-park-studio · https://www.thekilnrooms.com/work-exchange-programme | "This varies depending on when current members leave and how many people on the list are still interested." · "We currently have a short waiting list, please email info@thekilnrooms.com for more info or to be added to the waiting list." · "Our work exchange programme consists of providing 18-20 early career ceramicists free membership" |
| 2 Staff | 37 people listed: two co-founders (Technical Director; Operations Director), a General Manager, Studio Managers per site, Assistant Studio Managers, a Technical Manager, a Bookings Manager, technicians, teachers, admin support. Non-owner running operations: **Y** (General Manager) | https://www.thekilnrooms.com/the-team | "Lisa Darrer": "General Manager" — "she ensures the smooth running of studio operations" · "Ben Cooper": "Operations Director & Co-founder" · "Stuart Carey": "Technical Director & Co-founder" · "Krista Speakman-Brown": "Bookings Manager" · "Sean Bruno": "Studio Manager – Copeland Park & teacher" |
| 3 Work exchange | **Y.** 7 hours a week of hands-on studio support with a technician, for free membership, free firings "within reason", free glazes, studio-management training, occasional paid work; apply by CV to workexchange@; no spaces now | https://www.thekilnrooms.com/work-exchange-programme | "In exchange we ask for 7 hours a week hands on studio support alongside one of our technicians." · "having free, extended access to the studio with generous storage space, firings are free (within reason)" · "We spend time training people in how to manage a studio, mix glazes, pack/service/manage kilns" · "If you're interested please send a CV to workexchange@thekilnrooms.com" |
| 4 Member duties | Y: clean own and communal areas after use; drop-in clean-up starts 15 minutes before the end | https://www.thekilnrooms.com/terms · https://www.thekilnrooms.com/drop-in | "Members are expected to clean their work area and communal areas after use." · "You must clean your work area and shared spaces." |
| 5 Clay | Materials store at Bellenden Rd sells clays "just above cost"; materials not included in Copeland membership; personal materials need staff approval; classes and private sessions include materials | https://www.thekilnrooms.com/terms · https://www.thekilnrooms.com/copeland-park-studio · https://www.thekilnrooms.com/private-sessions-studio-hire | "Materials store located at Bellenden Rd – with a variety of materials and clays sold just above cost price." · "Firings & materials are not included and will be charged at cost price." · "All prices include materials, firings and VAT, so there are no additional costs." |
| 6 Glazes | In-house glazes; personal glazes with staff approval; glaze materials sold (members can mix); exchangees trained to mix glazes | https://www.thekilnrooms.com/terms · https://www.thekilnrooms.com/copeland-park-studio | "Personal materials, glazes or equipment must be approved by staff and stored correctly." · "We stock a range of clays and glaze materials at our Bellenden Road studio, which is a 5mins walk away." |
| 7 Retail | Materials store; members' work sold twice a year; vouchers | https://www.thekilnrooms.com/faq · https://www.thekilnrooms.com/ | "Not regularly. We sell members' work twice a year during our Christmas Sale and Summer Show weekends." · "Treat someone to a voucher or credit note" |
| 8 Open studio | Drop-ins (course students only) booked through per-studio Bookwhen links, capped numbers, 4.5-hour sessions, technician always present. Members: sign in/out, 18 h/week cap at part-time studios; Copeland Park key holders 7am–11pm with minimal support | https://www.thekilnrooms.com/drop-in · https://www.thekilnrooms.com/terms · https://www.thekilnrooms.com/copeland-park-studio | "Please use the links below to book a session in one of our studios:" · "Numbers are capped to ensure adequate space." · "There will not be a teacher available onsite however a technician is always present to answer basic studio questions." · "Maximum studio use: 18 hours per week" · "Access anytime between 7am-11pm, 7 days a week with no limit on usage (each member is a key holder)." · "Wheels and areas cannot be reserved" · "A £300 refundable deposit is required." (Copeland Park) |
| 9 Kilns | Copeland Park induction covers kiln packing; Bellenden Rd firings 2–3 weeks (aim 5–10 days), 50 L a month; class work about 10 days per firing; email when ready; 21 days to collect | https://www.thekilnrooms.com/copeland-park-studio · https://www.thekilnrooms.com/terms · https://www.thekilnrooms.com/collection-of-work | "In depth induction and training, including kiln packing and best practice studio management" · "Bellenden Rd: Firings normally take 2–3 weeks, though we aim for 5–10 days where possible." · "You will receive an email informing you when the work is ready for collection." · no outside firing: it cannot fire work "that isn't made by our members or on a Kiln Rooms course." (https://www.thekilnrooms.com/faq) |
| 10 Events/rentals | Team building, corporate events, parties, masterclasses and photoshoots for 5–22 people, by email or phone; a wheel-hire page | https://www.thekilnrooms.com/private-sessions-studio-hire | "team building days, group classes, corporate events, parties, masterclasses and photoshoots" · "The maximum we can host in a studio is 22 people." |
| 11 Equipment list | Y (Copeland Park) | https://www.thekilnrooms.com/copeland-park-studio | "plaster facilities, glaze booth, compressed air, extruder, slab roller" |
| 12 Member comms | Mailing list; Terms & Conditions for members; email notices | https://www.thekilnrooms.com/terms | "we will notify members by email of any significant changes" |

### 57 · Studio Pottery London · London, UK · teaching studio

Pages fetched 2026-10-20: https://www.studio-pottery-london.com/ · https://www.studio-pottery-london.com/members/ · https://www.studio-pottery-london.com/members-shop/ · https://www.studio-pottery-london.com/the-team/ · https://www.studio-pottery-london.com/studio-hire/ · https://www.studio-pottery-london.com/shop/ · https://www.studio-pottery-london.com/corporate-off-site/

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | 14-wheel classroom; one kiln and glazing room. Members, waitlist, area NS | https://www.studio-pottery-london.com/ | "Our classroom of 14 wheels ensuring intimate classes." · "In the rear of the studio is the kiln and glazing room, monitored by our in-house technician." |
| 2 Staff | Director, Artistic Director, Studio Manager, lead teachers, teachers, four studio technicians. Non-owner running operations: **Y** (Studio Manager) | https://www.studio-pottery-london.com/the-team/ | "Lucy Attwood, Studio Pottery's Director" · "Becky Harle, Studio Manager" · "Lucas Buttchewits, Studio Technician" · "Jago Poynter, Lead Teacher" |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS ("cleaning and waste systems" listed as a facility only) | https://www.studio-pottery-london.com/members/ | — |
| 5 Clay | Several clays bought through the studio; not in the membership inclusions; price NS | https://www.studio-pottery-london.com/members/ | "White, buff and black stoneware, crank, APC and porcelain clays from established Stoke-on-Trent suppliers" (continues "can be purchased via the studio") |
| 6 Glazes | In-house glazes and all firing included | https://www.studio-pottery-london.com/members/ | "Reasonable use of in-house glazes and firings" · "Includes all glazing and firing of work" |
| 7 Retail | Artists', members' and studio shops, in person and online; historic and contemporary ceramics sold to visitors | https://www.studio-pottery-london.com/shop/ · https://www.studio-pottery-london.com/members-shop/ · https://www.studio-pottery-london.com/ | "Our online shop is a broader platform presenting a wide range of ceramics" · the members' shop "invites friends, peers, and colleagues around the world to buy work from our membership." · "Curated window and vitrine displays of historic and contemporary ceramics inspire our makers and are for sale to visitors." |
| 8 Open studio | Up to 18 h a week in opening hours; full-time technician; tutors and technicians on hand. Reservations NS | https://www.studio-pottery-london.com/members/ | "Maximum studio usage is 18 hours per week" · "Full-time ceramic technician on-site" · "Expert tutors and technicians are always on-hand for advice" |
| 9 Kilns | Technician monitors the kiln room; communal bisque and high-fire glaze firings. Loading, turnaround, requests NS | https://www.studio-pottery-london.com/members/ | "Communal firings are 1000oC and high-temperature glaze firings" |
| 10 Events/rentals | Corporate sessions up to 60 (£150–185 per person, by email); exclusive studio hire for up to 150 (£3,500 half day, £5,000 day) | https://www.studio-pottery-london.com/corporate-off-site/ · https://www.studio-pottery-london.com/studio-hire/ | "We can host groups of up to 60 guests and tailor your session to suit your needs" · "Exclusive day hire library and classroom £5,000" |
| 11 Equipment list | Partial | https://www.studio-pottery-london.com/ | (see item 1) |
| 12 Member comms | Mindbody "CLIENT LOGIN"; membership terms and conditions | https://www.studio-pottery-london.com/ · https://www.studio-pottery-london.com/members/ | "For a comprehensive summary of what membership includes, please read our terms and conditions." |

### 58 · Trace Ceramics · London (Hackney Wick), UK · membership/community studio

Pages fetched 2026-10-20: https://www.traceceramics.co.uk/monthly-membership · https://www.traceceramics.co.uk/policy-1 · https://www.traceceramics.co.uk/studio-maker-policy · https://www.traceceramics.co.uk/trace-ceramics-faq · https://www.traceceramics.co.uk/about · https://www.traceceramics.co.uk/kiln-hire

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | A monthly member limit (no number); two Rohde kilns (60 L and 80 L). Wheels, area NS | https://www.traceceramics.co.uk/monthly-membership · https://www.traceceramics.co.uk/kiln-hire | "To maintain a comfortable working environment, we accept a limited members per month." · "Rohde Ecotop 60LA compact top-loading kiln, ideal for smaller batch firings." |
| 2 Staff | Team listed by name only; technicians on duty. Non-owner running operations NS | https://www.traceceramics.co.uk/about · https://www.traceceramics.co.uk/trace-ceramics-faq | "Our technicians can help with general studio procedures and technical questions." |
| 3 Work exchange | NS | — | — |
| 4 Member duties | Y: leave wheel, table and area clean; weekly hours include cleaning | https://www.traceceramics.co.uk/studio-maker-policy | "Please leave your wheel, table, tools and surrounding area clean and ready for the next maker." · "Up to 16 hours per week, including cleaning." |
| 5 Clay | Not included; outside clay needs approval; price NS | https://www.traceceramics.co.uk/studio-maker-policy · https://www.traceceramics.co.uk/trace-ceramics-faq | "Clay is purchased separately." · "Please check with us before using any outside clay or glaze in our kilns." |
| 6 Glazes | Designated studio glazes included; others charged; outside glazes with approval | https://www.traceceramics.co.uk/studio-maker-policy | "Only the designated selection of studio glazes is included." |
| 7 Retail | Gift card page. No shop seen | https://www.traceceramics.co.uk/monthly-membership | ("GIFT CARD" navigation link) |
| 8 Open studio | No booking after induction; up to 16 h/week; capacity-dependent; technicians on duty | https://www.traceceramics.co.uk/monthly-membership · https://www.traceceramics.co.uk/studio-maker-policy | "Booking: You are welcome to come anytime during the membership opening." · "Studio time is subject to capacity, occasional closures and any advance booking arrangements" |
| 9 Kilns | Monthly allowance (30 L bisque + 2 kg glaze); team handles firing; firing service booked by email, drop-off by appointment, about one week to the kiln, email when ready, 7 days to collect | https://www.traceceramics.co.uk/studio-maker-policy · https://www.traceceramics.co.uk/kiln-hire | "Each paid membership month includes up to 30L of bisque firing capacity and 2kg of glaze firing." · "Your work usually goes into the kiln about one week after drop-off." · "We'll email you when your work is ready." · "The fastest way to book is by email." |
| 10 Events/rentals | Private sessions, birthdays, team and corporate workshops; whole-kiln hire (£90 Ecotop, £120 TE-80S) and firing for outside makers | https://www.traceceramics.co.uk/trace-ceramics-faq · https://www.traceceramics.co.uk/kiln-hire | "Yes. We offer private sessions, birthdays, team activities, corporate workshops and customised group experiences." · "We offer kiln firing for independent makers, students and small studios who don't have access to a kiln of their own." |
| 11 Equipment list | Partial (two kilns with makes and sizes) | https://www.traceceramics.co.uk/kiln-hire | (see item 1) |
| 12 Member comms | Newsletter; account login; separate policy pages per membership type | https://www.traceceramics.co.uk/monthly-membership · https://www.traceceramics.co.uk/policy-1 | "Updates on workshops, classes, and things happening in the studio." · "Here you’ll find the terms for each membership and studio access option." |

### 59 · Art Craft Studios · London (Trinity Buoy Wharf), UK · teaching studio

Pages fetched 2026-10-20: https://www.artcraftstudios.co.uk/ · https://www.artcraftstudios.co.uk/pottery · https://www.artcraftstudios.co.uk/about · https://www.artcraftstudios.co.uk/freestyle · https://www.artcraftstudios.co.uk/corporate-events (course detail pages are hosted on ClassBento, a marketplace, and were not used)

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | NS | — | — |
| 2 Staff | Owner (also teacher); Pottery Teacher & Studio Manager; Studio Assistants; Social Media Manager; teachers; a technician at freestyle sessions. Non-owner running operations: **Y** (Studio Manager) | https://www.artcraftstudios.co.uk/about · https://www.artcraftstudios.co.uk/freestyle | Amanda: "OWNER, POTTERY TEACHER & EVERYTHING ELSE INBETWEEN" · Clare: "POTTERY TEACHER & STUDIO MANAGER" · Khadijah: "Studio Assistant" · "A technician will be on hand for support & guidance but you will not be taught how to create." |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS | — | — |
| 5 Clay | Freestyle sessions include stoneware clay and glazes; firing extra at 50p per cm | https://www.artcraftstudios.co.uk/freestyle | "We have stoneware clay in red, black, white & lava, with glazes included." · "Kiln Firing 50p per cm (Extra)" |
| 6 Glazes | Included in freestyle | https://www.artcraftstudios.co.uk/freestyle | (see item 5) |
| 7 Retail | Gift vouchers | https://www.artcraftstudios.co.uk/ | ("Gift Vouchers" navigation link) |
| 8 Open studio | Freestyle: buy 1–10 sessions, book drop-ins; free meet-and-greet on Acuity; technician on hand; half a shelf | https://www.artcraftstudios.co.uk/freestyle | "You can buy 1, 3 or 5, sessions (valid 100 days) or 10 sessions (valid 150 days)" · "½ a shelf for your artwork between sessions." |
| 9 Kilns | NS beyond the per-cm fee | — | — |
| 10 Events/rentals | Corporate team building at the studio or the client's site; parties ("socials") page; contact form | https://www.artcraftstudios.co.uk/corporate-events | "Looking to book your next team-building event? Look no further, we can you come to you or you can come to us!" |
| 11 Equipment list | NS | — | — |
| 12 Member comms | Mailchimp newsletter; T&Cs page | https://www.artcraftstudios.co.uk/ | (footer "Newsletter" link to mailchi.mp) |

### 60 · Bonbon Pottery · London (5 studios), UK · paint-your-own-pottery (PYOP)

Pages fetched 2026-10-20: https://www.bonbonpottery.co.uk/faq · https://track.bonbonpottery.co.uk/ · https://careers.bonbonpottery.co.uk/ · https://www.bonbonpottery.co.uk/pickup-and-care · https://www.bonbonpottery.co.uk/locations · https://www.bonbonpottery.co.uk/bon-party · https://www.bonbonpottery.co.uk/sessions

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Five London studios (Wembley opening soon); a franchise programme. Seats, kilns, area NS | https://www.bonbonpottery.co.uk/locations | (five locations listed) |
| 2 Staff | Job titles: Pottery Worker – Night Shift, Pottery Teacher, Pottery Café Assistant, Beverage Development & Training Specialist; instructors; a reception desk and kiln room. No managers named | https://careers.bonbonpottery.co.uk/ · https://www.bonbonpottery.co.uk/sessions | "Pottery Worker - Night Shift (Wembley)" · "Whether you're making coffee, glazing mugs in the kiln room, or welcoming guests with a smile at the reception desk," · "Guided by a Bonbon instructor throughout" |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS (PYOP) | — | — |
| 5 Clay | NS (PYOP; the foundation course covers "clay prep") | — | — |
| 6 Glazes | All-inclusive price covers painting, glazing and firing | https://www.bonbonpottery.co.uk/faq | "The price of your ceramic is all-inclusive, covering painting, glazing and firing. It's the only thing you pay for." |
| 7 Retail | Square eGift cards (never expire); café menu; postage of finished pieces for a fee | https://www.bonbonpottery.co.uk/faq · https://www.bonbonpottery.co.uk/pickup-and-care | "No, our gift cards never expire." · "If collecting isn't practical, we can post your pottery for a fee." |
| 8 Open studio (PYOP seating) | Booking recommended, walk-ins if space; limited seating | https://www.bonbonpottery.co.uk/faq · https://www.bonbonpottery.co.uk/locations | "At weekends we strongly recommend booking online, as we get very busy and walk-in space may be limited." · "If a studio has space on the day, walk-ins are welcome." |
| 9 Kilns | Staff glaze and fire; around two weeks; **online order tracker** (email, store, session date, session type) and email notifications; unfinished pieces disposed of after 28 days | https://www.bonbonpottery.co.uk/sessions · https://www.bonbonpottery.co.uk/pickup-and-care · https://track.bonbonpottery.co.uk/ · https://www.bonbonpottery.co.uk/faq | "We glaze and fire your piece, ready for pickup in around 2 weeks" · "Rather than calling the studio, you can check the status of your pieces online at any time through our order tracker." · "After submission, you will receive an email confirming the status of your item(s)." · "We'll email you when your piece is ready. Please bring your collection notification email when you visit." |
| 10 Events/rentals | Kids' and adult parties, hen dos, corporate events; half or whole studio for 2.5 h; booking page "Request" button or email | https://www.bonbonpottery.co.uk/bon-party | "Birthdays, hen dos, baby showers, corporate events and team socials." · "Half or the whole studio to yourselves for 2.5 hours" · "Questions before booking? Email party@bonbonpottery.co.uk" |
| 11 Equipment list | NS | — | — |
| 12 Member comms | No members; customer terms and refund policy; status emails | https://www.bonbonpottery.co.uk/faq | — |

### 61 · Pitter Potter / Pottery Café · London (Fulham, Battersea), UK · paint-your-own-pottery (PYOP)

Pages fetched 2026-10-20: https://pottery-cafe.com/ · https://pottery-cafe.com/faqs/ · https://pottery-cafe.com/work-with-us/ · https://pottery-cafe.com/book-your-party-2/ · https://pottery-cafe.com/pottery-cafe-at-home/

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Two studios; Fulham seats 50+ plus a 20–26-seat Studio Suite | https://pottery-cafe.com/ · https://pottery-cafe.com/book-your-party-2/ | "With a studio seating upwards of 50 people and also a separate Studio Suite" · "The room seats between 20-26 painters and is available at the weekends only." |
| 2 Staff | Studio assistants; no names or titles | https://pottery-cafe.com/faqs/ | "studio assistants are always on hand to give advice" |
| 3 Work exchange | NS (jobs: "send us your CV") | https://pottery-cafe.com/work-with-us/ | "you're hard-working and enthusiastic, please feel free to send us your CV." |
| 4 Member duties | NS (PYOP) | — | — |
| 5 Clay | NS (PYOP) | — | — |
| 6 Glazes | Studio fee covers materials, glazing and firing; outside pottery or paints are not fired | https://pottery-cafe.com/ · https://pottery-cafe.com/pottery-cafe-at-home/ | "to cover all materials plus glazing and firing of all items decorated that session." (fragment) · "we cannot fire any other pottery, or our pottery painted with any other type of paint." |
| 7 Retail | Emma Bridgewater pottery and toys in store; webshop for paint-at-home bisque, underglazes and brushes (click & collect); Square gift cards | https://pottery-cafe.com/ · https://pottery-cafe.com/pottery-cafe-at-home/ · https://pottery-cafe.com/faqs/ | "browse a wide selection of Emma Bridgewater pottery, and a fantastic range of traditional and soft toys." · "Now you can buy your pottery online and it will be ready to pick up from your selected studio from the next working day." · "the webshop is just for Paint Pottery at Home orders" |
| 8 Open studio (PYOP seating) | Booking recommended; tables held 20 minutes | https://pottery-cafe.com/faqs/ | "Yes please, it will guarantee you a table, especially at busy times." |
| 9 Kilns | Staff dip, wipe and load; collection about 7–10 days after painting with receipt or reference number; kept 30 days, then donated | https://pottery-cafe.com/faqs/ · https://pottery-cafe.com/pottery-cafe-at-home/ | "Your pottery has to be hand dipped in glaze, dried, foot wiped and loaded into the kiln." · "anybody can collect on your behalf as long as they have the receipt or know your reference number." · "if your pottery is uncollected after those 30 days it will be donated to a local charity" |
| 10 Events/rentals | Children's (8+), adult, hen and corporate party deals (£37.95 a head) booked by phone; £100 Studio Suite private-room hire (Fulham) | https://pottery-cafe.com/book-your-party-2/ | "For more information or to book a party, please call the Fulham studio directly on 020 7736 2157." · "Please note this is an additional fee for the private room hire only." · "We take a deposit of £5 per painter" (FAQ, https://pottery-cafe.com/faqs/) |
| 11 Equipment list | NS | — | — |
| 12 Member comms | Newsletter ("news and offers"); no members | https://pottery-cafe.com/ | "Sign up to receive news and offers!" |

### 62 · Social Pottery · London and Milton Keynes (5 studios), UK · paint-your-own-pottery (PYOP)

Pages fetched 2026-10-20: https://socialpottery.com/memberships/ · https://socialpottery.com/frequently-asked-questions/ · https://socialpottery.com/careers/ · https://socialpottery.com/pickup/ · https://socialpottery.com/about-us/

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Five studios (one franchised), Brent Cross opening; Kentish Town is a 120-person flagship, Camden a 20-person space; 300–500 ceramic blanks in the collection | https://socialpottery.com/about-us/ | "Our 120-person flagship and one of the largest pottery painting studios in London." · "a 20-person space built around pottery painting and connection." |
| 2 Staff | Founder; co-owner of the Camden studio; creative/marketing lead; studio managers, teachers and technicians mentioned as a group; jobs for Senior/Junior Pottery Teacher, Studio Assistant, Senior Barista, Assistant Manager. Non-owner running operations: **Y** (Assistant Manager posting) | https://socialpottery.com/about-us/ · https://socialpottery.com/careers/ | "Marian Walteros · Founder." · "Together with our studio managers, artists, potters, teachers and technicians" (fragment) · Assistant Manager "plays a key role in the successful day-to-day running of the Social Pottery studio" |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS | — | — |
| 5 Clay | Membership plans include clay | https://socialpottery.com/memberships/ | "Every plan includes wheels, clay and tools." |
| 6 Glazes | Studio underglazes for painting; staff glaze and fire | https://socialpottery.com/frequently-asked-questions/ | "We glaze it and fire it in our kiln, and it comes back a finished, food-safe keepsake." |
| 7 Retail | Digital gift vouchers (12 months); café (barista roles). No shop | https://socialpottery.com/frequently-asked-questions/ | "Vouchers are delivered digitally via email and can be printed at home." |
| 8 Open studio | Members book hours in the members' calendar (bsport); Gold 10 h/month, Platinum 60 h; £15 drop-in hour; making sessions booking-only, painting walk-ins welcome | https://socialpottery.com/memberships/ · https://socialpottery.com/frequently-asked-questions/ | "Reserve studio time in the members calendar using your hours whenever suits you." · "Wheel and hand-building sessions run with a tutor and are booking-only." |
| 9 Kilns | Staff glaze and fire; painted work 2–3 weeks, clay work 3–4 weeks; 30 days to collect, then donated. The FAQ promises a message when ready, but the collection page says none is sent for painted pottery (clay pieces get a message) | https://socialpottery.com/frequently-asked-questions/ · https://socialpottery.com/pickup/ | "Painted pottery is ready in 2–3 weeks, sooner at some studios, and we message you the moment it is." · "No message goes out: count from your session date, and call the studio if you want to check before travelling." · "We message you when it is ready, and we can ship it if you would rather not collect." |
| 10 Events/rentals | Hen, birthday, kids', baby-shower, date-night and corporate events, 4–150 in studio and up to 1,000 off site; mobile sessions; 50% booking fee | https://socialpottery.com/frequently-asked-questions/ | "Team events from 4 to 150 guests in-studio, and up to 1000 off-site." · "Yes, mobile sessions for groups of 10+, with a £50–£100 travel fee depending on location." · "Corporate and private events: a 50% booking fee is due 15 days before the event and is non-refundable;" |
| 11 Equipment list | NS | — | — |
| 12 Member comms | Members' calendar and "Members area"; members@ email | https://socialpottery.com/memberships/ | "Buy a plan, book studio hours with your membership, and use the wheels and hand-building benches at your own pace." |

### 63 · Clay Time · London (Islington, Highgate), UK · teaching studio (not-for-profit CIC)

Pages fetched 2026-10-20: https://www.claytime.london/ · https://www.claytime.london/volunteers · https://www.claytime.london/about · https://www.claytime.london/copy-of-are-my-pots-ready · https://www.claytime.london/community

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Two sites. No counts or area | — | — |
| 2 Staff | Four long-standing teachers; "experienced staff"; volunteers; no names or titles | https://www.claytime.london/ · https://www.claytime.london/about | "We have been teaching pottery for over 10 years with the same 4 teachers." · "Clay Time's experienced staff will offer assistance on a one to one basis." |
| 3 Work exchange | **Y (volunteers)**, details NS; "get in touch" | https://www.claytime.london/volunteers | "We are a not-for-profit community pottery studio that is in need of volunteers to help us run well." · "If you’re happy to get into clay and contribute to the community, we welcome new members to our team!" |
| 4 Member duties | NS | — | — |
| 5 Clay | Course fee includes six glazed and fired pots; stoneware fired to cone 7–8 | https://www.claytime.london/ | "6 glazed and fired pots are included in your course fee." |
| 6 Glazes | Course glazes; students layer them | https://www.claytime.london/ | "Some of these were created by our students through mixing and layering some basic course glazes" |
| 7 Retail | Studio-made pots sold at the studio and markets; gift card and voucher | https://www.claytime.london/ | "Our pebble pots and a variety of handcrafted treasures are on sale this weekend!" · "Come and grab a bargain with 25% off all pots." |
| 8 Open studio | NS (painting drop-ins at Islington) | https://www.claytime.london/ | "Drop in at our islington location: 168 Blackstock Road N5 1HA" |
| 9 Kilns | Staff fire; posted turnarounds by session type; coloured paper tickets; no notification (phone to check) | https://www.claytime.london/copy-of-are-my-pots-ready | "Painting sessions: 7-10 days" · "Make & Paint sessions: 3 weeks (please bring your pink or blue ticket)" · "If you would like to check if your pieces are ready before you make the journey to our studio, please call us at" |
| 10 Events/rentals | NS (school, nursery and charity workshops only) | https://www.claytime.london/community | "We work with local charities to offer clay and painting sessions." |
| 11 Equipment list | NS | — | — |
| 12 Member comms | Mailing list | https://www.claytime.london/ | "Join our mailing list!" |

### 64 · Creative Bloc · Glasgow (Maryhill), UK · membership/community studio (social enterprise)

Own site found by trying likely domains (WebSearch was exhausted): https://creativeblocglasgow.com/. The census Eventbrite listing (https://www.eventbrite.co.uk/e/open-access-ceramic-studio-tickets-1120144294739) shows sales ended 15 Mar 2025. Pages fetched 2026-10-20: https://creativeblocglasgow.com/ · https://creativeblocglasgow.com/membership/ · https://creativeblocglasgow.com/open-studio/ · https://creativeblocglasgow.com/about/ · https://creativeblocglasgow.com/kiln-hire/ · https://creativeblocglasgow.com/event-hire/

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Long membership waiting list, prioritised by need; 3 freestanding wheels + 1 tabletop; main studio holds 16, front room 8; one 80 L electric kiln described | https://creativeblocglasgow.com/membership/ · https://creativeblocglasgow.com/open-studio/ · https://creativeblocglasgow.com/event-hire/ | "We currently have a long waiting list for membership" · "We have 3 professional freestanding wheels, 1 tabletop wheel and space for 4 people to be hand-building or glazing." · "The main studio can accommodate 16 people" |
| 2 Staff | No names or titles; staff, volunteers, tutors, "experienced potter"; founded by artists | https://creativeblocglasgow.com/membership/ · https://creativeblocglasgow.com/about/ | "Members sessions are untaught but always supervised by an experienced member of staff or volunteer." · "Founded by artists with over 20 years experience in making, curating and community organizing..." |
| 3 Work exchange | NS (volunteers supervise sessions, but no programme described) | — | — |
| 4 Member duties | NS | — | — |
| 5 Clay | Clay and glazes included; members and open-studio users pay only for fired work (£1.10 per 100 g) | https://creativeblocglasgow.com/membership/ · https://creativeblocglasgow.com/open-studio/ | "We provide all the tools and materials you need to get started including clay and glazes." · "You can use as much clay as you like to practice with and only pay for pieces that go on to be fired in the kiln." |
| 6 Glazes | Included (slips, underglazes, glazes) | https://creativeblocglasgow.com/open-studio/ | "You will have access to all studio tools and materials including clay and a selection of slips, underglazes and glazes." |
| 7 Retail | NS | — | — |
| 8 Open studio | Members: 4 sessions a month (£5 extra each), always supervised. Open Studio (from Oct 2026): booked on Eventbrite, reserve a wheel or table | https://creativeblocglasgow.com/membership/ · https://creativeblocglasgow.com/open-studio/ | "4 member-only studio sessions per month" · "Our new Open Studio Sessions launch in October 2026 and are now available to book on Eventbrite." · "When you book you can choose whether to reserve a wheel or table." |
| 9 Kilns | NS (firing charged by weight after glaze firing) | https://creativeblocglasgow.com/membership/ | "This is charged by weight after the final glaze firing and currently costs £1.10 per 100g." |
| 10 Events/rentals | Private group workshops (team building, birthdays) via enquiry form; kiln hire from £45 via quote form | https://creativeblocglasgow.com/event-hire/ · https://creativeblocglasgow.com/kiln-hire/ | "Consider a creative workshop for your next team building day, birthday party or social event." · "Our electric kiln is available for hire at our Maryhill studio." · "Prices start at £45 for a low/bisque firing." |
| 11 Equipment list | Partial (wheels; kiln size and cone) | https://creativeblocglasgow.com/kiln-hire/ | "The chamber is approximately 48 x 46cm or 80L and we have numerous shelves and kiln furniture for different configurations." |
| 12 Member comms | Mailing list | https://creativeblocglasgow.com/ | "Join Our Mailing List" |

### 65 · Clay Shed · Bristol, UK · membership/community studio

Pages fetched 2026-10-20: https://www.clayshed.co.uk/ceramics-membership-bristol · https://www.clayshed.co.uk/facilities · https://www.clayshed.co.uk/about-clay-shed-bristol · https://www.clayshed.co.uk/clay-shed-terms-and-conditions · https://www.clayshed.co.uk/members-area-files (401, password-protected) · https://www.clayshed.co.uk/group-events-pottery-bristol · Tech Exchange specification PDF (https://www.clayshed.co.uk/s/2026-Tech-Exchange-Member-Specification-3.pdf; WebFetch could not decode its text, so its contents are **unreachable**)

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | 4 electric kilns (2 × 60 L, 190 L, 290 L); 7 Shimpo wheels. Members, waitlist, area NS | https://www.clayshed.co.uk/facilities | "Clay Shed has four electric kilns available to members and resident artists." · "7 Shimpo pottery wheels with up to 11kg (25lb) centring capacity." |
| 2 Staff | Three founder-directors; throwing and ceramics tutors; four technicians; visiting tutors. No one else named as running operations | https://www.clayshed.co.uk/about-clay-shed-bristol | "Clay Shed was founded in 2019 by three ceramic artists with over 50 years of combined experience." · "The studio is artist-led and supported by practicing makers and industry professionals." |
| 3 Work exchange | **Y. Tech Exchange:** 4 hours of technical support a week in exchange for membership; a published specification and application form (spec text unreadable, so duties NS) | https://www.clayshed.co.uk/ceramics-membership-bristol | "4 hours of technical support each week in exchange for membership" |
| 4 Member duties | NS | — | — |
| 5 Clay | Members NS; course participants pay for extra clay or firing at £5/kg | https://www.clayshed.co.uk/clay-shed-terms-and-conditions | "Extra clay or firings are charged at £5 per 1 kg of fired work." |
| 6 Glazes | Members trained to mix glazes (free induction; glaze-mixing table); personal glaze storage. Studio glazes NS | https://www.clayshed.co.uk/ceramics-membership-bristol | "Free inductions: clay recycling, glaze mixing, kiln loading & firing" · "Personal storage for tools, clay, and glazes" |
| 7 Retail | Gift vouchers; a "Ceramics Market 2026" page (not checked) | https://www.clayshed.co.uk/ceramics-membership-bristol | ("Gift Vouchers" navigation link) |
| 8 Open studio | 24/7 member access; wheels, kilns and equipment booked in an app (unnamed) | https://www.clayshed.co.uk/ceramics-membership-bristol | "Book wheels, kilns, and equipment easily via our app" · "24/7 access - create when it suits you" |
| 9 Kilns | Split model: the team fires for Flexi 4 and Flexi 15 members (extra cost); Flexi Unlimited and Studio Space members manage their own firings after induction; kilns booked in the app | https://www.clayshed.co.uk/ceramics-membership-bristol | "Firings are conducted by the Clay Shed team and are charged as an additional cost." · "You manage your own kiln firings." · "We provide training in essential studio skills - from clay recycling and glaze mixing to kiln loading and firing" |
| 10 Events/rentals | Group events (birthday, team building, social) for 4–10 people from £60 each; email. No hire | https://www.clayshed.co.uk/group-events-pottery-bristol | "Our Group Events are perfect for birthday parties, team building or social gatherings." · "Suitable for groups of 4 to 10 people" |
| 11 Equipment list | Y | https://www.clayshed.co.uk/facilities · https://www.clayshed.co.uk/ceramics-membership-bristol | (see item 1) · "including kilns, potter's wheels, slab rollers and glaze facilities" |
| 12 Member comms | Newsletter; password-protected "Members Area"; booking app | https://www.clayshed.co.uk/ceramics-membership-bristol | "Sign up with your email address to receive news and updates from the Shed" |

### 66 · Clay Studio (Community Clay Studio Manchester CIC) · Manchester, UK · membership/community studio (not-for-profit CIC)

Pages fetched 2026-10-20: https://www.claystudio.co.uk/membership · https://www.claystudio.co.uk/kiln-and-clay · https://www.claystudio.co.uk/the-shop · https://www.claystudio.co.uk/about · https://www.claystudio.co.uk/job-vacancies · https://www.claystudio.co.uk/faqs · https://www.claystudio.co.uk/terms (the Membership Guide is a PDF and was not opened)

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | 5 electric kilns, 8 teaching wheels and 10 members' wheels; waiting list emailed monthly; "We have spaces" now | https://www.claystudio.co.uk/about · https://www.claystudio.co.uk/membership | "The current facilities include: 5 electric kilns, 8 teaching potters' wheels and 10 members wheels," · "We will email out people on the waiting list mid month to start membership on the 1st of the following month." |
| 2 Staff | Three founders (two are directors); a director for social media; technician-tutors; tutors; a production potter; administration. No one other than founders named as running operations | https://www.claystudio.co.uk/about | "Community Clay Studio Manchester was set up by Caroline Tattersall, Stephen Graham and Robert Bree." · "Our current team is: Polly Vaduz (Technician and Tutor) Florrie Andrews (Technician and Tutur)" |
| 3 Work exchange | NS ("We have no Current Vacancies 2026") | https://www.claystudio.co.uk/job-vacancies | "We have no Current Vacancies 2026" |
| 4 Member duties | Y: clean thoroughly and co-operatively, about 20 minutes; members who don't clean pay a 20% surcharge for staff cleaning | https://www.claystudio.co.uk/terms | "To maintain acceptable Health and Safety standards it is essential that all members clean up thoroughly and co-operatively." · "will be required to pay a 20% surcharge on their membership fee to pay for staff cleaning." |
| 5 Clay | Buy by the bag from the studio or source elsewhere (staff-approved); half bag 6 kg or bag 12 kg, collection only; Pathway course gets a clay discount | https://www.claystudio.co.uk/terms · https://www.claystudio.co.uk/the-shop | "You can purchase clay by the bag directly from us or you can source it independently." · "As for all materials, clay must be checked and approved by a member of staff." · "We do not deliver clay, collection only." |
| 6 Glazes | Outside or self-made glazes allowed once approved by staff. Studio glazes for members NS | https://www.claystudio.co.uk/terms | "Externally sourced or self-made glazes must not be used in the studio until approved by a member of staff." |
| 7 Retail | Shop with members' and local makers' work, the in-house production potter's wares, clay and tools; exhibitions; wholesale clay; digital gift cards | https://www.claystudio.co.uk/the-shop · https://www.claystudio.co.uk/faqs | "the shop stocks work created by the members and other local makers" · "The Shop also stocks wares made by our in house trained Production potter" · "Commercial wholesale prices available please email us." |
| 8 Open studio | 3-hour sessions booked on Setmore, first come first served, up to 4 per week (full) | https://www.claystudio.co.uk/membership · https://www.claystudio.co.uk/terms | "All member sessions are for a duration of 3 hours and can be booked up to 1 minute in advance" · "Access to the studio shared workspace and facilities for up to 4 sessions per week for full membership" |
| 9 Kilns | Members prepay per litre on the website, drop off Mon/Thu, about 2 weeks, no collection email; class work 1–8 weeks with email for one-off classes; technicians may refuse unsafe work | https://www.claystudio.co.uk/kiln-and-clay · https://www.claystudio.co.uk/faqs · https://www.claystudio.co.uk/terms | "Please calculate the litres for firing your work and make payment before you arrive." · "items are taking about 2 weeks to process please come and collect as close to the 2 weeks After dropping off as you can." · "We don't email to inform you to collect your items." · "We email you to let you know your pots are ready for collection after one off and private workshop." · "Technicians have the right to refuse firing items they deem to be unsafe." · "Members are liable for any damage to the kiln or furniture caused by poorly produced ware and applied glazes." |
| 10 Events/rentals | Private workshops; members' kiln hire | https://www.claystudio.co.uk/faqs · https://www.claystudio.co.uk/membership | "It takes about 4-8 weeks to fire and glaze pots after a one off or private workshop class." |
| 11 Equipment list | Y (kilns and wheels counted) | https://www.claystudio.co.uk/about | (see item 1) |
| 12 Member comms | Mailing list; Setmore login; T&Cs; Membership Guide | https://www.claystudio.co.uk/membership | "Click below to log in and book your sessions." · "You can read our full T&Cs HERE." · "Read our Membership Guide here" |

### 67 · Slow Clay Centre · Melbourne, Australia · teaching studio

Pages fetched 2026-10-20: https://www.slowclay.com/ · https://www.slowclay.com/pottery-classes/sat/ (password-protected) · https://www.slowclay.com/our-team/ · https://www.slowclay.com/sccfaq/ · https://www.slowclay.com/shop/

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Up to 12 people on wheels and 10 in the hand-building room (private events). Members, kilns, area NS | https://www.slowclay.com/sccfaq/ | "We can take up to 12 people on wheels and 10 people in our hand building room." |
| 2 Staff | Director; office manager (also teaches glaze courses); a technician; tutors and teachers. Non-owner running operations: **Y** (office manager) | https://www.slowclay.com/our-team/ | Jane Sawyer "is the director of Slow Clay Centre" · Amelia Black "is our office manager" · Sarah Mead "works as a technician at Slow Clay Centre" |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS | — | — |
| 5 Clay | Unlimited clay included in course fees; firing user-pays by weight | https://www.slowclay.com/sccfaq/ | "Your fees cover full tuition for the term of your course and UNLIMITED clay." · firing is "applied on a user-pays system (by weight)." |
| 6 Glazes | All studio materials included; students taught to make their own glazes | https://www.slowclay.com/sccfaq/ | "Course fees are fully inclusive of all materials available in our studio including unlimited clay." · "We prefer to teach you how to make and develop your own glazes" |
| 7 Retail | Online shop: a Slow Clay trimming tool, ceramics journals, gift vouchers | https://www.slowclay.com/shop/ | (products listed: "Slow Clay Turning/Trimming Tool" $38.00; "Gift Voucher $135") |
| 8 Open studio | Studio Access Times for enrolled students: booking required, $30; Holiday SATs for experienced users. Details page is password-protected | https://www.slowclay.com/sccfaq/ · https://www.slowclay.com/ | "SATs are only available if you are currently enrolled in either Wheel or Form & Surface courses." · "Bookings are required and there is a $30 fee." · "This content is password protected. To view it please enter your password below:" |
| 9 Kilns | Staff fire all student work; students weigh and pay at collection ($20/kg). Turnaround NS | https://www.slowclay.com/sccfaq/ | "We fire all our student's work for them as part of their course" · "Students are asked to weigh and pay for their work before they take it home." |
| 10 Events/rentals | Private parties, corporate team building, hens | https://www.slowclay.com/ · https://www.slowclay.com/sccfaq/ | "Team-building activities can be tailor-made to suit your group." · "Glazing and firing costs (for two pieces per attendee) are fully included in the fee" |
| 11 Equipment list | NS | — | — |
| 12 Member comms | Monthly e-newsletter | https://www.slowclay.com/ | "Subscribe to our Monthly newsletter to stay informed of when classes become available." |

### 68 · Twelve80 · Melbourne, Australia · membership/community studio · UNREACHABLE

Tried 2026-10-20: https://twelve80.com.au/ (returns only the title "twelve80 - Melbourne Community Ceramic Studio" and an image placeholder) · https://twelve80.com.au/membership (title "twelve80 - Membership", no text) · https://twelve80.com.au/faq (404). The site is a Bubble.io app (per 03) that renders all content by script, so WebFetch sees no text. All 12 items recorded as NS (unreachable). Segment from the page title ("Community Ceramic Studio") and the census.

**UNVERIFIED (third-party listings seen only in search summaries; not studio evidence, not counted):** monthly membership with anytime access to the space, equipment and a lounge (Concrete Playground); $290/month or $20/hour, 460 m², 32 wheels (Creative Spaces listing).

### 69 · Sunroom Studio · Melbourne (Pascoe Vale South), Australia · teaching studio

Pages fetched 2026-10-20: https://www.sunroomstudio.store/ · https://www.sunroomstudio.store/open-studio · https://www.sunroomstudio.store/services · https://www.sunroomstudio.store/about · https://www.sunroomstudio.store/events

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | NS (kiln dimensions are in an image) | — | — |
| 2 Staff | Founder; the studio is "managed by" a group of artists and teachers; a course teacher who is also studio technician; teachers. Non-owner running operations: **Y** | https://www.sunroomstudio.store/about | "Founded by Beck Millsom and managed by a glorious handful of talented local artists and teachers" · "is a treasured 6 week course teacher and studio technician" |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS (clean shared space described, no duties) | https://www.sunroomstudio.store/open-studio | "We take care in maintaining a clean and safe shared space to nurture a positive studio experience for everyone." |
| 5 Clay | Open-studio bookings include free recycled studio clay | https://www.sunroomstudio.store/open-studio | "free recycled studio clay, kitchen facilities and storage space." |
| 6 Glazes | NS | — | — |
| 7 Retail | Gift vouchers | https://www.sunroomstudio.store/ | ("Gift Vouchers" navigation link) |
| 8 Open studio | Studio time booked by the hour or day; policies form before first booking | https://www.sunroomstudio.store/open-studio | "booking into our studio space by the hour or for the day." · "Please take your time to read through our open studio policies and agreements form before booking in for your first time." |
| 9 Kilns | The technician trims, glazes and fires student pots; prepaid firing service by weight on arrival ($10/kg bisque, $14/kg glaze); no turnaround promise | https://www.sunroomstudio.store/about · https://www.sunroomstudio.store/services | "trimming, glazing and kiln firing student pots ensuring beautiful final results" · "Your work will be weighed and price calculated upon arrival." · "note that earthenware and stoneware will have longer waiting times than midfire" |
| 10 Events/rentals | Birthday, hens and team-building group bookings by email/phone; full kiln hire | https://www.sunroomstudio.store/events · https://www.sunroomstudio.store/services | "Get in touch with a few details (group size, preferred dates, and type of event), and we’ll take it from there." · "We also offer full kiln hire for independent artists and organisations; our kiln internal dimensions can be found below." |
| 11 Equipment list | NS | — | — |
| 12 Member comms | Open-studio policies and agreements form; firing service terms | https://www.sunroomstudio.store/services | "All kiln hire customers are encouraged to read through our firing service terms and conditions :)" |

### 70 · Urth Clay Studio · Adelaide (Goodwood, Burnside) and Melbourne (Strathmore), Australia · teaching studio

Pages fetched 2026-10-20: https://urthclaystudio.com.au/pottery-studio-club-membership-adelaide/ · https://urthclaystudio.com.au/work-with-us-pottery0teacher-australia/ · https://urthclaystudio.com.au/pottery-firing-services-2/ · https://urthclaystudio.com.au/pottery-classes-2/pottery-wheel-hiring-adelaide-sa/ · https://urthclaystudio.com.au/goodwood/

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | 15 Shimpo wheels; three sites. Members, kilns, area NS | https://urthclaystudio.com.au/pottery-studio-club-membership-adelaide/ | "Pottery wheel unit; 15 top-of-the-range Shimpo pottery wheels" |
| 2 Staff | Pottery tutors (paid hourly, flexible shifts); "studio staff" manage firings. No names | https://urthclaystudio.com.au/work-with-us-pottery0teacher-australia/ · https://urthclaystudio.com.au/pottery-studio-club-membership-adelaide/ | "Join Our Team as a Pottery Tutor in Adelaide or Melbourne!" · "All firings are managed by studio staff only." |
| 3 Work exchange | NS | — | — |
| 4 Member duties | Y: leave equipment clean after each use | https://urthclaystudio.com.au/pottery-studio-club-membership-adelaide/ | "Leave all equipment clean and in good condition after each use." |
| 5 Clay | Not provided in membership; bought at extra charge | https://urthclaystudio.com.au/pottery-studio-club-membership-adelaide/ | "clay can not be provided, it can be bought." · "Clay: For extra charge." |
| 6 Glazes | NS ("glazing areas" only) | — | — |
| 7 Retail | Online shop: pottery wheel, tool kit, apron, clay sink trap; gift vouchers | https://urthclaystudio.com.au/pottery-studio-club-membership-adelaide/ | (shop menu: "Pottery Wheel", "Buy Pottery Tools", "Clay Sink Trap") |
| 8 Open studio | 24/7 access by passcode; 4 or 8 hours a week plans ($50/$75 a month); no shelves unless hired | https://urthclaystudio.com.au/pottery-studio-club-membership-adelaide/ | "Membership provides 24/7 access to the studio." · "No Shelves are offered." |
| 9 Kilns | Staff only; members cannot load or unload. Community firing by online form with a photo, then a quote; form must be attached to the box. Class work ready 4 weeks after the session; round paper token at pickup; "Missing Piece Form" | https://urthclaystudio.com.au/pottery-studio-club-membership-adelaide/ · https://urthclaystudio.com.au/pottery-firing-services-2/ · https://urthclaystudio.com.au/goodwood/ | "kilns room ( Members have access to firing but they can not load and unload a kiln." · "please fill in the form below, we will send you a quotation" · "please don't drop off your pieces without a copy of the form attached to the box." · "Your work will be ready for pickup 4 weeks after your session." · "The round paper token given on the day." |
| 10 Events/rentals | Team building, hens/private and kids' parties; home wheel hire $145 a month with a $250 bond | https://urthclaystudio.com.au/pottery-classes-2/pottery-wheel-hiring-adelaide-sa/ | "$145 per month" · "a bond of $250 will be taken right away" |
| 11 Equipment list | Partial | https://urthclaystudio.com.au/pottery-studio-club-membership-adelaide/ | (see item 1) |
| 12 Member comms | Membership rules on the page; terms of use; payment and start confirmed by email | https://urthclaystudio.com.au/pottery-studio-club-membership-adelaide/ | "We will email you to confirm payment and start date" · "Only the named member may use the studio during their membership period." |

### 71 · Pot Dispensary · Melbourne (Chapel St), Australia · teaching studio

Pages fetched 2026-10-20: https://www.potdispensary.com.au/ · https://www.potdispensary.com.au/memberships · https://www.potdispensary.com.au/faq-1 · https://www.potdispensary.com.au/about · https://www.potdispensary.com.au/shop

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | 8 wheels; class waitlist by email. Members, kilns, area NS | https://www.potdispensary.com.au/memberships · https://www.potdispensary.com.au/faq-1 | "You will have access to 8 pottery wheels (2 Venco No.3’s, 2 Venco Direct Drives, & 4 Shimpo VL Lite pottery wheels)" · "you can email us at potdispensary@outlook.com.au to be put down on our waitlist." |
| 2 Staff | Owner; studio manager & teacher; technician/studio hand & teacher; a glaze tech; studio hands. Non-owner running operations: **Y** (studio manager) | https://www.potdispensary.com.au/about · https://www.potdispensary.com.au/memberships | Alexandra Sinclair, "ring leader and owner of pot dispensary." · Tiarn, "studio manager & teacher." · Ellie, "dispensary technician/studio hand & teacher." · "Our studio hands are not available to help cover your work or offer assistance." |
| 3 Work exchange | NS | — | — |
| 4 Member duties | Y: cover own work, keep own area clean, leave the space clean | https://www.potdispensary.com.au/memberships | "You're responsible for your own work, this includes covering your work, keeping your area clean" · "leaving the space clean when you leave for the night." |
| 5 Clay | Not included: buy at wholesale price or bring your own (T&Cs apply); clay orders through the team WhatsApp; no mid-fire or earthenware | https://www.potdispensary.com.au/memberships | "Clay (you must purchase this, or BYO -T&C apply)" (under "WHAT'S NOT INCLUDED") · "Team WhatsApp for updates & clay orders" · "NO MID-FIRE, or EARTHENWARE CLAY in the studio." |
| 6 Glazes | Unlimited studio glazes; lead and wood-ash glazes need the glaze tech's permission; making your own glazes in the glaze lab is not included | https://www.potdispensary.com.au/memberships | "Unlimited use of all studio glazes" · "No glazes with lead, and wood ash (cause damage to our kilns) - without permission from our glaze tech (Becca)." · "The ability to make your own glazes in our glaze lab" (under "WHAT'S NOT INCLUDED") |
| 7 Retail | Storefront studio selling its own hand-made ceramics; online at-home clay and painting kits; ceramics and class gift vouchers | https://www.potdispensary.com.au/about · https://www.potdispensary.com.au/shop · https://www.potdispensary.com.au/faq-1 | "Each of our pieces are thoughtfully hand-crafted in-store and designed with the intention to be treasured every day, forever." · "Yes, we have online gift vouchers for our ceramics here, and gift vouchers for our pottery classes here." |
| 8 Open studio | 24/7 with a key; limited time on workshop weekends; visitors max 30 minutes | https://www.potdispensary.com.au/memberships | "The Membership studio is open 24/7 Monday - Sunday" · "(you will be given a key to the studio)" |
| 9 Kilns | Firing service by booked drop-off time; class work 2–3 weeks (term) or 4–8 weeks (one-off); email when out of the kiln; kept two months, then donated | https://www.potdispensary.com.au/faq-1 | "Please read our firing T&Cs before selecting a drop-off time." · "our one-off classes usually take between 4-8 weeks, and our term classes usually take between 2-3 weeks." · "We will send out an email when your pieces are out of the kiln and are ready to be collected from the studio." |
| 10 Events/rentals | Private workshops for 1–20; whole-kiln hire $180–350 | https://www.potdispensary.com.au/faq-1 | "Yes, we do offer private workshops for 1 to 20 potters." · "You can also hire a whole kiln, from $180 - $350." |
| 11 Equipment list | Partial (wheels by make) | https://www.potdispensary.com.au/memberships | (see item 1) |
| 12 Member comms | Team WhatsApp; mailing list; account login; rules on the membership page; firing T&Cs; Safety Data Sheets folder (Google Drive) | https://www.potdispensary.com.au/memberships · https://www.potdispensary.com.au/ | "Team WhatsApp for updates & clay orders" · "Sign up to our mailing list to receive updates on classes, workshops, shop updates & offers" |

### 72 · Bisque Studios · Melbourne, Australia · teaching studio

Pages fetched 2026-10-20: https://bisquestudios.com/membership/ · https://bisquestudios.com/meet-the-team/ · https://bisquestudios.com/booking-refund-policies/ · https://bisquestudios.com/students/ (password-protected) · https://bisquestudios.com/corporate-team-events/

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Membership "limited spaces". Counts and area NS | https://bisquestudios.com/membership/ | "Applications open, with limited spaces available." |
| 2 Staff | Studio Owner (also maker and wheel host) who coordinates and drives the team; teachers; studio techs; a glaze tech; an administrative assistant. Non-owner running operations: **N** | https://bisquestudios.com/meet-the-team/ | Kate Brouwer: "Studio Owner & Maker, Host of Specialist Wheel Classes." · "You'll find her on the other end of your emails co-ordinating, creating content & driving our team." · "Teacher, Maker & Administrative Assistant" · "Studio Tech & Hand-building Teacher" |
| 3 Work exchange | **N** (explicitly prefers paid staff) | https://bisquestudios.com/booking-refund-policies/ | "But we also value peoples time and prefer to pay employees rather than seek out volunteers." |
| 4 Member duties | NS | — | — |
| 5 Clay | Bagged clay sold on site to members and students only; classes include clay | https://bisquestudios.com/membership/ · https://bisquestudios.com/booking-refund-policies/ | "We stock a varied range of bagged clay available for sale on site at the studio." · "Unfortunately the clay we have in the studio is for our members’ program and term classes only." · "This typically includes clay, our tool library, glazes and access to our firing service" |
| 6 Glazes | In-house glaze library included | https://bisquestudios.com/membership/ | "Included in the membership is access to our unique in-house studio glaze library." |
| 7 Retail | Digital gift vouchers | https://bisquestudios.com/booking-refund-policies/ | "Yes, we offer digital gift vouchers that can be used toward any class or program." |
| 8 Open studio | Members: 24 hours except Tuesday evenings. Students: book an unsupervised access timeslot (password page) | https://bisquestudios.com/membership/ · https://bisquestudios.com/booking-refund-policies/ · https://bisquestudios.com/students/ | "24 hours (except for Tuesday evenings, 6pm – 9pm)" · "This is an unsupervised session that gives you the chance to catch up on missed work" · "Book your student access timeslot." |
| 9 Kilns | In-house firing service for members ($9/kg); no public firing; workshop work 4–6 weeks | https://bisquestudios.com/membership/ · https://bisquestudios.com/booking-refund-policies/ | "We do not offer firing services to the public at this time." · "It typically takes 4 – 6 weeks after our workshop or short course to have your items fired and ready for collection" |
| 10 Events/rentals | Birthday, team-building and corporate groups of 6–20+; whole-warehouse venue hire; brochure and email | https://bisquestudios.com/corporate-team-events/ | "Book out our whole warehouse for you & your guests." · "Download our private bookings brochure below for more info, or send us an email with your enquiry details." |
| 11 Equipment list | NS | — | — |
| 12 Member comms | Mailing list; password emailed to students; login; FAQs and policies page | https://bisquestudios.com/students/ | "Password available in your email inbox" |

### 73 · Terrapotter Studios · Melbourne (Central, North Melbourne), Australia · teaching studio

Pages fetched 2026-10-20: https://www.terrapotterstudios.com/ · https://www.terrapotterstudios.com/publicfiring · https://www.terrapotterstudios.com/studio-time · https://www.terrapotterstudios.com/about · https://www.terrapotterstudios.com/faqs · https://www.terrapotterstudios.com/private-events

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Central: 18 wheels, capacity 36; North Melbourne: 20 wheels, capacity 40 | https://www.terrapotterstudios.com/private-events | "Our Central Melbourne studio has 18 wheels, and a max capacity of 36 people." · "Our North Melbourne studio has 20 wheels, and a max capacity of 40 people." |
| 2 Staff | "TPS Team"; staff with a teaching masters; teachers named only in reviews. No titles | https://www.terrapotterstudios.com/about · https://www.terrapotterstudios.com/faqs | "Classes and content are developed by our staff who hold a 'Masters in Teaching'" (trimmed) · "your pots will have to go through a series of works completed by the TPS Team" |
| 3 Work exchange | NS | — | — |
| 4 Member duties | Y: open-studio users clean the equipment they use | https://www.terrapotterstudios.com/studio-time | "Potters attending open studio are responsible for the clean up of any equipment used." |
| 5 Clay | Recycled clay $2/kg and fresh bags $30–60 sold in studio; midfire only; outside midfire clay implied (staff advise where to buy) | https://www.terrapotterstudios.com/studio-time · https://www.terrapotterstudios.com/publicfiring | "Recycled clay is available for purchase at $2/KG in studio." · "Fresh bag clay is available for purchase in studio pending stock ($30-$60/bag)." · "Only midfire clay is permitted in the studio." |
| 6 Glazes | Studio glazes at +$5/kg on firing; outside glazes permitted | https://www.terrapotterstudios.com/studio-time | "Studio glazes are available at +$5/KG firing price." · "Outside glazes permitted but to follow firing agreements." |
| 7 Retail | Gift cards (Acuity); newsletter mentions "sale seasons" | https://www.terrapotterstudios.com/ | "receive updates on term class openings, specialty workshops, and sale seasons" |
| 8 Open studio | Reserve in advance through an online scheduler; at least one term of classes; pieces stored 2 weeks | https://www.terrapotterstudios.com/studio-time | "Studio access time must be reserved in advance." · "Pieces made during open studio are to be dated, and will be stored for a maximum of 2 weeks before being recycled." |
| 9 Kilns | The TPS team processes work; public firing by agreement form, weigh and pay, place on the public firing shelf; about 2 weeks; firing updates on Instagram; pickup email for classes; 2-month hold | https://www.terrapotterstudios.com/publicfiring · https://www.terrapotterstudios.com/faqs | "Have your pieces weighed, settle your payment, and take your pieces to the public firing shelf." · "Our turn around is roughly 2 weeks." · "Keep up to date with your firings, check in regularly with our Instagram account, or feel free to message/email us." · "You will receive a pick up email when they are ready." · damage fees: "will result in a maintenance fee of $50/damaged shelf." and "Any major damage made to the kiln shelves will result in a $250 charge to replace the damaged shelf." |
| 10 Events/rentals | Anniversaries, birthdays, hens and corporate functions on a sliding scale ($80–250 pp); offsite +$250; payment link | https://www.terrapotterstudios.com/private-events | "Whether its anniversaries, birthdays, hens parties or corporate functions" · "Once you confirm your event, we will send you a payment link" |
| 11 Equipment list | Partial (wheel counts) | https://www.terrapotterstudios.com/private-events | (see item 1) |
| 12 Member comms | Newsletter; studio policies and guidelines; public firing agreement | https://www.terrapotterstudios.com/studio-time | "By reserving your studio time, you are agreeing to the studio policies and guidelines." |

### 74 · Urban Clay · Melbourne (Highett), Australia · teaching studio

Pages fetched 2026-10-20: https://www.urbanclay.com.au/ · https://www.urbanclay.com.au/about/ · https://www.urbanclay.com.au/pottery-classes/ · https://www.urbanclay.com.au/terms-conditions/. Small class-only site; no membership, open studio, FAQ or jobs pages.

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | NS (class caps only, e.g. "Just 12 places" for a masterclass) | https://www.urbanclay.com.au/ | "Just 12 places" |
| 2 Staff | Founder and ceramics artist; "highly experienced teachers". No one else named | https://www.urbanclay.com.au/about/ · https://www.urbanclay.com.au/ | "Rebecca Withington – Founder and ceramics artist" · "Our highly experienced teachers will guide you through the fundamental skills of working with clay." |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS | — | — |
| 5 Clay | NS | — | — |
| 6 Glazes | NS | — | — |
| 7 Retail | Gift vouchers (online) | https://www.urbanclay.com.au/pottery-classes/ | "Gift a complete course or choose the amount you want to give." |
| 8 Open studio | NS (classes only) | — | — |
| 9 Kilns | The studio fires students' work; finished work held until firing fees are paid; uncollected work disposed of after 3 months | https://www.urbanclay.com.au/ · https://www.urbanclay.com.au/terms-conditions/ | "which will be bisque fired by Urban Clay." · "The Studio reserves the right to retain completed pieces until all relevant firing fees have been paid in full." · "The Studio also reserves the right to dispose of any uncollected work after a period of three (3) months," |
| 10 Events/rentals | NS | — | — |
| 11 Equipment list | NS | — | — |
| 12 Member comms | Mailing list; "My Account"; T&Cs with a conduct clause | https://www.urbanclay.com.au/ · https://www.urbanclay.com.au/terms-conditions/ | "Join our mail list to keep updated when online bookings open…" · "Our Studio is committed to fostering a harmonious, creative, and safe environment" |

### 75 · Neo Ceramics · Melbourne (Kensington), Australia · teaching studio

Pages fetched 2026-10-20: https://neoceramics.com.au/studio-access · https://www.neoceramics.com.au/faq · https://www.neoceramics.com.au/about (404) · https://www.neoceramics.com.au/equipment · https://www.neoceramics.com.au/studio-subscription · https://www.neoceramics.com.au/supplies

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | NS | — | — |
| 2 Staff | NS (about page returned 404) | — | — |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS | — | — |
| 5 Clay | Free reclaim clay for studio access; subscription includes weekly-pugged reclaim stoneware | https://neoceramics.com.au/studio-access · https://www.neoceramics.com.au/studio-subscription | "Studio reclaim clay use is free." · "PB103 stoneware pugged weekly fresh reclaim" |
| 6 Glazes | House glazes included | https://neoceramics.com.au/studio-access | "The fee covers equipment, shared tools, house glazes, and communal greenware storage." |
| 7 Retail | Sells studio equipment it fabricates (downdraft glaze table $6,750, spray booth from $10,750, slip-casting table, glaze stirrers), by email enquiry; gift vouchers | https://www.neoceramics.com.au/supplies · https://www.neoceramics.com.au/faq | "Professional studio equipment fabricated in Melbourne." · "Gift vouchers purchased online are valid for a period of 2 years from date of issue." |
| 8 Open studio | Sessions bought singly, in packs or monthly, then booked with a code; set hours; subscription gives unlimited timetabled sessions | https://neoceramics.com.au/studio-access · https://www.neoceramics.com.au/studio-subscription | "Sessions can be booked individually, monthly subscriptions, in a pack of 5, 10 or 15." · "Use the code provided after purchase to book your sessions." |
| 9 Kilns | Collection date given at end of class (3–4 weeks); unclaimed work donated after a month | https://www.neoceramics.com.au/faq | "You will be given a collection date at the end of class (usually this is 3-4 weeks after the course or session)." · "Work that is not picked up within one month will be donated to an opportunity shop." |
| 10 Events/rentals | NS | — | — |
| 11 Equipment list | NS (equipment for sale is listed, not the studio's own) | — | — |
| 12 Member comms | NS | — | — |

**Hypothesis note (inventory and sales):** a studio that also manufactures and sells high-value equipment by enquiry (https://www.neoceramics.com.au/equipment).

### 76 · Lygon Street Pottery · Melbourne, Australia · teaching studio

Pages fetched 2026-10-20: https://lygonstreetpottery.com.au/memberships · https://lygonstreetpottery.com.au/our-team-1 · https://lygonstreetpottery.com.au/studios-for-rent · https://lygonstreetpottery.com.au/kiln-hire · https://lygonstreetpottery.com.au/faq

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Three hireable kilns (Rohde 200 L, Tetlow 510 L electric, Tetlow 592 L gas); five private 13 m² studios for rent | https://lygonstreetpottery.com.au/kiln-hire · https://lygonstreetpottery.com.au/studios-for-rent | "We have two electric kilns that you can hire for private firings." (the page then lists three, one gas) · "We have five freshly painted and constructed studios providing ample light and air." |
| 2 Staff | Two studio founders (also teachers) approve memberships; teachers; a studio tech/teacher. No one else named as running operations | https://lygonstreetpottery.com.au/our-team-1 · https://lygonstreetpottery.com.au/memberships | Gordon Hickmott: "STUDIO FOUNDER & TEACHER" · Caine Stednitz: "STUDIO TECH/TEACHER" · "Membership requests will be approved by our studio founders Brigid and Gordon." |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS for members; students pick up fallen clay and wipe surfaces | https://lygonstreetpottery.com.au/faq | "The team at LSP go to a great deal of trouble to keep the dust down," |
| 5 Clay | Members supply their own tools and materials; intensive courses include clay, glazes and up to 2 kg of firing | https://lygonstreetpottery.com.au/memberships · https://lygonstreetpottery.com.au/faq | "Members MUST supply their own tools and materials" · "All materials and equipment are supplied, this includes clay, glazes, and firing up to 2kg glazed ware," |
| 6 Glazes | House glazes available, charged with firing; 15% member firing discount | https://lygonstreetpottery.com.au/memberships | "A selection of house glazes are available to use (and will be added to firing charges)." · "Members recieve a 15 per cent discount on firing." |
| 7 Retail | Gift vouchers; retail space for renters "to be negotiated" | https://lygonstreetpottery.com.au/faq · https://lygonstreetpottery.com.au/studios-for-rent | "Retail and exhibition space to be negotiated as our community and pottery expands." |
| 8 Open studio | Members: unlimited access in opening hours (varies with courses). Casual untutored studio-access sessions booked 1–2 weeks ahead. Studio renters: 24-hour access | https://lygonstreetpottery.com.au/memberships · https://lygonstreetpottery.com.au/faq · https://lygonstreetpottery.com.au/studios-for-rent | "A studio membership includes unlimited studio access." · "Studio access sessions are untutored and intended for independent practice." · "Available with 24-hour access to your own private workspace of 13 square meters with full access to all studio facilities." |
| 9 Kilns | Staff load; member firing roughly fortnightly; public firing 10–14 days, by the kilo or whole kiln | https://lygonstreetpottery.com.au/memberships · https://lygonstreetpottery.com.au/faq | "Staff will load the kilns so it's not necessary for members to know how to do this" · "Firing frequency will depend a little on demand but generally it can be expected fortnightly." · "There is approximately a 10 to 14 day turnaround." |
| 10 Events/rentals | Private workshops and work parties ($300 for 2, $80 per extra person); kiln hire ($230–550 per full kiln) by form or phone; private studios $480/month (6-month minimum) | https://lygonstreetpottery.com.au/faq · https://lygonstreetpottery.com.au/kiln-hire · https://lygonstreetpottery.com.au/studios-for-rent | "$300 for 2 people for 2 hours ($80 per extra person)" · "To inquire about booking a kiln please fill out the form below or give us call." · "Cost $480 PCM for a six-month minimum period." |
| 11 Equipment list | Y | https://lygonstreetpottery.com.au/studios-for-rent · https://lygonstreetpottery.com.au/memberships | "Access to equipment includes large pug-mill, slab roller, glaze lab, sinks, kilns," · "Shimpo and Fired Up pottery wheels" |
| 12 Member comms | NS (membership request form only) | https://lygonstreetpottery.com.au/memberships | "To request a studio membership please fill out the form below" |

### 77 · Robert Gordon Australia · Pakenham (Melbourne), Australia · production pottery (with a PYOP studio)

Pages fetched 2026-10-20: https://www.robertgordonaustralia.com/pages/paint-your-own-pottery · https://www.robertgordonaustralia.com/products/paint-your-own-pottery-booking · https://www.robertgordonaustralia.com/pages/the-people · https://www.robertgordonaustralia.com/pages/careers · https://www.robertgordonaustralia.com/pages/our-process

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Production scale: 35 craftspeople, 300,000+ pieces a year. Kiln and seat counts NS | https://www.robertgordonaustralia.com/pages/the-people · https://www.robertgordonaustralia.com/pages/our-process | "Here at Robert Gordon, we employ 35 local craftspeople to make and process the pottery." · "the Pakenham workshop producing more than 300,000 pieces of high-fired stoneware each year." |
| 2 Staff | Craftspeople; retail, production, warehouse and office teams; a PYO front desk. No names or titles | https://www.robertgordonaustralia.com/pages/careers · https://www.robertgordonaustralia.com/pages/paint-your-own-pottery | "Opportunities may become available throughout the year across our retail, production, warehouse, and office teams in both full-time and casual roles." · "Payment is per piece and should be made at the front desk after your 1-hour session." |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS (PYOP) | — | — |
| 5 Clay | NS (PYOP) | — | — |
| 6 Glazes | Glaze and firing included in the piece price | https://www.robertgordonaustralia.com/pages/paint-your-own-pottery | "Each piece and shape is priced individually, with firing and glaze costs included." |
| 7 Retail | Own stoneware sold online and at the Pakenham outlet; gift cards; wholesale hub | https://www.robertgordonaustralia.com/pages/paint-your-own-pottery | (links: "All Products", "Gift cards", "Wholesale Hub") |
| 8 Open studio (PYOP seating) | Bookings essential; limited seating; pay at the front desk | https://www.robertgordonaustralia.com/pages/paint-your-own-pottery | "Bookings are essential as spaces are limited." · "We cannot guarantee space for unregistered guests." |
| 9 Kilns | Staff write an order number and date on each piece; 2–3 weeks; notified when ready; postage available | https://www.robertgordonaustralia.com/pages/paint-your-own-pottery | "so our staff can add your order number and date." · "Firing usually takes two to three weeks (please note peak periods such as school holidays may see some delays)." · "We'll notify you when your finished pieces are ready to collect from the shop." |
| 10 Events/rentals | Large groups, pottery parties, birthdays, school-holiday and team-building events on request | https://www.robertgordonaustralia.com/pages/paint-your-own-pottery | "welcomes all ages and can also accommodate large groups on request" |
| 11 Equipment list | Partial (production machinery) | https://www.robertgordonaustralia.com/pages/our-process | "Our interiors collections are fired in solar-assisted electric kilns." · "As a production pottery, we utilise machinery and mouldings to make each piece." |
| 12 Member comms | NS | — | — |

### 78 · Everyday Clay · Healesville VIC, Australia · teaching studio (mobile workshops, kits)

Pages fetched 2026-10-20: https://www.everydayclay.com.au/ · https://www.everydayclay.com.au/glazing-firing-booking · https://www.everydayclay.com.au/glaze-fire · https://www.everydayclay.com.au/studio-policies · https://www.everydayclay.com.au/workshops

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | NS (a mobile studio; one studio kiln mentioned) | https://www.everydayclay.com.au/workshops | "As a mobile studio, we're fully equipped to host workshops anywhere in Victoria." |
| 2 Staff | One person (maker-owner). Non-owner running operations: **N** | https://www.everydayclay.com.au/ | "Amanda Reishi is the maker behind Everyday Clay (yes, we're a one-person show over here!)." |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS (no members) | — | — |
| 5 Clay | Sold as DIY clay kits online; the firing service only takes pieces made from its own kits | https://www.everydayclay.com.au/ · https://www.everydayclay.com.au/studio-policies | "Our clay kits are packed with everything you need to create pottery at home." · "We also do not glaze and fire pieces made using other (i.e., non-Everyday Clay) clay kits" |
| 6 Glazes | Studio's own custom glazes, chosen on a glaze selection card | https://www.everydayclay.com.au/glaze-fire | "you'll have exclusive access to our studio's unique range of custom glazes for us to finish your pieces in." |
| 7 Retail | Online shop (24/7) of DIY clay kits and its own pottery | https://www.everydayclay.com.au/ | "Our online shop is open 24/7!" · "Browse new arrivals and restocks hot off the kiln, or shop everything if you can't decide!" |
| 8 Open studio | NS (studio visits by appointment only) | https://www.everydayclay.com.au/studio-policies | "Our studio is open by appointment only during business hours (Friday and Saturday, 10am-4pm)." |
| 9 Kilns | Owner glazes and fires; drop-off booked on a calendar, then an invoice paid before drop-off; 4–6 weeks; email when ready; 30 days (kits) or 60 days (workshops) to collect | https://www.everydayclay.com.au/glazing-firing-booking · https://www.everydayclay.com.au/glaze-fire · https://www.everydayclay.com.au/studio-policies | "Use the calendar below to book a suitable day and time to bring in your pieces." · "After booking, you’ll receive an invoice with our account details." · "Please allow 4-6 weeks for glazing and firing to be completed." · "You'll be notified by email as soon as your pieces are ready." |
| 10 Events/rentals | Mobile team-building, staff wellbeing, hens, birthdays and conference workshops (5 to 150 people), often at partner wineries; enquiry, call, signed agreement | https://www.everydayclay.com.au/workshops | "Team-building events and staff wellbeing days" · "Submit your enquiry and we'll get in touch to arrange a call to discuss your event." · "Your booking will be secured once payment is received and the agreement signed off." |
| 11 Equipment list | NS | — | — |
| 12 Member comms | Mailing list; studio policies page | https://www.everydayclay.com.au/ | "Join our mailing list to stay connected with all things clay." |

### 79 · Maker's Loft · Sydney, Australia · teaching studio (multi-craft)

Pages fetched 2026-10-20: https://www.makersloft.com.au/pages/pottery-ceramics-studio · https://www.makersloft.com.au/products/pottery-open-studio-membership · https://www.makersloft.com.au/pages/kiln-firing-service-1 · https://www.makersloft.com.au/pages/about-us · https://www.makersloft.com.au/pages/venue-hire

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | 14 wheels (4 dual-direction); four electric kilns; membership subject to availability | https://www.makersloft.com.au/products/pottery-open-studio-membership · https://www.makersloft.com.au/pages/pottery-ceramics-studio | "Unlimited use (no booking required) of our 14 pottery wheels (incl. 4 dual directional wheel)." · "Our Studios offer a range of Venco & Shimpo pottery wheels and four Electric Kilns as part of our Pottery membership." · "Our memberships are subject to availability (please contact us prior to booking)." |
| 2 Staff | Founded by two sisters; generic staff; outside "Makers" invited to teach. No titles | https://www.makersloft.com.au/pages/about-us | "Sisters Anouke & Sacha (life long crafters & tinkerers), established their first creative studios in Cammeray" |
| 3 Work exchange | NS ("Run a Workshop" page invites makers to teach) | — | — |
| 4 Member duties | Y: clean all equipment and tools each visit; last person out turns off lights and air-con | https://www.makersloft.com.au/products/pottery-open-studio-membership | "All equipment and tools must be thoroughly cleaned at the end of every visit." · "Turn off lights and Aircon. if you're the last person to leave our studios." |
| 5 Clay | Members use their own clay, which can be ordered from the studio's online shop; not included | https://www.makersloft.com.au/products/pottery-open-studio-membership · https://www.makersloft.com.au/pages/pottery-ceramics-studio | "Own clay (this can be ordered" (continues with a shop link) · "We have a small range of midfire and stoneware clays available via our online Shop." |
| 6 Glazes | Communal and studio glazes provided; own glazes allowed | https://www.makersloft.com.au/products/pottery-open-studio-membership | "Selection of communal and studio glazes." · "Own Grazes (for personal preference)." [sic] |
| 7 Retail | Online shop of clays, pottery materials and tools; gift cards | https://www.makersloft.com.au/pages/pottery-ceramics-studio | (menu: "Midfire Clays", "Stoneware Clays", "Pottery Materials & Tools", "Shop Clay & tools") |
| 8 Open studio | 24/7; no booking needed for wheels; self-directed sessions; induction appointment first; casual use $35 | https://www.makersloft.com.au/products/pottery-open-studio-membership | "Unlimited (24/7) use of our air-conditioned creative space." · "These studio sessions are not led by a teacher and are completely self-directed." · "Please make an appointment for a short induction before commencement." |
| 9 Kilns | Staff load and unload; firings ordered and paid as online products (10 kg/month included for members); order sheet at drop-off; request form for large work | https://www.makersloft.com.au/pages/kiln-firing-service-1 · https://www.makersloft.com.au/products/pottery-open-studio-membership | "We take extraordinary care of everyone's work loading and unloading" · "Select from below firing options to place your order and book your required firing." · "Please bring a copy of your order sheet with itemised description of your work and contact details." · "10 kg of firing per month (for pieces created in our studio by you)." |
| 10 Events/rentals | Corporate team bonding, hens and kids' parties (enquiry form); front-studio hire $440 (4 h) or $750 (8 h); equipment hire; full or half kiln bookings from $60 | https://www.makersloft.com.au/pages/about-us · https://www.makersloft.com.au/pages/venue-hire · https://www.makersloft.com.au/pages/kiln-firing-service-1 | "Offering a range of creative events from corporate team bonding, Hens parties to birthday parties for all ages." · "$440 up to 4 hrs Mon - Fri" · "A non refundable $100 deposit is required at the time of booking to secure your time slot." · "We offer full and half Kiln bookings starting from $60 for our smaller kiln (30x30x45cm)" |
| 11 Equipment list | Partial | https://www.makersloft.com.au/pages/pottery-ceramics-studio | (see item 1) |
| 12 Member comms | Newsletter; customer account; membership rules on the product page; firing terms | https://www.makersloft.com.au/pages/pottery-ceramics-studio | "Promotions, new products and sales. Directly to your inbox." |

### 80 · Studio P · Perth (Mount Hawthorn), Australia · teaching studio

Pages fetched 2026-10-20: https://www.studiop.net.au/memberships · https://www.studiop.net.au/faqs · https://www.studiop.net.au/about · https://www.studiop.net.au/shop · https://www.studiop.net.au/coursesandworkshops

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | 22 wheels; kilns: 4 electric (membership page) or 3 (about page) — the pages disagree; memberships currently available | https://www.studiop.net.au/memberships · https://www.studiop.net.au/about | "We have 4 electric kilns which we run multiple times every week for both bisque and glaze firings." · "22 x wheels," "3 x electric kiln," · "We currently have availability for memberships." |
| 2 Staff | Two founders (one a teacher); team members with day jobs; a "Studio helper". No titles for operations | https://www.studiop.net.au/about | Vaughan and Katie launched Studio P in their "‘homie’ backyard studio." · "Katie is a qualified secondary school teacher and has started teaching wheel-throwing classes in 2020." · Neo: "Studio helper" |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS | — | — |
| 5 Clay | Bought at the studio, extra to membership; price NS | https://www.studiop.net.au/memberships | "Clay and firing come at an additional cost for members." · "Clay can be purchased at the studio." |
| 6 Glazes | 10 studio glazes included | https://www.studiop.net.au/memberships | "Unlimited use of our 10 studio glazes included" |
| 7 Retail | Online shop: toolboxes, underglazes and glazes; handmade items in the studio store; gift cards (no expiry) | https://www.studiop.net.au/shop · https://www.studiop.net.au/faqs | "Did you know that you can use your voucher for classes as well as handmade items in our studio store!?" · "Our gift vouchers don't have an expiry date!" |
| 8 Open studio | 24/7 member access except during private functions; members may work during classes | https://www.studiop.net.au/memberships | "Members have unlimited 24/7 use of the studio space." · "Due to the fact that we have more wheels than the maximum number of our class sizes, members can work in the studio during class times." |
| 9 Kilns | Kilns run several times a week; class work fired within 2 weeks; email when fired; kept 2 months; members pay $8/kg | https://www.studiop.net.au/faqs · https://www.studiop.net.au/memberships | "All items from your class will be either bisque fired or glaze fired within 2 weeks of course completion." · "We will also send emails out to everyone once pieces are fired" · "Firings are charged per weight at $8 a kilo for members." |
| 10 Events/rentals | Private functions (bridal showers, family events, birthdays, Christmas) | https://www.studiop.net.au/faqs | "Yes! We can host your bridal shower, family event, birthday party and Christmas events!" |
| 11 Equipment list | Y | https://www.studiop.net.au/about | "Pug-mill (for recycling clay)," "Clay extruder," and "Slap roller." [sic] |
| 12 Member comms | Newsletter; membership by email application | https://www.studiop.net.au/memberships | "Sign up to receive studio news and updates." |

### 81 · ClayMake Studio · Perth, Australia · teaching studio

Pages fetched 2026-10-20: https://www.claymakestudio.com.au/ · https://www.claymakestudio.com.au/firing-tcs.html · https://www.claymakestudio.com.au/membership.html · https://www.claymakestudio.com.au/our-facilities.html · https://www.claymakestudio.com.au/drop-in-sessions.html · https://www.claymakestudio.com.au/private-functions.html · https://www.claymakestudio.com.au/meet-the-team.html

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | 3 electric kilns, 15 wheels. Members, area NS | https://www.claymakestudio.com.au/our-facilities.html | "3 electric kilns" · "There are 15 pottery wheels" |
| 2 Staff | Run by a mother–daughter team (ownership not stated); studio technicians; teachers; an instructor and ceramic technician; an arts facilitator | https://www.claymakestudio.com.au/ · https://www.claymakestudio.com.au/meet-the-team.html | "ClayMake Studio is run by mother - daughter team, Andrea & Emma Vinkovic" · "one of the studio technicians at ClayMake" · "an Instructor and Ceramic Technician" |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS | — | — |
| 5 Clay | Studio stocks clay; drop-ins exclude clay and firing; price NS | https://www.claymakestudio.com.au/drop-in-sessions.html | "Drop-in sessions do not include clay or firings:" · "For more information about the clay we stock please visit" |
| 6 Glazes | 14 glazes and engobes; included in membership | https://www.claymakestudio.com.au/our-facilities.html · https://www.claymakestudio.com.au/membership.html | "14 glazes, and engobes to choose from" · "Access to all equipment and facilities (including tools & glazes)" |
| 7 Retail | Gift certificates | https://www.claymakestudio.com.au/ | ("Gift Certificates" navigation link) |
| 8 Open studio | Drop-ins need no booking (pay in person, $15/2 h); induction for everyone; wheels unavailable during throwing classes; Essential membership up to 40 h/month | https://www.claymakestudio.com.au/drop-in-sessions.html · https://www.claymakestudio.com.au/membership.html | "At ClayMake you can drop in any time to come use the studio - no pre-booking required!" · "You can pay as you go, or you can choose to pre-purchase 10 sessions to receive a 10% discount" · "all this is done in person." · "Everyone is required to go through an induction before using the studio for the first time." · "Up to 40 hours studio time per month" |
| 9 Kilns | Firing form per submission; sorted by size and readiness; $1 handling fee for guests; contact if work is refused; 2-month collection limit | https://www.claymakestudio.com.au/firing-tcs.html | "Please make sure you fill in the firing form correctly and completely, including your phone number." · "Work is sorted into the kilns according to size and readiness, large or wet work may take longer to process." · "There is a $1 handling fee included in the firing costs for guest firings." · "If kiln shelves are damaged (due to overflowing glaze etc.) you will be charged a fee of $10 per case." |
| 10 Events/rentals | Birthdays, bridal showers, bachelorettes, club gatherings, fundraisers, team building and corporate functions; contact page | https://www.claymakestudio.com.au/private-functions.html | "From birthday parties (kids, teens and adults), bridal showers, bachelorette parties, club gatherings," |
| 11 Equipment list | Y | https://www.claymakestudio.com.au/our-facilities.html | "a slab roller" · "3 extruders" |
| 12 Member comms | Mailing list; studio T&Cs, OHS page and firing T&Cs | https://www.claymakestudio.com.au/ | "Subscribe to our mailing list to receive an email when we publish new classes or clay events:" |

### 82 · Not Yet Perfect · Perth (Applecross), Australia · teaching studio (workshops; maker's shop)

Pages fetched 2026-10-20: https://www.notyetperfect.com/ (503 on the first try, loaded on retry) · https://www.notyetperfect.com/workshops · https://www.notyetperfect.com/private-workshops · https://www.notyetperfect.com/about · https://www.notyetperfect.com/shop · https://www.notyetperfect.com/services · (https://www.notyetperfect.com/faq returned 404)

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | NS (wheel workshops capped at 12) | https://www.notyetperfect.com/private-workshops | "Wheel work workshops 12 guests max" |
| 2 Staff | NS (a "host" runs workshops; no names or titles) | https://www.notyetperfect.com/private-workshops | — |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS (no members) | — | — |
| 5 Clay | NS | — | — |
| 6 Glazes | NS | — | — |
| 7 Retail | Online shop of its own vases, bowls and tumblers ($45–199), a candle, an "At Home Pottery Box", a firing & glazing voucher; gift card | https://www.notyetperfect.com/shop | (products: "Teardrop Vase Large: $189.00", "At Home Pottery Box: $155.00", "Firing & Glazing Voucher: $35.00") |
| 8 Open studio | NS (workshops only) | — | — |
| 9 Kilns | NS (a "Firing Service" is listed without details) | https://www.notyetperfect.com/services | — |
| 10 Events/rentals | Corporate, social-club, team-building, birthday, bridal and hens workshops; $700 + GST minimum; booking form; can travel; "Pottery Wheel Hire" service | https://www.notyetperfect.com/private-workshops · https://www.notyetperfect.com/services | "Corporate Events \| Social Clubs \| Team Building \| Birthdays \| Bridal Showers" · "Then minimum spend for private workshops is $700 + GST" · "please fill out our booking form below, and we'll get back to you within 2 business days." |
| 11 Equipment list | NS | — | — |
| 12 Member comms | NS | — | — |

### 83 · Walyalup Fremantle Arts Centre (WFAC) · Fremantle, Australia · municipal/recreation (a City of Fremantle arts centre)

Pages fetched 2026-10-20: https://wfac.org.au/art-classes/ · https://wfac.org.au/product/wheel-throwing-mondays/ (Open Studio: Wheelwork and Handbuilding) · https://wfac.org.au/terms/ · https://wfac.org.au/about-wfac/our-people/ · https://shop.wfac.org.au/

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | NS (open-studio course showed "2 Spot(s) left") | https://wfac.org.au/product/wheel-throwing-mondays/ | "2 Spot(s) left" |
| 2 Staff | City arts team: Manager of Arts, Creative Learning Manager, Studio Coordinators, a ceramics studio technician, Shop Manager, tutors. Non-owner running operations: **Y** (City staff and coordinators) | https://wfac.org.au/about-wfac/our-people/ | "Studio Technician – Ceramics" (Finn Anderson) · "Studio Coordinator Creative Learning" (Joanne Brown) · "Studio Coordinator" (Bevan Honey) · the centre is "a proud city of Fremantle service" |
| 3 Work exchange | NS ("Opportunities" page not checked) | — | — |
| 4 Member duties | NS (code of conduct only) | https://wfac.org.au/terms/ | "Treat shared spaces, equipment, materials, and Walyalup Fremantle Arts Centre property with care." |
| 5 Clay | One bag of clay included in the course; extra clay bought locally | https://wfac.org.au/product/wheel-throwing-mondays/ | "One bag of clay is included in the cost of this course." · "If you need additional clay, you're welcome to purchase more—your tutor can recommend where to buy it locally." |
| 6 Glazes | NS (glazing taught; stand-alone glazing classes) | https://wfac.org.au/product/wheel-throwing-mondays/ | "If you'd like more time to finish your pieces, we offer stand-alone glazing classes each term" |
| 7 Retail | WFAC Shop of local WA makers' work (incl. ceramics), online and in store | https://shop.wfac.org.au/ | "Explore handmade creations by local WA makers." · "Shop online or visit us in store." |
| 8 Open studio | "Open Studio" is a tutor-supported term course ($408) for experienced makers, enrolled online; limited places | https://wfac.org.au/product/wheel-throwing-mondays/ | "designed for makers with some prior experience in wheelwork and hand building." · "Mel combines practical demonstrations with personalised feedback" |
| 9 Kilns | Best-effort weekly firing; not all work fired by course end; email with collection times | https://wfac.org.au/product/wheel-throwing-mondays/ | "We do our best to fire as much work as we can each week" · "it's not always possible to fire and glaze all pieces before the end of your course" |
| 10 Events/rentals | NS (not linked from the classes pages) | — | — |
| 11 Equipment list | NS | — | — |
| 12 Member comms | Enrolment T&Cs with a code of conduct | https://wfac.org.au/terms/ | "Follow all directions given by tutors and staff, particularly regarding safety." |

### 84 · Earth & KLĀ · Perth, Australia · teaching studio

Pages fetched 2026-10-20: https://earthandkla.com.au/ · https://earthandkla.com.au/self-guided-classes/ · https://earthandkla.com.au/terms-conditions/ · https://earthandkla.com.au/workshops-2/ · https://earthandkla.com.au/tableware/

| Item | Finding | URL | Verbatim quote(s) |
|---|---|---|---|
| 1 Size | Two kilns (named). Others NS | https://earthandkla.com.au/self-guided-classes/ | "Our firing is done in-house by our kilns affectionately known as Frank and Pietro." |
| 2 Staff | Founder and resident potter (Nellie Ting), who also teaches; "studio staff" | https://earthandkla.com.au/ · https://earthandkla.com.au/terms-conditions/ | "our resident potter Nellie Ting" · "The studio staff value and handle each piece of work with their utmost care." |
| 3 Work exchange | NS | — | — |
| 4 Member duties | NS | — | — |
| 5 Clay | Clay available to purchase at the studio; price NS | https://earthandkla.com.au/self-guided-classes/ | "Enjoy the use of our equipment and quality tools offered, along with a variety of clay you are welcome to purchase here." |
| 6 Glazes | NS for students (the founder makes the studio's own glazes for its tableware) | https://earthandkla.com.au/ | "fired using kilns located onsite with the glazes made by Nellie in our studio" |
| 7 Retail | Online tableware by the resident potter ($45–260); gift cards | https://earthandkla.com.au/tableware/ | "Each clayware is crafted with intention, created from a quiet & mindful place with connection in mind." |
| 8 Open studio | Self-guided sessions for experienced throwers: 2-hour session ($60) or 10-session pass ($500). Booking, capacity, supervision NS | https://earthandkla.com.au/self-guided-classes/ | "Our Self Guided classes are suited to students who have prior wheel throwing experience" |
| 9 Kilns | Fired in-house by studio staff. Turnaround NS | https://earthandkla.com.au/self-guided-classes/ | (see item 1) |
| 10 Events/rentals | NS (public workshops only) | — | — |
| 11 Equipment list | Partial | — | — |
| 12 Member comms | T&Cs page | https://earthandkla.com.au/terms-conditions/ | — |

<!-- END OF STUDIO BLOCKS -->

## Totals per item (denominator 42)

Counts are studios coded **Y** (or the value named) out of the fixed 42, including the two unreachable rows (51 UBC Pottery Club, 68 Twelve80), which count as NS everywhere. Where a sub-field also has explicit **N** answers, they are shown on their own line; everything else is NS. Counts were taken from the `id:value` codes in the summary table.

Context denominators (not used for the headline counts): **31** of the 42 sites show membership or open-studio access (Canada 9, UK 9, Australia 13); 4 are PYOP; 1 is a production pottery; 2 are unreachable; the rest are class- or workshop-only.

| Item / sub-field | Total /42 | Canada /12 | UK /12 | Australia /18 |
|---|---|---|---|---|
| **1 Size: any size fact stated** | **24** | 6 | 9 | 9 |
| 1a member count or numeric cap | 1 | 0 | 1 | 0 |
| 1b waitlist | 6 | 2 | 3 | 1 |
| 1c number of kilns | 10 | 1 | 4 | 5 |
| 1d number of wheels | 14 | 2 | 5 | 7 |
| 1e area or people capacity | 7 | 2 | 3 | 2 |
| **2 Staff: any title or role shown** | **38** | 11 | 12 | 15 |
| 2a owner or founder | 20 | 6 | 5 | 9 |
| 2b studio manager or coordinator | 11 | 4 | 5 | 2 |
| 2c operations title | 2 | 1 | 1 | 0 |
| 2d technician or kiln tech | 19 | 4 | 8 | 7 |
| 2e instructors or teachers | 30 | 8 | 10 | 12 |
| 2f front desk | 2 | 0 | 1 | 1 |
| 2g volunteers | 3 | 1 | 2 | 0 |
| 2h board | 1 | 1 | 0 | 0 |
| 2i other titles (assistants, admin, bookings, shop, café, production) | 29 | 7 | 11 | 11 |
| **2x someone other than the owner named as running operations: Y** | **12** | 3 | 5 | 4 |
| 2x: N (site says the owner runs it) | 3 | 1 | 0 | 2 |
| **3 Work-exchange, assistant, monitor or volunteer programme: Y** | **6** | 3 | 3 | 0 |
| 3: N (site says it prefers paid staff) | 1 | 0 | 0 | 1 |
| **4a required cleaning or upkeep duty** | **12** | 4 | 4 | 4 |
| 4b chore rotation or co-op work hours | 0 | 0 | 0 | 0 |
| **5a must buy clay from the studio: Y** | **4** | 3 | 0 | 1 |
| 5a: N (outside clay allowed, often with approval) | 12 | 4 | 3 | 5 |
| 5b clay included in membership or classes: Y | 18 | 5 | 6 | 7 |
| 5b: N (clay is an extra cost) | 13 | 4 | 4 | 5 |
| 5c price per bag or box published | 4 | 3 | 0 | 1 |
| 5d purchase method stated (in studio 9, online shop 3, staff 1, self-serve 1, WhatsApp 1) | 15 | 5 | 3 | 7 |
| (derived) site says the studio sells clay | 22 | 8 | 5 | 9 |
| **6a studio glazes provided or included** | **30** | 9 | 10 | 11 |
| 6b outside glazes allowed: Y | 8 | 2 | 3 | 3 |
| 6b: N | 3 | 2 | 1 | 0 |
| 6c members mix glazes: Y | 4 | 0 | 3 | 1 |
| 6c: N | 2 | 1 | 0 | 1 |
| **7 any retail, including gift cards** | **38** | 10 | 11 | 17 |
| 7a shop selling clay, tools, supplies or kits | 15 | 3 | 4 | 8 |
| 7a: N | 1 | 1 | 0 | 0 |
| 7b finished work for sale or consignment | 15 | 2 | 6 | 7 |
| 7c gift cards | 33 | 8 | 10 | 15 |
| 7d online store selling goods | 15 | 3 | 2 | 10 |
| (derived) any goods retail (7a, 7b or 7d) | 23 | 6 | 6 | 11 |
| **8a open-studio reservations required: Y** | **16** | 2 | 6 | 8 |
| 8a: N (no booking needed) | 7 | 1 | 4 | 2 |
| 8b capacity or per-member hours/sessions cap | 17 | 4 | 9 | 4 |
| 8c staffed or supervised: Y | 13 | 3 | 8 | 2 |
| 8c: N (unstaffed or unsupervised sessions) | 2 | 1 | 0 | 1 |
| 8d keys, fob, code or 24-hour access | 12 | 4 | 2 | 6 |
| **9a who loads: staff** | **21** | 4 | 6 | 11 |
| 9a: members / both | 1 / 1 | 0 / 0 | 1 / 1 | 0 / 0 |
| 9b firing frequency or turnaround stated | 25 | 7 | 8 | 10 |
| 9c how firing is requested: stated | 20 | 5 | 7 | 8 |
| — drop-off | 9 | 2 | 5 | 2 |
| — form | 5 | 1 | 0 | 4 |
| — paper slip or ticket | 2 | 1 | 1 | 0 |
| — booked drop-off slot | 2 | 0 | 0 | 2 |
| — app | 1 | 0 | 1 | 0 |
| — labelled firing shelf | 1 | 1 | 0 | 0 |
| 9d firing queue, calendar or status published | 2 | 0 | 1 | 1 |
| **10a parties, private or team events: Y** | **31** | 8 | 10 | 13 |
| 10a: N (paused) | 1 | 0 | 1 | 0 |
| 10b studio, kiln or equipment rental | 18 | 3 | 8 | 7 |
| **11 equipment list: Y** | **9** | 2 | 4 | 3 |
| 11: Partial | 13 | 4 | 3 | 6 |
| **12 any member communication visible** | **32** | 8 | 11 | 13 |
| 12a newsletter or mailing list | 22 | 4 | 9 | 9 |
| 12b login, account or members' page | 14 | 4 | 5 | 5 |
| 12c Slack, Discord, WhatsApp or Facebook group | 2 | 0 | 1 | 1 |
| 12d rules, policies or handbook | 23 | 6 | 6 | 11 |

Within the 31 studios that show member or open-studio access: 12 require members to clean (4a); 15 require reservations and 4 say none are needed (8a); 13 cap hours, sessions or places (8b); 9 say the time is staffed or supervised and 2 say it may not be (8c); 12 offer key, fob, code or 24-hour access (8d).

## Totals by segment and by country

**Segment mix (one label per studio):** teaching studio 25 · membership/community studio 8 · PYOP 4 · municipal/recreation 3 · university/college/school 1 · production pottery 1 · cooperative 0 · nonprofit art centre 0 · makerspace 0. Two of the community studios are not-for-profit CICs or social enterprises (Clay Studio Manchester, Creative Bloc) and Clay Time is a not-for-profit CIC teaching studio; WFAC is a City of Fremantle arts centre and is counted as municipal. 21 of the 25 teaching studios also sell membership or open-studio access, so "teaching" means "classes lead the site", not "classes only".

**By country:** Canada 12 (teaching 7, membership 1, PYOP 1, municipal 2, university 1) · UK 12 (teaching 3, membership 6, PYOP 3) · Australia 18 (teaching 15, membership 1, municipal 1, production 1). Country totals for every sub-field are in the table above.

**By segment** (counts of Y; N counts in brackets where the site said No):

| Sub-field | Teaching /25 | Membership /8 | PYOP /4 | Municipal /3 | University /1 | Production /1 |
|---|---|---|---|---|---|---|
| 1 any size fact | 14 | 7 | 3 | 0 | 0 | 0 |
| 1b waitlist | 2 | 3 | 1 | 0 | 0 | 0 |
| 1c kilns counted | 5 | 5 | 0 | 0 | 0 | 0 |
| 1d wheels counted | 9 | 5 | 0 | 0 | 0 | 0 |
| 2a owner or founder | 15 | 4 | 1 | 0 | 0 | 0 |
| 2b studio manager or coordinator | 6 | 3 | 1 | 1 | 0 | 0 |
| 2c operations title | 0 | 2 | 0 | 0 | 0 | 0 |
| 2d technician | 11 | 5 | 1 | 2 | 0 | 0 |
| 2e instructors | 20 | 6 | 3 | 1 | 0 | 0 |
| 2x non-owner runs operations | 8 (N 2) | 2 (N 1) | 1 | 1 | 0 | 0 |
| 3 work-exchange programme | 4 (N 1) | 2 | 0 | 0 | 0 | 0 |
| 4a cleaning duty | 8 | 4 | 0 | 0 | 0 | 0 |
| 5a must buy studio clay | 3 (N 8) | 0 (N 3) | 0 | 1 (N 1) | 0 | 0 |
| 5b clay included | 12 (N 9) | 4 (N 3) | 1 | 1 (N 1) | 0 | 0 |
| studio sells clay (derived) | 16 | 4 | 0 | 2 | 0 | 0 |
| 6a studio glazes included | 20 | 5 | 4 | 0 | 0 | 1 |
| 6b outside glazes allowed | 5 (N 2) | 3 | 0 (N 1) | 0 | 0 | 0 |
| 6c members mix glazes | 1 (N 2) | 3 | 0 | 0 | 0 | 0 |
| 7a supplies/tools/kits shop | 9 | 3 | 1 | 2 | 0 | 0 |
| 7b finished work sold | 9 | 3 | 1 | 1 | 0 | 1 |
| 7c gift cards | 23 | 5 | 4 | 0 | 0 | 1 |
| 7d online store of goods | 11 | 0 | 1 | 2 | 0 | 1 |
| any goods retail (derived) | 15 | 3 | 1 | 3 | 0 | 1 |
| 8a reservations required | 9 (N 2) | 4 (N 2) | 1 (N 3) | 1 | 0 | 1 |
| 8b capacity or hours cap | 6 | 5 | 4 | 1 | 0 | 1 |
| 8c staffed or supervised | 4 (N 2) | 4 | 3 | 1 | 0 | 1 |
| 8d keys/fob/code/24-hour | 9 | 3 | 0 | 0 | 0 | 0 |
| 9a staff load | 14 | 2 (+1 members, +1 both) | 4 | 0 | 0 | 1 |
| 9b turnaround or frequency | 14 | 4 | 4 | 2 | 0 | 1 |
| 9c request method stated | 11 | 3 | 4 | 1 | 0 | 1 |
| 9d queue or status published | 1 | 0 | 1 | 0 | 0 | 0 |
| 10a events | 21 | 5 (N 1) | 4 | 0 | 0 | 1 |
| 10b rentals | 11 | 5 | 2 | 0 | 0 | 0 |
| 11 equipment list Y (Partial) | 4 (10) | 5 (2) | 0 | 0 | 0 | 0 (1) |
| 12a newsletter | 15 | 6 | 1 | 0 | 0 | 0 |
| 12b login or members' page | 10 | 3 | 1 | 0 | 0 | 0 |
| 12c group chat | 1 | 1 | 0 | 0 | 0 | 0 |
| 12d rules or policies | 17 | 4 | 0 | 2 | 0 | 0 |

Small cells (PYOP 4, municipal 3, university 1, production 1) are too small for percentages; the university and one membership row are the two unreachable sites.

## Notable verbatim examples

All accessed 2026-10-20; each also appears in the studio's evidence block above.

**Who runs the studio**
- Clay With Me (Toronto), Studio Coordinator job ad, $19/h: "Oversee the daily operations at our studios" · "Monitor inventory of clay, glazes, tools, packaging, cleaning supplies, and retail products" · "Order materials and supplies before inventory runs low" · "Schedule equipment servicing and repairs" · "Help coordinate instructor schedules" — https://claywithme.ca/pages/workwithus
- The Kiln Rooms (London), one of 37 people listed: "Lisa Darrer": "General Manager" — "she ensures the smooth running of studio operations" — https://www.thekilnrooms.com/the-team
- Centred Clay Studios (Calgary): "Owner, Studio Manager" and "Owner, Operations Manager." — https://www.centredclaystudios.com/page-4/
- Everyday Clay (Healesville): "Amanda Reishi is the maker behind Everyday Clay (yes, we're a one-person show over here!)." — https://www.everydayclay.com.au/
- Atelier Forma (Montréal), on its partner: "has created and implemented multiple human-scale and computer-level systems which have helped streamline our services" — https://atelierforma.ca/pages/team

**Work-trade instead of payroll**
- The Kiln Rooms: "In exchange we ask for 7 hours a week hands on studio support alongside one of our technicians." · "We spend time training people in how to manage a studio, mix glazes, pack/service/manage kilns" — https://www.thekilnrooms.com/work-exchange-programme
- Atelier Forma, Studio Assistant (work-trade): "Tasks include greeting customers, logging information, supervising open studio sessions, and maintaining cleanliness." — https://atelierforma.ca/pages/work-with-us
- Bisque Studios (Melbourne), the opposite view: "But we also value peoples time and prefer to pay employees rather than seek out volunteers." — https://bisquestudios.com/booking-refund-policies/

**Kilns and the piece loop**
- Turning Earth (London): "Kiln-loading handled by technicians." · "Firings run to a schedule and can't be brought forward for one person's deadline." — https://www.turningearth.org/membership · https://www.turningearth.org/faq
- Lygon Street Pottery (Melbourne): "Staff will load the kilns so it's not necessary for members to know how to do this" — https://lygonstreetpottery.com.au/memberships
- City of Pickering: "A new ticketing system will be implemented to manage kiln space and maintain a steady flow of work through the studio." — https://www.pickering.ca/potterystudio
- Bonbon Pottery (London): "Rather than calling the studio, you can check the status of your pieces online at any time through our order tracker." — https://www.bonbonpottery.co.uk/pickup-and-care
- Clay Studio, Manchester: "We don't email to inform you to collect your items." — https://www.claystudio.co.uk/kiln-and-clay
- Social Pottery (London), two pages that disagree: "we message you the moment it is" (FAQ) vs "No message goes out: count from your session date, and call the studio if you want to check before travelling." — https://socialpottery.com/frequently-asked-questions/ · https://socialpottery.com/pickup/
- Terrapotter Studios (Melbourne): "Keep up to date with your firings, check in regularly with our Instagram account, or feel free to message/email us." — https://www.terrapotterstudios.com/publicfiring

**Clay, stock and sales**
- Pot Dispensary (Melbourne): "Team WhatsApp for updates & clay orders" — https://www.potdispensary.com.au/memberships
- Parkdale Pottery (Toronto): "Clay (we have a self serve option for in house stock clay for purchase by the bag or box)" — https://parkdalepottery.ca/pages/membership
- The Mud Oven (Ottawa, PYOP): "We have hundreds of items in stock at all time, with a wide range of prices to suit every budget." — https://www.mudoven.com/
- Clay Studio, Manchester: "the shop stocks work created by the members and other local makers" · "Commercial wholesale prices available please email us." — https://www.claystudio.co.uk/the-shop
- Everyday Clay: "After booking, you’ll receive an invoice with our account details." — https://www.everydayclay.com.au/glazing-firing-booking
- Urban Clay (Melbourne): "The Studio reserves the right to retain completed pieces until all relevant firing fees have been paid in full." — https://www.urbanclay.com.au/terms-conditions/

**Access, booking and rules**
- Turning Earth: "You can visit any time during opening hours with no booking required." · "Members can come in for a maximum of 15 hours a week, or 22 on Studio Membership Plus." — https://www.turningearth.org/membership · https://www.turningearth.org/faq
- Clay Shed (Bristol): "Book wheels, kilns, and equipment easily via our app" — https://www.clayshed.co.uk/ceramics-membership-bristol
- Parkdale Pottery: "At the Dupont location, there may not be staff on-site when you are in the studio" — https://parkdalepottery.ca/pages/membership
- Clay Studio, Manchester: members who don't clean "will be required to pay a 20% surcharge on their membership fee to pay for staff cleaning." — https://www.claystudio.co.uk/terms

## Hypothesis evidence

Ratings use this scale: **strong** (most studios show the need, in several ways) · **moderate** (a large minority show it, or it is common but indirect) · **weak** (a few direct mentions) · **none** (no evidence either way) · **contradicts** (evidence points the other way). All counts are out of 42 unless stated. Public sites under-report back-office work, so "weak" or "none" here means "not visible", not "not needed".

### 1. Inventory tracking (clay, glaze materials, retail stock, PYOP bisque): **moderate**

For:
- **22** sites say the studio sells clay (Canada 8, UK 5, Australia 9); **13** say clay is an extra cost on top of membership or access (5b:N). Clay is bought in informal ways: self-serve from house stock (Parkdale), "ask one of our team members" (Atelier Forma), "Team WhatsApp for updates & clay orders" (Pot Dispensary), "available for purchase in studio pending stock" (Terrapotter).
- **23** sell goods of some kind: supplies, tools or kits **15**; finished work **15**; an online store of goods **15**. Clay Studio Manchester's shop carries members' work, local makers' work, its production potter's ware, clay and tools.
- PYOP and kit businesses hold bisque stock: "hundreds of items in stock" and mobile bookings need "2-4 weeks advance notice to ensure we can stock the desired items" (The Mud Oven); Social Pottery's 300–500-piece collection; at-home kits at Pot Dispensary, Everyday Clay, Not Yet Perfect and Pottery Café.
- The one detailed job description in the sample (Clay With Me, Studio Coordinator) puts inventory first: "Monitor inventory of clay, glazes, tools, packaging, cleaning supplies, and retail products" and "Order materials and supplies before inventory runs low".

Against / limits:
- Only **4** publish a clay price; **12** let members bring outside clay; **7** include clay in membership or open-studio access (Centred Clay's first block, Art Craft's freestyle sessions, Social Pottery, Creative Bloc's practice clay, free reclaim at Sunroom and Neo, a bag in WFAC's open-studio course), and Slow Clay's courses include "UNLIMITED clay".
- The most common retail item is the gift card (**33**), which is a stored-value liability, not stock.
- Only **1** site (Clay With Me's job ad) names inventory work; no site mentions stock counts, reorder points or stock-outs beyond Terrapotter's "pending stock".

### 2. Asset and equipment tracking: **weak** as "tracking"; moderate if reframed as bookable and hireable assets

For:
- **22** publish an equipment list (9 full, 13 partial), often with makes and sizes (Clay Shed's four kilns by litre; Lygon Street's three kilns, each priced for hire; Pot Dispensary's eight wheels by make).
- **18** rent something out: whole-kiln hire **7** (Trace, Creative Bloc, Sunroom, Pot Dispensary, Lygon Street, Maker's Loft, Clay Studio Manchester's members' kiln hire), wheel or equipment hire **4** (Kiln Rooms, Urth's $145-a-month home wheel with a $250 bond, Maker's Loft, Not Yet Perfect), studio or venue hire **11**.
- Maintenance appears as a duty at **3**: Clay With Me ("Coordinate routine maintenance of pottery wheels, kilns, slab rollers, extruders, and studio equipment"; "Schedule equipment servicing and repairs"), Turning Earth technicians ("maintaining equipment"), The Kiln Rooms (a Technical Manager; exchangees learn to "pack/service/manage kilns").
- Damage charges show the cost of equipment: kiln-damage fees at Atelier Forma, shelf fees at Terrapotter ($50/$250) and ClayMake ($10), member liability at Clay Studio Manchester.

Against / limits:
- No site mentions an asset register, maintenance log, element or firing-count tracking, or downtime. Equipment lists read as marketing ("15 top-of-the-range Shimpo pottery wheels").
- Only **1** site states equipment servicing as a scheduled task. Full equipment lists are concentrated in membership studios (5 of 8) rather than teaching studios (4 of 25).

### 3. Sales integrations with multiple payment providers: **weak**

For:
- Studios sell through many channels: gift cards **33**, online goods **15**, events **31** (usually with deposits: Zei 30%, Pottery Lab 40%, Social Pottery 50%, The Mud Oven "$10 per seat" card hold, Maker's Loft $100, Pottery Café £5 per painter), firing sold by weight or litre online (Clay Studio Manchester, Maker's Loft), plus in-person payment ("Payment is per piece and should be made at the front desk" at Robert Gordon; drop-ins "done in person" at ClayMake; "weigh and pay" at Slow Clay; "settle your payment" at Terrapotter), invoices with bank details (Everyday Clay), payment links (Terrapotter) and bonds (Urth, The Kiln Rooms).

Against:
- No site shows two card processors side by side. Square is visible only as a gift-card link at **4** (Create Art Studio, The Mud Oven, Bonbon, Pottery Café); everything else runs through the storefront's own checkout (03's census). The evidence supports **one provider used across several channels (online, in person, invoice)**, not several providers.

### 4. QuickBooks Online integration: **none**

- **0** of 42 sites mention QuickBooks or any accounting package. That is expected, because accounting is invisible on public sites, so the audit cannot confirm or reject the hypothesis.
- Indirect signals only: invoices (Everyday Clay), unpaid fees deducted from deposits (The Kiln Rooms), finished work held until fees are paid (Urban Clay), and finance roles at two public bodies (OSCA's Bookkeeper, WFAC's Finance & Systems Coordinator), which will use their own systems.
- 30 of the 42 studios are in the UK and Australia. This sample cannot show which accounting tool studios there use, so a QuickBooks-first choice needs interview evidence, country by country.

### 5. Scheduling: **strong** for resources, events and staff; **weak** for replacing class booking

For:
- **16** require reservations for open studio or sessions, with tools named on the sites: Jackrabbit portal (Create Art Studio), Bookwhen links (The Kiln Rooms), bsport members' calendar (Social Pottery), Setmore (Clay Studio Manchester), Eventbrite (Creative Bloc), an unnamed app (Clay Shed), an online scheduler or booking code (Terrapotter, Neo).
- **17** cap capacity or usage: Turning Earth 15 or 22 h/week; The Kiln Rooms and Studio Pottery London 18 h/week; Trace 16 h/week; Pottery Lab 10 sessions a month; Atelier Forma 34 or 60 h per 4 weeks; Tendre 9 h/week; Urth 4 or 8 h/week; ClayMake 40 h/month; Social Pottery 10 or 60 h/month; Creative Bloc 4 sessions a month; Clay Studio Manchester 4 sessions a week.
- Resource-level booking exists: "Book wheels, kilns, and equipment easily via our app" (Clay Shed); "reserve a wheel or table" (Creative Bloc); booked firing drop-off slots (Everyday Clay, Pot Dispensary); kiln hire at 7.
- **31** run private or team events, mostly booked by enquiry form or email plus a deposit, not self-serve.
- Staff and helper scheduling: "Help coordinate instructor schedules" and "Monitor attendance and staffing" (Clay With Me); a Bookings Manager (The Kiln Rooms); exchange shifts "scheduled together week by week" (Clay With Me).

Against:
- **7** say no booking is needed (Turning Earth members, Maker's Loft wheels, ClayMake drop-ins, Trace members after induction, three PYOP studios for walk-ins), and The Kiln Rooms says "Wheels and areas cannot be reserved". **12** give key, fob, code or 24-hour access, where the limit is an hours cap rather than a time slot.
- Class and session booking is already done by the storefront or booking tool each studio uses (03), so a new class-booking calendar would compete with tools already in place.

## Other strong signals

1. **Kiln work is staff work, and the piece loop is the core operational job.** Staff load at **21** studios; members load at 1 (Kiln Rooms' Copeland Park, after induction) and it is split at 1 (Clay Shed, by membership tier). **25** state a turnaround or firing frequency, **20** say how firing is requested (drop-off 9, form 5, paper slip or ticket 2, booked slot 2, app 1, labelled shelf 1), **13** promise a "ready" email or notice. **4** leave it to the customer for at least one flow: two say no message is sent (Clay Studio Manchester kiln use; Social Pottery painted work), Clay Time asks people to phone, and Clay With Me says "we are unable to check the status of individual pieces due to volume". **16** publish a collection deadline (7 days to 3 months), usually followed by disposal or donation. Only **2** publish status: Bonbon's online order tracker and Terrapotter's Instagram. This supports the piece and firing tracker more than any of the five hypotheses.
2. **Firing for outsiders is a revenue line.** 11 sell firing or kiln time to non-members (Atelier Forma, Trace, Creative Bloc, Sunroom, Urth, Pot Dispensary, Terrapotter, Lygon Street, Maker's Loft, ClayMake, Not Yet Perfect), and Everyday Clay fires its own kit customers' work. 5 refuse outside work (Parkdale when kilns are full, Bisque, The Kiln Rooms, Pottery Café, Turning Earth for kiln rental). Intake is by form, photo, weigh-and-pay, order sheet or email quote.
3. **Studios trade studio time for labour.** 6 programmes (Canada 3, UK 3, Australia 0), 3–7 hours a week: Clay With Me 4 h for 4 h; Parkdale 3–4 h for equal time; Atelier Forma's work-trade Studio Assistant; The Kiln Rooms 7 h a week for free membership (18–20 places); Clay Shed's Tech Exchange, 4 h for membership; Clay Time's volunteers. The duties listed (reclaim, wedging, cleaning, greeting, logging information, supervising open studio, kiln packing, glaze mixing, test tiles) are studio-manager tasks, so a tool that "stands in for a studio manager" would be assigning work to these people.
4. **A management layer exists at about a quarter of studios.** 11 show a studio manager, coordinator or operations title; 12 name someone other than the owner as running operations; 19 list technicians; 3 are plainly owner-run. The bigger operators have layers: The Kiln Rooms lists a General Manager, site Studio Managers, Assistant Studio Managers, a Technical Manager and a Bookings Manager. Roles in the product need more than "owner" and "member": technician, coordinator, bookings, work-trade helper.
5. **Multi-site operation is common.** 12 of 42 run two or more sites (Turning Earth 4, The Kiln Rooms 5, Bonbon 5, Social Pottery 5, Urth 3, and seven with 2), and both PYOP chains franchise.
6. **Events and hire are near-universal side businesses.** 31 run private or team events and 18 rent space, kilns or equipment; events are booked by enquiry, quote and deposit.
7. **Gated access and approvals.** 8 require or provide an induction, orientation or glaze demo before independent use (required at Clay With Me, Parkdale, Turning Earth, The Kiln Rooms, Maker's Loft, ClayMake and Atelier Forma; offered free at Clay Shed, where it covers "kiln loading & firing"). 9 run an approval step for outside clay or glaze (test tile, staff sign-off or "glaze tech" permission). This matches the certification and approval needs in 03.
8. **Member communication is thin and email-first.** 22 newsletters, 14 logins (mostly storefront accounts), 23 rules or policy pages, 2 handbooks or guides (Turning Earth, Clay Studio Manchester), and only 2 member group chats (Turning Earth's Facebook page; Pot Dispensary's WhatsApp, which also takes clay orders).
9. **Two clay models.** Clay is either a sold add-on (13 say it costs extra; 22 sell it) or a bundled material (7 include it in membership or open-studio access, two of them as free reclaim). The second model needs little or no clay inventory.

## Gaps and limits

- **Reach:** 40 of 42 sites were opened. **UBC Pottery Club** (TLS certificate error on its CampusGroups host) and **Twelve80** (Bubble.io app, no server-rendered text) are unreachable and coded NS throughout; search-result summaries for both are recorded as UNVERIFIED and not counted. **OSCA** is thin: its programme listings load by script on Amilia and returned "Loading...". The census URLs for **The Mud Oven** (Tock, 403) and **Creative Bloc** (an expired Eventbrite listing) were replaced by the studios' own sites (mudoven.com, found through a directory redirect; creativeblocglasgow.com, found by trying likely domains).
- **Wayback Machine:** web.archive.org is blocked for WebFetch, so no snapshots could be used.
- **WebSearch budget:** the search budget, which is shared with other agents in this session, ran out partway through; own sites for The Mud Oven and Creative Bloc were then found without search, and UBC's pages were located once the budget refilled near the end.
- **Processed text:** WebFetch returns a model-processed view with a 125-character quote limit, so long sentences appear as fragments, and some answers were paraphrased. Only text returned inside quotation marks is quoted; paraphrased content is described in the Finding column without quotation marks. One page (Clay Shed's facilities) twice refused to quote and was re-asked.
- **Page cap:** 4–9 pages per studio. Pages not opened include Clay Shed's Ceramics Market, The Kiln Rooms' wheel-hire page, WFAC's "Opportunities" and venue pages, and most class product pages. Parkdale's holiday-market page returned 404 and Neo Ceramics' about page returned 404.
- **PDFs and members' areas:** the Clay Shed Tech Exchange specification PDF could not be decoded by WebFetch (the tool kept a copy in its own cache, which was not opened), and Clay Studio Manchester's Membership Guide PDF was not opened. Password-protected areas (Slow Clay's Studio Access Times, Bisque's student page, Clay Shed's members' area) hide exactly the internal rules this audit looks for.
- **NS is not No:** low counts such as chore rotations (0), group chats (2) or member caps (1) show what sites publish, not what studios do. Back-office items (inventory, maintenance, accounting) are the least likely to be published, which biases hypotheses 1, 2 and 4 downward.
- **Coding judgements** (one coder): segment labels (a single label for hybrid studios; 21 of 25 "teaching studios" also sell access); 8c for PYOP rows reads the painting floor as staffed; 12b counts storefront customer accounts; 1e counts people capacity as a size fact; 9a "staff" includes sites that only say staff fire class work. The coding rules are written at the top so the US half can apply the same ones.
- **Conflicts inside sites** are recorded rather than resolved: Clay With Me (outside clay allowed vs not), Studio P (4 vs 3 kilns), Social Pottery (ready message vs none), Clay Studio Manchester (email for classes, none for kiln use), Lygon Street ("two electric kilns" with three listed).
- **Not re-collected:** booking and commerce platforms and firing fees, as instructed (they are in 03). Platform names appear here only where they are the booking tool asked about in item 8.
- **Timing:** all pages accessed 2026-10-20. Some content is old or seasonal (OSCA's search hits are 2024 listings; Creative Bloc's Eventbrite sales ended March 2025; several class pages show "Sold out").