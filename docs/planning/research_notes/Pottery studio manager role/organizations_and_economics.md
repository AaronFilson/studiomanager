# Organizations and economics: who runs pottery studios

_Working notes. Researched 2026-10-20. Status: complete for what was reachable; see "Gaps and limits". Countries: US first, then NZ and UK; Canada and Australia mostly not reached._

## TL;DR

- **Three different "managers".** (1) Nonprofit clay centres: a paid Executive Director (median $110,235 across 9 centres), 7-38 W-2 staff, mostly part-time or contract teachers, and technical or facility managers below the Form 990 reporting threshold. (2) Owner-run and founder-run studios: the owner is the manager and reports overwork (5 of 6 podcast owners and managers). (3) Co-ops and guilds: **no manager at all**. Work is split among volunteer chairs (kiln, purchasing, maintenance, classes, membership, treasurer), and 4 of 5 US guilds and 8 of 13 NZ clubs have no paid staff.
- **Classes pay the bills in teaching centres; dues pay the rent in co-ops.** Tuition is the largest program-revenue line in 15 of 23 US organizations, and 56-100% of program revenue where itemized. Membership dues are 99% of revenue at a pure co-op and 57% at a membership studio. 18 of 23 sell physical goods. At 10 of the 12 that report inventory sales, those sales are at most 17% of revenue; the two exceptions are a production pottery and a clay-supply business. Consigned artwork is bigger at one art centre (NCC, 23% of revenue).
- **Demand rose after 2020.** Earned revenue is up 27-103% on 2019 at 7 of 8 US clay centres; income is up 84-213% at 3 NZ clubs; UK "pottery" company formations rose from 27 (2019) to 59 (2025). Broad statistical counts: 16,299 US fine-arts-school establishments (2025, +13.7% on 2019, mostly not pottery); 2,320 UK cultural-education enterprises (2026, +48%).
- **Hypotheses:** scheduling **strong** (classes); inventory **moderate** (goods are sold) but weak as a pain; asset and equipment tracking **weak** (equipment is billed and booked, not tracked); QuickBooks **weak** (indirect only: treasurers log the most volunteer hours); multiple payment providers **none** in this evidence. A strong signal outside the list: **recurring membership billing**.
- **Biggest gaps:** for-profit economics (no numeric owner statements found), PYOP location counts, press accounts of the boom, and closure news with reasons. The web search budget ran out early and several sources blocked WebFetch.

## Plan

1. Nonprofit filings (US): ProPublica Nonprofit Explorer API (search + organization JSON) and full-filing HTML views for Part I lines 5/6 and Part VII. Target 20+ clay/ceramics organizations. Confirm EIN and nonprofit status; drop non-matches.
2. UK / Canada / Australia / NZ charities: Charity Commission register, CRA charity listings, ACNC register, NZ Charities Services (if fetchable).
3. For-profit studios: owner interviews, podcasts, business press 2023-2026 (revenue mix, member counts and prices, staffing, rent, margins). Avoid the 84 studio websites in 03-tools-in-use.md (another researcher is auditing them).
4. Market counts: US Census CBP + Nonemployer Statistics (NAICS 611610 and others; broader than pottery), ONS / Nomis UK Business Counts, Crafts Council, ABS, StatCan. PYOP franchise location counts.
5. Growth accounts: pandemic boom, Great Pottery Throw Down, waitlists.
6. Who manages, staff-per-member ratios, models with no manager.
7. Workload, burnout, closures and ownership changes 2023-2026.
8. Closing sections: Hypothesis evidence, Other strong signals, Gaps and limits.

Conventions: every data point tagged [country] [segment]. Segments: membership/community studio, teaching studio, cooperative, nonprofit art centre, PYOP, production pottery, university/college/school, makerspace, municipal/recreation. "UNVERIFIED" = could not open the source myself. Vendor/franchisor marketing labelled as such.

## Findings log

### A. US nonprofit filings (Form 990)

**Method.** EINs found with the ProPublica Nonprofit Explorer search API (`/api/v2/search.json?q=...`). The latest filing's object ID was read from the ProPublica organization page (`/nonprofits/organizations/{EIN}`). Line items were then read from the IRS e-file XML for that object ID in the GivingTuesday 990 data lake (`https://gt990datalake-rawdata.s3.amazonaws.com/EfileData/XmlFiles/{object_id}_public.xml`, the source URL that GivingTuesday's own index API `990-infrastructure.gtdata.org/irs-data/efilexml?ein=` returns). ProPublica's own XML and PDF downloads returned 403 to WebFetch; its "full filing" view renders by JavaScript and showed nothing; the IRS PDF copy (apps.irs.gov/pub/epostcard/cor/...) is a scanned image with no text layer. Field names: Part I line 5 = `TotalEmployeeCnt` (W-3 headcount for the calendar year, includes part-time and seasonal staff, so it is a headcount, not FTE); line 6 = `TotalVolunteersCnt` (an estimate the filer makes, often loosely); Part VIII line 2 = `ProgramServiceRevenueGrp` (descriptions are the filer's own words). All accessed 2026-10-20. Segment for all rows below: nonprofit art centre unless noted. Country: US.

**Uncertainty.** Line 5 counts every W-2 recipient in the year, so a studio with 10 part-time teaching artists reports 10 employees; it overstates FTEs. Line 6 volunteer counts are self-estimated and inconsistent between years (Northern Clay Center reported 270 in 2019 and 63 in 2020). Totals that include capital-campaign gifts (The Clay Studio FY2025) distort revenue mix; earned-revenue shares are therefore also given as a share of program-service revenue. Part VII lists every officer and director, but staff only when they are key employees or among the five highest paid over $100,000. **A studio manager or technician paid under $100,000 never appears in Part VII**, so the absence of a "studio manager" title in a 990 says nothing about whether one exists. Membership dues are reported inconsistently: some filers put them in Part VIII line 1b (contributions), others in line 2 (program revenue). Sample bias: most organizations were named in the brief because they are well known, so the sample leans large.

#### A1. Northern Clay Center, Minneapolis MN. EIN 41-1616650. 501(c)(3), ruling 1993-10.
- Source: XML object 202501639349301305 (FY 2024-01-01 to 2024-12-31, Form 990, filed 2025-06-12 per ProPublica). https://gt990datalake-rawdata.s3.amazonaws.com/EfileData/XmlFiles/202501639349301305_public.xml
- Employees (line 5): 25. Volunteers (line 6): 141.
- Total revenue $1,692,225; total expenses $2,107,413 (deficit).
- Contributions and grants $343,987 (government grants $158,399; all other $175,862; membership dues $9,726).
- Program-service revenue $1,187,593: "PROGRAM SERVICES" $802,097; "CONSIGNED SALES - GALLERY" $291,095; "CONSIGNED SALES - APF" $85,595; "CONSIGNED SALES - EXH & ED" $8,806. Gross sales of inventory $56,739; "MISCELLANEOUS" $42,148.
- Other salaries and wages $638,924; occupancy $112,586.
- Part VII: KYLE RUDY-KOHLHEPP, "EXECUTIVE DI", 40.00 h/wk, $93,110. 19 board members listed, 16 at $0; three small payments ($14,650; $1,736; $660; likely artist or teaching fees, not stated).
- Part III (paraphrased by WebFetch, not verbatim): the sales gallery represents 179 ceramic artists.
- Newer filing, FY2025 (Jan-Dec 2025, filed 2026-04-30), ProPublica summary only: total revenue $1,810,961; total expenses $2,232,889; net income -$421,928. Officers: "Kyle Rudy-kohlhepp, Exec Dir Thr" $72,764; "Judy Hawkinson, Executive Di" $42,419 (https://projects.propublica.org/nonprofits/organizations/411616650). CauseIQ (aggregator) gives 26 employees for 2025 and lists staff titles "Business Manager", "Marketing and Technology Manager", "Community Engagement Manager", "Sales Gallery Manager" (https://www.causeiq.com/organizations/northern-clay-center,411616650/).
- Trend (GivingTuesday 990 basic fields API, https://990-infrastructure.gtdata.org/irs-data/990basic120fields?ein=411616650): employees 19 (2010) to 27 (2021); volunteers 63 to 313; total revenue stayed between $1.47M and $2.13M from 2010 to 2022 with no upward trend, and was $1.69M in 2024.

#### A2. The Clay Studio, Philadelphia PA. EIN 23-7380408. 501(c)(3).
- Source: XML 202543509349300309 (FY 2024-06-01 to 2025-05-31, filed 2025-12-16).
- Employees 38. Volunteers 23.
- Total revenue $6,875,649 (inflated by contributions); total expenses $4,314,433.
- Contributions $4,692,910 (government $50,000; all other $4,569,680; fundraising events $73,230).
- Program-service revenue $1,753,853: "TUITION AND LAB FEES" (code 611710) $1,154,741; "SALES" $266,475; "HOST FEES" (453000) $173,030; "ARTIST FEES AND CHARGES" $155,491; "ARTWORK AND WHOLESALE SALES" $4,116. Misc $59,841.
- Other salaries and wages $1,371,553; occupancy $997,617.
- Part VII: one paid person, JENNIFER MARTIN, Executive Director, 39.75 h/wk, $150,362 (+$4,530 other). 23 of 24 listed unpaid.
- Part III counts (quoted fragments as returned): "MORE THAN 600 STUDENTS EACH TERM"; "MORE THAN 40 CERAMIC ARTISTS" in the associates program; Claymobile "MORE THAN 6,000 CHILDREN, YOUTH, AND ADULTS" a year at "200 PARTNER SITES"; "12 FUNDED FELLOWSHIPS".
- Ratio: 38 W-2 staff to 600+ students per term (about 1 per 16 students, headcount basis).

#### A3. Baltimore Clayworks, Baltimore MD. EIN 52-1409133. 501(c)(3).
- Source: XML 202533179349307268 (FY 2024-01-01 to 2024-12-31, filed 2025-11-13).
- Employees 20. Volunteers 127.
- Total revenue $1,501,036; total expenses $1,619,654.
- Contributions $470,569 (government $115,311; other $338,656; events $16,602).
- Program-service revenue $937,791: "CLASSES & WORKSHOPS" (611600) $675,001; "STUDIO & EQUIPMENT REN[TAL]" (532000) $177,766; "GALLERY SALES" $85,024.
- Other salaries $495,728; occupancy $144,876.
- Part VII: MATTHEW HYLECK, Executive Director, 35 h/wk, $89,048. Seven trustees received $68-$9,649 (likely teaching or artist fees); 14 unpaid.
- Part III counts: "2,347 children and adults" in studio classes; "1,072 students at 23 sites" in free community programs; "6 individuals with monthly wheel access"; "108 individuals with monthly community firing access"; "62 residents and visiting artists"; "88 shop artists".
- Ratios: 20 staff : 2,347 class students (1 : 117 per year); the membership-like group (monthly wheel or firing access) is 114 people.
- CauseIQ (aggregator; data date not shown) lists "Cyndi Wish: Executive Director", plus Directors of Finance and Administration, Marketing and Communications, Community Arts and Engagement, and Development; "20 (2024)" employees. https://www.causeiq.com/organizations/baltimore-clayworks,521409133/. Because the FY2024 990 names Matthew Hyleck as Executive Director, this points to a leadership change after 2024. The timing is UNVERIFIED.

#### A4. Mudflat Pottery School (Mudflat Studio), Somerville MA. EIN 04-2505540. 501(c)(3).
- Source: XML 202543099349301719 (FY 2024-01-01 to 2024-12-31, filed 2025-11-05). The FY2025 XML (filed 2026-08-14) was not yet in the data lake (404).
- Employees 31. Volunteers 60.
- Total revenue $1,628,709; total expenses $1,493,192.
- Contributions $249,328 (government $152,717; other $96,611).
- Program-service revenue $1,291,889: "CLASS TUITION" $797,113; "POTTERY SALES" $136,020; "STUDIO FEES" $116,125; "STUDENT MEMBERSHIP" $85,275; "SUPPLIES" $79,669.
- Other salaries $481,717; occupancy $155,409.
- Part VII: LYNN GERVENS, "EXEC. DIRECT", 40 h/wk, $158,534. Five directors received $1,950-$41,144 (largest: LISA KNEBEL $41,144, likely an instructor or staff role; not stated). 7 of 13 unpaid.
- FY2025 (ProPublica summary): revenue $1,588,976; expenses $1,534,271; "Lynn Gervens (Exec. Direct)" $175,370.
- Note the revenue mix: class tuition is 62% of program revenue; firing/studio fees plus "student membership" plus supplies are 22%.
- CauseIQ (aggregator) lists "Lynn Gervens, Executive Director", "Kate Kuligowski, Marketing and Communications Director", "Cary Atsalis, Technical Director" and "Bill Russell, Facility Manager"; "30 (2025)" employees. https://www.causeiq.com/organizations/mudflat-pottery-school,042505540/. This is the ED + technical lead + facility manager model in practice.

#### A5. Clay Art Center, Port Chester NY. EIN 20-4611534. 501(c)(3).
- Source: XML 202601199349302470 (FY 2024-09-01 to 2025-08-31, filed 2026-04-29).
- Employees 18. Volunteers 115.
- Total revenue $1,903,453; total expenses $1,895,801.
- Contributions $683,964 (government $107,500; other $423,228; fundraising events $153,236).
- Program-service revenue $1,160,097: "EDUCATIONAL PROGRAMS" (611610) $958,100; "ARTIST FEES" (713990) $154,937; "EXHIBITIONS" $47,060. "RENTAL INCOME" $62,450 (other revenue).
- Other salaries $598,251; occupancy $199,495.
- Part VII: EMILY PECK, "EXECUTIVE DI", 40 h/wk, $121,474. Five board members $150-$802; 6 unpaid.
- Part III: classes, studio-artist programme with private and shared studios, a residency for "THREE" graduates a year; no counts.

#### A6. Pottery Northwest, Seattle WA. EIN 91-0815956. 501(c)(3).
- Source: XML 202521299349302317 (FY 2024-01-01 to 2024-12-31, filed 2025-05-09). FY2025 XML (filed 2026-05-13) not yet in the data lake (404).
- Employees 8. Volunteers 30.
- Total revenue $1,027,282; total expenses $1,005,897.
- Contributions $530,215 (government $20,000; other $431,452; fundraising events $78,763).
- Program-service revenue $497,128: "Education & Enrollment Fees" (611610) $468,728; "Gallery Space & Events" $28,400. Inventory sales $38,535.
- Part III per-programme figures: community education expenses $336,767, revenue $468,728; gallery expenses $101,031, revenue $28,400; "studio memberships" expenses $235,738, revenue reported as $0 on that line (membership income is not broken out anywhere in this return).
- Other salaries $363,869; occupancy $149,458.
- Part VII: Edwin King, Executive Director, 40 h/wk, $122,590; 12 unpaid.
- FY2025 (ProPublica summary): revenue $2,018,620, expenses $1,087,966 (no breakdown, because the FY2025 XML wasn't available, so the cause of the jump is unknown); "Edwin King, Executive Director" $142,029.

#### A7. Clay Center of New Orleans, LA. EIN 81-5241031. 501(c)(3). Form 990-EZ.
- Source: XML 202541289349200604 (FY 2024-01-01 to 2024-12-31, filed 2025-05-07).
- 990-EZ has no employee or volunteer line. No salaries line present.
- Total revenue $96,045 (contributions $7,262; program-service revenue $80,418; gross sales of inventory $15,721). Total expenses $109,415; occupancy, rent, utilities and maintenance $49,327 (45% of expenses). Net assets negative (-$45,444, ProPublica summary).
- Officers: Michelle Swafford, Board President, 20 h/wk, $0; Vice President 2 h/wk, $0; Secretary Treasurer 1 h/wk, $0.
- Programme text (WebFetch paraphrase): classes, workshops, low-cost kiln firing, open studio, studio rentals, a "Studio Monitor program", free recycled clay.
- Reading: a volunteer-run studio where the board president works 20 hours a week unpaid. This is the "no manager" model inside a nonprofit shell.

#### A8. Kansas City Clay Guild (K C Clay Guild), Kansas City MO. EIN 43-1480279. 501(c)(3). Segment: cooperative / guild (nonprofit).
- Source: XML 202620299349300402 (FY 2025-01-01 to 2025-12-31, filed 2026-01-23).
- Employees 3. Volunteers 55.
- Total revenue $1,212,330; total expenses $1,652,847. Contributions $759,201 (no government; membership dues $53,940; fundraising $10,194). Occupancy $1,136,098 (69% of expenses; a building project year, by inference from the size, not stated).
- Program-service revenue $432,778 in five lines, all described only as "All Other Amusement and Recreation Industries" (713990): $5,466; $193,423; $104,785; $38,786; $43,744. Part III names the programmes as special events, clay classes, "studio and equipment rental" plus clay and tool sales, private parties, and unpaid outreach, so the five lines probably map to those but the return does not say which is which.
- Other salaries $249,167.
- Part VII: no executive director. Officers are volunteer chairs: PRESIDENT 5 h/wk; TREASURER 12 h/wk; GALLERY CHAIR; MEMBERSHIP CHAIR; PUBLICITY CHAIR; WORKSHOP CHAIR; EDUCATIONAL CHAIR; VOLUNTEER CHAIR; SECRETARY. Three received pay ($14,950; $6,663; $1,175), most likely as instructors (not stated).
- Part III counts: classes for "OVER 300 INDIVIDUALS" (adults) and "OVER 50 CHILDREN"; "THREE CERAMIC ARTISTS IN RESIDENCE"; raku event "MORE THAN 200 PEOPLE"; two sales with "MORE THAN 40" artists each.

#### A9. Worcester Center for Crafts, Worcester MA. EIN 04-2105859. 501(c)(3). Segment: nonprofit art centre (multi-craft) affiliated with a university.
- Source: XML 202521339349305282 (FY 2023-07-01 to 2024-06-30, filed 2025-05-13). FY2025 XML not yet in the data lake.
- Employees 33. Volunteers 30.
- Total revenue $1,159,504; total expenses $1,126,109. Contributions $209,905.
- Program-service revenue $687,827: "TUITION & FEES" $611,879; "CRAFT FAIRS" $75,948. Gross sales of inventory $147,528.
- Other salaries $518,421; occupancy $128,968.
- Part VII: ELAINE MCKENNA-YEAW, EXECUTIVE DIRECTOR, 40 h/wk, $0 from the organization and $97,348 from a related organization. Schedule O: "ANY PAYROLL LISTED FOR BOARD MEMBERS IS FOR THEIR EMPLOYMENT WITH WORCESTER STATE UNIVERSITY". Schedule R lists Worcester State University as related, with shared paid employees.
- Reading: the director is a university employee; the board is largely university officers. Multi-craft (not clay only).

#### A10. Clayworks Inc, Charlotte NC. EIN 90-0198258. 501(c)(3).
- Source: XML 202503029349302275 (FY 2024-07-01 to 2025-06-30, filed 2025-10-29).
- Employees 7. Volunteers 53.
- Total revenue $1,127,901; total expenses $931,350. Contributions $255,498.
- Program-service revenue $838,040, one line: "TUITION AND EDUCATION SERVICES". Other income $10,654.
- Other salaries $155,089; occupancy $56,325.
- Part VII: ADRIENNE DELLINGER, EXECUTIVE DIRECTOR, 40 h/wk, $110,235; 15 of 16 unpaid.
- Reading: 74% of revenue is tuition with only 7 W-2 staff. Instructors are probably contractors (other salaries are only $155,089), but the return does not say.

#### A11. Archie Bray Foundation, Helena MT. EIN 81-0284022. 501(c)(3). Segment: nonprofit art centre (residency) with a clay-supply business.
- Source: XML 202503169349309205 (FY 2024-01-01 to 2024-12-31, filed 2025-11-12).
- Employees 54. Volunteers 45.
- Total revenue $2,113,313; total expenses $2,727,567. Contributions $1,340,820.
- Program-service revenue $134,874 ("PROGRAM FEES"). Gross sales of inventory $1,341,598, cost of goods $805,016 (the Bray runs a ceramic supply business; the return lists it as inventory sales).
- Other salaries $1,202,106; occupancy $192,696.
- Part VII: Rebecca Harvey, "Executive Dir.", 60 h/wk, $125,768; 22 of 23 unpaid.
- Part III: "TEN LONG-TERM RESIDENCIES THAT LAST FOR UP TO TWO YEARS, TEN SUMMER RESIDENCIES, AND 6-8 SHORT-TERM RESIDENCIES".

#### A12. Watershed Center for the Ceramic Arts, Edgecomb ME. EIN 01-0427824. 501(c)(3). Segment: nonprofit art centre (residency).
- Source: XML 202512309349300411 (FY 2024-01-01 to 2024-12-31, filed 2025-08-18).
- Employees 16. Volunteers 50.
- Total revenue $1,106,625; total expenses $1,046,797. Contributions $753,972 (government $35,000).
- Program-service revenue $226,409 ("REGISTRATION FEES"). Inventory $14,964.
- Other salaries $285,678; occupancy $54,854.
- Part VII: DAVID S EAST, "EXECUTIVE DI", 40 h/wk, $54,712; SADIE BLISS, "INTERIM EXEC", 24 h/wk, $43,119 (a leadership change within the year). 19 of 21 unpaid.
- Part III: "~ 18 ARTISTS-IN-RESIDENCE" (summer); "12 CERAMIC ARTISTS OF COLOR" (fall); workshops and kiln rental to local artists.

#### A13. Arrowmont School of Arts and Crafts, Gatlinburg TN. EIN 58-2007394. 501(c)(3). Segment: nonprofit craft school (multi-media; included as requested, not clay-only).
- Source: XML 202512769349301001 (FY 2024-01-01 to 2024-12-31, filed 2025-10-03).
- Employees 63. Volunteers 10.
- Total revenue $8,749,560; total expenses $6,868,818. Contributions $2,379,772 (government $699,676).
- Program-service revenue $2,013,273: "STUDENT HOUSING AND BOARD" $1,139,734; "TUITION AND WORKSHOPS" $873,539. Inventory $138,705.
- Other salaries $2,149,748; occupancy $379,773.
- Part VII (ProPublica page): Trudy Hughes, Chief Executive Officer, $175,935; Chief Officer of Institutional Advancement $126,101; Chief Financial Officer $125,965; Chief Officer of People and Culture $116,361.
- Part III: "MORE THAN 150 CLASSES".

#### A14. Penland School of Craft, Penland NC. EIN 56-0623948. 501(c)(3). Segment: nonprofit craft school (multi-media).
- Source: XML 202610699349300806 (FY 2024-05-01 to 2025-04-30, filed 2026-03-10).
- Employees 143. Volunteers 220.
- Total revenue $10,699,059; total expenses $10,797,378. Contributions $4,879,434.
- Program-service revenue $3,096,488: "TUITION, ROOM/BOARD, FEES" $2,734,015; "GALLERY" $362,473. Inventory $265,315.
- Other salaries $3,363,063; occupancy $392,079.
- Part VII (ProPublica page): Maria Hall, Executive Director, $200,158; Director of Finance $123,507; Creative Director $121,262; "Director Of Facilities And Grounds" $102,361.
- Part III: "PENLAND SERVED 1,500 STUDENTS" (FY25 adult workshops) "IN 16 STUDIOS"; "16,000 VISITORS".

#### A15. Houston Center for Contemporary Craft, Houston TX. EIN 76-0621817. 501(c)(3). Segment: craft museum with studio-artist residencies (multi-media).
- Source: XML 202640639349301704 (FY 2024-07-01 to 2025-06-30, filed 2026-03-04).
- Employees 19. Volunteers 45.
- Total revenue $2,264,363; total expenses $2,335,585. Contributions $2,182,715 (96% of revenue).
- Program-service revenue $56,897: "WORKSHOP FEES" $42,147; "EXHIBIT FEES/OTHER" $14,750. Inventory $64,877.
- Other salaries $719,059; occupancy $727,294.
- Part VII: LEILA CARTIER, EXECUTIVE DIRECTOR, 40 h/wk, $135,185.
- Part III: "COMPETITIVE AWARDS TO UP TO 10 ARTIST PER YEAR" for "6 TO 12 MONTH RESIDENCIES". Not a membership or teaching studio; a museum. Weak fit for the product.

#### A16. Pewabic Society (Pewabic Pottery), Detroit MI. EIN 38-2277840. 501(c)(3). Segment: production pottery run by a nonprofit, plus education.
- Source: XML 202512049349300736 (FY 2023-10-01 to 2024-09-30, filed 2025-07-23). FY2025 XML (filed 2026-07-24) not yet in the data lake.
- Employees 70. Volunteers 15.
- Total revenue $2,605,735 (as reported; this figure nets cost of goods); total expenses $2,592,760. Contributions $484,434.
- Program-service revenue $290,151 ("EDUCATIONAL"). Gross sales of inventory $3,392,959; cost of goods sold $1,682,604. Tile and pottery sales are the main business.
- Other salaries $1,227,071; occupancy $59,434.
- Part VII: STEVE MCBRIDE, EXECUTIVE DIRECTOR, 40 h/wk, $173,222; HEATHER SIMMET, "SR. DIR. OF ABUNDANCE AND HAPPINESS" (the CFO), $117,549.
- FY2025 (ProPublica summary): revenue $3,454,163; expenses $3,375,533; "Steve Mcbride, Executive Director" $142,206; "Heather Simmet, Senior Director And C.f.o." $117,549.
- Reading: the one nonprofit in the sample where inventory, production and wholesale matter more than classes.

#### A17. Claymakers Arts Community, Durham NC. EIN 45-2908222. 501(c)(3).
- Source: XML 202611329349304006 (FY 2025-01-01 to 2025-12-31, filed 2026-05-12).
- Employees 18. Volunteers 50.
- Total revenue $532,789; total expenses $422,175. Contributions $53,965 (10% of revenue).
- Program-service revenue $463,274, one line: "Classes & Workshops" (code 711130). Gross sales of inventory $90,751 (a materials store). Occupancy $102,729.
- Part VII: Emily Lassiter, Executive Director, 40 h/wk, $61,236 (+$6,837 other); Board Chair $4,491; 9 of 11 unpaid.
- Part III counts: "more than 1,000 adult learners annually"; "over 100 weekly classes and workshops"; "21 need-based scholarships"; "25 artists maintained private studio spaces"; firing service for "more than 70 members of the general public"; materials store serving "more than 1,000 individual artists, students, schools".
- Ratio: 18 W-2 staff : 1,000+ learners a year : 25 private-studio artists. Earned income is 87% of revenue, closer to a for-profit teaching studio than to the grant-funded centres above.

#### A18. Clay Studio of Missoula, MT. EIN 81-0523194. 501(c)(3).
- Source: XML 202503179349302925 (FY 2024-01-01 to 2024-12-31, filed 2025-11-13).
- Employees 19. Volunteers 105.
- Total revenue $1,302,079 (contributions $949,038, probably a building campaign; not stated); total expenses $469,363. Membership dues $7,445.
- Program-service revenue $303,636: "CLASS TUITION" $171,416; "STUDIO RENTAL INCOME" $65,025; "GALLERY INCOME" $33,400; "OTHER STUDIO INCOME" $29,069; "INSURANCE CLAIM" $3,112.
- Other salaries $161,032; occupancy $43,457.
- Part VII: SHALENE VALENZUELA, "EXECUTIVE DI", 40 h/wk, $55,869; 10 unpaid.

#### A19. Potters Guild Inc (Ann Arbor Potters Guild), Ann Arbor MI. EIN 38-6075181. 501(c)(3). Segment: cooperative / guild.
- Source: XML 202543189349316579 (FY 2024-01-01 to 2024-12-31, filed 2025-11-15).
- Employees 0. Volunteers: not reported.
- Total revenue $274,225; total expenses $265,539. Contributions $247,017 (membership dues $27,571 reported within contributions; other contributions $219,446).
- Program-service revenue $27,167: "CLASS FEES" $23,921; "WORKSHOP INCOME" $3,246; "MISC OVERLIMITS" (no amount).
- Occupancy $22,471. No salaries.
- Part VII: seven volunteer officers at 1 h/wk and $0: TREASURER, PRESIDENT, SECRETARY, VICE PRESIDENT, DIRECTOR, "KILN CHAIR", "REGISTRAR".

#### A20. Boulder Potters Guild, Boulder CO. EIN 84-0593203. 501(c)(3). Segment: cooperative / guild.
- Source: XML 202600689349301770 (FY 2024-05-01 to 2025-04-30, filed 2026-03-09).
- Employees 0. Volunteers 75.
- Total revenue $180,490; total expenses $207,405. Membership dues $34,401; contributions $56,340 in total.
- Program-service revenue $78,328: "CLASS & WORKSHOP INCOME" $51,673; "MEMBER KILN FEES" $12,116; "MEMBER MATERIALS INCOME" $2,989; "SHELF & STUDIO RENTAL INC" $11,550.
- Salaries $0; occupancy $4,532.
- Part VII (all $0): President 6 h/wk; Treasurer 8 h/wk; Secretary; "Class Chair"; "Membership Chai[r]"; "Workshop Chair"; "Maintenance Cha[ir]"; "Kiln Chair"; "Purchasing Chai[r]"; "Publicity Chair"; "Finance Chair" (3 h/wk each).
- Reading: the manager's job is split across 11 volunteer officers, 8 of them chairs. Kiln, purchasing (inventory), maintenance (equipment), classes (scheduling) and membership each have a named owner.

#### A21. Northern Colorado Potters Guild & Studio, Fort Collins CO. EIN 20-1482678. 501(c)(3). Segment: cooperative / guild.
- Source: XML 202601139349301155 (FY 2025-01-01 to 2025-12-31, filed 2026-04-23). The prior year was a 990-EZ.
- Employees 0. Volunteers 94.
- Total revenue $202,147; total expenses $141,608. Membership dues $83,442 (41% of revenue).
- Program-service revenue $116,666: "Classess & Workshops" [sic] $90,165; "Show Income" $26,501. Inventory sales $14,812. Occupancy $63,743.
- Part VII (all $0, 2 h/wk each): President, Vice President, Treasurer, Secretary, "Marketing Chair", "Classes Chair", two "Membership" co-chairs.

#### A21b. The Digs Chicago, Chicago IL. EIN 85-3163773. 501(c)(3). Segment: membership/community studio (nonprofit shell, run "as a product service business", per D1).
- Source: XML 202520589349300317 (FY 2024-01-01 to 2024-12-31, Form 990, filed 2025-02-27). The prior year was a 990-EZ.
- Employees 3. Volunteers: not reported.
- Total revenue $263,945; total expenses $252,653. **Membership dues $151,216 (57% of revenue)**; other contributions $34,508; program-service revenue $78,221 (one line, "Programs").
- Other salaries $6,896; occupancy $70,863 (28% of expenses).
- Part VII: Zoe Minzenberger, Director, 40 h/wk, $47,530; Fawn Penn, Director, 40 h/wk, $45,200; Treasurer 1 h/wk, $0.
- Part III: "Provided affordable studio space, classes, and events for artists and the public." No counts. The podcast (D1) gives "home to 100 artists" in about 2,000 sq ft.
- Derived (approximate): about $1,500 in dues per artist a year (about $125 a month) if all 100 pay dues. Two full-time co-director-owners for about 100 members, so the founders are the managers, at roughly $46,000 each.

#### A21c. Zen and Now Clay Studio Cooperative, Tucker GA. EIN 47-1025874. 501(c)(3). Form 990-EZ. Segment: cooperative.
- Source: XML 202513359349200601 (FY 2024-08-01 to 2025-07-31, filed 2025-11-30).
- Total revenue $67,120, of which **membership dues $66,445 (99%)**. Total expenses $58,454, of which **occupancy, rent, utilities and maintenance $46,474 (80%)**. No salaries line.
- Officers: five "Board Member"s at 1-2 h/wk, $0. Purpose: "To provide ceramic arts to the community." Programme text lists only charity donations of pottery (about 100 Empty Bowls bowls, for example).
- Reading: the pure co-op. Dues pay the rent; nobody is paid; no classes revenue.

#### A21d. Flamingo Clay Studio, Lake Worth FL. EIN 20-2847213. 990-EZ.
- ProPublica summary only (FY2024, filed 2025-11-17): revenue $11,981; expenses $23,021; "Joyce Brown, Executive Dir.: $0" and eight trustees at $0. https://projects.propublica.org/nonprofits/organizations/202847213. A micro nonprofit with an unpaid director. Not opened further.

#### Dropped or qualified candidates
- **Greenwich House Pottery (New York).** Not a separate filer. ProPublica returns only "Greenwich House Inc" (EIN 13-5562204), a multi-service settlement house; the pottery is one programme inside its return, so clay figures can't be separated. Dropped from the clay table.
- **Odyssey ClayWorks (Asheville).** No 501(c)(3) found under "Odyssey Clayworks", "Odyssey" (NC) or "Odyssey Center". Dropped. Its legal status is not confirmed here.
- **Radius Community Art Studio (Portland).** ProPublica search "Radius Community Art" returned no result. Not pursued.
- **Red Lodge Clay Center.** Not found by name in the MT results page shown (25 of 87). Not pursued.
- **Name-based nonprofit counts are not usable as market counts.** ProPublica search returns 667 results for "pottery" (the first page includes "Potter And Potters Mission Church" and several "Potters House" churches), 112 for "ceramic" (including "Ceramic Tile Distributors Association"), 29 for "Potters Guild" and 11 for "clay studio". Screening them by hand would be needed.

#### A22. Earned-revenue trend through the pandemic (US nonprofits, primary filings)

Program-service revenue (Form 990 Part VIII line 2g) from the ProPublica API `filings_with_data` (`totprgmrevnue`), https://projects.propublica.org/nonprofits/api/v2/organizations/{EIN}.json, plus the latest year from the XML above. Nominal dollars (not inflation-adjusted). Accessed 2026-10-20. Country: US.

| Organization (FY end) | FY2019 | FY2020 | FY2021 | FY2022 | FY2023 | Latest (year) | Latest vs 2019 |
|---|---|---|---|---|---|---|---|
| Baltimore Clayworks (Dec) | 462,777 | 243,888 | 519,621 | 692,043 | 820,785 | 937,791 (2024) | +103% |
| Clayworks, Charlotte (Jun) | 462,452 | 404,334 | 387,831 | 604,318 | 709,024 | 838,040 (FY2025) | +81% |
| The Clay Studio, Philadelphia (May) | 1,107,017 | 904,756 | 760,586 | 889,207 | 1,384,694 | 1,753,853 (FY2025) | +58% |
| Claymakers, Durham (Dec) | 307,628 | 131,490 | 212,760 | 349,598 | 500,139 | 463,274 (2025) | +51% |
| Clay Art Center, Port Chester (Aug) | 802,497 | 465,369 | 689,466 | 967,673 | 1,085,358 | 1,160,097 (FY2025) | +45% |
| Pottery Northwest, Seattle (Dec) | 357,844 | 158,677 | 164,174 | 346,070 | 462,650 | 497,128 (2024) | +39% |
| Mudflat, Somerville (Dec) | 1,013,632 | 536,528 | 871,625 | 1,161,942 | 1,240,481 | 1,291,889 (2024) | +27% |
| Northern Clay Center, Minneapolis (Dec) | 1,155,591 | 643,797 | 844,029 | 984,078 | 1,125,689 | 1,187,593 (2024) | +3% |

- Calendar-2020 earned revenue fell 44-57% at the five December-year-end studios (Baltimore -47%, Claymakers -57%, Pottery Northwest -56%, Mudflat -47%, NCC -44%). All eight were above their 2019 level by 2023 or later, and 7 of 8 are 27-103% above 2019 in nominal terms. Inflation since 2019 takes back part of these gains; I did not pull a price index, so the real change isn't quantified here. NCC's +3% is a real-terms decline.
- This is the cleanest primary evidence of a post-pandemic demand rise for teaching and membership studios that this run found. It covers nonprofits only.

### B. New Zealand potters' clubs (Charities Services register)

**Method.** NZ Charities Services public OData feed, `GrpOrgLatestReturns` (each charity's latest annual return), filtered on names containing "Potter", "Pottery", "Ceramic" or "Clay", excluding "House", "Mary" and "Masonic" (churches, a hospice and a lodge). URL: https://www.odata.charities.govt.nz/GrpOrgLatestReturns?$filter=(substringof('Potter',Name) or substringof('Pottery',Name) or substringof('Ceramic',Name) or substringof('Clay',Name)) and not substringof('House',Name) and not substringof('Mary',Name) and not substringof('Masonic',Name)&$format=json (accessed 2026-10-20). Of the 27 records returned, 13 are pottery clubs or societies with a return for a year ending in 2025 or 2026; the rest are unrelated trusts (for example "The Marie Clay Literacy Trust"), award trusts (Portage Ceramics Trust) or old returns. Fields are as filed: `NumberOfFulltimeEmployees`, `NumberOfParttimeEmployees`, `AvgAllPaidHoursPerWeek`, `AvgNoVolunteersPerWeek`, `AvgAllVolunteerHoursPerWeek`. Amounts are NZ$. Country: NZ. Segment: cooperative / club (community membership studio run by an incorporated society).

| Club | Year ended | Gross income | Membership fees | Service / trading income | FT staff | PT staff | Paid h/wk | Volunteer h/wk |
|---|---|---|---|---|---|---|---|---|
| Waikato Society of Potters | 2026-03-31 | 304,080 | 20,011 | null | 1 | 2 | 70 | 5 |
| Wellington Potters Association | 2025-07-31 | 289,983 | 112,922 | null | 0 | 2 | 40 | 18 |
| Marlborough Community Potters | 2025-06-30 | 106,564 | 12,840 | 45,028 | 0 | 4 | 8 | 163 |
| Bethlehem Pottery Club | 2025-05-31 | 105,927 | 18,926 | 78,283 | 0 | 0 | 0 | 132 |
| Taradale Pottery Group | 2025-12-31 | 95,608 | 15,295 | 75,523 | 0 | 1 | 20 | 0 |
| Craft Potters Nelson | 2026-03-31 | 89,671 | 20,810 | null | 0 | 0 | 0 | 20 |
| Whanganui Potters Studio | 2025-08-31 | 80,041 | 8,989 | 69,629 | 0 | 0 | 0 | 24 |
| Nelson Community Potters | 2025-07-31 | 78,822 | 28,155 | 46,446 | 0 | 0 | 0 | 33 |
| New Plymouth Potters | 2026-03-31 | 71,071 | 13,276 | 55,372 | 0 | 4 | 8 | 1 |
| Hakatere Ceramics & Pottery Ashburton | 2026-03-31 | 43,586 | 7,275 | 0 | 0 | 0 | 0 | 13 |
| South Canterbury Pottery Group | 2025-12-31 | 39,267 | 4,653 | 30,496 | 0 | 0 | 0 | 9 |
| Keirunga Potters | 2026-03-31 | 24,673 | 1,800 | 13,229 | 0 | 0 | 0 | 2 |
| Manawatū Pottery Society | 2025-12-31 | 24,083 | 13,263 | 0 | 0 | 0 | 0 | 20 |

- **8 of 13 clubs have no paid staff at all.** 5 of 13 have part-time staff; 1 of 13 has a full-time employee. The median club has gross income of about NZ$80,000 (Whanganui, 7th of 13).
- Paid hours, where any exist, are 8-70 a week in total. A 40-hour week across two part-timers (Wellington) is the largest "manager" footprint among clubs that have no full-time staff.
- Deregistered clubs in the same feed: Ashburton Pottery Society (last return 2014), Rotorua Potters Group (last return 2016, income NZ$23,352). Wyndham Potters Charitable Trust also deregistered.
- Caveat: these are self-reported annual-return figures. "Volunteer hours per week" is an estimate. Clubs that are not registered charities (Auckland Studio Potters did not appear under any "Potter" name in the Organisations feed) are missing, so this is not a census of NZ clubs.

**Trend for three clubs** (`GrpOrgAllReturns`, same fields, all years; https://www.odata.charities.govt.nz/GrpOrgAllReturns?$filter=Name eq '...'&$format=json; accessed 2026-10-20):

| Club | Gross income, year ending 2019 | Latest | Change | Membership fees 2019 → peak → latest | Staffing change |
|---|---|---|---|---|---|
| Wellington Potters Association (Jul) | 134,803 | 289,983 (2025) | +115% | 39,434 → 198,011 (2023) → 112,922 (2025) | **0 paid staff in every return from 2009 to 2023**; 1 part-timer at 20 h/wk in 2024; 2 part-timers at 40 h/wk in 2025. Volunteer hours fell from 100-200 a week (2009-2018) to 3-45 (2019-2025). |
| Waikato Society of Potters (Mar) | 165,462 | 304,080 (2026) | +84% | 10,104 → 20,222 (2024) → 20,011 (2026) | 8 part-timers, 60 paid h/wk (2019); 11 part-timers, 83 h (2020); 1 full-time and 2 part-time, 70 h (2026). |
| Nelson Community Potters (Jul) | 25,216 | 78,822 (2025) | +213% | 6,661 → 28,155 (2025) | Part-time headcount swung between 0 and 7 a year; 0 paid staff in 2025. |

- Reading: NZ clubs saw the same post-2020 demand rise as the US nonprofits. The largest volunteer-run club (Wellington) crossed into paid part-time management only after its income roughly doubled, which is evidence of a size threshold at which volunteer management stops scaling. The threshold is inferred from one case, so treat it as a hypothesis.

### C. Market counts from statistical agencies

#### C1. US: BLS Quarterly Census of Employment and Wages (QCEW), NAICS 611610 Fine Arts Schools
- **Why QCEW and not CBP/NES.** The Census Data API (`api.census.gov/data/2022/cbp`, `/2023/cbp`) now returns "Missing Key"; a key needs an account, which this brief forbids. census.gov NAICS pages returned 403. BLS QCEW open-data slices need no key: `https://data.bls.gov/cew/data/api/{year}/a/industry/611610.csv`, row `area_fips = US000`, `own_code = 5` (private). QCEW counts establishments with employees covered by unemployment insurance, so it is close to CBP and, like CBP, **excludes nonemployer sole proprietors**. Nonemployer Statistics could not be reached (same API key barrier). Accessed 2026-10-20. Country: US. Segment: all teaching and membership studios that register as schools, mixed with every other kind of arts school.
- **Scope warning.** NAICS 611610 is "instruction in the arts, including dance, art, drama, and music". Its index entries include "Ceramics instruction" and "Handicrafts instruction" but also "Ballet schools", "Dance studios", "Music instruction (e.g., guitar, piano)", "Theater schools" (index list read from https://www.naics.com/naics-code-description/?code=611610, a third-party mirror of the Census NAICS manual, because census.gov returned 403). Pottery is a small and unknown fraction. Studios may also code themselves as 713990 (recreation), 453/459 (retail) or 711510 (independent artists).

| Year | Private establishments (annual avg) | Employment (annual avg) | Avg annual pay | Establishments, change on prior year |
|---|---|---|---|---|
| 2019 | 14,334 | 105,230 | $19,642 | +2.7% |
| 2020 | 14,821 | 92,006 | $20,085 | +3.4% (employment -12.6%) |
| 2022 | 15,311 | 109,409 | $22,123 | +2.4% (employment +11.9%) |
| 2024 | 16,080 | 123,931 | $23,379 | +2.1% (employment +5.7%) |
| 2025 | 16,299 | 129,074 | $23,617 | +1.4% (employment +4.1%) |

- Derived: establishments +13.7% from 2019 to 2025; employment +22.7%; about 7.9 employees per establishment in 2025 (129,074 / 16,299). Average pay of about $23,600 means this is overwhelmingly part-time teaching work.
- **Context code, NAICS 327110 (Pottery, Ceramics, and Plumbing Fixture Manufacturing), 2025, private:** 797 establishments, 12,332 employees, average pay $68,102; establishments -0.6% and employment -3.3% on 2024. This is mostly industrial (sanitaryware, tile); studio potters are mostly nonemployers and invisible here.

#### C2. UK: ONS Inter-Departmental Business Register via Nomis, UK Business Counts (enterprises), dataset NM_142_1
- URL: https://www.nomisweb.co.uk/api/v01/dataset/NM_142_1.data.csv?geography=2092957697&industry=134241138,134303248&legal_status=0&measures=20100&date=... (industry codes are 134217728 + the 5-digit SIC 2007 code). Accessed 2026-10-20. Country: UK.
- Coverage warning: UK Business Counts include only VAT- and/or PAYE-registered businesses. Sole-trader studios below the VAT threshold and with no employees are missing. SIC 85520 "Cultural education" also covers dance, music and drama schools.

| Year (March snapshot) | 85520 Cultural education | of which 0-4 employees | 23410 Ceramic household and ornamental articles | of which 0-4 employees |
|---|---|---|---|---|
| 2018 | 1,490 | | 220 | |
| 2019 | 1,570 | | 220 | |
| 2020 | 1,675 | | 225 | |
| 2021 | 1,760 | | 225 | |
| 2022 | 1,895 | | 250 | |
| 2023 | 1,975 | | 265 | |
| 2024 | 2,040 | | 265 | |
| 2025 | 2,115 | 1,675 | 285 | 215 |
| 2026 | 2,320 | 1,820 | 270 | 200 |

- Derived: cultural-education enterprises +48% from 2019 to 2026 (1,570 to 2,320); ceramic-ware makers +23% (220 to 270), with a dip in 2026 after a 2025 peak. In 2026, 78% of cultural-education enterprises (1,820 of 2,320) and 74% of ceramic-ware makers (200 of 270) have 0-4 employees. Counts are rounded to the nearest 5 by ONS.

#### C3. UK: Companies House register, companies with "pottery" in the name
- Method: Companies House advanced search (`find-and-update.company-information.service.gov.uk/advanced-search/get-results?companyNameIncludes=pottery&...`), filtering by incorporation year or by dissolution year and status. Accessed 2026-10-20. Country: UK. Segments: mixed (studios, PYOP cafés, makers, retailers, and some non-pottery uses of the word, such as "POTTERY CLOSE MANAGEMENT COMPANY LIMITED", a property company).
- Active companies with "pottery" in the name: **323**.
- Incorporations per year (all current statuses): 2018: 28 · 2019: 27 · 2020: 21 · 2021: 31 · 2022: 35 · 2023: 42 · 2024: 46 · 2025: 59. **Formations more than doubled from 2019 to 2025 (27 to 59, +119%).**
- Dissolutions per year: 2019: 15 · 2023: 26 · 2024: 18 · 2025: 20. Net formation is positive every year checked.
- SIC codes on the first 20 active results (ordered by relevance) are scattered: 23410 (ceramic ware), 85520 (cultural education), 90030 (artistic creation), 90040 (operation of arts facilities), 47781/47789/47910/47990 (retail), 56102 (unlicensed cafés, a "POTTERY CAFE"), 96090 (other personal services), 32990 (other manufacturing). **Studios register under at least eight SIC codes, so any single-code count (C2) undercounts them.**
- Limits: name-based only (misses "clay", "ceramics" or "kiln" names and every sole trader); includes non-studio companies; relevance-ordered first pages were not screened one by one.
- A plain name search for "pottery studio" ordered by relevance showed 20 companies; 17 of the 20 were incorporated in 2020 or later and 13 in 2023-2026. This is a biased sample (relevance ordering), so treat it only as colour.

#### C4. UK clay charities (CharityLens, an aggregator of Charity Commission data)
- The Charity Commission register (register-of-charities.charitycommission.gov.uk) and findthatcharity.uk refused WebFetch (403 and socket hang-up). CharityLens (charitylens.co.uk) loaded but shows only income and expenditure, with no employee, volunteer or income-source data. Accessed 2026-10-20. Country: UK.
- "pottery" search on CharityLens: 50 results, many unrelated (churches, community centres, a "POTTERY PRIMARY SCHOOL FUND"). https://charitylens.co.uk/search?q=pottery
- **Clapham Pottery Limited** (charity 1129048). Community pottery: "Clapham pottery is a not-for-profit community pottery providing classes and workshops for those who can afford to pay", with free or low-cost sessions for community groups (paraphrase of the remainder). FY2025 income £138,782; expenditure £115,511. Segment: nonprofit community/teaching studio. https://charitylens.co.uk/charity/clapham-pottery-limited-1129048
- **Bernard Leach (St Ives) Trust Limited** (1111263), the Leach Pottery. Objects include to "train people in the art, craft and manufacture of pottery and related skills". FY2025 income £8,361,491 (a one-off, by inference; expenditure was £848,353 and gross assets £9,592,523). Segment: heritage production pottery and training. https://charitylens.co.uk/charity/bernard-leach-st-ives-trust-limited-1111263
- **The Dacorum and Chiltern Potters Guild** (1161265). A guild: lectures, newsletter, public have-a-go events. FY2025 income £22,790; expenditure £22,746. Segment: cooperative/guild (no studio). https://charitylens.co.uk/charity/the-dacorum-and-chiltern-potters-guild-1161265
- From the search list only (rounded incomes, not opened individually): Exmouth Ceramic Group £24K; The Farnham Pottery Trust £82K; Sharpe's Pottery Heritage and Arts Trust £143K; Max's Pottery £51K; Swansea Pottery Collective (newly registered, no income shown).

#### C5. Canada and Australia: not reached
- Canada: CRA charity search renders by JavaScript ("Loading/Chargement..."); charitydata.ca search returned an empty shell; CanadaHelps had no page at the guessed URL. Statistics Canada business counts were not attempted, because the web tables are dynamic and the alternative is a file download.
- Australia: the ABS Data API dataflow list (790k characters) showed no Counts of Australian Businesses (CABEE) flow in the first 200k characters; CABEE is published as spreadsheet data cubes. ACNC register search timed out; data.gov.au returned 403.

### D. Owner and manager voice (podcast show notes)

**Method and limits.** The session-wide web search cap was reached after my second search, and Reddit and The Guardian refused WebFetch outright ("unable to fetch"). Press and forum discovery was therefore replaced by the internal search of *The Potters Cast* site (`thepotterscast.com/?s=...`; "community studio" has 31 result pages, of which I read 2; "membership studio" page 1; "paint your own" page 1; "closing studio" 1 result) and by direct fetches of episode pages. The pages carry bios and short show-note Q&A excerpts, not full transcripts. Quotes below are the short fragments WebFetch returned verbatim; anything longer is marked as a paraphrase. Practitioner voice, not vendor marketing.

| # | Source (date) | Country, segment | What it says |
|---|---|---|---|
| D1 | Ep. 1132, Fawn Penn, The Digs Chicago, https://thepotterscast.com/1132/ (2025-05-15) | US, nonprofit community studio run like a business | Founded 2020; "a non-profit community ceramics studio home to 100 artists"; host refers to "the two thousand square feet". Finances: "We did not receive grants for probably two years after we opened"; "we just ran it as a product service business"; "We definitely were scraping by pretty brutally for the first year but we have never not covered the cost." Lease: "It stresses me out all of the time." Workload: the founder says the management role leaves her "I don't have much time at all" for her own work. |
| D2 | Ep. 1253, Jeffery Liang, https://thepotterscast.com/1253/ (2026-08-11) | US (Menlo Park CA), membership/teaching studio, for-profit (not stated explicitly) | "officially opened for business in February 2025"; "membership is really stable for us" (memberships described as the most stable income). Answering emails about private events, class scheduling and makeup classes "takes a lot of effort every day" (fragment). An unexpected attorney and realtor bill was a "big amount of amount of money". No member counts or prices given. |
| D3 | Ep. 997, Courtney Brewer and Scott Pierce, The Clay Warehouse, https://thepotterscast.com/inside-look-at-running-a-community-studio-courtney-brewer-scott-pierce-episode-997/ (2024-01) | Canada (BC), community studio | "founded in 2021"; offers "services such as kiln rentals"; "having a booking platform"; Instagram is "our main source to get the information out there". Workload: "it's a lot of lost sleep but we are making it happen." |
| D4 | Ep. 1098, Jennifer Waverek, "The Business of a Community Studio", https://thepotterscast.com/1098/ (2025-01-16) | US, community/teaching studio | "there are five classes most days" ("maybe two classes" on light days); spring classes set "12 weeks from now"; "we actually have a pretty extensive operations manual" with job descriptions; "the teachers are all independent" (contractors); "Members have their own space"; no sign-up system for studio time, only "a schedule if you want an outside firing appointment". |
| D5 | Ep. 1102, Yesha Panchal, ceramics studio manager, Hudgens Center for Art, Duluth GA, https://thepotterscast.com/1102/ (2025-01-30) | US, nonprofit art centre, hired studio manager | Duties described are housekeeping and shelf control: "We mop almost every day in the studio"; "at the end of every session we mark things down and move things to an orphan shelf"; unclaimed work is discarded or recycled after one more session. Her own studio is in the same building: "it is really hard to keep that time separate." |
| D6 | Ep. 1169, Sela Stazzone, "A Year In To Opening A Studio", https://thepotterscast.com/1169/ (2025-10-14) | Country not stated, teaching studio with retail | "People can purchase pottery on top of registering for classes"; "It was more so like a lease situation"; "I had to get more beefier insurance under the business and the brick and mortar". Workload: "I joke around that I practically live here and that I have an air mattress under my desk"; "I'm here all the time." On website tools: "it's definitely more cost effective with go daddy but I currently don't have nice things to say." |

Counts across D1-D6 (n = 6 episodes): owner or manager workload or stress mentioned in 5 (D1, D2, D3, D5, D6); membership named as the stable revenue base in 1 (D2); class scheduling, booking or class load mentioned in 3 (D2, D3, D4); a booking platform mentioned in 1 (D3), with none named; a website-builder complaint in 1 (D6); inventory, QuickBooks, Stripe or Square mentioned in 0; teachers as independent contractors in 1 (D4); retail sales of pottery alongside classes in 1 (D6). Also screened but not used: Ep. 1195 (Twisted Clay Studio, 2026-01-20) had no operational detail beyond an "online Membership" for classes.

### E. Vendor and franchisor material (marketing; kept apart from practitioner voice)

- **E1. Kiln Fire, "The Comprehensive Guide to Opening a Pottery Studio"** (vendor blog, 2023-12-12, no author). https://kilnfire.com/blog/opening-a-pottery-studio. Country: US. Segment: membership/teaching (vendor framing). Quotes: "have between 150 and 200 square feet of working space for every student or member"; "A small studio for personal use or teaching a few students may require as little as 400-600 square feet."; "you may need anywhere from 1,000 to 2,000 square feet or more."; "monthly rent can range from the high hundreds to low 5-figures."; "You will likely have members that require renewal tracking and payment management."; "And you’ll have staff to schedule." The article promotes Kiln Fire.
- **E2. Color Me Mine franchising** (franchisor marketing, accessed 2026-10-20). Country: US. Segment: PYOP. "Since 1991, Color Me Mine has been the leader in the Paint Your Own Pottery industry." (https://www.colormeminefranchising.com/). Investment page (https://colormeminefranchising.com/investment/): estimated initial investment "$219,180" to "$475,410"; initial franchise fee "$30,000"; tenant improvements $65,000-$200,000; furniture, fixtures and equipment $65,000-$84,000; additional funds for the first 3 months $20,000-$60,000; leasehold estimates assume "a 1,200 to 2,000 square foot space"; 2025 landlord contributions "ranged from $0 to $133,623". No royalty, Item 19 (revenue) or Item 20 (location count) figures on the public pages. The studio locator (https://www.colormemine.com/location-finder/ and /locations) renders by JavaScript and the sitemaps list no studio pages, so **the location count was not obtained**. Crock A Doodle (Canada) returned HTTP 402.

### F. Quantified profile by segment

Counts below come from sections A-E. "US sample" = the 24 US organizations in A1-A21d. Line-item counts use the 23 with line items (Flamingo has only a summary).

| Segment | Evidence (n, country) | Size | Revenue mix | Paid staff (W-3 headcount) | Who manages | Uncertainty |
|---|---|---|---|---|---|---|
| **Nonprofit community/teaching clay centre** | 9 US 990 filers: NCC, Clay Studio, Baltimore, Mudflat, Clay Art Center, Pottery Northwest, Clayworks, Claymakers, Clay Studio of Missoula. Plus 1 990-EZ (Clay Center of New Orleans) and 1 UK charity (Clapham Pottery, income only) | Revenue $0.53M-$6.88M, median $1.50M (Baltimore). Students: "MORE THAN 600 STUDENTS EACH TERM" (Clay Studio); "2,347 children and adults" a year (Baltimore); "more than 1,000 adult learners annually" (Claymakers) | Class/workshop tuition = 56-100% of program-service revenue (Missoula 56, Mudflat 62, Clay Studio 66, Baltimore 72, Clay Art Center 83, PNW 94, Clayworks 100, Claymakers 100). Studio/firing/artist fees 9-31% where itemized (Mudflat 9, Clay Studio 9, CAC 13, Baltimore 19, Missoula 31). Sales of gallery, consigned or supply goods 6-32% (PNW 6, Baltimore 9, Missoula 11, Clay Studio 15, Mudflat 17, NCC 32). Contributions 10-73% of total revenue (median 31%) | 7-38 W-2 people, median 19; volunteers 23-141 | A paid Executive Director in 9 of 9 990 filers (pay $55,869-$158,534, median $110,235). Below the ED: technical director, facility manager, business manager, gallery manager (Mudflat and NCC via CauseIQ). The 990-EZ filer (CCNO) has no paid manager; its board president gives 20 h/wk | W-3 is headcount, not FTE; Part VII hides staff under $100,000; capital gifts inflate contributions for Clay Studio and Missoula |
| **Membership / community studio** (for-profit, or nonprofit run as a business) | 1 US 990 (The Digs Chicago) + 5 podcast owners (US 3, Canada 1, not stated 1) | Digs: about 100 artists, about 2,000 sq ft, revenue $263,945 | Digs: membership dues 57%, programs 30%; occupancy 28% of expenses. D2: "membership is really stable for us" | Digs: 3 W-2 staff, two 40 h/wk co-directors at about $46,000 each | The founder-owners | **No quantified for-profit revenue mix, price list, margin or rent was found** (gap). Membership price at the Digs is derived (about $125/month) |
| **Cooperative / guild with volunteer officers** | US 5 (KC Clay Guild, Ann Arbor PG, Boulder PG, Northern Colorado PG, Zen and Now co-op); NZ 13 clubs; UK 1 guild (no studio) | US revenue $67,120-$274,225, except KC $1.21M; NZ gross income NZ$24,083-NZ$304,080, median about NZ$80,000 | Dues 99% (Zen and Now), 41% (NoCo), 19% (Boulder), 10% (Ann Arbor), 4% (KC). Classes 29% (Boulder) and 45% (NoCo) of total revenue. Boulder member kiln fees 7% and shelf/studio rental 6% of total revenue | 0 W-2 staff in 4 of 5 US guilds (KC has 3); 0 paid staff in 8 of 13 NZ clubs; 1 of 13 NZ clubs has a full-time employee | **Nobody.** The job is split among volunteer chairs: Kiln Chair (2 of 5 US guilds), Purchasing Chair (1), Maintenance Chair (1), Membership Chair (3), Class/Workshop/Educational Chair or Registrar (4), Treasurer (5) | Self-reported hours; NZ register covers only clubs that are registered charities |
| **Residency centre / craft school** | US 5: Archie Bray, Watershed, Arrowmont, Penland, HCCC | Revenue $1.1M-$10.7M; Penland "1,500 STUDENTS" in "16 STUDIOS"; Arrowmont "MORE THAN 150 CLASSES"; residencies from "UP TO 10" a year (HCCC) to about 30 (Watershed: about 18 summer and 12 fall) | Contributions 27-96%; Arrowmont housing and board $1.14M exceeds tuition $0.87M | 16-143 | Paid ED or CEO plus C-suite (CFO, facilities director) | Multi-media schools; a weak fit for a studio-manager product |
| **Production pottery** | US 1 nonprofit (Pewabic) + Archie Bray's supply business; UK 1 (Leach, income only); QCEW 327110 | Pewabic: 70 W-2 staff | Pewabic gross inventory sales $3,392,959 against educational revenue $290,151; Archie Bray inventory sales $1,341,598 | 54-70 | ED + CFO | QCEW 327110 is mostly industrial (797 US establishments, 2025) |
| **PYOP** | Franchisor marketing only (Color Me Mine); UK Companies House: 10 active companies with "pottery painting" in the name | Franchise assumes 1,200-2,000 sq ft; investment $219,180-$475,410 | Not found | Not found | Owner-operator (franchise model) | **No location counts or revenue figures obtained** |
| **Municipal / recreation, university, makerspace** | Not covered in this run (they appear only in the 84-studio census in 03) | | | | | Gap |

**Market counts and growth by country**

| Country | Measure | Count | Growth | Method notes |
|---|---|---|---|---|
| US | QCEW NAICS 611610 Fine Arts Schools, private establishments | 16,299 (2025) | +13.7% since 2019; employment +22.7% | Includes dance, music and drama; excludes nonemployers; the ceramics share is unknown |
| US | Nonprofit earned revenue, 8 clay centres | Program revenue +3% to +103% from 2019 to the latest year (7 of 8 at +27% or more) | | Nominal dollars |
| UK | Nomis UK Business Counts, SIC 85520 Cultural education | 2,320 enterprises (2026) | +48% since 2019 | VAT/PAYE-registered only; covers all arts teaching |
| UK | Nomis, SIC 23410 Ceramic household and ornamental articles | 270 enterprises (2026) | +23% since 2019 | Makers, not studios |
| UK | Companies House, active companies with "pottery" in the name | 323 | Formations 27 (2019) to 59 (2025), +119%; dissolutions 15-26 a year | Name-based; mixed segments |
| NZ | Potters' clubs registered as charities with 2025-26 returns | 13 | Wellington +115%, Waikato +84%, Nelson +213% income since 2019 | Charities only |
| Canada | | Not reached | | CRA and charitydata.ca are JavaScript-rendered |
| Australia | | Not reached | | ABS CABEE is spreadsheets only; ACNC timed out |

### G. Who manages, by model

| Model | Who the "manager" is | Evidence (counts) | Paid staff per member or student | Manager exists? |
|---|---|---|---|---|
| Owner only | The owner does everything | D6 ("I practically live here"). UK: 1,820 of 2,320 cultural-education enterprises (78%) have 0-4 employees (C2). No count of owner-only US studios | n/a | Yes: the owner |
| Owner + part-time staff or contract teachers | The owner; teachers are contractors | D4 ("the teachers are all independent", with an operations manual); D2; QCEW average pay in 611610 of $23,617 suggests mostly part-time staff | Not quantified for for-profits | Yes: the owner |
| Founder co-directors (membership studio) | Two founders at 40 h/wk | The Digs: 3 W-2 staff, about 100 artists | About 1 paid person per 33 artists (headcount) | Yes: founders |
| Hired manager in a for-profit | Not evidenced in this run | 0 sources | | Unknown |
| Nonprofit with ED, studio/technical manager and technicians | The ED in small centres; the technical director or studio manager runs the floor in larger ones | Paid executive in 17 of 24 US organizations. Technical Director + Facility Manager at Mudflat; Business Manager + Sales Gallery Manager at NCC (CauseIQ); hired studio manager at Hudgens (D5) | Clay Studio 38 staff : 600+ students a term (about 1:16); Claymakers 18 : 1,000+ learners a year (about 1:56); Baltimore 20 : 2,347 a year (about 1:117) | Yes |
| Co-op / guild with volunteer officers | No one. Kiln, purchasing, maintenance, classes, membership and money are split among volunteer chairs | 0 W-2 staff in 4 of 5 US guilds; 0 paid staff in 8 of 13 NZ clubs. Treasurer logs the most hours in both US guilds whose officers report different hours (Boulder 8 h/wk, KC 12 h/wk) | KC: 3 staff : 350+ class students; Boulder, Ann Arbor, NoCo, Zen and Now: 0 | **No** |
| Micro nonprofit | An unpaid president or director | Clay Center of New Orleans (president 20 h/wk, $0); Flamingo Clay Studio ("Executive Dir.: $0") | 0 | Volunteer only |
| University-affiliated nonprofit | A university employee seconded as ED | Worcester Center for Crafts: ED paid $97,348 by Worcester State University | 33 W-2 staff | Yes, paid by the university |
| Municipal / recreation | Not evidenced in this run | | | Unknown |

**Models with no manager at all:** cooperatives and guilds (4 of 5 US; 8 of 13 NZ clubs with no paid staff) and micro nonprofits (2 of 2 990-EZ studios checked by summary). In all, 7 of 24 US organizations list no paid executive.

### H. Workload, burnout, closures and changes of hands

- **Owner workload (practitioner voice):** 5 of 6 podcast owners and managers mention stress or long hours (D1, D2, D3, D5, D6). Examples: "it's a lot of lost sleep" (D3, 2024); "I have an air mattress under my desk" (D6, 2025); lease "stresses me out all of the time" (D1, 2025).
- **Leadership changes visible in filings:** 3 of 24 US organizations. Northern Clay Center's FY2025 990 lists an outgoing "Exec Dir Thr[u]" and a new "Executive Di[rector]"; Watershed's FY2024 990 lists an ED and an "INTERIM EXEC"; Baltimore Clayworks' ED in its FY2024 990 (Matthew Hyleck) differs from CauseIQ's current listing (Cyndi Wish; timing UNVERIFIED). The filings give no reasons.
- **Deficits in the latest filing:** 9 of 24 (NCC -$415,188 in 2024 and -$421,928 in 2025; Archie Bray -$614,254; KC Clay Guild -$440,517 in a year with $1,136,098 of occupancy costs; Baltimore -$118,618; Penland -$98,319; HCCC -$71,222; Boulder Potters Guild -$26,915; Clay Center of New Orleans -$13,370 with net assets of -$45,444; Flamingo -$11,040).
- **Volunteer capacity:** Wellington Potters Association's reported volunteer hours fell from 100-200 a week (2009-2018) to 3-45 (2019-2025) while income doubled, and it took on paid part-timers in 2024 and 2025 (B).
- **Closures:** UK Companies House shows 26 (2023), 18 (2024) and 20 (2025) dissolutions of companies with "pottery" in the name, against 42, 46 and 59 formations; the register gives no reasons. NZ: Ashburton Pottery Society (last return 2014) and Rotorua Potters Group (last return 2016) are deregistered. **No 2023-2026 news report of a specific studio closing or being sold, with reasons, could be reached in this run** (web search budget exhausted; Reddit, The Guardian, BizBuySell and Crafts Council blocked). An adjacent item seen only as a search-result teaser: "Changes at Kemper Tools Ripple Through Ceramics Industry" (Craft Industry Alliance, 2024-08-27), about a tool supplier's possible closure and buyer (https://craftindustryalliance.org/changes-at-kemper-tools-ripple-through-ceramics-industry/). Not opened; UNVERIFIED.

## Hypothesis evidence

| Hypothesis | Rating | What the evidence shows (counts) |
|---|---|---|
| **Inventory tracking** | **Moderate** that goods are sold; **weak** that tracking them is a pain | 18 of 23 US organizations sell physical goods: 12 report inventory sales on Part VIII line 10a, 5 more have sales, supplies, materials or gallery lines in program revenue, and KC Clay Guild says it sells clay supplies and tools. But in 10 of the 12 inventory reporters, gross inventory sales are at most 17% of reported total revenue (NCC 3.4%, PNW 3.8%, Watershed 1.4%, Arrowmont 1.6%, Penland 2.5%, HCCC 2.9%, NoCo 7.3%, WCC 12.7%, CCNO 16.4%, Claymakers 17.0%). Only Pewabic (production pottery) and Archie Bray (clay supply business) are inventory-led. Supporting roles: a "Purchasing Chair" (Boulder); Mudflat "SUPPLIES" $79,669; Claymakers' materials store serving "more than 1,000" customers. 0 of 6 podcast owners mention inventory. |
| **Asset and equipment tracking** | **Weak** | 10 of 23 US organizations bill for studio space, firing or equipment use: 4 as a named revenue line (Baltimore "STUDIO & EQUIPMENT REN[TAL]" $177,766; Mudflat "STUDIO FEES" $116,125; Missoula "STUDIO RENTAL INCOME" $65,025; Boulder "MEMBER KILN FEES" $12,116 and "SHELF & STUDIO RENTAL" $11,550) and 6 in programme descriptions (KC, Watershed, CCNO, Claymakers, Clay Art Center, the Digs). Equipment has named owners: "Kiln Chair" in 2 of 5 US guilds, "Maintenance Chair" in 1; a Technical Director and Facility Manager at Mudflat; a "Director Of Facilities And Grounds" at Penland. Kiln rental also appears in D3. **No source describes losing track of equipment or wanting an asset register** (0 of 6 podcasts, 0 filings). The evidence points to billing and booking equipment use, not to registering assets. |
| **Sales integrations with multiple payment providers** | **None** (direct) | 0 of 24 filings and 0 of 6 podcasts name a payment provider. Indirect only: 8 of 23 organizations itemize 3 or more program revenue streams (tuition, memberships, studio/firing fees, gallery/consignment sales, host fees, supplies), so money arrives through several channels, but nothing here shows more than one processor. Tool adoption is covered in 03-tools-in-use.md. |
| **QuickBooks Online integration** | **Weak** (indirect only) | 0 of 24 filings and 0 of 6 podcasts name QuickBooks or any accounting package. Indirect: in both US guilds whose officers report different hours, the treasurer logs the most (Boulder 8 h/wk against the president's 6 and the chairs' 3; KC 12 h/wk against the president's 5). At least 5 of 24 organizations employ finance staff (NCC Business Manager, Baltimore Director of Finance and Administration, Penland Director of Finance, Arrowmont CFO, Pewabic CFO). Consignment (NCC: $385,496 across four consigned-sales lines; 179 artists) means artist payouts to reconcile. |
| **Scheduling** | **Strong** for class scheduling and registration; **moderate** for studio-time and kiln scheduling | Class or workshop tuition is the largest program-service revenue line in 15 of 23 US organizations (4 unclear, 4 led by dues or inventory); tuition is 56-100% of program revenue in the 8 teaching centres that itemize it. 3 of 6 podcasts concern class scheduling, booking or class load (D2 "class scheduling" and makeup-class emails; D3 "booking platform"; D4 "five classes most days", set 12 weeks ahead). 4 of 5 US guilds have a volunteer class, workshop or education chair or a registrar. For studio time, D4 has no sign-up system and only "a schedule if you want an outside firing appointment"; studio, firing or equipment use is billed in 10 of 23 organizations (see the asset row), so it is a bookable resource there. Caveat: 03 shows class booking is already handled by general tools, so the need is real but crowded. |

## Other strong signals

1. **Recurring membership billing is the backbone of co-ops and membership studios.** It is not on the hypothesis list. Dues are 99% of revenue at Zen and Now, 57% at the Digs, 41% at NoCo and 19% at Boulder; NZ club membership fees run NZ$1,800-NZ$112,922. D2: "membership is really stable for us." Kiln Fire's guide (vendor) also names "renewal tracking and payment management".
2. **Many studios have no manager to sell to.** 7 of 24 US organizations list no paid executive; 4 of 5 US guilds and 8 of 13 NZ clubs have no paid staff. The work is split among volunteer roles (Kiln Chair, Purchasing Chair, Maintenance Chair, Membership Chair, Class Chair, Registrar, Treasurer) giving 1-12 hours a week each. A tool that "stands in for a manager" in this segment has to support many part-time volunteer roles and permissions, not one administrator.
3. **In nonprofits the ED is the manager**, at a median of $110,235 in the 9 clay centres (range $55,869-$158,534), with 7-38 W-2 staff, mostly part-time or contract teachers (QCEW average pay in fine arts schools was $23,617 in 2025). Studio managers and technicians sit below the Part VII threshold and do not show in filings.
4. **Demand grew strongly after 2020.** Earned revenue is up 27-103% on 2019 at 7 of 8 US clay centres; income is up 84-213% at 3 NZ clubs; UK "pottery" company formations rose from 27 (2019) to 59 (2025); UK cultural-education enterprises are up 48% and US fine-arts-school establishments 13.7% since 2019.
5. **A size threshold for paid management.** Wellington Potters (NZ) ran with no paid staff for 15 years and hired part-timers only after income doubled. Small studios that are growing are the natural first users of a free "virtual manager".
6. **Occupancy is the cost that hurts.** It takes 80% of expenses at Zen and Now, 45% at Clay Center of New Orleans and at NoCo, and 28% at the Digs; owners name lease stress in D1 and D6.
7. **Consignment and gallery sales of members' and artists' work are a distinct workflow at art centres.** NCC: $385,496 in consigned sales and 179 represented artists; Baltimore: "88 shop artists" and gallery sales of $85,024; Mudflat pottery sales $136,020; Clay Studio sales $266,475.
8. **Firing and studio fees are a real but secondary revenue line:** 9-31% of program revenue where itemized (5 organizations); Boulder's member kiln fees are $12,116 (7% of total revenue).
9. **Owner workload is the dominant theme in owner voice:** 5 of 6 podcast sources.

## Gaps and limits

- **Search budget.** The session-wide WebSearch cap was hit after my second query. Everything after that came from direct fetches of known URLs, public APIs and site-internal search (ProPublica, GivingTuesday, NZ OData, Nomis, BLS, Companies House, CharityLens, The Potters Cast, Ceramic Arts Network, Craft Industry Alliance). Discovery of press stories, owner interviews and closures suffered most.
- **Blocked or unusable:** Reddit and The Guardian ("unable to fetch"); Census Data API (needs a key, so CBP and Nonemployer Statistics were not used); census.gov NAICS pages (403); ProPublica XML and PDF downloads (403) and its JavaScript-only full-filing view; IRS PDF copy (scanned, no text); GivingTuesday API root and docs (403); Charity Commission register (403); findthatcharity.uk (socket hang-up); CRA charity search and charitydata.ca (JavaScript shells); ACNC (timeout); data.gov.au (403); Crafts Council (403); BizBuySell (403); Crock A Doodle (402); Color Me Mine locator (JavaScript).
- **Not covered:** for-profit revenue mix, membership prices, rent and margins stated by owners (no numeric statements found); PYOP location counts; press accounts of the pandemic boom, the Great Pottery Throw Down effect and waitlists; 2023-2026 closure or sale news with reasons; municipal, university and makerspace staffing; kiln counts; Canadian and Australian statistics and charities; US Nonemployer Statistics.
- **Method limits.** Form 990 line 5 is a W-2 headcount, not FTEs. Part VII omits staff paid under $100,000. Membership dues are filed inconsistently (line 1b or line 2). Volunteer counts are estimates. The US nonprofit sample was mostly named in the brief and leans toward large, well-known organizations. NZ covers only clubs that are registered charities. UK Companies House counts are name-based. WebFetch summarizes pages and refuses quotes over 125 characters, so long passages are paraphrased and marked as such.
- **Side effect.** WebFetch automatically saved some binary responses (IRS e-file XML served as octet-stream, and one IRS PDF) to the session's tool-results folder under the user's .claude directory. I did not open those files.
- **Suggested follow-ups:** read Reddit and Facebook owner groups by hand; get a free Census API key for CBP and Nonemployer data on NAICS 611610 and 713990; pull Color Me Mine's FDD (Items 19 and 20) from a state franchise registry; read the Crafts Council "Market for Craft" PDF by hand; ABS CABEE spreadsheet for ANZSIC 8212 Arts Education; CRA T3010 open data for Canadian potters' guilds.
