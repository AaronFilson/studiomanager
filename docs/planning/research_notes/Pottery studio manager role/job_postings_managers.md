# Job postings: what pottery and ceramics studios ask of the people who run them

_Researched 2026-10-20 (all sources accessed that day). Status: COMPLETE. The corpus has 39 distinct postings, short of the 50 target because reachable sources ran out (see Gaps and limits)._

## Plan

1. Discover candidate postings with a small number of searches (general US queries, PYOP chains, nonprofit art centres, universities, municipal/recreation, makerspaces, then UK, Canada, AU/NZ). Prefer fetching known job-board and studio URLs directly over searching.
2. Open every posting myself with WebFetch and pull verbatim text: title, employer, location, date, FT/PT and hours, pay, benefits, reports to, supervises, duties, software, outputs, frequencies. Anything I could not open is marked UNVERIFIED and is not counted in the corpus.
3. Scope filter: roles that run or co-run studio operations (studio manager, studio coordinator, operations manager, studio director, assistant manager, membership/program coordinator). Technician postings only if hybrid manager/technician (tagged HYBRID-TECH). Reposts (same employer + title within 6 months) count once, latest kept.
4. Code each posting against the duty codes (D01-D18, plus added codes), software, outputs, hours/pay, reporting line, supervises. Keep one verbatim example phrase per code.
5. Append findings every few sources. Close with counts (with denominators) by segment and country, then "Hypothesis evidence", "Other strong signals", "Gaps and limits".

Earlier research not redone: `03-tools-in-use.md` (84-studio tool census) and `01-market-survey.md` (competitors, pain points). This file is about job postings only.

## Coding scheme

Duty codes (from the brief; added codes are marked "added"):

| Code | Duty |
|---|---|
| D01 | Kiln operations (loading, firing, unloading, firing schedules) |
| D02 | Kiln and equipment maintenance and repair |
| D03 | Glaze mixing, testing and upkeep |
| D04 | Clay, reclaim and materials |
| D05 | Inventory, ordering, purchasing and receiving |
| D06 | Members' work in progress (tracking pieces through firing, shelves, pickup) |
| D07 | Member management (onboarding, orientations, memberships, billing, rules) |
| D08 | Classes and programs (class scheduling, instructor coordination, registration) |
| D09 | Teaching |
| D10 | Front desk and customer service |
| D11 | Retail and point of sale |
| D12 | Staff, volunteer and work-exchange management (hiring, training, shift scheduling, supervision) |
| D13 | Cleaning, facility and safety (silica, ventilation, SDS, incidents) |
| D14 | Events, parties and rentals |
| D15 | Finance and bookkeeping (QuickBooks, invoicing, payroll, budgets) |
| D16 | Marketing and communications |
| D17 | Reporting (metrics, board or grant reports) |
| D18 | Gallery, exhibitions and consignment |
| D19 | (added) Artist residency programme (selection, mentoring, residents' hours) |
| D20 | (added) Equipment and asset tracking: equipment inventory, FF&E counts, documented inspections, mould inventory |
| D21 | (added) Production order / work-in-progress tracking in production potteries (work orders, WIP, finished goods, yield loss) |
| D22 | (added) Booking of shared resources: open-studio sign-ups, kiln/spray-booth sign-up, facility reservations, kiln rentals, shared studio calendar |

Coding rules: a code is counted only when the posting's own words (as returned verbatim) support it. Where WebFetch returned a summary instead of the posting's words, the code is listed as "summary-only" and not counted. Weak codes are flagged in Appendix A.

Segments: membership/community studio (COMM), teaching studio (TEACH), cooperative (COOP), nonprofit art centre (NPO), paint-your-own-pottery (PYOP), production pottery (PROD), university/college/school (UNIV), makerspace (MAKER), municipal/recreation (MUNI).

## TL;DR

- **Corpus: 39 distinct postings**, each opened and coded (target 50; see Gaps and limits). By country: 37 US, 1 UK, 1 Canada, 0 Australia, 0 New Zealand. By segment: 22 nonprofit art centres, 6 university, 5 PYOP, 3 community studios, 2 production potteries, 1 municipal. None are cooperatives, makerspaces or pure teaching studios. By date: 28 are from 2023–2026 or were live when accessed, and 11 are older and labelled PRE-2023 (one 2016, one 2020, two 2021, seven 2022). 4 of the 39 are tagged HYBRID-TECH.
- **The job is mostly running the physical studio and coordinating people.** The commonest duties are cleaning, facility and safety (32 of 39), inventory, ordering and purchasing (30), staff, volunteer and student-worker management (28), equipment maintenance and repair (27), and kiln operations (24). Classes and programmes come next (23), then member management, counted broadly as orientations, rules and access (21).
- **Postings rarely name software.** 23 of 39 name no product at all. Named products: Microsoft Office or Outlook (12), Google Workspace (9), Mailchimp or Constant Contact (3), PayPal (2), Adobe (3), and one each of QuickBooks (a 2020 posting), NetSuite, Airtable, Monday.com, FileMaker, WebCheckout, KilnFire, HyperGlaze and WordPress. **Square, Stripe, Shopify, Xero, Mindbody and kiln-controller software are each named 0 of 39 times.**
- **The hypotheses fare differently.** Inventory: strong as a duty (30 of 39), weak as a stated software expectation (3 of 39). Asset and equipment tracking: moderate (maintenance 27 of 39, but asset registers or inspection records only 6). Multi-provider sales integrations: weak, and none at all for Stripe or Square (0 of 39). QuickBooks Online: weak (1 of 39, and 0 of 28 recent postings). Scheduling: strong (32 of 39), but it covers four different jobs: class and open-studio calendars (23), firing schedules and logs (11), staff and volunteer rosters (8), and bookings, sign-ups and rentals (8).
- **Pay and hours.** Full-time salaried US roles: median of range midpoints $52,135 (n=10, $38,000–$71,962). For 2023 onward the median is $56,500 (n=5). Hourly roles: median $22 (n=11, $16.50–$32). 14 of 39 are part-time, with a median of 20 h/week (n=13, range 12–30).
- **Reporting lines.** 11 of 39 report to a programme, education, artistic or ceramics director, 10 to the top of the organisation (executive director, president, museum director, owner or founder; 4 of these 10 are implied), 7 to another manager or academic administrator, and 11 don't say. **No posting reports to a board.** Only 2, both PYOP, imply the owner or founder as the boss.

## Corpus summary

| Segment | n | Countries | 2023–26 or live | PRE-2023 |
|---|---|---|---|---|
| Nonprofit art centre (NPO) | 22 | US 21, CA 1 | 15 | 7 (P09 P16 P17 P22 P23 P26 P28) |
| University/college (UNIV) | 6 | US 6 | 4 | 2 (P11 P27) |
| Paint-your-own pottery (PYOP) | 5 | US 5 | 5 | 0 |
| Membership/community studio (COMM) | 3 | US 2, UK 1 | 1 | 2 (P07 P15) |
| Production pottery (PROD) | 2 | US 2 | 2 | 0 |
| Municipal/recreation (MUNI) | 1 | US 1 | 1 | 0 |
| Teaching studio, cooperative, makerspace | 0 | – | – | – |
| **Total** | **39** | US 37, UK 1, CA 1, AU 0, NZ 0 | **28** | **11** |

HYBRID-TECH (a manager or coordinator title whose duties are mostly technician work): P30, P33, P34, P36. They are counted in the corpus, and any figure can be recomputed without them from the table.

Sources the corpus came from: CeramicsNow jobs board (14), UW School of Art advising blog (5), SimplyHired live listings (8), Greater Philadelphia Cultural Alliance job bank (2), Handshake share previews (2), studios' own pages (4), East City Art (1), Associated Artists of Pittsburgh (1), LinkedIn (1), Betterteam ATS (1), Craigslist (1). Some postings came through more than one source.

## Posting table

Duty codes are listed without the "D". "Date" is the posted date, or else the deadline. Where only an aggregator's relative date was available, it is given as "~Oct 2026". "Live" means the page was open on 2026-10-20 but undated. No archive URLs: the Wayback Machine was unreachable from this toolset. "alt" is a second live copy of the same posting.

| ID | Title | Employer | City | Ctry | Seg | Date | FT/PT, hours | Pay as written | Reports to | Supervises | URL | Duty codes | Software named | Outputs named |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| P01 | Ceramics Studio Manager | American Museum of Ceramic Art | Pomona CA | US | NPO | 2025-04-28 | FT, 40 h, non-exempt | "$62,000" | Executive Director | not stated ("Recruit and train staff") | https://www.ceramicsnow.org/jobs/ceramics-studio-manager-at-the-american-museum-of-ceramic-art/ (alt ceramicsnow.org/?p=38761) | 01 02 03 05 08 10 11 12 13 14 15 16 19 | MS Office, Gmail, Google Calendar, unnamed POS, Mailchimp, Linktree, Facebook, Instagram | calendar, timesheets, contracts, class registration, attendance, kiln logs, studio glazes, budget/expense tracking |
| P02 | Community Pottery Studio Coordinator | Folk Pottery Museum of NE Georgia | Sautee Nacoochee GA | US | NPO | live, undated | PT, ~16 h | not stated | FPM Director (implied) | none | https://www.folkpotterymuseum.com/jobs | 01 02 03 04 05 07 08 10 13 14 16 | KilnFire app, email | kiln-firing logs, supply orders for approval, recycled clay, mixed glazes |
| P03 | Ceramics Studio Manager | Community Arts Center | Wallingford PA | US | NPO | 2026-07-28 | PT salaried, ~30 h | "$30,000.00"–"$32,000.00" | Program Director | kiln techs | https://portal.philaculture.org/node/75005 | 01 02 03 05 07 08 12 13 14 18 | none | electric-kiln firing schedule, repair estimates, kiln-tech schedules, glaze safety labels, open-studio schedule, annual sale |
| P04 | Studio Manager | Pittsburgh Center for Arts & Media | Pittsburgh PA | US | NPO | 2026-03-06 | FT, M–F | "$40,000 per year" | not stated | not stated | https://www.aapgh.org/artist-opportunities/2026/3/6/studio-manager-pittsburgh-center-for-arts-amp-media-job-opportunity | 01 02 03 05 07 08 11 13 16 17 19 | MS Office, Google Drive | studio policies and rules, member onboarding, open-studio schedule |
| P05 | Ceramics Studio Manager | Minot State University | Minot ND | US | UNIV | 2026-08-15 | PT, hours n/s | not stated | not stated | student workers (optional) | https://www.ceramicsnow.org/jobs/ceramics-studio-manager-at-minot-state-university/ (alt ?p=46654) | 01 02 04 05 12 13 15 | none | materials and supply orders |
| P06 | Ceramic Studio Manager | Ilwaco Artworks (Sou'wester Arts) | Ilwaco WA | US | COMM | 2024-04-03 | 30 h incl. weekends | "$23+/hr DOE" | not stated | not stated | https://www.ceramicsnow.org/jobs/ceramic-studio-manager-at-ilwaco-artworks/ | 01 05 06 07 08 09 10 13 14 16 | none | firing/work-tracking records, clay inventory, contact and attendance lists, shipped work |
| P07 | Studio Manager | Studio Pottery London | London | UK | COMM | 2022-01-24 PRE | FT, 5 days | "£31,000 per annum, FTE" | Studio Director | not stated | https://www.ceramicsnow.org/jobs/studio-manager-at-studio-pottery-london/ (alt ceramicsnow.org/?p=16858) | 10 14 15 | none | invoices, schedules |
| P08 | Art School Ceramics Programs Manager | Flint Institute of Arts | Flint MI | US | NPO | 2025-11-17 | FT, exempt | not stated | Director of Art School | not stated (trains instructors and technicians) | https://www.ceramicsnow.org/news/the-flint-institute-of-arts-is-hiring-an-art-school-ceramics-programs-manager/ | 01 02 06 08 09 12 13 16 | none | course plans |
| P09 | Ceramics Studio Coordinator | University City Arts League | Philadelphia PA | US | NPO | 2022-06-15 PRE | PT, 20 h | "$22 an hour" | Executive Director | volunteers, student interns | https://www.ceramicsnow.org/jobs/ceramics-studio-coordinator-at-university-city-arts-league/ | 01 02 03 04 05 06 07 08 12 13 16 17 | MS Office, Google Drive | session orientations, session emails, class surveys, roster-based shelf assignment, purchasing and inventory |
| P10 | Ceramics/Sculpture Studio Manager | California College of the Arts | San Francisco CA | US | UNIV | 2023-05-24 | FT, 37.5 h | "$32 per hour" | not named | 10–12 student workers, 2 grad assistants | https://www.ceramicsnow.org/jobs/ceramics-sculpture-studio-manager-at-california-college-of-the-arts/ (alt ?p=26588) | 02 04 05 07 08 12 13 15 20 22 | Google Suite, WebCheckout, Airtable, Adobe Suite | student-worker schedule, timesheets, annual and labour budgets, manuals, orientation/access records, shared calendar, SDS catalog, material-usage tracking, equipment inventory |
| P11 | Ceramics Studio Manager | Princeton University (New College West) | Princeton NJ | US | UNIV | 2022-05-09 PRE | PT, 20 h, 10-month | not stated | Dean via College Program Administrator | student staff | https://www.ceramicsnow.org/jobs/ceramics-studio-manager-at-princeton-university/ (alt ?p=18620) | 02 05 07 08 09 12 13 15 16 | unnamed "sign-in software", webpage, email | studio budget, student-staff schedule, publicity |
| P12 | Pottery Painting Studio Manager | Color Me Mine Frisco | Frisco TX | US | PYOP | undated; Jooble card "6 days ago" (~Oct 2026) | FT | not stated | Studio Owner (implied) | studio staff | https://app.joinhandshake.com/jobs/11334402/share_preview | 02 05 10 11 12 13 14 16 20 | none named ("software and technology adopted by Color Me Mine") | staff roster, performance and compensation reviews, SKU pricing, inventory and FF&E counts, quarterly safety audit |
| P13 | Pottery/Ceramics Studio Manager and Visual/Community Arts Support | View Arts Center | Old Forge NY | US | NPO | live, undated | FT | "commensurate with qualifications and experience" | President; operationally CFO/Director of Business Operations | not explicit (recruits instructors, assistants, interns) | https://app.joinhandshake.com/jobs/6379061/share_preview | 01 02 03 04 07 08 09 12 13 14 15 16 18 | none | annual operating budget, policies and procedures, school field-trip schedule |
| P14 | Ceramics Studio Manager | Kirkland Arts Center | Kirkland WA | US | NPO | 2023-01-30 | FT, 40 h, exempt | "$54,000 to $59,000 DOE" | Education Director | 2 technicians, work/study students, volunteers | https://depts.washington.edu/advis104/2023/01/30/kirkland-art-center-seeks-ceramics-studio-manager/ | 01 02 03 05 07 08 09 12 13 15 20 | none | firing and kiln schedules, open-studio schedule, cleaning schedules, equipment assessments, inventory, glaze tests, budget input |
| P15 | Studio Manager | The Clay Corner (Gallery and Firing Center) | Seattle WA | US | COMM | 2021-08-05 PRE | ~20 h (3 shifts) | not stated | not stated | not stated | https://depts.washington.edu/advis104/2021/08/05/the-clay-corner-seeks-studio-manager/ | 01 10 11 14 18 | none | gallery events |
| P16 | Studio Coordinator | Pottery Northwest | Seattle WA | US | NPO | 2022-11-02 PRE | FT, 40 h, exempt | "$47,000-$50,000 DOE" | Operations Director | volunteers | https://depts.washington.edu/advis104/2022/11/02/pottery-northwest-seeks-studio-coordinator/ | 05 11 12 14 17 18 (07 08 10 summary-only) | Google Suite, Monday.com | annual work plan with metrics, event registration |
| P17 | Ceramics Studio Manager | The Canvas | Juneau AK | US | NPO | 2016-09-01 PRE | not stated | not stated | not stated | members, volunteers | https://depts.washington.edu/advis104/2016/09/01/ceramics-studio-manager-juneau-ak/ | 05 07 09 12 13 14 18 | none | supply orders (with Artistic Coordinator), exhibit selections |
| P18 | Studio Manager | Just Kiln' Time "Paint Your Own Pottery" | Menomonee Falls WI | US | PYOP | undated; Jooble card "23 days ago" (~Sep 2026) | FT, 4–8 h shifts | not stated | not stated | staff (hiring, schedules) | https://justkilntimepottery.betterteam.com/studio-manager-1 | 05 06 09 10 11 12 13 14 16 | none | work schedules, daily social post, party bookings |
| P19 | MPA Clay Ceramics Studio Manager (earlier title "Part-Time Ceramics Studio Manager") | McLean Project for the Arts | McLean VA | US | NPO | latest live, undated; earlier version 2026-03-31 | PT, ~20 h (earlier 15–20 h) | "$27,500 - $30,000 a year" (earlier "$22-26/hour") | Artistic Director | not stated | https://www.simplyhired.com/job/HPx02-UVXcGpvrFe-kqcFDbEL5UDJ2YrpvsF792wFe7FqHyJl9BBOg (earlier https://www.eastcityart.com/?p=106321) | 01 02 03 04 05 06 07 08 11 13 16 22 | Outlook, Google Drive/Docs, email | firing logs, inventory, purchasing, open-studio sign-ups, shelf assignments, shared studio calendar |
| P20 | Assistant Studio Manager | Marcello Andres Ceramics | Dallas TX | US | PROD | LinkedIn "1 week ago" (~Oct 2026) | FT, 40 h, M–F | "Competitive pay based on experience" | studio leadership | production staff (quarterly reviews) | https://www.linkedin.com/jobs/view/assistant-studio-manager-at-marcello-andres-ceramics-4476828332 | 01 02 03 04 11 12 13 14 20 21 | none (prefers "production or inventory tracking systems") | production-tracking documents, same-day logs (pieces poured, glazed, yield loss), daily work assignments, mould inventory, quarterly reviews |
| P21 | Production Manager | Farmhouse Pottery | Woodstock VT | US | PROD | 2024-03-31 | not stated | not stated | not stated | studio supervisor, artisans | https://www.ceramicsnow.org/jobs/production-manager-at-farmhouse-pottery-woodstock-vermont/ (alt ?p=31903) | 02 05 12 13 15 21 | NetSuite (ERP), MS Office, Google Sheets | weekly production plan and forecast, weekly work orders, wheel assignments, days-off schedule, WIP/finished-goods tracking |
| P22 | Ceramics Studio Coordinator | Sonoma Community Center | Sonoma CA | US | NPO | 2022-03-29 PRE | 20 h, non-exempt | "$20-25 per hour" | Director of Ceramics | artists in residence, interns, monitors | https://www.ceramicsnow.org/jobs/ceramics-studio-coordinator-at-sonoma-ceramics/ (alt ?p=18344) | 01 02 03 04 05 07 08 12 13 15 16 18 19 | none | kiln-firing schedule, inventory, instructor payouts (assist), exhibitions |
| P23 | Director of Ceramics | Truro Center for the Arts at Castle Hill | Truro MA | US | NPO | deadline 2021-06-18 PRE | FT, year-round | "$45,000 - $55,000 + benefits" | works with Executive Artistic Director (implied) | interns, work study | https://www.castlehill.org/director-of-ceramics | 01 02 03 04 05 08 09 12 13 14 | none | seasonal workshop schedule, auction ware, "100 mugs for the Winter Party" |
| P24 | Ceramics Instructional Operations Coordinator | Harvard University, OFA Ceramics Program | Cambridge MA | US | UNIV | 2024-04-22 | n/s (flexible hours, weekends) | not stated | Director, OFA Ceramics Program | 9 direct reports, 12 interns | https://www.ceramicsnow.org/jobs/ceramics-instructional-operations-coordinator-at-the-ceramics-program-office-for-the-arts-at-harvard/ | 01 02 03 04 05 07 09 11 12 13 14 15 18 22 | Outlook, Word, Excel, FileMaker Pro, HyperGlaze, Rhino, Slic3r; online sign-up sites | kiln/spray-booth sign-up sites, certification tracking, MSDS and SOP sheets, budget, material-use tracking, monthly instructional payroll |
| P25 | Residency Coordinator | Medalta | Medicine Hat AB | CA | NPO | 2023-05-17 | FT, 32 h (4 days) | "$42,000 – $45,000 per year" (CAD) | Executive Director | studio technician, class instructors, volunteers | https://www.ceramicsnow.org/jobs/residency-coordinator-at-medalta/ | 05 08 11 12 14 15 16 17 18 19 22 | Adobe Creative Suite, MS Office, Constant Contact, PayPal | purchase orders, invoices (kiln rentals, decals, materials), grant applications and reports, budgets, kiln-rental and lodging bookings |
| P26 | Studio Coordinator of Ceramics | Anderson Ranch Arts Center | Snowmass Village CO | US | NPO | 2022-06-06 PRE | FT, exempt | "Base Salary: $45,000 – $47,000" | VP, Artistic Affairs | volunteers, TAs, interns | https://www.ceramicsnow.org/jobs/studio-coordinator-of-ceramics-at-anderson-ranch-arts-center/ | 02 05 08 09 12 13 14 15 16 17 19 20 | none ("standard software") | documented inspections, purchase orders, inventory, budget requests, workshop evaluations, reports |
| P27 | Studio Operations Manager – 3D | California College of the Arts | San Francisco CA | US | UNIV | 2022-06-17 PRE | FT, 37.5 h | "$70,551 – $71,962 annually" | Associate Director of Studio Operations | 3 FT staff (incl. Ceramics Studio Manager), 2 GSAs, up to 20 work-study | https://www.ceramicsnow.org/jobs/studio-operations-manager-3d-at-california-college-of-the-arts/ | 07 09 12 13 15 | G Suite, Excel, Adobe Illustrator, CAD | safety and access policies, orientation manuals, budgets, hazardous-waste SOPs |
| P28 | Operations Coordinator | Pottery Northwest | Seattle WA | US | NPO | 2020-12-01 PRE | FT, 40 h, non-exempt | "$38,000 – $42,000" | Executive Director | not stated | https://depts.washington.edu/advis104/2020/12/01/pottery-northwest-seeks-operations-coordinator/ | 05 07 08 10 11 15 16 17 18 22 | QuickBooks, PayPal, MS Office, unnamed scheduling software, website online shop | PayPal batch invoices, deposits, month- and year-end reconciliations, forecasts, payroll, board materials, class and studio-access rosters |
| P29 | Retail/Business Manager | thrown elements pottery | Arlington Heights IL | US | PYOP (+classes) | SimplyHired "8 days ago" (~Oct 2026) | FT | "$49,040 - $59,500 a year" | founder (implied) | staff | https://www.simplyhired.com/job/GkiBqsqPNEfmy49EYdNB8MYHZJfPXYdR9kj83khHE3iqcXbp457qYw | 05 10 11 12 15 17 | none | schedules, budgets, sales goals, labour/payroll targets, "basic business reports" |
| P30 | Back Room Manager - Art Studio (HYBRID-TECH) | MADE: my art + design experience | Mamaroneck NY | US | PYOP | live, undated | PT, 20–30 h | "$19.50–$23/hr" | not stated | support staff (as lead) | https://www.simplyhired.com/job/DSIrNs5btFfAYqBAQJ5rCzxK0C7wL2jv2Br61BEBJQ89M_J1WzuQLA | 01 02 03 06 10 12 | none | piece tracking to pickup date, pickup bags, re-fires |
| P31 | Management Trainee ("Studio Manager – Creative Arts Studio") | Amazing Glaze | Bel Air MD | US | PYOP | SimplyHired "7 days ago" (~Oct 2026) | FT | "$16.50 - $20.00 an hour" + tips | not stated | daily studio staff | https://www.simplyhired.com/job/akSjR0l-iHVai6QMXvtXmuTjgz3NioGu23iXcpuv0Fs65D5lH6YuPg | 01 05 09 10 12 13 14 | none | project firing schedules, material inventory, orders |
| P32 | Recreation Supervisor (Jaycee Park Center for Arts & Crafts) | City of Greenville | Greenville NC | US | MUNI | live, undated | FT | "$57,616.00 - $89,273.60 a year" (hiring range "$57,616.00 to $61,500.00") | not stated | Recreation Assistant (Art Assistant & Pottery Coordinator), Staff Support Specialist I, PT attendants and instructors | https://www.simplyhired.com/job/bZaDPb5UwhjqYXMqFQI2rq8OMQzRyGduaZg2GlJSyKER3gsiZf5s7Q | 01 02 05 07 08 09 12 13 14 15 16 17 22 | Constant Contact | reservation and attendance records and reports, time sheets, accident reports, budget and revenue tracking, requisitions |
| P33 | Ceramic Studio Coordinator (HYBRID-TECH) | Newport Art Museum | Newport RI | US | NPO | SimplyHired "10 days ago" (~Oct 2026) | PT, 21 h | "$20–$25 per hour" | Studio Manager | none | https://www.simplyhired.com/job/SrjJx6A_bgH1TN3-2EM4ZLsiHFqoNq3rWugmIBQqqT1ofbb7zVcj-Q | 01 02 03 04 07 08 10 13 | none | glaze labels and storage |
| P34 | Art Studio Manager (part-time) (HYBRID-TECH) | Framingham State University | Framingham MA | US | UNIV | 2026-08-26 | PT, ≤18 h | "$20-22/hour" | Department Chair | "N/A" | https://www.simplyhired.com/job/mSgnAeGV7P6l7owyTtoB5KW_Es4bpVLrPqXkhJHA0KO5lNrMxhARrg | 02 05 13 20 | none | supply and equipment inventory |
| P35 | Ceramics Program Coordinator | ArtsQuest | Bethlehem PA | US | NPO | live, undated | FT, M–F 10–6 | "From $20 an hour" | works with Manager of Visual Arts (implied) | not stated | https://www.simplyhired.com/job/_qmIWD9OLeJMZIxrPbD2X3-Il-HNdlJZsteak7Sgp0DNLoRWn8Kd8A | 01 02 05 07 08 11 13 15 17 18 22 | Outlook, MS Office, OneDrive | annual programme and classroom budget, P&Ls, revenue tracking, inventory, shared classroom calendar |
| P36 | Ceramics Studio Coordinator (HYBRID-TECH) | Sebastopol Center for the Arts | Sebastopol CA | US | NPO | 2026-10-13 | PT, 12–16 h | "$25 hr" | Ceramics Program Manager | none (orients volunteers) | https://www.craigslist.org/view/d/sebastopol-ceramics-studio-coordinator/hdUMtisz5EcwrxTuBg68AJ | 01 04 07 10 12 13 | none | – |
| P37 | Ceramics Studio Manager | GoggleWorks Center for the Arts | Reading PA | US | NPO | live, undated | FT, exempt | not shown | Artistic Director | not shown | https://goggleworks.org/ceramics-studio-manager | 02 05 06 07 08 11 12 13 | Google Drive/Docs, MS Office (Excel, Word), WordPress | annual calendar, supply-order approvals, student work moved to pickup |
| P38 | Ceramics Studio Manager (seasonal) | Truro Center for the Arts at Castle Hill | Truro MA | US | NPO | deadline 2024-03-31 | seasonal, 40 h, May–Sep | "$16,000 - $17,000" | Ceramic Director | not stated | https://www.castlehill.org/ceramics-studio-manager | 01 02 04 05 08 13 14 | none | end-of-season inventory |
| P39 | Ceramics Program Coordinator | Main Line Art Center | not stated (posting lists Delaware and Montgomery Counties, PA) | US | NPO | 2026-04-27 | PT, 30 h | "$22 per hour" | Co-Executive Directors | 2–3 PT ceramics technicians, volunteer monitors | https://portal.philaculture.org/node/74773 | 01 02 03 05 07 08 12 13 14 16 18 22 | MS Office | Open Studio calendar, firing schedule, cleaning schedule, volunteer-monitor schedule, technicians' annual reviews, semester newsletter, orders |

## Duty counts (n = 39), with one verbatim example per code

Each "recent" figure counts the 28 postings dated 2023–2026 or live when accessed. The example quotes are verbatim as WebFetch returned them, with typographic quotes normalised.

| Code | Duty | All (n=39) | Recent (n=28) | Verbatim example |
|---|---|---|---|---|
| D13 | Cleaning, facility, safety | 32 (82%) | 25 | P36 "Deep-clean studio surfaces, floors, and equipment using wet-cleaning methods to minimize clay and silica dust." |
| D05 | Inventory, ordering, purchasing, receiving | 30 (77%) | 22 | P26 "Purchase and organize supplies, maintain inventory, receive shipments and manage purchase orders." |
| D12 | Staff / volunteer / student-worker management | 28 (72%) | 20 | P12 "Reviews Applications, Schedules Interviews, Recruits Staff, Trains Staff" |
| D02 | Kiln and equipment maintenance and repair | 27 (69%) | 22 | P39 "Regular kiln and furniture maintenance like changing elements and grinding shelves." |
| D01 | Kiln operations | 24 (62%) | 20 | P39 "Kilns: Loading and firing the electric kilns with bisque and Cone 6 work daily." |
| D08 | Classes and programmes | 23 (59%) | 17 | P39 "Creating and communicating the Open Studio calendar and the firing schedule to our community at the start of each semester." |
| D07 | Member management, broad (onboarding, orientations, rules, access) | 21 (54%) | 15 | P04 "Coordinate onboarding of new members, instructors, and students as needed, including but not limited to:" "facilitating studio tours, designating storage space, adding to studio email list, sharing studio policies and rules," |
| D07a | of which "members"/"membership" named explicitly | 9 (23%) | 7 | P32 "Collects fees, payments, and memberships." |
| D14 | Events, parties, rentals | 20 (51%) | 14 | P31 "Host & Instruct: Facilitate engaging group events, including children's birthday parties," |
| D15 | Finance and bookkeeping (any) | 17 (44%) | 11 | P28 "Tracking transactions in Quickbooks" |
| D16 | Marketing and communications | 17 (44%) | 12 | P01 "Update studio social media and contribute content and news on studio channels (Facebook, Instagram, Linktree, Mailchimp)" |
| D03 | Glaze mixing, testing, upkeep | 15 (38%) | 12 | P24 "Supervises the mixing and testing of over 100 glazes and the inventory of all ceramic materials." |
| D11 | Retail and point of sale | 14 (36%) | 11 | P18 "Answer phones, book parties, ring up sales, wrap up masterpieces, process pottery, unpack stock, and re-stock shelves." |
| D04 | Clay, reclaim, materials | 13 (33%) | 10 | P09 "reclaim/pug clay as needed" |
| D09 | Teaching (10 teach classes or workshops; 3 only demos or equipment instruction) | 13 (33%) | 8 | P06 "Teach workshops/series/clay plays." |
| D10 | Front desk and customer service | 13 (33%) | 10 | P01 "Deliver exceptional customer service in person, over the phone, and via email" |
| D18 | Gallery, exhibitions, consignment | 11 (28%) | 6 | P35 "Act as a support for selecting ceramic artists for 1st floor retail area artist consignment program." |
| D17 | Reporting (metrics, board, grant, business) | 9 (23%) | 5 | P16 "Produce an annual work plan with metrics tied to overall organizational goals." |
| D22 | Booking shared resources (added) | 8 (21%) | 7 | P24 "Maintains online sign-up sites for electric kilns, gas kilns, and spray booths" |
| D06 | Members' work in progress (pieces through firing, shelves, pickup) | 7 (18%) | 6 | P30 "Track every customer-painted piece so nothing misses its pick-up date" |
| D20 | Equipment/asset tracking (added) | 6 (15%) | 5 | P12 "Adheres to inventory and FF&E (Furniture, Fixtures and Equipment) count schedules" |
| D19 | Artist residency (added) | 5 (13%) | 3 | P01 "Oversee all aspects of the Artist-in-Residence program, including the artist selection process," |
| D21 | Production order / WIP tracking (added) | 2 (5%) | 2 | P21 "Properly track the production workflow from assigned work orders to WIP to finished goods," "both physically and digitally in our inventory management system" |

Finance split: only 6 of 39 postings carry bookkeeping-level work: invoicing, deposits, AP/AR, reconciliation, payroll, or posting income and expenses (P07, P22, P25, P26, P28, P29). 13 of 39 carry budget work (P01 P05 P10 P11 P13 P14 P21 P24 P25 P26 P27 P32 P35).

### Duty counts by segment

Cells give count/segment size.

| Code | NPO /22 | UNIV /6 | PYOP /5 | COMM /3 | PROD /2 | MUNI /1 |
|---|---|---|---|---|---|---|
| D01 kiln ops | 16 | 2 | 2 | 2 | 1 | 1 |
| D02 maintenance | 17 | 5 | 2 | 0 | 2 | 1 |
| D03 glaze | 12 | 1 | 1 | 0 | 1 | 0 |
| D04 clay/reclaim | 9 | 3 | 0 | 0 | 1 | 0 |
| D05 inventory/purchasing | 18 | 5 | 4 | 1 | 1 | 1 |
| D06 WIP/pieces | 4 | 0 | 2 | 1 | 0 | 0 |
| D07 member mgmt (broad) | 15 | 4 | 0 | 1 | 0 | 1 |
| D08 classes/programmes | 19 | 2 | 0 | 1 | 0 | 1 |
| D09 teaching | 6 | 3 | 2 | 1 | 0 | 1 |
| D10 front desk | 5 | 0 | 5 | 3 | 0 | 0 |
| D11 retail/POS | 8 | 1 | 3 | 1 | 1 | 0 |
| D12 staff/volunteers | 15 | 5 | 5 | 0 | 2 | 1 |
| D13 cleaning/safety | 19 | 6 | 3 | 1 | 2 | 1 |
| D14 events/parties/rentals | 11 | 1 | 3 | 3 | 1 | 1 |
| D15 finance | 8 | 5 | 1 | 1 | 1 | 1 |
| D16 marketing/comms | 12 | 1 | 2 | 1 | 0 | 1 |
| D17 reporting | 7 | 0 | 1 | 0 | 0 | 1 |
| D18 gallery/consignment | 9 | 1 | 0 | 1 | 0 | 0 |
| D19 residency | 5 | 0 | 0 | 0 | 0 | 0 |
| D20 asset tracking | 2 | 2 | 1 | 0 | 1 | 0 |
| D21 production tracking | 0 | 0 | 0 | 0 | 2 | 0 |
| D22 booking | 5 | 2 | 0 | 0 | 0 | 1 |

### Duty counts by country

US (n=37): D13 32, D05 29, D02 27, D12 27, D01 24, D08 22, D07 21, D14 18, D03 15, D15 15, D16 16, D04 13, D09 13, D11 13, D10 12, D18 10, D17 8, D06 7, D22 7, D20 6, D19 4, D21 2.
UK (n=1, P07, 2022): D10, D14, D15.
Canada (n=1, P25, 2023): D05, D08, D11, D12, D14, D15, D16, D17, D18, D19, D22.
Australia and New Zealand: 0 postings, so no data.

## Outputs and deliverables named (n = 39)

| Output | Postings | IDs |
|---|---|---|
| Inventory records or counts ("inventory" named) | 19 | P01 P06 P09 P10 P12 P14 P17 P19 P20 P21 P22 P23 P24 P26 P29 P31 P34 P35 P38 |
| Budgets (annual, labour, programme, P&L) | 13 | P01 P05 P10 P11 P13 P14 P21 P24 P25 P26 P27 P32 P35 |
| Mixed glazes / glaze programme | 11 | P01 P02 P03 P04 P14 P22 P23 P24 P30 P33 P39 |
| Firing schedules or kiln/firing logs | 11 (logs specifically: 3, P01 P02 P19) | P01 P02 P03 P09 P14 P19 P20 P22 P24 P31 P39 |
| Class / open-studio / annual calendars | 10 | P01 P03 P04 P10 P14 P19 P23 P35 P37 P39 |
| Newsletters, session emails, social posts, email campaigns | 10 | P01 P06 P09 P12 P13 P18 P22 P25 P32 P39 |
| Reclaimed clay | 9 | P02 P05 P09 P13 P19 P22 P33 P36 P38 |
| Policies, manuals, handbooks, SOPs | 9 | P04 P10 P13 P14 P24 P27 P28 P35 P37 |
| Staff / volunteer / student-worker schedules | 8 | P03 P10 P11 P12 P18 P21 P29 P39 |
| Reports (metrics, grant, board, business, accident) | 7 | P16 P25 P26 P28 P29 P32 P35 |
| Member/student records (rosters, attendance, access, certifications, sign-ups) | 7 | P01 P09 P10 P19 P24 P28 P32 |
| Safety documents (SDS/MSDS, SOP sheets, safety audit, accident reports, glaze labels) | 6 | P03 P10 P12 P24 P27 P32 |
| Timesheets / payroll | 6 | P01 P10 P24 P28 P29 P32 |
| Purchase orders, requisitions, orders submitted for approval | 4 | P02 P25 P26 P32 |
| Invoices | 3 | P07 P25 P28 |
| Performance reviews (quarterly or annual) | 3 | P12 P20 P39 |
| Equipment inspection/assessment records, FF&E counts | 3 | P12 P14 P26 |
| Production tracking documents, work orders | 2 | P20 P21 |

## Software named (n = 39)

| Software | Postings | IDs |
|---|---|---|
| Any specific product named | 16 | P01 P02 P04 P09 P10 P16 P19 P21 P24 P25 P27 P28 P32 P35 P37 P39 |
| No product named | 23 | – |
| Microsoft Office / Outlook / Excel / OneDrive | 12 | P01 P04 P09 P19 P21 P24 P25 P27 P28 P35 P37 P39 |
| Google Workspace (Gmail, Calendar, Drive, Docs, Sheets, "Suite") | 9 | P01 P04 P09 P10 P16 P19 P21 P27 P37 |
| Adobe | 3 | P10 P25 P27 |
| Email marketing (Mailchimp 1; Constant Contact 2) | 3 | P01; P25 P32 |
| PayPal | 2 | P25 P28 |
| QuickBooks | 1 (2020) | P28 |
| Unnamed POS ("POS program") | 1 | P01 |
| Unnamed scheduling / sign-in / sign-up software | 3 | P11 P24 P28 |
| WebCheckout (equipment checkout/booking) | 1 | P10 |
| Airtable / Monday.com / FileMaker Pro | 1 each | P10 / P16 / P24 |
| NetSuite ERP (plus an unnamed "inventory management system") | 1 | P21 |
| Pottery-specific: KilnFire (studio app) 1; HyperGlaze (glaze software) 1 | 2 | P02; P24 |
| Social platforms named (Facebook, Instagram, Linktree) | 1 | P01 |
| WordPress | 1 | P37 |
| Rhino, Slic3r, CAD | 2 | P24, P27 |
| Square, Stripe, Shopify, Xero, Mindbody or other class-booking tool, kiln-controller software | **0 each** | – |

Three postings mention inventory software or systems: P10 ("digital systems for financial, scheduling, and inventory management"), P20 (prefers "production or inventory tracking systems") and P21 (NetSuite and an "inventory management system"). The kiln controller comes up once, only as hardware: P35 "electric Skutt & Cone Art Kilns, computerized".

## Hours, pay and benefits

- **FT/PT (n=39):** FT 21, PT 14, seasonal 1 (P38), not stated 3 (P17 P21 P24).
- **Part-time hours (n=13 with a number):** median 20 h/week, range 12–30. Values: P36 12–16, P02 ~16, P34 ≤18, P09 20, P11 20, P15 ~20, P19 ~20, P22 20, P33 21, P30 20–30, P03 ~30, P06 30, P39 30.
- **Full-time hours (n=9 with a number):** median 40, range 32–40 (P25 32, P10 and P27 37.5, the rest 40).
- **Pay stated:** 26 of 39, not stated 13.
- **US full-time salaried (n=10):** median of midpoints $52,135, range of stated bounds $38,000–$71,962. Values: P04 $40,000; P28 $38,000–42,000 (2020); P26 $45,000–47,000 base (2022); P16 $47,000–50,000 (2022); P23 $45,000–55,000 (2021); P29 $49,040–59,500; P14 $54,000–59,000; P32 hiring range $57,616–61,500; P01 $62,000; P27 $70,551–71,962 (2022, a multi-studio manager). **2023 onward (n=5: P01 P04 P14 P29 P32):** median $56,500, range $40,000–$62,000.
- **US hourly (n=11):** median of midpoints $22, range $16.50–$32. Values: P31 $16.50–20 plus tips, P35 from $20, P34 $20–22, P30 $19.50–23, P09 $22 (2022), P39 $22, P22 $20–25 (2022), P33 $20–25, P06 $23+, P36 $25, P10 $32. **2023 onward (n=9):** median $22, range $16.50–$32.
- **US part-time or seasonal salaried:** P03 $30,000–32,000 for ~30 h; P19 $27,500–30,000 for ~20 h; P38 $16,000–17,000 for a 40 h May–September season.
- **Outside the US:** P07 £31,000 FT (2022). P25 CAD 42,000–45,000 FT at 32 h (2023).
- **Health insurance named:** 15 of 39 (P01 P04 P10 P11 P14 P16 P21 P23 P24 P26 P27 P28 P31 P32 P39). P11 says only "full benefit coverage". Partial cover is explicit in P01 and P23 ("50%").
- **Studio access, free firing or free classes as a benefit:** 10 of 39 (P04 P06 P14 P16 P19 P21 P22 P26 P28 P39). Housing appears once (P26: "Employer Housing Provided – On campus studio apartment").

## Reporting lines and supervision

**Reports to (n=39):**
- Top of the organisation, 10. Executive director or co-executive directors: P01 P25 P28 P39. Works directly with the ED: P09. President, operationally the CFO: P13. Implied, 4: museum director P02, Executive Artistic Director P23, Studio Owner P12, founder P29.
- A programme, education, artistic, operations or ceramics director, or a studio director or VP, 11: P03 P07 P08 P14 P16 P19 P22 P24 P26 P37 P38.
- Another manager or academic administrator, 7: P11 (dean via College Program Administrator), P20 ("studio leadership"), P27 (Associate Director of Studio Operations), P33 (Studio Manager), P34 (department chair), P35 (Manager of Visual Arts, implied), P36 (Ceramics Program Manager).
- Not stated, 11: P04 P05 P06 P10 P15 P17 P18 P21 P30 P31 P32.
- Reports to a board: 0. Prepares board materials: 1 (P28).

**Supervises (n=39):** 26 state some supervision and 13 state none or don't say (P02 P04 P06 P07 P13 P15 P19 P28 P33 P34 P35 P37 P38). Who is supervised (a posting can appear in several groups):
- Technicians: 5 (P03 P14 P25 P27 P39). Stated headcounts: P14 "studio technician team (2)", P39 "2-3 part-time Ceramics Technicians".
- Student workers or work-study students: 6 (P05 P10 P11 P14 P23 P27). The largest is P10: "a team of 10-12 student workers".
- Volunteers, including open-studio monitors: 7 (P09 P14 P16 P17 P25 P26 P39). P36 only orients volunteers.
- Interns, residents or TAs: 5 (P09 P22 P23 P24 P26).
- Instructors or instructional staff: 3 (P24 "9 direct reports (monthly instructional payroll)", P25, P32). P08 trains instructors.
- Retail, front-of-house or production staff: 8 (P12 P18 P20 P21 P29 P30 P31 P32).
- Volunteer, student or intern labour of any kind (volunteers, work-study, student workers, interns, residents): 14 of 39 (P05 P09 P10 P11 P14 P16 P17 P22 P23 P24 P25 P26 P27 P39).

## Differences by segment

- **PYOP (n=5: P12 P18 P29 P30 P31). A retail-floor and shift-management job.** Front desk 5/5, staff management 5/5, inventory and ordering 4/5, retail 3/5, events and parties 3/5, kiln operations 2/5. Member management and class programmes are each 0/5. **Software named: 0/5.** PYOP is the only segment where the owner is the boss (P12 implied owner, P29 implied founder), and the only one that talks about pricing: P12 "Manages and maintains adequate stock levels and inventory SKUs pricing". The handover is explicit in P29: "Enjoy improving systems and helping a small business become less owner-dependent".
- **Nonprofit art centres (n=22). A programme-plus-studio job.** Classes and programmes 19/22, cleaning and safety 19/22, inventory 18/22, maintenance 17/22, kiln operations 16/22, member management 15/22, staff and volunteers 15/22, marketing 12/22, events 11/22, gallery and consignment 9/22, finance 8/22, retail 8/22, reporting 7/22, residency 5/22, booking 5/22. Typical events are fundraisers: Empty Bowls (P17, P39), the Chili Bowl (P13), an auction (P23), and seconds or emerging-artist sales (P03, P39).
- **University (n=6). Compliance and student labour.** Cleaning and safety 6/6, with explicit hazmat or SDS duties in P10, P24 and P27. Student-worker management 5/6, budgets 5/6, maintenance 5/6, access and certification tracking 4/6. Front desk 0/6 and kiln operations 2/6. Software use is the heaviest: P10 and P24 name 4 or more products.
- **Community studios (n=3).** Front desk 3/3 and events 3/3. Weak evidence: two of the three are PRE-2023.
- **Production potteries (n=2). A manufacturing job.** Both track work orders through WIP (D21), manage production staff, run quality control and plan weekly. P21 uses NetSuite. P20: "Own and maintain the studio's core production-tracking documents, keeping them current as the single source of truth". Neither mentions members or classes.
- **Municipal (n=1, P32).** A recreation supervisor runs a multi-craft centre with a ceramics facility. Facility scheduling, fee and membership collection, Constant Contact, accident reports.

### What the owner or director keeps, and what is handed over

| Posting | Kept by owner, director or another role | Handed to the manager |
|---|---|---|
| P02 | "Submit supply orders to the FPM Director for approval." ; "Report major repair needs ... to the FPM Director for service coordination." | daily kiln work, logs, cleaning, recycling, glazes |
| P03 | ED approval of repairs ("approval from the ED for the repairs"); Program Director schedules classes (manager is "Assisting") | purchasing "within budget limits", kiln-tech supervision |
| P07 | "You are supported by a financial account manager" | client interface, scheduling, invoicing |
| P11 | budget "with oversight from the College Program Administrator" | studio budget, student staff, supplies |
| P12 | "Collaborates effectively with the Studio Owner in regards to maintenance with kilns and in-studio repairs" ; marketing "as directed by the Studio Owner" ; donations "as approved" | hiring, scheduling, reviews, inventory and FF&E counts, safety audit |
| P14 | "Assist Education Coordinator with purchasing and coordinating purchasing." | technicians, firing/glaze/cleaning schedules, inventory |
| P25 | invoicing and POs "in accordance with FOMS financial policies"; budgets "with the Executive Director" | residencies, kiln rentals, invoicing, POs |
| P29 | founder keeps "the ideas, relationships, and long-term vision" | "turn priorities into consistent execution"; budgets, schedules, staff, cash, inventory, reports |
| P35 | budget "Working closely with Manager of Visual Arts" | kilns, inventory, membership selection, P&Ls |
| P37 | – | "Supervise and approve ordering of all supplies and materials that are required for classes." (the manager is the approver) |
| P38 | ordering "with Program Manager"; firing "under the direction of the Ceramic Director" | studio prep, reclaim, end-of-season inventory |

In 5 of 39 postings, purchasing or repairs need someone else's approval or partnership (P02 P03 P12 P14 P38). In 1 (P37), the manager is the approver.

## Stated frequencies and durations

- **Kiln firing cadence is stated in only 1 of 39:** P39 "Loading and firing the electric kilns with bisque and Cone 6 work daily." ; "Loading and firing the Cone 10 gas kiln bi weekly." Elsewhere it is "as needed" (P04 "fire kilns as needed"; P09 "fire kilns as needed"; P33 "Load and unload kilns as needed") or tied to the programme: P35 "as needed during each semester for student work & member work"; P23 "(Wednesday throwing/wood firings)".
- **Open studio:** P35 "Facilitate Ceramic open studio 2 times per week" ; P13 "Pottery Circle meets most Mondays 10:00 am -1:00 pm."
- **Programme rhythm:** P01 "10-12 hand-building and wheel-throwing classes (6-week sessions), quarterly" and "2 introductory clay classes, monthly" ; P09 orientations for each of 6 sessions ("fall 1, fall 2, winter, spring 1, spring 2, summer") ; P39 calendar and firing schedule "at the start of each semester" ; P24 "over 3 semesters annually", "Gallery 224 installations (6-8 annually)", "two annual large-scale Show and Sales".
- **Cleaning:** P35 "daily cleaning of all spaces Monday-Friday" ; P39 "Daily and weekly cleaning" ; P36 cleaning "approx. 70% of role", mopping "each shift" ; P09 "twice a year major studio clean up" ; P22 "bi-annual studio cleaning" ; P03 cleaner "twice per week" and an "annual summer studio cleanup".
- **Admin cadence:** P18 "Daily social media post." ; P20 "Log same-day entries", daily and weekly output targets, quarterly reviews ; P21 weekly planning, work orders and production meetings ; P12 "quarterly Safety Audit" ; P16 office supplies "quarterly as needed", "annual work plan" ; P28 "End of month and year reconciliations" ; P24 "monthly instructional payroll" ; P10 "semiannual waste pickup" ; P39 annual technician reviews, Empty Bowls "annually in November", sale "annually in April".
- **Time splits stated by the employer:** P16 Administrative Assistance 70% / Studio Event Support 15% / Gallery Attendant 10% / Shared Leadership 5%. P10 Studio Operations 40% / Management & Administration 30% / Academic 25% / Non-Essential 5%. P24 Ceramics Facilities 25%, Kiln Management 5%, Health & Safety 15%, Financial & Procurement 10%, Staff Supervision 15%, Academic Collaborations 15%, Gallery 5%, Show & Sales 5%, Tenant duties 5%. P27 Management & Administration 40% / Studio Operations 30% / Academic 25% / Non-Essential 5%.

## Appendix A: per-posting evidence (verbatim, as returned by WebFetch; appended as I go)

Entries are in discovery order, and later updates to a posting appear as separate "update" or "addendum" entries.

Format: header line = ID | title | employer | city, country | segment | date. Then fields. Quotes are verbatim from the WebFetch return of the page; "..." marks a cut. Accessed 2026-10-20 unless noted.

### P01 | Ceramics Studio Manager | American Museum of Ceramic Art (AMOCA) | Pomona CA, US | NPO (museum studio) | posted 2025-04-28
- URL: https://www.ceramicsnow.org/jobs/ceramics-studio-manager-at-the-american-museum-of-ceramic-art/ (CeramicsNow repost of employer listing)
- Hours: "40 hours per week with benefits" ; "Non-Exempt Employee" ; "Responsibilities will require evening and weekend hours"
- Pay: "$62,000 with paid holidays, sick leave, and vacation days." Benefits: "50% of the health insurance premium is covered." "An annual stipend is provided to attend NCECA."
- Reports to: "Reports to the Executive Director". Supervises: not stated (but "Recruit and train staff"; kiln work "with the Studio Technician").
- Duties (verbatim): "The Studio Manager is responsible for the overall operation, maintenance, and safety of a 12,000 square-foot ceramic studio," ; "Deliver exceptional customer service in person, over the phone, and via email" ; "Oversee all aspects of the Artist-in-Residence program" ; "Manage inventory needs to ensure seamless studio operations" ; "Coordinate scheduling, loading, and unloading of gas and electric kilns; production of studio glazes;" "and maintenance of kilns, tools, and equipment with the Studio Technician" ; "Oversee studio cleanliness and safety standards" ; "Prepare materials, set up spaces, and coordinate with instructors and volunteers" ; "Monitor, prioritize, and implement studio repairs and maintenance" ; "Coordinate scheduling for K-12 programs with the Education Manager" ; "Maintain administrative systems: calendar, POS program, timesheets, contracts, class registration," "attendance tracking, security, and kiln logs" ; "Develop and manage adult ceramic programs" ; "Recruit and train staff" ; "Represent the studio at community events" ; "Assist with studio budget development, expense tracking, and financial management" ; "Update studio social media and contribute content and news on studio channels (Facebook, Instagram, Linktree, Mailchimp)"
- Software: "proficiency in Microsoft Office, Gmail, and Google Calendar"; "POS program" (unnamed); Mailchimp; Linktree; Facebook; Instagram.
- Outputs: calendar, timesheets, contracts, class registration, attendance tracking, kiln logs, studio glazes, budget/expense tracking, social posts.
- Frequencies: "10-12 hand-building and wheel-throwing classes (6-week sessions), quarterly" ; "2 introductory clay classes, monthly" ; "two 12-month residencies and two 6-month residencies annually"
- Codes: D01 D02 D03 D05 D08 D10 D11 D12 D13 D14 D15 D16 + D19(residency)

### P02 | Community Pottery Studio Coordinator | Folk Pottery Museum of Northeast Georgia | Sautee Nacoochee GA, US | NPO (museum community studio with members) | undated (accessed 2026-10-20)
- URL: https://www.folkpotterymuseum.com/jobs
- Hours: "Approximately 16 hours per week". Pay: not stated. Benefits: not stated.
- Reports to: not stated; implied FPM Director: "Submit supply orders to the FPM Director for approval." ; "Report major repair needs (e.g., kiln elements, relays, electrical components) to the FPM Director for service coordination." Supervises: none stated.
- Duties (verbatim): "Staff the studio during open studio hours to answer questions and support members." ; "Ensure proper and safe use of equipment, tools, and materials (non-instructional role)." ; "Load and unload kilns to maintain efficient firing schedules." ; "Maintain accurate kiln-firing logs and records." ; "Perform waxing and glazing on pottery as needed." ; "Promote and uphold cleanliness standards by ensuring members clean their work areas." ; "Conduct regular deep cleaning, including floors, clay traps, sinks, countertops, shelving," ; "Monitor the condition of kilns, wheels, slab rollers, and other studio equipment." ; "Perform basic maintenance tasks such as shelf scraping and kiln washing, vacuuming kilns, and adjusting wheel foot pedals." ; "Track and plan for supply needs including clay, glazes, kiln wash, wax resist, cones, tools, and brushes." ; "Perform clay recycling and glaze mixing as part of routine studio operations." ; "Communicate with class instructors to ensure the studio is prepared and equipped for classes and workshops." ; "Monitor and communicate with members and students through KilnFire App and Email." ; "Act as studio ambassador during events and promotional opportunities."
- Software: KilnFire app; email.
- Outputs: kiln-firing logs and records; supply orders (for approval); recycled clay; mixed glazes.
- Owner keeps: purchasing approval and major-repair coordination stay with the Director (quotes above).
- Codes (final): D01 D02 D03 D04 D05 D07(rules) D08 D10 D13 D14 D16. D06 is not counted: "waxing and glazing on pottery" is not tracking pieces.

### P03 | Ceramics Studio Manager | Community Arts Center (CAC) | Wallingford PA, US | NPO | posted 2026-07-28
- URL: https://portal.philaculture.org/node/75005 (Philadelphia Cultural Fund/PhilaCulture job portal)
- Hours: "This is a part time salaried position (approximately 30 hours per week)." Pay: "Pay Type: Salary" / "Minimum Salary/Hourly Rate: $30,000.00" / "Maximum Salary/Hourly Rate: $32,000.00". Benefits: not stated.
- Reports to: "The Ceramic Studio Manager reports to the Program Director". Supervises: "Supervising kiln techs, including assigning tasks, scheduling, monitoring progress, and providing feedback to address issues."
- Duties (verbatim): "Assisting the Program Director to schedule classes in the Ceramic Studio and identify potential faculty members." ; "Overseeing the operations of outdoor kilns (wood, soda, raku, gas)," ; "Purchasing supplies for the ceramic program within budget limits." ; "Maintaining ceramic studio equipment including, but not limited to, kilns, wheels and other mechanical systems." ; "aware of needed repairs in the studio, getting estimates for those repairs," ... "approval from the ED for the repairs and scheduling necessary repairs." ; "Coordinating and scheduling firings of all electric kilns, including loading and unloading kilns" ; "Coordinating the mixing of glazes and insuring proper safety labeling of glazes." ; "Scheduling Open Studio Hour. Coordinating plan for locking studio at the end of day." ; "Coordinating the cleaning of the Ceramic Studio" ; "Coordinating and running the Emerging Artist Ceramics Sale." ; "Coordinating schedule of any special programs, volunteer cleanings, and/or tech work during break weeks"
- Software: none named. Outputs: firing schedule for electric kilns; repair estimates; kiln-tech task schedules; glaze safety labels; open-studio schedule; annual sale.
- Owner keeps: ED approves repairs; Program Director schedules classes (manager "Assisting").
- Frequencies: "The CAC studio cleaner cleans the studio floors twice per week" ; "annual summer studio cleanup"
- Codes: D01 D02 D03 D05 D07(open studio scheduling) D08 D12 D13 D14 D18(sale)

### P04 | Studio Manager | Pittsburgh Center for Arts & Media | Pittsburgh PA, US | NPO (multi-media art centre; ceramics emphasis) | posted 2026-03-06
- URL: https://www.aapgh.org/artist-opportunities/2026/3/6/studio-manager-pittsburgh-center-for-arts-amp-media-job-opportunity (Associated Artists of Pittsburgh repost)
- Hours/pay: "This is a full-time position paying $40,000 per year." ; "Position works Monday – Friday, daylight/evening hours, and additional time to complete work and fire kilns as needed." Benefits: "competitive benefits package including health insurance and paid time off." "Studio use and access is also included."
- Reports to: not stated (liaison to Director of Education). Supervises: not stated.
- Ceramics scope: "This position also has a strong emphasis in coordinating/managing a fast paced ceramics studio."
- Duties (verbatim): "Load and fire electric, soda, and gas kilns; perform regular raku firings" ; "ordering clay and other supplies, mixing glazes, and general studio upkeep and cleanup" ; "Serve as the primary point of contact for ceramics members, managing communications about studio closures, announcements, and open studio schedule." ; "Create studio policies and rules for members and students and effectively communicate policies" ; "Coordinate onboarding of new members, instructors, and students as needed, including but not limited to:" / "facilitating studio tours, designating storage space, adding to studio email list, sharing studio policies and rules," ; "Serve as a mentor to annual Ceramics Artist in Residence" ; "Work with the front desk on sales of clay and kiln space for specific member based firings" ; "Make sure all studios are in working order, restock supplies, communicate cleaning and repair needs to facilities and cleaning persons," ; "researching and purchasing new or improved tools/storage/and environmental needs" ; "Create and maintain policies on studio membership in ceramics, printmaking, and media alongside Member and Donor Coordinator," ; "Identify opportunities to work with and support grant writing for studio needs"
- Software: "Proficiency in Microsoft Office, Google drive". Outputs: studio policies and rules; member onboarding (storage assignment, email list); open studio schedule.
- Codes (final): D01 D02(repair needs communicated) D03 D05 D07 D08 D11(sales of clay and kiln space via front desk) D13 D16(member communications) D17(grant support) D19(residency). D04 is not counted, because "ordering clay" is purchasing (D05).

### P05 | Ceramics Studio Manager | Minot State University (Dept of Art) | Minot ND, US | UNIV | posted 2026-08-15
- URL: https://www.ceramicsnow.org/jobs/ceramics-studio-manager-at-minot-state-university/
- Hours: "This is a part-time staff position in Art." Hours not stated. "No overtime." Pay/benefits: not stated.
- Reports to: not stated. Supervises: "Position has the option of hiring student workers to assist in the management of the ceramics studio."
- Duties (verbatim): "Ensure clean and efficient operations, consistent materials and supplies, studio safety," "and maintenance of studio facilities and equipment." ; "This includes kiln firings, clay recycling, and general studio cleaning." ; "Maintain and communicate all safety protocols for the ceramics studio." ; "order raw materials and supplies to support all courses." ; "Provide technical and operational support to Ceramic Program courses and students." ; "Work within a pre-determined operating budget."
- Software: none. Outputs: none named beyond orders.
- Codes: D01 D02 D04 D05 D12(student workers) D13 D15(budget - work within) 

### P06 | Ceramic Studio Manager | Ilwaco Artworks (sister location of Sou'wester Arts non-profit) | Ilwaco WA, US | COMM (nonprofit-affiliated community studio + gallery) | posted 2024-04-03
- URL: https://www.ceramicsnow.org/jobs/ceramic-studio-manager-at-ilwaco-artworks/
- Org: "a community ceramics studio, workshop space and art gallery." ; "Ilwaco Artworks is a sister location for Sou'wester Arts Non-Profit."
- Hours: "30 hrs/week including weekends." Pay: "$23+/hr DOE." Benefits: "Simple IRA" ; "Clearly defined personal studio use" ; "50% off workshops" ; "Staff discounts on lodging, retail & sauna use"
- Reports to / supervises: not stated.
- Duties (verbatim): "Manage the ceramics studio." ; "Teach workshops & Clay Plays." ; "General cleanliness, safety and order of the ceramic studio." ; "Maintaining and ordering supplies for the studio." ; "Developing projects curriculum." ; "Load/unload kiln." ; "Glazing participant works." ; "Shipping visiting participant finished works." ; "Host studio members during self-guided open studio hours." ; "Orientation of new members about the proper use of the studio space and handbuilding techniques." ; "Kiln firings. Preparing, loading & unloading bisque/glazing firings, including glazing as needed." ; "Host private events." ; "Maintaining administrative systems related to firing student/member work, clay inventory, clay play contacts," "workshop attendants, tracking work, shipping, and others as necessary." ; "Managing ceramic workshop series with local and visiting artist teachers." ; "Capturing photos of events and studio for social media."
- Software: none named. Outputs: tracking of student/member work through firing; clay inventory; contact lists; attendance lists; shipped finished work.
- Codes: D01 D05 D06 D07 D08 D09 D10 D13 D14 D16

### P07 | Studio Manager | Studio Pottery London | London, UK | COMM (first coded PROD, revised; see UPDATE below) | posted 2022-01-24, closed 2022-02-11 (PRE-2023)
- URL: https://www.ceramicsnow.org/jobs/studio-manager-at-studio-pottery-london/ (Crafts Council copy https://www.craftscouncil.org.uk/sector-support/opportunities/studio-manager returned 403)
- Pay/hours: "Salary: £31,000 per annum, FTE, 5 days a week". Reports to: "you directly work for Studio Director Lucy Attwood". Supervises: not stated (liaises with "Artistic Director Gregory Tingay and studio technicians").
- Duties: only "it is your responsibility to be the direct interface with the clients." Experience asked: "email account management & technical competence of the relationship management".
- UPDATE from fuller copy https://ceramicsnow.org/?p=16858 (same posting): segment is COMM, not PROD: "The studio offers open access teaching as well as a workshop area for its 100 members." ; "Opened on the 11th September 2019 it is the first such space for pottery enthusiasts" "and beginners alike in central London." Duties/experience (verbatim): "Thorough experience is essential of working in a busy studio administrative management role with scheduling," "daily operations, invoicing, email account management & technical competence of the relationship management," "project/event delivery." ; "You are supported by a financial account manager"
- Owner keeps: finance support sits with a "financial account manager" (bookkeeping not fully handed over).
- Codes (final): D10 D14 D15(invoicing). Segment: COMM. Not counted: D07 (members are described but no member duty is given) and D08 ("scheduling" is generic).

### P08 | Art School Ceramics Programs Manager | Flint Institute of Arts | Flint MI, US | NPO (museum art school) | posted 2025-11-17
- URL: https://www.ceramicsnow.org/news/the-flint-institute-of-arts-is-hiring-an-art-school-ceramics-programs-manager/
- Hours: "FLSA Status: Exempt, Full-Time". Pay/benefits: not stated. Reports to: "Reports to: Director of Art School". Supervises: not stated ("Instructor/technician training and mentoring").
- Duties (verbatim bullets): "Equipment maintenance and improvement" ; "Ceramic facilities upkeep and organization" ; "Oversight of firing process" ; "Studio demonstrations and outreach" ; "Instructor/technician training and mentoring" ; "Course planning, research, and instruction" ; "Firing and organization of student work" ; "Recruitment of students and supporters" ; "Development of new programs and cross-departmental collaboration"
- Scale: "handling and firing of more than 20 tons of clay each year" ; "Each year, the FIA Art School offers more than 400 non-credit classes"
- Codes: D01 D02 D06 D08 D09 D12 D13 D16(recruitment/outreach)

### P09 | Ceramics Studio Coordinator | University City Arts League (UCAL) | Philadelphia PA, US | NPO (community art centre) | posted 2022-06-15 (PRE-2023)
- URL: https://www.ceramicsnow.org/jobs/ceramics-studio-coordinator-at-university-city-arts-league/
- Hours/pay: "This position is 20 hours per week with a flexible schedule" ; "at $22 an hour". Benefits: not stated.
- Reports to: "The Ceramics Studio Coordinator will work directly with the Executive Director". Supervises: "supervise/manage volunteers to maintain day-to-day studio operations" ; "train new volunteers and mentor student interns as needed"
- Duties (verbatim): "provide logistic support to teaching artists, which includes adult, after school, and community arts classes" ; "conduct first day orientations for each session (fall 1, fall 2, winter, spring 1, spring 2, summer) to go over studio rules" ; "distribute beginning and end of session emails to students" ; "create adult ceramic class surveys for each session" ; "purchase and inventory, including clay, glaze chemical, tool kits, equipment parts, and others as needed" ; "attend weekly staff meetings" ; "organize volunteers for twice a year major studio clean up" ; "plan fall and spring session special topic workshops" ; "troubleshoot any ceramic related issues, including glaze, slip, and oxide problems" ; "maintain and fix kilns and equipment, including throwing wheels, slab roller, and pug mill" ; "reclaim/pug clay as needed" ; "monitor kiln firing schedule to ensure timely turnaround of fired work" ; "fire kilns as needed" ; "receive ceramic supply delivery and restock" ; "construction/build structure, shelves, etc. as needed" ; "keep track of student roster for each session to organize student shelf space" ; "maintain sediment trap to ensure a functional plumbing system" ; "ensure the studio's cleanliness through monitoring the volunteers' completion of their weekly tasks"
- Software: "general computer skills, including Microsoft Office Suite, Google drive, and others as needed"
- Outputs: session orientations; session emails; class surveys (each session); purchase/inventory; firing schedule monitoring; student roster -> shelf space assignment.
- Frequencies: 6 sessions/yr; "twice a year major studio clean up"; weekly staff meetings; volunteers' "weekly tasks".
- Codes: D01 D02 D03 D04 D05 D06(shelf space by roster, turnaround of fired work) D07(orientations, rules) D08 D12 D13 D16 D17(class surveys - weak)

### P10 | Ceramics/Sculpture Studio Manager | California College of the Arts | San Francisco CA, US | UNIV | posted 2023-05-24
- URL: https://www.ceramicsnow.org/jobs/ceramics-sculpture-studio-manager-at-california-college-of-the-arts/
- Hours: "CCA considers a full-time work week to be 37.5 hours per week." ; "Monday – Friday, 9am-5:00pm." Pay: "The compensation for the position is $32 per hour." Benefits: "comprehensive benefits package, including the choice of five medical plans," "generous holiday, sick & vacation time, a retirement match and tuition remission."
- Reports to: not named ("Coordinates with colleagues and supervisor"). Supervises: "The Ceramics/Sculpture Studio Manager supervises a team of 10-12 student workers." ; "Manages/provides supervisory support to 2 Graduate Student Assistants: 1 within Ceramics, 1 within Sculpture."
- Time split (WebFetch summary of section headings, percentages as stated): Studio Operations (40%), Management & Administration (30%), Academic Role/Support (25%), Non-Essential (5%).
- Duties (verbatim fragments): "Maintains equipment inventory and manages studio materials and supplies." ; "Manages storage and handling of studio materials, including hazardous materials;" ; "updates catalog of Safety Data Sheets" ; "creating the student worker schedule" ; "administration of timesheets" ; "plan and execute the annual studio budget" ; "managing the labor budget" ; "Maintains and updates studio manuals, orientation and access records, shared studio calendar" ; "Orders and tracks clay and sculpture material usage." ; "Repairs and/or sends equipment out for repair as needed." ; "work with exterior vendors for specialized repair, upkeep, and purchasing" ; "supports semiannual waste pickup"
- Software: "Studio Managers should be comfortable with the use of digital systems for financial, scheduling, and inventory management." ; "including Google Suite, WebCheckout, Airtable, Adobe Suite, and others."
- Outputs: student worker schedule; timesheets; annual studio budget; labor budget; studio manuals; orientation/access records; shared calendar; SDS catalog; material-usage tracking.
- Codes: D02 D04 D05 D12 D13 D15 D20(equipment/asset inventory - added) ; kiln/glaze only in qualifications ("Blaauw gas kiln, electric kilns, pugmill, potter wheels")

### P11 | Ceramics Studio Manager | Princeton University (New College West) | Princeton NJ, US | UNIV (residential-college open studio) | posted 2022-05-09 (PRE-2023)
- URL: https://www.ceramicsnow.org/jobs/ceramics-studio-manager-at-princeton-university/
- Hours: "The Studio Manager will work on site 20h/week, and regular nights and weekends are required." ; "This is an in-person, 10-month position". Pay: not stated. Benefits: "with full benefit coverage for 12 months"
- Reports to: "Reporting to the dean of New College West through the College Program Administrator". Supervises: "The Studio Manager hires and supervises student staff who help during open studio hours."
- Duties (verbatim): "Lead regular open studio hours and study breaks, assisted by student staff" ; "Hire, schedule, train, and manage student staff to help with studio open hours" ; "Maintain safety and organization in the studio under the direction of Environmental Health and Safety" ; "Manage the studio budget with oversight from the College Program Administrator" ; "Order, organize, and maintain studio supplies and equipment" ; "Manage studio webpage and sign-in software" ; "Manage studio publicity and email account" ; "Assist in shop maintenance and clean up; coordinate with Building Services Staff" ; "Coordinate and teach focused ceramics workshops for undergraduates and graduate students" ; "Work with college staff to plan studio programming during fall break, Wintersession, and spring break"
- Software: "sign-in software" (unnamed); studio webpage; email.
- Codes (final): D02 D05 D07(open-studio sign-in/access - weak) D08 D09 D12 D13 D15 D16

### P12 | Pottery Painting Studio Manager | Color Me Mine Frisco | Frisco TX, US | PYOP (franchise) | undated (Handshake job 11334402; accessed 2026-10-20)
- URL: https://app.joinhandshake.com/jobs/11334402/share_preview
- Hours: "Job Type: Full-Time". Pay/benefits: not stated in preview.
- Reports to: implied Studio Owner ("as directed by the Studio Owner"). Supervises: "This position has supervisory responsibilities." (studio staff team members)
- Duties (verbatim): "Reviews Applications, Schedules Interviews, Recruits Staff, Trains Staff" ; "Conducts Performance & Compensation reviews, Counsels the employees as needed" ; "Operates and Manages studio in accordance with Manuals, Procedures, Processes and Standards" ; "Schedules employees and prepares employees' roster" ; "Is present for large events, and participates in corporate marketing promotions" ; "Engages customers with the Color Me Mine brand through execution of social media platforms" ; "Manages and maintains adequate stock levels and inventory SKUs pricing" ; "Adheres to inventory and FF&E (Furniture, Fixtures and Equipment) count schedules" ; "Performs a quarterly Safety Audit of premises" ; "Processes donation requests as approved" ; "Responds to customers' queries and complaints" ; "Manages marketing programs and displays as directed by the Studio Owner" ; "Liaises between Studio Staff Team Members, Customers, and Studio Owner" ; "Collaborates effectively with the Studio Owner in regards to maintenance with kilns and in-studio repairs"
- Software: "Adaptable to software and technology adopted by Color Me Mine." (none named)
- Outputs: staff roster/schedule; performance and compensation reviews; inventory SKU pricing; inventory and FF&E counts (on a schedule); quarterly safety audit.
- Owner keeps: kiln maintenance and repairs are shared with the owner; marketing "as directed by the Studio Owner"; donations "as approved".
- Frequencies: "Performs a quarterly Safety Audit of premises"; FF&E "count schedules" (interval not stated).
- Codes: D02(with owner) D05 D10 D11(stock/SKU pricing) D12 D13 D14 D16 D20(FF&E asset counts - added)

### P13 | Pottery/Ceramics Studio Manager and Visual/Community Arts Support | View Arts Center | Old Forge NY, US | NPO | undated (Handshake job 6379061; accessed 2026-10-20)
- URL: https://app.joinhandshake.com/jobs/6379061/share_preview
- Hours: "Job Type: Full-Time"; "able and willing to work an occasional evening". Pay: "Compensation and position offer will be commensurate with qualifications and experience". Benefits: not stated.
- Reports to: "This position reports to the President, and operationally to the CFO/Director of Business Operations." Supervises: not explicit; "recruitment and retention of talented clay instructors, studio assistants and interns".
- Duties (verbatim, some truncated by the tool): "Develops and implements policies and procedures for our pottery/ceramics program." ; "Prepare annual operating budget for areas of responsibility and take responsibility for management of the budget…" ; "Oversees pottery/ceramics studio safety and maintenance of all studio facilities and equipment, making…" ; "Reclaim studio clay, glazes, load and unload kilns, and manage the firing process and all related…" ; "Overall responsibility for directing our instruction-led programs, including teaching, as well as recruitment and…" ; "Oversees and runs paint-on-pottery, and other summer drop-in classes as needed or assigned." ; "Work with Visual Arts management to plan and facilitate exhibitions for clay or other studio related mediums." ; "Work with Marketing to propose social media and advertising content that promotes our pottery/ceramics studio." ; "Act in the lead role in coordination with the Pottery Committee to plan, manage and execute the Chili Bowl…" ; "Manages the independent pottery program." ; "Works with schools to schedule and manage school field trips for pottery."
- Frequencies: "Pottery Circle meets most Mondays 10:00 am -1:00 pm." ; Chili Bowl "(in February)".
- Codes: D01 D02 D03 D04 D07(policies; independent pottery program) D08 D09 D12(recruit instructors/assistants) D13 D14 D15 D16 D18

### P14 | Ceramics Studio Manager | Kirkland Arts Center | Kirkland WA, US | NPO | posted 2023-01-30
- URL: https://depts.washington.edu/advis104/2023/01/30/kirkland-art-center-seeks-ceramics-studio-manager/ (UW School of Art advising blog repost)
- Hours: "Status: Exempt/Salary (40 hrs)". Pay: "Pay: $54,000 to $59,000 DOE". Benefits: "medical and dental insurance, generous PTO," "one free art course per quarter ($2000 value), free parking, and relevant professional training."
- Reports to: "Reports To: Education Director". Supervises: "Supervises: Technicians, Work/Study Students, Volunteers" ; "Supervise and train studio technician team (2)."
- Duties (verbatim): "Train others in safe operation and maintenance of ceramics facilities, including but not limited to:" "kiln loading, firing and unloading, glaze mixing, supply inventory, equipment maintenance and general cleanliness." ; "Provide instruction, coaching, and/or mentorship to the KAC community, when necessary and appropriate." ; "Act as technical advisor for students and instructors." ; "Work with the Education Director, instructors, and students to prepare for classes and workshops." ; "Maintain, inventory and replace all equipment, materials, and supplies within the ceramics department," ; "Maintain firing schedules, glaze mixing, equipment assessments, routine maintenance for studio equipment" "and cleaning schedules." ; "inspect glaze test results and update, change, or add glazes." ; "establish open studio schedule, kiln schedule," ; "Keep all supplies stocked (clay, glazes, tools, class supplies)." ; "Assist Education Coordinator with purchasing and coordinating purchasing." ; "See that kilns are loaded, fired, unloaded properly and on a timely basis." ; "Effectively communicate verbally and in writing the studio policies and procedures concerning health," ; "Help prepare departmental budgets." ; "Advise on equipment purchases."
- Software: none. Outputs: firing schedules; kiln schedule; open studio schedule; cleaning schedules; equipment assessments; equipment/material inventory; glaze tests; departmental budget input.
- Codes: D01 D02 D03 D05 D07(policies) D08 D09 D12 D13 D15 D20(equipment inventory and replacement)

### P15 | Studio Manager | The Clay Corner (Gallery and Firing Center) | Seattle (Ballard) WA, US | COMM/firing service + gallery (segment: COMM, see note) | posted 2021-08-05 (PRE-2023)
- URL: https://depts.washington.edu/advis104/2021/08/05/the-clay-corner-seeks-studio-manager/
- Hours: "Monday – Noon to 7 pm," "Thursday – Noon to 7 pm," "Saturday – Noon to 6 pm." (20 h by my sum) ; "The work schedule is flexible and subject to change." Pay: not stated ("increased responsibilities and pay may be available."). Reports/supervises: not stated.
- Duties (all bullets): "Help customers and sell products." ; "Load and unload kilns." ; "Plan gallery events." ; "Organize artists and their work." ; "Additional admin work."
- Codes: D01 D10 D11 D14 D18

### P16 | Studio Coordinator | Pottery Northwest | Seattle WA, US | NPO | posted 2022-11-02 (PRE-2023)
- URL: https://depts.washington.edu/advis104/2022/11/02/pottery-northwest-seeks-studio-coordinator/
- Hours: "Full-time, 40 hours per week; salaried, exempt" ; "availability to work 1-2 nights and weekends per month required". Pay: "$47,000-$50,000 DOE". Benefits: "Open (unlimited) PTO" ; "100% employer premium paid medical, vision and dental insurance" ; "401(k) retirement plan" ; "Monthly commuter stipend of $140" ; "Annual professional development stipend of $500" ; "Complimentary classes, workshops, and studio usage"
- Reports to: "Reports to: Operations Director". Supervises: "Manage volunteers."
- Time split (section headings as returned): Administrative Assistance (70%), Studio Event Support (15%), Gallery Attendant (10%), Shared Leadership (5%). The tool summarized the admin bullets (front desk, calls/emails, tours/orientations, calendars, data entry, registration, scholarship paperwork, student records); verbatim bullets obtained: "Register and sign-in event attendees." ; "Ring in gallery sales." ; "order office, kitchen, and bathroom supplies quarterly as needed" ; "Produce an annual work plan with metrics tied to overall organizational goals." ; "Participate in team check-ins, weekly staff meetings, and planning and support for Pottery NW events."
- Software: "Proficiency with the Google Suite, Monday.com, and the ability to learn new software."
- Outputs: annual work plan with metrics.
- Codes (final, verbatim-supported only): D05(office supplies) D11 D12 D14 D17 D18. Summary-only and not counted: D07 (orientations), D08 (registration), D10 (front desk).

### P17 | Ceramics Studio Manager | The Canvas (community art studio serving artists with disabilities) | Juneau AK, US | NPO | posted 2016-09-01 (PRE-2023, 2016)
- URL: https://depts.washington.edu/advis104/2016/09/01/ceramics-studio-manager-juneau-ak/
- Hours/pay/benefits: not stated. Reports to: not stated (works "with the Artistic Coordinator to order supplies"; projects "as coordinated with Canvas Director"). Supervises: "orienting and supervising members and volunteers"
- Duties (verbatim): "Teach both artists who experience disabilities and members of the community of all ages." ; "Help select student art work for exhibit in the studio and/or outside exhibits." ; "Maintain the pottery studio environment including cleaning the space and equipment," "inventorying and organizing supplies, and working with the Artistic Coordinator to order supplies as needed." ; "Ensure the studio is prepared each day depending on the class and open studio schedule." ; "Manage special projects, including empty bowls, ceramic works for sale,"
- Codes: D05 D07(orienting members) D09 D12(volunteers) D13 D14 D18

### P18 | Studio Manager | Just Kiln' Time "Paint Your Own Pottery" | Menomonee Falls WI, US | PYOP | undated (Betterteam ATS; accessed 2026-10-20)
- URL: https://justkilntimepottery.betterteam.com/studio-manager-1
- Hours: "Full-time." ; "Shifts can range from 4- 8 hours per day depending on busy seasons!" ; "able to work days, nights & weekends on weekly basis". Pay/benefits/reports/supervises: not stated.
- Duties (verbatim): "Handling staff, hiring, work schedules." ; "Daily social media post." ; "Event planning." ; "Ordering all studio supplies and stock." ; "Customer service (interacting with both kids and adults)." ; "Answer phones, book parties, ring up sales, wrap up masterpieces, process pottery, unpack stock, and re-stock shelves." ; "Painting displays." ; "Cleaning studio (mop, sweep, organize, take out the trash, resetting tables." ; "Leading and assisting birthday parties and other events (adult and child)." ; "Must be comfortable giving paint demonstrations (both small and large groups)."
- Software: none. Outputs: staff work schedules; daily social post; party bookings.
- Frequencies: "Daily social media post."
- Codes: D01(implied "process pottery" - weak, not coded) D05 D06("wrap up masterpieces, process pottery") D09(paint demos) D10 D11 D12 D13 D14 D16

### P10 addendum (full verbatim text obtained from https://www.ceramicsnow.org/?p=26588, same posting)
- "Delivers general mandatory studio safety orientations for all studio users;" ; "maintains inventory documents, updates catalog of Safety Data Sheets," ; "supports semiannual waste pickup and attends mandatory HazMat trainings." ; "Independently manages a team of 10+ student workers, which includes:" "managing the labor budget, creating the student worker schedule, writing and updating job descriptions," "hiring, administration of timesheets, performance management," ; "Collaborates with their leadership to plan and execute the annual studio budget," "including materials, supplies, equipment, and student worker funding requests." ; "Maintains and updates studio manuals, orientation and access records, shared studio calendar, and" "Portal web page." ; "Provides feedback on course scheduling" ; "Works with Chairs, faculty and studio users on course-specific needs: this may include ordering materials,"
- Adds D07 (orientations, access records), D08 (feedback on course scheduling) and D22 (WebCheckout equipment checkout, shared calendar). Final codes for P10: D02 D04 D05 D07 D08 D12 D13 D15 D20 D22. Time-split headings, verbatim: "Studio Operations (40%)", "Management & Administration (30%)", "Academic Role / Support (25%)", "Non-Essential Job Functions (5%)".

### P19 | Part-Time Ceramics Studio Manager (MPA Clay) | McLean Project for the Arts (MPA) | McLean VA, US | NPO | posted 2026-03-31, deadline 2026-06-30
- URL: https://www.eastcityart.com/?p=106321 (East City Art repost). Employer PDF https://mpaart.org/wp-content/uploads/2026/03/mpa_clay_studio_manager.pdf could not be decoded by WebFetch.
- Hours: "Part-time, non-exempt" ; "Approximately 15–20 hours per week." ; "Hours are flexible, 15–20 hours per week, distributed evenly throughout the work week". Pay: "Compensation: $22-26/hour, commensurate with experience". Benefits: "Unlimited access to MPA Clay for personal work" ; "Free glazing and firing for personal (non-production) work" ; "Training in kiln operation, glaze mixing, and studio systems" ; "Potential teaching opportunities"
- Reports to / supervises: not stated.
- Duties (verbatim): "Maintain a clean, organized, and fully functional MPA Clay studio and storage areas" ; "Independently load, unload, and fire electric kilns" ; "Monitor kiln performance, maintain firing logs, and manage kiln shelf care" "(cleaning, grinding, kiln wash application)" ; "Oversee routine maintenance of equipment, including kilns and wheels; coordinate repairs as needed" ; "Reclaim clay and manage studio materials and waste systems" ; "Track inventory of supplies and materials; coordinate purchasing and pickups (mileage reimbursed)" ; "Prepare classroom spaces for scheduled classes" ; "Support administrative needs related to MPA Clay, including scheduling, data entry, and coordination" ; "Assist in the development and maintenance of the MPA Clay glaze program (training provided)" ; "Lead orientations for new students and studio members;" "communicate and enforce MPA Clay studio policies and safety protocols" ; "Serve as an ambassador for MPA Clay, supporting community building and helping attract new members and instructors" ; "support a successful launch and ongoing operations" (new studio)
- Software: "Basic computer proficiency (email, Outlook, Google Drive/Docs)". Outputs: firing logs; inventory tracking; purchasing; schedules; data entry.
- Codes: D01 D02 D03 D04 D05 D07 D08 D13 D16(member recruitment - weak)

### P20 | Assistant Studio Manager (body: "Assistant Production Manager") | Marcello Andres Ceramics | Dallas TX, US | PROD (handmade tableware for restaurants/hotels) | LinkedIn "1 week ago" at access 2026-10-20 (~Oct 2026)
- URL: https://www.linkedin.com/jobs/view/assistant-studio-manager-at-marcello-andres-ceramics-4476828332
- Org: "Marcello Andres Ceramics is a Dallas-based studio producing handmade ceramic tableware" "for leading restaurants, hotels, and design clients."
- Hours: "Full-time, 40 hours per week, Monday through Friday, 9:00 AM to 6:00 PM, with a one-hour unpaid lunch break". Pay: "Competitive pay based on experience". Benefits: not stated.
- Reports to: "studio leadership" (not named). Supervises (de facto): "Conduct performance reviews of production staff on a quarterly basis" ; "Hold the production team accountable to output, quality, and process standards"
- Duties (verbatim): "Own and maintain the studio's core production-tracking documents, keeping them current as the single source of truth" "for what's in the pipeline, where each order stands, and its projected completion" ; "Log same-day entries — pieces poured, pieces glazed, and yield-loss figures —" ; "Translate order priorities into the production team's daily work assignments" ; "Know current production capacity and pace well enough to help set and protect promised delivery dates" ; "Ensure client communication stays ahead of any delays" ; "Prepare, mix, test, and maintain casting slip at the appropriate specific gravity," ; "Apply glaze to pieces per studio specifications" ; "Load and unload kilns according to firing schedules" ; "Track and manage mold drying cycle times and rotations" ; "Quality control, packing, and shipping" ; "Train production staff on proper technique" ; "Manage studio production assets and ensure proper use and care of equipment and molds" ; "Monitor and troubleshoot warping, cracking, defects, and yield loss" ; "Inventory and care for one-part and multi-part plaster molds, storing them to prevent breakage" ; "Perform basic maintenance and repairs on studio production equipment; flag malfunctions that need outside repair" ; "Support retail and studio events as needed" ; "Work studio sales; this is a mandatory part of the role"
- Software: none named; preferred: "Experience with production or inventory tracking systems"
- Outputs: production-tracking documents (pipeline, order status, projected completion); same-day logs (pieces poured, glazed, yield loss); daily work assignments; quarterly performance reviews; mold inventory.
- Frequencies: "Log same-day entries" ; "daily and weekly output targets" ; "on a quarterly basis" (reviews)
- Codes: D01 D02 D03 D04 D11 D12 D13 D14 D20(production assets, mold inventory) D21(production/order tracking - added: work-order pipeline, analogous to D06 for production studios)

### P21 | Production Manager | Farmhouse Pottery | Woodstock VT, US | PROD | posted 2024-03-31
- URL: https://www.ceramicsnow.org/jobs/production-manager-at-farmhouse-pottery-woodstock-vermont/ (also https://www.ceramicsnow.org/?p=31903)
- Hours/pay: not stated. Benefits: "Medical, dental, and vision insurance" ; "Eight weeks paid parental leave" ; "401(k) with 4% company match" ; "Annual company profit sharing" ; "Opportunity to make your own pottery, plus product discounts"
- Reports to: not stated. Supervises: "managing a team of artisans" ; "Manage the studio supervisor to foster a positive work environment"
- Duties (verbatim): "Weekly production planning and forecasting based on inventory levels," "planned promotions, upcoming product launches etc." ; "Assist in budget planning and cost control measures to optimize studio operations" ; "Conduct regular safety inspections and address any hazards or concerns promptly" ; "Coordinate with the lead trainer on the scheduling of the weekly work orders, wheel assignments," "and days off scheduling for production staff" ; "recruiting, onboarding, training, career development, and retention strategies" ; "Lead weekly production meetings" ; "Work with studio technician to ensure equipment is regularly inspected, maintained, and fully operational" "and that tools, clay, and glaze are ordered in a timely manner" ; "Implement and maintain quality control measures" ; "Properly track the production workflow from assigned work orders to WIP to finished goods," "both physically and digitally in our inventory management system" ; "Manage scheduling of large or special work orders through the production process" "(i.e. custom, wholesale, research and development, etc.)" ; "Communicate demand levels of finished goods to glaze & kiln teams"
- Software: "Computer literacy required; this position will be trained in our ERP, NetSuite" ; "Proficiency in Microsoft Office Suite, Google Sheets"
- Outputs: weekly production plan and forecast; weekly work orders; wheel assignments; days-off schedule; WIP/finished-goods tracking in inventory system; safety inspections.
- Frequencies: weekly planning, weekly work orders, weekly production meetings.
- Codes: D02(via technician) D05 D12 D13 D15(budget/cost control) D21(work orders->WIP->finished goods)

### P22 | Ceramics Studio Coordinator | Sonoma Community Center (Sonoma Ceramics) | Sonoma CA, US | NPO | posted 2022-03-29 (PRE-2023)
- URL: https://www.ceramicsnow.org/jobs/ceramics-studio-coordinator-at-sonoma-ceramics/ (same text at https://www.ceramicsnow.org/?p=18344)
- Hours: "Hours: 20 hours per week, non-exempt". Pay: "$20-25 per hour depending on experience". Benefits: "paid sick leave; use of the ceramics facilities."
- Reports to: "Director of Ceramics". Supervises: "Train and oversee Artists in Residence and Interns" ; "Attend monitor meetings, communicate and coordinate with monitors"
- Duties (verbatim): "Fire gas, electric, Raku, soda kilns" ; "Manage kiln-firing schedule" ; "Maintain equipment such as kilns, slab rollers, tables, and wheels; coordinate repair of equipment as needed" ; "Mix glazes and clay; reclaim clay" ; "Order and procure supplies and materials; track inventory" ; "Plan and lead bi-annual studio cleaning" ; "Assist Ceramics Director with administrative tasks, including data entry, mailings, scheduling, and marketing" ; "Assist with marketing and communications, including planning and implementation of a strong social media presence" ; "Participate in fundraising planning and execution" ; "Plan, install and execute Gallery 212 exhibitions, sales, educational programming and outreach" ; "Assist with class set up and instructor relations leading up to and following programming including payouts etc." ; "Co-manage resident artist's hours and scope of work with Director" ; "Help to keep website up to date and correct" ; "Coordinate jury for resident artist applications" ; "Help recruit new instructors" ; "Membership stewardship, some volunteer coordination"
- Software: none named. Outputs: kiln-firing schedule; inventory; instructor payouts (assist); exhibitions.
- Codes: D01 D02 D03 D04 D05 D07 D08 D12 D13 D15(instructor payouts - assist) D16 D18 D19

### P23 | Director of Ceramics | Truro Center for the Arts at Castle Hill | Truro MA, US | NPO | deadline 2021-06-18 (PRE-2023, 2021)
- URL: https://www.castlehill.org/director-of-ceramics
- Hours: "full time, year round." ; "9:00 a.m. – 5:00 p.m. (times will vary depending on evening workshops and firings)". Pay: "$45,000 - $55,000 + benefits (50% paid for health Insurance)"
- Reports to: not stated (works "in conjunction with Executive Artistic Director"). Supervises: "Assisting with the overseeing of interns and work study"
- Duties (verbatim): "Preparing studios for each workshop, cleaning up after use, closing down at the end of the day" ; "Assisting the faculty, checking attendance, and generally monitoring each class" ; "Maintaining equipment and supervising its use" ; "Firing of all kilns: electric, gas, salt/soda, wood, raku" ; "Mixing glazes and managing all raw materials" ; "Scheduling of workshops for Spring, Summer, Fall & Winter in conjunction with Executive Artistic Director" ; "Teaching children, teen, and adult classes in throwing and hand building year-round (Wednesday throwing/wood firings)" ; "Keeping grounds in order and all public spaces, i.e. bathrooms, gallery, landscaping, parking" ; "Assisting with Auction and other events as planned" ; "Ordering all materials and supplies" ; "To make plates or other clay items for the Auction (during the off season) & 100 mugs for the Winter Party." ; "Cleaning, inventory and closing the studios as needed"
- Outputs: workshop schedule (4 seasons); "100 mugs for the Winter Party".
- Codes: D01 D02 D03 D04 D05 D08 D09 D12 D13 D14

### P24 | Ceramics Instructional Operations Coordinator | Harvard University, Office for the Arts Ceramics Program | Cambridge MA, US | UNIV (program also serves community members) | posted 2024-04-22
- URL: https://www.ceramicsnow.org/jobs/ceramics-instructional-operations-coordinator-at-the-ceramics-program-office-for-the-arts-at-harvard/
- Hours: "Ability to work flexible hours and weekends." FT/PT and pay: not stated. Benefits: "3-4 weeks of accrued vacation time per year" ; "Comprehensive medical, dental, and vision benefits" ; "University-funded retirement plan" ; "Tuition Reimbursement"
- Reports to: "reports to the Director, OFA Ceramics Program and Visual Arts". Supervises: "9 direct reports (monthly instructional payroll)" ; "12 studio interns who assist with maintenance, upkeep, and special projects."
- Time split (verbatim headings): Ceramics Facilities (25%); Kiln Management (5%); Health and Safety, University Compliances (10%); Health and Safety, Internal Compliances (5%); Financial & Procurement (10%); Staff Supervision (15%); Academic Collaborations (15%); Gallery 224 (5%); Show & Sales (5%); Tenant duties/HRE (5%).
- Duties (verbatim): "Supervises the mixing and testing of over 100 glazes and the inventory of all ceramic materials." ; "Maintains online sign-up sites for electric kilns, gas kilns, and spray booths" "and tracks those certified to fire kilns, use of plaster/mold room, and materials mixing room." ; "Responsible for repairing equipment when needed and overseeing glaze mixing and material ordering." ; "Also monitors the scheduling of loading, firing and unloading of these kilns." ; "Responsible for total of 13 kilns (2 gas, 1 soda, 10 electric)." ; "scheduling with vendors to remove non-hazardous glaze waste." ; "Orders and maintains Material Safety Data Sheets (MSDS) in addition to other compliant safety labeling" "such as Standard Operating Procedure (SOP) safety sheets for all potentially dangerous studio equipment." ; "Develops and implements budget for studio maintenance and usage with Director." ; "Responsible for tracking materials use, ordering, receiving and storing clay materials, kiln repair supplies;" "and maintains contact with vendors." ; "Oversees purchasing of additional equipment." ; "Advises and executes technical aspects of Gallery 224 installations (6-8 annually)" ; "Oversees staff and participants with detailed set-up of two annual large-scale Show and Sales,"
- Software: "Proficient with MS Outlook, Microsoft Word and Excel." ; "Knowledge of Filemaker Pro, Glaze • Softwares (such as HyperGlaze) and 3D software (such as Rhino, Slic3r)." ; online kiln/spray-booth sign-up sites (unnamed).
- Outputs: kiln and spray-booth sign-up (booking) sites; certification tracking; MSDS/SOP sheets; budget; material-use tracking; monthly instructional payroll (supervision of 9).
- Frequencies/scale: "serves more than 300 students, faculty, staff, and members of the public over 3 semesters annually" ; gallery installs "(6-8 annually)" ; "two annual large-scale Show and Sales"
- Codes (final): D01 D02 D03 D04(supervises "clay-mixing tasks") D05 D07(certification tracking) D09(instructs equipment use - weak) D11(show and sales) D12 D13 D14(two annual Show and Sales) D15 D18 D22(equipment booking/sign-up - added)

### P25 | Residency Coordinator (SHAW Centre programs) | Medalta | Medicine Hat AB, Canada | NPO (museum + ceramic studio/residency centre) | posted 2023-05-17, due 2023-05-29
- URL: https://www.ceramicsnow.org/jobs/residency-coordinator-at-medalta/
- Hours: "This is a full-time salaried position (4 days/week, 32 hours/week." Pay: "Salary: $42,000 – $45,000 per year" (CAD). Benefits: "Casual dress, Company events, Flexible schedule, On-site parking, Paid time off, Store discount"
- Reports to: "Reporting directly to the Executive Director". Supervises: "Provide day-to-day oversight to the SHAW Centre Studio Technician, class instructors, and SHAW Centre volunteers."
- Duties (verbatim): "Develop and maintain operational policies and procedures for SHAW Centre programs, and Artists' Lodge." ; "Program and manage the artist residencies, workshops, classes, and special events." ; "Grant application writing and reporting for funding programming" ; "Manage kiln rental bookings and accommodations at the Artists' Lodge." ; "Prepare invoicing for sales from within the SHAW Centre, including kiln rentals, decal printing," "and ceramic material purchases in accordance with FOMS financial policies." ; "Prepare purchase orders for procurements to the SHAW Centre in accordance with the FOMS purchasing policies." ; "Liaise with curatorial and exhibition programming for the Yuill Family Gallery." ; "Work with the Executive Director to prepare and manage budgets for the SHAW Centre, Artists' Lodge, and programming." ; "Provide regular updates to the Executive Director."
- Software: "Strong computer skills with superior knowledge of Adobe Creative Suite and MS Office are essential." ; "Knowledge of Constant Contact, PayPal an asset." ; "computer software for word processing, financial management, contact management, and Internet presence."
- Outputs: purchase orders; invoices (kiln rentals, decal printing, materials); grant applications and reports; budgets; kiln-rental and lodging bookings.
- Codes: D05(POs) D08 D11(material sales) D12 D14(kiln rentals, events) D15 D16 D17 D18 D19 D22(kiln rental bookings)

### P26 | Studio Coordinator of Ceramics | Anderson Ranch Arts Center | Snowmass Village CO, US | NPO (workshop/residency centre) | posted 2022-06-06 (PRE-2023)
- URL: https://www.ceramicsnow.org/jobs/studio-coordinator-of-ceramics-at-anderson-ranch-arts-center/
- Hours: "Regular Full-Time, Exempt" ; "particularly intense time demands in the summer months." Pay: "Base Salary: $45,000 – $47,000" ; "Total Compensation Package Value (including housing/studio space): $68,000 – $70,000". Benefits: "Employer Housing Provided – On campus studio apartment" ; "Company paid medical insurance" ; "Retirement plan/discretionary 403(b) match"
- Reports to: "Vice President, Artistic Affairs". Supervises: "Support the recruitment, selection, training and oversight of assigned volunteers, teaching assistants and interns."
- Duties (verbatim): "Manage and supervise the studio facilities, equipment and supplies." ; "Perform and document routine inspections and service maintenance of equipment, including upgrades and minor repairs." ; "Purchase and organize supplies, maintain inventory, receive shipments and manage purchase orders." ; "Manage administrative records, including operating within a budget, posting income, expenses and" "calculating estimates and budget requests." ; "Support the Artistic Director to develop workshop concepts and curriculum, assist with identifying faculty," "edit descriptions, recruit students, orchestrate faculty and workshop logistics and monitor workshop evaluations." ; "Studio Coordinators are expected to actively teach and/or co-teach scheduled workshops" ; "supporting grants and preparing reports."
- Software: "Strong computer skills and experience with standard software" (none named). Outputs: documented equipment inspections; purchase orders; inventory; budget requests; workshop evaluations; reports.
- Codes: D02 D05 D08 D09 D12 D13 D15 D16 D17 D19 D20(documented equipment inspections)

### P27 | Studio Operations Manager – 3D (oversees Ceramics Studio Manager) | California College of the Arts | San Francisco CA, US | UNIV (multi-studio) | posted 2022-06-17 (PRE-2023)
- URL: https://www.ceramicsnow.org/jobs/studio-operations-manager-3d-at-california-college-of-the-arts/
- Hours: 37.5 h/wk FT basis, "Monday through Friday". Pay: "The range for the position is $70,551 – $71,962 annually." Benefits: medical (CCA covers "an average of 91%"), retirement match, tuition remission.
- Reports to: "The Studio Operations Manager reports to the Associate Director of Studio Operations at CCA,". Supervises: "direct supervision of 3 full-time employees, 2 Graduate Student Assistants," "and indirect supervision of up to 20 workstudy students." ; "Management of 3 full-time union-eligible staff in 3D Studio: Ceramics Studio Manager,"
- Duties (verbatim): "Develops and maintains systems and policies for safety, access, and management of the 3D Studios." ; "Oversight of operating, capital and refresh budgets for 3D Studios." ; "Manages content, scheduling and delivery of safety orientations" ; "Primary coordinator of Hazardous Waste Management and Disposal for the 3D Studios:" ; "Maintains up-to-date orientation manuals"
- Software: "G Suite, Excel, Adobe" "Illustrator and/or CAD applications"
- Time split: Management & Administration 40%, Academic Support 25%, Studio Operations 30%, Non-Essential 5%.
- Codes: D07(access/orientation systems) D09(instructional resource) D12 D13 D15 ; no kiln/glaze duties at this level (delegated to Ceramics Studio Manager).

### P19 update: later repost of the same MPA role (counted once; latest kept as primary)
- URL (live at access): https://www.simplyhired.com/job/HPx02-UVXcGpvrFe-kqcFDbEL5UDJ2YrpvsF792wFe7FqHyJl9BBOg - title "MPA Clay Ceramics Studio Manager"; date not shown.
- Hours: "About 20 hours a week, primarily weekday mornings". Pay: "$27,500 - $30,000 a year". Reports to: "It reports to the Artistic Director."
- Extra verbatim duties in this version: "Keep the studio, glaze room and storage clean, organized and fully functional." ; "Lead orientations for new students and Studio Artist Members." ; "Serve as the day-to-day contact for members." ; "Maintain safe practices for clay dust, glaze materials and kiln ventilation." ; "Support visiting-artist workshops, including setup, firing and artist hospitality." ; "Help stock the studio store: tool kits, clay, aprons and supplies." ; "Manage student open studio sign-ups, shelf assignments and the shared studio calendar."
- Adds codes D06(shelf assignments) D11(studio store) D22(open studio sign-ups, shared calendar).

### P28 | Operations Coordinator | Pottery Northwest | Seattle WA, US | NPO | posted 2020-12-01 (PRE-2023, 2020)
- URL: https://depts.washington.edu/advis104/2020/12/01/pottery-northwest-seeks-operations-coordinator/
- Hours: "This position is a 40 hour a week, full-time, non-exempt position, with occasional weekend and evening hours." Pay: "$38,000 – $42,000". Benefits: "3 weeks vacation" ; "Medical and dental insurance" ; "Complimentary class and access to the facility"
- Reports to: "The Operations Coordinator reports directly to the Executive Director". Supervises: not stated.
- Duties (verbatim): "Reception desk duties" ; "Manage scheduling and calendar for ED and leadership committees" ; "Compile and distribute board meeting materials" ; "Maintain rosters for classes and studio access" ; "Manage scheduling software for studio access and virtual classes" ; "Manage website including online shop, classes products, and landing pages" ; "Gather inventory, track sales, coordinate pick-up and shipping for online gallery" ; "Issuing invoices through PayPal batch uploads" ; "Accounts payable/receivable" ; "Tracking transactions in Quickbooks" ; "Keeping physical records of transactions" ; "Preparing deposits (cash, checks, PayPal)" ; "End of month and year reconciliations" ; "Forecasting" ; "Tracking and processing gallery and retail sales" ; "Running payroll" ; "Collecting and filing tax forms, contracts, etc"
- Software: QuickBooks ("Tracking transactions in Quickbooks"; desired "Quickbooks or accounting software"); PayPal (batch invoices, deposits); "scheduling software for studio access and virtual classes" (unnamed); website with "online shop"; "Experience with Microsoft Office Suite or similar software"
- Outputs: invoices; deposits; month-end and year-end reconciliations; forecasts; payroll; board meeting materials; class/studio-access rosters.
- Frequencies: "End of month and year reconciliations"
- Codes: D05(inventory for online gallery) D07(studio access rosters) D08 D10 D11 D15 D16(website) D17(board materials) D18 D22(studio access scheduling software)

### P29 | Retail/Business Manager | thrown elements pottery | Arlington Heights IL, US | PYOP (also classes; "paint-your-own pottery, wheel throwing, handbuilding, classes, workshops, camps, parties") | SimplyHired "8 days ago" at access 2026-10-20 (~Oct 2026)
- URL: https://www.simplyhired.com/job/GkiBqsqPNEfmy49EYdNB8MYHZJfPXYdR9kj83khHE3iqcXbp457qYw
- Hours: "Full-time" ; "Can work a consistent weekend day, some evenings, and peak seasonal periods". Pay: "$49,040 - $59,500 a year". Benefits: "401(k)" ; "Paid time off" ; "40 hours of paid leave each year" ; "20 percent employee discount on eligible classes, workshops, and paint-your-own pottery, with no studio fees"
- Reports to: implied founder ("You will work closely with a creative founder who has the ideas, relationships, and long-term vision" "needs a manager who can turn priorities into consistent execution."). Supervises: staff (unspecified): "to lead our people, daily operations, financial discipline, and customer experience"
- Duties (verbatim): "You will still work with budgets, schedules, sales goals, staff performance, and systems." ; "Have personally hired, scheduled, trained, coached, evaluated, and corrected employee performance" ; "Can manage sales goals, labor or payroll targets, cash, expenses, inventory, and basic business reports" ; "Enjoy improving systems and helping a small business become less owner-dependent" ; "including the systems and team needed for a larger destination location."
- Owner keeps vs hands over: founder keeps "ideas, relationships, and long-term vision"; manager takes execution, staff, money, inventory, reports ("less owner-dependent").
- Software: none named. Outputs: staff schedules; budgets; sales goals; labor/payroll targets; "basic business reports".
- Codes: D05 D10 D11 D12 D15 D17

### P30 | Back Room Manager - Art Studio | MADE: my art + design experience | Mamaroneck NY, US | PYOP (multi-craft; "pottery painting, wet clay, mosaics, parties, camps") | HYBRID-TECH | undated (SimplyHired; accessed 2026-10-20)
- URL: https://www.simplyhired.com/job/DSIrNs5btFfAYqBAQJ5rCzxK0C7wL2jv2Br61BEBJQ89M_J1WzuQLA
- Hours: "Part-time, 20–30 hrs/week year-round (hours vary a bit by time of year)". Pay: "Pay: starting at $19.50–$23/hr, based on experience". Benefits: "Employee discount" ; "Flexible schedule"
- Reports to: not stated. Supervises: "Lead the back room day-to-day, set priorities and delegate to supporting staff" ; "keeping support staff on track without needing to be a formal manager"
- Duties (verbatim): "Glaze customer pieces, then load and unload the kilns" ; "Clean and vacuum the kilns to keep them in good shape" ; "Manage the glaze tank: strain, top off and replace glaze as needed" ; "Check every finished piece before it goes home" ; "Spot issues with fired pieces and re-fire when needed" ; "Track every customer-painted piece so nothing misses its pick-up date" ; "Manage customer bags for pickup" ; "Reach out to customers directly if there's an issue with their piece"
- Codes: D01 D02 D03 D06 D10 D12

### P31 | Management Trainee (body: "Studio Manager – Creative Arts Studio") | Amazing Glaze | Bel Air MD, US | PYOP (multi-medium: pottery, glass fusing, mosaics, canvas) | SimplyHired "7 days ago" (~Oct 2026)
- URL: https://www.simplyhired.com/job/akSjR0l-iHVai6QMXvtXmuTjgz3NioGu23iXcpuv0Fs65D5lH6YuPg
- Hours: "Full-time." "Type: Full-Time (Days, Evenings, Weekends)". Pay: "$16.50 - $20.00 an hour." "Competitive pay with tips". Benefits: "Health insurance," "401(k)," "Paid time off," "401(k) matching"
- Supervises: "Supervise, train, and support daily studio staff to ensure smooth operations and exceptional customer experiences."
- Duties (verbatim): "Host & Instruct: Facilitate engaging group events, including children's birthday parties," "adult reservations, and specialized creative workshops." ; "Guide Guest Creations: Help daily walk-in guests explore various art mediums," ; "Manage Studio Technicals: Learn and oversee the operation of studio kilns," "material inventory, and project firing schedules." ; "Maintain Studio Standards: Keep the workspace organized, fully stocked, vibrant, and welcoming." ; "Inventory Management: Ordering, receiving" (text cut off on page)
- Codes: D01 D05 D09(instruct workshops) D10 D12 D13 D14

### P32 | Recreation Supervisor (Jaycee Park Center for Arts & Crafts) | City of Greenville | Greenville NC, US | MUNI | undated (SimplyHired; accessed 2026-10-20)
- URL: https://www.simplyhired.com/job/bZaDPb5UwhjqYXMqFQI2rq8OMQzRyGduaZg2GlJSyKER3gsiZf5s7Q
- Hours: "Full-time." ; "Must be able to work a flexible schedule including nights, weekends, and holidays". Pay: "$57,616.00 - $89,273.60 a year." ; "The hiring range for this position is $57,616.00 to $61,500.00 per year." Benefits: health, dental, vision, life, disability insurance.
- Reports to: not stated. Supervises: "Supervises a full-time Recreation Assistant (Art Assistant & Pottery Coordinator), a Staff Support Specialist I," "and part-time facility attendants, art instructors, and specialists; recruits, trains, and evaluates staff"
- Duties (verbatim): "Plans, organizes, and supervises a variety of arts and crafts programs such as but not limited to pottery," ; "Schedules the use of center facilities." ; "Maintains a ceramic facility, requiring knowledge of equipment such as potters' wheels and kilns." ; "Prepares and maintains reservation and attendance records and reports, employee time sheets, accident reports," ; "Oversees the center's operating budget, including expenditures and revenue tracking." ; "Conducts facility inspections and coordinates routine maintenance of center facilities and equipment" ; "Reports damages and losses." ; "Collects fees, payments, and memberships." ; "Enters requisitions for supplies and equipment necessary to operate various programs." ; "Develops and manages marketing and communications for the center, including social media and email campaigns" "(e.g., Constant Contact)" ; requirement: "Prior experience loading and firing kilns is required"
- Software: Constant Contact; "various software packages".
- Outputs: reservation and attendance records and reports; employee time sheets; accident reports; budget/revenue tracking; supply requisitions; damage/loss reports.
- Codes: D01(required experience) D02 D05 D07(memberships) D08 D09 D12 D13 D14 D15 D16 D17 D22(facility scheduling/reservations)

### P33 | Ceramic Studio Coordinator | Newport Art Museum (Museum School) | Newport RI, US | NPO | HYBRID-TECH (assistant-level coordinator under a Studio Manager) | SimplyHired "10 days ago" (~Oct 2026)
- URL: https://www.simplyhired.com/job/SrjJx6A_bgH1TN3-2EM4ZLsiHFqoNq3rWugmIBQqqT1ofbb7zVcj-Q
- Hours: "Part-time" ; "Expected hours: 21.0 per week" ; "Schedule: Sunday, Monday, and Friday, 9AM-4PM (negotiable)". Pay: "Compensation: $20–$25 per hour, commensurate with experience". Benefits: "401(k)"; "Paid time off"
- Reports to: "Take direction from the Studio Manager on daily priorities, special projects, and studio needs." Supervises: none.
- Duties (verbatim): "Maintain clean, organized, and safe working conditions across all three ceramics studio rooms." ; "Help with clay reclaiming and processing as needed." ; "Load and unload kilns as needed, following established safety procedures and Studio Manager direction." ; "Monitor students during open studio hours" ; "Communicate studio policies and safety procedures to students as needed." ; "Assist with glaze preparation and mixing under the direction of the Studio Manager." ; "Properly label, store, and maintain glaze materials and supplies." ; "Help prepare the studios for classes, workshops, and other Museum School programs as needed."
- Codes: D01 D02 D03 D04 D07(policies) D08 D10 D13

### P34 | Art Studio Manager (part-time) | Framingham State University | Framingham MA, US | UNIV | HYBRID-TECH | posted 2026-08-26
- URL: https://www.simplyhired.com/job/mSgnAeGV7P6l7owyTtoB5KW_Es4bpVLrPqXkhJHA0KO5lNrMxhARrg
- Hours: "This is a part-time, non-benefited contract position, not to exceed 18 hours per week." Pay: "The hourly rate is $20-22/hour."
- Reports to: "Supervised by the Chair of the Art, Design, and Art History Department". Supervises: "N/A"
- Duties (verbatim): "acts as the primary studio technician for all individual art studios" ; "conducts maintenance of studios, point of contact for supply procurement," "inventory of art supplies, and equipment, and coordination." ; "The studio manager participates in supply ordering and equipment maintenance." ; requirement: "Knowledge and experience with ceramics studio operations, including kiln firings."
- Codes (final): D02 D05 D13 D20(equipment inventory). D01 is not counted, because kiln firing appears only as a qualification.

### P35 | Ceramics Program Coordinator | ArtsQuest (Creative Factory) | Bethlehem PA, US | NPO | undated (SimplyHired; accessed 2026-10-20)
- URL: https://www.simplyhired.com/job/_qmIWD9OLeJMZIxrPbD2X3-Il-HNdlJZsteak7Sgp0DNLoRWn8Kd8A
- Hours: "Full-time" ; "Normal business hours for this position are Monday through Friday, 10:00 a.m. to 6:00 p.m.," plus events. Pay: "From $20 an hour". Benefits: not stated.
- Reports to: not stated (works "closely with the Manger [sic] of Visual Arts"; "Weekly Meetings with Manger of Visual Arts"). Supervises: not stated.
- Duties (verbatim): "Independently load and unload kilns, fire kilns (electric Skutt & Cone Art Kilns, computerized)" "as needed during each semester for student work & member work" ; "Complete minor repairs and/or coordinate and oversee repairs for equipment & kilns as needed." ; "Track and maintain inventory of clay, glazes, tools, and other supplies needed for the classroom space." ; "Order materials and schedule deliveries as needed." ; "Includes daily cleaning of all spaces Monday-Friday" ; "Gives mandatory safety orientation to all new Teaching Artists and students during the first 15-20 mins" "of all classes & workshops." ; "Facilitate Ceramic open studio 2 times per week as part of weekly schedule." ; "Working closely with Manager of Visual Arts, creating annual ceramic program & classroom budget." ; "Track spending to budget throughout the year, track revenue goals" ; "Develop any separate budgets or P&L's for ceramic-focused programs as needed" ; "Work with ArtsQuest Retail Department & Visual Arts Manger to develop 1-2 ceramic production items for ArtsQuest." ; "Act as a support for selecting ceramic artists for 1st floor retail area artist consignment program." ; "Oversee selection of ceramic artists for ArtsQuest Studio Membership Program," "act as main liaison between members and ArtsQuest Staff." ; "Maintains and updates classroom policies, safety orientations, and shared classroom calendar"
- Software: "Computer proficiency, including email (Outlook) and Microsoft Office Suites, Microsoft One Drive." Kiln controllers implied by "electric Skutt & Cone Art Kilns, computerized" (brands named, not software).
- Outputs: annual ceramic program and classroom budget; spend-vs-budget and revenue tracking; program P&Ls; inventory; shared classroom calendar; safety orientations.
- Frequencies: open studio "2 times per week"; "daily cleaning of all spaces Monday-Friday"; safety orientation "first 15-20 mins" of every class; weekly meetings.
- Codes: D01 D02 D05 D07(membership selection, liaison) D08 D11(production items for retail) D13 D15 D17(P&L, revenue tracking) D18(consignment) D22(shared calendar)

### P36 | Ceramics Studio Coordinator (job title field "Ceramics Coordinator") | Sebastopol Center for the Arts (SebArts) | Sebastopol CA, US | NPO (community clay studio with members) | HYBRID-TECH (assistant-level coordinator) | posted 2026-10-13
- URL: https://www.craigslist.org/view/d/sebastopol-ceramics-studio-coordinator/hdUMtisz5EcwrxTuBg68AJ
- Hours: "Thursday 9-1pm, Friday 10am-5pm, plus an additional 3-5 hours (flexible)" ; "12-16hours /week". Pay: "$25 hr". Benefits: none listed (perks: "Flexible scheduling"; "Potential opportunities to teach ceramics classes at SebArts").
- Reports to: "Ceramics Program Manager". Supervises: none (works "alongside volunteer Studio Facilitators"; "Assist with orienting new volunteers to studio routines and cleaning standards.")
- Duties (verbatim): "Deep-clean studio surfaces, floors, and equipment using wet-cleaning methods to minimize clay and silica dust." ; "Mop floors and wipe down work tables, glaze counters, and sink areas each shift." ; "Empty and maintain sink sediment traps and reclaim systems." ; "Assist with loading and unloading bisque and glaze kilns as directed." ; "Help recycle and reclaim studio clay." ; "Greet new students, members, and visitors and help them get oriented to the studio layout, storage, and procedures." ; "Answer everyday questions during shifts, such as where to find tools, materials, or work in progress." ; "Help facilitators with setup, cleanup, and restocking before and after sessions." ; "Step in as a Studio Facilitator when coverage is needed."
- Time: cleaning "approx. 70% of role"; mopping/wiping "each shift".
- Software: none. Codes: D01 D04 D07(orienting members) D10 D12(volunteer orientation - weak) D13

### P37 | Ceramics Studio Manager | GoggleWorks Center for the Arts | Reading PA, US | NPO | undated (accessed 2026-10-20)
- URL: https://goggleworks.org/ceramics-studio-manager
- Hours: "Full Time Exempt" (FLSA status); "Keep regular and consistent weekly hours and make the schedule available to all studio-users and administration." Pay/benefits: not shown.
- Reports to: "The Studio Manager reports to the Artistic Director". Supervises: not shown.
- Duties (verbatim): "Coordinate with head of facilities to ensure satisfactory classroom environment" ; "Create and enforce health and safety best practices" ; "Maintain annual calendar with schedule of events, key dates and deadlines" ; "Interview and assess potential instructors/partners" ; "Address maintenance needs and requests made by instructors, monitors and/or students" ; "Supervise and approve ordering of all supplies and materials that are required for classes." ; "Ensure a steady progression of student artwork toward completion as required by the studio's needs in a timely manner" ; "Facilitate in the creation of artworks as requested for retail purposes" ; "Prepare and complete necessary tasks to facilitate student pickup of completed art from short term programming"
- Software: "Google Drive / Google Docs" ; "Microsoft Office Suite (Excel, Word)" ; "Experience with Word Press preferred"
- Outputs: annual calendar of events and deadlines; supply orders (approval); student work progression to pickup.
- Codes: D02 D05 D06 D07(policies) D08 D11(retail artworks) D12(interview instructors) D13

### P38 | Ceramics Studio Manager (seasonal) | Truro Center for the Arts at Castle Hill | Truro MA, US | NPO | "Apply by: March 31, 2024"
- URL: https://www.castlehill.org/ceramics-studio-manager (different role from P23 Director of Ceramics)
- Hours: seasonal, "40 hours per week"; "May - September is Ideal but the dates can vary." Pay: "$16,000 - $17,000" (season). Benefits: none listed; "Must have housing on Cape Cod."
- Reports to: "under the direction of the Ceramic Director". Supervises: not stated.
- Duties (verbatim): "Work with Program Manager regarding ordering materials or supplies" ; "Contacting faculty members to determine workshop needs for materials and equipment" ; "Preparing studios for each workshop, cleaning up after use, closing down at the end of the day" ; "Assisting the faculty, checking attendance, and generally monitoring each class" ; "Maintaining equipment and supervising its use, reclaiming and pugging clay" ; "Keeping grounds in order and all public spaces, i.e. bathrooms, gallery, landscaping, parking" ; "Assist in firing kilns under the direction of the Ceramic Director" ; "Assisting with Open House and other events as planned" ; "Cleaning, inventory and closing the studios at the end of the season"
- Codes: D01 D02 D04 D05 D08 D13 D14

### P39 | Ceramics Program Coordinator | Main Line Art Center | city not stated (posting lists Delaware and Montgomery Counties, PA), US | NPO | posted 2026-04-27
- URL: https://portal.philaculture.org/node/74773 (Greater Philadelphia Cultural Alliance job bank)
- Hours: "30 hours, flexible M-F". Pay: "$22 per hour compensation". Benefits: "This position includes fully funded health and dental insurance" ; "Employer matched SIMPLE IRA retirement plan." ; "11 paid holidays, 1 week paid during December closure" ; "Staff are also encouraged to take a free class each term"
- Reports to: "The Ceramics Program Coordinator reports directly to the Co-Executive Directors." Supervises: "Ceramics Technicians: Management of 2-3 part-time Ceramics Technicians that support the coordinator in all their duties." ; "Volunteer Monitors: Managing a schedule of Volunteer Monitors who oversee Open Studio hours."
- Duties (verbatim): "Kilns: Loading and firing the electric kilns with bisque and Cone 6 work daily." ; "Loading and firing the Cone 10 gas kiln bi weekly." ; "Regular kiln and furniture maintenance like changing elements and grinding shelves." ; "Some basic kiln repair is needed." ; "Glazes: Monitoring glaze supply and mixing new glazes." ; "Responding to feedback about glaze performance and adjusting when needed." ; "Studios: Coordinator must develop and implement a cleaning schedule" ; "Daily and weekly cleaning that includes all floors and equipment." ; "Addressing performance standards when needed and completing their annual reviews." ; "Creating and communicating the Open Studio calendar and the firing schedule to our community at the start of each semester." ; "Ordering all clay and glaze materials as well as any equipment pertaining to the ceramics studio." ; "Coordinate kiln rentals by outside groups." ; "Empty Bowls Event: Managing the collection of ceramic bowls for the event" ; "Recruiting volunteers to staff the event which is held annually in November." ; "Ceramics Sale: Managing the collection of ceramic seconds year-round to contribute to our sale held annually in April." ; "Leading the orientation of all Teaching Artists and Volunteer Monitors that are new to the studios." ; "Communicating guidelines for safety and best practices to the entire community." ; "Communication with students through a semester newsletter and any flyers and notices posted on community boards"
- Software: "use of Microsoft office suite" (required knowledge area, per tool summary).
- Outputs: Open Studio calendar; firing schedule (each semester); cleaning schedule; technicians' annual reviews; volunteer-monitor schedule; semester newsletter; orders for clay, glaze materials and equipment.
- Frequencies: electric kilns "daily"; Cone 10 gas kiln "bi weekly"; cleaning "Daily and weekly"; calendar and firing schedule "at the start of each semester"; Empty Bowls "annually in November"; sale "annually in April"; technician reviews annual.
- Codes: D01 D02 D03 D05 D07(orientation, safety guidelines to community) D08 D12 D13 D14 D16 D18(sale of seconds) D22(kiln rentals, open studio calendar)

## Appendix B: opened but excluded, and leads that could not be verified

### B1. Opened and read, but out of scope (not counted)

Some of these have useful verbatim lines. They are kept separate so they cannot inflate the corpus counts.

| Posting | Why excluded | Useful verbatim lines |
|---|---|---|
| The Potters' Studio, Berkeley CA. Ceramics Studio Technician, 2022-02-28. https://www.ceramicsnow.org/jobs/ceramics-studio-technician-at-the-potters-studio/ | Technician; "reports to the Operations Manager"; "The Studio Tech is not responsible for repairing kilns and other equipment." | "The studio uses lots of paper forms to track things like clay inventory, firing, orientations etc." ; "Keep track of fired goods throughout the studio, rotating them onto the Rack of Doom" "and discarding uncollected wares after 60 days." ; "Help contract firers measure their work and fill out Firing Submission Forms." ; "Ring up customer sales (clay sales, firing fees, day-passes, etc.) on the register (POS)." ; "Basic computer skills – MS Office, Google Docs, ability to use POS systems." |
| Greenwich House Inc, New York NY. Pottery Technician & Program Assistant, live, undated. https://www.simplyhired.com/job/E3KuWSY_F4FvwfYNUXw0qFHe0XO_IT4BtoY6oYisfQNci5AjnU4QZA | Technician and assistant ("Assist Operations Manager and/or Studio and Program Coordinator") | "Process registrations, payments, and tool sales via POS systems (Shopify and Stripe)" ; "Load, unload, and monitor kilns; document firings and report malfunctions" |
| Rain City Clay, Seattle WA. Studio Technical Assistant, 2023-03-28. https://depts.washington.edu/advis104/2023/03/28/rain-city-clay-is-hiring/ | Assistant; "works directly with the Studio Manager" | "primarily responsible for the studio flow, processing artwork through kilns, mixing glazes," |
| Muckenthaler Cultural Center Foundation, Orange CA. Ceramic Studio Technician, expires 2026-12-31. https://arts.ca.gov/?p=68162 | Technician, independent contractor | "Maintain accurate firing logs, material records, and equipment maintenance schedules." ; "Basic computer skills for inventory tracking and record keeping and kiln firing log." |
| Fox Valley Clay, Aurora IL. Ceramic Studio Technician -- Kiln & Studio Operations, ~Oct 2026. https://www.simplyhired.com/job/14lF93RWkAsddhcsS3LKzGQBljl8OTo5z0BSlBDvyLcaFxxxzMrJ_g | Technician; no management duties | "Assist customers with clay purchases, work-in-process storage, finished piece pick-up, and cleanup procedures." |
| University of Louisville, KY. Administrative Assistant – Ceramics Studio, 2024-09-24. https://www.ceramicsnow.org/jobs/administrative-assistant-ceramics-studio-at-the-university-of-louisville/ | Technician-type duties, no management | "Mixing clay from scratch weekly" ; "Maintaining a thorough and accurate materials inventory" ; "Proficient in Microsoft Excel" |
| Lansdowne Economic Development Corp (Utility Works), Lansdowne PA. Ceramic Co-Operator (Part-Time), 2026-03-11. https://portal.philaculture.org/node/74663 | Unpaid work-exchange ($0.00; "Free kiln firings", studio space). Covered by other researchers | "Dedicate 6-8 hours to help maintain the studio." ; "Monitor during Open Studio." |
| Crock a Doodle Calgary SW, AB, Canada. Studio Leader, live, undated. https://www.simplyhired.ca/job/77ZVw6Sm5Y1bZKPzzD_36DKTU-dMFN7rm8KiB7WzAh0Zy8M7GhL0dg | Front-line PYOP role, no supervision stated ($15–$17/h) | "Processing pottery and assisting with glazing, kiln preparation, loading, and firing procedures" |
| Unnamed "CERAMIC STUDIO", Los Angeles CA. Full-Time Tech/Maintenance, 2026-09-24. https://www.craigslist.org/view/d/los-angeles-ceramic-studio-full-time/cpu4Krk28BZnBswx4JVeZD | Technician | "comfortable using Slack, email, and photo sharing with owners and staff." |
| The Artist Lab School of Art, Irvine CA. Art Studio Manager. https://www.simplyhired.com/job/hnImWrz1QbIcbjuPBBy-6LV-4-f2W6Nuyjshe-hCY0YzGLLF0tY7og | Not a ceramics studio (fine and digital art) | – |
| County Hall Pottery, London. Potter in Residence, 2025. https://artdeadline.com/?p=21777 | Residency, not management | – |

Seen only as aggregator cards, not opened in full, and out of scope: Color Me Mine associate and team-member roles (Santa Monica, Long Beach, Minneapolis; Gusto Upper West Side), Polka Dot Pottery sales associate, Klaystation assistants, the MIY Ceramics studio assistant, and Toronto and Saanich municipal pottery technicians.

### B2. UNVERIFIED leads (search snippet only; the page could not be opened; not counted)

Each is listed with what the search snippet said, which may be inaccurate, and why it could not be opened.

| Lead | Snippet claims (UNVERIFIED) | Why not opened |
|---|---|---|
| Claytek Studios, Vancouver BC. Pottery Studio Manager/Tech (allianceforarts.com, 2026-03-27) | FT 40 h, "$22 - $30hr", "minimum 2 years of pottery studio experience", deadline 2026-04-01; "scheduling, organizing studio space, inventory, equipment/supply orders and overall studio upkeep" | 404 |
| The Art Department, Tsawwassen BC. Studio Lead/Tech (allianceforarts.com, 2025-06-13) | new studio; firing, scheduling, inventory, member inquiries | 404 |
| Pottery Northwest, Seattle. Assistant Studio Manager | supervises studio techs, runs kiln firing schedules; open studio "seven days a week from 10am to 9pm" | 404 |
| Seward Park Clay Studio, Seattle. Assistant Studio Manager (Artist Trust) | "$58,000–66,000 per year", 32 h, deadline 2024-03-01 | 404 |
| Phipps Center for the Arts, Hudson WI. Ceramics Studio Manager | "$10,400 a year for about 10 hours a week", teaching $32/h; develops quarterly class schedules | 404 |
| Workhouse Arts Center, Lorton VA. Ceramic Studio Manager | reports to Director of Glass and Ceramics; gas kiln scheduling, inventory, collecting fees from Open Studio participants | 404 (AMP copy also 404) |
| Waupaca Arts Hub, WI. Pottery Studio Manager (UWSP job blog) | $20/h, 6–10 h/week, deadline 2025-11-17 | page removed |
| City of Farmington Hills MI. Arts Studios Area Manager (Hawk Art Studios), April 2026 PDF | $23/h, ~25 h/week; glaze mixing, firing, kiln work | PDF text could not be decoded |
| Ann Arbor Art Center MI. Ceramics Studio Manager, May 2026 PDF | FT; reports to Director of Education; supervises the Ceramics Studio Tech; helps hire and train instructors; kiln loading | PDF could not be decoded; no HTML copy found |
| Clay Art Center, Port Chester NY. Studio Manager (2020 PDF and artaxis copies) | $32,000–36,000, health insurance, free firings | PDF could not be decoded; artaxis.org 403 |
| Clayworks, Charlotte NC. Studio Manager (artaxis) | FT, reports to ED; kiln operation, inventory, staff supervision | artaxis.org 403 |
| Creative Arts Workshop, New Haven CT. Studio Manager (Jan 2022 PDF) | 10–20 h/week; equipment maintenance; clay and glaze inventory; firings coordinated with a studio potters' membership group | PDF could not be decoded |
| Plains Art Museum, Fargo ND. Clay Studio Coordinator part time (June 2025 PDF) | 20+ h/week; open-studio members; kiln loading; supply levels | PDF could not be decoded; not on current jobs page |
| Hearts Afire Pottery, Missoula MT. Studio Manager/Program Coordinator (idahoworks.gov) | PYOP; "$21-$23 per hour", 30–35 h/week | 401 |
| The Potters' Studio, Berkeley CA. Director of Studio Operations & Facilities (Idealist) | $80,000–85,000; supervises ~7 technicians plus work-trade members | 404 (en, es and pt URLs) |
| Lexington Arts and Crafts Society MA. Ceramics Studio Manager (Idealist) | – | 404 |
| Made in Stirling, Scotland. Studio Manager (Craft Scotland) | £28,600, 37.5 h, 18-month contract, closing 2026-07-17; operations, programme delivery, partnerships, memberships | listing page no longer shows it |
| Warwickshire (B95), UK. Pottery Studio Manager & Teacher (Crafts Council) | £30,000–40,000, 20–40 h/week, closing 2026-01-06; "managing membership access & teaching scheduling and ordering supplies" | craftscouncil.org.uk 403 |
| Studio Pottery London. Lead technician (Crafts Council, 2024) | "Lead Managerial Technician Role" | 403 |
| British Ceramics Biennial, Stoke. Studio Officer (Feb 2025 .docx) | reports to a Studio & Community Programme Manager; day-to-day studio operations | .docx not opened; no HTML jobs page found |
| Inkwareny Artists, remote Aboriginal art centre, Australia. Studio Coordinator (Ceramics) (redhotarts.com.au) | "$75-$85,000pa" plus super | 404 |
| District of Saanich BC. Ceramics Studio Technician (Team Leader) | runs day-to-day studio operations; trains instructors and volunteers | 404 |
| Maryland Institute College of Art. Ceramics Studio Manager; Chastain Arts Center (Atlanta) project coordinator (artaxis) | – | artaxis.org 403 |
| Guldagergaard, Denmark. Ceramics Studio Manager (2023) | – | outside the target countries; not pursued |

## Hypothesis evidence

Ratings use the brief's scale: strong, moderate, weak, none, contradicts. n = 39 throughout, and "recent" means the 28 postings from 2023–2026 or live when accessed.

**1. Inventory tracking: STRONG as a duty, WEAK as a software expectation.**
- For: D05 (inventory, ordering, purchasing, receiving) is in 30 of 39 postings (77%) and 22 of 28 recent ones. The word "inventory" itself appears in 19 of 39. Inventory figures in every segment: NPO 18/22, UNIV 5/6, PYOP 4/5, PROD 1/2, COMM 1/3, MUNI 1/1. Examples: P26 "Purchase and organize supplies, maintain inventory, receive shipments and manage purchase orders." ; P24 "Responsible for tracking materials use, ordering, receiving and storing clay materials, kiln repair supplies;" ; P12 "Manages and maintains adequate stock levels and inventory SKUs pricing".
- Against, or limiting: only 3 of 39 mention inventory software or systems (P10, P20, P21), and the two clearest are production potteries (NetSuite in P21). Purchasing often isn't the manager's alone. It needs another person's approval or help in 3 postings (P02 "Submit supply orders to the FPM Director for approval."; P14 "Assist Education Coordinator with purchasing and coordinating purchasing."; P38 "Work with Program Manager regarding ordering materials or supplies"), and is capped by a budget in 1 (P03 "within budget limits"). In 1 posting the manager is the approver (P37). So an inventory feature should include purchase requests and approval, not just stock counts. Usage tracking also appears (P10 "Orders and tracks clay and sculpture material usage."; P24).

**2. Asset and equipment tracking: MODERATE.**
- For: D02 (equipment maintenance and repair) is in 27 of 39 (69%) and 22 of 28 recent. Coordinating repairs (reporting them, getting estimates or approval, working with vendors, owners or facilities staff) appears in 8 (P02 P03 P04 P10 P12 P19 P22 P35). Kiln-specific upkeep is spelled out, e.g. P39 "Regular kiln and furniture maintenance like changing elements and grinding shelves."; P19 "manage kiln shelf care" "(cleaning, grinding, kiln wash application)".
- Limiting: an actual asset register, count or documented inspection (D20) appears in only 6 of 39: P10 equipment inventory, P12 "FF&E ... count schedules", P14 "equipment assessments", P20 mould inventory, P26 "Perform and document routine inspections", P34. Maintenance schedules are named in 2 (P14, P26). Outside the corpus, the Muckenthaler technician posting names "equipment maintenance schedules". Postings describe maintenance as work to be done, not as records to be kept.

**3. Sales integrations with several payment providers (Stripe, Square and others): WEAK. For Stripe and Square specifically: NONE.**
- For: D11 (retail and point of sale) is in 14 of 39 (36%) and 11 of 28 recent. Sales take several forms: clay and kiln-space sales (P04 "Work with the front desk on sales of clay and kiln space for specific member based firings"), the studio store (P19), gallery, consignment and seconds sales (P15 P16 P35 P39), kiln rentals and materials invoicing (P25), and PYOP walk-in sales (P18 "ring up sales"). Payment collection is explicit in P32 ("Collects fees, payments, and memberships.") and P28 ("Preparing deposits (cash, checks, PayPal)").
- Against: no posting names Stripe, Square or Shopify (0 of 39). Payment rails named: PayPal 2 (P25 in 2023, P28 in 2020) and one unnamed "POS program" (P01). The segment where point of sale matters most, PYOP, names no software at all (0 of 5). The only posting that names Shopify and Stripe is an excluded technician and assistant role (Greenwich House: "Process registrations, payments, and tool sales via POS systems (Shopify and Stripe)"). That suggests payment tools sit with front-desk and assistant staff, and that managers don't select them. Only one posting (P28, 2020) shows more than one payment channel being reconciled. The evidence supports "the studio takes payments", not "the manager needs integrations with several providers".

**4. QuickBooks Online integration: WEAK, close to NONE for recent postings.**
- For: P28 (Pottery Northwest Operations Coordinator, 2020): "Tracking transactions in Quickbooks", "Accounts payable/receivable", "End of month and year reconciliations", "Running payroll", and desired "Quickbooks or accounting software".
- Against: QuickBooks is named in 1 of 39, and in 0 of 28 recent postings. Xero 0. Bookkeeping-level work is in only 6 of 39 (P07 P22 P25 P26 P28 P29). Budget work (13 of 39) is twice as common: "Help prepare departmental budgets." (P14); "Track spending to budget throughout the year, track revenue goals" (P35). Several postings place finance with someone else: P07 "You are supported by a financial account manager"; P25 "in accordance with FOMS financial policies"; P11 budget "with oversight from the College Program Administrator"; P35 budget "Working closely with Manager of Visual Arts". The studio manager is usually a budget holder, not the bookkeeper. A QBO export may help the owner or finance staff, but the postings give little reason to make it a first-phase feature built for the manager.

**5. Scheduling: STRONG, but it is four different scheduling jobs.**
- For: 32 of 39 (82%) carry at least one scheduling duty, as do 24 of 28 recent postings. Broken down:
  - Class, programme and open-studio calendars (D08): 23 of 39. P39 "Creating and communicating the Open Studio calendar and the firing schedule to our community at the start of each semester."
  - Firing schedules or kiln logs: 11 of 39. P02 "Load and unload kilns to maintain efficient firing schedules." ; P22 "Manage kiln-firing schedule".
  - Staff, volunteer and student-worker rosters: 8 of 39. P12 "Schedules employees and prepares employees' roster" ; P39 "Managing a schedule of Volunteer Monitors who oversee Open Studio hours."
  - Bookings, sign-ups and rentals of shared resources (D22): 8 of 39. P24 "Maintains online sign-up sites for electric kilns, gas kilns, and spray booths" ; P25 "Manage kiln rental bookings" ; P19 "Manage student open studio sign-ups, shelf assignments and the shared studio calendar."
- Limiting: scheduling software is named only 5 times (Google Calendar P01; WebCheckout P10; unnamed "sign-in software" P11; unnamed "online sign-up sites" P24; unnamed "scheduling software for studio access and virtual classes" P28). Mindbody, Acuity and other class-booking tools appear 0 times. The rhythm is set by sessions and semesters (P01, P09, P39), not by individual appointments. Firing cadence is stated in only 1 posting (P39: electric kilns "daily", Cone 10 gas kiln "bi weekly").

## Other strong signals

- **The manager does the physical work.** Kiln operations appear in 24 of 39 postings, cleaning and safety in 32, maintenance in 27. Even full-time manager titles carry hands-on firing, e.g. P04 "Load and fire electric, soda, and gas kilns; perform regular raku firings". 4 of 39 manager or coordinator titles are effectively technician roles (HYBRID-TECH). Implication (an inference, not something the postings state): software for this role should be usable in short bursts on a phone or a shared studio device.
- **Much of the team is volunteers, students and interns.** 14 of 39 supervise volunteers, work-study students, interns or residents. Volunteer "monitors" run open studio hours (P22, P39; "monitors" also in P37). Paid teams are small where a headcount is given: P14 has a "studio technician team (2)", P39 "2-3 part-time Ceramics Technicians", and the largest is P10's "10-12 student workers". Rosters, orientations, certification ("tracks those certified to fire kilns", P24) and simple task hand-offs matter more than HR software.
- **Piece tracking is rarely a named duty, but it is central where it appears.** Only 7 of 39 code D06. Those 7 describe pickup deadlines and shelf assignment: P30 "Track every customer-painted piece so nothing misses its pick-up date"; P09 "keep track of student roster for each session to organize student shelf space"; P19 "shelf assignments"; P37 "facilitate student pickup of completed art". The excluded Potters' Studio technician posting describes the same work on paper ("lots of paper forms"; uncollected work discarded "after 60 days"). This fits [01](../../01-market-survey.md)'s ranking of piece tracking as the top pain: employers file it under firing and pickup, not under its own heading.
- **Postings barely name software.** 23 of 39 name no product. The rest are mostly office suites (Microsoft 12, Google 9). Pottery-specific tools appear twice (KilnFire, HyperGlaze). This matches [03](../../03-tools-in-use.md)'s finding that studios run general tools and paper. Beyond basic office skills, postings rarely ask for software skills, so a phase-1 product shouldn't assume a trained operator (an inference).
- **Safety and compliance paperwork is a real duty, especially in universities and larger nonprofits.** Explicit hazmat, SDS or silica language appears in 7 of 39 (P03 glaze safety labelling, P10, P19, P24, P27, P35, P36). Safety documents are a named output in 6 (SDS catalogue, SOP sheets, quarterly safety audit, accident reports).
- **Events and fundraising take real time in nonprofits.** D14 is in 20 of 39 (NPO 11/22): Empty Bowls (P17, P39), Chili Bowl (P13), auction ware (P23), seconds sales (P39 "collection of ceramic seconds year-round"), and show-and-sales (P24, twice a year). These events involve collecting donated work, recruiting volunteers and selling pieces. That is an inventory-and-sales need with no payment-integration angle.
- **Budget holder, not bookkeeper.** Budgets appear in 13 postings and bookkeeping in 6. Reports appear in 9 (grant reports, an annual work plan with metrics, P&Ls, board materials, "basic business reports" in P29).
- **"Stands in for the owner" fits PYOP and small businesses, not most of the corpus.** 11 of 39 report to a programme or education director and none to a board. Only the two PYOP postings imply the owner or founder as the boss. P29 is the clearest statement of the stand-in case: "a manager who can turn priorities into consistent execution" and "helping a small business become less owner-dependent".
- **Production potteries are a different product.** Both production postings (P20, P21) centre on work orders, WIP, yield loss, mould inventory and weekly production planning, and one uses NetSuite. Neither mentions members or classes.
- **Pay is low and the job is often part-time.** 14 of 39 are part-time with a median of 20 h/week. The hourly median is $22. Full-time salaried roles have a median midpoint of $52,135 (n=10). 10 of 39 offer studio access or free classes as a benefit. Implication (an inference): the people doing this work are paid little and work part-time, which is consistent with the free-software premise.

## Gaps and limits

- **Corpus size and coverage.** 39 distinct postings against a target of 50. Discovery hit a shared WebSearch budget (200 calls per session, shared by every agent) that ran out three times during this run. The tool also couldn't reach the Wayback Machine ("unable to fetch from web.archive.org") or Reddit. With no archive access, most expired postings could not be recovered: 25 leads, in 24 rows, are listed as UNVERIFIED in Appendix B2.
- **Geography is almost all US.** 37 US, 1 UK (2022), 1 Canada (2023), 0 Australia, 0 New Zealand. The UK, Australian and New Zealand job boards either blocked fetches (Crafts Council 403, Seek 403, ArtsHub 403) or had no current ceramics manager listings (Arts Jobs UK, CharityJob, The Big Idea NZ, Job Bank Canada, Craigslist Toronto and Vancouver). No country comparison is possible.
- **Segment imbalance.** 22 of 39 are nonprofit art centres, because the archives that keep old postings (CeramicsNow, the UW art advising blog, the Philadelphia cultural job bank) are nonprofit-oriented. PYOP has only 5 postings, production 2 and municipal 1. Cooperatives, makerspaces and pure teaching studios have 0. Live listings (SimplyHired, LinkedIn, Craigslist) lean the other way, towards PYOP and small businesses.
- **Blocked or failed sources:** Indeed, ZipRecruiter, CareerBuilder, Monster, Adzuna, Jora (403); artaxis.org (403, including its RSS feed); Crafts Council UK (403); idahoworks.gov (401); LinkedIn (429 after 2–4 requests); careers.collegeart.org (certificate error); classifieds.nyfa.org (DNS failure); cfileonline.org (expired certificate); Reddit and web.archive.org (blocked by the tool); Idealist (search needs JavaScript, and expired postings return 404). Jooble cards redirect to ZipRecruiter (403).
- **PDFs.** WebFetch could not decode compressed PDFs from Ann Arbor Art Center, Clay Art Center, Creative Arts Workshop, Plains Art Museum, McLean Project for the Arts and Farmington Hills. The McLean posting was recovered from HTML copies (P19). The rest are in B2.
- **Processed text.** WebFetch returns a processed view. Quotes here are what it returned as verbatim, some long bullets were split into pieces, and typographic quotes are normalised. Two postings were partly summarised by the tool: P10 (later recovered in full via an alternate URL) and P16 (codes limited to verbatim lines; D07, D08 and D10 not counted). P12 and P13 are Handshake previews that may omit part of the posting.
- **Dates.** 6 postings are undated but were live on 2026-10-20 (P02 P13 P30 P32 P35 P37). 6 rely on an aggregator's relative date (~Sep–Oct 2026: P12 P18 P20 P29 P31 P33). 11 are PRE-2023 and labelled; the oldest is 2016 (P17). The recent-only (n=28) figures are given alongside the totals so older postings can be discounted.
- **Source type (primary vs repost).** 4 postings come from employers' own sites (P02 P23 P37 P38). 7 come from job boards or applicant-tracking systems where the employer posts directly: Handshake P12 P13, LinkedIn P20, Betterteam P18, Craigslist P36, and the PhilaCulture job bank P03 P39. 20 are third-party reposts: CeramicsNow 14, the UW art advising blog 5, Associated Artists of Pittsburgh 1. 8 are aggregator copies (SimplyHired). Reposts and aggregators reproduce the employer's text but may trim it, so missing fields (pay, reporting line) may be absent from the copy rather than the original.
- **Reposts.** P19 (McLean) appears as two versions, March 2026 and a later live repost with different pay. It is counted once, with the latest as primary. P12 appears on Handshake and as a Jooble card, counted once. P23 and P38 are the same employer but different roles and years, so both are counted.
- **Coding.** A single coder with no inter-rater check. D07 is broad (rules, orientations, access), so the stricter D07a (members named, 9 of 39) is also reported. Weak codes are flagged in Appendix A. HYBRID-TECH postings (4) are included, and any count can be recomputed without them from the table.
- **What postings can't show.** A posting lists intended duties, not time actually spent, and not the tools actually used. Software left unnamed in a posting doesn't mean it isn't used; the tool census in 03 is the better evidence for that. The only time-share data are the employer-stated percentages in P10, P16, P24, P27 and P36.
- **Hypothesis evidence is indirect.** Postings test whether a duty exists. They can't test whether owners would adopt a particular integration. The finding of 0 of 39 for Stripe and Square is evidence that managers aren't hired to run those systems, not proof that studios don't use them.

