# Inventory and assets: what a pottery studio stocks, maintains and accounts for

Research notes, started 2026-10-20. Author: research agent (web only: WebSearch, WebFetch).

Status: COMPLETE (2026-10-20). Web-search budget ran out twice and Reddit was unreachable; see Gaps and limits.

**Bottom line.** The evidence does not support making inventory the anchor of phase 1. Studios sell clay by the bag through 7 different mechanisms (3 of 7 on paper), but no first-hand pain was found and no studio uses an inventory tool. PYOP's real SKU inventory is already handled by POS systems. Equipment is the better asset story, but narrowly: kilns and ventilation. The one readable kiln manufacturer (L&L) schedules maintenance by firing count, and studios already record every firing, so a firing-driven kiln maintenance log is cheap to build and differentiated. UK law requires a 14-month LEV test with 5-year records. Most real equipment pain is sudden failure and capital replacement ($3,800-$50,000, 6 of 8 events crowd-funded), not missed checklists. Sales integrations, QuickBooks Online and scheduling get only weak, indirect support from this scope.

## Plan

1. Skim prior work (done): `03-tools-in-use.md` already covers kiln-controller subscription prices and APIs, glaze software (Glazy, Insight-Live) and supplier platforms; `01-market-survey.md` ranks "Material inventory and reordering" as pain point #9 with a "low" signal. Do not redo these; focus on maintenance data, quantities, mechanisms and problems.
2. Supplier facts (US first, then CA, UK, AU/NZ): clay bag/box sizes, prices, minimum orders, pallet/freight, lead times.
3. Studio clay-sale mechanisms and problems (honour logs, POS, online shop, included in membership). Another researcher counts policies; here only mechanisms and failure modes.
4. Glaze: batch sizes, specific-gravity upkeep, test tiles, studio glaze-mixing logs.
5. Kiln furniture, kiln wash, PYOP bisque and paints, consignment.
6. Manufacturer maintenance: Skutt, L&L, Paragon, Olympic, Bailey, Shimpo, Brent, Peter Pugger, North Star; element and thermocouple life; controller diagnostics (Bartlett Genesis, KilnLink, TAP, Orton, Nabertherm).
7. Regulation: OSHA HazCom 1910.1200, HSE COSHH (ceramics), Safe Work Australia register, NFPA/NEC kiln clearances, insurer schedules, depreciation (IRS Section 179 / de minimis).
8. Tools: vendor pages (label as vendor claims) and any studio evidence of use.
9. Pain evidence: Reddit RSS (few queries), forum posts, handbooks. Count independent sources. (Outcome: Reddit blocked to both tools; pain drawn from fundraisers, news, studio pages.)
10. Closing: Hypothesis evidence, Other strong signals, Gaps and limits.

## Labels used

- Number type: **[MFR]** manufacturer guidance · **[REG]** regulation · **[VENDOR]** vendor claim · **[PRAC]** practitioner anecdote · **[SUPPLIER]** supplier list price or policy (a kind of vendor fact, but a price list rather than a marketing claim) · **[STUDIO]** a studio's own published policy or price.
- Country: US, CA, UK, AU, NZ, DE etc.
- Segment: membership/community studio · teaching studio · cooperative · nonprofit art centre · PYOP · production pottery · university/college/school · makerspace · municipal/recreation · (n/a for supplier or manufacturer pages).
- UNVERIFIED = seen only in a search snippet or could not be opened.

## Findings

All sources accessed 2026-10-20 unless stated; publication dates given where the page shows one.

### A. Equipment maintenance: manufacturer guidance

#### A1. L&L Kiln Mfg (US; segment n/a, manufacturer). Accessed 2026-10-20. All pages render a date of 2026-10-19 (likely a dynamic render date, not a publish date).

| Item | Interval / number | Label | Verbatim quote | URL |
|---|---|---|---|---|
| Maintenance schedule exists, keyed to firings | After each firing / every 10 firings / after 30 firings or annually | MFR | "Keep a kiln log of firings." · "Tracking the performance of your kiln over time may turn out to be an extremely valuable tool." | https://hotkilns.com/support/pottery-kiln-knowledgebase/longevity-how-should-my-kiln-be-maintained-regular-basis |
| Every firing | Breaker off; check element holders and walls for glaze/clay chips; vacuum holders; remove glaze splatter; keep shelf tops kiln-washed; log the firing | MFR | "Check element holders and walls for glaze, clay chips or anything that could melt at a high temperature." · "Make sure the tops of the shelves are coated with kiln wash as it will keep running glaze from ruining the shelf." | same |
| Every 10 firings | Check cord/receptacle/breaker temperature at peak; inspect plug; **check element resistance**; tighten case; repair brick | MFR | "Check temperatures of the main power cord at the main receptacle and the main kiln breaker while the kiln is at its hottest." · "Check element resistance." · "Check the tightness of the case and retighten it if necessary." | same |
| Orton AutoCone / kiln-sitter backup | 6-12 months | MFR (Orton, quoted by L&L) | "Orton recommends checking the pivot point for corrosion and sluggishness every 6 to 12 months." | same |
| Every 30 firings or annually | Wires, terminals, power-connection tightness; more often near salt air | MFR | "Check wires for deterioration or oxidation." · "Check power connection terminals in the kiln and control box for tightness." | same |
| Element replacement threshold | +8% to +10% resistance | MFR | "Typically, when the element has increased in resistance by 8% to 10%, it is time to change it." | https://hotkilns.com/support/pottery-kiln-knowledgebase/elements-how-can-i-extend-ceramic-kiln-element-life |
| Element life by temperature | 2100°F element lasts ~2x a 2200°F one and ~4x a 2300°F one; **no firing count given** | MFR | "an element operated at 2100°F (1150°C) may last twice as long as an element operated at 2200°F (1200°C)" · "but four times as long as an element operated at 2300°F (1260°C)" | same |
| Relay life | ~200,000 cycles ≈ 173 firings (8-h firing, 25-s cycle) ≈ "18+ months" | MFR | "Relays generally last around 200,000 cycles on average" · "roughly 18+ months depending on usage, firing temperature, and firing profile" · "200,000 divided by 1152 = 173 kiln firings." · relays "can fail long before or long after without any explanation." | https://hotkilns.com/support/pottery-kiln-knowledgebase/relays-how-long-do-relays-last |
| Kiln wash on shelves | ~3 coats; inspect visually; no interval | MFR | "Three coatings are a rule of thumb but can certainly vary from person to person" · "Just visually inspect the coating from time to time" | https://hotkilns.com/support/pottery-kiln-knowledgebase/shelves-how-many-times-do-i-coat-my-shelves-kiln-wash |
| Vent-Sure downdraft vent life | Years, condition-dependent; flexible aluminium duct is the weak part | MFR | "If you have good conditions the vent can last for many years." · "The most vulnerable part seems to be the thin aluminum flexible duct that goes from the kiln to the vent motor." | https://hotkilns.com/support/pottery-kiln-knowledgebase/how-long-does-vent-sure-downdraft-vent-last |
| Kiln life | ~10 years default; use matters more than age | MFR | "a nice safe middle number is about 10 years" · "the longevity of a kiln is based much more on how it is being used and maintained, rather than how old it is" | https://hotkilns.com/support/pottery-kiln-knowledgebase/longevity-how-long-does-kiln-last |

Takeaway: L&L's own schedule is counted **in firings** (1 / 10 / 30) and asks the owner to keep a kiln log and track resistance over time. That is a natural per-asset counter + checklist data model. L&L gives no element life in firings, only relative life by temperature.

Skutt (skutt.com) returned **HTTP 403** to every fetch on 2026-10-20 (home page and KilnLink FAQ), so Skutt guidance must come from copies hosted elsewhere or is listed under Gaps.

Paragon (paragonweb.com now redirects to paragonkilns.com, a Shopify store): the FAQ ("Sorry, No FAQs found" without JavaScript) and manual list are rendered client-side, so no Paragon maintenance text could be read (accessed 2026-10-20). Gap.

#### A2. Practitioner rules of thumb and other sources on component life

| Item | Number | Label | Quote | Source |
|---|---|---|---|---|
| Thermocouple replacement | every 30-50 firings (type K) | PRAC (US; Lakeside Pottery, which publishes these tips as a teaching studio; another page on the same site is titled as a made-to-order and restoration studio, so treat the segment as uncertain) | "K style thermocouples need to be replaced about every 30-50 firings." | https://lakesidepottery.com/Pages/Pottery-tips/computerized-electric-kiln-troubleshooting-reference-guide.htm (undated; accessed 2026-10-20) |
| Elements and relays | ~80 firings at cone 5-6 | PRAC (same) | "If firing to Cone 5-6, about every 80 firing" · "When start seeing increase firing duration by over 30 minutes, time to change elements and relays" | same |
| Log | each firing's duration | PRAC (same) | "Record each firing duration." | same |
| Relay life | 12-24 months, or "every 200 firings" (relayed second-hand) | VENDOR blog (TAP / SDS Industries, US) quoting Kiln Frog and "other artists" | "Kiln Frog recommends replacing them every 12-24 months." · "Other artists recommend replacing them every 200 firings." · "Solid-state relays, on the other hand, usually last the lifespan of your kiln (or longer!)." | https://www.kilncontrol.com/blog/kiln-relays/ ("Updated August 7, 2026") |
| Relay life spread | 50 to 600 firings | PRAC, **UNVERIFIED** (search snippet of a Ceramic Arts Daily thread; forum returns 403) | snippet paraphrase only: relay life "can vary from 50 firings to 600 firings depending on the kiln and the kiln room environment" | https://community.ceramicartsdaily.org/topic/42164-i-want-to-troubleshoot-by-diagnostic-numbers-not-the-%E2%80%9Cvibes%E2%80%9D |
| Element resistance gauge | ±10% from new-element baseline; check when re-seating; no schedule | PRAC (trade article, Ceramic Arts Network, Chris Vaughn; published 2025-04-01, first published 2024) | "A reading of +/- 10% or greater from the baseline resistance values of new elements" "is a good general gauge for when it's time for replacement," | https://ceramicartsnetwork.org/daily/article/how-to-re-seat-bulging-electric-kiln-elements |
| L&L contradicts itself on relay life | One L&L page: ~173 firings / "18+ months"; another: "many years" | MFR | "Under normal circumstances, relays will last many years even when the kiln is fired every day." · "The biggest contributing variable to why a relay would fail is the environment, specifically heat." | https://hotkilns.com/support/pottery-kiln-knowledgebase/relays-my-contactors-relays-are-wearing-out-too-quickly-what-can |
| L&L asks owners to send Genesis log files for diagnosis | — | MFR | "If you are trying to troubleshoot a problem send your log files to service@hotkilns.com for analysis." | https://hotkilns.com/node/5269 |

How a member-run studio documents kiln maintenance (example): ENTS (CA probably; makerspace, member-run) keeps a BookStack wiki page per kiln with **nominal element resistances and in-spec ranges** (Bartlett V6-CF kiln "Jill": elements 1 and 6 nominal 14.2 Ω, range 12.78-15.62; elements 2-5 nominal 17.9 Ω, range 16.11-19.69), a 10% replace-all rule, relay bench-test notes and a post-repair test fire; "Replacing elements" is still "TODO"; created 2024-05-10, updated 2025-05-17. [PRAC] "Using a multimeter, record the resistance of all 6 elements" · "If any of the 6 elements are more than 10% out of spec, replace all elements (values are recorded below)" · "Do not perform troubleshooting without approval from the Board." https://docum.ents.ca/books/pottery/page/troubleshooting/export/html

Spread of element/relay/thermocouple life figures found so far: thermocouple 30-50 firings (PRAC) vs "outlast elements" (PRAC snippets); relay 173 firings (L&L MFR worked example) vs "many years" fired daily (another L&L MFR page) vs 80 (Lakeside PRAC) vs 200 (artists, via TAP) vs 50-600 (forum, UNVERIFIED); element replacement at +8-10% (L&L MFR) or ±10% (CAN, ENTS) resistance change. The spread itself is the point: life must be learned per kiln, which is why TAP lets users enter their own thresholds.

#### A3. Other equipment (manufacturer pages, accessed 2026-10-20)

| Item | Finding | Label | Quote | URL |
|---|---|---|---|---|
| North Star slab roller (US) | Marketed as near maintenance-free; canvases are the consumable ($58.99 for a 30" set; roller package $1,849.99 at Clay-King) | VENDOR/SUPPLIER | "Rust free and virtually maintenance free." · "No chains, cables or cast-iron gears." · canvas set "Easily Replaced When Discolored or Worn" | https://www.clay-king.com/product/north-star-551-slab-roller-30/ |
| Shimpo wheels, pugmills, slab roller (US/JP) | Only troubleshooting PDFs (2019 uploads) for vacuum-pump oil (NVA-04S), gear-case grease (NRA-03S/07S), chain tension (SR-3050); the oil PDF is a scanned image WebFetch cannot read. **No interval found** | MFR | "Troubleshooting (Vacuum Pump Oil) Manual for NVA-04S Pugmill." · "Troubleshooting (Gear Case Grease) Manual for NRA-03S and NRA-07S Pugmill." | https://www.shimpoceramics.com/troubleshooting/ |
| Peter Pugger (US) | FAQ has no maintenance content; no manual online | MFR | — | https://peterpugger.com/history/faqs/ |
| Olympic Kilns (US) | olympickilns.com returned 403 | — | — | Gap |
| Gas kilns (Bailey, Geil) | No pottery-kiln-maker maintenance schedule found in one search; results were industrial burner guides (not used) | — | — | Gap |

Takeaway: outside kilns, manufacturers publish almost no time- or count-based maintenance schedule for studio equipment. Wheels, slab rollers and pug mills are repaired when they fail.

#### A4. Kiln furniture and wear-part prices (US supplier, Seattle Pottery Supply site search, accessed 2026-10-20) [SUPPLIER]

| Item | Price as listed |
|---|---|
| 6" × ½" round mullite shelf | $12.00 |
| 10" × ½" full shelf (Skutt 122/123) | $19.00 |
| 15" × ⅝" half shelf, round | $29.00 |
| 15" × ⅝" full shelf (184/186) | $47.00 |
| 21" × 11" × 1" CoreLite semi-hollow shelf | $75.00 |
| Kiln shelf kit | from $125.00 |
| Ceramic-fibre shelf paper 20.5" × 20.5" | $7.50 |
| Relay, 30 A 240 V (Hongfa) | $59.70 |
| Skutt thermocouple, 18" | $65.00 |
| Element pack, Skutt 1027/1018 240 V | $112.49 |
| Kiln-sitter porcelain tube assembly | $96.00 |
| Olympic 18" top-loading kiln (1818HE/1827HE) | from $2,475.00 |

Sources: https://seattlepotterysupply.com/search?q=advancer+shelf&type=product · https://seattlepotterysupply.com/search?q=skutt+1027&type=product

Scale: wear parts are $60-115 each and a kiln needs several (one element pack per section, one relay per section), so a re-element plus relays on one kiln is a few hundred dollars of parts, and the kiln itself costs thousands. This is small next to the downtime and replacement costs in §I1.

### B. What kiln controllers record about maintenance (adds to 03-tools-in-use §3, which covers prices and APIs)

| Controller | Maintenance-relevant data it records or exposes | Label | Quote | URL (accessed 2026-10-20) |
|---|---|---|---|---|
| **TAP (SDS Industries, US)** | **Component-life counters** for relays, elements and thermocouples, with user-set thresholds, a Diagnostics usage view and replacement alerts; error log; real-time cost from $/kWh. Free tier includes the counters; Premium ($4.99/mo or $49.99/yr) adds graphs and emailed log export | VENDOR | "Set threshold values for relay, element, and thermocouple life." · "View and clear relay, element, and thermocouple usage" · "View and delete firing logs and view and clear error logs." · "Export your logs from your device via email." · alert text: "time to replace these components before they impact the performance of your kiln." · values come from datasheets ("Contact Rating Cycles" or "Mechanical Endurance") or the kiln maker | https://help.kilncontrol.com/knowledge/premium-subscription-vs-free-subscription · https://help.kilncontrol.com/knowledge/life-expectancy |
| **Bartlett Genesis (on L&L, Olympic, Evenheat; Paragon controllers made by Bartlett per 03)** | Built-in diagnostics and error codes, live graph, exportable logs; amps/volts and cost reporting depend on an optional current sensor (per 03-tools-in-use and UNVERIFIED search snippets). No firing counter or maintenance reminder documented on the pages read | VENDOR | "Built-in diagnostics testing, alarm, and error codes." · "Free Kiln Aid app for monitoring; optional Bartlett subscription adds notifications" | https://hotkilns.com/node/5098 |
| **KilnAid / KilnAid+ (Bartlett)** | Remote status of all claimed kilns; alarms in paid tier ($1.99/mo, $19.99/yr). No history, counters or maintenance on the product page | VENDOR | "List view shows limited status for all kilns" · "Alarm notifications, advanced monitoring and more available for $1.99/month or $19.99/year." | https://bartlettinstrument.com/kilnaid |
| Genesis diagnostics in practice | A Genesis 2.0 owner could not find guidance on interpreting the amps/volts diagnostics; a reply says the diagnostics don't help diagnose wear | PRAC, **UNVERIFIED** (search snippet; CAD forum 403) | thread title: "I want to troubleshoot by diagnostic numbers, not the 'vibes'" | https://community.ceramicartsdaily.org/topic/42164-i-want-to-troubleshoot-by-diagnostic-numbers-not-the-%E2%80%9Cvibes%E2%80%9D |
| **Orton AutoFire** | Logging only through optional Windows software | VENDOR | "Monitor and log your kiln firings with optional computer software (for Windows operating systems)" | https://www.ortonceramic.com/autofire-controllers |
| **Nabertherm MyNabertherm (DE)** | Malfunction list with descriptions, push alerts, run times, multiple furnaces; "stored furnace data" for support. No maintenance intervals on the page | VENDOR | "Display of malfunctions with an associated description in the individual overview and in a message list" · "Stored furnace data facilitate rapid support for you" | https://nabertherm.com/en/mynabertherm-app |
| Skutt KilnLink | Not re-checked: skutt.com 403. 03-tools-in-use records amperage per section, voltage, firing graphs, notes, $100/yr per kiln | — | — | see 03-tools-in-use §3 |

Takeaway: only TAP (a retrofit/Evenheat controller) has a built-in maintenance counter. The controllers on most US studio kilns (Bartlett Genesis on L&L/Olympic, Skutt KMT) expose diagnostics and logs but leave maintenance history to the owner. None found exposes a documented API (per 03).

### C. Supplier facts: clay pack sizes, prices, minimums, lead times, freight (segment n/a; accessed 2026-10-20)

| Supplier (country) | Fact | Label | Verbatim quote | URL |
|---|---|---|---|---|
| Laguna Clay (US) | Moist clay sold online **per lb**; mid-fire list $0.61-1.39/lb (e.g. WC401 B-Mix ^5 $0.85/lb, WC437 Frost porcelain $1.39/lb); Shopify variant weight 454 g = 1 lb | SUPPLIER | "Regular price $0.85 / lb" | https://lagunaclay.com/collections/clay-mid-fire-5-6 · https://lagunaclay.com/collections/clay-mid-fire-5-6/products.json?limit=5 |
| Laguna Clay (US) | Online order window 50-250 lb in 50 lb steps; larger orders by phone; B-Mix ^5 showed sold out | SUPPLIER | "Min Order 50lbs. - Max Order 250lbs." · "Order quantity: 50–250 (increments of 50)" · "Please call for larger orders." · "Sale Sold out" | https://lagunaclay.com/products/b-mix-5-moist-clay-boxed |
| Laguna Clay (US) | Lead time | SUPPLIER | "Orders ship in 7-9 business days." | https://lagunaclay.com/policies/shipping-policy |
| Standard Clay (US; standardceramic.com → standardclay.com) | No direct retail; distributor-only pricing | SUPPLIER | "For all retail pricing please visit Ceramic Supply Pittsburgh or find a distributor near you." | https://www.standardclay.com/pages/pricing |
| Clay-King (US distributor, Standard 266G) | $1.05 shown; ordered in 25 lb steps; full pallets ship from the mill; limited stock | SUPPLIER | "Clay must be ordered in 25lb increments. Full pallets ship direct from Standard." · "Limited Quantity In Stock At Clay-King!" | https://www.clay-king.com/product/standard-clay-266g-umbria-with-grog-mid-range/ |
| Clay-King (same page) | Tier table 25 lb $1.05/lb, 50 lb $0.95/lb, 100 lb $0.90/lb, full pallet 3,000 lb $0.78/lb | SUPPLIER, **UNVERIFIED** (search snippet; table is client-rendered and not returned by WebFetch) | — | same |
| Continental Clay (US) | Truck freight from 350 lb; liftgate $35-75; residential $30-40; 3-5 business days processing; no web estimate over 200 lb | SUPPLIER | "Common carrier truck is normally used for 350 lbs. and over." · "A $35 to $75 lift gate charge is applicable in addition to a slight delay." · "allow 3-5 business days for orders to be processed, packed and shipped." · "Automated shipping estimates are not calculated or shown for orders greater than 200 lbs." | https://www.continentalclay.com/Shipping_Policy.php |
| Continental Clay (US) | Price volatility: clay and raw-material prices must be confirmed at order | SUPPLIER | "some Clay Prices, Raw Materials, and other products will need to be confirmed for availability" · "as they change daily, and inventory is often unpredictable." | https://www.continentalclay.com/subcats.php?cat_id=451 |
| Potclays (UK) | Clay sold by weight in 12.5 kg bags; ex-VAT price breaks for Premium Sanded Buff: 12.5 kg £9.38 (£0.75/kg) → 100 kg £59.79 (£0.60/kg) → 250-1000 kg £0.56/kg (−25% vs a single bag) | SUPPLIER | "Please note our clay is sold by approximate weight." · "Your total order weight for a specific clay body will be despatched in the most economic combination of packs possible." | https://www.potclays.co.uk/pyop-clay-clay...-getting-started/pyop-clay-clay...-getting-started-stoneware/premium-sanded-buff |
| Potclays (UK) | Pallet cap 950 kg = 76 bags because of tail-lift rules; order 1,000 kg to get the tonne price and they refund the difference | SUPPLIER | "the maximum net weight of clay we are now permitted to pack on standard pallet is 950kg (76 x 12.5kg bags of clay)." · "To achieve the best price for your clay, please add the full 1000kg to your cart when checking out" | same |
| Potclays (UK) | Lead times (notice dated July 2026) | SUPPLIER | "Our standard lead times for UK deliveries are 3-7 working days for parcels and 5-10 working days for pallet orders." | https://www.potclays.co.uk/latest-news/ |
| Pottery Supplies Online (AU, Brisbane) | Walker White Midfire 10 kg bag AUD 38.40 inc GST; **out of stock** at access; freight quoted by email for ≥ ~20 kg; pay before shipping | SUPPLIER | "For larger orders (approximately 20kg and over), we will email you the best possible freight quote within 2 business days." · "Please note that no orders will be shipped without full payment." | https://www.potterysuppliesonline.com.au/product/walker-white-midfire-clay-10kg/ |

Municipal studio purchase record (AU): City of Whitehorse "Creative Whitehorse" (Victoria; municipal/recreation) sells Clayworks Walkers 10 $25/pack, Clayworks RGH $32, Keanes White Raku $35, Keanes Toast $37 inc GST "As of 1st July 2025"; firing $14/kg bisque+glaze. Mechanism: **a paper tally sheet** — "All purchases to be recorded in the firing tally sheet" · "Clay purchased externally must have a completed label before going into the kiln" [STUDIO]. https://www.creativewhitehorse.vic.gov.au/sites/creative/files/2025-07/Clay%20%26%20Firing%20charges%201%20July%202025.pdf

Scale implications: a US studio buying by the pallet deals in ~2,000-3,000 lb lots shipped LTL with liftgate fees; a UK studio in ≤950 kg pallets of 12.5 kg bags; an AU studio in 10 kg bags with per-order freight quotes. Volume breaks (UK: −25% from one bag to 250 kg) make bulk buying and then reselling to members by the bag the normal economics.

### D. How studios record clay sales to members: mechanisms and their failure points (accessed 2026-10-20)

(Policy counts are another researcher's job; these are worked examples of each mechanism.)

| Mechanism | Studio (country; segment) | Verbatim quote | What can go wrong (inferred from the mechanism unless a quote says so) | URL |
|---|---|---|---|---|
| **Pay at office, then show receipt, then write it in a paper register** | Living & Learning Nillumbik (AU; municipal/recreation, council learning centre) | "Please select a bag of clay and come to the office with the clay bag number to make payment." · "You will need to show your receipt to the Studio Manager before you collect the bag." · "Please write the details, including your payment receipt number on the Clay Bag Register in the studio." · firing is bought in bulk and "will then be deducted from your pre-purchased amount." | Three hand-offs (office, studio manager, register) for one bag; the register and the till must be reconciled by hand; prepaid firing balance kept on paper | https://www.livinglearningnillumbik.vic.gov.au/Courses/View-all-courses-by-topic/Art-craft-and-clay/EL-Clay-and-Firing-Purchasing |
| **Buy online, then sign a paper binder with the order number** | Sebastopol Center for the Arts (US; nonprofit art centre) | "requires all clay be purchased from the SebARTs website." · "Studio users sign in to the studio's red binder using the purchase order number provided via email" · "after online purchase to then receive clay from the facilitator." · "Clay left beyond 60 days in the studio is considered abandoned and will be recycled." | Online sale and physical hand-over are separate records; a facilitator must be present; unclaimed clay needs a dated record to enforce the 60-day rule | https://www.sebarts.org/classeslectures/p/clay-purchase |
| **Paper tally sheet** | Creative Whitehorse (AU; municipal/recreation) | "All purchases to be recorded in the firing tally sheet" | Honour-based; totals depend on members writing entries | see §C |
| **Front desk** | OSU Craft Center (US-OR; university/college, student craft centre) | "All clay used in the OSU Craft Center ceramics studio must be purchased from the OSU Craft Center" "(available at the front desk)." | Needs desk staffing during open hours | https://see.oregonstate.edu/craft-center/studios/ceramics |
| **Website or gift shop** | Rockport Art Center (US; nonprofit art centre) | "Clay is available for purchase through www.rockportartcenter.com or the RCA gift shop." · "Members are only allowed to use clay purchased from RCA." | Two sales channels whose stock must agree | https://www.rockportartcenter.com/ceramics-studio-policies |
| **Materials fee per sleeve used (class students)** | Berkshire Art Center (US; nonprofit art centre; page "last updated February 2026") | "For students, there is a materials fee for every 25-pound sleeve of clay used." · "More clay can be purchased, if needed." | Usage has to be counted per student | https://berkshireartcenter.org/ceramics-studio-guidelines-and-etiquette |
| **Tickets bundled with each bag** | City of Pickering (CA; municipal) | (already in 03-tools-in-use §2: 20 bisque + 20 glaze tickets per bag) | — | https://www.pickering.ca/potterystudio |

Why studios insist on selling the clay (the asset-protection link):
- Berkshire Art Center (US; nonprofit art centre; Feb 2026): "Clay and glaze materials not purchased at Berkshire Art Center require authorization before being fired" · "You will be responsible for damage to our equipment by firing a low fire clay or glaze." · repairs "can be upwards of $1,000." · glaze grinding "you will be fined $35 per piece." [STUDIO]
- Rockport (above): outside commercial glazes "must be approved by an instructor prior to submission for firing" and "An ingredient sheet (MSPS) should be provided." [STUDIO] (also a HazCom-style document request; see §G).
- UNVERIFIED (search snippets only, not opened): North Little Rock library studio says firing clay at the wrong temperature can damage kilns, so outside clay needs Studio Manager approval; outright outside-clay bans at John Burrison Pottery Studio, Santa Paula Art Museum, Hillsboro (OR) and the UVM co-op (Fall 2026).

So the clay-sale record does double duty: revenue, and proof that what goes into the kiln is a known clay body. That ties clay inventory to kiln (asset) protection.

### E. Glaze: studio-mixed and commercial (accessed 2026-10-20)

| Fact | Label | Country; segment | Verbatim quote | URL |
|---|---|---|---|---|
| Volunteer glaze team of 5-6 maintains 12 core glazes; each assistant owns a "stable" of glazes, regulates SG and levels; new batch at a fill line; 7,500 g batches ≈ ¾ of a 5-gal bucket; batch date recorded to track usage; test tiles of every studio clay body per batch; weigh-off checklist on each recipe sheet; shared base recipes reduce materials stocked | PRAC **(2018)** | US-VA; nonprofit art centre (Workhouse Arts Center, Lorton) | "The Workhouse Glaze Team consists of 5 to 6 volunteers or assistants." · "That responsibility includes regulating the specific gravity of the glazes in the buckets, as well as monitoring glaze levels and quantities." · "Assistants mix a new batch when a glaze hits a predetermined refill level, denoted by a fill line on the bucket." · "We have our glaze recipes written in 7500-gram batches." · "They also have a place for the batch date so that we can track usage." · "we immediately dip test tiles of each studio clay body into the new batch of glaze." · "our glaze palette consists of 12 core glazes" · shared bases help "to cut down on the number of materials we need in stock." | https://ceramicartsnetwork.org/pottery-making-illustrated/pottery-making-illustrated-article/In-the-Studio-Group-Studio-Glaze-Management ("Appears in the Nov/Dec 2018 issue of Pottery Making Illustrated") |
| SG targets: dipping 1.4-1.5 (self-mixed ~1.45); commercial brushing 1.25-1.55; casting slip 1.75-1.8; glazes settle into a hard layer; pick a target per glaze and stick to it | Technical reference (Digitalfire glossary; undated; not a manufacturer) | n/a; n/a | "Instructions for dipping glaze products can vary from recommending 1.4 SG (for a 5 second dipping time) to 1.5 (for a 1 second time)." · "generally do best with them in the 1.45 range." · "Commercial brushing glazes can have specific gravities ranging from below 1.25 up to 1.55." · "Casting slips typically have specific gravities of 1.75-1.8." · "determine, by experience with a specific glaze, what specific gravity and flocculation procedure works well, then stick to that for future batches." | https://digitalfire.com/glossary/specific+gravity |
| Commercial glaze pack size and price: Mayco Stroke & Coat 12 × 1 pint (16 oz) kit $232.00 list / $220.40 sale (≈ $18.37/pint); **out of stock** with an estimated arrival of about 3 weeks (paraphrase) | SUPPLIER | US; n/a (used by PYOP, schools, studios) | "Mayco Stroke & Coat glazes are intended to fire at cone 06/05 but can be fired up to cone 6 with some color variation." | https://www.clay-king.com/product/mayco-stroke-coat-kit-1-set-of-12-pints/ |

Glaze raw materials, pack sizes and price breaks (US supplier, Seattle Pottery Supply public Shopify product JSON, accessed 2026-10-20) [SUPPLIER]: Silica 325 mesh — 1 lb $4.52 · 5 lb $12.06 · 10 lb $18.56 · 25 lb $35.31 · 50 lb $50.00, i.e. **$4.52/lb in a 1 lb bag vs $1.00/lb in a 50 lb bag**. Other glaze materials on the same site start at $4.25-8.53 for the smallest bag (kaolins, Frit 3269). https://seattlepotterysupply.com/products/silica-325m-90.json · https://seattlepotterysupply.com/search?q=EPK+kaolin&type=product

Implication: a studio glaze is a recipe + batch + bucket with a target SG, a refill line and a batch date. That is inventory with a quality-control record per batch, consuming raw materials by the gram. The Workhouse runs it on paper sheets and volunteers (2018). 03-tools-in-use found Insight-Live is the only group-capable glaze tool.

### F. PYOP bisque and paint (accessed 2026-10-20)

| Fact | Label | Country; segment | Verbatim quote | URL |
|---|---|---|---|---|
| Bisque assortment of 400+ SKUs | STUDIO (tourism listing text) | US-VA; PYOP (Glazed Bisque-It, Roanoke) | "choose from over 400 pottery pieces, including dinnerware, home décor, children's items, and seasonal selections" | https://www.visitroanokeva.com/listings/glazed-bisque-it/8799/ |
| Bisque assortment of 300+ SKUs | STUDIO (tourism listing text) | US (Jacksonville); PYOP (Doing Dishes) | "We have over three hundred pieces of bisque to choose from" | http://www.visitjacksonville.com/directory/doing-dishes-attractions/ |
| Wholesale bisque: sold by the case (rice bowl, case of 6, $22.50); **$150 minimum**; free US shipping over **$1,000**; drop-shipped from maker; ~1 week lead time | SUPPLIER | US; PYOP supply | "The minimum order of wholesale bisqueware is $150." · "Free US shipping on wholesale bisqueware orders after $1,000." · "This item ships directly from the manufacturer. Estimated lead time of 1 week." | https://www.clay-king.com/product/wholesale-bisqueware-rice-bowl/ |
| Bisque Imports items sold "Case of 4" or single | SUPPLIER, **UNVERIFIED** (search snippet) | US | — | https://www.clay-king.com/product/bisque-imports-1255-star-box/ |

Implication: PYOP is the segment with true retail-style SKU inventory (300-400+ shapes, seasonal lines, case packs, minimums, reorder lead times) plus paint stock by colour. This is classic POS inventory territory (Square/Shopify/Lightspeed), not a pottery-specific gap. 03-tools-in-use already found Square used by a PYOP studio and Color Me Mine rolling out a franchise POS.

### G. Record-keeping obligations (accessed 2026-10-20)

| Rule (country) | What it requires of a small studio | Label | Verbatim quote | URL |
|---|---|---|---|---|
| OSHA Hazard Communication, 29 CFR 1910.1200 (US) | **Employers** keep a written HazCom programme with **a list of hazardous chemicals present** (by product identifier matching the SDS), keep **an SDS for each**, readily accessible every shift. Consumer products used as a consumer would are exempt. *My reading, not legal advice:* bottled commercial glazes may fall under that exemption while bulk dry glaze chemicals (silica, frits, oxides, carbonates) would not; and the duty attaches to employers, so a member-run co-op with no staff may sit outside it while studios with paid techs or teachers sit inside | REG | (b)(1) "all employers to provide information to their employees about the hazardous chemicals to which they are exposed." · (e)(1) "Employers shall develop, implement, and maintain at each workplace, a written hazard communication program" · (e)(1)(i) "A list of the hazardous chemicals known to be present using a product identifier" "(the list may be compiled for the workplace as a whole or for individual work areas)" · (g)(8) "The employer shall maintain in the workplace copies of the required safety data sheets for each hazardous chemical," "shall ensure that they are readily accessible during each work shift to employees when they are in their work area(s)." · (b)(6)(ix) "Any consumer product or hazardous substance, as those terms are defined in the Consumer Product Safety Act" (exemption when used as intended at consumer-level exposure) | https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.1200 |
| COSHH assessment (UK) | Written COSHH assessment mandatory at **5+ employees**; advised below that; review regularly | REG (HSE guidance, page dated 2025-07-03) | "If you have 5 or more employees, you must record your assessment" · "even if you have fewer than 5, it makes sense to write down what steps you have taken to identify the risks." · "You must review the controls you have put in place to make sure they are working." | https://www.hse.gov.uk/coshh/basics/assessment.htm |
| COSHH reg 9: LEV (spray booths, extraction) and RPE (UK) | **Thorough examination and test of local exhaust ventilation at least every 14 months**; RPE (non-disposable) examined at suitable intervals; **records of tests and repairs kept 5 years**. This is the clearest legally mandated, dated equipment-maintenance record found for a ceramics studio | REG | LEV examined and tested "at least once every 14 months," · RPE: "ensure that thorough examination and, where appropriate, testing of that equipment is carried out at suitable intervals." · "a suitable record of the examinations and tests carried out." · "shall be kept available for at least 5 years from the date on which it was made." | https://www.legislation.gov.uk/uksi/2002/2677/regulation/9 |
| HSE "COSHH essentials in ceramics" (UK) | Six task sheets for the ceramics industry: CR0 Advice for managers, CR1 Glaze and colour preparation, CR2 Casting, CR3 Fettling, CR4 Kiln loading and unloading, CR5 Spraying glazes and colours (list page dated 2024-08-14; CR0 PDF metadata "COSHH essentials in ceramics: Silica", updated 2023-05-03). Sheet text not readable by WebFetch | REG guidance | — | https://www.hse.gov.uk/pubns/guidance/crseries.htm |
| WHS hazardous chemicals register (AU) and HSWA hazardous-substances inventory (NZ) | **Not verified.** safeworkaustralia.gov.au timed out twice; worksafe.qld.gov.au, safework.sa.gov.au and worksafe.act.gov.au returned 403; legislation.gov.au (model WHS Regulations) timed out; worksafe.govt.nz failed (header overflow); web-search budget exhausted before a mirror was found | — | — | Gap |

Studio-level evidence of SDS handling: Rockport Art Center (US; nonprofit art centre) asks members bringing commercial glazes for "An ingredient sheet (MSPS)" and says "Documentation on glazes and other materials are available by request." [STUDIO] https://www.rockportartcenter.com/ceramics-studio-policies

Fire and electrical items for kilns (US manufacturer checklist; codes themselves not reachable: codes.iccsafe.org returned 403):

| Item | Label | Verbatim quote | URL |
|---|---|---|---|
| Clearances and floor | MFR (L&L installation checklist) | "Ensure a 12" clearance (18" recommended) from non-combustible surfaces and 36" from combustible surfaces." · "Install the kiln on a non-combustible floor, 2" thick, extending 12" beyond the kiln" | https://hotkilns.com/node/5388 |
| Electrical | MFR | "Hire a licensed electrician for the installation" · "Ensure the kiln will be located within 50' of the breaker" · "(if required) Install a fused disconnect switch for lockout/tagout procedures" · "Determine if my kiln is listed to UL499 or Canadian Standards" | same |
| Fire | MFR | "Have a regularly inspected fire extinguisher nearby" · "(if applicable) Consult local fire codes regarding sprinkler systems" | same |
| UL 499 listing | MFR | "some public buildings (like schools) may require it" · "check with your local code inspector to be sure." · "all L&L kilns are designed to meet UL499 and National Electrical Code standards as we interpret them." | https://hotkilns.com/support/pottery-kiln-faqs/certification-standards-what-ul499-listing |

These are one-off installation facts per kiln (clearance, circuit, listing, extinguisher inspection date) — a natural set of fields on a kiln asset record, but not a recurring workload.

Fixed-asset depreciation (does tax force an asset register?):

| Rule (country) | Fact | Label | Verbatim quote | URL |
|---|---|---|---|---|
| IRS de minimis safe harbor (US) | Items up to **$2,500 per invoice or item** can be expensed (no applicable financial statement); policy must exist at start of year | REG | "If you don't have an AFS, you may use the safe harbor to deduct amounts up to $2,500" · "consistent accounting procedure or policy existing at the beginning of the taxable year." | https://www.irs.gov/businesses/small-businesses-self-employed/tangible-property-final-regulations ("Page Last Reviewed or Updated: 04-Aug-2026") |
| Section 179 / bonus (US) | 2026 §179 limit **$2,560,000**; 100% bonus depreciation reinstated for property acquired after 2025-01-19 | REG | "the maximum section 179 expense deduction is $2,560,000" · One Big Beautiful Bill Act "reinstated the 100% special depreciation allowance" | https://www.irs.gov/publications/p946 ("For use in preparing 2025 Returns") |
| Annual Investment Allowance (UK) | **£1 million** a year of plant and machinery deductible in full | REG | "The AIA amount is £1 million." · "You can claim AIA on most plant and machinery up to the AIA amount." | https://www.gov.uk/capital-allowances/annual-investment-allowance |
| CCA Class 8 / 12 (CA) | Equipment 20% declining balance; small tools under $500 fully deductible | REG | "Class 8 with a CCA rate of 20% includes certain property that is not included in another class." · "furniture, appliances, tools costing $500 or more per tool, some fixtures, machinery" · small tools "are fully deductible in the year of purchase." | https://www.canada.ca/en/revenue-agency/services/tax/businesses/topics/sole-proprietorships-partnerships/report-business-income-expenses/claiming-capital-cost-allowance/classes-depreciable-property.html (modified 2026-08-31) |
| Instant asset write-off (AU) | Not verified: ato.gov.au returned 403 | — | — | Gap |

Implication: in the US and UK, a small studio can usually expense a kiln or wheel in the year of purchase (§179/bonus; AIA), so tax rarely forces a multi-year depreciation schedule. Canada's 20% CCA does. Depreciation is the accountant's job and lives in the accounting package; it is weak support for building an asset register for tax reasons. Insurance schedules were not verified (no insurer page reached; see Gaps).

Implication: the chemical list + SDS binder (US), COSHH assessment and LEV test log (UK) are *document* obligations attached to *inventory* (what chemicals are on the shelf) and *assets* (the extraction system). A materials inventory that stores an SDS link per material would directly produce the OSHA list; an asset register with a 14-month LEV test reminder would produce the COSHH reg 9 record.

### H. Current tools for inventory and maintenance (vendor pages accessed 2026-10-20; all features and prices are VENDOR claims)

| Tool | What it offers for this use | Price (USD unless noted) | Studio shown using it? | URL |
|---|---|---|---|---|
| Paper registers, binders, tally sheets, clipboards | Clay-bag register with receipt numbers; red binder with order numbers; firing tally sheet | Free | **Yes: 3 of the 7 clay-sale mechanisms in §D are paper** (Nillumbik AU, SebArts US, Whitehorse AU); 03-tools-in-use found paper in ~10 of 35 back-office examples | §D |
| Square for Retail | Plus tier: "Inventory management and stock intake," "Vendor and purchase order management," "Cost of goods sold and inventory reporting"; low-stock alerts; cycle counts. Free tier has no inventory features | Prices not rendered to WebFetch | PYOP: Social Pottery UK (03, 2023) | https://squareup.com/us/en/point-of-sale/retail/pricing |
| Shopify POS Pro | "Perform inventory counts, complete purchase orders, and get low stock alerts from a single place." | "+ $89 USD/month for each POS Pro location" | Laguna, Seattle Pottery Supply etc. run Shopify (03) — suppliers, not studios | https://www.shopify.com/pos/pricing |
| Lightspeed Retail (X-Series) | PO sync via NuORDER, forecasting and order recommendations | Basic $89, Core $149, Plus $289 /mo | Tuckers (CA supplier) runs Lightspeed (03) | https://www.lightspeedhq.com/pos/retail/pricing/ |
| Craftybase (renamed **Stocksmith**) | Materials inventory, "Automatic COGS", "Batch & lot tracking", "Automated production assemblies"; QuickBooks inventory sync on Growth | Pro from $20/mo; Studio $41-49/mo; Indie $83/mo annual ($99 monthly); Business $166; Growth $291 | Not found (03: recommended by PMI 2024, no studio seen) | https://stocksmith.io/pricing/ ("Craftybase → Stocksmith" · "Same team, same software — just a new name.") |
| Katana | Materials + products, batch/serial traceability, kits via BOMs; Shopify on free tier; QuickBooks Online and Xero listed | Free up to 30 SKUs; Advantage custom annual | Not found | https://katanamrp.com/pricing/ |
| inFlow | Reorder points, barcodes, BOM (Manufacturing edition); QuickBooks Online and Shopify included | Inventory Lite $99/mo annual ($129 monthly), Core $299, Pro $499, Max $699; Manufacturing from $169; Stockroom from $69 | Not found | https://www.inflowinventory.com/software-pricing-inflow |
| Sortly | "Date-based Alerts so you can track maintenance and repair schedules for assets, tools, and equipment." · low-stock alerts · check-in/check-out · QR labels; QuickBooks Online only on Premium | Free (100 items, 1 user); Advanced $24/mo first year ($49 list); Ultra $74 ($149 list); Premium $149 ($299 list) | Not found (03: recommended by PMI 2024, no studio seen) | https://www.sortly.com/pricing/ |
| Zoho Inventory | Composite items; batch tracking on Premium; Shopify, QuickBooks Online sync | Free (50 orders, 1 user, 1 location); Standard $29/org/mo annual | Not found | https://www.zoho.com/inventory/pricing/ |
| UpKeep (CMMS) | PM scheduling and checklists (Premium), meter readings, asset lifecycle, parts inventory | Essential $24/user/mo; Premium $55/user/mo | Not found | https://upkeep.com/pricing/ |
| MaintainX (CMMS) | Free Basic plan: unlimited work orders but only "2 active repeating Work Orders"; **meter-based maintenance ("Log meter readings and trigger Work Orders automatically at certain thresholds.") only on Premium** | Basic $0; Essential $20/user/mo annual; Premium $65/user/mo annual | Not found | https://www.getmaintainx.com/pricing |
| Limble (CMMS) | "Meter and Threshold Based Scheduling" and spare parts only on Premium+; prices via calculator | Not public | Not found | https://limble.com/pricing |
| Fabman (makerspace) | "Track required maintenance and tell users about downtimes." · per-minute equipment usage billing · Stripe payments. No consumables/materials stock found | €49-199/mo + per machine (03) | Makerspaces (03); no pottery studio | https://fabman.io/product |
| Glazy / Insight-Live | Glazy: material inventory with prices → batch cost; Insight-Live: only group-capable glaze tool | see 03-tools-in-use §3 | Not found in studio sources (03) | — |

Fit to a kiln: the natural maintenance trigger is **firing count** (L&L: every firing / 10 / 30). Generic CMMS tools put count- or meter-based triggers in paid upper tiers (MaintainX Premium $65/user/mo; Limble Premium+; UpKeep meter readings tier unclear). Sortly's maintenance alerts are date-based. None of these tools reads a kiln controller. Only the TAP controller counts component usage itself (§B).

### I. Pain evidence (accessed 2026-10-20)

Access note: Reddit is unreachable from this environment (WebFetch: "unable to fetch from www.reddit.com" and old.reddit.com; WebSearch with reddit.com as a domain filter: "not accessible to our user agent"). The Ceramic Arts Daily forum returns 403. So practitioner forum voice is largely missing; the evidence below is from fundraisers, news, studio pages and trade articles.

#### I1. Equipment failure and downtime (8 independent events, 6 of them dated 2024-2026, plus 1 advice article)

| # | Event | Country; segment; date | Label | Verbatim quote | URL |
|---|---|---|---|---|---|
| 1 | Kiln control panel destroyed; ~16 members' firing stopped; ~$6,500 to replace panel, 3-phase cable, install and code work; target $4K CAD | CA-ON (Toronto); membership/community studio (shared studio at Akin Dupont); created 2024-08-07 | PRAC | "The kiln panel malfunctioned and was completely destroyed." · "The kiln fires work for approximately 16 studio members." | https://www.gofundme.com/f/Lorens-kiln |
| 2 | Kilns need new lid, bricks, elements; largest kiln not serviced in 8+ years; $6,000 goal ($2,412 raised) | US-CA (Castro Valley); PYOP + clay studio; created 2025-07-25 | PRAC | the kilns "are in dire need of repairs." · the largest kiln "hasn't had a real tune-up in over 8 years" | https://www.gofundme.com/f/help-arans-art-studio-repair-upgrade-our-kilns |
| 3 | Firing delayed waiting for a thermocouple | US (city not on the fetched page); nonprofit art centre (Fitton Center); 2025-06-03, by the Studio Manager | PRAC | "There is a small delay in firing for a few pieces due to an issue with one of the kilns." · "The small kiln is loaded, just waiting for a thermocouple replacement on Friday." | https://www.fittoncenter.org/grouppage/pottery-studio/discussion/8ca36d35-0a3e-4d64-bd43-f84f816be3d7 |
| 4 | Raku hood cable snapped; burners "already on their last legs" destroyed; replacement Raku kiln ~$3,800; wood kiln retired after ~130 logged firings | US (Yellow Springs); nonprofit art centre (began as a co-op, now a 501(c)(3)), John Bryan Community Pottery; 2026-07-10 | PRAC (news) | "the impact finished off burners that were already on their last legs." · "no injuries, but the kiln was done." · "Over its lifetime, it logged an estimated 130 firings." | https://ysnews.com/news/2026/07/john-bryan-community-pottery-wheels-into-new-era |
| 5 | Fire broke out in the kiln area; all kilns and wheels lost; $50,000 goal; ~$30,000 from 240+ donations by May; building insured by landlord association, equipment cover not stated | US-FL (St. Petersburg); teaching studio (inferred; segment not stated in the articles) (St. Pete Ceramics); 2026-01-28 and 2026-05-21 | PRAC (news) | "The fire broke out in the kiln area, Miranda said." · "We've lost all our kilns, wheels, and pretty much everything that helped make the space special." · "the 1,800-square-foot building itself was insured by the Warehouse Arts District Association." | https://patch.com/florida/stpete/fire-destroys-st-pete-ceramics-studio-owners-vow-rebuild · https://stpetecatalyst.com/arts-alive-st-pete-ceramics-is-pivoting-forward/ |
| 6 | Kiln "ready for retirement"; raising £10,000 | UK (Colchester); makerspace ceramics studio; created 2024-11-27 | PRAC | the kiln is "ready for retirement." | https://gofundme.com/f/rdhk9-fund-our-community-kiln |
| 7 | Shut-off gauge failed; kiln, shelves and contents destroyed; remaining kiln can't cope | US-NY; teaching studio (Supermud); **2016** | PRAC | "an essential gauge in one of those kilns failed to shut it off." · "the kiln, shelves and contents were damaged beyond repair." | https://www.gofundme.com/f/supermud |
| 8 | Ventilation overhaul needed; firing stopped ~2 months for 13 members; $15,000 | US-WI (Middleton); cooperative (Madison Potters' Guild); **2014** | PRAC | "firing has come to a standstill" · "nearly two months now" · "all thirteen of our member potters." | https://www.gofundme.com/f/HelpTheMPG |
| 9 | Business case in trade press: unplanned kiln breakdowns disrupt schedules | n/a; advice article (PMI Nov/Dec 2024) | PRAC (editorial advice, not an event) | "If a kiln breaks down unexpectedly, it can disrupt the entire studio's schedule," "leading to missed deadlines and frustrated clients." | https://ceramicartsnetwork.org/pottery-making-illustrated/pottery-making-illustrated-article/in-the-studio-streamlining-for-success |

Downtime durations found: "nearly two months" (#8, 2014); days (#3). Costs found: $3,800 (Raku kiln), ~$6,500 (panel + install), $6,000 (repairs), £10,000 (new kiln), $15,000 (ventilation), $50,000 (fire rebuild). **Count: 8 real events (6 dated 2024-2026) + 1 advice article.** Only 1 of 8 (#2) explicitly ties the problem to missed maintenance ("hasn't had a real tune-up in over 8 years"); #4 implies worn burners. Most are sudden failures or end-of-life replacements, i.e. *capital* problems (funding a replacement) more than *scheduling* problems.

#### I2. Materials: running out, waste, theft, glaze batches

| # | Signal | Country; segment; date | Label | Verbatim quote | URL |
|---|---|---|---|---|---|
| 1 | Low supplies reported by members to the board; supply requests go to a committee; volunteers do kiln maintenance | CA (probably; Plainsman clay); makerspace (ENTS, member-run); wiki entries 2024 | PRAC | "Notifying the Board of Directors if any supplies are running low" · "Requests for glazes, tools and other supplies can be submitted to the Pottery Committee for consideration." · "Trained & certified members volunteer their time to load and unload the kilns and do kiln maintenance." | https://docum.ents.ca/books/pottery/export/html |
| 2 | Glaze waste dumped in a storm drain by staff; studio paying fines and clean-up | US-CA (Santa Monica); PYOP (Color Me Mine franchise); ~May 2024 | PRAC (news) | "We are working with the city to pay for fines and the cleanup process as well." | https://ktla.com/news/local-news/santa-monica-pottery-studio-responds-after-video-surfaces-of-employees-dumping-glaze-in-storm-drain/amp/ |
| 3 | Supplier stock-outs at time of access: Laguna B-Mix ^5 "Sold out"; Mayco 12-pint kit "Out of stock" (about 3-week ETA); Walker White Midfire 10 kg "Out of stock"; Clay-King "Limited Quantity" on Standard 266G | US, AU; supplier; 2026-10-20 | SUPPLIER | see §C, §E | §C, §E |
| 4 | Supplier price volatility | US; supplier (Continental Clay) | SUPPLIER | prices "change daily, and inventory is often unpredictable." | §C |
| 5 | Raw-material batch variation forces record keeping and testing; overstock ties up capital; manual tracking "error prone" | n/a; advice article (PMI Nov/Dec 2024) | PRAC (editorial advice) | "different batches of raw materials may have slight variations in composition" · "This necessitates meticulous record keeping and constant testing to avoid discrepancies in product offerings." · "Tracking inventory manually can be time consuming and error prone." | https://ceramicartsnetwork.org/pottery-making-illustrated/pottery-making-illustrated-article/in-the-studio-streamlining-for-success |
| 6 | Kiln damage from unapproved low-fire clay/glaze charged to members ("upwards of $1,000") | US; nonprofit art centre (Berkshire Art Center); Feb 2026 | STUDIO | see §D | §D |
| 7 | Theft of finished work from an honour-system pickup shelf | US-GA (Athens); PYOP/kids studio (Good Dirt); **2017**; UNVERIFIED (search snippet only) | — | — | https://www.redandblack.com/athensnews/picking-up-the-pieces-art-studio-owners-reported-children-s-pottery-projects-stolen/article_cb791430-8422-11e7-856f-e768cef0f56c.html |

**Not found:** any first-hand report (2023-2026) of members stealing studio clay or glaze, of a studio running out of clay mid-term, or of a studio glaze batch failure costing money. One search aimed at clay/glaze theft returned nothing on point. With Reddit and the CAD forum closed, this is absence of reachable evidence, not evidence of absence.

### J. Inventory classes and scale: summary

| Class | Unit and scale found | Typical price found | How it is recorded today | Sources |
|---|---|---|---|---|
| Clay sold to members | Bought by the pallet (US full pallet 3,000 lb at Clay-King, UNVERIFIED; UK ≤950 kg = 76 × 12.5 kg) or 50-250 lb online (Laguna); resold by the 25 lb bag/sleeve (US), 12.5 kg bag (UK), 10 kg bag/"pack" (AU) | Supplier: US $0.61-1.39/lb (Laguna); UK £0.56-0.75/kg ex VAT (Potclays); AU AUD 38.40 per 10 kg retail. Studio resale: AU $25-37 per pack (Whitehorse 2025); 03/other researcher has US studio prices | Paper register, binder, tally sheet, front desk, web shop, per-sleeve fee, tickets (§D) | §C, §D |
| Glaze raw materials | Grams per batch (7,500 g batches); 1-50 lb bags | Silica $4.52/lb (1 lb) → $1.00/lb (50 lb) | Recipe sheets with weigh-off checklist and batch date (Workhouse 2018) | §E |
| Studio-mixed glazes | 12 core glazes in 5-gal buckets with fill lines; SG ~1.45 | (labour + materials) | Volunteer team; paper | §E |
| Commercial glazes/underglazes | 2 oz, pint (16 oz) and kits of 12 | ~$18.37/pint (Stroke & Coat kit) | Not found | §E |
| Kiln furniture and kiln wash | Shelves per kiln; ~3 coats of wash | Shelves $12-75; kits from $125 | Not found | §A1, §A4 |
| Kiln wear parts | Elements, relays, thermocouples per kiln section | $59.70 relay, $65 thermocouple, $112.49 element pack | Wiki/log (ENTS); controller counters (TAP only) | §A4, §B |
| Tools and supplies for sale | — | — | Not found (gap) | — |
| Finished work, retail and consignment | 01-market-survey: 30-50% commission, monthly statements, 1099-NEC | — | Not re-researched (Wheelhouse NH Notion page did not render) | 01 |
| PYOP bisque and paints | 300-400+ bisque shapes; case packs (4, 6); $150 wholesale minimum; ~1 week lead time | $22.50 per listed case/unit (rice bowl) | POS (Square, franchise POS per 03) | §F |
| Cleaning and safety supplies | HSE has ceramics task sheets (CR0-CR5) | — | Not found (gap) | §G |

### K. Segment differences

| Segment | What it stocks and maintains | Evidence (country) |
|---|---|---|
| **PYOP** | Hundreds of bisque SKUs, case-pack reorders with minimums; commercial paints by colour; glaze-water disposal; kilns run by staff | Glazed Bisque-It 400+ and Doing Dishes 300+ (US); Clay-King wholesale terms (US); Color Me Mine Santa Monica fines (US, 2024); Aran's Art Studio kiln repairs (US, 2025). POS already covers SKU stock (03) |
| **Membership/community studios and nonprofit art centres** | Clay resale by the bag; studio glazes; kilns and shared equipment; outside-clay control | 7 clay-sale mechanisms (§D: US 4, AU 2, CA 1); Berkshire damage fees (US); Workhouse glaze team (US, 2018); 3 of 8 failure events in §I1 (Lorens/Toronto, Fitton, John Bryan); of the other 5, 2 are teaching studios (Supermud, St. Pete), 1 PYOP (Aran's), 1 cooperative (Madison) and 1 makerspace (Colchester) |
| **Cooperatives / member-run makerspaces** | Same as above, run by volunteers and boards; maintenance by "trained & certified members"; documentation in a wiki | ENTS (CA probable, 2024-25); Madison Potters' Guild (US, 2014); Colchester makerspace (UK, 2024) |
| **Production potteries** | Materials as cost of goods; batch consistency. **No primary production-pottery source reached.** Vendor content only: beancount.io blog (2026-07-20) illustrative medium bowl "near $21" production cost of which ~$3 clay, ~$4 firing, ~$8 labour; glaze 5-10 ml per piece; a gallon covers 60-100 pieces [VENDOR, illustrative, not data]. Stocksmith (ex-Craftybase) sells "Automatic COGS" to makers [VENDOR] | https://beancount.io/blog/2026/07/20/pottery-ceramics-studio-bookkeeping-kiln-glaze-cogs-guide · §H |
| **Universities/colleges** | Course materials fees; technician-run workshop sells extra materials; WHS induction by technician; student craft centre sells clay at the front desk | ANU ARTV2150 Ceramics 2 (AU, 2026): "There will be a materials fee for this course." · "You can purchase additional material from the Workshop and take advantage of the GST-free status." · Week 1 "WHS inductions with technician" (https://programsandcourses.anu.edu.au/2026/course/artv2150/first%20semester/4164) · OSU Craft Center front desk (US) · UNVERIFIED snippet: Harvard OFA glaze chemistry course fee "include[s] clay for test tiles, access to all raw glaze materials" (page returned 403) |
| **Municipal/recreation** | Clay by the pack with paper tallies and registers; prepaid firing balances | Whitehorse and Nillumbik (AU); Pickering (CA, via 03) |

### L. How studios log firings and maintenance today

| Practice | Example | Source |
|---|---|---|
| Manufacturer asks for a kiln log and periodic resistance checks | L&L: "Keep a kiln log of firings." | §A1 |
| Record firing duration as an element-wear signal | Lakeside Pottery: "Record each firing duration." | §A2 |
| Wiki page per kiln with nominal resistances and procedures | ENTS BookStack (2024-25) | §A2 |
| Approximate lifetime firing count kept | John Bryan wood kiln "logged an estimated 130 firings" (2026) | §I1 |
| Status posts to members when a kiln is down | Fitton Center Studio Manager post (2025) | §I1 |
| Controller-side counters | TAP only; Genesis logs exported and emailed to L&L for diagnosis; KilnLink graphs (03) | §B |
| Firing logistics (not maintenance) on paper slips, Google Forms/Sheets, Airtable | 03-tools-in-use §2 | 03 |
| CMMS, Sortly or asset software for kilns | **0 studios found** | §H |

## Hypothesis evidence

Ratings use only this file's evidence (inventory and assets scope). The counts come from targeted searches, not a random sample, so read them as "how much reachable evidence exists", not as prevalence.

| Hypothesis | Rating | What the evidence shows (counts) |
|---|---|---|
| **1. Inventory tracking** (clay, glaze materials, retail/PYOP stock) | **Weak as the anchor of phase 1; moderate for a narrow "materials ledger"** | *For:* 7 clay-sale mechanisms found (US 4, AU 2, CA 1); **3 of 7 rely on paper** (register, binder, tally sheet) and **2 of 7 split an online sale from a physical hand-over**, so somebody reconciles by hand. 3 of 8 supplier product pages fetched showed the item out of stock or sold out, and 1 more "Limited Quantity"; lead times run 3-10 business days. Bulk-to-retail spreads are large (silica $4.52/lb vs $1.00/lb; UK clay −25% from 1 bag to 250 kg), so clay and materials resale has a margin worth knowing. 2 regulations attach to the materials list (OSHA HazCom chemical list + SDS for US employers; UK COSHH assessment). 1 glaze-team practice records batch dates and SG (2018). 1 trade advice article (PMI 2024) calls manual tracking "error prone". *Against:* **0 first-hand complaints found (2023-2026)** about running out, theft or glaze-batch losses (Reddit and the CAD forum were unreachable, so this is weak negative evidence). **0 studios found using any inventory tool** (Sortly, Stocksmith/Craftybase, inFlow, Katana, Zoho), here or in 03. 01-market-survey already ranks this #9 with a "low" signal. **2 of 7 mechanisms avoid stock-keeping** by bundling clay into a fee or tickets, and class fees often include clay (John Bryan: "The class fee covers clay, tools, glazes and firing."). PYOP, the segment with real SKU inventory (300-400+ shapes), is already served by POS inventory (Square, Shopify, Lightspeed). |
| **2. Asset and equipment tracking** (kilns, ventilation, wheels) | **Moderate**, centred on kilns and extraction | *For:* the 1 kiln maker whose guidance was readable (L&L) sets a schedule **counted in firings** (every firing / every 10 / every 30 or annually) and asks for a kiln log; 1 controller vendor (TAP) builds component-life counters, but **only 1 of the 5 controllers or controller apps checked** does; 1 hard legal maintenance record (UK COSHH reg 9: LEV tested **every 14 months, records kept 5 years**); **8 independent failure events** (6 dated 2024-2026) costing $3,800-$50,000, with "nearly two months" of lost firing in one; 1 member-run studio keeps kiln maintenance in a wiki with "TODO" gaps; 1 L&L page conflicts with another on relay life (173 firings vs "many years"), so studios need their own thresholds; generic CMMS tools charge for meter-based maintenance (MaintainX Premium $65/user/mo). *Against:* **3 of 3 non-kiln equipment makers reached** (North Star, Shimpo, Peter Pugger) publish no maintenance interval; **only 1 of 8 failure events** is explicitly blamed on missed maintenance ("hasn't had a real tune-up in over 8 years"); the rest are sudden failures, fire or end of life, which a maintenance log would not have prevented; **0 studios found using CMMS or asset software**; US and UK tax rules let small studios expense equipment, so tax does not force an asset register (Canada's 20% CCA is the exception). |
| **3. Sales integrations with several payment providers** | **Weak (indirect only)** | 2 of 7 clay-sale mechanisms sell through a website and 1 of those also through a gift shop, so stock moves in whatever system takes the money; 0 of the 7 studios name their payment processor; the tools that tie sales to stock are single-provider (Fabman: Stripe; Square for Retail; Shopify POS). Nothing in this scope shows a need for *several* providers. |
| **4. QuickBooks Online integration** | **Weak** | 5 of the 5 dedicated inventory tools checked list a QuickBooks Online integration (inFlow included; Sortly only on Premium; Zoho; Katana; Stocksmith on Growth), so it is the vendor norm; 1 trade advice article (PMI 2024) recommends QuickBooks or FreshBooks; **0 studio sources in this file mention QuickBooks**; depreciation does not need a sync because small US/UK studios can expense equipment. |
| **5. Scheduling** | **Weak (from this scope)** | Maintenance falls due by firing count, so it needs slots on the kiln calendar (03 already proposes a "maintenance or out of order" booking type); downtime hits firing schedules (Fitton 2025 delay; Madison Potters' Guild "nearly two months", 2014; PMI 2024 advice). This scope says nothing about class or wheel booking. |

## Other strong signals

1. **The firing log is the hidden maintenance meter.** L&L's schedule is counted in firings, and studios already record every firing for billing (paper slips, Google Forms, Airtable per 03). A per-kiln firing counter fed by the existing firing record could trigger L&L's every-10 and every-30 checks without reading the controller. Generic CMMS tools charge for exactly this (meter-based triggers sit in MaintainX Premium and Limble Premium+).
2. **The clay-sale record protects the kiln.** Studios sell their own clay partly to control what gets fired: Berkshire charges members for kiln damage from low-fire clay ("can be upwards of $1,000") and needs approval for outside materials; Whitehorse requires a "completed label" on outside clay. Recording the clay body against the member, and from there against the firing, is a safety control as well as a sale.
3. **The equipment pain is capital, not scheduling.** 6 of 8 failure events ended in fundraising (GoFundMe targets $3,000-$50,000). L&L's ~10-year default kiln life, together with kiln age and replacement cost, would support a replacement-reserve view. In the one fire case, the building was insured by the landlord association and equipment cover was not mentioned.
4. **Regulatory documents hang off inventory and assets.** The US HazCom chemical list + SDS binder, the UK COSHH assessment (mandatory at 5+ employees) and the UK 14-month LEV test with 5-year records are cheap to support if materials and assets are already modelled. The UK LEV test is the only legally dated maintenance record found.
5. **Suppliers constrain reordering.** There are no supplier APIs (03). Stock-outs appeared on 3 of 8 product pages, prices "change daily" (Continental), lead times run 3-10 business days, and freight thresholds shape order sizes (US truck freight from 350 lb; UK pallet ≤950 kg; AU emailed freight quotes from ~20 kg). Reorder points therefore need lead time and pack size, not just a minimum.
6. **Vendor facts to correct elsewhere:** Craftybase is now **Stocksmith** ("Same team, same software — just a new name."), and paragonweb.com redirects to a Shopify store at paragonkilns.com.

## Gaps and limits

- **Practitioner voice is missing.** Reddit is blocked to both tools ("unable to fetch from www.reddit.com"; WebSearch: "not accessible to our user agent"); the Ceramic Arts Daily forum returns 403; Facebook needs a login. The pain counts above are therefore drawn from fundraisers, news and studio pages, and the "0 complaints" findings are weak negatives.
- **Search budget ran out twice** (the 200-call limit is shared across the session), so these planned searches were never run: the AU WHS register (mirror), NZ hazardous-substances inventory, insurers' equipment schedules, Skutt/Paragon/Olympic maintenance text, gas-kiln maintenance, manufacturer element life by cone, glaze-batch failures, clay theft, and production-pottery COGS from a practitioner.
- **Blocked or unreadable sources:** skutt.com (403), olympickilns.com (403), codes.iccsafe.org (403, so the IMC/NEC kiln text was not read; only L&L's checklist), ato.gov.au (403), safeworkaustralia.gov.au (timeout ×2), worksafe.qld.gov.au / safework.sa.gov.au / worksafe.act.gov.au (403), worksafe.govt.nz (header overflow), legislation.gov.au (timeout), crowdfunder.co.uk (403), Harvard OFA (403), and JavaScript-rendered pages (Paragon FAQs and manuals, the Wheelhouse NH Notion consignment page, Square for Retail prices, Clay-King tier tables). PDFs with compressed text could not be read: Imperial Centre 2026 rules, Town of Leland handbook, UVM co-op Fall 2026, CCSA Opening a Studio Guide (2026), L&L Genesis manual, Shimpo pug-mill PDFs and HSE CR0.
- **Not established:** element or thermocouple life in firings by cone from any manufacturer (L&L gives only relative life by temperature); what Skutt KMT/KilnLink, Paragon and Orton controllers record for maintenance; maintenance intervals for wheels, pug mills, extruders, spray booths and clay traps; prices and volumes of tools sold to members; cleaning and safety consumables; consignment mechanics (see 01); AU instant asset write-off; insurer equipment schedules.
- **Geography is thin outside the US and UK.** Canada has 1 fundraiser, 1 probable-CA wiki, CRA CCA, and Pickering via 03. Australia has 2 municipal studios, 1 supplier and 1 university. **New Zealand has none.**
- **Small denominators:** 7 clay-sale mechanisms, 8 failure events, 8 supplier product pages and 5 controllers. These are examples found by targeted search, not prevalence estimates.
- **Dates:** several pages are undated (Lakeside, Digitalfire, Rockport, OSU). L&L pages show a render date (2026-10-19), not a publish date. Older items are labelled (Workhouse 2018, Supermud 2016, Madison Potters' Guild 2014, Good Dirt 2017).
- **Segment tags from tourism listings:** the PYOP SKU counts are studio-supplied text on visitor-bureau pages.


