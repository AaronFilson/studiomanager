# Studio site audit: the 42 US studios

_Audit started 2026-10-20. Scope: rows 1–42 of the per-studio table in [03-tools-in-use.md](../../03-tools-in-use.md) (every row whose "City, country" is US), in table order. The 42 Canadian, UK and Australian rows are audited separately with the same checklist, to be merged into one n = 84 audit._

_Method: for each studio, the home page plus up to eight linked pages (membership, classes, firing or kiln policy, FAQ, about or team, jobs, volunteer or work exchange, shop, policy or handbook) fetched with WebFetch, asking for exact sentences. Only the studio's own site, booking pages or shop pages count as evidence. Booking/commerce platform and firing fees are already in 03 and are not re-collected here._

## Coding key

- **Y** = Yes, stated on the studio's own site (URL + verbatim quote in the evidence section).
- **N** = No, the site explicitly says no.
- **NS** = Not stated on the pages checked. Not stated is not No.
- **UNR** = site or page unreachable (recorded, never inferred).
- **UNVERIFIED** = seen only in a search snippet or other text I could not open myself.
- Quotes are verbatim as returned by WebFetch. A `[...]` marks an omission I made. Anything not in quotation marks is my own summary.

Sub-item codes used in the summary table:
- **1 Size:** mem = member count or cap · wl = waitlist · k = kilns · w = wheels · area = studio area.
- **2 Staff:** titles shown; **ops** = someone other than the owner is named as running operations (Y/N/NS).
- **3 Work-exchange / assistant / monitor / volunteer program:** Y/N/NS.
- **4 Required member duties:** Y/N/NS.
- **5 Clay:** must = must buy from studio · incl = clay included in membership or classes · price · how = how bought.
- **6 Glazes:** prov = studio glazes provided/included · out = outside glazes allowed · mix = members mix glazes.
- **7 Retail:** sup = shop selling clay/tools/supplies · work = finished work for sale/consignment · gc = gift cards · online = online store.
- **8 Open studio:** res = reservations required (tool) · cap = capacity limits · access = staffed/monitored vs key/24 h.
- **9 Kilns:** load = who loads (staff/members) · freq = firing frequency or turnaround promise · req = how firing is requested · queue = queue or calendar published.
- **10 Events/rentals:** party = parties/private/team events · rent = studio or kiln rental · how = how booked.
- **11 Equipment list published:** Y/NS.
- **12 Member comms visible:** news = newsletter · portal · chat = Slack/Discord/WhatsApp/Facebook group · rules = rules/handbook document.

### Coding decisions (apply the same way when merging)

- **Segment:** one tag per studio. A stated or evident nonprofit is a nonprofit art centre, unless the site says members run it (then cooperative). 03's types were checked, not copied: Radius is tagged membership/community because its team page lists a "Studio Owner"; The Digs is nonprofit ("501(c)(3) ceramic studio"); row 24 is tagged by Ceramigos, the studio where Brave Ceramics teaches; row 42's booking pages are for GnarWare Workshop, Chicago.
- **2 ops = Y** only for a named person, or an explicit manager, director, coordinator or operations title, other than the owner or founder. Unnamed "techs", "technicians", front desk and instructors are recorded as roles but do not make ops Y.
- **3** is split into a programme tied to the ceramics studio and organisation-wide volunteering (gardens, galleries, docents). Paid jobs are not work-exchange; they are noted separately.
- **4** is split into shared duties (chores, rotations, co-op work, compulsory firing help) and "own cleanup only" (clean your own space).
- **5 must = N** when outside clay is allowed, including with approval. The conditions are noted.
- **7 sup = Y** when the studio sells clay, tools or supplies through any channel (shop, front desk, monitor, invoice). **online = Y** only for an online store selling goods; class, membership and gift-card checkouts do not count.
- **8 cap = Y** for any stated limit on open-studio users: per-session spots, "space is limited", or a membership cap. Access is grouped as 24/7 or key/code/card/badge; "unlimited access" with no mechanism; set hours only.
- **9 freq = Y** needs a concrete frequency or time, or an explicit "we fire when the shelves are full". "Regular" alone is NS.
- **11 = Y** when at least one item beyond wheels and tables is named (slab roller, extruder, kiln make or type, etc.).
- **12 portal = Y** for a described member or student portal, firing account or app, or an explicit "Members Login". A generic "Log In" link is NS.
- **MudFire (row 10):** only page titles that WebFetch returned count. Search-snippet material is UNVERIFIED and is not counted.

## Studio list (table order)

| # | Studio | City | Country | Segment (this audit) | 03 type | URL |
|---|---|---|---|---|---|---|
| 1 | BKLYN CLAY | Brooklyn & Manhattan | US | membership/community studio | M T | https://bklynclay.com/ |
| 2 | Powerhouse Arts Community Ceramics Studio | Brooklyn | US | nonprofit art centre | A M | https://powerhousearts.org/community-ceramics-studio |
| 3 | Gasworks NYC | Brooklyn | US | membership/community studio | M T | https://gasworksnyc.com/ |
| 4 | Choplet | Brooklyn | US | membership/community studio | M T | https://choplet.com/ |
| 5 | Centerpoint Ceramics | Brooklyn | US | membership/community studio | M | https://www.centerpointceramics.com/ |
| 6 | Still Life Ceramics | Los Angeles | US | membership/community studio | M T | https://still-life-studio.com/ |
| 7 | Claytivity | Los Angeles | US | membership/community studio | M T | https://www.claytivitypottery.com/ |
| 8 | Pottery Northwest | Seattle | US | nonprofit art centre | A M T | https://www.potterynorthwest.org/ |
| 9 | Northern Clay Center | Minneapolis | US | nonprofit art centre | A T | https://www.northernclaycenter.org/ |
| 10 | MudFire | Decatur GA | US | membership/community studio (content UNR) | M | https://www.mudfire.com/ |
| 11 | Callanwolde Fine Arts Center | Atlanta | US | nonprofit art centre | A T | https://callanwolde.org/ |
| 12 | Emerald City Fired Arts | Seattle | US | paint-your-own-pottery (PYOP) | P | https://www.emeraldcityfiredarts.com/ |
| 13 | The Digs | Chicago | US | nonprofit art centre (501(c)(3) ceramics studio) | A M | https://www.thedigschicago.com/ |
| 14 | ClaySpace | Lisle IL | US | cooperative (501(c)(3), member-run) | M | https://www.clayspace.net/ |
| 15 | Morning Ceramics | Portland OR | US | membership/community studio | M T | https://www.morningceramics.com/ |
| 16 | Fancy Banana | Portland OR | US | membership/community studio (closing) | M | https://fancybanana.studio/ |
| 17 | Fine Things | Portland OR | US | membership/community studio | M | https://finethingspdx.com/ |
| 18 | Radius Community Art Studio | Portland OR | US | membership/community studio (has a "Studio Owner") | A M | https://www.radiusstudio.org/ |
| 19 | St. Johns Clay Collective | Portland OR | US | membership/community studio | M | https://www.stjohnsclay.com/ |
| 20 | Greentree Art Studio | Portland OR | US | teaching studio | T | https://greentreeartstudio.com/ |
| 21 | Parker Art School | Parker CO | US | teaching studio | T M | https://parkerartschool.com/ |
| 22 | Feats of Clay | Austin | US | membership/community studio | M T | https://featsofclaypottery.com/ |
| 23 | Mud Studio | Austin & San Antonio | US | membership/community studio | M T | https://mudstudioceramics.com/ |
| 24 | Brave Ceramics at Ceramigos | Austin | US | membership/community studio (Ceramigos) | T | https://www.braveceramics.com/ + https://ceramigos-austin.com/ |
| 25 | Austin Clay Arts | Austin | US | membership/community studio | M T | https://www.austinclayarts.com/ |
| 26 | ATX Pottery (Austin Pottery) | Austin | US | membership/community studio | T | https://www.atxpottery.com/ |
| 27 | The Contemporary Austin Art School | Austin | US | nonprofit art centre (museum school) | A T | https://thecontemporaryaustin.org/artschool/ |
| 28 | Dougherty Arts School | Austin | US | municipal/recreation | C | https://www.austintexas.gov/dougherty/dougherty-arts-school |
| 29 | The Workshop Mpls | Minneapolis | US | membership/community studio (UNR; from 03) | M T | https://www.theworkshopmpls.com/ |
| 30 | The Bascom | Highlands NC | US | nonprofit art centre | A | https://www.thebascom.org/ |
| 31 | Color Me Mine Edina | Edina MN | US | paint-your-own-pottery (PYOP) | P | https://edina.colormemine.com/ |
| 32 | UWM Studio Arts & Craft Centre | Milwaukee | US | university/college/school | U | https://uwm.edu/union/craft-centre/ |
| 33 | UVM Ceramics Co-op | Burlington VT | US | university/college/school | U | https://www.uvm.edu/cas/soa/soa-ceramics-co-op |
| 34 | Miami University Art Center | Oxford OH | US | university/college/school | U | https://miamioh.edu/cca/art-center/membership.html |
| 35 | NWTC Artisan & Business Center | Green Bay WI | US | university/college/school | U | https://www.nwtc.edu/about-nwtc/nwtc-locations/artisan-and-business-center/open-studio |
| 36 | Plains Art Museum | Fargo ND | US | nonprofit art centre (museum) | A | https://plainsart.org/ |
| 37 | John Burrison Pottery Studio (Folk Pottery Museum) | Sautee GA | US | nonprofit art centre (museum) | A | https://www.folkpotterymuseum.com/ |
| 38 | Sonoma Community Center | Sonoma CA | US | nonprofit art centre | A M | https://sonomacommunitycenter.org/ |
| 39 | Art Alliance | Greensboro NC | US | nonprofit art centre | A T | https://www.artalliancegso.org/ |
| 40 | Hayden Center Pottery Co-Op | Hayden CO | US | municipal/recreation | C | https://www.haydencolorado.com/276/Pottery-Co-Op |
| 41 | Wayne Art Center | Wayne PA | US | nonprofit art centre | A T | https://wayneart.org/ |
| 42 | Recess Urban Recreation (listing for GnarWare Workshop) | Chicago | US | membership/community studio | M T | https://recessurbanrecreation.getgalore.com/ |

Segment counts: membership/community studio 19 (2 unreachable) · nonprofit art centre 12 · university/college/school 4 · teaching studio 2 · PYOP 2 · municipal/recreation 2 · cooperative 1 · production pottery 0 · makerspace 0. Total 42.

## Summary table (one row per studio)

| # | Studio | City | Country | Segment | URL | 1 Size | 2 Staff | 3 Work-exch | 4 Duties | 5 Clay | 6 Glazes | 7 Retail | 8 Open studio | 9 Kilns | 10 Events/rentals | 11 Equip list | 12 Comms |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | BKLYN CLAY | Brooklyn & Manhattan | US | membership/community studio | https://bklynclay.com/ | Y: mem 210 + 50; wl Y (15–18 mo); w 44 + 28; area 5,260 + 3,800 sq ft; k NS | Founder/Director/CEO; Operations Director/COO & CFO; Studio & Programming Dir.; Studio & Creative Dir.; Facilities Manager; Front of House (mgrs, coordinators, staff); Studio Techs; instructors. ops Y | NS (paid Studio Technician $22/h with firing credit + shelf; not a trade) | Own cleanup only | must N (outside clay with manager approval); incl Y (membership); price NS; how online shop | prov Y (in membership); out Y (approval); mix NS (stir only) | sup Y; work Y (BKLYN CLAY Made); gc Y; online Y | res NS (members 24/7; students sign in/out); cap NS; access 24/7 door code | load staff (tech duty); freq 7–10 days; req app (fire.bklynclay.com); queue NS (semester deadlines) | party Y (form, 48 h notice); rent Y kiln rental (email); | Y (wheels, slab roller, extruder, spray booth, 3D printer, rental kilns) | news Y; portal Y (firing system); chat NS; rules Y |
| 2 | Powerhouse Arts Community Ceramics Studio | Brooklyn | US | nonprofit art centre | https://powerhousearts.org/community-ceramics-studio | Y: k 4 electric + 2 gas; w 20 Shimpo + Brent; mem NS; wl NS; area NS | Community Ceramic Studio Manager; Ceramics Studio Director; Ceramics Studio Project Manager; Ceramics Studio Technician and Fabricator (5); Operations Manager; Board. ops Y | NS | NS | must N (own clay with tech approval); incl NS; price NS; how NS ("available for purchase") | prov NS; out Y (with firing info / approval); mix N (no raw materials) | sup Y (clay bodies); work Y (museum store artist work); gc NS; online Y | res NS; cap NS; access set hours 9–10 weekdays, 10–8 weekends | load staff; freq NS; req form (Google Form); queue NS | party NS (ceramics); rent Y (full kiln rental; venue rentals by inquiry form) | Y (detailed) | news Y; portal Y; chat NS; rules NS |
| 3 | Gasworks NYC | Brooklyn | US | membership/community studio | https://gasworksnyc.com/ | Y: wl Y (all memberships full); k 4 electric + 1 gas; mem NS; w NS; area NS | Founding director and co-owner; Co-owner and CFO; Assistant Director; Studio Manager (+ internship coordinator); Assistant Managers; Studio Technicians; facilities+materials manager; teachers. ops Y | NS (Internship coordinator title; "How to be a Studio Tech" is a paid class) | NS | must N (outside clay pre-approved by Director); incl Y (50 lb/mo Lite); price $27/25 lb; how online shop, studio pickup | prov Y (~60 glazes); out Y (approval); mix NS (recipes need approval) | sup Y; work Y (annual Holiday Sale); gc Y (Kiln Fire); online Y | res Y for Lite glazing sessions only; cap NS; access 24/7, staffed 12–10pm | load NS; freq weekly, ≥1 wk (10–14 days busy); req app (Kiln Fire photos); queue N ("do not always fire work at the same time") | party Y (email, PayPal invoice); rent NS | Y (partial: Shimpo wheels, slab roller, extruder, kilns 3 Skutt + 1 L&L + Bailey gas) | news Y; portal Y (Kiln Fire); chat NS; rules Y (handbook, community agreements) |
| 4 | Choplet | Brooklyn | US | membership/community studio | https://choplet.com/ | Y: k 5; w 33+ (Williamsburg), 24 (Greenpoint); mem NS; wl NS; area NS | No names; "On-Site Front Desk Support"; unnamed founder. ops NS | NS | Own cleanup only | must N ("strongly recommend"; proof of purchase for outside clay); incl N (drop-in) / NS (members); price NS; how online shop or in person | prov Y (40–50 house glazes); out Y (proof of purchase + cone); mix NS | sup Y; work Y (gallery incl. "Instructor and Member Work"; commissions); gc Y; online Y (no shipping of clay) | res Y for 2-h drop-in slots (members NS); cap NS; access 24 h add-on after 2-month probation; front desk | load NS (rental loading fee $60/h); freq daily; 10 + 10 days, rush 48–72 h; req online firing system + front-desk firing slips + Firing Card; queue NS | party NS; rent Y kilns (inquiry form/phone) | Y (partial: slab roller, 2 extruders, spray booth, 24 Shimpo wheels, 5 kilns) | news NS; portal Y (account / firing system); chat NS; rules NS |
| 5 | Centerpoint Ceramics | Brooklyn | US | membership/community studio | https://www.centerpointceramics.com/ | Y: k 1 (L&L E23T); mem NS; wl NS; w NS; area NS | None (one instructor named, no title). ops NS | NS | NS (members page locked) | must N (outside clay reviewed); incl Y classes (unlimited studio clay), NS members; price NS; how in studio "24/7" | prov Y (membership); out Y (reviewed); mix NS | sup Y (clay); work NS; gc NS; online NS (cart sells classes/memberships) | res Y (Google sheet sign-out); cap NS; access 24 h, all members have keys | load NS; freq NS; req contact page; queue NS | party Y (up to 10; online reserve + email confirm); rent Y (whole kiln, contact) | Y (partial: slab roller, extruder, photo area, L&L kiln) | news Y (form shows "needs a storage option"); portal Y (password-protected For Members page, 401); chat NS; rules NS |
| 6 | Still Life Ceramics | Los Angeles (5 locations) | US | membership/community studio | https://still-life-studio.com/ | NS (one 24 cu ft gas kiln described; no counts) | No titles; "staff on duty"; firing "team". ops NS | NS | NS | must NS; incl N members ("additional costs ... clay"), Y beginner classes; price e.g. $25 (B-Mix 5, size NS); how online shop | prov Y (50+ glazes); out NS; mix NS | sup Y; work Y (member/instructor/staff work collection, 0 items listed); gc Y; online Y | res Y (time slots via Student Portal); cap Y ("Space is limited"); access 24/7 most tiers; staff on duty | load staff (firing service); freq weekly; 14-day guarantee (service); req online drop-off reservation; queue Y (firing tracking system) | party Y (inquiry form); rent Y (gas kiln full/half/quarter) | Y (partial: Shimpo wheels, extruder, slab roller, spray booth) | news Y; portal Y (Student Portal); chat NS; rules NS |
| 7 | Claytivity | Los Angeles (3 locations) | US | membership/community studio | https://www.claytivitypottery.com/ | Y: w 16 + 8 + 12 member wheels (+ 8 teaching wheels per site); classes max 8–10; mem NS; k NS; area NS | NS. ops NS | NS | NS | must NS; incl Y classes (12.5 lb); members "Clay prices vary"; price NS; how NS | prov Y ("Multiple types of clay and glazes"); out NS; mix NS | sup Y (clay sold: "Clay prices vary"); work NS; gc Y ($195, online); online NS | res NS; cap NS; access "24 hours a day, 7 days a week controlled access" | load NS; freq NS; req NS; queue NS (studio may "limit the number of pieces fired") | party NS; rent NS | Y (partial: wheel counts, slab rollers, extruder) | news NS; portal NS (bare "Log In" link); chat NS; rules NS |
| 8 | Pottery Northwest | Seattle | US | nonprofit art centre | https://www.potterynorthwest.org/ | Y: area 8,000 sq ft; ~2,000 community members a year; k 9 electric; w 34; wl Y | Executive Director; Programs & Exhibitions Dir.; Studio Manager; Asst. Studio Manager; Finance & Operations Specialist; Education Manager; Studio Technicians (2); board. ops Y | Y (volunteer sign-up form; no hours/trade/duties stated) | Own cleanup only (incl. mop around your area) | must Y (policy: "Clay must be purchased from Pottery Northwest"); incl N (membership); price NS; how online student store (code emailed) | prov Y; out Y (staff review); mix NS | sup Y (students only); work NS (non-commercial gallery); gc Y; online Y (code-gated) | res NS; cap NS; access 10am–9pm daily, building codes after orientation | load NS; freq NS (bi-monthly luster firings); req NS; queue NS | party Y (inquiry form, $1,000 min, $500 deposit, ≤15); rent N (no whole-studio buyout) / kiln NS | Y (9 kilns, 34 wheels, Bailey slab roller, 2 extruders, Laguna spray booth) | news NS; portal NS; chat NS; rules Y (policies + community guidelines) |
| 9 | Northern Clay Center | Minneapolis | US | nonprofit art centre | https://www.northernclaycenter.org/ | Y: w 44; k types (gas, soda, electric), count NS; wl Y (shared artist studio); private studios 95–210 sq ft; mem NS | Executive Director; Operations & Development Mgr; Facilities Mgr; Studio Manager; Kiln & Facilities Technician; Materials Technician; Education Mgr; Registrar; Sales Gallery staff; Sales Gallery & Volunteer Coordinator. ops Y | Y (volunteer shifts: studio cleaning, gallery inventory; booked through the Shopify cart; perks NS) | NS | must NS; incl Y (classes: 25 lb); price "up to $25" per 25 lb; how in person ("may be purchased at NCC") | prov Y ("basic glaze materials"); out NS; mix NS (glaze-mixing room listed) | sup Y (tool kits, clay); work Y (Sales Gallery, online); gc Y ($25–$500); online Y | res N ("not required to sign up"); cap NS; access set student hours (to midnight Tue/Thu); studio artists 24 h | load NS (students) / artists fire own; freq NS; req work "ON THE CART"; queue Y (kiln-unload updates posted) | party Y (email; team-building, birthdays); rent Y (juried artist studios); kiln NS | Y (partial) | news Y; portal NS; chat NS; rules Y (registration policies, Student Guide) |
| 10 | MudFire | Decatur GA | US | membership/community studio | https://www.mudfire.com/ | UNR (UNVERIFIED snippet: waitlist) | Owners Deanna and Daphne (page title). ops UNR | UNR (UNVERIFIED snippet: "Rangers" residency in exchange for studio access) | UNR | UNR | UNR (UNVERIFIED: 60–70+ house glazes) | gc Y (page title "Gift cards at MudFire"); work UNR (a "Pottery Gallery" page exists); sup UNR; online UNR | UNR (UNVERIFIED: 57 open hours/week) | UNR (UNVERIFIED: 2,500 in³ glaze allowance) | UNR | UNR (UNVERIFIED: Brent/Shimpo wheels, slab roller) | UNR |
| 11 | Callanwolde Fine Arts Center | Atlanta | US | nonprofit art centre | https://callanwolde.org/ | NS ("one of the largest in the Southeast", no figures) | Ceramics Program Director; 18 Pottery Instructors; Operations Director; Facilities & Maintenance Director; Rentals & Hospitality Coordinator. ops Y | Y org-level only (gardens, kids' camps; nothing for ceramics) | NS | NS | NS | gift certificates by email (Y); sup NS; work NS; online NS | NS | NS | party NS (ceramics); rent Y (venue/weddings via Event Inquiry Form); kiln NS | NS | news Y; portal NS; chat NS; rules NS |
| 12 | Emerald City Fired Arts | Seattle | US | paint-your-own-pottery (PYOP) | https://www.emeraldcityfiredarts.com/ | NS | NS ("professional instruction"). ops NS | NS | NS | must NS; incl Y (project price includes "all materials"); price NS; how NS | prov Y (80+ glazes); out NS; mix NS | sup NS; work NS; gc Y (in store, mailed); online NS | res N (walk-ins welcome; events reserved); cap NS; access staffed hours noon–6 Tue–Sun | load staff; freq ready in 7 days; req n/a (staff fire all); queue NS | party Y (birthdays, office/corporate; $50 deposit; phone/email); rent Y (space for meetings and after-hours events, phone) | NS | NS |
| 13 | The Digs | Chicago | US | nonprofit art centre (501(c)(3) ceramic studio) | https://www.thedigschicago.com/ | Y: mem "over 100"; area 6,000 sq ft; wl Y; private studios avg 64 sq ft; k NS; w NS | Co-Founder/Facilities Manager; Co-Founder/Program Coordinator; Studio Manager; Studio Tech; Arts Administrator; Studio Monitor; critique facilitator. ops Y | Y (internship program, "currently full"; details NS) | Own cleanup only (residents) | must NS; incl NS; price $2/lb; how NS | prov Y (12+ glazes; residents free firing + glazes); out Y (approval); mix NS | sup Y (clay); work Y (member markets); gc NS; online NS | res Y (day passes booked online; "no walk-ins"); cap NS; access residents 24/7 | load NS; residents "Schedule Personal Kilns"; freq NS; req NS; queue NS | party Y (online "BOOK NOW", phone/text); rent Y (kiln rental, terms NS) | Y (minimal: wheels, slab roller, tables) | news Y; portal NS; chat NS; rules NS |
| 14 | ClaySpace | Lisle IL | US | cooperative (501(c)(3) nonprofit, member-run) | https://www.clayspace.net/ | Y: mem cap 55; wl Y; area 9,000 sq ft; k 10 (2 gas, 5 electric, 2 test, 1 raku); w 22 | NS (no staff or officers named). ops NS | Y (internships, "not accepting ... at this time") | Y (residents run the enterprise; tasks NS) | NS | NS (glaze area listed) | work Y (annual Holiday Market); sup NS; gc NS; online NS | res NS; cap 55 members; access unlimited (residents); students 8 open-lab sessions | NS | party Y (custom classes on request); rent NS | Y (detailed) | portal Y ("Members Login"); news NS; chat NS; rules NS |
| 15 | Morning Ceramics | Portland OR | US | membership/community studio | https://www.morningceramics.com/ | Y: area 5,000 sq ft; k 5 Skutt 1227 (+ luster kilns); w NS; mem NS; wl NS | Founders named, no titles; "small & resilient team". ops NS | N ("not offering any work trade opportunities currently"); paid Studio Technician, rolling | NS | must NS; incl N (billed monthly); price NS; how monthly invoice | prov Y (house glazes); out Y (if cone 6 safe); mix NS | sup Y (clay billed on invoice; merch); work Y (vendor markets); gc Y; online Y (merch) | res optional (booking system or drop-in); cap NS; access 24/7 members, staffed Wed–Sun 10–5 | load staff; freq up to 13 firings/week, ready 3 days after unload; req NS; queue NS | party Y (form, custom quote); rent NS | Y (5 Skutt 1227, extruder, slab roller, wedging tables) | news Y; portal NS; chat NS; rules NS |
| 16 | Fancy Banana | Portland OR | US | membership/community studio (closing notice: "closing at the end of September") | https://fancybanana.studio/ | NS ("a small community of artists") | NS (names in captions, no roles). ops NS | NS | NS | NS | NS | NS | res NS; cap NS; access 24/7, largely independent | load members ("fire your own kilns (we’ll teach you!)"); freq NS (only "regular opportunities for gas and raku firing"); req NS; queue NS | NS | NS ("professional equipment") | news Y (form "needs a storage option"); portal NS; chat NS; rules NS |
| 17 | Fine Things | Portland OR | US | membership/community studio (with retail storefront and production home) | https://finethingspdx.com/ | NS | NS. ops NS | NS | NS (cleaning covered at orientation) | must NS; incl NS; price "at cost"; how NS | prov Y (6+ Clay Art Center glazes); out NS; mix NS | sup Y (art supplies, clay at cost); work Y (members may sell in storefront); gc NS; online Y (Shopify gift shop) | res NS (guest pass reservable $40/day); cap NS; access 24/7 | load NS; freq NS; req NS; queue NS (no outside firing) | NS | Y (partial: wheels, slab roller, extruder, Giffin grip) | news Y; portal NS; chat NS; rules NS (orientation required) |
| 18 | Radius Community Art Studio | Portland OR | US | membership/community studio (team page lists a "Studio Owner") | https://www.radiusstudio.org/ | Y: mem 70+; spots limited; wl ~3 months; area 3,200 sq ft; k types (cone 6 electric, cone 10 gas); w NS | Studio Owner; Studio Manager; Glaze Technician; Studio Technicians (4); Studio Assistants (2); Art Educators. ops Y | NS (jobs page: instructor only) | NS | must N (outside clay OK after staff check); incl NS; price NS ("discounted"); how in-house | prov Y (20+ in-house glazes, in firing cost); out Y (staff check); mix NS | sup Y (clay in-house); work Y (consignment shop 60/40 members; markets); gc Y; online NS | res N (first-come, first-served; punch-card blocks); cap Y (monthly membership spots limited); access 24/7 monthly members (key fob); studio assistants on duty | load certified members fire own; freq monthly community cone 10; req wait-list form (cone 10); queue NS | party Y (private group class product; email); rent N (no private workspace) | Y (minimal) | news Y (+ weekly members-only newsletter); portal NS; chat NS; rules NS |
| 19 | St. Johns Clay Collective | Portland OR | US | membership/community studio | https://www.stjohnsclay.com/ | Y: wl Y (membership units); unit 4'×18"×6'; others NS | Founder; "artist, instructor and program coordinator"; studio monitor; Open Studio Monitor; kiln master; volunteer helpers. ops Y | NS (volunteers mentioned, no program) | Y (kiln-yard firings: help load, unload, clean) | must NS; incl N (members supply own materials); price NS; how online Square store or from studio monitor | prov Y (house glazes, in firing fee); out Y (cone 6); mix NS | sup Y (clay, basic tools); work NS; gc Y; online Y (Square) | res NS (sign-up page linked, not checked); cap NS; access members all day every day; open hours with monitors | load staff (first come, first served) + participants at kiln-yard firings; freq weekly bisque and cone 6; req firing slip + measuring cubby + pay online; queue Y (kiln-yard firing events listed) | party Y (team building, Fridays 1–3); rent Y (whole kiln by email; space by sq ft) | Y (Shimpo wheels, extruder, slab roller, tables; soda and raku kilns) | news Y (+ kiln-yard list); portal NS; chat Y (kiln master's firing "thread"); rules Y (user and membership agreements) |
| 20 | Greentree Art Studio | Portland OR | US | teaching studio (home studio) | https://greentreeartstudio.com/ | Y: k 1 (Skutt 1227); w 7; home lower level; mem NS | Founder (no title) + unnamed teachers. ops NS | NS | NS | must N (studio or Georgies); incl N (students buy own bag); price NS; how at studio | prov Y (shared pool: "Students contribute one pint of glaze per month"); out NS; mix NS | sup Y (clay at studio); work NS; gc Y (online); online NS | res Y (text the founder to schedule); cap NS; access Sunday 9am–9pm open studio | NS | party Y (4–12, $70 pp, online cart); rent NS | Y (minimal) | news Y (form "needs a storage option"); others NS |
| 21 | Parker Art School | Parker CO | US | teaching studio (multi-media art school with a pottery membership) | https://parkerartschool.com/ | NS | Owners Sadi and Kyle (teachers) + teacher Molly. ops N ("family owned and operated") | NS | NS | must NS; incl Y (membership 25 lb); price NS; how NS | prov Y ("Unlimited Glaze usage included"); out NS; mix NS | NS | res NS; cap NS; access "during open hours" | load NS; freq NS; req NS; queue NS | party Y (birthday, corporate, bridal; contact form); rent NS | NS | news Y (monthly coupons); others NS |
| 22 | Feats of Clay | Austin | US | membership/community studio | https://featsofclaypottery.com/ | Y: wl Y ("may be a waitlist"); w 7 in open-studio room; others NS | No titles; "techs" mentioned; instructors by first name; "woman-owned and operated". ops NS | NS | NS | must NS; incl N (students buy own for independent study); price $25/25 lb; how from studio | prov Y (membership includes "glaze usage"); out NS; mix NS | sup Y (clay); work Y (Staff & Member Holiday Market, gallery, seconds sale); gc Y (physical + digital); online Y (merch, donations) | res Y (self-scheduling on member site; phone for sliding-scale hours); cap NS; access 9–9 standard, 24 h tier | load NS; freq 1–2×/week when full; 1–2 weeks; req measured + paper Kiln Firing Log; queue NS | party NS (public fundraisers only); rent NS | NS (wheels and tables only) | news Y; portal Y (member site); chat NS; rules Y (student guide) |
| 23 | Mud Studio | Austin & San Antonio | US | membership/community studio | https://mudstudioceramics.com/ | Y: w 8 (members' room); dedicated members' kiln (Skutt); mem NS; wl NS; area NS | Owner (founder); Studio Manager; Instructors (6). ops Y (Studio Manager; owner also "manages ... day-to-day operations") | NS (jobs page: instructor only) | NS | must Y ("Only clay purchased at Mud Studio can be used and fired"); incl N; price $18–25/bag (firing + glaze bundled); how at studio | prov Y (in clay price); out NS; mix NS | sup Y (clay); work NS; gc Y (classes); online NS | res NS; cap NS; access set member hours | load NS; freq NS; pickup 2–3 weeks; req NS; queue NS | party Y (private classes 4–20); rent NS | Y (minimal: slab roller, extruder, Skutt kilns) | news Y (per-city lists); others NS |
| 24 | Brave Ceramics at Ceramigos | Austin | US | membership/community studio (Ceramigos; Brave is a production potter teaching there) | https://www.braveceramics.com/ + https://ceramigos-austin.com/ | Y: w 4 members' + 10 class-area + 2 resident wheels; others NS | Brave: "owner & founder". Ceramigos: unnamed staff, instructors, front desk. ops NS | Y (Ceramigos residency: ~24 h/week, 6-month minimum; space, 24/7 access, free non-production firing; teaching paid) | NS | must Y for open-studio passes ("No outside clay or glazes"), members may use other cone 6 clay with authorization; incl NS; price NS; how at studio | prov Y (members); out N (open studio) / approval (members); mix NS | sup Y (clay); work Y (Brave's work via boutiques, markets, custom orders; site shop empty); gc Y; online NS | res Y (members' site, up to 7 days ahead; open-studio packages); cap NS; access business hours (members), 24/7 (residents), front-desk check-in | load staff ("Only our staff is allowed to operate the kilns"); freq NS; classes 3–4 weeks; req Kiln Fire (03); queue NS | party Y (corporate team building via contact form; Ceramigos private events page); rent NS | NS | news Y; portal Y (members' site / Kiln Fire login); chat NS; rules Y ("Studio Guidelines" page linked) |
| 25 | Austin Clay Arts | Austin | US | membership/community studio | https://www.austinclayarts.com/ | Y: wl Y (membership "sold out"); others NS | Founder; unnamed instructors. ops NS | NS | NS | must NS; incl Y ("All materials and firing included"); price NS; how NS | prov Y (same "all materials" quote; glazes not itemised); out NS; mix NS | sup NS ("We don’t have a rotating shop"); work Y ("occasional market days"); gc NS; online NS | res NS; cap Y ("LIMITED SPACE AVAILABLE"); access 24/7/365 (members); open hours for students | NS | NS | NS | news Y; others NS |
| 26 | ATX Pottery (Austin Pottery) | Austin | US | membership/community studio (with street-front gallery) | https://www.atxpottery.com/ | Y: wl Y (classes full, wait list); others NS | Generic only: "instructor or studio employee", "kiln loaders", "management". ops NS | NS | Own cleanup only | must Y ("All clay used in the studio must be purchased from Austin Pottery"); incl N (clay price covers glaze + firing); price NS; how NS | prov Y ("Use only the glazes and washes provided"); out approval only; mix NS | sup Y (clay); work Y (gallery of local artists); gc NS; online NS | res NS; cap Y (members only "if it is not full with student use"); access member hours 10–9 Mon–Thu; sign-in station | load staff ("kiln loaders"); freq NS; req NS (unmarked work not fired); queue NS | party Y (up to 3 days/month reserved for corporate events); rent NS | NS | rules Y (studio policy + agreement); others NS |
| 27 | The Contemporary Austin Art School | Austin | US | nonprofit art centre (museum art school) | https://thecontemporaryaustin.org/artschool/ | NS | Faculty bios only (one "studio assistant and teacher"); no admin titles. ops NS | Y (Art School volunteers; "not currently accepting") | NS | must N (outside cone 10 clay OK); incl NS; price NS; how in person at museum gift shop (10% off code) | NS | sup Y (clay in museum shop); work NS; gc NS; online Y (museum shop) | NS | load NS; freq NS; req NS; queue NS ("We will not fire other clays" than cone 10) | party NS (ceramics); rent Y (venue rentals via inquiry form) | NS | news Y (museum list); others NS |
| 28 | Dougherty Arts School (Dougherty Arts Center) | Austin | US | municipal/recreation | https://www.austintexas.gov/dougherty/dougherty-arts-school | NS | Studio Technician (named); instructor bios. ops Y | NS | NS | NS | NS | NS | NS | NS | party N ("does not rent space for ... birthdays"); rent Y (theater, free "creative spaces"; no studio/kiln rental) | NS | NS |
| 29 | The Workshop Mpls | Minneapolis | US | membership/community studio (per 03 type M T; not verifiable) | https://www.theworkshopmpls.com/ | UNR | UNR | UNR | UNR | UNR | UNR | UNR | UNR | UNR | UNR | UNR | UNR |
| 30 | The Bascom | Highlands NC | US | nonprofit art centre | https://www.thebascom.org/ | Y: area 2,500 sq ft barn; "Limited availability"; others NS | Director of Ceramics; Ceramics Artist-Educator; Executive Director; Facilities Mgr; Retail Mgr; board officers. ops Y | Y (members may assist at large workshops; general volunteer form) | NS | must NS; incl Y ("provides clay, glaze, and firings for members"); price NS; how NS | prov Y; out NS; mix NS | sup NS; work Y (shop of local/regional artists); gc NS; online NS (shipping on request) | res NS; cap Y ("Limited availability"); access from 9 am 7 days + summer evenings; Director approval required | load NS; freq NS; ABA projects 2–3 weeks; req NS; queue NS | party Y (private Art-by-Appointment sessions, web form); rent Y (terrace venue; no studio rental) | NS | news Y; others NS |
| 31 | Color Me Mine Edina | Edina MN | US | paint-your-own-pottery (PYOP), franchise | https://edina.colormemine.com/ | NS | "Ceramic Studio Team Member" (job ad: POS, processing pottery, unboxing inventory). ops NS | NS | NS | n/a (PYOP); NS | prov Y (unlimited paint colours); out NS; mix NS | gc Y (CardFoundry); sup NS; work NS; online NS | res N (walk-ins all day); cap NS; access store hours | load NS (staff "Processing customer pottery"); freq NS; req n/a; queue NS | party Y (online "Book a party"; large groups); rent Y (private studio rental $500, min 15, 18% gratuity) | NS | news NS ("Studio News" blog); others NS (policies page empty) |
| 32 | UWM Studio Arts & Craft Centre | Milwaukee | US | university/college/school | https://uwm.edu/union/craft-centre/ | Y: opens only with ≥35 memberships; others NS | "Ceramics Technicians" (unnamed). ops NS (no named manager or coordinator) | NS | NS | must NS; incl NS; price $15 student / $20 community per 25 lb; how NS | NS | sup Y (clay); work NS; gc NS; online NS | res NS; cap NS; access Mon–Thu 10am–9pm; technicians advise | NS ("Special Firing" $35 in price table) | NS for the centre (Union-wide event services exist) | NS ("basic equipment") | NS |
| 33 | UVM Ceramics Co-op | Burlington VT | US | university/college/school | https://www.uvm.edu/cas/soa/soa-ceramics-co-op | Y: cap 11 wheel + 6 hand-building spots per block; others NS | Studio Manager (named). ops Y | NS | NS (member agreement PDF unreadable) | must NS; incl Y ("Everything you need is included"); price n/a; how n/a | prov Y (included); out NS; mix NS | NS | res Y (blocks; Friday sign-up sheet); cap Y (per-block spots); access blocks; manager absent at late/weekend blocks | NS (firings included) | NS | Y (minimal: wheels, slab roller, coil press) | news Y; rules Y (member agreement PDF); portal NS; chat NS |
| 34 | Miami University Art Center | Oxford OH | US | university/college/school | https://miamioh.edu/cca/art-center/membership.html | Y: w 12; electric kilns (count NS); memberships "limited"; others NS | NS (staff directory did not render). ops NS | NS | NS (must sign "safety and facility use agreement") | must Y ("You must buy your clay from the Art Center"); incl N (firing included, clay bought); price NS; how NS | prov Y; out NS; mix NS | work N ("NOT for production or sales"); sup Y (clay sold by the Art Center); gc NS; online NS | res NS; cap Y (memberships limited, first-come); access open lab hours; "a monitor must be present" at certain times | load NS; freq NS; req NS; queue NS ("reasonable amount" of firing included) | NS | Y (minimal: wheels, rolling press, molds, kilns) | rules Y (facility use agreement); others NS |
| 35 | NWTC Artisan & Business Center | Green Bay WI | US | university/college/school (technical college community education) | https://www.nwtc.edu/about-nwtc/nwtc-locations/artisan-and-business-center/open-studio | NS | Ceramics Studio Coordinator; Manager, Community Education; Education Coordinator; Office Assistants. ops Y | NS | NS | NS | NS | sup Y ("select art supplies available for purchase onsite"); work NS; gc NS; online NS | res NS; cap NS; access staffed hours + badge access after certification ($30 appointment) | load NS; freq NS; req NS; queue Y (kiln activity logs published; "Inclusion in a firing is not guaranteed") | NS (Studio Scoop orientation meetings) | NS (certification required per area) | news Y (quarterly + per-rotation); rules Y (live Airtable policies); portal NS; chat NS |
| 36 | Plains Art Museum | Fargo ND | US | nonprofit art centre (museum) | https://plainsart.org/ | NS | Clay & Studio Program Manager; Director of Learning & Engagement; COO; Facilities Supervisor; Events Manager. ops Y | Y org-level (museum program volunteers; none ceramics-specific) | NS | must NS; incl NS ("clay, firing ... may have additional fees"); price NS; how NS | prov Y; out NS; mix NS | NS (Square shop linked, not checked) | res NS (Sawyer drop-in schedule linked from home page); cap NS; access key card during Open Studio hours; orientation required | NS | party NS (ceramics); rent Y (museum spaces; inquiry) | NS ("Shared studio tools and equipment") | news Y; others NS |
| 37 | John Burrison Pottery Studio (Folk Pottery Museum) | Sautee GA | US | nonprofit art centre (museum) | https://www.folkpotterymuseum.com/classes | Y: mem cap 10; w 9; k 2 | Studio Coordinator (unnamed); "staff members". ops Y | Y org-level (museum docents; none for studio) | NS (members prepare and label own pieces) | must Y ("Outside clay and glazes are not permitted"); incl N; price $50 / 25 lb (glaze + firing bundled); how NS | prov Y (studio dipping glazes); out N; mix NS | sup Y (clay); work NS; gc Y (Kiln Fire, any amount over $10); online NS | res optional ("encouraged to reserve ... using the studio mobile app"); cap Y (10 members); access set sessions, all staffed | load staff ("staff members handle all loading and firing"); freq NS; req Kiln Fire app + labelled shelf; queue NS | NS | Y (minimal: 9 wheels, 2 kilns, glazing room) | portal Y (Kiln Fire app); rules Y (provided on approval); news NS; chat NS |
| 38 | Sonoma Community Center | Sonoma CA | US | nonprofit art centre (community center) | https://sonomacommunitycenter.org/ | Y: w 16; kiln types (gas, electric, raku, soda), count NS; mem NS; wl NS; area NS | Director of Ceramics; Ceramics Studio Coordinator; Studio Technician and Instructor; "12 studio monitors on rotation"; Director of Operations & Finance; Facilities Caretakers. ops Y | Y org-level (events, classrooms, galleries; none ceramics-specific) | Own cleanup only (membership qualification) | must Y ("must be purchased through SCC" unless the Director permits); incl NS; price NS; how from monitor on the floor | prov Y (30+ in-house glazes); out NS; mix Y ("Raw materials lab for custom glaze mixing") | sup Y (monitors "sell materials"); work NS; gc NS; online NS | res NS (rolling enrollment); cap NS; access Mon–Sat 10–8; 40 of 60 hours monitored | NS (home-studio members bring work "for professional firing") | party NS; rent Y (community-centre spaces) | Y (wheels, Peter Pugger, slab roller, extruder, spray booth, kiln types) | news Y (weekly); others NS |
| 39 | Art Alliance | Greensboro NC | US | nonprofit art centre (board-governed art school) | https://www.artalliancegso.org/ | NS | Executive Director; Ceramics Director; Program Assistant; board officers. ops Y | Y (volunteers staff the pottery sale: "assisting customers, completing transactions"; interest form) | NS | NS | NS | work Y (Creative Clayworks Pottery Sale); sup NS; gc NS; online NS (shop empty) | res NS; cap NS; access "During Open Studio" (eligibility: recent class) | NS | NS | NS | rules Y ("current studio guidelines", not published); others NS |
| 40 | Hayden Center Pottery Co-Op | Hayden CO | US | municipal/recreation (town-run; "co-op" in name only on this page) | https://www.haydencolorado.com/276/Pottery-Co-Op | NS | "studio manager" referenced (unnamed). ops Y | NS | NS | must N (outside clay with studio manager approval); incl Y (25 lb in first month); price $40/bag; how invoiced to Amilia account | prov Y (50+ studio glazes); out NS; mix NS | sup Y (clay); work Y (sell at town events); gc NS; online NS | res NS; cap NS; access 7 days a week | load NS; freq "when our shelves are getting full"; limit 20 pieces/month; req NS; queue NS | NS (members-only workshops) | NS | news Y (town newsletter); others NS |
| 41 | Wayne Art Center | Wayne PA | US | nonprofit art centre | https://wayneart.org/ | NS | Faculty pages only; jobs list "Youth Art Program Manager", "Evening & Weekend Registrar". ops NS | Y org-level (reception desk, data entry, gallery shop, events) | NS | NS (in "Ceramic Studio Guidelines", not checked) | NS | work Y (Gallery Shop of local/regional artists; annual Pottery Sale); gc Y (front office); sup NS; online NS | res NS; cap NS; access: open studio for enrolled members only | NS ("refer to the Ceramic Studio Guidelines") | party Y (ceramic birthday parties $405 ≤15, email/phone; ceramics team building, inquiry form); rent Y (facilities) | NS | news Y; rules Y ("Ceramic Studio Guidelines" referenced); others NS |
| 42 | Recess Urban Recreation (listing is for GnarWare Workshop) | Chicago (per provider page; 03 had no city) | US | membership/community studio | https://recessurbanrecreation.getgalore.com/providers/671-gnarware-workshop | NS | Instructors by name, no titles; "approved by staff". ops NS | NS | NS ("Cleaning day - Studio closed 4-8pm" listed; member role not stated) | must NS; incl N; price $2.50/lb incl. firing + glaze ($3 porcelain); how NS | prov Y (15 house glazes, in clay price); out NS; mix NS | sup Y (clay sold per lb); work NS; gc NS; online NS | res N ("First come first serve"); cap NS; access 24/7, reduced during classes | load NS; turnaround ~2 weeks per kiln load; req NS; queue NS | party Y (private parties, wheel room; booked on Galore); rent NS | NS | rules Y (membership terms: "a rental agreement"); others NS |

## Totals per item (n = 42 US studios; 40 reachable)

Every row sums to 42. UNR = the site's content could not be read: row 10 (MudFire, script-rendered) and row 29 (The Workshop Mpls, certificate error). MudFire's gift-card page title is the one verified MudFire item. Read every NS as "not found on the pages checked", never as "no".

| Item | Question | Y | N | NS | UNR | Detail |
|---|---|---|---|---|---|---|
| 1 Size | Any size figure stated | 25 | 0 | 15 | 2 | |
| 1a | Member count or cap | 8 | — | 32 | 2 | BKLYN 210 + 50; Pottery NW ~2,000 served a year; The Digs 100+; ClaySpace cap 55; Radius 70+; UWM opens only at ≥35; UVM 17 spots per block; John Burrison cap 10 |
| 1b | Waitlist | 11 | — | 29 | 2 | 10 for membership or studio space, 1 for classes only (ATX). BKLYN: 15–18 months; Radius: ~3 months |
| 1c | Kiln count | 9 | — | 31 | 2 | 1 to 10 kilns |
| 1d | Wheel count | 14 | — | 26 | 2 | 7 to 44 (+28 at BKLYN Tribeca) |
| 1e | Studio area | 7 | — | 33 | 2 | 2,500–9,000 sq ft; BKLYN 5,260 + 3,800 |
| 2a Staff | What the site shows | — | — | — | 2 | Named team with non-owner titles 17 · owner or founder only 4 · roles without names (or names without titles) 14 · nothing 5 |
| 2b | Someone other than the owner named as running operations | 19 | 1 | 20 | 2 | N = Parker ("family owned and operated") |
| 3 Work-exchange | Programme offered | 13 | 1 | 26 | 2 | Tied to the ceramics studio 7 (Pottery NW volunteer form; NCC studio-cleaning volunteers; Digs and ClaySpace internships; Ceramigos residency; Bascom workshop assistants; Art Alliance pottery-sale volunteers) · organisation-wide only 6 · N = Morning. **None publishes a trade of work hours for membership** (MudFire "Rangers" UNVERIFIED) |
| 4 Duties | Required member duties | 8 | — | 32 | 2 | Shared duties 2 (ClaySpace co-op; St Johns firing crew) · own cleanup only 6. **No studio publishes a cleaning rota or co-op hours quota** |
| 5a Clay | Must buy clay from the studio | **6** | 10 | 24 | 2 | Y: Pottery NW, Mud, ATX, Miami, John Burrison, Sonoma (unless the Director permits). Of the 10 N, 8 allow outside clay only with approval, review or proof of purchase |
| 5b | Clay included | 12 | 11 | 17 | 2 | In membership 7 · in classes only 4 · PYOP 1 |
| 5c | Price per bag or box stated | 10 | — | 30 | 2 | $15–$50 per 25 lb; $2–$2.50/lb. 4 fold firing (and glaze) into the clay price: Mud, ATX, John Burrison, GnarWare |
| 5d | How clay is bought, stated | 14 | — | 26 | 2 | Online shop 6 · in person, front desk or monitor 8 · billed to a monthly invoice or account 2 (Morning; Hayden via Amilia). Honour log 0 (Centerpoint: "for purchase 24/7 in the studio", method unstated) |
| 6a Glazes | Studio glazes provided or included | 30 | — | 10 | 2 | Greentree's is a pool students stock themselves |
| 6b | Outside glazes allowed | 12 | 1 | 27 | 2 | All 12 attach conditions (approval, review, proof, cone match). N = John Burrison; Ceramigos also bans them for open-studio passes |
| 6c | Members mix glazes | 1 | 1 | 38 | 2 | Y = Sonoma raw-materials lab; N = Powerhouse. 4 say their glazes are made in house (Choplet, Radius, Sonoma, GnarWare) |
| 7a Retail | Sells clay, tools or supplies (any channel) | 27 | 1 | 12 | 2 | Online supply shop 7 (BKLYN, Gasworks, Choplet, Still Life, Pottery NW, Fine Things, St Johns). N = Austin Clay Arts |
| 7b | Finished work for sale or consignment | 19 | 1 | 20 | 2 | Member markets or sales 11; consignment terms published 1 (Radius). N = Miami |
| 7c | Gift cards | 20 | — | 21 | 1 | Separate gift-card systems: Kiln Fire (Gasworks, John Burrison), CardFoundry (Color Me Mine) |
| 7d | Online store selling goods | 12 | — | 28 | 2 | |
| 7 | Any retail item | 36 | — | 5 | 1 | |
| 8a Open studio | Reservations required | 9 | 5 | 24 | 2 | **+ 2 optional (Morning, John Burrison) = 42.** Y includes partial (some tiers or passes). N = explicitly first come or walk-in (NCC, Emerald City, Radius, Color Me Mine, GnarWare). Tools: Google Sheet, paper sign-up sheet, text message, phone, Student Portal (BookThatApp), "member site", Kiln Fire app, online day passes |
| 8b | Capacity limits | 9 | — | 31 | 2 | |
| 8c | Access | — | — | 5 | 2 | 24/7 or key, code, card or badge 20 (17 say 24/7) · "unlimited access", mechanism not stated 2 · set hours only 13 |
| 8d | Explicit staffed or monitored hours statement | 12 | — | 28 | 2 | |
| 9a Kilns | Who loads | — | — | 28 | 2 | Staff only 8 · members or mixed 4 (NCC studio artists, Fancy Banana, Radius certified members, St Johns firing crew) |
| 9b | Frequency or turnaround stated | 14 | — | 26 | 2 | "ready to pick up 3 days later" (Morning) to "3 -4 weeks" (Brave classes); 1 guarantee (Still Life, 14 days) |
| 9c | How firing is requested, stated | 13 | — | 25 | 2 | + 2 PYOP n/a. App or online system 5 · form 2 · paper slip or log 3 · shelf, cart or marking 3 · contact 1 · drop-off reservation 1 (overlaps) |
| 9d | Firing queue or status published | 4 | 1 | 35 | 2 | Y: Still Life tracking system, NCC unload notices, St Johns firing events, NWTC kiln logs. N: Gasworks "We do not always fire work at the same time." |
| 10a Events | Parties, private or team events | 21 | 1 | 18 | 2 | Booking: inquiry form 8 · online self-booking 6 · email or phone only 3 · not stated 4. N = Dougherty (no birthdays) |
| 10b | Studio, space or kiln rentals | 17 | 2 | 21 | 2 | Kiln rental 7 · venue or space 11 (Powerhouse both). N = Pottery NW (no whole-studio buyout), Radius (no private workspace) |
| 11 Equipment | List published | 21 | — | 19 | 2 | 7 name kiln makes (Skutt, L&L, Bailey, Blaauw, Fredrickson) |
| 12a Comms | Newsletter | 27 | — | 13 | 2 | 3 sign-up forms show the builder notice "This newsletter signup form needs a storage option." |
| 12b | Member portal or app | 10 | — | 30 | 2 | 5 are firing systems or Kiln Fire |
| 12c | Slack, Discord, WhatsApp or Facebook group | 1 | — | 39 | 2 | Only a firing "thread" (St Johns); no named platform |
| 12d | Rules, handbook or agreement | 15 | — | 25 | 2 | |
| 12 | Any channel visible | 35 | — | 5 | 2 | |

## Totals by segment

Counts of Y per segment. M = membership/community studio (19, of which 2 UNR) · NP = nonprofit art centre (12) · U = university/college/school (4) · T = teaching studio (2) · PYOP (2) · Muni = municipal/recreation (2) · Co-op = cooperative (1). Production pottery and makerspace: 0 studios.

| Indicator | M (19) | NP (12) | U (4) | T (2) | PYOP (2) | Muni (2) | Co-op (1) | All (42) |
|---|---|---|---|---|---|---|---|---|
| Any size figure | 13 | 7 | 3 | 1 | 0 | 0 | 1 | 25 |
| Waitlist | 7 | 3 | 0 | 0 | 0 | 0 | 1 | 11 |
| Non-owner named as running operations | 5 | 10 | 2 | 0 | 0 | 2 | 0 | 19 |
| Work-exchange or volunteer programme tied to the studio | 1 | 5 | 0 | 0 | 0 | 0 | 1 | 7 |
| Organisation-wide volunteering only | 0 | 6 | 0 | 0 | 0 | 0 | 0 | 6 |
| Required duties (shared or own cleanup) | 4 | 3 | 0 | 0 | 0 | 0 | 1 | 8 |
| Must buy studio clay | 2 | 3 | 1 | 0 | 0 | 0 | 0 | 6 |
| Clay included | 6 | 2 | 1 | 1 | 1 | 1 | 0 | 12 |
| Studio glazes provided | 16 | 7 | 2 | 2 | 2 | 1 | 0 | 30 |
| Outside glazes allowed (conditional) | 9 | 3 | 0 | 0 | 0 | 0 | 0 | 12 |
| Sells clay or supplies | 15 | 7 | 3 | 1 | 0 | 1 | 0 | 27 |
| Sells finished work | 11 | 6 | 0 | 0 | 0 | 1 | 1 | 19 |
| Gift cards | 12 | 5 | 0 | 1 | 2 | 0 | 0 | 20 |
| Online store (goods) | 8 | 4 | 0 | 0 | 0 | 0 | 0 | 12 |
| Open-studio reservations required | 6 | 1 | 1 | 1 | 0 | 0 | 0 | 9 |
| Capacity limit | 4 | 2 | 2 | 0 | 0 | 0 | 1 | 9 |
| 24/7 or key/code/card/badge access | 15 | 4 | 1 | 0 | 0 | 0 | 0 | 20 |
| Staff-only kiln loading | 5 | 2 | 0 | 0 | 1 | 0 | 0 | 8 |
| Firing frequency or turnaround stated | 11 | 1 | 0 | 0 | 1 | 1 | 0 | 14 |
| Firing request method stated | 10 | 3 | 0 | 0 | 0 | 0 | 0 | 13 |
| Firing queue or status published | 2 | 1 | 1 | 0 | 0 | 0 | 0 | 4 |
| Parties or private events | 11 | 5 | 0 | 2 | 2 | 0 | 1 | 21 |
| Studio, space or kiln rentals | 5 | 9 | 0 | 0 | 2 | 1 | 0 | 17 |
| Equipment list | 11 | 6 | 2 | 1 | 0 | 0 | 1 | 21 |
| Newsletter | 13 | 9 | 2 | 2 | 0 | 1 | 0 | 27 |
| Member portal or app | 7 | 2 | 0 | 0 | 0 | 0 | 1 | 10 |
| Rules, handbook or agreement | 7 | 5 | 3 | 0 | 0 | 0 | 0 | 15 |

Segment pattern in one line: membership studios publish the operational detail (firing turnaround and requests, 24/7 access, retail); nonprofits publish staff structures, volunteers and venue rentals but little about how the studio runs; university, municipal and PYOP sites publish almost nothing operational.

## Notable verbatim examples

Staffing and who does the work
- BKLYN CLAY job ad, Studio Technician, "$22/hr": "Mix and replenish clay" · "Load and unload kilns" · "Unload clay deliveries, keep stock organized" · "They will receive $100 month in firing fees and shelf space for storage." (https://bklynclay.com/jobs)
- Sonoma: "12 studio monitors on rotation" · "40 of our 60 weekly hours are monitored – a team member is on the floor to answer questions and sell materials." (https://sonomacommunitycenter.org/ceramics/ceramics-open-studio-program/)
- Mud Studio: "Lindsay manages all aspects of the business—from day-to-day operations and scheduling to marketing and social media." (https://mudstudioceramics.com/austinteam)
- Morning Ceramics: "We are not offering any work trade opportunities currently." (https://www.morningceramics.com/faq)
- Northern Clay Center, volunteers: "The Rest of the Year volunteers help us keep the studios clean and functional, evaluate gallery inventory," · "Then, "check out" as you would to buy a pot or register for a class." (https://www.nccshop.org/pages/volunteering-at-ncc)
- Color Me Mine Edina job ad: "Running the POS system (handling payments)" · "Unboxing the inventory, shelving, bagging and general upkeep of the studio." (https://edina.colormemine.com/we-are-hiring/)

Clay and materials as the unit of charge
- Mud Studio: "Only clay purchased at Mud Studio can be used and fired in our studio." · "Firing and glaze fees are incorporated into the cost of clay, our classes and memberships." (https://mudstudioceramics.com/faq)
- ATX Pottery: "The clay price includes an additional amount that helps covers the materials and labor costs of the glazes and firings." (https://www.atxpottery.com/studio-policy)
- Morning Ceramics: "Firing fees and clay are added to your invoice at the end of each month." (https://www.morningceramics.com/faq)
- Hayden: "Additional clay can be purchased for $40/bag to be invoiced on your Amilia account" (https://www.haydencolorado.com/276/Pottery-Co-Op)
- Choplet: outside work is fired if you "provide proof of purchase and exact cone reference for the clay and the glazes you intend to use" (https://choplet.com/faq/)
- Pottery Northwest: "All registered students have been emailed the code for the studio store." (https://www.potterynorthwest.org/store)

Firing: requests, status and fairness
- Choplet: "IF YOU REQUIRE RUSH FIRING, YOU MUST INFORM THE FRONT DESK WHEN YOU PRINT THE FIRING SLIPS." · "Do not call us to find out if your work has been fired." (https://choplet.com/firing-services-kiln-rentals/)
- Feats of Clay: "Once work is measured and charged accordingly, you will sign our Kiln Firing Log." (https://featsofclaypottery.com/studio-space-firing-services)
- St. Johns: "Go to our online store: CLAY STORE and add the proper denominations needed into your cart to pay the fee" · "If your email and order number is not on the slip, we can not put your work in the kiln." (https://www.stjohnsclay.com/curbside-firing-spring-2020)
- Still Life: "Firing tracking system - know when your glazed piece is loaded and unloaded from the kiln!" (https://still-life-studio.com/pages/ceramics-studio-memberships)
- Radius: work "will be ready for pick up one calendar month after your workshop date" and "You will not be emailed/notified" (https://www.radiusstudio.org/contact-us/)
- NWTC: "Inclusion in a firing is not guaranteed and may not be reflected immediately." (https://www.nwtc.edu/about-nwtc/nwtc-locations/artisan-and-business-center/open-studio)
- Ceramigos: "Only our staff is allowed to operate the kilns." (https://ceramigos-austin.com/team) · "Larger pieces and repetitive items may be deprioritized by our staff depending on demand." (https://ceramigos-austin.com/membership)
- Gasworks: "Our studio management system Kilnfire asks students and members to take pictures" · "We do monthly purges on the last Monday of every month." (https://gasworksnyc.com/pages/glazing-firing)

Access, scheduling and certification
- Centerpoint: "all members have keys" · "access to a Google sheet to sign out time." (https://www.centerpointceramics.com/memberships)
- John Burrison: "Members will be encouraged to reserve work space times using the studio mobile app." (https://www.folkpotterymuseum.com/open-studio-application)
- NWTC: "Eligible members may access the studio outside of staffed hours after completing required certification and badge setup." · "Certification appointments are $30" (open studio and schedule pages)
- Pottery Northwest: "New members must attend an in-person orientation session that covers studio policies and procedures prior to being assigned a shelf and given building access codes." (https://www.potterynorthwest.org/membership)
- Choplet Greenpoint: "24-hour studio access available after completing a two-month probationary period." (https://choplet.com/studio-rental/choplet-memberships/choplet-greenpoint-membership/)

Selling work and events
- Radius: "All items are on consignment at a 50/50 split for non-members and a 60/40 split for members." (https://www.radiusstudio.org/sell-at-our-retail-shop/)
- Gasworks: "Holiday Sale (bringing in annually 600+ people to celebrate you all) and other events." (https://gasworksnyc.com/pages/studio-membership) · parties: "we will send a confirmation email as well as a PayPal invoice, and a google calendar invite." (https://gasworksnyc.com/pages/parties)
- Color Me Mine Edina: "A 18% gratuity will be added to the entire party total for your party host" (https://edina.colormemine.com/private-studio-rental/)

Fragility
- Fancy Banana: "Fancy Banana is closing at the end of September." (https://fancybanana.studio/)
- UWM: "The Studio Arts and Craft Centre will open over the summer of 2026, if we receive a minimum of 35 purchased memberships." (https://uwm.edu/union/craft-centre/)
- Centerpoint, Fancy Banana and Greentree newsletter forms: "This newsletter signup form needs a storage option."

## Per-studio evidence

_Each studio's block lists the pages checked, then items 1–12 with code, URL and verbatim quote._

### 1. BKLYN CLAY (Brooklyn & Manhattan, US) — membership/community studio

Pages checked (2026-10-20): https://bklynclay.com/ · /firing · /membership · /rules · /team · /jobs · /privates · /kiln-rentals · /shop

1. **Size — Y.** https://bklynclay.com/membership — "In Prospect Heights, we have 210 members in our 5,260 square foot space with 44 wheels." · "In Tribeca, we have 50 members in our 3,800 square foot space with 28 wheels," · "BKLYN CLAY’s membership in Prospect Heights is currently full, those who join the waitlist can expect a wait of 15 - 18…" Kiln count NS (kiln-rentals page lists four rentable kiln types, no total).
2. **Staff — Y; ops Y.** https://bklynclay.com/team — titles include "Founder/Director/CEO" (Jennifer Waverek), "Operations Director/COO & CFO" (Laura Vogel), "Studio & Programming Director", "Studio & Creative Director", "Production & Creative Director | Instructor", "Facilities Manager", "Front of House Assistant Manager" (2), "Front of House Coordinator" (2), "Front of House" (7), "Studio Tech" (about 12), "Instructor", "Private Lesson Instructor". Gustav Hamilton: "He is a Studio Manager at BKLYN CLAY". Rules page: "Dry sanding requires approval from studio managers (Anders, Gustav, or Sarah),"
3. **Work-exchange / assistant / volunteer — NS.** https://bklynclay.com/jobs — no trade program; paid role instead: Studio Technician "$22/hr"; "Studio Technicians working at least 20 hours per week" get studio access; "They will receive $100 month in firing fees and shelf space for storage." Duties: "Mix and replenish clay", "Load and unload kilns", "Unload clay deliveries, keep stock organized", "Clean facilities thoroughly". Apply: "Please email your application materials here."
4. **Required member duties — own cleanup only.** https://bklynclay.com/rules — "Clean up your work area when done working" · "Wedge all clay and return it to the appropriate bucket before leaving the studio." No rotation or work hours stated.
5. **Clay — must N (approval); incl Y; price NS; how online shop.** https://bklynclay.com/membership — "Traditional Memberships include 24/7/365 access to studio facilities, including all of the studio equipment…" "…clay, glazes, and either a private shelf (approximately 4 feet long x 18 inches deep x 18 inches tall) for $300/month…" · "The only additional costs are firing fees at 4 cents per cubic inch per firing and specialty clays (like porcelain)." https://bklynclay.com/rules — "OUTSIDE CLAY & GLAZES can be used only with the approval of studio managers (Anders and Gustav)." Home page shop tile: "Clay from 25lbs. to Bulk orders" (no price returned).
6. **Glazes — prov Y; out Y (approval); mix NS.** Membership quote above ("…clay, glazes, and…"). Rules: outside glazes with approval (quote above); "You must mix glazes before use." (stirring, not batch mixing). Membership: "Each studio has different glazes; members may fire at the member rate at both studios;"
7. **Retail — sup Y; work Y; gc Y; online Y.** https://bklynclay.com/shop — categories "Clay Shop", "Tools", "Brushes", "Amaco Glaze", "Mayco Glazes"; "BKLYN CLAY Made" items "Saturn Dinnerware," "No-Spill Mugs," "Tumblers". Home: "Gift certificates for any activity or amount." Consignment NS.
8. **Open studio — res NS; cap NS; access 24/7 door code (members), sign-in (students).** https://bklynclay.com/rules — "Members have 24/7/365 use of the studio." · "Members MAY NOT give their door code to anyone else, nor let anyone into the space outside of open studio hours." · "Students MUST sign in and out when using open studio hours," · "Both studios have the same open studio hours: Monday-Thursday 9:30am-9:30pm"
9. **Kilns — load staff; freq Y; req app; queue NS.** https://bklynclay.com/firing — "Most work is completed in 7-10 days" · "Please measure and submit your work for firing via" fire.bklynclay.com. https://bklynclay.com/rules — "You must register for the firing fee system at fire.bklynclay.com in order to fire work at the studio." · "must submit work prior to the firing deadlines defined each semester". Loading: Studio Technician duty "Load and unload kilns" (jobs page).
10. **Events/rentals — party Y; rent Y (kilns); how form/email.** https://bklynclay.com/privates — "Kids or adults can enjoy a hands-on event guided by a teaching artist who leads the group in a private 2 hour class/party." · "Up to 12 people, wheel and/or handbuilding $1250" · "To book a private lesson or event, please submit the form below and we will get back to you ASAP". https://bklynclay.com/kiln-rentals — "Individual kiln rental is available to members, but must be scheduled in advance." · "Fredrickson Front Loader Kiln": "$550 for members" / "$1100 for outside" · "For inquiries, please email info@bklynclay.com"
11. **Equipment list — Y.** https://bklynclay.com/membership — "including all of the studio equipment (wheels, vertical slab roller, extruder, ceramic 3D printer," · "at our Tribeca location we do not have a vertical slab roller, 3D printer, spray booth, or photo booth". Kiln-rentals page lists four kilns with cubic inches and dimensions.
12. **Comms — news Y; portal Y; chat NS; rules Y.** Home/firing: "Sign up to receive news and updates." Firing account at fire.bklynclay.com. https://bklynclay.com/rules — "The BKLYN CLAY Student Agreement can be accessed by clicking here."

### 2. Powerhouse Arts Community Ceramics Studio (Brooklyn, US) — nonprofit art centre

Pages checked: https://powerhousearts.org/community-ceramics-studio (membership + FAQ on one page) · /team · /rentals · /careers · https://store.powerhousearts.org/

1. **Size — Y (equipment counts only).** Studio page — "(4) Electric Kilns" · "(2) Front Loading Gas Kilns" · "(20) Shimpo VL Whisper Wheels," (plus Brent wheels). Member count, cap, waitlist, area NS.
2. **Staff — Y; ops Y.** https://powerhousearts.org/team — "Community Ceramic Studio Manager" (Evan Lewis), "Ceramics Studio Director", "Ceramics Studio Project Manager", "Senior Ceramics Fabricator", "Ceramics Studio Technician and Fabricator" (3), "Jr. Ceramics Studio Technician and Fabricator" (2), "Operations Manager", "Operations Coordinator", "Visitor Services Coordinator" (4), "Board Chair", "Board Members".
3. **Work-exchange — NS.** https://powerhousearts.org/careers lists only "Production Manager, Live Programs and Rentals" and "Executive Assistant"; no volunteer or trade program.
4. **Required member duties — NS.**
5. **Clay — must N; incl NS; price NS; how NS.** Studio FAQ — "Can I use my own clay and glaze?" "Yes, but all clays and glazes must first be approved by our technicians." · "No, we do not make clay on premises." · section heading "Clay bodies available for purchase" · "Firing comes at an additional cost."
6. **Glazes — prov NS; out Y; mix N.** Studio FAQ — "Can I use my own commercial glaze?" "Yes, you can use your own commercial glazes as long as you provide us with the firing information." · "Can I use the studio to make my own glazes?" "No. Members do not have access to the raw materials in the glaze lab." · "Please speak to a tech about having glaze tests made."
7. **Retail — sup Y (clay); work Y; gc NS; online Y.** Clay "available for purchase" (above). https://store.powerhousearts.org/ sells artist work, e.g. ceramic "Beans" at "$295" each.
8. **Open studio — res NS; cap NS; access set hours.** Studio page — "Monday – Friday, 9AM – 10PM" · "Saturday – Sunday, 10AM – 8PM" · "Closed Between 12/25 – 1/1 & Federal Holidays". No 24 h access stated.
9. **Kilns — load staff; freq NS; req form; queue NS.** Studio FAQ — "Our technicians will be loading all the kilns, but members are welcome to observe." · "If a member would like to load their own work, they must arrange this with a studio technician to schedule a time." Firing is requested through a linked "firing form" (Google Form, see 03).
10. **Events/rentals — party NS (ceramics); rent Y; how inquiry form.** Studio page — "Full kiln rental" (section heading) · "Locker rental is also available as an add on for $25/month." https://powerhousearts.org/rentals (whole-venue hire) — "To inquire about booking an event in one of our spaces, get started below."
11. **Equipment list — Y.** Studio page lists Skutt and Blaauw kilns with sizes, Shimpo and Brent wheels, "(1) 40” Wide Electric Slab Roller", "(1) 30” Wide Manual Slab Roller", pugmill, North Star and Bailey extruders, "Glazing Spray Booth". "Technicians and fabricators can work with artists one on one by appointment" ("$120/hour").
12. **Comms — news Y; portal Y; chat NS; rules NS.** Studio page links a "member portal" (https://ccs.powerhousearts.org/) and a newsletter page (https://powerhousearts.org/newsletter).

### 3. Gasworks NYC (Brooklyn, US) — membership/community studio

Pages checked: https://gasworksnyc.com/ · /pages/studio-membership · /pages/glazing-firing · /pages/faq · /pages/community-agreements · /pages/how-to-be-a-studio-tech · /pages/about · /pages/parties · /products/clay

1. **Size — Y (waitlist, kilns).** /pages/studio-membership — "All memberships are currently filled." "Apply for the waitlist for all memberships below." /pages/glazing-firing — "We are equipped with 4 electric kilns (3 Skutt and 1 L+L), and one Bailey gas kiln." Member count, wheel count, area NS.
2. **Staff — Y; ops Y.** /pages/about — "Founding director and co-owner" (Cor Garcia-Held), "Co-owner and CFO", "Assistant Director, Teacher", "Studio Manager, Internship coordinator, Teacher" (Ester Sang Hee Kwon), "Assistant Manager, Teacher" (2), "Studio Technician, facilities+materials manager, Teacher", "Studio Technician" (several, incl. "(PT)" and "(Floater)"), "Media Coordinator, Studio Technician, Teacher", "Teacher". "As Studio Manager, she is also responsible for ensuring the studio is filled to the brim with delicious clay."
3. **Work-exchange — NS.** No program page found; an "Internship coordinator" title exists (about page). /pages/how-to-be-a-studio-tech is a class, not a job: "This class does not come with studio access" · "students will learn how to load and program kilns, reclaim clay, mix glazes, perform glaze tests, and work with plaster."
4. **Required member duties — NS.** /pages/community-agreements — "we ask you to care of the space as well." (no chores or hours).
5. **Clay — must N; incl Y; price $27 / 25 lb; how online shop + studio pickup.** /pages/studio-membership — "Membership includes: 50 lbs of clay a month, (choose from our 4 clay bodies)" (Membership Lite). /pages/glazing-firing — "Any and all outside clay bodies, materials, glazes, kiln furniture, etc. must be pre-approved by our Director." /products/clay — "25 lb block of clay in various colors." · "Regular price $27.00" · "Pick-up at our studio only (we do not ship)."
6. **Glazes — prov Y; out Y (approval); mix NS.** /pages/glazing-firing — "Gasworks has almost 60 Cone 6 and Cone 10 glazes" · outside glazes pre-approved (quote above) · "All glaze recipes must be pre-approved by the teacher or Director."
7. **Retail — sup Y; work Y; gc Y; online Y.** Shop menu: "Gift Cards" (links to gasworksnyc.kilnfire.com/gift-card), "Terra Sigillata," "Clay," "Merch". /pages/studio-membership — "Holiday Sale (bringing in annually 600+ people to celebrate you all) and other events." Consignment NS.
8. **Open studio — res Y (Lite tier glazing sessions only); cap NS; access 24/7, staffed 12–10pm.** /pages/studio-membership — "Unlimited open studio time 24/7 days a week at no extra cost." · "A full-service and full-staffed studio from 12-10pm." · "Member Lites have the ability to book their 5 hours/week glazing sessions 24/7." /pages/faq — "Staff are working in the studio every day 12pm-10pm."
9. **Kilns — load NS; freq Y; req app; queue N.** /pages/glazing-firing — "We fire a large volume of work every week" · "We tell folks to wait at least a week before looking for your piece after submission." · "Our studio management system Kilnfire asks students and members to take pictures" · "We do not always fire work at the same time." · "We do monthly purges on the last Monday of every month." /pages/faq — "it may be 10-14 days before you see your piece come out of the kiln."
10. **Events/rentals — party Y; rent NS; how email.** /pages/parties — "Throw your next birthday party or company outing with the help of a teaching artist and a little mud." · "Parties include a 2-hour event for up to 10 participants and one finished piece per student." · "10 people: $1050" · "To schedule a private event, email hello@gasworksnyc.com with the following information:" · "we will send a confirmation email as well as a PayPal invoice, and a google calendar invite."
11. **Equipment list — Y (partial).** /pages/studio-membership — "Access to the studio’s equipment, including: shimpo wheels, handbuilding tables, slab roller, extruder, plaster molds." Kilns as in item 1.
12. **Comms — news Y; portal Y; chat NS; rules Y.** "Sign up for our newsletter." (Mailchimp link). Footer "Kilnfire" login. /pages/faq — "A lot of answers to questions can be found in the Student/Member Handbook." "Community Agreements" page.

### 4. Choplet (Brooklyn, US) — membership/community studio

Pages checked: https://choplet.com/ · /studio-rental/choplet-memberships/ · /studio-rental/choplet-memberships/choplet-greenpoint-membership/ · /faq/ · /firing-services-kiln-rentals/ · /about/ · /shop-choplet/shop-tools-supplies/ · /shop-choplet/shop-gallery/ · /studio-rental/drop-in/

1. **Size — Y (equipment counts).** /studio-rental/choplet-memberships/ — "over 33 wheels in the indoor and outdoor spaces" · "5 kilns". Greenpoint page — "24 Shimpo VL-Whisper Wheels, 6 communal hand building tables, a slab roller". About — "Williamsburg and Greenpoint locations in Brooklyn." Member count, waitlist, area NS.
2. **Staff — minimal; ops NS.** No names or titles on /about/ (only an "OUR FOUNDER" link label). Greenpoint membership lists "On-Site Front Desk Support". FAQ: "Please refer to the front desk hours listed on the Contact page of our website."
3. **Work-exchange — NS.**
4. **Required member duties — own cleanup only.** /studio-rental/drop-in/ — "Members of our shared community clean up after themselves." · "Clean-Up Service: $20 per drop in."
5. **Clay — must N; incl N for drop-ins, NS for members; price NS; how online or in person.** /faq/ — "We strongly recommend that you purchase your cone 5-6 clay or slip from the studio" · "We have 12 different cone 5-6 clay bodies available for purchase". /firing-services-kiln-rentals/ — "If you bring pieces made with non-studio clay, you must provide proof of purchase". /studio-rental/drop-in/ — "Purchase your clay here or in person when you arrive." Shop: "The studio does not ship tools, clay, plaster or glazes and there is no refund or return available once purchased."
6. **Glazes — prov Y; out Y (conditions); mix NS.** Memberships page — "40 – 50 housemade glazes". /faq/ — the studio will "maintain an inventory of 40 to 50 cone 5-6 glazes at all times"; outside work fired if you "provide proof of purchase and exact cone reference for the clay and the glazes you intend to use".
7. **Retail — sup Y; work Y; gc Y; online Y.** /about/ — "ceramic tools are available for purchase at all times." Shop lists "Clay", "All Tools", "Glazes", "Pottery Plaster", "Firing Card – Choplet Williamsburg", "Gift Cards". /shop-choplet/shop-gallery/ sections "Williamsburg Current Exhibition" and "Instructor and Member Work". About — "Choplet Gallery hosts a new sculptural and/or functional ceramic show several times a year." Menu: "Commissioned Work". Consignment terms NS.
8. **Open studio — res Y (drop-in slots only); cap NS; access 24 h add-on after probation.** Greenpoint page — "24-hour studio access available after completing a two-month probationary period." · "One (1) Shelf: Unlimited access during open Bench Time, 1 Personal Studio Shelf, Additional Separate Clay Storage". /studio-rental/drop-in/ — "Rescheduling is not permitted within 12 hours of your session." · "No instruction is provided; participants should be comfortable working independently." FAQ — "you can register for a 2 hour Drop In time slot ($50) plus the cost of clay and firing."
9. **Kilns — load NS; freq Y; req online system + slips; queue NS.** /faq/ firing frequency — "Every day." /firing-services-kiln-rentals/ — "Please come back to the studio 10 days after dropping your piece to be bisque fired" · "You should expect another 10 days for your piece to go through the glaze firing." · "You must have an account to use our Firing Fee System." · "IF YOU REQUIRE RUSH FIRING, YOU MUST INFORM THE FRONT DESK WHEN YOU PRINT THE FIRING SLIPS." · "YOUR PIECES WILL BE PLACED IN A DEDICATED AREA TO ENSURE THEY GET IN THE NEXT KILN BEING LOADED, AS LONG THEY ARE DRY." · "Do not call us to find out if your work has been fired." Rental: "Loading fee will be charged at $60/per hour."
10. **Events/rentals — party NS; rent Y (kilns); how form/phone.** /firing-services-kiln-rentals/ — "We have several different kiln sizes to fit any size load or budget." · "Kiln #4: 31 W x 41 L x 25.5 H $360" · "Fill out our quick inquiry form and someone will get back to you shortly or call us at 917-547-8316 to discuss."
11. **Equipment list — Y (partial).** Memberships page — "a slab roller, 2 extruders, a spray booth," and "5 kilns"; Greenpoint wheels quote above; Greenpoint "Includes 1 mandatory, instructor-led Spray Booth session designed to ensure safe and skillful use of studio equipment."
12. **Comms — news NS; portal Y (account/firing system); chat NS; rules NS.** "My Account" link; firing account quote in item 9. Facebook and Instagram pages only.

### 5. Centerpoint Ceramics (Brooklyn, US) — membership/community studio

Pages checked: https://www.centerpointceramics.com/ · /memberships · /firing-services · /about · /for-members (HTTP 401, password-protected) · /classes · /contact · /news

1. **Size — Y (kiln only).** /firing-services — "The studio homes a 6.3 cubic foot L&L E23T that's available for firing up to cone 6." Member count, waitlist, wheels, area NS. /about — "Centerpoint Ceramics is a community-centered, member based pottery studio located in Bushwick, Brooklyn."
2. **Staff — minimal; ops NS.** No staff names or titles on /about or /memberships. /classes — Mug Workshop "Taught by Serina James." (no title).
3. **Work-exchange — NS.**
4. **Required member duties — NS.** The members' page (/for-members) returned HTTP 401, so any rules there are unverified.
5. **Clay — must N (review); incl Y in classes, NS in membership; price NS; how in studio.** /memberships — "multiple clays for purchase 24/7 in the studio." /firing-services — outside clay: "the studio will need to review your clay purchase". /classes — "Will have your own shelf in the studio to store clay, tools, and projects and unlimited studio clay." · "The course fee includes unlimited studio clay and house glazes." Day pass excludes "clay (which can be purchased at the studio)".
6. **Glazes — prov Y; out Y (review); mix NS.** /memberships — "studio glazes (cone 5/6)" (listed as included). /firing-services — "All non-studio glazes must be reviewed by the studio."
7. **Retail — sup Y (clay only); work NS; gc NS; online NS.** Clay quote in item 5. No shop, gift card or consignment found; the cart sells classes and memberships (platform in 03).
8. **Open studio — res Y (Google Sheet); cap NS; access 24 h with keys.** /memberships — "unlimited 24 hour access to the studio" · "all members have keys" · "access to a Google sheet to sign out time." /classes — open studio "$25 per hour ($30 per hour for nights and weekends) with a two hour minimum." /contact — "(by appointment only)".
9. **Kilns — load NS; freq NS; req contact; queue NS.** /firing-services — "Please reach out via our contact page for more information" · "Rent out the whole kiln or drop off individual pieces." (home).
10. **Events/rentals — party Y; rent Y (kiln); how online reserve + email / contact.** /classes — "Or book a party for up to 10 people!" · "Upgrade to a party for groups of 7 to 10 people - includes 2 instructors" · "Please note workshops and parties are not officially booked until you receive an email confirmation from the studio." /firing-services — "Standard bisque or glaze firing - $230 per fire for non-members and $195 per fire for members, plus load fee" · "to set up a time to rent the kiln". /about — "small private events".
11. **Equipment list — Y (partial).** /memberships — "basic tools, slab roller, extruder, and photo area"; kiln in item 1.
12. **Comms — news Y; portal Y (locked page); chat NS; rules NS.** "Sign up with your email address to receive news and updates." The same block shows the template notice "This newsletter signup form needs a storage option." (the form may not be wired up). "For Members" page is password-protected (401).

### 6. Still Life Ceramics (Los Angeles, US; also DTLA, Ferndale MI, Richmond VA, Silver Spring MD) — membership/community studio

Pages checked: https://still-life-studio.com/ · /pages/ceramics-studio-memberships · /pages/faqs · /pages/santa-monica-membership-application · /pages/private-parties · /products/firing-service-santa-monica · /collections/member-work-created-at-still-life-studio · /pages/company · /collections/midfire-clay. Several pages ended with "Access Denied: You are not allowed to access this page", but their main content came through.

1. **Size — NS.** No member, wheel or kiln counts or area. Firing-service page describes one kiln: "Our 24 cubic feet gas kiln measures 32" wide, 32" deep, 40" tall and is rentable at full, half, and quarter capacities."
2. **Staff — no titles; ops NS.** /pages/ceramics-studio-memberships — "Experienced, friendly staff on duty". Firing service — "Our dedicated team, with over seven years of experience in loading and firing,". /pages/company names no one.
3. **Work-exchange — NS.**
4. **Required member duties — NS.**
5. **Clay — must NS; incl N (members), Y (beginner classes); price e.g. $25; how online shop.** /pages/santa-monica-membership-application — "The only additional costs are tools, clay, and glaze firing fees at 2 cents per cubic inch" · "Members must provide their own tools and bats." /pages/faqs — "Our beginning classes come with everything you'll need: clay, firing" · "We do have different types of clay available for purchase if you'd like to try another kind." /collections/midfire-clay — "B-Mix 5" – $25.00; "Dark Horse" – $28.00 (bag size not stated); "Shipping, taxes, and discount codes calculated at checkout."
6. **Glazes — prov Y; out NS; mix NS.** Membership pages — "Comprehensive palette of 50+ glazes, decorating slips, and oxide stains".
7. **Retail — sup Y; work Y; gc Y; online Y.** Home — "Shop a variety of mid and high fire clays, trimming tools, storage caddies, pottery ribs, bats, and more." Member-work collection — "Shop handmade ceramics made by members, instructors, and staff at our Santa Monica studio." (showed "0 products"). Menu: "Gift Cards", "Wholesale". Membership perk: "Discounts on classes, workshops, in-house supply store, and kiln rentals".
8. **Open studio — res Y; cap Y; access 24/7 (most tiers).** /pages/faqs — "Space is limited, so don't forget to sign up for a time slot in advance." · "Most membership types offer 24/7 studio access." Booking via "STUDENT PORTAL" link (BookThatApp calendar, per 03).
9. **Kilns — load staff (service); freq weekly; req online reservation; queue Y (tracking).** Membership page — "kilns are fired weekly" · "Firing tracking system - know when your glazed piece is loaded and unloaded from the kiln!" Firing service — "We guarantee a 14-day turnaround." · "Please choose your _temperature_, _kiln space_, and make a _Drop-off Reservation_." · "Drop off days are Friday and Saturday - you must schedule ahead of time." /pages/faqs — "For Bowl in One and Happy Hour classes, turnaround time is about three weeks."
10. **Events/rentals — party Y; rent Y (kiln); how inquiry form / online.** /pages/private-parties — "Private classes are offered at all of our locations: DTLA Arts District, Santa Monica, Ferndale MI, and Richmond VA." · "We can accommodate groups both small and large!" · "Fill in the form below to inquire, and we look forward to having you celebrate with us!" Kiln rental: see item 1.
11. **Equipment list — Y (partial).** Membership page — "State-of-the-art equipment: Shimpo Whisper wheels, extruder, slab roller, and spray booth".
12. **Comms — news Y; portal Y; chat NS; rules NS.** Home — "Join our newsletter and be the first to know about new workshops and classes." "STUDENT PORTAL" (book practice time). Only Terms of Service and refund policy pages.

### 7. Claytivity (Los Angeles: Silverlake, Frogtown, Burbank, US) — membership/community studio

Pages checked: https://www.claytivitypottery.com/ · /memberships · /faqs · /about-us · /gift-card · /copy-of-about-us (contact) · /silverlake (class list only; truncated)

1. **Size — Y (wheels).** /memberships — "Total of 16 wheels dedicated to members" (Silverlake) · "Total of 8 wheels dedicated to members" (Frogtown) · "Total of 12 wheels dedicated to members" (Burbank) · "Total of 8 wheels for instruction (will be open to members when no classes)". /faqs — "Typically our classes have a maximum of 8-10 students." Member count, waitlist, kilns, area NS.
2. **Staff — NS; ops NS.** /about-us names no one. Contact page: "Phone hours are between 9AM-4PM Monday-Friday."
3. **Work-exchange — NS.**
4. **Required member duties — NS.**
5. **Clay — must NS; incl Y (classes); price NS; how NS.** /faqs — "Class fee covers 4 sessions, 12.5 pounds of clay and access to studio during practice hours". /memberships — "Clay prices vary."
6. **Glazes — prov Y; out NS; mix NS.** /memberships lists "Multiple types of clay and glazes" (no access rules).
7. **Retail — sup Y (clay is sold, "Clay prices vary", item 5); work NS; gc Y; online NS.** /gift-card — "$195" with "Other amount" and a "Buy Now" button.
8. **Open studio — res NS; cap NS; access 24/7 controlled.** /memberships — "24 hours a day, 7 days a week controlled access to the studio". /faqs (students) — "access to studio during practice hours 9am-9pm when other classes are not in session".
9. **Kilns — all NS.** /memberships — "We reserve the right to limit the number of pieces fired for an individual". (Per-pound glaze fee is in 03.)
10. **Events/rentals — NS.** Public "Datenight Wheelthrowing" workshops only ("200 US dollars").
11. **Equipment list — Y (partial).** /memberships — "40" electric slab roller" (Silverlake), "36" slab roller" (Frogtown, Burbank), "Extruder" at all three, wheel counts in item 1.
12. **Comms — all NS.** Only a "Log In" link and social profiles.

### 8. Pottery Northwest (Seattle, US) — nonprofit art centre

Pages checked: https://www.potterynorthwest.org/ · /membership · /ourteam · /our-studio · /job-openings · /community-guidelines · volunteer form (forms.monday.com, linked from the site) · /private-group-events · /store · /registration-policies. Note: the studio is moving to South Lake Union ("Save the Date: November 1st").

1. **Size — Y.** /our-studio — "Our spacious 8,000 sq ft. studio serves approximately 2,000 community members annually." · "9 electric kilns ranging from 1 ft³ to 20 ft³" · "34 pottery wheels, including an adaptive wheel for wheelchair users". /membership — "The Studio Membership cannot be paused and any cancellation will require you to reenter the waitlist queue."
2. **Staff — Y; ops Y.** /ourteam — "EXECUTIVE DIRECTOR", "PROGRAMS and EXHIBITIONS DIRECTOR", "STUDIO MANAGER" (Becca Headline), "FINANCE & OPERATIONS SPECIALIST", "EDUCATION MANAGER", "ASSISTANT STUDIO MANAGER", "STUDIO TECHNICIAN" (2). Board page linked (/bod).
3. **Work-exchange / volunteer — Y (sign-up only).** Home nav "Volunteer" opens a form titled "Pottery Northwest Volunteer Signup" (no description, hours, duties or perks returned). /job-openings — "Pottery Northwest has no job openings at this time. Please check back."
4. **Required member duties — own cleanup only.** /community-guidelines — "Clean up your workspace thoroughly, including the wheel, counter, table, etc. Leave it cleaner than you found it." /registration-policies — "clean your area thoroughly using a wet sponge and please mop the floor around where you were working." /membership mentions "scheduling cleaning days" (studio closures, not member duty).
5. **Clay — must Y; incl N; price NS; how online student store.** /registration-policies — "Clay must be purchased from Pottery Northwest." (Class Supplies & Materials). /membership — "Studio Membership does not include clay." · "Clay and other supplies can be purchased from us online at the student store." /store — "Pottery Northwest only sells clay and supplies to currently enrolled students." · "All registered students have been emailed the code for the studio store."
6. **Glazes — prov Y; out Y (review); mix NS.** /membership — "Use of the studios cone 5 and cone 10 glazes, slips, and stains." · "Outside commercial glazes must be reviewed by staff prior to use in the studio." /registration-policies — "your membership includes access to our studio during open studio hours and all glazes and firings."
7. **Retail — sup Y (enrolled students only); work NS; gc Y; online Y.** Store quotes in item 5; "Gift Certificates" (Shopify collection). /our-studio — "We also have a community kitchen and a non-commercial gallery."
8. **Open studio — res NS; cap NS; access set hours + building codes.** /membership — "Everyday: 10am to 9pm" · "New members must attend an in-person orientation session that covers studio policies and procedures prior to being assigned a shelf and given building access codes."
9. **Kilns — load NS; freq NS; req NS; queue NS.** /membership — "Access to communal bisque and glaze firings" · "Bi-monthly community luster firings". /registration-policies — "Students making an excessive amount of work as determined by the Studio Managers may be asked to either pay for their firing(s) or reduce their volume of work."
10. **Events/rentals — party Y; studio buyout N; kiln rental NS; how inquiry form.** /private-group-events — "Private events can accommodate up to 15 participants maximum." · "There is a $1,000 minimum for all private events." · "A $500 deposit is required to reserve a date." · "Pottery Northwest does not reserve our entire studio for your event." · "Please submit your event inquiry as early as possible."
11. **Equipment list — Y.** /our-studio — "Bailey slab roller" · "Two wall-mounted clay extruders" · "Laguna Pro-V spray booth" plus kilns and wheels in item 1.
12. **Comms — news NS; portal NS; chat NS; rules Y.** /membership — "The studio’s full list of polices, procedures, and registration info can be found" (links to /registration-policies and /community-guidelines). /community-guidelines — "Reach out to info@potterynorthwest.org." No newsletter sign-up on the pages checked (03 detected Constant Contact in the HTML).

### 9. Northern Clay Center (Minneapolis, US) — nonprofit art centre

Pages checked: https://www.nccshop.org/pages/classes-for-adults-kids · https://www.northernclaycenter.org/ · /education/student-info/ · /education/registration-policies/ · /artist-services/artist-studios/ · /about/staff/ · https://www.nccshop.org/pages/volunteering-at-ncc · /parties-private-events/ · https://www.nccshop.org/products/gift-card · /gallery-services/terms-of-sale/. NCC has classes and juried artist studios, not an open community membership; its "NCC membership" is a supporter membership.

1. **Size — Y.** /artist-services/artist-studios/ — "wheel classrooms with 44 wheels, handbuilding classroom with slab roller and extruder" · "Private studios: from 95 to 210 square feet" · "Shared Community Studio: 35 – 72 square feet of vertical storage space" · "you will be placed on the waitlist and contacted when you are next in cue for space in the shared studio." Kiln count NS. Home — "Northern Clay Center is a 501(c)(3) nonprofit organization".
2. **Staff — Y; ops Y.** /about/staff/ — "Executive Director", "Operations & Development Manager", "Facilities Manager", "Studio Manager" (Audra Smith), "Kiln & Facilities Technician", "Materials Technician", "Outreach Technician", "Education Manager", "Education Registrar", "Sales Gallery & Special Events Manager", "Sales Gallery Assistant" (2), "Sales Gallery & Volunteer Coordinator", "Community Engagement Manager".
3. **Volunteer program — Y.** /pages/volunteering-at-ncc — "The Rest of the Year volunteers help us keep the studios clean and functional, evaluate gallery inventory," · "click on the task that you're interested in, choose your shift date and time, and add to cart." · "Then, "check out" as you would to buy a pot or register for a class." Perks/trade NS. Scholarships are need-based, not work-based (registration policies).
4. **Required member duties — NS.** Only for kids' camps: "Class periods will include time for cleanup, an integral part of the process." Artist studios: "a unique accountability agreement which promotes activity in areas of professional development, advancement, and service."
5. **Clay — must NS; incl Y (classes); price ≤ $25 / 25 lb; how in person.** /education/registration-policies/ — "25 lbs. of clay, basic glaze materials, and a firing allowance." · "25 pounds of clay (up to $25)" · "Tool kits and additional clay may be purchased at NCC."
6. **Glazes — prov Y; out NS; mix NS.** "basic glaze materials" (above) · "Some students may incur additional expenses if they choose unusual glaze materials." Artist studios list a "glaze-mixing room".
7. **Retail — sup Y; work Y; gc Y; online Y.** Home — "The Sales Gallery features work by a diverse and exciting group of artists". Terms of sale — "works by NCC studio artists are final sale." · payment "accepted online and in-store". Gift card "$25.00" to "$500.00". Consignment terms NS.
8. **Open studio — res N; cap NS; access set hours (students), 24 h (studio artists).** /education/student-info/ — "Students are not required to sign up in advance for open studio time." · "Tuesdays and Thursdays, 9 am – midnight;" /artist-services/artist-studios/ — "24-hour access to facility and equipment, including kilns (gas, soda, electric)".
9. **Kilns — load NS (students), artists fire own; freq NS; req cart; queue Y.** /education/student-info/ — "All work must be ON THE CART to be bisqued by the end of your second to last class date, no exceptions." · "Studio Technicians use chalk on bisqueware to date and track pieces." · "The small cone 10 glaze firing was unloaded today Friday, August 21st." /artist-services/artist-studios/ — "Individuals who cannot fire their own work are not eligible to license studio space at NCC."
10. **Events/rentals — party Y; rent Y (artist studios); kiln NS; how email.** /parties-private-events/ — "Throw your next birthday party or company outing with the help of a teaching artist and a little mud." · "Bring your team to NCC for a unique, hands-on celebration or team-building experience." · "$125 per person" with a "Minimum cost of $900" (wheel team-building) · "To schedule a private event, email juliarodman@northernclaycenter.org with the following information:". Studios: "available on a competitive, juried basis".
11. **Equipment list — Y (partial).** Item 1 quotes plus "kilns (gas, soda, electric)" and "glaze-mixing room".
12. **Comms — news Y; portal NS; chat NS; rules Y.** Home links /newsletter/ and a Mailchimp "email-sign-up". Student-info page has "Community Guidelines" and "Student Guide" headings and posts kiln updates; registration policies page.

### 10. MudFire (Decatur GA, US) — membership/community studio — **content unreachable**

Pages tried: https://www.mudfire.com/ · /memberships · /faq · /about · /artist-residency-programs · /product/trial-month/33 · /s/gift-cards · /health-and-safety-at-mudfire (all returned only a page title; the site renders its content with script) · /rangers-residency1 (HTTP 404). Wayback Machine: WebFetch cannot fetch web.archive.org. Everything below except the page titles is **UNVERIFIED** (from search-result summaries, not opened pages) and is not counted in the totals.

- Verified page titles only: "About our owners Deanna and Daphne" (staff: owners), "Gift cards at MudFire" (gift cards: Y), "Pottery Gallery at MudFire", "Artist in residence at MudFire pottery studio", "Pottery making classes and date nights at MudFire studio - DIY pottery", "Memberships at our pottery studio".
- UNVERIFIED (search summaries, 2026-10-20): membership $155/month after the first 30 days and a waitlist; "57 hours of open studio time per week"; "2500 cubic inch allowance for the glaze kilns" with bisque included; "Brent and Shimpo wheels and slab roller"; 60–70+ glazes made in house; electric, Bailey gas car kiln (6–10 week cycle), soda and raku firings; artists in residence called "Rangers" who work in exchange for studio access.

### 11. Callanwolde Fine Arts Center (Atlanta, US) — nonprofit art centre

Pages checked: https://callanwolde.org/classes/ · /pottery/ · /team/ · /rentals/ · /volunteer/. Class registration goes to ActiveNet (03). No ceramics policy, open-studio, FAQ or handbook page was linked from these pages. Web search budget was exhausted at this point, so no further pages could be discovered by search.

1. **Size — NS.** /pottery/ — "Callanwolde's Pottery and Ceramics department is one of the largest in the Southeast." (no figures).
2. **Staff — Y; ops Y.** /team/ — "Ceramics Program Director" (John Roberts), "Operations Director", "Facilities & Maintenance Director", "Rentals & Hospitality Coordinator", "Program Administration Director". /pottery/ — "Director: John Roberts" and 18 people listed as "Pottery Instructor".
3. **Volunteer program — Y at organisation level, none for ceramics.** /volunteer/ — Art Scholars camps: "Volunteers are needed to assist with:" "Classroom assistance during art instruction," "Meal prep and distribution," and "Craft clean up." Master Gardeners: "Time: 9 am - 12 pm". Sign-up via "Hands On Atanta" [sic].
4. **Required member duties — NS.**
5. **Clay — NS.** 6. **Glazes — NS.**
7. **Retail — gc Y (by email/phone); others NS.** /classes/ — "Gift Certificates for our Arts Programs are available." (contact Steve Cole by email or phone).
8. **Open studio — NS.** Only building hours: "Monday - Thursday - 9:00 am - 5:00 pm".
9. **Kilns — NS.**
10. **Events/rentals — venue rental Y; pottery parties NS; kiln NS.** /rentals/ — "Interested in having your special event at Callanwolde? Fill out the Event Inquiry Form and we'll be in touch!"
11. **Equipment list — NS.**
12. **Comms — news Y; others NS.** Newsletter form: "I am most interested in the following:".

### 12. Emerald City Fired Arts (Seattle, US) — paint-your-own-pottery (PYOP)

Pages checked: https://www.emeraldcityfiredarts.com/ · /works/ · /birthdays-and-events/ · /classesworkshopscamps/ · /?page_id=624 (gift certificates). No about, staff, FAQ, shop, jobs or policy pages exist in the menu.

1. **Size — NS.**
2. **Staff — NS; ops NS.** /works/ — "We'll help you get started by providing professional instruction" (no names or titles).
3. **Work-exchange — NS.** 4. **Duties — NS** (not applicable to a walk-in PYOP).
5. **Clay — incl Y (all materials in project price); must/price/how NS.** /works/ — "The cost of your project includes, all materials, instruction, the time you need to complete your work, glazing and firing." · "We do not charge additional painting or studio fees."
6. **Glazes — prov Y; out NS; mix NS.** Home — "We provide over 80 colors of glaze, brushes, professional instruction".
7. **Retail — gc Y; others NS.** /?page_id=624 — "Gift certificates are available for purchase at the store." · "Order from us and we’ll mail your gift card to you or your loved ones at no extra charge."
8. **Open studio — res N (walk-in); cap NS; access staffed hours.** Home — "Walk-ins welcome anytime plus we offer classes, workshops and space to hold birthdays and other events." · "Noon-6pm" (Tuesday–Sunday), Monday "By Appointment Only".
9. **Kilns — load staff; freq 7 days; req n/a; queue NS.** /works/ — "We'll fire your project (s) in our kilns and have your artwork ready for pick up in 7 days."
10. **Events/rentals — party Y; rent Y (space); how phone/email.** /birthdays-and-events/ — "Office Parties Corporate Events – Scout Troops – Church Groups – Any Occasion!" · "A $50 deposit is required to book your chosen date & time." · "Rent our space for meetings and after-hours parties and events." · "Call us at (206) 721-0450 for more information."
11. **Equipment list — NS.**
12. **Comms — NS.** (The site's email link is malformed: "create@nullemeraldcityfiredarts.com".)

### 13. The Digs (Chicago, US) — nonprofit art centre (a 501(c)(3) ceramics studio)

Pages checked: https://www.thedigschicago.com/membership · /faq · /team · /pottery-parties · /aboutdigs · /schedule-1 (HTTP 404)

1. **Size — Y.** /aboutdigs — "grown into a 6,000 sq ft facility supporting over 100 members across Residency and Pay-to-Clay programs." /faq — "Our Pay to Clay and Residency memberships currently have a waitlist!" /membership — "Residency is currently full and applications are open for the waitlist." · "_Studios sizes vary- the average size is 64 sq/ft_". Kiln and wheel counts NS.
2. **Staff — Y; ops Y.** /team — "Co-Founder | Facilities Manager" (Fawn Penn), "Co-Founder | Program Coordinator" (Zoe Minzenberger), "Studio Manager" (Delia Pelli-Walbert), "Studio Tech", "Arts Administrator", "Chicago Critique Group Facilitator", "Studio Monitor". /aboutdigs — "The Digs Chicago is a 501(c)(3) ceramic studio in West Town, Chicago."
3. **Work-exchange / internship — Y (details NS).** /faq — "The Digs internship program is currently full." A paid or volunteer "Studio Monitor" role is listed on /team (terms NS).
4. **Required member duties — own cleanup only (residents).** /membership — "Residents are expected to keep a clean studio space for dust safety."
5. **Clay — must NS; incl NS; price $2/lb; how NS.** /membership — "clay is available to purchase for $2/lb."
6. **Glazes — prov Y; out Y (approval); mix NS.** /membership — "12+ Studio Glazes" · "Outside glazes can be brought in with approval." /aboutdigs — "Free kiln firing + studio glazes" (residents).
7. **Retail — sup Y (clay); work Y (markets); gc NS; online NS.** /membership — "Ability to Participate in Digs Markets & Sales". /aboutdigs — "Participation in studio events and markets".
8. **Open studio — res Y (day passes); cap NS; access residents 24/7.** /faq — "The Digs offers Day Pass hours on Saturdays and Sundays 12pm-5pm." · "Please book on our website to grab a spot." · "Studio Experience is required for Day Pass." /membership — "Residents receive 24/7 access to the facilities" · "Please check the Digs Schedule before arrival to ensure no classes or events will be utilizing equipment." /aboutdigs — "Please no walk-ins!"
9. **Kilns — load NS; freq NS; req NS; queue NS.** /membership — "Free Kiln Firing" · "Ability to Schedule Personal Kilns" (residency).
10. **Events/rentals — party Y; rent Y (kiln; terms NS); how online + phone.** /faq — "Yes we do! Book a birthday party, company team outing, or even a date night at the Digs." /pottery-parties — "get creative with 5–15 guests" · "All private parties are BYOB and catering-friendly!" · "Call or Text 312-772-3578 to chat with our staff". FAQ has a "Kiln Rental" heading; /team assigns "kiln rental, facilities" to the Facilities Manager.
11. **Equipment list — Y (minimal).** /membership — "Pottery Wheels", "Slab Roller", "Work Tables".
12. **Comms — news Y; others NS.** "Stay tuned about upcoming classes, workshops and events!"

### 14. ClaySpace (Lisle IL, US) — cooperative (501(c)(3) nonprofit run by resident-artist members)

Pages checked: https://www.clayspace.net/ · /our-story/ · /join-the-community/ · /application/ · /classes/ · /gallery/ · /events/

1. **Size — Y.** /join-the-community/ — "We limit our members to 55, and we currently have a wait list." /our-story/ — "9,000 square foot studio space" · "1 front-loading gas kiln for Cone 9" · "1 front-loading gas kiln for Soda/Salt firing" · "5 top-loading electric kilns for clay" · "2 test kilns" · "1 raku kiln" · "22 various electric potters wheels, Brent & Shimpo".
2. **Staff — NS; ops NS.** No staff, officers or board named on the pages checked. /our-story/ — "founding ClaySpace NFP in 2006 as a 501(c)(3) nonprofit Illinois corporation".
3. **Internship — Y (closed).** /join-the-community/ — "We are not accepting internship applications at this time." /application/ — internships are "for individuals seriously planning to pursue a career in the ceramic arts" (hours, duties, trade NS).
4. **Required member duties — Y (co-op).** /application/ — Resident Artists are "responsible for active participation in running of the enterprise" (tasks and hours NS).
5. **Clay — NS.** 6. **Glazes — NS.** /our-story/ lists a "Fully equipped glaze area".
7. **Retail — work Y (market); others NS.** /events/ — "Our annual Holiday Market will be held the weekend of November 20th-22nd." Home — "Our on-site gallery features work by our resident artists, interns and students." (sales not stated).
8. **Open studio — res NS; cap Y (55 members); access unlimited for residents.** /join-the-community/ — "Our Resident Artists members receive full and unlimited access to the studio and equipment." /classes/ — "All 8-week classes include 8 Open Lab Sessions, scheduled at designated times, to be used during the 8-week class session."
9. **Kilns — NS.**
10. **Events/rentals — party Y (custom classes); rent NS.** /classes/ — "We’re thrilled to offer custom classes tailored just for you." · "we kindly request a lead time of at least two weeks." Public events: "Raku Fest is happening Saturday, October 17th from 10am – 4pm".
11. **Equipment list — Y.** /our-story/ — kilns and wheels (item 1), "3 slab-rollers", "1 extruder", "3 spray booths", "Fully equipped glaze area".
12. **Comms — portal Y; others NS.** Header link "Members Login" (03: WordPress login, private WooCommerce store).

### 15. Morning Ceramics (Portland OR, US) — membership/community studio

Pages checked: https://www.morningceramics.com/membership · /faq · /about · /jobs · /shop · /private-events · /markets

1. **Size — Y.** /membership — "Morning is located in SE Portland in a 5000 sq.ft space complete with 2 bay doors" · "Our five Skutt 1227 kilns (George, Mildred, Dad Jr., Lasagna, & J1mmy!) are loaded by staff and fired regularly." · "Offerings include multiple potters wheels and handbuilding tables". /faq — "We also fire a Luster and a Mother of Pearl kiln once a month."
2. **Staff — no titles; ops NS.** /about — founders "Alexa & Peter" (no titles) · "Behind it all is a small & resilient team—here every day to take care of the space and the people in it."
3. **Work-exchange — N (currently).** /faq — "We are not offering any work trade opportunities currently." /jobs — Studio Technician: "No open positions currently - but accepting rolling applications" · "Assist staff with daily tasks including kiln loading, reclaiming clay, helping customers," (apply via a Google Form).
4. **Required member duties — NS.**
5. **Clay — must NS; incl N (billed); price NS; how monthly invoice.** /faq — "Firing fees and clay are added to your invoice at the end of each month." · "We have 9 different clay bodies available at the studio."
6. **Glazes — prov Y; out Y (cone 6 safe); mix NS.** /membership — "a glaze station stocked with a selection of community house glazes." /faq — "you're welcome to use what you like." · "Yes! All of our current house glazes are food safe."
7. **Retail — sup Y (clay sold on the monthly invoice, item 5); work Y (markets); gc Y; online Y (merch).** /markets — "From cozy holiday markets to sunny summer sales, the studio has hosted a number of special vendor events over the past years." /shop — "T-Shirt," "$25.00"; "If you're looking to purchase a gift card for a class or workshop, click here to find out more."
8. **Open studio — res optional; cap NS; access 24/7.** /membership — "Studio time is available 24 hours a day via our booking system or drop in." · "Members have access to all communal equipment, tools, and materials, 24 hours a day." · private studio residents get "24 hour keyed access to their personal studio." /faq — "Open studio hours are available from 10am to 5pm, Wednesday through Sunday." · "please be conscious of the number of other people in the space."
9. **Kilns — load staff; freq Y; req NS; queue NS.** /membership quote (item 1, "loaded by staff"). /faq — "We load our kilns as soon as there is enough work to fill them." · the studio can "fire up to 13 times a week." · "Work is unloaded and ready to pick up 3 days later."
10. **Events/rentals — party Y; rent NS; how form.** /private-events — "battle it out with your colleagues to see who can make the best mug" · "Pricing varies based on group size, project selection, scheduling, and space needs." · "Use the form on this page to get in touch with us and find out more about our team building & private event opportunities."
11. **Equipment list — Y.** /membership — five Skutt 1227 kilns; "clay extruder, slab roller, designated light & dark plaster wedging tables."
12. **Comms — news Y; others NS.** /faq — "Our mailing list is always the best place to hear about new rounds of classes and workshops!"

### 16. Fancy Banana (Portland OR, US) — membership/community studio — **closing**

Pages checked: https://fancybanana.studio/ · /membership · /contact (the whole site; no other pages exist apart from an event RSVP product). Home page: "Fancy Banana is closing at the end of September."

1. **Size — NS.** Home — "Join a small community of artists, many of whom are selling their work or showing in galleries" · "We offer LOTS of space for glaze, clay, and large work-in-progress storage".
2. **Staff — NS; ops NS.** People appear only in photo captions, with no roles.
3. **Work-exchange — NS.** 4. **Duties — NS.**
5. **Clay — NS.** 6. **Glazes — NS.** 7. **Retail — NS.**
8. **Open studio — res NS; cap NS; access 24/7.** /membership — "Memberships offer unlimited 24/7 access to the studio, additional clay and glaze storage, and professional equipment." Home — "All ranges of experience are welcome, as long as you feel comfortable navigating a studio largely independently!" · "the whole rack of personal shelves included in your membership".
9. **Kilns — load members; freq NS ("regular" only); req NS; queue NS.** Home — "fire your own kilns (we’ll teach you!)" · "We’ll teach you to fire kilns in a supported environment" · "regular opportunities for gas and raku firing".
10. **Events/rentals — NS.** (A free public film screening: "Free to attend, just RSVP!")
11. **Equipment list — NS.**
12. **Comms — news Y; others NS.** "Sign up and receive updates!" with the builder notice "This newsletter signup form needs a storage option."

### 17. Fine Things (Portland OR, US) — membership/community studio with a retail storefront

Pages checked: https://finethingspdx.com/pages/membership-studio · /pages/frequently-asked-questions-about-fine-things · /pages/about-fine-things ("We are still developing this page, more info soon.") · /collections

1. **Size — NS.**
2. **Staff — NS; ops NS.** /pages/about-fine-things — "A collaborative multi-use space for membership, storefront and production home" (mentions Mary Carroll Ceramics and Alexandria Cummings Ceramics, no roles).
3. **Work-exchange — NS.**
4. **Required member duties — NS.** FAQ — "which we will go over cleaning, equipment, etc. during the new member orientation."
5. **Clay — must NS; incl NS; price at cost; how NS.** Membership page — "Clay Art Center clay available for purchase". FAQ — "they are available to members at cost."
6. **Glazes — prov Y; out NS; mix NS.** Membership page — "6+ cone 5/6 glaze options from Clay Art Center and more". FAQ — "We stock Clay Art Center glazes and add a new one every other month or so."
7. **Retail — sup Y; work Y; gc NS; online Y.** Membership page — "The opportunity to sell your work in the Fine Things storefront." FAQ — "Fine Things is a shared membership studio as well as a storefront." /collections lists "ART SUPPLIES", "HOME GOODS", "APOTHECARY", etc.
8. **Open studio — res NS for members; cap NS; access 24/7.** Membership page — "24/7 access to the studio". FAQ — "you can reserve a guest pass for $40/day."
9. **Kilns — NS** apart from the allowance already in 03. FAQ — outside firing: "Not at this time." · "We fire to cone 04 bisque and cone 5 and cone 6 for glaze."
10. **Events/rentals — NS.**
11. **Equipment list — Y (partial).** Membership page — "Access to wheels, slab roller, extruder, giffen grip, banding wheel, some tools and more".
12. **Comms — news Y; others NS.** "Sign up for our newsletter". Membership page — "members must do a studio orientation prior to beginning their membership".

### 18. Radius Community Art Studio (Portland OR, US) — membership/community studio

Pages checked: https://www.radiusstudio.org/membership/ · /about-radius/the-team/ · /apply/ · /contact-us/ (FAQ) · /sell-at-our-retail-shop/ · /shop/services/ · /shop/workshops/team-building/. Note: 03 typed Radius as art centre/nonprofit, but its team page lists a "Studio Owner"; no nonprofit statement was found, so it is tagged membership/community studio.

1. **Size — Y.** /membership/ — "Meet and make with our 70+ other working artists, dedicated staff, skilled instructors, and cool crew of students." · "Monthly Membership spots are limited." · "The current wait time is approximately 3 months." · "24/7 access to our 3,200 square foot studio and facilities." · kilns: "cone 6 electric or cone 10 gas kilns."
2. **Staff — Y; ops Y.** /about-radius/the-team/ — "Studio Owner" (Rin Noelle), "Studio Manager" (Sandra Casey), "Glaze Technician and Art Educator", "Studio Technician" (4), "Studio Assistant" (2), "Art Educator" (4).
3. **Work-exchange — NS.** /apply/ lists only "Ongoing: Ceramics Instructor".
4. **Required member duties — NS.**
5. **Clay — must N; incl NS; price NS; how in-house.** /membership/ — "Clay can be purchased in-house, and shared tools are available to check out for free." · "Buy clay from us at our discounted prices." /contact-us/ — "We do allow members to use clay and glazes from outside of the studio" (after checking with on-duty staff).
6. **Glazes — prov Y; out Y; mix NS.** /membership/ — "Use of our 20+ in-house-made glazes (included in the cost of firing)."
7. **Retail — sup Y (clay); work Y; gc Y; online NS.** /membership/ — "Vend at our markets and apply to sell your work in our shop or show in our gallery space." /sell-at-our-retail-shop/ — "All items are on consignment at a 50/50 split for non-members and a 60/40 split for members." · "Our Belmont Shop sells all local, handmade goods from Oregon makers." /contact-us/ — "YES! We have gift certificates" (for workshops).
8. **Open studio — res N; cap Y ("Monthly Membership spots are limited.", item 1); access 24/7 (monthly) with key fob; staffed by assistants.** /membership/ — "space is available on a first-come, first-served basis." · "Open Studio blocks (one punch) are two hours each, during designated open studio times." · "our studio assistants are always available for basic questions".
9. **Kilns — load certified members; freq monthly (cone 10); req wait-list form; queue NS.** /membership/ — monthly members can "become kiln certified with us to have full control of firing your pieces." /shop/services/ — "We host community Cone 10 firings about once a month" · "Sold out? Join the Cone 10 Firing Wait-list here!" · "we are not able to offer outside (non-member) bisque firings or outside Cone 6 firings." /contact-us/ — "Work made in class will be ready for pick up one calendar month after your workshop date." · "You will not be emailed/notified" when work is ready.
10. **Events/rentals — party Y; rent N; how email/online.** /shop/workshops/team-building/ lists a "Private Group Clay Class" product (contact "radiusartstudio[at]gmail.com"). /contact-us/ — "We do not offer any private workspace." /membership/ — "Radius hosts at least two markets at the studio per year".
11. **Equipment list — Y (minimal).** /membership/ — "This space includes a sink, wheels, work tables, and a slab roller." Kiln types in item 1.
12. **Comms — news Y; others NS.** /membership/ — "At-home members will receive a weekly members-only newsletter". /contact-us/ — "Sometimes our newsletter ends up in your spam folder so be sure to check!"

### 19. St. Johns Clay Collective (Portland OR, US) — membership/community studio

Pages checked: https://www.stjohnsclay.com/ · /membership · /studiofaq · /membership-agreement (returned only a title) · /teachers-staff · /about-5 ("Required Reading") · /curbside-firing-spring-2020 ("Firing Process") · /kiln-yard · /team-building. The Square clay store's subdomain is "st-johns-clay-cooperative", but no page checked describes a member-run co-op.

1. **Size — Y (waitlist only).** /membership — "We usually have a wait-list for Membership units." · "Members each have a 4' x 18"x 6 ft unit with five shelves of storage."
2. **Staff — Y; ops Y.** /teachers-staff — "Sidney founded St. Johns Clay Collective in 2016." · "She is currently an artist, instructor and program coordinator for St. Johns Clay Collective." · "Nick is an Open Studio Monitor." · Lisa "working as a studio monitor" · Brentwood "a valuable contributor to studio operations and instruction in the SJCC community since 2021." /about-5 refers to a "kiln master".
3. **Work-exchange / volunteer — NS.** /teachers-staff — Emily "has been an instructor and volunteer"; Olive "has agreed to lend her skills to helping during open studio." No program described.
4. **Required member duties — Y (firing participants).** /about-5 — "All participants agree to be present and assisting in loading and the unloading/ clean up of the kilns." · "Loading includes wading your own work and helping the kiln master load works into the kiln." · "When you rent the whole kiln, you are required to participate in the firing."
5. **Clay — must NS; incl N; price NS; how online or from monitor.** /membership — "Members are responsible for their own materials and tools." /studiofaq — "We sell our house white stoneware clay, and occasionally guest clay bodies (see our clay store)." · "You can purchase these online or from our studio monitor during open hours."
6. **Glazes — prov Y; out Y; mix NS.** /membership — "We offer weekly cone 6 firing and a range of house glazes for your use." /studiofaq — "this payment covers both the bisque and glaze firing and use of our in-house glazes." · "You can bring in your own glazes provided they are Cone 6."
7. **Retail — sup Y; work NS; gc Y; online Y.** /studiofaq — "basic potter's wheel tools" are sold; "We do not offer refunds for gift cards, clay or tools." Clay Store at st-johns-clay-cooperative.square.site. /membership — membership "is not a suitable venue for professional or retail ceramic production."
8. **Open studio — res NS; cap NS; access members all day, monitors in open hours.** /membership — "all day, everyday access to the semi-private studio." /studiofaq — "Mon. & Tues. 3-9 pm, Thurs. 3-6, Sat & Sun 12-6 pm." · "Studio monitors are there to help as well." An "Open studio sign up" page is linked (not checked).
9. **Kilns — load staff/first-come + participants at kiln-yard firings; freq weekly; req slip + online payment; queue Y (events).** /studiofaq — "Bisque is loaded every week." · "Glaze kiln is loaded weekly or as demand requires." · "To get work fired, bring us your work during open studio hours and we will help you measure your work for firing." /curbside-firing-spring-2020 — "Please use the measuring cubbie and firing slips." · "Go to our online store: CLAY STORE and add the proper denominations needed into your cart to pay the fee" · "If your email and order number is not on the slip, we can not put your work in the kiln." · "We load our kilns on a first come, first serve basis and space availability for size and dimensions." /membership — for cone 10, "We have a sign up list... and will fire when we have enough work for a full kiln." /kiln-yard — "Reserve your spot in a kiln event!" (soda and raku firing events sold as tickets).
10. **Events/rentals — party Y (team building); rent Y (whole kiln); how email / tickets.** /team-building — "Let the SJCC host a workshop for you and your team." · "Maximum of 20 people." · "$50 registration fee for all events." · "team builds are offered on Fridays from 1-3:00 only." /studiofaq — "Private lessons, events and team building opportunities require payment in full to get on our schedule." /about-5 — "In order to rent the whole kiln, please email us to get your fire on the calander." · "You can buy space by the square foot."
11. **Equipment list — Y.** /membership — "The studio equipment available for use are Shimpo Wheels, an extruder, slab roller, three handbuilding tables." /kiln-yard — soda and raku kilns.
12. **Comms — news Y; chat Y (firing thread); rules Y; portal NS.** "Sign up for our Newsletter" · "Get Kiln Yard News" sign-up. /about-5 — "The kiln master will use this thread to communicate with the group throughout the course of the fire". Menu: "Studio User Agreement", "Membership Agreement", "Required Reading", "Studio How To Videos".

### 20. Greentree Art Studio (Portland OR area, US) — teaching studio (home-based)

Pages checked: https://greentreeartstudio.com/classes · /greentreepotterystudioabout · /one-time-pottery-parties-and-experiences · /gift-certificates · /contact

1. **Size — Y.** /greentreepotterystudioabout — "In 2014 I started Greentree Pottery studio in the lower level of my home" · "Greentree Art Studio has expanded to seven wheels and two large work tables" · "a skutt 1227 kiln" · "a slab roller".
2. **Staff — founder only; ops NS.** About — "a wonderful group of teachers, in addition to myself," (founder Karen Haugan Tomlinson, no title). /classes — "Text Karen @ 971 330 9608 to schedule your time to create with clay."
3. **Work-exchange — NS.** 4. **Duties — NS.**
5. **Clay — must N; incl N; price NS; how at studio.** /classes — "Students use their own bag of clay, which they can purchase at the studio or at Georgies Ceramics."
6. **Glazes — shared pool.** /classes — "Students contribute one pint of glaze per month" · "We are brushing and pouring glaze and firing at both cone 5 and cone 6."
7. **Retail — sup Y (clay); work NS; gc Y; online NS.** /gift-certificates — "Gift Certificate for Four Youth Pottery Lessons" "$185.00" (bought online).
8. **Open studio — res Y (by text); cap NS; access Sunday.** /classes — "uninstructed open studio time is also available on Sunday 9am-9pm." Scheduling quote in item 2.
9. **Kilns — NS** (cone 5/6 quote in item 6).
10. **Events/rentals — party Y; rent NS; how online.** /one-time-pottery-parties-and-experiences — "Pottery Throwing Parties, for kids and adults, include instruction in the creation of two pieces of pottery" · "The minimum number is 4 participants and the maximum number that we can host for a party is 12 participants." · "The cost is $70 per participant for 90 minutes to two hours." ("Add to cart").
11. **Equipment list — Y (minimal).** Item 1.
12. **Comms — news Y (unfinished form); others NS.** /contact — "This newsletter signup form needs a storage option. Edit the block and enter a storage location via the Storage tab."

### 21. Parker Art School (Parker CO, US) — teaching studio (multi-media art school with a pottery membership)

Pages checked: https://parkerartschool.com/ · /pottery-wheel-classes · /adult-classes · /events%2Fparties · /about

1. **Size — NS.** Home — "Featuring professional-grade wheels, kilns, and a wide selection of tools and glazes," (no counts).
2. **Staff — owners + teachers; ops N.** /about — "Parker Art school is a family owned and operated studio in Parker CO." · "Sadi and Kyle are the main teachers at Parker Art School along with Molly." Home — "Today, my husband Kyle and I run Parker Art School with one simple goal".
3. **Work-exchange — NS.** 4. **Duties — NS.**
5. **Clay — incl Y (membership); others NS.** /pottery-wheel-classes — "Membership incudes 25 lbs of Clay" · "$199 per month (add a spouse or friend for just $75)". Home — "All materials are included, and every project is fired and finished in our professional ceramic studio."
6. **Glazes — prov Y; out NS; mix NS.** /pottery-wheel-classes — "Unlimited Glaze usage included".
7. **Retail — NS** (no shop, gift card or consignment page found).
8. **Open studio — res NS; cap NS; access set hours.** /pottery-wheel-classes — "Studio access during open hours" · "Sunday 10-7 pm".
9. **Kilns — NS** beyond the allowance in 03 ("Bisque & glaze kiln firings for up to 12 pieces of any size").
10. **Events/rentals — party Y; rent NS; how contact form.** /events%2Fparties — "Looking for the perfect place to host your next corporate party?" · "Pottery Wheel parties are recommended for parties under 10 guests." · "9-12” Piece $125 PP" · "Contact us to book today!"
11. **Equipment list — NS.**
12. **Comms — news Y; others NS.** Home — "Join our Newsletter and receive coupons every month for art classes!"

### 22. Feats of Clay (Austin, US) — membership/community studio

Pages checked: https://featsofclaypottery.com/studio-space-firing-services · /faq · /about · /upcoming-events · /classes · /featsmerch

1. **Size — Y (waitlist, wheels).** /studio-space-firing-services — "Keep in mind that there may be a waitlist." · "A dedicated open studio room with 7 wheels, handbuilding tables, tools". /about — "Feats of Clay is a woman-owned and operated ceramics studio that's been around since 1976!"
2. **Staff — no titles; ops NS.** /faq — "Please ask a tech for assistance if you are ever unsure of something." /classes — instructors by first name ("taught by Bella").
3. **Work-exchange — NS.** 4. **Duties — NS.**
5. **Clay — must NS; incl N (independent study); price $25 / 25 lb; how from studio.** /faq — "Update as of 3/1/26: Students must purchase their own clay to use during this independent study time." · "Clay will be available for purchase through Feats of Clay at a rate of $25 per 25lb bag." · "This clay cannot be stored at the studio due to space limitations."
6. **Glazes — prov Y; out NS; mix NS.** /studio-space-firing-services — "This membership includes studio access, equipment use, firings, glaze usage".
7. **Retail — sup Y (clay); work Y; gc Y; online Y (merch).** /upcoming-events — "Our talented staff and studio members will be selling their own handmade ceramics" (Staff & Member Holiday Market) · "Help our artists clear space and shop beautiful pieces on sale!" /faq — "All sales of gallery work are final." · "Feats of Clay offers both physical and digital gift cards!" /featsmerch — "Printed Aprons": "$18.00"; "Commemorative Tile Donation - 5" x 5"": "$75.00".
8. **Open studio — res Y; cap NS; access 9–9 or 24 h.** /studio-space-firing-services — "Self-scheduling of studio time through our member site" · "Daily Studio access available from 9AM-9PM" · "24 hour studio access available". /faq — "Please note all sliding-scale open studio hours must be scheduled (via phone call during open hours) ahead of time." /classes — "Students can schedule their additional 3-hour open studio time using the link on the student guide."
9. **Kilns — load NS; freq Y; req paper log; queue NS.** /studio-space-firing-services — "We fire our kilns once or twice a week only when there is enough work to fill." · "Our typical turnaround time is 1-2 weeks, this time may vary especially with glaze firings." · "Once work is measured and charged accordingly, you will sign our Kiln Firing Log."
10. **Events/rentals — NS.** Only public fundraisers (e.g. "come party for a purpose!") and private lessons ("We offer private lessons for all ages.").
11. **Equipment list — NS** (wheels and tables only).
12. **Comms — news Y; portal Y; chat NS; rules Y.** "Join our Mailing List"; "member site" (item 8); "student guide" (item 8).

### 23. Mud Studio (Austin & San Antonio, US) — membership/community studio

Pages checked: https://mudstudioceramics.com/austin-memberships · /faq · /austinteam · /careers-austin · /gift-austin

1. **Size — Y (wheels).** /austin-memberships — "Dedicated Members room with 8 wheels" · "Dedicated Members kiln & pick up/drop off shelves". /faq — "We have electric Skutt kilns and fire cone 6."
2. **Staff — Y; ops Y.** /austinteam — "OWNER" (Lindsay Werner, "the founder and owner of Mud Studio"), "STUDIO MANAGER" (Matt Hill), "INSTRUCTOR" (6). "Lindsay manages all aspects of the business—from day-to-day operations and scheduling to marketing and social media."
3. **Work-exchange — NS.** /careers-austin lists only "Instructor" ("While we aren't currently hiring for instructors").
4. **Required member duties — NS.**
5. **Clay — must Y; incl N; price $18–25; how at studio.** /faq — "Only clay purchased at Mud Studio can be used and fired in our studio." · "Bags of clay cost anywhere from $18-$25." · "Firing and glaze fees are incorporated into the cost of clay, our classes and memberships."
6. **Glazes — prov Y; out NS; mix NS.** /austin-memberships — "Firing included & access to studio glazes". /faq — "As long as you use the clays and glazes provided by Mud Studio, your pottery is food safe."
7. **Retail — sup Y (clay); work NS; gc Y; online NS.** /gift-austin — "Give the gift of ceramics. Gift cards can be used towards the purchase of classes."
8. **Open studio — res NS; cap NS; access set hours.** /austin-memberships — member hours from "Monday: 12pm-9pm".
9. **Kilns — load NS; freq NS; turnaround Y; req NS; queue NS.** /faq — "The firings can take up to 24 hours and then have to cool." · pickup "2-3 weeks after completion".
10. **Events/rentals — party Y; rent NS.** /faq — "We can offer private classes for 4-20 people."
11. **Equipment list — Y (minimal).** /austin-memberships — "slab roller, extruder, glaze area, communal tools".
12. **Comms — news Y; others NS.** /faq — "Join our Austin mailing list" / "Join our San Antonio mailing list".

### 24. Brave Ceramics at Ceramigos (Austin, US) — membership/community studio (Ceramigos), with Brave Ceramics as a production potter who teaches there

Pages checked: https://www.braveceramics.com/ · /about · /events-1 · /shop ("No results found") · https://ceramigos.kilnfire.com/ (login form only) · https://www.ceramigos-austin.com/ · /membership · /open-studio · /team · /residency-program. The row names two businesses; Brave's pages describe the potter, Ceramigos' pages describe the studio where the classes and Kiln Fire account sit.

1. **Size — Y (wheels).** Ceramigos /membership — "As a member, you will have access to 4 dedicated wheels and a large table in our members’ area anytime during our business hours." · "members can reserve any of the other 10 studio wheels or table spaces in our class area during our walk-in hours." /residency-program — "The studio offers 2 resident-dedicated wheels."
2. **Staff — owner (Brave); unnamed staff (Ceramigos); ops NS.** Brave /about — "I'm Tina Williams -the owner & founder of Brave Ceramics." Ceramigos /team (no names) — "check in/checkout with our front desk" · "authorization by an instructor or a member of our staff".
3. **Residency — Y.** Ceramigos /residency-program — "Ceramigos offers residencies to clay artists who desire to share their passion with our students." · "Each Resident Artist will have a dedicated working area with personal table and storage, and 24/7 access to the studio." · "and free firing for non-production work." · "Resident Artists must initially commit to a minimum of 6 months, and +/- 24 hours per week." · teaching: "be compensated for their involvement" · "Clay artists may apply to the residency program by submitting a resumé and portfolio of their work".
4. **Required member duties — NS.**
5. **Clay — must Y (open studio) / N with authorization (members); incl NS; price NS; how at studio.** Ceramigos /open-studio — "No outside clay or glazes are allowed." · "We have bags of clay for sale." /membership — "Ceramigos offers a selection of clay bodies for purchase." · "Other cone 6 clay bodies may be used with prior authorization from our staff."
6. **Glazes — prov Y; out N (open studio) / approval (members); mix NS.** /membership — "Members have access to all tools and studio glazes." · "Please ask our staff for any other cone 6 glaze approval and proper testing procedure."
7. **Retail — sup Y (clay); work Y; gc Y; online NS.** Brave /about — "I sell my work at local boutiques and markets while also doing custom orders for businesses and restaurants such as Real Ale Brewery and El Alma." Ceramigos menu "Gift Cards". Brave /shop listed nothing.
8. **Open studio — res Y; cap NS; access business hours / 24/7 residents.** /membership — "Reservations can be made up to 7 days in advance." · "Both wheels and table space will be available to reserve on our members’ site." /open-studio — packages "give access to our reservation platform." · "Must have taken a multi-week classes before or be currently enlisted in a multi-week class."
9. **Kilns — load staff; turnaround (classes) Y; req Kiln Fire; queue NS.** Ceramigos /team — "Only our staff is allowed to operate the kilns." /membership — "Free non-production firing." · "Larger pieces and repetitive items may be deprioritized by our staff depending on demand." /open-studio — "Firing fees will apply to pieces larger than 8in in any direction." Brave /events-1 — "Pottery made in class is ready 3 -4 weeks after class for pick up."
10. **Events/rentals — party Y; rent NS.** Brave /events-1 — "We love teaching custom classes for corporate team building events." · "Request a Class" (contact page). Ceramigos menu "Private Events" (not checked).
11. **Equipment list — NS.**
12. **Comms — news Y; portal Y; chat NS; rules Y.** Brave — "Sign up to receive upcoming pottery event info and new class releases." Ceramigos — "Be the first to know about all our events and special programmings." · members' site (item 8) · ceramigos.kilnfire.com login · "Studio Guidelines" menu link.

### 25. Austin Clay Arts (Austin, US) — membership/community studio

Pages checked: https://www.austinclayarts.com/ · /shop/p/membership · /about-1-1 (FAQ) · /about-1 · /shop/adult-classes

1. **Size — Y (waitlist).** /shop/p/membership — "Membership - Click through for waitlist" · "sold out." Home — "LIMITED SPACE AVAILABLE >". /about-1-1 — "Membership is a small community of emerging artists who work independently developing bodies of work."
2. **Staff — founder only; ops NS.** /about-1 — "FOUNDER - AUSTIN CLAY ARTS" (Justin Goodall). /shop/adult-classes — "Our skilled instructors provide guidance, encouragement, and techniques to help you along your journey".
3. **Work-exchange — NS.** 4. **Duties — NS.**
5. **Clay — incl Y; others NS.** /shop/p/membership — "All materials and firing included." /shop/adult-classes — "Every multi-week class includes materials, tools, and firing."
6. **Glazes — prov Y (via "All materials"); out NS; mix NS.** /about-1-1 — "do not eat or drink in the glaze area."
7. **Retail — sup NS; work Y (markets); gc NS; online NS.** /about-1-1 — "Do you have any artwork or other products for sale?" "We don’t have a rotating shop, but we will offer occasional market days."
8. **Open studio — access 24/7 (members); res NS; cap Y (home page "LIMITED SPACE AVAILABLE >").** /shop/p/membership — "Delight in 24/7/365 access." /about-1-1 — "Yes! There are open studio hours for all students to work on class projects, or to experiment with new ideas."
9. **Kilns — NS.** 10. **Events/rentals — NS.** 11. **Equipment list — NS.**
12. **Comms — news Y; others NS.** "Stay up to date with announcements, invitations and news by joining our mailing list."

### 26. ATX Pottery / Austin Pottery (Austin, US) — membership/community studio with a street-front gallery

Pages checked: https://www.atxpottery.com/ · /studio-policy · /open-studio · /studio · /8weekclass

1. **Size — Y (class wait list only).** /8weekclass — "Late Fall 2026 Classes Are Full!" · "Click HERE and fill in the required information to join the wait list!"
2. **Staff — generic roles only; ops NS.** /studio-policy — "please ask your instructor or studio employee." · "The kiln loaders are handling everyone's works and must know what they are dealing with for their safety." · "Any use of outside materials must be approved, in advanced, by Austin Pottery management."
3. **Work-exchange — NS.**
4. **Required member duties — own cleanup only.** /studio-policy — "It is the responsibility of all users to clean up any areas, equipment, and tools that they have used."
5. **Clay — must Y; incl N; price NS; how NS.** /studio-policy — "All clay used in the studio must be purchased from Austin Pottery." · "The clay price includes an additional amount that helps covers the materials and labor costs of the glazes and firings."
6. **Glazes — prov Y; out approval only; mix NS.** /studio-policy — "Use only the glazes and washes provided by the studio." · outside materials need management approval (item 2).
7. **Retail — sup Y (clay); work Y; gc NS; online NS.** /studio — "Austin Pottery is a community studio offering an assortment of classes and handmade gifts." · "The street front gallery offers a variety of unique, handmade pottery by local artists."
8. **Open studio — res NS; cap Y; access member hours + sign-in.** /studio-policy — "Members' access times effective July, 2024 are: 10am-9:00pm Mon-Thurs;" · members "may work in the open studio space during open studio if it is not full with student use." · "All students, staff, and members must sign in at the designated station". /open-studio — "Please note that Open Studio is only available to current students enrolled in an 8-week class."
9. **Kilns — load staff; others NS.** /studio-policy — "Any unmarked work will be placed on the 'Unmarked' area of the shelves and will not be fired." (kiln-loader quote in item 2).
10. **Events/rentals — party Y (corporate); rent NS.** /studio-policy — "up to 3 days per month may be reserved for Corporate Events".
11. **Equipment list — NS.**
12. **Comms — rules Y; others NS.** /studio-policy — "All members and students registered for multi-week classes must submit their agreement" before using studio facilities.

### 27. The Contemporary Austin Art School (Austin, US) — nonprofit art centre (museum art school)

Pages checked: https://thecontemporaryaustin.org/artschool/ · /faq/ · /faculty/ · /volunteering-interniship/ · /supply-lists/ · /site-rentals/. The Art School pages say almost nothing about how the ceramics studio runs.

1. **Size — NS.**
2. **Staff — faculty only; ops NS.** /faculty/ — faculty bios such as "a ceramic artist and educator"; Shannon Hogarty is described as "serving as a studio assistant and teacher." No admin or studio titles.
3. **Volunteer — Y (closed).** /volunteering-interniship/ — "The Art School is not currently accepting volunteer applications." · "Check back around November for information about volunteer opportunities for Spring 2025."
4. **Required member duties — NS.**
5. **Clay — must N; incl NS; price NS; how in person at the museum shop.** /supply-lists/ — "Clay can be purchased at Laguna Gloria, in the museum gift shop." · "Clay can ONLY be purchased in-person, and during museum open-hours: Wednesday-Sundays 9am-3pm, and Thursdays 9am-9pm." · "All Art School students will receive 10% off their clay purchases from the Shop using the code MEMBER10." · "Students can also purchase clay at Armadillo Clay".
6. **Glazes — NS.**
7. **Retail — sup Y (clay); work NS; gc NS; online Y (museum shop at shopatthecontemporary.com).**
8. **Open studio — NS.** /artschool/ — "Membership to The Contemporary Austin is required for all Art School classes, except for Saturday Samplers."
9. **Kilns — NS.** /supply-lists/ — "All clay MUST BE HIGH FIRE (Cone 10). We will not fire other clays."
10. **Events/rentals — venue rental Y; ceramics parties NS.** /site-rentals/ — "non-wedding private events at either location at any time by submitting your inquiry here:" · "Please allow 3-5 business days for Site Rentals to respond to your email."
11. **Equipment list — NS.**
12. **Comms — news Y (museum ActiveCampaign form linked); others NS.**

### 28. Dougherty Arts School / Dougherty Arts Center (City of Austin, US) — municipal/recreation

Pages checked: https://www.austintexas.gov/department/dougherty-arts-school · https://www.austintexas.gov/dougherty/dougherty-arts-school · /dougherty/rentals. Registration is the city's WebTrac (03). No ceramics-specific policy, open-studio or equipment page was linked.

1. **Size — NS.**
2. **Staff — Y; ops Y.** /dougherty/dougherty-arts-school — "Ashley Dodd (Studio Technician)"; instructor bios (e.g. Pooja Ramnath "teaches and fires kilns at East Side Pot Shop, Feats of Clay Pottery, and the Dougherty Arts Center.").
3–9. **NS.** The page says only "The DAC Ceramics Program is designed to create a supportive atmosphere where students learn from each other." · "Advanced students are welcome in any class to work on improving their skills."
10. **Events/rentals — party N; rent Y (non-ceramics rooms).** /dougherty/rentals — "The DAC does not rent space for life, family or personal events such as weddings, birthdays," · "Two 450 sq. ft. community "creative spaces" free during business hours." · "Complete the document, save to your device, and email it as an attachment to DACTheater@austintexas.gov."
11–12. **NS.**

### 29. The Workshop Mpls (Minneapolis, US) — **unreachable**

Tried 2026-10-20: https://www.theworkshopmpls.com/ and https://theworkshopmpls.com/ both failed with "self signed certificate" (03 recorded "Site not found" on 2026-10-19). WebFetch cannot open web.archive.org, and the session's web-search budget was exhausted, so no snapshot or alternative page could be found. All 12 items: UNR. Segment carried from 03's type (M T), unverified.

### 30. The Bascom (Highlands NC, US) — nonprofit art centre

Pages checked: https://www.thebascom.org/ · /education/open-studio · /education/art-by-appointment/ceramics · /get-to-know-us/our-staff · /support/volunteer · /shop · /gather/venue-rentals

1. **Size — Y.** Home — "a 2,500 square-foot barn rebuilt for use as the ceramic studio". /education/open-studio — "Limited availability."
2. **Staff — Y; ops Y.** /get-to-know-us/our-staff — "Director of Ceramics" (Frank Vickery), "Ceramics Artist-Educator", "Executive Director", "Facilities Manager", "Retail Manager", "Community Engagement Manager"; board "Chair", "Vice-Chair", "Treasurer", "Secretary".
3. **Studio assistant / volunteer — Y.** /education/open-studio — "Potential to be a studio asst for a workshop with more than 8 students (preregister)". /support/volunteer — "Find a creative, meaningful volunteer opportunity at The Bascom." · "Please complete and submit the application below" (no ceramics roles, hours or perks).
4. **Required member duties — NS.**
5. **Clay — incl Y; others NS.** /education/open-studio — "The studio provides clay, glaze, and firings for members" · year membership includes "One 25 lb bag of clay".
6. **Glazes — prov Y; out NS; mix NS.** Same quote.
7. **Retail — work Y; sup NS; gc NS; online NS.** /shop — "The Bascom Shop showcases a wealth of local and regional artistic talent." · "Gift wrapping and shipping available upon request."
8. **Open studio — res NS; cap Y; access daily + approval.** /education/open-studio — "Must have experience." · "Must be approved by Director of Ceramics." · "The studio is open to members beginning at 9 am." · "open 7 days a week, closed only for special events, workshops, and holidays." · "Regular 3 month Membership: $550".
9. **Kilns — mostly NS.** /education/open-studio — "Work made at home can be fired in studio kilns". /education/art-by-appointment/ceramics — "a 2-3 week turnaround on ready projects".
10. **Events/rentals — party Y (private sessions); rent Y (terrace); how web form.** /education/art-by-appointment/ceramics — "We offer both private and open enrollment ABA sessions in a variety of projects and methods of making." · "Groups of 8 or more require a 2-hour class." · "Please complete all the required fields below." /gather/venue-rentals — "The Bascom’s covered terrace is available to rent for guest functions Monday-Sunday from 6:00-10:00pm."
11. **Equipment list — NS.**
12. **Comms — news Y (home-page sign-up form); others NS.**

### 31. Color Me Mine Edina (Edina MN, US) — paint-your-own-pottery (PYOP), franchise

Pages checked: https://edina.colormemine.com/ · /visit-studio · /policies (heading only, no policy text) · /private-studio-rental/ · /we-are-hiring/

1. **Size — NS.**
2. **Staff — job ad only; ops NS.** /we-are-hiring/ — "Ceramic Studio Team Member" · duties "Running the POS system (handling payments)" · "Processing customer pottery after painting" · "Unboxing the inventory, shelving, bagging and general upkeep of the studio." · "Part-time: 10–24 hours per week" · "Competitive hourly wage" · "Tips included".
3. **Work-exchange — NS.** 4. **Duties — NS.** 5. **Clay — n/a (PYOP), NS.**
6. **Glazes — prov Y.** /visit-studio — "You’ll select your paint colors – no limit to how many or how much!"
7. **Retail — gc Y; others NS.** Home — "Click here to purchase gift card" (cmmedinamn.cardfoundry.com).
8. **Open studio — res N (walk-ins); cap NS; access store hours.** Home — news item "Walk-Ins All Day." · "Sunday 11:00am - 6:00pm Monday 10:00am - 8:00pm".
9. **Kilns — NS** (staff duty "Processing customer pottery after painting").
10. **Events/rentals — party Y; rent Y (private studio).** /private-studio-rental/ — "The Private Party Package – $500" · "Private parties require a minimum of 15 people." · "Studio fees for 15-40 people (Does not include pottery)" · "A 18% gratuity will be added to the entire party total for your party host" · "$500 is non-refundable unless party is canceled at least 7 days prior to event".
11. **Equipment list — NS.** 12. **Comms — NS** (a "Studio News" page only).

### 32. UWM Studio Arts & Craft Centre (Milwaukee, US) — university/college/school

Pages checked: https://uwm.edu/union/craft-centre/ · https://uwm.edu/union/student-employment/ (no craft-centre roles) · https://uwmunion.vbotickets.com/event/SACC_2026_Summer_Membership/190475 (title only) · https://uwm.edu/studentaffairs/event/ceramics-qa/

1. **Size — Y (opening threshold).** /union/craft-centre/ — "The Studio Arts and Craft Centre will open over the summer of 2026, if we receive a minimum of 35 purchased memberships."
2. **Staff — role only; ops NS (unnamed technicians do not count, see coding decisions).** /union/craft-centre/ and the Q&A event — "individualized advice from our Ceramics Technicians".
3. **Work-exchange — NS.** 4. **Duties — NS.**
5. **Clay — price Y; others NS.** /union/craft-centre/ — "Discounted prices on workshops, clay, and locker rentals for UWM students." Price table: "Clay Mid-Fire White 25lb. Bag" $15.00 student, $20.00 community (table values as returned).
6. **Glazes — NS.**
7. **Retail — sup Y (clay); others NS.**
8. **Open studio — access set hours; res/cap NS.** "Monday – Thursday: 10:00 am – 9:00 pm" · "Open studio space: Work on your own projects with access to basic equipment." Q&A: "RSVP in advance through our website or just drop in!"
9. **Kilns — NS** (price table lists "Special Firing" $35.00).
10. **Events/rentals — NS for the centre.** "The Craft Centre offers so many free options from demonstrations to crochet nights!"
11. **Equipment list — NS.** 12. **Comms — NS.** Rule: "No one below the age of 14 is allowed to access the space."

### 33. UVM Ceramics Co-op (Burlington VT, US) — university/college/school

Pages checked: https://www.uvm.edu/cas/soa/soa-ceramics-co-op · the linked "Studio Information and Member Agreement" PDF (WebFetch could not decode it; **unreachable**). Despite the name, no member work duties were found on the page (the agreement may hold them).

1. **Size — Y (capacity per block).** "There are 11 wheel-throwing and 6 hand-building spots per block."
2. **Staff — Y; ops Y.** "For questions, please reach out to the Studio Manager, Britton Blanchard." · "the manager will show you around."
3. **Work-exchange — NS.** 4. **Duties — NS** (agreement PDF unreadable).
5. **Clay — incl Y.** "Everything you need is included with your membership." · "Membership Pricing for a 13 week session (everything included):" ($400 students, $500 non-students).
6. **Glazes — prov Y** (same quote); out NS; mix NS.
7. **Retail — NS.**
8. **Open studio — res Y; cap Y; access blocks; partly unstaffed.** "Friday: Free Block Time 9am-8pm (members get two extra hours every other week)" · Friday open studio: "there will be a sign-up sheet" · "The manager will not be helping with late blocks or weekend blocks."
9. **Kilns — NS** (firings included in membership).
10. **Events/rentals — NS.** "Follow us on Instagram to see ceramics work, studio events, and more."
11. **Equipment list — Y (minimal).** "wheels, slab roller, coil press, clay, tools, glazes, firings, etc."
12. **Comms — news Y; rules Y; portal NS; chat NS.** "Join Our Mailing List" · "SoA Ceramics Co-op Studio Information and Member Agreement (PDF)".

### 34. Miami University Art Center (Oxford OH, US) — university/college/school

Pages checked: https://miamioh.edu/cca/art-center/membership.html · /cca/art-center/index.html · /cca/art-center/sprint-courses/index.html · /profiles/cca/art-center/index.html (staff list did not render)

1. **Size — Y.** /index.html — "The ceramics area houses 12 pottery wheels, electric kilns, and a hand building area". /membership.html — "Memberships are limited and first-come, first-served, so don't miss out!"
2. **Staff — NS; ops NS.** Staff directory loads by script; no names returned.
3. **Work-exchange — NS.** /index.html only recruits instructors ("We are seeking innovative, enthusiastic, and qualified instructors").
4. **Required member duties — NS.** Members must "agree to the safety and facility use agreement".
5. **Clay — must Y; incl N; price NS; how NS.** /membership.html — "You must buy your clay from the Art Center" · "Bisque and glaze firing for a reasonable amount of clay is included."
6. **Glazes — prov Y.** /membership.html — "Includes use of throwing wheels, hand-building area, ceramic tools, molds, rolling press, kilns, glazes, and glaze brushes."
7. **Retail — work N; sup Y (clay must be bought from the Art Center, item 5); others NS.** /membership.html — "This is NOT for production or sales."
8. **Open studio — cap Y (membership cap); access lab hours; monitored.** /membership.html — "You'll have access to open studio hours (in your class studio)". /sprint-courses/ — "For those specific times, **a monitor must be present in the studio.**" · "Art Center studio and labs are available only to students who are currently registered and enrolled".
9. **Kilns — NS** beyond "a reasonable amount" of firing.
10. **Events/rentals — NS.**
11. **Equipment list — Y (minimal).** Item 6 quote and "12 pottery wheels, electric kilns".
12. **Comms — rules Y (use agreement); others NS.**

### 35. NWTC Artisan & Business Center (Green Bay WI, US) — university/college/school (technical college community education)

Pages checked: https://www.nwtc.edu/about-nwtc/nwtc-locations/artisan-and-business-center/open-studio · …/open-studio/open-studio-schedule · …/artisan-and-business-center · the linked Airtable policy share (no text rendered: "browser is unsupported").

1. **Size — NS.**
2. **Staff — Y; ops Y.** /artisan-and-business-center — "Ceramics Studio Coordinator" (Annie Stenseth), "Manager, Community Education", "Education Coordinator", "Office Assistant" (2). Open studio — "Certification is granted at the discretion of Artisan Center instructors and staff."
3. **Work-exchange — NS.** 4. **Duties — NS.** 5. **Clay — NS.** 6. **Glazes — NS.**
7. **Retail — sup Y; others NS.** Open studio — "Access to select art supplies available for purchase onsite."
8. **Open studio — res NS; cap NS; access staffed hours + certified badge access.** Open studio — "Eligible members may access the studio outside of staffed hours after completing required certification and badge setup." · "Studio access may vary based on classes, events, and facility needs." Schedule page — "The calendar below displays daily Open Studio availability at the Artisan & Business Center." · "Studio certification is required to use equipment and work independently in each studio area." · "Completion of a class does **not automatically result in certification**." · "Certification appointments are $30".
9. **Kilns — queue Y; others NS.** "The logs below provide a general view of current kiln activity for the Ceramics and Fused Glass studios." · "Inclusion in a firing is not guaranteed and may not be reflected immediately." · "Information is updated by staff and may change throughout the firing process."
10. **Events/rentals — NS.** "Studio Scoop meetings are held in advance of each membership period and are strongly encouraged for all members."
11. **Equipment list — NS.**
12. **Comms — news Y; rules Y; portal NS; chat NS.** "This quarterly newsletter is sent to subscribers, open studio members, and current students." · "Registration dates, Studio Scoop meetings, and important updates are shared through:" (email, social media, newsletters, signage) · "Policies are maintained as a live system and reflect the most current expectations."

### 36. Plains Art Museum (Fargo ND, US) — nonprofit art centre (museum)

Pages checked: https://plainsart.org/ · /programs-learning/open-ceramics-studio/ · /staff-directory/ · /jobs/ · /facility-rentals/. (One home-page fetch mis-handled the prompt; it was re-run.)

1. **Size — NS.**
2. **Staff — Y; ops Y.** /staff-directory/ — "Clay & Studio Program Manager" (Louie Albertson), "Director of Learning & Engagement", "Chief Operating Officer", "Facilities Supervisor", "Events Manager".
3. **Volunteer — Y at museum level.** /jobs/ — "We are looking for enthusiastic volunteers to help with our youth, family, and adult programs at Plains Art Museum." · "You don't have to be an art expert to volunteer." No ceramics roles, hours or perks.
4. **Required member duties — NS.**
5. **Clay — NS (may cost extra).** Open studio page — "Some materials, clay, firing, and other studio-specific supplies may have additional fees."
6. **Glazes — prov Y.** "Ceramics — Access to our ceramics studio, shared equipment, tools, glazes, and designated storage."
7. **Retail — NS** (home links a Square shop, not checked).
8. **Open studio — access key card; orientation; res/cap NS.** "Plains Art Museum membership is required." · "Open Studio orientation is required before access begins." · "Key-card access during Open Studio hours" · "**Tuesdays & Thursdays:** Noon – 9 PM" · "$75 / month, Individual".
9. **Kilns — NS** (fees quote in item 5).
10. **Events/rentals — rent Y; party NS.** /facility-rentals/ — "Host your event here." · booking via "Inquire About Your Date".
11. **Equipment list — NS.**
12. **Comms — news Y (Constant Contact sign-up link); others NS.** Open studio page — "Invitations to studio events and creative opportunities throughout the year".

### 37. John Burrison Pottery Studio, Folk Pottery Museum (Sautee GA, US) — nonprofit art centre (museum)

Pages checked: https://www.folkpotterymuseum.com/classes · /open-studio-application · /volunteer-docent · /contact

1. **Size — Y.** /classes — "the studio is now fully equipped with nine pottery wheels, a dedicated hand-building and sculpture area, and two kilns". /open-studio-application — "Membership is currently limited to 10 members, but this will expand in the future".
2. **Staff — Y (role only); ops Y.** /open-studio-application — "All new members are required to attend a New Member Orientation with the Studio Coordinator." · "Our staff members handle all loading and firing of kilns."
3. **Volunteer — Y at museum level (docents).** /volunteer-docent — "We are seeking enthusiastic volunteers to train as docents for the Folk Pottery Museum". No studio roles.
4. **Required member duties — NS.** Only piece preparation: "Members are responsible for making sure their pieces are ready for firing, labeled, and on the correct shelf."
5. **Clay — must Y; incl N; price $50 / 25 lb; how NS.** /open-studio-application — "Outside clay and glazes are not permitted for use at this time." · "Clay $50/25lb Bag" (covers clay, studio glazes and firing) · "using Standard Clay Company #225 Clay" · membership "$85 per month".
6. **Glazes — prov Y; out N; mix NS.** "Fully equipped glazing room with a selection of studio dipping glazes" · outside glazes not permitted (item 5).
7. **Retail — sup Y (clay); gc Y; work NS; online NS.** /classes — "Gift Certificates are available in any amount over $10 and can be used for any class, workshop, or studio membership." (sold through johnburrisonpotterystudio.kilnfire.com/gift-card).
8. **Open studio — res optional (app); cap Y; access set sessions, staffed.** "Members will be encouraged to reserve work space times using the studio mobile app." · "If a reservation is not made, workspace and equipment is available on a first come first serve basis." · "Tuesday, Wednesday, Saturday 4:00-8:00pm & Sunday 2:00-6:00pm" · "All available Open Studio sessions are staffed to answer questions about studio equipment".
9. **Kilns — load staff; req app + shelf; freq/queue NS.** Quotes in items 2 and 4; "The studio uses the mobile app KilnFire that allows members to manage their studio membership".
10. **Events/rentals — NS.**
11. **Equipment list — Y (minimal).** Item 1 and the glazing room.
12. **Comms — portal Y; rules Y; news NS; chat NS.** Kiln Fire quote (item 9) · "All studio policies, rules, and procedures will be provided upon approval of application."

### 38. Sonoma Community Center (Sonoma CA, US) — nonprofit art centre (community center)

Pages checked: https://sonomacommunitycenter.org/ceramics/ceramics-open-studio-program/ · /about-us/our-team/ · /support/ · /ceramics/ · /program-policies/

1. **Size — Y.** Open-studio page — "16 electric pottery wheels" · "One of the most complete kiln arrays in the Bay Area". /ceramics/ — "All kiln types: midrange electric, highfire gas, raku, and cross-draft soda".
2. **Staff — Y; ops Y.** /about-us/our-team/ — "Director of Ceramics" (Megan Billingham), "Ceramics Studio Coordinator" (Azelia Mazur), "Director of Operations & Finance", "Facilities Caretaker" (4). /ceramics/ — Daniel Clauson "Studio Technician and Instructor". Open-studio page — "12 studio monitors on rotation" · "40 of our 60 weekly hours are monitored – a team member is on the floor to answer questions and sell materials."
3. **Volunteer — Y at organisation level.** /support/ — "Volunteers help at events, assist in classrooms, support gallery installations, and much more." No ceramics roles, hours or perks.
4. **Required member duties — own cleanup only.** Membership qualification: "properly clean up after myself in a shared studio".
5. **Clay — must Y (unless permitted); incl NS; price NS; how from monitors.** "All clay used at the studio must be purchased through SCC, unless the Director of Ceramics grants explicit permission." · "10 clay types in stock at all times".
6. **Glazes — prov Y; out NS; mix Y.** "30+ glazes, all made in-house at SCC" · "Raw materials lab for custom glaze mixing."
7. **Retail — sup Y; others NS.** Monitors "sell materials" (item 2). /ceramics/ — texture mats "are available for sale at Sonoma Community Center".
8. **Open studio — res NS; cap NS; access set hours, mostly monitored.** "Mon – Sat" "10 AM – 8 PM" · Monitor on Duty "Mon- Fri" "10 AM – 6 PM" · "Rolling enrollment – join any time".
9. **Kilns — NS.** Home Studio members: "Bring your own clay work from home for professional firing". (03 records the bisque-discard rule.)
10. **Events/rentals — rent Y (general spaces); party NS.** /ceramics/ — "Versatile spaces for meetings, performances, receptions, and community gatherings." Members: "Monthly shop talks & member-only events".
11. **Equipment list — Y.** /ceramics/ — "16 electric pottery wheels, Peter Pugger, 30″ slab roller, extruder, 40″ spray booth".
12. **Comms — news Y; others NS.** "Get the good stuff, weekly" · "Equipment, policies, and available materials are regularly updated."

### 39. Art Alliance (Greensboro NC, US) — nonprofit art centre (board-governed art school)

Pages checked: https://www.artalliancegso.org/project/adult-studio-membership/ · /about-us/board-staff/ · /volunteer-intern/ · /shop/ ("No products were found matching your selection.") · /about-us/

1. **Size — NS.** /about-us/ — "Art Alliance classes are held in the Greensboro Cultural Center in studio 127 and 129."
2. **Staff — Y; ops Y.** /about-us/board-staff/ — "Executive Director", "Ceramics Director" (Patrick Rowe), "Program Assistant"; board "Chair", "Treasurer", "Vice Chair", "Secretary".
3. **Volunteer — Y (pottery sale).** /volunteer-intern/ — "We're looking for volunteers to help guide customers to the sale location" · "help out in the sales room by assisting customers, completing transactions, answering questions, and more!" · sign-up via SignUpGenius; "please fill out the voluntneer interest form." [sic]
4. **Required member duties — NS.** 5. **Clay — NS.** 6. **Glazes — NS.**
7. **Retail — work Y (annual pottery sale); others NS.** Volunteer page refers to the "Creative Clayworks Pottery Sale".
8. **Open studio — eligibility only.** Membership page — "You must be an intermediate or advanced potter who has taken a class at Art Alliance within the past 6 months" · class time listed as "During Open Studio." · tuition "$195".
9. **Kilns — NS.** 10. **Events/rentals — NS.** 11. **Equipment list — NS.**
12. **Comms — rules Y; others NS.** "Open studio members must be familiar with and abide by all current studio guidelines." (guidelines not published on the pages checked).

### 40. Hayden Center Pottery Co-Op (Hayden CO, US) — municipal/recreation (Town of Hayden)

Pages checked: https://www.haydencolorado.com/276/Pottery-Co-Op (single page; the town site links no studio policy, staff or handbook page). Despite the name, the page states no member work duties.

1. **Size — NS.**
2. **Staff — role only; ops Y.** "Yes! Outside clay is permissible, although it MUST be approved by the studio manager."
3. **Work-exchange — NS.** 4. **Duties — NS.**
5. **Clay — must N; incl Y (first month); price $40/bag; how invoiced.** "25 pound bag of your choice of clay included in your first month!" · "Additional clay can be purchased for $40/bag to be invoiced on your Amilia account".
6. **Glazes — prov Y.** "Access to over 50 premium studio glazes and glazing tools" · "Membership includes access to studio-owned glazes."
7. **Retail — sup Y (clay); work Y; gc NS; online NS.** "Opportunity to sell your work at Town Events (TBD)".
8. **Open studio — access 7 days; res/cap NS.** "Unlimited access to Hayden Center pottery studio 7 days a week!" · "individuals must attend an orientation meeting and have previously participated in pottery courses".
9. **Kilns — freq Y; limit Y; others NS.** "We fire when our shelves are getting full, so it varies." · "Up to 20 small/medium pieces per month." · "This is a monthly membership for glazing and firing only." ($60 tier).
10. **Events/rentals — NS.** "Members only workshops".
11. **Equipment list — NS.** "Use of all studio equipment including potter's wheels and all tools".
12. **Comms — news Y (town newsletter link); others NS.**

### 41. Wayne Art Center (Wayne PA, US) — nonprofit art centre

Pages checked: https://wayneart.org/ · /about-wayne-art-center/policies/ · /support-wac/volunteer/ · /visit/employment/ · /about-wayne-art-center/team-building/ · /visit/your-visit/shop/shop/ · /visit/facility-rentals/childrens-birthday-parties/. The referenced "Ceramic Studio Guidelines" (materials and firing rules) were not linked from these pages as HTML and were not checked.

1. **Size — NS.**
2. **Staff — NS (ceramics); ops NS.** /visit/employment/ lists "Youth Art Program Manager", "Art and Music Educators", "Evening & Weekend Registrar". Faculty pages only.
3. **Volunteer — Y at organisation level.** /support-wac/volunteer/ — "Volunteers are needed to help with many daily tasks such as reception desk, data entry, the gallery shop," · "culinary assistance, special events and educational programs."
4. **Required member duties — NS.** 5. **Clay — NS.** 6. **Glazes — NS.** /policies/ — "Please refer to the Ceramic Studio Guidelines for materials use and firing information."
7. **Retail — work Y; gc Y; sup NS; online NS.** Shop page — "browse our carefully curated collection of unique hand-crafted items created by local and regional artists." · "Wayne Art Center Gift Certificates are available in any denomination, throughout the year." · "Stop by the front office to purchase a Gift Certificate today." Home — "Wayne Art Center Announces 5th Annual Pottery Sale and Studio Fundraiser".
8. **Open studio — enrolled members only; res/cap/access NS.** /policies/ — "Ceramic Open Studio is open to registration for Wayne Art Center members, currently enrolled in class" · "Ceramic Open studio is not for completing work from home or another studio."
9. **Kilns — NS.**
10. **Events/rentals — party Y; rent Y; how email/phone/form.** Birthday page — "$405 for ceramic project parties/ceramic beaded jewelry" · "To schedule a birthday party, please contact birthdays@wayneart.org or call 610-688-3553." Team building — "Or try a more leisurely art or ceramics class to foster creativity." · "For more information, call 610.688.3553 or fill out our Online Inquiry form." Home links "Facilities" rentals.
11. **Equipment list — NS.**
12. **Comms — news Y ("Sign up!" mailing list); rules Y (guidelines referenced); others NS.**

### 42. Recess Urban Recreation — listing for GnarWare Workshop (Chicago, US) — membership/community studio

Pages checked: https://recessurbanrecreation.getgalore.com/memberships/8895-re-occuring-monthly-membership · /providers/671-gnarware-workshop · https://recessurbanrecreation.getgalore.com/ (302 redirect to getgalore.com, not followed). No studio website is linked from the booking pages, so this audit rests on the studio's own booking pages only. The provider page names the studio "GnarWare Workshop" and calls it a "Chicago based ceramics studio" ("In business since '17"), which fills the city that 03 lacked.

1. **Size — NS.**
2. **Staff — instructors only; ops NS.** Membership page — "NEW MEMBERS MUST BE APPROVED BY STAFF VIA EMAIL". Provider page lists class instructors by name, no titles.
3. **Work-exchange — NS.**
4. **Required member duties — NS.** Provider page lists "Cleaning day - Studio closed 4-8pm" (who cleans is not stated).
5. **Clay — must NS; incl N; price $2.50/lb; how NS.** "Clay per lb @ $2.50, includes firing, underglaze (speedball) and glaze, $3 for porcelain and specialty bodies."
6. **Glazes — prov Y.** "15 house made glazes".
7. **Retail — sup Y (clay sold per lb, item 5); others NS.**
8. **Open studio — res N; cap NS; access 24/7.** "24/7 access to the studio, reduced access during class times (see calendar)." · "First come first serve use of equipment and tables." · "YOU MUST TAKE A SIX WEEK CLASS BEFORE APPLYING FOR MEMBERSHIP".
9. **Kilns — turnaround Y; others NS.** "Kiln firing turn around about two weeks for each kiln load, longer for larger pieces."
10. **Events/rentals — party Y; rent NS; how Galore booking.** Listings "Private Party - Wheel room" and "Private Wheel Throwing Party".
11. **Equipment list — NS.**
12. **Comms — rules Y (terms on booking page); others NS.** "Membership at Gnarware is a rental agreement with an automated month to month payment schedule." · "We will only give refunds after you have removed your items from the building." · "Individual shelf space, sizes range based on availability."

## Hypothesis evidence

Ratings use only what public sites show. Back-office tools (stock control, accounting, reconciliation) rarely appear on a public site, so "none" means "no public evidence", not "no need". Counts are out of 42 (40 reachable).

### H1 Inventory tracking (consumables and retail stock) — moderate

For:
- 27 of 42 sell clay, tools or supplies. 6 require members to buy studio clay, and 8 more allow outside clay only after approval, review or proof of purchase, so 14 of 42 control what clay goes into their kilns.
- Clay is often the unit of charge. 10 publish a clay price, 4 fold firing (and glaze) into the clay price (Mud, ATX, John Burrison, GnarWare), and 2 bill clay to a monthly invoice or account (Morning; Hayden via Amilia).
- Stock-outs show on public shop pages. Gasworks lists clay bodies marked "- Unavailable", and Still Life marks "B-3 Brown" Sold Out.
- Job ads include stock work. BKLYN's technicians "Unload clay deliveries, keep stock organized" and "Mix and replenish clay". Color Me Mine's staff do "Unboxing the inventory, shelving, bagging". Sonoma's monitors "sell materials".
- 4 say they make their glazes in house (Choplet, Radius, Sonoma, GnarWare), and 2 more have a glaze lab or mixing room (Powerhouse, NCC). That means a stock of raw glaze materials.
- Other retail stock and stored value: 19 sell finished work (Radius on consignment), 12 run online stores and 20 sell gift cards.

Against or limits:
- No site names an inventory tool or describes stock-keeping.
- Catalogues are small: Choplet "13 stoneware clay bodies", Sonoma "10 clay types", St Johns one house clay plus occasional guest clays.
- Online sales already run through the website builder's shop (03), which keeps its own product stock.

Rating: **moderate.** Selling and controlling materials is common and brings in money, and stock-outs are visible. No public site shows that tracking it is a pain.

### H2 Asset and equipment tracking — weak

For:
- 21 publish equipment lists. 9 give kiln counts, 14 give wheel counts and 7 name kiln makes.
- 7 rent kilns with per-kiln prices. BKLYN lists 4 kilns with cubic inches and prices; Choplet prices 4 kilns at $160–$360.
- Access to equipment is gated:
  - Radius has kiln certification.
  - NWTC requires certification per studio area ("Certification appointments are $30").
  - Choplet requires a spray-booth session.
  - 5 more require an orientation before access (Pottery NW, Plains, John Burrison, Hayden, Fine Things).
- Some staff titles are about equipment: NCC's "Kiln & Facilities Technician" and "Materials Technician". 9 organisations list facilities roles, and Gasworks teaches kiln maintenance in its "How to be a Studio Tech" class.

Against:
- The equipment lists read as marketing. No site mentions maintenance logs, element or thermocouple changes, firing counts per kiln or an asset register.
- Members book equipment at only 2 studios: Ceramigos (wheels and tables) and Centerpoint (time on a Google Sheet).

Rating: **weak** for asset tracking itself. A neighbouring need is **moderate**: equipment as a resource that can be booked and is gated by certification.

### H3 Sales integrations with multiple payment providers (Stripe, Square) — weak

For:
- Money comes in through many channels:
  - online stores: 12
  - gift cards: 20
  - in-person sales at a front desk or monitor: 8
  - invoices: 2
  - party deposits and minimums (Pottery NW "$500 deposit"; Color Me Mine "$500" package)
- 8 of 42 show two or more separate payment channels:
  - BKLYN: shop plus its "Firing Fee System"
  - Gasworks: Shopify, Kiln Fire gift cards and "a PayPal invoice" for parties
  - Morning: booking system, monthly invoice and shop
  - St Johns: Wix Clay Pass plans plus a Square store that also takes firing fees
  - NCC: payment "accepted online and in-store"
  - Color Me Mine: POS plus CardFoundry gift cards
  - Plains: Sawyer, Givebutter and a Square shop
  - Contemporary Austin: online museum shop plus clay sold "ONLY ... in-person"
- Gift cards sometimes live in a separate system (Kiln Fire at 2 studios, CardFoundry at 1), which splits stored value from the main store.

Against:
- Stripe is named on none of the pages checked (03 found it only inside BKLYN's firing app). Square is named on 2 (St Johns, Plains) and PayPal on 1.
- The common pattern is one storefront per studio plus in-person and invoice sales, not studios juggling Stripe and Square. The evidence favours connecting to the storefront or POS a studio already has, and recording front-desk and invoice sales. It does not show a need for several payment providers.

Rating: **weak.**

### H4 QuickBooks Online integration — none

- QuickBooks, Xero or any other accounting product: 0 of 40 reachable sites.
- Indirect only. 9 organisations list finance or accounting staff:
  - BKLYN: "Operations Director/COO & CFO"
  - Gasworks: "Co-owner and CFO"
  - Powerhouse: "Accounting Manager"
  - Callanwolde: "Finance & Administration Director"
  - The Bascom: "Director of Finance & Administration"
  - Sonoma: "Director of Operations & Finance"
  - Plains: "Controller"
  - NCC: "Accountant"
  - Pottery NW: "FINANCE & OPERATIONS SPECIALIST"
- 2 studios bill members monthly. Books clearly exist, but the tool is invisible.
- This is not "contradicts": public sites are the wrong instrument for this question, and only owner interviews can test it.

Rating: **none.**

### H5 Scheduling — moderate (strong for firings, events and rentals; weak for members' studio time)

For:
- **Events:** 21 offer parties or private events. Booking is mostly by inquiry form (8) or email and phone (3); only 6 let customers book themselves online.
- **Rentals:** 17 rent studios, spaces or kilns, including 7 that rent kilns. BKLYN: "must be scheduled in advance".
- **Firing:**
  - 13 say how firing is requested and 14 state a frequency or turnaround.
  - Still Life takes drop-off reservations: "you must schedule ahead of time".
  - St Johns sells tickets to firing events.
  - Only 4 publish a queue or status.
- **Open studio:**
  - 9 require reservations for at least some users, using improvised tools: a Google Sheet, a paper sign-up sheet, a text to the owner, a phone call.
  - 9 state capacity limits and 11 run waitlists.

Against:
- 20 offer 24/7 or key, code, card or badge access, and 5 say outright that open studio is first come or walk-in. Most members never book a slot.
- 03 shows that existing storefront and scheduler tools already handle class and membership booking.

Rating: **moderate** overall. The unmet scheduling need is for firings and shared resources (kilns, wheels, events), not members' studio time.

## Other strong signals

1. **Firing status is the visible gap.** Staff load the kilns at 8 studios and members at 4, but only 4 publish any queue or status.
   - Choplet: "Do not call us to find out if your work has been fired."
   - Radius: "You will not be emailed/notified"
   - NWTC: "Inclusion in a firing is not guaranteed and may not be reflected immediately."

   Firing requests still run on paper at 3 (slips, and Feats' "Kiln Firing Log" that members sign) and on Kiln Fire or home-built apps at 5. Tracking each piece and sending a "ready" notice addresses a problem studios state in public.
2. **Approving outside materials is a workflow.** 8 allow outside clay and 12 allow outside glazes, but only after approval, review, testing or proof; Choplet asks for "proof of purchase and exact cone reference". 1 studio bans outside glazes. The same steps recur: request, approve, record.
3. **Firing allowances and fairness rules** appear at 6:
   - Claytivity: "We reserve the right to limit the number of pieces fired for an individual"
   - Hayden: "Up to 20 small/medium pieces per month."
   - Parker: "Bisque & glaze kiln firings for up to 12 pieces of any size"
   - Ceramigos: "Free non-production firing", with "repetitive items" deprioritised
   - Pottery NW: may charge or cut back "excessive amount of work"
   - Miami: "a reasonable amount of clay"

   Allowance ledgers matter as much as per-piece fees.
4. **Who staffs the studios.** 19 studios name non-owner operations roles. By title:
   - studio managers or ceramics directors: 15
   - technicians: 11
   - coordinators: 5
   - monitors: 4
   - front desk: 3

   Monitors also sell materials (Sonoma; St Johns sells clay "from our studio monitor during open hours"), so sales happen at the monitor's station as well as online.

   Work-trade is nearly absent. No hours-for-membership programme was verified, and Morning says it offers none. Paid technician jobs with perks take its place (BKLYN: a firing credit and a shelf).

   Volunteering is a nonprofit feature: 11 of the 12 nonprofits have some, against 1 of 19 membership studios.
5. **Access control and certification go together.** 20 studios offer 24/7 or key, code, card or badge access. 9 put a gate before access:
   - Radius: kiln certification
   - NWTC: $30 certification and badges
   - Choplet: a two-month probation for 24-hour access, plus a spray-booth session
   - Pottery NW: orientation before "building access codes"
   - Plains: orientation, then "Key-card access"
   - John Burrison, Hayden and Fine Things: orientations
   - The Bascom: approval by the Director of Ceramics

   Membership status, certification and door access belong in one model.
6. **Selling members' work is common.** 19 studios sell finished work and 11 run member markets or sales (Gasworks' sale draws 600+ people). Radius publishes its consignment split (60/40 for members), and Fine Things offers members its storefront. Consignment and market sales are a sales-integration need the hypotheses don't name.
7. **Waitlists and caps.** 11 have waitlists (BKLYN 15–18 months; Radius about 3 months; Gasworks and The Digs full), and 9 state capacity limits. A waitlist that offers freed places automatically is a concrete feature.
8. **Studio websites are fragile.**
   - 2 of 42 were unreachable and 1 is closing (Fancy Banana).
   - 3 newsletter forms are not connected to anything.
   - Key pages at several studios render only by script (MudFire; Miami's staff list; NWTC's policies on Airtable).
   - UWM's studio opens only if 35 memberships sell.

   Public booking, waitlist and policy pages that the product provides, and that a studio cannot break, fit a product that "stands in for" a manager.
9. **Communication is mostly email and in person.** 27 have newsletters, 15 rules or agreements, 10 portals and 1 group chat (an unnamed "thread"). Policies change ("Policies are maintained as a live system" at NWTC), and several studios require a signed agreement (ATX, Wayne, Miami, UVM, BKLYN's Student Agreement). Versioned rules that members acknowledge would fit.
10. **Owner-operated small studios.** 4 studios show only the owner, Parker is "family owned and operated", and Mud's owner "manages all aspects of the business—from day-to-day operations and scheduling to marketing and social media." For studios like these, the software would stand in for a manager.

## Gaps and limits

- **Search budget.** The session's shared web-search budget ran out at studio 11. After that, pages could be found only through each site's menus, so pages they don't link were missed.
- **Wayback Machine blocked.** WebFetch cannot fetch web.archive.org, so MudFire and The Workshop Mpls stay unreachable. MudFire details from search summaries are marked UNVERIFIED and not counted.
- **Script-rendered and protected pages** returned only a title or nothing: all of MudFire (Square Online), St Johns' membership agreement, Miami's staff directory, NWTC's Airtable policies and the UWM membership page. Centerpoint's members page returned HTTP 401.
- **PDFs.** UVM's member agreement PDF could not be decoded. WebFetch automatically saved the file to the session's tool-results folder; it was not opened. Wayne's "Ceramic Studio Guidelines" were not checked. Handbooks often sit in PDFs, so member duties and kiln and glaze rules are probably under-counted.
- **Extraction limits.** WebFetch answers through a small model:
  - It caps quotes at about 125 characters, so some quotes end in "…".
  - It sometimes summarised instead of quoting. Where that mattered the page was re-queried, and only text it returned in quotation marks is presented as a quote.
  - It can miss content, so NS means "not found on the pages checked".
- **Page cap.** Each studio got its home page plus up to 8 linked pages. Large sites (BKLYN, St Johns, Gasworks) have more. Row 24 covers two businesses and used 10 pages.
- **Coding judgment.** The "Coding decisions" list at the top applies throughout. The main calls:
  - clay sold through any channel counts as retail
  - "own cleanup" rules are kept apart from shared duties
  - ops = Y needs a named person or an explicit management title
  - capacity limits include membership caps
  - "regular" firing counts as NS
- **Segments that differ from 03.** Radius (its team page lists a "Studio Owner"), The Digs (a 501(c)(3)), row 24 (tagged by Ceramigos) and row 42 (GnarWare Workshop, Chicago) differ from 03's types or names. The merge should use this file's tags.
- **Sample.** Inherited from 03: 6 of the 42 studios are in Portland, 6 in Austin, and 12 are nonprofits. Segments of 1 to 4 studios can't support segment-level conclusions.
- **What public sites can't show.** Stock control, accounting, payment reconciliation, maintenance and staff rotas are back-office work. H1, H2 and H4 need owner interviews or screenshots to confirm or refute.
- **Not re-collected.** Booking and commerce platforms and firing fees are in 03. Some quotes here include prices only because they sit in the same sentence.
- **Date.** All pages were fetched on 2026-10-20.

