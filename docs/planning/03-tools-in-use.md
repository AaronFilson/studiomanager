# Tools in Use: What Pottery Studios Actually Run

_Researched 2026-10-19. Method: 84 studio websites in the US, Canada, the UK and Australia, picked from city searches ("pottery studio membership <city>", "community clay studio", "open studio ceramics membership", "paint your own pottery <city>") and from local press round-ups, never from vendor customer lists. Each site's home page and up to nine linked pages (membership, classes, firing, booking) were fetched, and the HTML was checked for booking, payment, form, waiver and door-access vendors in links, embeds and checkout URLs. Ambiguous sites were checked by hand. Limits: static HTML only, so a widget loaded purely by script, or anything behind a member login, is invisible. The web-search budget ran out partway through, so the sample leans toward sites reachable from earlier results. Reddit's HTML and JSON routes were blocked, but its RSS feeds worked. The Ceramic Arts Daily forum exposed only opening posts, through RSS. Capterra, G2, Trustpilot and Facebook were blocked. Details are in [section 5](#5-voice-of-the-studio-owner). Sources older than 2024 are flagged **(pre-2024)**._

## TL;DR

- **Studios run their storefront, not studio software.** The most common setup (33 of 84) is a website builder's own commerce: Shopify, WooCommerce, Wix or Squarespace, selling classes and memberships as products. Next come Acuity / Squarespace Scheduling (8) and class or event registration tools such as Bookwhen, Sawyer and Corsizio. **Momence, Pike13, Punchpass, Arketa, Glofox, WellnessLiving and Square Appointments appeared zero times. Mindbody appeared twice.** [01](01-market-survey.md) overstated general booking tools.
- **Pottery-specific software is barely adopted.** Kiln Fire was the only one found (3 of 84). WheelOwl, Kilnfox, MudBuddy, Classly, Zenamu and ClayTrack appeared nowhere. The niche is crowded with vendors but not with customers.
- **The piece and firing loop is still paper, forms and spreadsheets.** Common tools are firing slips, carbon-copy tickets, a signed "Kiln Firing Log", Google Forms for firing requests, a Google Sheet to sign out studio time, Airtable kiln calendars, and an email when work is ready, if anyone sends one. BKLYN CLAY, a Queensland studio and a Portland makerspace have **built their own** piece and firing apps.
- **How fees are measured depends on the country.** US studios charge per cubic inch (8 in the census). UK and Australian studios charge per kilogram or per litre. Others use per pound, firing included in the clay price, a monthly shelf allowance, prepaid firing cards and tickets, member discounts and oversize surcharges. Payment is taken at drop-off, at pickup, at the end of a session, or on a monthly invoice.
- **Owners on Reddit complain about tools that don't connect, about admin time, and about fairness for heavy users more than about price.** Their words: no Zapier or email follow-up, can't use their existing POS, "I'm an artist, I don't love doing the admin stuff", "ready" emails that fire before all of a member's pieces are out, and production potters crowding the kilns. Members describe kiln approval before booking, per-shelf and whole-kiln pricing, and "a few pieces from each member, then oldest to newest" loading.
- **Kiln controllers, Glazy and suppliers offer no official API.** Logs leave the controller as files (Bartlett Genesis LAN CSV, Rohde CSV, Nabertherm USB CSV) or by email (TAP). Glazy's data is CC BY-NC-SA and its 2025 policy forbids scraping. Makerspace tools, several of them open source, have already solved certification gating, consumable billing, allowances and unclaimed-object workflows.

## 1. Adoption census

### Totals by primary platform (one per studio, n = 84)

"Primary" means the tool a member or student uses to book or pay for classes, memberships or studio time.

| Category | Studios | Breakdown |
|---|---|---|
| **Website-builder commerce** | **33** | Shopify 10 · WooCommerce 10 · Wix (Bookings, Events, Pricing Plans) 7 · Squarespace Commerce 5 · Ecwid 1 |
| **Appointment schedulers** | **11** | Acuity / Squarespace Scheduling 8 · Setmore 1 · Calendly 1 · SimplyBook.me 1 |
| **Class / event registration and marketplaces** | **11** | Sawyer 2 · Bookwhen 1 · Corsizio 1 · Jackrabbit 1 · Cademy 1 · Eventbrite 1 · Tock 1 · GetGalore 1 · VBO Tickets 1 · Stova 1 |
| **Municipal, recreation and campus systems** | **9** | ActiveNet 4 · Amilia 2 · RecTrac 1 · Nelnet campus store 1 · college events system 1 |
| **No online booking** (email, phone, contact form, walk-in) | **7** | |
| **Pottery-specific SaaS** | **3** | Kiln Fire 3 |
| **Fitness / wellness studio software** | **3** | Mindbody 2 · bsport 1 |
| **Forms only** | **3** | Jotform 2 · Google Forms 1 |
| **Custom-built** | **1** | Bubble.io no-code app |
| **Unknown** | **3** | site down, redirect, unidentified cart |

By country: US 42, Canada 12, UK 12, Australia 18. By type: about 45 membership or community studios (many also teach), 10 paint-your-own-pottery (PYOP) or PYOP hybrids, 5 university or college programs, 14 art centres, museums or nonprofits, and 4 municipal or community-association studios. The type groups overlap.

### Every tool seen, in any role (n = 84)

| Tool | Studios | Notes |
|---|---|---|
| Shopify (classes and memberships as products) | 10 | Plus 2 that use Shopify only for a shop. Booking apps seen: BookThatApp, Appstle memberships |
| WooCommerce | 10 | Memberships, "Firing Card" products, members-only shops |
| Acuity / Squarespace Scheduling | 8 | Mostly Australian teaching studios and day passes |
| Google Forms | 8 | Membership applications, waitlists, firing requests |
| Square (Online store, checkout or gift cards) | 7 | Mostly gift cards. **Square Appointments: 0** |
| Wix Bookings / Events / Pricing Plans | 7 | |
| Squarespace Commerce | 6 | |
| ClassBento (marketplace) | 4 | UK and Australia, alongside the studio's own tool |
| ActiveNet | 4 | Art centres and city recreation |
| Kiln Fire | 3 | Gasworks NYC; Ceramigos (Austin); John Burrison Pottery Studio (GA) |
| Jotform | 3 | Applications and enrolment |
| Mindbody · Bookwhen · Sawyer · Amilia | 2 each | Turning Earth uses Bookwhen for classes and Mindbody for memberships |
| Airtable · Google Sheets · monday.com forms · Calendly · Setmore · SimplyBook.me · Corsizio · Jackrabbit · Cademy · bsport · SevenRooms · Tock · GetGalore · Ecwid · Eventbrite (for ceramics) | 1 each | |
| Custom-built | 2 | BKLYN CLAY's firing app (WordPress + Stripe); Twelve80 (Bubble.io) |
| **Zero in the sample** | 0 | Momence, Pike13, Punchpass, Arketa, Glofox, WellnessLiving, Square Appointments, ActivityHero, WheelOwl, Kilnfox, Classly, MudBuddy, Zenamu, ClayTrack, Smartwaiver, WaiverForever, Kisi |

**Email marketing:** Mailchimp 12, Constant Contact 3, Klaviyo 3, Omnisend 2, Flodesk 1.

**Nonprofit CRM and donation tools:** Bloomerang, Neon CRM, Givebutter, DonorPerfect/Qgiv, Spektrix and Veevart.

**Waivers:** 7 sites mention a waiver. All are the studio's own form or a clause in the platform's terms. No e-waiver vendor was detected.

**Door access:** 29 sites advertise 24/7 access, keys or "building access codes". No door-access vendor shows on any public page. That is expected, because Kisi and similar tools live in the members' app. Kisi does list Kiln Fire and a pottery chain, 24hr Pottery, as integrations ([section 4](#4-adjacent-domains-worth-learning-from)).

### What the sites say about fees, pieces and shelves

24 of 84 sites publish a firing fee. Their units:

| Model | Studios (census) | Examples |
|---|---|---|
| Per cubic inch | 8 (all US) | BKLYN CLAY 4¢ members / 8¢ non-members; Gasworks 3.5¢; Centerpoint 5¢; Radius 6¢; St. Johns 10¢; Still Life 2¢, or 4¢ for pieces over 12"; Morning 2¢ (1¢ for a re-fire), "added to your invoice at the end of each month" |
| Per kilogram | 4 (UK and Australia) | The Kiln Rooms £5/kg stoneware, £10/kg porcelain; Slow Clay $20/kg; Bisque $9/kg per firing; Neo $19/kg |
| Per litre | 1 (UK) | Turning Earth £2.25 per 500 ml; the membership includes 30 litres of firing |
| Per pound | 2 (US) | Claytivity $1/lb, or $2/lb for 12–18"; Fine Things $1/lb over the allowance |
| Allowance included | 2 | Fine Things "1 full shelf (1027) per month"; Turning Earth 30 L |
| Bundled into clay or membership | 2 | Mud Studio; Recess ("clay per lb @ $2.50, includes firing") |
| Prepaid cards or tickets | 2 | Choplet "Firing Card" (WooCommerce product); City of Pickering issues 20 bisque + 20 glaze tickets per bag of clay |
| Member discount | 1 | Lygon Street: members 15% off; glazes are added to the firing charge |

16 sites publish pickup or disposal rules. Hold periods range from **1 week** (The Kiln Rooms communal shelves) through **28 days** (Bonbon), **30 days** (Atelier Forma; Neo, after which work is "donated to an opportunity shop"), and **monthly purges** (Gasworks: "all work fired in January will be discarded") to **2 months** (BKLYN CLAY). Notification varies:
- Clay With Me says outright: "We do not send reminder emails or pickup notifications."
- Bonbon, Atelier Forma and Trace send an email.

Certification shows up as well:
- Radius offers "the opportunity to learn to fire kilns and fire your own work (become kiln certified…)".
- Clay Shed gives free inductions in kiln loading and firing.
- Pottery Northwest requires an orientation before it assigns a shelf and building codes.

### Per-studio table

Type key: **M** membership or community studio · **T** teaching studio · **P** paint-your-own (PYOP) · **U** university or college · **A** art centre, museum or nonprofit · **C** municipal or community association.

| # | Studio | City, country | Type | Platform(s) | Notes | Evidence |
|---|---|---|---|---|---|---|
| 1 | BKLYN CLAY | Brooklyn & Manhattan, US | M T | Sawyer (classes); **custom firing app** (fire.bklynclay.com, WordPress + Stripe); Squarespace site | Members submit work from a phone with photos; 4¢/8¢ per in³; work held 2 months | https://bklynclay.com/classes-prospect-heights · https://bklynclay.com/firing |
| 2 | Powerhouse Arts Community Ceramics Studio | Brooklyn, US | A M | Wix member portal and application; Spektrix; Google Form for firing | $0.04/in³; shelf tiers | https://powerhousearts.org/community-ceramics-studio |
| 3 | Gasworks NYC | Brooklyn, US | M T | **Kiln Fire**; Shopify site; Mailchimp | 3.5¢/in³; monthly discard of unclaimed work | https://gasworksnyc.com/ |
| 4 | Choplet | Brooklyn, US | M T | WooCommerce (memberships, "Firing Card"); Flodesk | 24-hour access add-on after probation | https://choplet.com/ |
| 5 | Centerpoint Ceramics | Brooklyn, US | M | Squarespace Commerce; **Google Sheet to sign out studio time**; keys | $0.05/in³; L&L kiln | https://www.centerpointceramics.com/memberships |
| 6 | Still Life Ceramics | Los Angeles, US | M T | Shopify + BookThatApp; Jotform applications | 2¢/in³ glaze, bisque free | https://still-life-studio.com/ |
| 7 | Claytivity | Los Angeles, US | M T | Wix Bookings + Pricing Plans | Glaze firing $1/lb | https://www.claytivitypottery.com/memberships |
| 8 | Pottery Northwest | Seattle, US | A M T | Shopify (classes); monday.com form (membership); Constant Contact | Orientation, then building access codes | https://www.potterynorthwest.org/membership |
| 9 | Northern Clay Center | Minneapolis, US | A T | Shopify (classes and membership; Appstle); Google Forms | | https://www.nccshop.org/pages/classes-for-adults-kids |
| 10 | MudFire | Decatur GA, US | M | Square Online + Squarespace Scheduling | | https://www.mudfire.com/ |
| 11 | Callanwolde Fine Arts Center | Atlanta, US | A T | ActiveNet; Eventbrite (concerts) | | https://callanwolde.org/classes/ |
| 12 | Emerald City Fired Arts | Seattle, US | P | None found (walk-in) | | https://www.emeraldcityfiredarts.com/ |
| 13 | The Digs | Chicago, US | A M | Acuity (day passes); Google Forms (membership); Squarespace Commerce | 24/7 access | https://www.thedigschicago.com/membership |
| 14 | ClaySpace | Lisle IL, US | M | WooCommerce (private store); WordPress login | Co-op | https://www.clayspace.net/ |
| 15 | Morning Ceramics | Portland OR, US | M T | Wix (Events) | 2¢/in³ billed monthly; staff load the kilns | https://www.morningceramics.com/membership |
| 16 | Fancy Banana | Portland OR, US | M | None (enquiry only; Squarespace) | Members fire their own work | https://fancybanana.studio/membership |
| 17 | Fine Things | Portland OR, US | M | Shopify (application page) | 1 shelf of firing per month included | https://finethingspdx.com/pages/membership-studio |
| 18 | Radius Community Art Studio | Portland OR, US | A M | WooCommerce; Google Forms (waitlists) | $0.06/in³; kiln certification | https://www.radiusstudio.org/membership/ |
| 19 | St. Johns Clay Collective | Portland OR, US | M | Wix Pricing Plans ("Clay Pass"); Square Online | $.10/in³ | https://www.stjohnsclay.com/ |
| 20 | Greentree Art Studio | Portland OR, US | T | Squarespace Commerce | | https://greentreeartstudio.com/classes |
| 21 | Parker Art School | Parker CO, US | T M | Corsizio; own waiver form | Membership includes 12 firings | https://parkerartschool.com/ |
| 22 | Feats of Clay | Austin, US | M T | Acuity; Square Online; Square gift cards | Members sign a paper "Kiln Firing Log" | https://featsofclaypottery.com/studio-space-firing-services |
| 23 | Mud Studio | Austin & San Antonio, US | M T | Google Forms (membership) on Squarespace | Firing bundled into clay | https://mudstudioceramics.com/austin-memberships |
| 24 | Brave Ceramics at Ceramigos | Austin, US | T | **Kiln Fire** (ceramigos.kilnfire.com) | | https://www.braveceramics.com/ |
| 25 | Austin Clay Arts | Austin, US | M T | Squarespace Commerce; Google Forms waitlist | | https://www.austinclayarts.com/shop/p/membership |
| 26 | ATX Pottery | Austin, US | T | Phone ("call to book your spot") | | https://www.atxpottery.com/ |
| 27 | The Contemporary Austin Art School | Austin, US | A T | ActiveNet; Veevart (museum membership) | | https://thecontemporaryaustin.org/artschool/ |
| 28 | Dougherty Arts School | Austin, US | C | RecTrac/WebTrac (City of Austin parks account) | | https://www.austintexas.gov/parks/account-creation-online-registration-and-faq |
| 29 | The Workshop Mpls | Minneapolis, US | M T | **Unknown** (domain shows "Site not found") | | https://www.theworkshopmpls.com/ |
| 30 | The Bascom | Highlands NC, US | A | **Unknown** site cart; Bloomerang CRM | | https://www.thebascom.org/ |
| 31 | Color Me Mine Edina | Edina MN, US | P | Franchise site; CardFoundry gift cards; no online booking found | | https://edina.colormemine.com/ |
| 32 | UWM Studio Arts & Craft Centre | Milwaukee, US | U | VBO Tickets | Opens only if 35 memberships sell | https://uwm.edu/union/craft-centre/ |
| 33 | UVM Ceramics Co-op | Burlington VT, US | U | Stova (eventscloud) | | https://www.uvm.edu/cas/soa/soa-ceramics-co-op |
| 34 | Miami University Art Center | Oxford OH, US | U | Nelnet campus store | | https://miamioh.edu/cca/art-center/membership.html |
| 35 | NWTC Artisan & Business Center | Green Bay WI, US | U | College events system; **Airtable** (kiln schedule, policies) | | https://www.nwtc.edu/about-nwtc/nwtc-locations/artisan-and-business-center/open-studio |
| 36 | Plains Art Museum | Fargo ND, US | A | Sawyer (classes); Givebutter (membership); Square Online | | https://plainsart.org/ |
| 37 | John Burrison Pottery Studio (Folk Pottery Museum) | Sautee GA, US | A | **Kiln Fire**; Neon CRM | | https://www.folkpotterymuseum.com/classes |
| 38 | Sonoma Community Center | Sonoma CA, US | A M | Jotform (PCI) for membership and classes | Unglazed bisque discarded after a month | https://sonomacommunitycenter.org/ceramics/ceramics-open-studio-program/ |
| 39 | Art Alliance | Greensboro NC, US | A T | Jotform (PCI) | | https://www.artalliancegso.org/project/adult-studio-membership/ |
| 40 | Hayden Center Pottery Co-Op | Hayden CO, US | C | Amilia | Extra clay "invoiced on your Amilia account monthly" | https://www.haydencolorado.com/276/Pottery-Co-Op |
| 41 | Wayne Art Center | Wayne PA, US | A T | ActiveNet | | https://wayneart.org/ |
| 42 | Recess Urban Recreation | US (city not stated) | M T | GetGalore; waiver in the terms | Firing bundled into clay | https://recessurbanrecreation.getgalore.com/memberships/8895-re-occuring-monthly-membership |
| 43 | Create Art Studio | Toronto, CA | T M | Jackrabbit; WooCommerce; Square gift cards | Hourly studio fee | https://createartstudio.ca/ |
| 44 | Zei Pottery | Toronto, CA | M T | WooCommerce (classes); membership by email application | 24/7 | https://zeipottery.com/membership/ |
| 45 | Clay With Me | Toronto, CA | P T M | Shopify; Google Forms | "We do not send reminder emails or pickup notifications" | https://claywithme.ca/ |
| 46 | Parkdale Pottery | Toronto, CA | T M | Shopify | | https://parkdalepottery.ca/ |
| 47 | Pottery Lab | Toronto, CA | P T M | Wix Bookings | | https://www.potterylab.ca/service-page/studio-membership |
| 48 | City of Pickering Pottery Studio | Pickering ON, CA | C | ActiveNet; **paper firing tickets** | | https://www.pickering.ca/potterystudio |
| 49 | Old Ottawa South Community Association | Ottawa, CA | C | Amilia | | https://oldottawasouth.ca/programs/ |
| 50 | The Mud Oven | Ottawa, CA | P | Tock (search result only; page returned 403, **unverified**) | | https://www.exploretock.com/the-mud-oven-ottawa/ |
| 51 | UBC Pottery Club | Vancouver, CA | U | **Unknown** (club page redirects to a vendor home page) | | https://amscampusbase.ubc.ca/ubcpottery/ |
| 52 | Tendre Atelier | Montréal, CA | T M | Shopify (classes as products) | | https://www.tendreatelier.com/en/pages/pottery-studio-membership |
| 53 | Atelier Forma | Montréal, CA | T M | Shopify (memberships and classes as products) | Email when ready; 30 days to collect; kiln damage fee $60–120 | https://atelierforma.ca/pages/firing-services |
| 54 | Centred Clay Studios | Calgary, CA | M | Contact form only | 24/7 | https://centredclaystudios.com/ |
| 55 | Turning Earth | London (4 sites), UK | M T | **Mindbody** (memberships, open-studio pass); Bookwhen (classes) | £2.25 per 500 ml; 30 L included | https://www.turningearth.org/membership |
| 56 | The Kiln Rooms | London, UK | M T | Bookwhen; Google Forms (membership requests) | £5/kg; communal shelves 1 week | https://www.thekilnrooms.com/drop-in |
| 57 | Studio Pottery London | London, UK | M T | **Mindbody**; WooCommerce members' shop | | https://www.studio-pottery-london.com/ |
| 58 | Trace Ceramics | London, UK | M T | Squarespace Commerce (membership products) | Email when ready | https://www.traceceramics.co.uk/monthly-membership |
| 59 | Art Craft Studios | London, UK | T M | Cademy; ClassBento | | https://www.artcraftstudios.co.uk/ |
| 60 | Bonbon Pottery | London, UK | P T | SimplyBook.me; Square gift cards | Email when ready; 28-day disposal | https://www.bonbonpottery.co.uk/faq |
| 61 | Pitter Potter / Pottery Café | London, UK | P | WooCommerce "book a table"; Square gift cards | | https://pottery-cafe.com/ |
| 62 | Social Pottery | London, UK | P T M | bsport (memberships); ClassBento; SevenRooms | | https://socialpottery.com/memberships/ |
| 63 | Clay Time | London, UK | P T | Wix Bookings | | https://www.claytime.london/booking |
| 64 | Creative Bloc | Glasgow, UK | M | Eventbrite (open-access sessions) | | https://www.eventbrite.co.uk/e/open-access-ceramic-studio-tickets-1120144294739 |
| 65 | Clay Shed | Bristol, UK | M T | Squarespace Commerce; members book wheels and kilns "via our app" (unnamed) | Inductions in kiln loading | https://www.clayshed.co.uk/ceramics-membership-bristol |
| 66 | Clay Studio (Community Clay Studio Manchester CIC) | Manchester, UK | M T | Setmore | | https://www.claystudio.co.uk/membership |
| 67 | Slow Clay Centre | Melbourne, AU | T | WooCommerce | $20/kg on collection | https://www.slowclay.com/ |
| 68 | Twelve80 | Melbourne, AU | M | **Custom app on Bubble.io** | 24/7 | https://twelve80.com.au/ |
| 69 | Sunroom Studio | Melbourne, AU | T M | Acuity; ClassBento | | https://www.sunroomstudio.store/ |
| 70 | Urth Clay Studio | Melbourne & Adelaide, AU | T M | WooCommerce; Stripe | | https://urthclaystudio.com.au/pottery-studio-club-membership-adelaide/ |
| 71 | Pot Dispensary | Melbourne, AU | T M | Acuity | | https://www.potdispensary.com.au/ |
| 72 | Bisque Studios | Melbourne, AU | T M | WooCommerce | $9/kg per firing | https://bisquestudios.com/membership/ |
| 73 | Terrapotter Studios | Melbourne, AU | T M | Acuity | Public firing by drop-off form | https://www.terrapotterstudios.com/publicfiring |
| 74 | Urban Clay | Melbourne, AU | T | WooCommerce | | https://www.urbanclay.com.au/ |
| 75 | Neo Ceramics | Melbourne, AU | T M | Acuity | $19/kg; unclaimed work donated after a month | https://neoceramics.com.au/studio-access |
| 76 | Lygon Street Pottery | Melbourne, AU | T M | Acuity; ClassBento | Members 15% off firing | https://lygonstreetpottery.com.au/memberships |
| 77 | Robert Gordon Australia | Melbourne, AU | P | Shopify (booking as a product) | | https://www.robertgordonaustralia.com/products/paint-your-own-pottery-booking |
| 78 | Everyday Clay | Healesville VIC, AU | T | Calendly (glaze and firing drop-off), then invoice | | https://www.everydayclay.com.au/glazing-firing-booking |
| 79 | Maker's Loft | Sydney, AU | M T | Shopify (memberships and firing payment) | | https://www.makersloft.com.au/pages/pottery-ceramics-studio |
| 80 | Studio P | Perth, AU | T M | None found (Squarespace) | | https://www.studiop.net.au/memberships |
| 81 | ClayMake Studio | Perth, AU | M T | None found (Weebly) | Guest firings carry a $1 handling fee | https://www.claymakestudio.com.au/firing-tcs.html |
| 82 | Not Yet Perfect | Perth, AU | T | Wix Events | | https://www.notyetperfect.com/workshops |
| 83 | Walyalup Fremantle Arts Centre | Fremantle, AU | A | WooCommerce | | https://wfac.org.au/art-classes/ |
| 84 | Earth & KLĀ | Perth, AU | T | Ecwid ("10 Studio Pass") | | https://earthandkla.com.au/ |

**Search results that couldn't be tied to a studio** (not counted):
- Two Occasion booking pages titled "Clay Studio Membership" and "The Clay Room Membership" (app.getoccasion.com returned 403).
- Several ma.to "venue" pages. Mato is an events-discovery site, not a booking tool.

## 2. Back-office and DIY tools

The booking tool stops at the front desk. Behind it, studios use the following (a separate sweep of 35 handbooks, policy pages and posts; accessed 2026-10-19):

| Practice | Examples | Source |
|---|---|---|
| **Paper firing forms and slips** | The Potters' Studio, Berkeley: paper "Firing Submission Forms" at a measuring table, "filed alphabetically by your FIRST NAME… billed on the next month's invoice"; coloured monthly tags; untagged work goes to the "Rack of Doom" | https://www.berkeleypottersstudio.com/s/2023FINAL_TPS_MemberHandbook.pdf **(pre-2024)** · https://www.thepottersstudio.org/members-zone (2026) |
| | VisArts, Richmond VA: "Weigh your work… fill out a firing slip. Pay… with our front desk" ($2/lb) | https://www.visarts.org/studio-access/equipment/community-pottery-firing/ |
| | Kilnfolk, Yakima WA: "No-name pieces without an appropriate firing slip or signature will not be fired"; members invoiced with a 5% late fee | https://www.kilnfolkclay.com/studio-use-policies |
| | Silvermine Arts Center, CT: "$.07 cents per cubic inch. Firing Fees MUST be paid at the end of the session" (Spring 2026) | https://www.silvermineart.org/wp-content/uploads/2026/02/OpenStudio_Ceramics_SP2026.pdf |
| | Feats of Clay, Austin: "Once work is measured and charged accordingly, you will sign our Kiln Firing Log" | https://featsofclaypottery.com/studio-space-firing-services |
| **Prepaid tickets and allowances** | City of Pickering: 20 bisque + 20 glaze tickets per bag of clay, with the number of tickets per piece set by a posted size chart (effective June 2025) | https://www.pickering.ca/potterystudio |
| **Weigh-point service** | Canberra Potters: "paid for at the time of weigh and drop off", "10kg limit per person per week", prices up from January 2026 | https://canberrapotters.com.au/firing-service/ |
| | Pottery Supplies, Brisbane: "FIRING FORM 2026", then "We… text you… You'll pay for your firing when you pick up" ($14/kg) | https://www.potterysuppliesonline.com.au/firing-service/ |
| **Google Forms / Sheets / Calendar** | Powerhouse Arts firing inquiry form (kiln, cubic inches, clay, photos). The form says $0.08/in³; the web page says $0.04 | https://docs.google.com/forms/d/e/1FAIpQLScME4Ocupui2u8PG3enSI1x6kWFVUpwbnYN82Wp26foN1K0CA/viewform |
| | Rat City Studios, Seattle: Stripe payment link, then a Google Form, then a Google Calendar of drop-off and pickup days; size-tier pricing $0.75–6.00 | https://ratcitystudios.com/firing-service |
| | Place des Arts, Coquitlam BC (2026/27 handbook): "The Open Studio Sign-Up Sheet is a Google Docs spreadsheet"; maker's marks on a clipboard list; shelf stamps | https://placedesarts.ca/wp-content/uploads/2026/03/2026_2027-Student-Ceramics-Handbook.pdf |
| | Centerpoint, Brooklyn: "all members have keys and access to a Google sheet to sign out time" | https://www.centerpointceramics.com/memberships |
| **Microsoft Forms** | Gasworks Arts Park, Melbourne: kiln firing service form | https://gasworks.org.au/visit-us/kiln-firing-service/ |
| **Airtable** | NWTC (WI): open-studio and kiln calendars are embedded Airtable views. Its 2024 newsletter warns "WE CAN'T GUARANTEE YOUR WORK IS IN THE KILN THAT IS BEING FIRED" | https://www.nwtc.edu/about-nwtc/nwtc-locations/artisan-and-business-center/open-studio/open-studio-schedule · https://www.nwtc.edu/getmedia/915e6968-3078-447d-9c92-6fbaff165630/Ceramic-Studio-Newsletter-Block-7-2024.pdf |
| | Amphora, San Rafael: Airtable "Firing Agreement Form", then each piece goes into Kiln Fire with photos | https://www.amphoraceramicstudio.org/firing-services |
| **Notion as the website or handbook** | Miles of Clay: "Firing is included… within reason. I don't want to create a strict policy" **(pre-2024)** | https://moderndesert.notion.site/Memberships-efe0aae4e93e4747bc03f75c23131c05 |
| | The Wheelhouse NH (already cited in [01](01-market-survey.md)) | https://thewheelhousenh.notion.site/Member-Consignment-Sales-1d11653845c3805eb89fd0d3bac37393 |
| **Honour system, Venmo, Slack** | TinkerMill makerspace: "We operate on the honor system"; pay by kiosk, website or Venmo, then email a photo of the pieces and maker's mark; marks are registered with the studio (wiki edited 2025-11-06) | https://wiki.tinkermill.org/index.php/Pottery/Ceramics |
| | MakeIt Labs, NH: "Once the kiln is fired and unloaded, a notice is sent to the Ceramics Channel on Slack" **(pre-2024)** | https://sites.google.com/a/makeitlabs.com/wiki/resource-areas/ceramics-studio |
| **Building their own** | Past Lives Makerspace, Portland (GitHub issue, 2026-10-08): "Currently, makers write a paper slip and set it next to the piece; slips get lost, nobody can see what is waiting, and nobody is told when a piece comes out." Built the same day in their own member app (PRs #695, #697, #700; issue closed as completed, verified 2026-10-19) | https://github.com/Past-Lives-Makerspace/plfog/issues/691 |
| | Clay Temple, QLD: a staff-training module on lovable.app; each student gets a code such as "H45" "required for tracking, glaze classes, collection", plus a "Track Your Pot" app | https://introductorycourse.lovable.app/module/before-they-leave |
| | BKLYN CLAY's fire.bklynclay.com and Twelve80 on Bubble (census #1 and #68) | see census |
| **PYOP back office** | Social Pottery (London) runs Square POS, payment links and appointment reminders (Square case study, 2023) **(pre-2024)** | https://squareup.com/gb/en/townsquare/seller-spotlight-social-pottery |
| | Color Me Mine franchisor is rolling out "a new point-of-sale system across Color Me Mine studios" (vendor not named; 2026-10-13) | https://www.entrepreneur.com/buying-a-franchise/heres-how-these-two-immigrants-started-their-own-color-me-mine-franchise |
| | Robert Gordon AU: "leave the bottom of your piece clear so our staff can add your order number and date" | https://www.robertgordonaustralia.com/pages/paint-your-own-pottery |
| **Accounting and email** | QuickBooks is the only accounting tool named by a potter (Pottery West, Mailchimp case study) **(pre-2024)**. Pottery Making Illustrated (Nov/Dec 2024) recommends ClassBug, CourseStorm, Square, Pike13, QuickBooks, FreshBooks, Craftybase, Sortly and barcode/RFID tags. That is advice; no studio was found using Sortly or Craftybase | https://mailchimp.com/case-studies/pottery-west/ · https://ceramicartsnetwork.org/pottery-making-illustrated/pottery-making-illustrated-article/in-the-studio-streamlining-for-success |

**Patterns**

- **Paper is the default per-piece record.** About 10 of 35 examples use a slip, ticket, sheet or log.
- **Online forms handle intake** (first-time firing customers, applications, waitlists), not per-piece tracking.
- **Piece identification is manual everywhere:** maker's marks, registered marks, shelf stamps, coloured tags, staff-written order numbers.
- **Not found in any studio source:** label printers, QR systems (only in vendor pitches), Trello, Discord, Xero, Wave.
- **Payment happens at four different points:** drop-off (VisArts, Canberra, Rat City, Amphora), pickup (Brisbane), end of session (Silvermine), or a monthly invoice (Berkeley, Morning, Kilnfolk).

## 3. Kiln, glaze and materials tooling

### Kiln controllers

| Product | What it logs | Price | API / export | Source |
|---|---|---|---|---|
| **Skutt KilnLink** (touchscreen KMT and Mini controllers) | Free app: live status. Premium: graph of every firing, cost, amperage per section, voltage, notes, alerts, several users and kilns | **$100/yr per kiln, $325 for 5 yrs** (2026 price list) | No API or export documented. A hobbyist reads the LCD with a camera to get data | https://skutt.com/kilnlink-page/kilnlink-faq/ · https://skutt.com/images/Ceramic-Kiln-Price-List-2026-1.pdf · https://github.com/rogerlew/skutt-kiln-monitor |
| **Bartlett Genesis + KilnAid** (standard on L&L; option on Olympic; Evenheat Icon; Paragon controllers are made by Bartlett) | Firing graph, diagnostics, firing log (cost needs a current sensor); log download to phone since v4.0.1 (2025-08) | KilnAid+ $1.99/mo or $19.99/yr | **No official API.** The controller's "Export Log File" serves up to 10 firing CSVs (30-second rows) over the LAN. Two 2026 Home Assistant integrations use undocumented cloud endpoints | https://bartlettinstrument.com/kilnaid · https://github.com/carterworks/bartlett-home-assistant · https://github.com/zander8807/home-assistant-kilnaid |
| **TAP II / Pro / Micro** (SDS Industries; ships on Evenheat, retrofits others) | Monitoring, logs, cost calculator | Premium $4.99/mo or $49.99/yr | Logs by email only (Premium); "No desktop or web-based application" | https://help.kilncontrol.com/knowledge/premium-subscription-vs-free-subscription |
| Orton AutoFire | Standalone; optional Windows monitoring software | — | No Wi-Fi app found | https://www.ortonceramic.com/autofire-controllers |
| Paragon Sentinel / Sentry | Graph mode; Wi-Fi only for firmware | — | No Paragon app found | https://paragonkilns.com |
| **Rohde myKiln** (DE) | Firing book with photos and notes, energy per firing | Free | **CSV/PDF/JPG export per firing since v2.0.0 (2025-05)** | https://apps.apple.com/us/app/rohde-mykiln/id1621886007 |
| **Nabertherm MyNabertherm + NTLog** | App: status and alerts. NTLog: CSV to USB | Free app | USB CSV | https://nabertherm.com/en/mynabertherm-app |
| **Stafford Kiln Portal** (UK; Kilncare, Cromartie, probably Tetlow AU) | Status, programs, firing log | Controller £581 | No API; USB CSV | https://kilnportal.co.uk · https://potclays.co.uk/st215-usb-data-log |
| **jbruce12000/kiln-controller** (Raspberry Pi, open source) | Live cost, CSV log | Free | **REST API, outbound webhooks, MQTT** | https://github.com/jbruce12000/kiln-controller |

**Correction to [01](01-market-survey.md):** its KilnAid source (help.kilncontrol.com) belongs to **TAP / SDS Industries, not Bartlett**. Bartlett's site is bartlettinstrument.com. KilnLink is $100 per kiln per year. KilnAid+ is $19.99/yr.

**Shared studios vs individual potters**
- Kiln cloud subscriptions are sold to institutions: Skutt pitches KilnLink to school districts, and its high-school equipment list budgets "1 subscription/kiln" ([Skutt Kids Need Clay](https://skutt.com/kidsneedclay/keys-to-success/)).
- No community-studio handbook was found that names KilnLink or KilnAid. Members never touch the controller. Staff load the kilns and members read a posted schedule.
- Individual potters now have many manual journaling apps from 2025–26 (KilnNote, Kilno, Blazed, ClayLab, Glaizit, Flux), mostly on-device with no API.

### Glaze software

| Product | Notes | Data access | Source |
|---|---|---|---|
| **Glazy** | New site 2025–26. Inventory with material prices gives batch cost; kiln schedules are linked to recipes. **No group or studio sharing** | **CC BY-NC-SA 4.0.** YAML export (Mar 2025); full dump `glazy_20260531.yaml.gz`. **AI & Data Policy (2025-11-30): no bulk scraping; "Commercial entities wishing to use Glazy data must obtain a separate commercial license"; it prefers to provide a dataset export on request. It treats ingredient lists as uncopyrightable factual data.** No documented public API | https://help.glazy.org/about/data-use-policy · https://github.com/derekphilipau/glazy-data |
| **Insight-Live** (Digitalfire) | Cloud lab notebook. **Group plan $178.85/yr for up to 20 users**, the only group-capable glaze tool found | CSV import; API "in progress" since 2022, not public | https://insight-live.com · https://digitalfire.com/glossary/digitalfire+api |
| HyperGlaze | Desktop; free download | Local files | https://hyperglaze.com |
| GlazeMaster, Matrix | Look defunct: glazemaster.com is a for-sale page, and the Matrix domain is an empty placeholder (**unverified**) | — | glazemaster.com · matrix2000.co.nz |

Group studios keep shared glazes on paper and in spreadsheets: recipe books, batch checklists, a volunteer glaze team. Source: PMI 2018 **(pre-2024)**, plus the [01](01-market-survey.md) glaze-management source.

### Materials and suppliers

- **No supplier has a customer API, order-history export or webhooks.** Laguna, Seattle Pottery Supply, Sheffield, Krueger and Highwater run on Shopify. Clay-King runs on WooCommerce (with PO and Net30). Tuckers runs on Lightspeed. Bath Potters and Potclays are custom or trade-by-phone.
- **Laguna has absorbed Axner** (axner.com now shows "Laguna Clay Company – FL").
- Shopify stores expose a public catalogue at `/products.json` (title, SKU, price; check each store's terms).
- Order history and invoices arrive as email and PDF, which supports the email-forward OCR path in [02](02-wave1-plan.md). Sources: https://lagunaclay.com · https://axner.com · https://clay-king.com/purchase-orders/
- **Firing cost** everywhere uses the same formula: kW × hours × $/kWh × ~0.5 duty factor (Skutt FAQ **(pre-2024)**; [CCSA sheet, 2025](https://assets.noviams.com/novi-file-uploads/ccsa/Membership/Kiln_Firing_Cost.pdf)). Controllers that measure energy (Rohde, Skutt touchscreen, TAP, Genesis with a sensor) can replace the estimate with the real figure.

## 4. Adjacent domains worth learning from

| Tool | Domain | What maps to pottery | Price | Source |
|---|---|---|---|---|
| **Fabman** | Makerspace SaaS + hardware | A training course lists the equipment it covers, so adding a kiln to "kiln-trained" gives every holder access at once. Access needs both a package and a training record. Billing by machine time. Credits are used before money. Training expiry | €49–199/mo + €10–14 per connected machine; 0.5–2% fee | https://fabman.io/pricing |
| **Fab Manager** (open source, AGPL) | Fablab | Trainings with `authorization_period` and "use it or lose it" `invalidation_period`. Prepaid hour packs with validity. Plan limits per machine category. Wallet. Price matrix by group × plan | Self-host free; cloud €45–85/mo | https://github.com/sleede/fab-manager |
| **MemberMatters** (open source, MIT, AU) | Makerspace | Interlock pricing per session, per hour or **per kWh**. Cards off when overdue, back on when paid. Every swipe logged | Free | https://github.com/membermatters/MemberMatters |
| **Makerspace Leiden CRM** (Apache-2.0) | Makerspace | A permit chain (only a holder of X may grant Y). Machines flagged "requires permit" or "out of order". Storage requests with an expiry. A **"UFO" (unidentified found object) workflow:** photo → deadline → claimed / donated / gone | Free | https://github.com/MakerSpaceLeiden/makerspaceleiden-crm |
| **HMS2** (Nottingham Hackspace, MIT) | Hackspace | Usage moves IN_PROGRESS → COMPLETE → CHARGED. Booking types normal, induction, maintenance. Member boxes and labelled projects with printable labels | Free | https://github.com/NottingHack/hms2 |
| **Cobot** | Coworking | MakerSpace NYC records certifications with machine, date and signing technician. **Glaze firing at 6¢/in³** goes on the same invoice as membership | Per member | https://www.cobot.me/en/case-studies/case-study-makerspace-nyc |
| **Skedda**, **LibreBooking** (GPL fork of Booked) | Resource booking | Per-resource minimum/maximum length, notice, buffers, approval, check-in with auto-release, credits, peak rates | Skedda from $249/mo in the US (from $99 in EMEA); LibreBooking free | https://support.skedda.com/en/articles/112700-booking-conditions · https://github.com/LibreBooking/librebooking |
| **SuperSaaS** | Generic scheduling | The de facto tool at glass and photo studios. BAGI, Foci (which has a "Kiln Shop" schedule) and Gallery 44 run one schedule per shop | Free tier; $9–180/mo | https://www.supersaas.com/info/pricing · https://mnglassart.org/policies |
| **Kisi** + **Seam** | Door access | Kisi lists Kiln Fire and 24hr Pottery as integrations (Kiln Fire adds and revokes members automatically). Seam's "Access Grant" (user, spaces, start/end, credential) covers many lock brands | Kisi from $99 + $850 controller; Seam $5/device/mo | https://www.getkisi.com/integrations · https://www.seam.co/pricing |
| Wild Apricot, MemberPress, CiviCRM, Nexudus, OfficeRnD | Membership / coworking | Dues and renewals only; no equipment or certification | $59–399+ | https://www.wildapricot.com/pricing |

**Glass, wood and print studios** solve the same problems with generic tools and written rules:
- **Prerequisite ladders:** BAGI requires Glassblowing I → II → III, then practice, then an orientation. BARN's woodshop goes Orientation → Checkout 1 → Checkout 2, and a coordinator reprograms the member's fob.
- **Included consumables with overage:** Wheaton Arts includes about 10 lb of glass per hour and charges $8 for each extra pound.
- **No-show and cleanup fees:** Foci charges $36 for a cancellation inside 24 hours and $57 for not cleaning up.
- **Occupancy charges:** Foci charges "kiln occupancy time" for work left in the kiln and recycles uncollected work after 30 days.
- Sources: https://bagi.org/pages/renter-prerequisites-hot-shop · https://bainbridgebarn.org/studios/woodworking/woodworking-faq/ · https://www.wheatonarts.org/about/rental-opportunities/glass-studio-rental-rates/ · https://mnglassart.org/policies

**Patterns worth copying**
1. **Certification as a permission on a resource**, stored as a join table (training ↔ kiln), with expiry and an issuer.
2. **Two independent gates:** membership tier and certification.
3. **Usage events kept separate from charges** (HMS2's IN_PROGRESS → COMPLETE → CHARGED), with a charge computed by a versioned rule. This matches the fee engine's price snapshots.
4. **Several price components on one consumable:** per session + per hour + per kWh, or per in³ + minimum + oversize surcharge.
5. **Allowances and prepaid packs** consumed through a ledger: "30 L of firing per month", firing cards, Pickering's tickets.
6. **An unclaimed-work workflow** with photo, deadline, disposal date and final state.
7. **Several booking types on one kiln calendar:** firing, training firing, maintenance or out of order.
8. **Access that follows membership state**, modelled as a time-boxed grant (Seam style), if door access is ever added.

## 5. Voice of the studio owner

**Access.**
- **Reddit:** readable through its **RSS feeds** (`/r/<sub>/.rss`, `search.rss`, `comments/<id>/.rss`), one request about every 65 seconds. Blocked routes:
  - old.reddit.com HTML and `.json`: 302 to login
  - www.reddit.com `.json` and api.reddit.com: 403
- **Ceramic Arts Daily forum:** topic and search pages are Cloudflare-blocked (403). Its forum RSS (Business, Studio Operations) gives opening posts only, no replies.
- **Blocked or unusable:** Capterra, G2, Trustpilot (403); Facebook groups (login required); the Wayback CDX API (offline).
- **Quote checking:** every Reddit quote below was checked against the full thread text from RSS. Snippet-only material is marked **unverified**.

### Quotes (newest first)

| Date | Who | Quote | Link |
|---|---|---|---|
| 2026-10-03 | Member | "You pay firing fees either by weight or the volume it takes up in the kiln… if a piece weighs 1.24 lbs, it costs $1.24 to fire it." | https://www.reddit.com/r/Pottery/comments/1wv5hcw/is_my_studio_membership_worth_it_compared_to_the/pdiw27c/ |
| 2026-10-02 | Member | "We're required to get kiln lessons with the owner and get 'approved' before we can sign up for kiln dates… Sometimes it's a pain to find a kiln date on the calendar" | https://www.reddit.com/r/Pottery/comments/1wv5hcw/is_my_studio_membership_worth_it_compared_to_the/pdfkjwx/ |
| 2026-10-02 | Member, quoting staff | "we don't sell time at the studio we sell the space that is your shelf" | https://www.reddit.com/r/Pottery/comments/1wv5hcw/is_my_studio_membership_worth_it_compared_to_the/pdef0og/ |
| 2026-09-26 | Member | "we have a kiln booking calendar managed by the studio tech and members can rent out the whole kiln… The kiln loaders pick a few pieces from each member and then if there's space, load oldest to newest." | https://www.reddit.com/r/Pottery/comments/1wpd2c4/looking_for_community_studio_production_pottery/pc6fuz4/ |
| 2026-09-25 | Member | "Our studio lets you fire one bag of clay a month. Around 20 pieces… She has a breakdown of each kiln shelf for the price. A full kiln load being $75." | https://www.reddit.com/r/Pottery/comments/1wpd2c4/looking_for_community_studio_production_pottery/pbzu0np/ |
| 2026-09-25 | Member | "The makerspace/studio I am a member of now uses a time scheduling app to allow members to reserve time with high demand tools like the wheels… Also works great for tracking usage metrics" | https://www.reddit.com/r/Pottery/comments/1wpd2c4/looking_for_community_studio_production_pottery/pbzr7nx/ |
| 2026-09-24 | Studio volunteer | "the little community studio that I'm involved with is a victim of its own success… it is now time to take on the 'Production Potter' beast." | https://www.reddit.com/r/Pottery/comments/1wpd2c4/looking_for_community_studio_production_pottery/ |
| 2026-06-08 | Unclear (was a member in 2023) | "there doesn't seem to be an all in one solution… one software is great for booking and kiln tracking but you cannot connect Zapier, there's limited functionality for email customisation and follow up" | https://www.reddit.com/r/PotteryBusiness/comments/1tzd9qk/looking_to_talk_to_pottery_studio_owners_about/oqdv4fs/ |
| 2026-06-07 | Studio | "There are software… specifically for pottery studios… aside from the price they seem to cover most of the areas… We use Wix. It works for us. The biggest problem is time. I'm an artist, I don't love doing the admin stuff." | https://www.reddit.com/r/PotteryBusiness/comments/1tzd9qk/looking_to_talk_to_pottery_studio_owners_about/oqaqhk6/ |
| 2026-05-20 | Helping a potter | "I started by building us an excel spreadsheet but… the inconvience of computer vs. phone or… the blunt/ugliness and somewhat complexity" | https://www.reddit.com/r/Ceramics/comments/1th19zt/supply_tracking_and_other_business_related/omxxxxt/ |
| 2026-05-02 | Owner (pottery + hot glass) | "i have a pottery and hot glass membership studio. in fact was interested in finding someone to build one for me." | https://www.reddit.com/r/PotteryBusiness/comments/1p2p4kq/trying_to_build_a_pottery_studio_management_app/ojh4w81/ |
| 2026-02-21 | Owner, 10+ years | "We don't pre-screen anyone, but we've built our rulebook the hard way. Every bad behavior has earned its own rule." | https://www.reddit.com/r/Pottery/comments/1raeaxb/question_for_ceramic_studio_owners_only/o6jgoos/ |
| 2026-02-19 | Unclear | "Provide an automatic notification list for people on the waiting list in case a place in the master class becomes available." | https://www.reddit.com/r/PotteryBusiness/comments/1p2p4kq/trying_to_build_a_pottery_studio_management_app/o6a0mqe/ |
| 2026-01-13 | Prospective Kiln Fire user | "Does kilnfire implement cancellation deadlines so that members can cancel on their own?" | https://www.reddit.com/r/Pottery/comments/1c8jxhv/kiln_fire_software/nzdbv7u/ |
| 2026-01-03 | Owner | "I run a community studio. We lose about 4-5 pieces a year" | https://www.reddit.com/r/Pottery/comments/1q238sh/damage_rates_in_community_studio/nxffwte/ |
| 2026-01-03 | Member | "I would have pieces that would sit on the shelf fully dry for months and never get loaded into the kilns" | https://www.reddit.com/r/Pottery/comments/1q238sh/damage_rates_in_community_studio/nxg34iq/ |
| 2026-01-03 | Member (90–100-member studio) | "The shelves are a little bit of a cluster." / "She seems incredibly overworked." | https://www.reddit.com/r/Pottery/comments/1q238sh/damage_rates_in_community_studio/nxdrkwg/ |
| 2025-10-31 | Owner (opening) | "We are trying to find the best solution to allow our monthly members 'after-hours' access… We looked into kisi but it seems expensive." | https://www.reddit.com/r/smallbusiness/comments/1okhcmn/best_company_to_use_for_247_secure_access_to_our/ |
| 2025-10-08 | Staff | "if the studio also has a PYOP operation — my studio does and honestly that's where a lot of the money comes from." | https://www.reddit.com/r/PotteryBusiness/comments/1o0mov9/building_a_shared_dataset_resource_for_pottery/nica27r/ |
| 2025-05-24 | Prospective owner | "Does it integrate with other POS? We're part indie bookshop…" (to the Kiln Fire founder) | https://www.reddit.com/r/Pottery/comments/15nga0y/tracking_firing_at_studios/mtziwio/ |
| 2025-04-14 | Studio | "…make it easy for returning members to submit repeat orders without re-entering the same info each time. We're currently using a WordPress + WooCommerce setup, but it's feeling a bit clunky" | https://www.reddit.com/r/Ceramics/comments/1jz2t12/how_do_other_ceramic_studios_manage_online_firing/ |
| 2025-04-14 | Member | "the one I go to uses the Wild Apricot service to charge for membership dues and firing fees" | https://www.reddit.com/r/Ceramics/comments/1jz2t12/how_do_other_ceramic_studios_manage_online_firing/mn322z8/ |
| 2025-02-26 | Owner (CAD, opening post) | "many studios are owned by ceramic artists (not entrepreneurs)… a mess inside, improperly priced, poorly marketed" | https://community.ceramicartsdaily.org/topic/42807-pottery-studio-franchise/ |
| 2024-04-20 / 08-30 | Pottery-club admin | "I think the software could do about 80% of my job… I'm having trouble convincing the committee… a lot of oldies in the club." Later: "No I couldn't convince the oldies." | https://www.reddit.com/r/Pottery/comments/1c8jxhv/kiln_fire_software/ |
| 2023-08-11 | Member **(pre-2024)** | "It seems to work well 80% of the time except sometimes the emails are sent too early (like if your items are split up across multiple kiln loads) or too late." | https://www.reddit.com/r/Pottery/comments/15nga0y/tracking_firing_at_studios/jvoho3v/ |
| 2023-08-10 | Studio staff **(pre-2024)** | "I'm also very over firing tickets…" / "people are sneaky and move things too to fit their own work on" | https://www.reddit.com/r/Pottery/comments/15nga0y/tracking_firing_at_studios/ |

**Unverified (snippet only):** CAD threads, probably before 2024, topic not pinned down:
- "trying to figure out the cubic inches someone uses in a kiln is a royal pain"
- a membership including four cubic feet a month that "doesn't track really accurately the usage"

### What they mention and what they complain about

| Software named by studios or members (vendors' own posts excluded) | Mentions | Tone |
|---|---|---|
| Kiln Fire | ~5 comments in 2 threads | Interested or positive. The barriers are older members and committees, plus questions about self-cancellation and connecting an outside POS |
| Excel / spreadsheets | 3+ | Clunky, especially on a phone |
| Wix · WordPress + WooCommerce · Wild Apricot | 1 each | "Works for us" · "clunky" · neutral |
| Kisi | 1 | "Seems expensive" |
| WhatsApp groups, Instagram unload videos, Slack, Trello, Notion, webcams, index-card kanban | several, mostly **pre-2024** | Workarounds for "is my piece out?" |

**No studio-voice mentions anywhere reachable** of Mindbody, Momence, Pike13, Punchpass, Classly, Zenamu or Square Appointments for pottery. **No independent reviews** of Kiln Fire, Kilnfox, ClayTrack, WheelOwl or MudBuddy were reachable; AlternativeTo has zero reviews. The only price criticism of Kiln Fire came from a competitor's blog (ClayTrack).

**Recurring complaints:**
- **Nothing connects.** People want Zapier, email follow-up and an existing POS.
- **Admin time.** "I'm an artist."
- **Changing tools is hard** when a committee has to agree.
- **Firing tickets are tedious**, and so is re-entering repeat orders.
- **"Ready" emails fire too early** when a member's pieces span several kiln loads.
- **Shelves congest.** Work sits unfired for months and managers are overworked.

**Needs that are not on the [01](01-market-survey.md) list:**
- **Fairness for production potters:** a monthly allowance by bag, pound or shelf; whole-kiln rental; "a few pieces from each member, then oldest to newest".
- **Kiln approval before booking.**
- **Policy review and acceptance** as part of member onboarding.
- **Wheel and equipment reservations** with usage figures.
- **Waitlist auto-notify** and self-service cancellation deadlines.
- **PYOP as a revenue line** alongside memberships.
- **Firing for non-members.**
- **Stale-piece alerts** and a damaged or lost-piece log.
- **Data export.**

### Market activity (vendor posts, not customer voice)

- **Kiln Fire's founder** has posted on r/Pottery since 2023: "I can't tell you how many studios I've talked to that still track things on clipboards, post-it-notes, or spreadsheets."
- **New builders posted in 2025–2026:** Claybench (CAD, 2026-07-28: studios "end up stitching together a class booking app, a spreadsheet for pieces, a glaze/firing log, and some e-commerce platform that skims a fee off every sale"), KilnTrack, CeramicSys ("a few studios in Australia"), WorkshopFolio, ClassDuo (same founder as Kiln Fire), and at least three anonymous r/PotteryBusiness builders.
- **r/Pottery bans self-promotion.** Moderators removed a Kiln Fire-related comment in 2024. r/PotteryBusiness is where owners and builders talk.
- Sources: https://www.reddit.com/r/Pottery/comments/1c8jxhv/kiln_fire_software/ · https://www.reddit.com/r/PotteryBusiness/comments/1p2p4kq/trying_to_build_a_pottery_studio_management_app/

## Where [01](01-market-survey.md) is wrong or out of date

| 01 said | Evidence now |
|---|---|
| "Many studios still run general booking tools (Mindbody, Momence, Pike13, Square, Acuity)" | Of 84 studios, Momence and Pike13 appear 0 times, Mindbody 2, Square Appointments 0, Acuity 8. The dominant pattern is website-builder commerce (33) plus class-registration and municipal systems (20) |
| "None has a permanently free plan" | Zenamu has a free "Getting Started" plan (30 orders/month, 1 teacher). 01's own competitor table says "Free tier". Kiln Fire's pricing page is now a "Build a Custom Plan" configurator with no public prices: https://zenamu.com/pricing/ · https://www.kilnfire.com/pricing |
| The niche is "crowded" | It is crowded with vendors, not customers: 3 of 84 studios run pottery-specific software, all Kiln Fire. Kiln Fire is also visible at Amphora, Berkshire Art Center, Dirtbag ArtHaus and Centered Clay Studio, outside the sample |
| KilnAid source and pricing ("$20–100 per kiln per year") | The cited help.kilncontrol.com page is TAP (SDS Industries). KilnLink is $100/kiln/yr (2026); KilnAid+ $19.99/yr; TAP Premium $49.99/yr |
| Glazy: "Open-source recipe database, free" | The code is open, but the **data is CC BY-NC-SA and the 2025 data policy forbids scraping; commercial use needs a licence**. That affects "import from Glazy" |
| Firing fees "mostly $0.03–0.08 per cubic inch" | True in the US. UK and Australian studios charge per kg (£5–10/kg, A$9–20/kg) or per litre (Turning Earth) |
| "Price creep as a studio grows is the most common complaint" | **Not supported** by studio voices that could be read. Owners talk most about tools that don't connect, admin time, and members resisting change. The only price criticism of Kiln Fire found was on a competitor's blog. Price still matters ("aside from the price…"), but it is not the top complaint. The evidence is thin, so treat this as unproven either way |
| Reddit, CAD and Facebook "could not be read" | Reddit can be read through RSS feeds, and CAD opening posts through forum RSS (see [section 5](#5-voice-of-the-studio-owner)). Facebook is still closed |

## Implications for the plan

**Wave 1 scope: keep it, with six adjustments. All are small changes to features 2–4, not new features.**
1. **Fee engine units.** Support per in³, per kg, per lb, per litre (volume in metric), per shelf, flat and per ticket from the start. Also support **allowances** ("30 L per month", "one shelf per month"), **oversize surcharges** (by longest dimension), **member % discounts**, minimums, and **"firing included in clay price"** (a zero-rate rule with a note). The census shows all of these in live use; see the table in [section 1](#what-the-sites-say-about-fees-pieces-and-shelves).
2. **Payment timing.** The ledger needs a per-studio setting for **when** a fee becomes payable: at drop-off, at pickup, end of session, or monthly statement. Several studios already invoice monthly (Berkeley, Morning, Kilnfolk, Hayden via Amilia).
3. **Pull a small part of the shelf work forward.** Add a configurable **hold period and "unclaimed" state** to the piece state machine in Wave 1, with reminders before disposal. Every studio publishes a hold period (1 week to 2 months), and the makerspace "UFO" model shows it is simple. Shelf assignment can stay in Wave 2.
4. **Notifications per submission, not per kiln.** The one concrete complaint about an existing tracker is "ready" emails sent "too early (like if your items are split up across multiple kiln loads)". Group pieces into an intake batch and notify when the whole batch is ready, or say "3 of 5 ready". Add a "stale piece" alert for work that sits in one stage too long ("sit on the shelf fully dry for months").
5. **Repeat intake.** Add "same as last time" (clay, cone, glaze, size class) so firing tickets aren't re-typed. This came up in a WooCommerce studio's complaint.
6. **Load queue policy.** Feature #4 (kiln loads) should let the studio choose how the queue is ordered:
   - oldest first;
   - at most N pieces per member per load;
   - whole-kiln rental as its own load type.

   This is how studios handle production potters today, and it costs little to add while the load builder is being built.

**Piece identification.** QR labels are a good idea, but no studio uses them today. Everyone uses maker's marks and paper. So in Wave 1:
- support a **registered maker's mark plus photo** as the fallback identity;
- make the printed QR label optional;
- make the photo-at-intake flow work with no label printer, since phone camera plus paper tag is the realistic start.

The unprompted Past Lives spec (photos, H×W×L, clay, bisque/glaze, queue, "ready" notification) matches feature #2 almost line for line. **Use it as an acceptance test.** They have already built it into their own member app (three PRs, merged 2026-10-08), so they are not a pilot candidate for this feature. Their public implementation is a useful reference for the loading and unloading workflow, and the story itself is evidence that studios want this badly enough to build it.

**Importers (Wave 2): re-prioritise.** Drop Mindbody and Momence as the first importers. In order:
1. **Generic CSV with column mapping**, which covers Google Sheets and Forms exports and Airtable.
2. **Shopify** customers and orders (10 studios).
3. **WooCommerce** (10).
4. **Acuity** client export (8).
5. **Squarespace and Wix** contacts and orders (12).

Mindbody (2 studios, both in London) can wait until a pilot needs it.

**Integrations.**
- **Don't build a booking calendar to replace the storefront.** Studios sell classes through Shopify, WooCommerce, Squarespace, Wix and Acuity, and they will keep doing so. Position the product as the **back room** (pieces, kilns, fees) that sits beside whatever storefront they use:
  - a "booking link" field;
  - inbound webhooks or a Zapier-style hook to create members from orders, later.
- **Payments.** Square appears at only 7 of 84 studios, mostly for gift cards. Stripe is what Kiln Fire, BKLYN CLAY's custom app, GetGalore and several WooCommerce sites use. Keep Square as planned if the pilot runs Square POS, but put payments behind a provider interface so Stripe Connect can follow. Revisit the Square-only decision in [02](02-wave1-plan.md#decisions-2026-10-19) when choosing a pilot.
- **Kilns (Wave 3, possibly earlier).** No vendor offers an API. Build **vendor-neutral firing-log file import** first:
  - formats: Genesis LAN CSV, Rohde CSV, Nabertherm NTLog CSV, Stafford USB CSV;
  - map each to one canonical firing record attached to a kiln load;
  - add a webhook receiver for jbruce12000/kiln-controller as a live demo;
  - ask Bartlett for partner access rather than using the undocumented KilnAid endpoints.
  - This is small and attaches directly to Wave 1's kiln loads, so a CSV importer could move into Wave 2.
- **Glazy (Wave 2 glaze book).** Only a **user-initiated import of the user's own YAML export**, plus links to recipe URLs. Before seeding anything from glazy-data, email info@glazy.org about whether a free SaaS counts as non-commercial.
- **Supplies OCR.** Suppliers have no APIs, and invoices arrive by email and PDF, so the **email-forward inbox** in [02](02-wave1-plan.md) is the right way in. Shopify suppliers' `/products.json` could help match SKUs (check terms).
- **Waivers and door access:** not needed early. No e-waiver vendor appears in the census, and one owner called Kisi "expensive". A dated "studio rules accepted" record (with version) on the member covers Wave 1. If doors come later, model them as Seam-style access grants.

**API and webhooks early (Wave 2, not Wave 3).** "You cannot connect Zapier" and "does it integrate with other POS?" are the integration asks owners actually make. Ship these with the importers:
- a documented REST API (already OpenAPI-first in [02](02-wave1-plan.md));
- outbound webhooks (piece ready, fee charged, member joined);
- CSV export.

They are cheap given the outbox design, and they answer the "nothing connects" complaint better than building more booking features.

**Certification (Wave 3) needs data-model space now.** Members describe needing to "get 'approved' before we can sign up for kiln dates". Radius, Clay Shed, Fancy Banana and Pottery Northwest all gate kilns or keys on training. Wave 1's roles should not hard-code "member vs staff". Leave room for a `certification` ↔ `resource` join table with expiry and issuer (Fab Manager / Leiden pattern), so "kiln-trained" can be added without a migration headache.

**Positioning.**
- "Free, and it works alongside the booking tool you already have."
- The competition is paper, forms and do-it-yourself builds more than Kiln Fire. BKLYN CLAY, Clay Temple and Past Lives Makerspace have built their own piece and firing tools, and Twelve80 built its whole site on Bubble.
- Bigger studios that build their own will want **an API and data export**. Offer both early; it is also good resume material.
- Market the US version in cubic inches and the UK/AU version in kilograms from day one. The units module already supports this.
- **Launch channel:** r/Pottery bans self-promotion. r/PotteryBusiness and the CAD Business forum are where owners and builders talk, and at least six other builders posted there in 2025–26. Lead with free, data export and "works with your storefront", not with another feature list.
- **PYOP is a real segment.** 10 of 84 census studios are PYOP or hybrids, and one staffer says the PYOP side is "where a lot of the money comes from". Their piece flow is simpler: order number on the base, one firing, pickup notice. It fits the Wave 1 piece tracker without membership features, so it is a possible early-adopter segment.

## Sources

**Census** (all accessed 2026-10-19): the 84 evidence URLs in the [per-studio table](#per-studio-table), plus:
- https://bklynclay.com/firing · https://fire.bklynclay.com · https://www.turningearth.org/classes · https://www.thekilnrooms.com/drop-in · https://www.radiusstudio.org/membership/ · https://neoceramics.com.au/faq · https://www.claytivitypottery.com/memberships · https://www.nccshop.org/pages/classes-for-adults-kids · https://ma.to/venue/morningceramics
- City round-ups used to find studios: https://www.pdxmonthly.com/arts-and-culture/ceramics-pottery-studios-portland · https://sitchu.com.au/melbourne/whats-on/best-pottery-classes-melbourne · https://perthisok.com/lifestyle/perths-best-pottery-studios/ · https://www.blogto.com/arts/2024/08/pottery-classes-painting-toronto/ · https://www.countryandtownhouse.com/interiors/pottery-painting-london/ · https://www.austingallery.org/blog/adult-art-classes-workshops-austin · https://fashionjournal.com.au/life/pottery-classes-melbourne/ · https://openworkshopnetwork.com/discipline/ceramic/

**Vendors and pricing:**
- https://zenamu.com/pricing/ · https://www.kilnfire.com/pricing · https://www.getkisi.com/integrations · https://www.getkisi.com/integrations/kilnfire-kisi · https://docs.kisi.io/marketplace/creative_space_management/kilnfire

**Back office:**
- Studio policies and forms: https://www.berkeleypottersstudio.com/s/2023FINAL_TPS_MemberHandbook.pdf **(pre-2024)** · https://www.thepottersstudio.org/firing-services · https://www.thepottersstudio.org/members-zone · https://www.visarts.org/studio-access/equipment/community-pottery-firing/ · https://www.kilnfolkclay.com/studio-use-policies · https://www.cityofirvine.gov/sites/default/files/legacy-documents/civica-32726-firing-form_updated_9.28.pdf · https://placedesarts.ca/wp-content/uploads/2026/03/2026_2027-Student-Ceramics-Handbook.pdf · https://explore.wspr.ca/INFO_POTTERYMANUAL2025.pdf · https://canberrapotters.com.au/firing-service/ · https://www.potterysuppliesonline.com.au/firing-service/ · https://www.silvermineart.org/wp-content/uploads/2026/02/OpenStudio_Ceramics_SP2026.pdf · https://coloradoclaystudio.com/memberships/ · https://hollandarts.org/ceramics-studio · https://ratcitystudios.com/firing-service · https://gasworks.org.au/visit-us/kiln-firing-service/ · https://www.amphoraceramicstudio.org/firing-services · https://www.berkshireartcenter.org/kiln-firing · https://www.centeredclaystudio.com/open-studio-membership · https://www.dirtbagarthaus.com/firing-fee-log-calculator
- NWTC: https://www.nwtc.edu/about-nwtc/nwtc-locations/artisan-and-business-center/open-studio/open-studio-schedule · https://www.nwtc.edu/getmedia/915e6968-3078-447d-9c92-6fbaff165630/Ceramic-Studio-Newsletter-Block-7-2024.pdf
- Notion, makerspace wikis and self-built tools: https://moderndesert.notion.site/Memberships-efe0aae4e93e4747bc03f75c23131c05 **(pre-2024)** · https://wiki.tinkermill.org/index.php/Pottery/Ceramics · https://sites.google.com/a/makeitlabs.com/wiki/resource-areas/ceramics-studio **(pre-2024)** · https://github.com/Past-Lives-Makerspace/plfog/issues/691 · https://introductorycourse.lovable.app/module/before-they-leave
- PYOP: https://squareup.com/gb/en/townsquare/seller-spotlight-social-pottery **(pre-2024)** · https://www.entrepreneur.com/buying-a-franchise/heres-how-these-two-immigrants-started-their-own-color-me-mine-franchise · https://www.robertgordonaustralia.com/pages/paint-your-own-pottery · https://asyouwishpottery.com/pages/faqs · https://assets.noviams.com/novi-file-uploads/ccsa/Membership/CCSA_-_Opening_a_Studio_Guide.pdf
- Articles and case studies: https://ceramicartsnetwork.org/pottery-making-illustrated/pottery-making-illustrated-article/in-the-studio-streamlining-for-success · https://mailchimp.com/case-studies/pottery-west/ **(pre-2024)** · https://www.ceramicsnow.org/news/introducing-kiln-fire-a-new-app-for-pottery-studio-management **(pre-2024)** · https://ceramicartsnetwork.org/ceramics-monthly/ceramics-monthly-article/Tips-and-Tools-Firing-Fee-Calculator-197715 **(pre-2024)**

**Kiln, glaze, materials:**
- Skutt: https://skutt.com/kilnlink-page/kilnlink-faq/ · https://skutt.com/images/Ceramic-Kiln-Price-List-2026-1.pdf · https://skutt.com/minitouchscreen/ · https://skutt.com/kidsneedclay/keys-to-success/
- Bartlett and TAP: https://bartlettinstrument.com/kilnaid · https://apps.apple.com/us/app/bartlett-kilnaid/id1336294986 · https://help.kilncontrol.com/knowledge/premium-subscription-vs-free-subscription
- Community kiln integrations on GitHub: https://github.com/carterworks/bartlett-home-assistant · https://github.com/zander8807/home-assistant-kilnaid · https://github.com/rogerlew/skutt-kiln-monitor · https://github.com/jbruce12000/kiln-controller
- Other controllers: https://www.ortonceramic.com/autofire-controllers · https://apps.apple.com/us/app/rohde-mykiln/id1621886007 · https://nabertherm.com/en/mynabertherm-app · https://kilnportal.co.uk · https://potclays.co.uk/st215-usb-data-log
- Glaze software: https://help.glazy.org/about/data-use-policy · https://help.glazy.org/about/changelog · https://github.com/derekphilipau/glazy-data · https://insight-live.com · https://digitalfire.com/glossary/digitalfire+api · https://hyperglaze.com
- Suppliers: https://lagunaclay.com · https://axner.com · https://clay-king.com/purchase-orders/
- Firing cost: https://assets.noviams.com/novi-file-uploads/ccsa/Membership/Kiln_Firing_Cost.pdf · https://ceramicartsnetwork.org/daily/article/a-super-handy-way-to-calculate-firing-fees **(pre-2024 original)**

**Owner voice** (Reddit read via RSS; comment links in the [quotes table](#quotes-newest-first)):
- r/Pottery: https://www.reddit.com/r/Pottery/comments/1wv5hcw/is_my_studio_membership_worth_it_compared_to_the/ · https://www.reddit.com/r/Pottery/comments/1wpd2c4/looking_for_community_studio_production_pottery/ · https://www.reddit.com/r/Pottery/comments/1q238sh/damage_rates_in_community_studio/ · https://www.reddit.com/r/Pottery/comments/1raeaxb/question_for_ceramic_studio_owners_only/ · https://www.reddit.com/r/Pottery/comments/1c8jxhv/kiln_fire_software/ · https://www.reddit.com/r/Pottery/comments/15nga0y/tracking_firing_at_studios/ **(pre-2024 thread with 2025 replies)**
- r/PotteryBusiness: https://www.reddit.com/r/PotteryBusiness/comments/1tzd9qk/looking_to_talk_to_pottery_studio_owners_about/ · https://www.reddit.com/r/PotteryBusiness/comments/1p2p4kq/trying_to_build_a_pottery_studio_management_app/ · https://www.reddit.com/r/PotteryBusiness/comments/1o0mov9/building_a_shared_dataset_resource_for_pottery/
- r/Ceramics: https://www.reddit.com/r/Ceramics/comments/1jz2t12/how_do_other_ceramic_studios_manage_online_firing/ · https://www.reddit.com/r/Ceramics/comments/1th19zt/supply_tracking_and_other_business_related/
- r/smallbusiness: https://www.reddit.com/r/smallbusiness/comments/1okhcmn/best_company_to_use_for_247_secure_access_to_our/
- CAD forum: https://community.ceramicartsdaily.org/topic/42807-pottery-studio-franchise/

**Adjacent domains:**
- Makerspace software: https://fabman.io/pricing · https://fabman.io/product · https://www.fab-manager.com/features · https://github.com/sleede/fab-manager · https://github.com/membermatters/MemberMatters · https://github.com/MakerSpaceLeiden/makerspaceleiden-crm · https://github.com/NottingHack/hms2 · https://github.com/Protospace/spaceport
- Coworking and booking: https://www.cobot.me/en/case-studies/case-study-makerspace-nyc · https://www.skedda.com/pricing · https://support.skedda.com/en/articles/112700-booking-conditions · https://github.com/LibreBooking/librebooking · https://www.supersaas.com/info/pricing · https://www.joinzipper.com/business-management-software/makerspaces
- Door access: https://www.seam.co/access · https://www.seam.co/pricing
- Membership tools: https://www.wildapricot.com/pricing · https://memberpress.com/pricing/
- Glass, wood and print studios: https://bagi.org/pages/renter-prerequisites-hot-shop · https://mnglassart.org/policies · https://www.wheatonarts.org/about/rental-opportunities/glass-studio-rental-rates/ · https://bainbridgebarn.org/studios/woodworking/woodworking-faq/ · https://www.pratt.org/access-info · https://bkmetalworks.com/studio · https://edinburghsculpture.org/?p=14761
