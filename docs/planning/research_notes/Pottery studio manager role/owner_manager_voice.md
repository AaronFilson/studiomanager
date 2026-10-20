# Owner and manager voice: what is hard, slow or broken about running a pottery studio

_Researched 2026-10-20. Working notes with the evidence log first, then the analysis. The closing sections are Hypothesis evidence, Other strong signals, and Gaps and limits._

## Summary

- **Volume:** 97 coded entries; 90 verbatim and verified, 4 reporter paraphrase, 3 unverified. They come from **34 independent studios or organisations** with a problem statement, and at least 40 people. 27 sources are dated 2023–26.
- **Geography:** US 23, Canada 5, UK 3, Australia 1, NZ 1, not stated 1.
- **Reddit was unreachable** (WebFetch refuses Reddit hosts) and the shared search budget ran out, so discovery leaned on podcast show notes (14 sources), news (9) and studios' own pages (9).
- **Top problems by independent source (n = 34):**

  | Rank | Category | Sources |
  |---|---|---|
  | 1 | Money: rent, leases, energy, pricing, cash flow | 19 |
  | 2 | Owner/manager burnout and admin load | 16 |
  | 3 | Equipment breakdowns and upkeep | 10 |
  | 4 | Staffing and coverage | 9 |
  | 5 | Inventory and supplies | 6 |
  | 6= | Pieces and shelves; scheduling; bookkeeping and taxes | 5 each |
  | 9= | Member communications; capacity and waitlists | 4 each |
  | 11= | Kiln logistics; software; safety; cleanliness | 3 each |

- **Hypotheses:**
  - Inventory tracking: moderate (pain is mostly supplier stock-outs).
  - Equipment/asset tracking: moderate (pain strong at 10/34; explicit tracking demand weak).
  - Multi-provider payments: weak. 0 of 34 name Stripe or Square; 0 ask for more than one provider.
  - QuickBooks Online integration: weak. 1 of 34 names QuickBooks, positively, in a vendor-hosted story.
  - Scheduling: moderate (5 problem sources + 4 capacity sources; 8 of a manager's 20 posts are schedule or closure notices).
- **Outside the hypotheses:**
  - The hired manager is a single point of failure.
  - Admin crowds out owners' own making (6 sources).
  - Rent, leases and energy dominate money worries, not payment processing.
  - Studios manage with written policies (shelf fines, orphan shelves, 60-day racks).
  - No source gives a number of admin hours per week.

## Plan (as executed)

1. Discovery: web searches (owner blogs, "why we closed", news 2023–2026, the UK studio boom, Ceramic Arts Network articles, podcasts with transcripts), plus Reddit RSS search queries (r/Pottery, r/Ceramics, r/PotteryBusiness, r/smallbusiness). **Reddit could not be fetched at all this session, and the shared search budget ran out after 28 searches. From then on, discovery mostly followed links from pages already opened (3 later searches succeeded).**
2. Fetch each primary page directly; ask WebFetch for exact sentences; quote only verbatim text it returns. Anything seen only as a search snippet is marked UNVERIFIED.
3. Log every quote in the evidence table: ID, quote, speaker role, segment, country, date, URL, codes, severity signals, verification status.
4. Code against the 14 given categories, adding codes when needed. Count by independent source: one source is one studio or organisation, and people or quotes from the same studio count once per category.
5. Close with: category ranking with source lists, time and outputs, hypothesis evidence, no-manager findings, other strong signals, gaps and limits.

Rules followed: no shell, no agents, no downloads, no forms. Quotes already used in 01-market-survey.md and 03-tools-in-use.md are not reused as new evidence (cross-references only).

## Code key

| Code | Category |
|---|---|
| KILN | kiln and firing logistics and backlog |
| PIECE | members' pieces (lost, unclaimed, shelf space) |
| INV | inventory and supplies (running out, clay/glaze tracking, ordering, theft, cost control) |
| EQUIP | equipment breakdowns and maintenance |
| CLEAN | cleanliness and shared-space behaviour |
| SCHED | scheduling (classes, open studio, staff coverage, kiln calendar) |
| STAFF | staffing, volunteers and work exchange |
| MONEY | money (pricing, cash flow, fee fairness, collecting payments, rent) |
| BOOKS | bookkeeping, accounting and taxes |
| SOFT | software (too many tools, tools that don't connect, cost) |
| COMMS | communication with members |
| MKT | marketing and finding students |
| SAFE | compliance and safety (silica, insurance, waivers) |
| BURN | owner burnout and admin load |
| CAP *(new)* | capacity and waitlists: demand exceeds kilns, wheels or space |
| NOMGR *(new)* | no manager: owner does everything, member-run, volunteers cover |
| SALES *(tag)* | sales and payment practice (POS, checkout); a topic tag, not a problem code |
| OUTPUTS *(tag)* | something the studio must produce (notices, orders, newsletters, logs); a topic tag, not a problem code |

Codes in the log mark every topic a quote touches. The ranking in the Analysis counts only problem statements (see its rule), so some log codes do not enter it, for example CLEAN on Q01 and Q39, or COMMS on routine notices.

Severity tags: [HRS] hours lost, [$] money lost, [MEM] members lost, [BURN] burnout, [CLOSE] closure.
Member tag: [MEMBER-MGMT] = a member complaint that reveals a management task or failure.

## Evidence log

Access notes as found: WebFetch refuses every Reddit host this session ("Claude Code is unable to fetch from www.reddit.com", same for old.reddit.com and reddit.com), so the Reddit RSS route used in 03 is closed. The Ceramic Arts Daily forum returns 403. WebFetch's reader truncates any single quoted span at 125 characters, so long sentences appear as excerpts with "…".

Columns: ID · quote (verbatim as returned by WebFetch) · speaker and role · studio, segment · country · date · URL · codes · severity · status (V = re-opened and verified by WebFetch; U = UNVERIFIED snippet).

### Batch 1 (2026-10-20)

| ID | Quote | Speaker (role) | Studio · segment | Ctry | Date | URL | Codes | Sev. | St |
|---|---|---|---|---|---|---|---|---|---|
| Q01 | "Special thanks to Haruko for taking over kiln loading, supply inventory, cleaning and generally keeping the studio running." | AJ Phirman (hired studio manager) | Fitton Center for Creative Arts, Hamilton OH · nonprofit art centre | US | 2025-06-03 | https://www.fittoncenter.org/grouppage/pottery-studio/discussion/8ca36d35-0a3e-4d64-bd43-f84f816be3d7 | STAFF, KILN, INV, CLEAN, OUTPUTS | — (single point of failure) | V |
| Q02 | "I broke my ankle back in April (playing kickball of all things!) which took me out of the studio for weeks." | same | same | US | 2025-06-03 | same | STAFF (coverage) | — | V |
| Q03 | "There is a small delay in firing for a few pieces due to an issue with one of the kilns." / "The small kiln is loaded, just waiting for a thermocouple replacement on Friday." / "Let me know if you have any pieces that have a deadline." | same | same | US | 2025-06-03 | same | EQUIP, KILN, COMMS | delay | V |
| Q04 | "I unloaded all three kilns this morning with lots of bisque and glazeware!" / "I also loaded and fired two more loads of bisqueware today, but I'll waiting until Tuesday to fire another glaze load." | same | same | US | 2025-01-31 | https://www.fittoncenter.org/grouppage/pottery-studio/discussion/43471fa7-853f-4146-bb6f-50713c01ab55 | KILN, COMMS, OUTPUTS (firing updates) | — | V |
| Q05 | "Hello! I made an error on the original summer schedule. We are closed for Studio+ next week as well 6/26." / "Please reach out if there are any clay emergencies (aka you need to come in to keep things wet.)" | same | same | US | 2026-06-19 | https://fittoncenter.org/grouppage/pottery-studio/discussion | SCHED, COMMS | — | V |
| Q06 | "I just picked up and restocked 10 boxes of 112 speckled brown clay." | same | same | US | 2026-05-05 | same | INV (manager picks up stock), OUTPUTS | — | V |
| Q07 | "Some things have been coaxed for very long, living with a nonprofit," / "We need quality equipment that's going to last a long time." / on the wood kiln: "But looking at what our population is like now, the cost of running it, and the cost of rebuilding it" | Meg Smallwood (studio director, hired) | John Bryan Community Pottery, Yellow Springs OH · nonprofit community studio | US | 2026-07-10 | https://ysnews.com/news/2026/07/john-bryan-community-pottery-wheels-into-new-era | EQUIP, MONEY | asset retired | V |
| Q08 | "Pottery is expensive. There's no getting around it," | same | same | US | 2026-07-10 | same | MONEY (pricing/access) | — | V |
| Q09 | Founder's email: studio "closed because of her declining health" (reporter's framing around a quoted phrase); landlord had filed to evict citing unpaid rent from February (reporter, not owner) | Rebecca Olbright (owner), via Mission Local | Clay by the Bay, San Francisco · membership/teaching studio | US | 2026-07-15 | https://missionlocal.org/2026/07/san-francisco-clay-by-the-bay-closes/ | owner health (not coded BURN), MONEY (rent arrears, reported) | CLOSE | V (owner did not comment) |
| Q10 | Former manager "said she left after what she called frustrations over worker pay." | Crimson Cashman (former hired manager) | same | US | 2026-07-15 | same | STAFF, MONEY | staff lost, CLOSE | V (paraphrase by reporter; not a direct quote) |
| Q11 | Members "worried about retrieving their work from the shuttered building" (reporter paraphrase); billing after closure: see Q77 (Hoodline) | members [MEMBER-MGMT] | same | US | 2026-07-15 | same | PIECE, MONEY | CLOSE | V (paraphrase) |
| Q12 | "We were unable to keep up with our rent," / "It is with a heavy heart I share that we have lost our lease for our studio." | Clayton (owner) | KClayton Studio, Overland Park KS · arts-and-crafts/teaching storefront (pottery content not confirmed) | US | 2022-02-07 (pre-2023) | https://johnsoncountypost.com/?p=218275 | MONEY (rent) | CLOSE (storefront) | V |
| Q13 | Price rise "was a necessary increase that will help in our pursuit to continue providing high-quality ceramic experiences" / revenue "pays a living wage to our employees, rewards our teachers with pay bumps…" / "Our per-class hourly cost is approximately $17.50" | Ed King (executive director, hired) | Pottery Northwest, Seattle · nonprofit art centre | US | July 13, year not shown (after the Pioneer Square move; accessed 2026-10-20) | https://www.potterynorthwest.org/news/price-increase-what-you-should-know | MONEY (pricing, staff pay, equipment) | — | V |

### Batch 2 (2026-10-20)

| ID | Quote | Speaker (role) | Studio · segment | Ctry | Date | URL | Codes | Sev. | St |
|---|---|---|---|---|---|---|---|---|---|
| Q14 | "And when nobody else stood up to take over, I ended up falling into it." / "Amazingly, I'm spending most of my time running the studio now, instead of actually doing ceramics!" | Rob Nealon (new leaseholder, owner-operator) | Ilium Ceramics, near Telluride CO · membership/community studio (member-run before takeover) | US | 2024-11-23 | http://koto.org/news/after-period-of-uncertainty-ilium-ceramics-studio-to-continue | BURN (admin load), NOMGR | near-CLOSE | V |
| Q15 | "…and a lot of it hinged on who was going to be able to sign a commercial lease." / "I know that the lease had a lot of stipulations that were…concerning in terms of personal liability." / "So it's been looming over our heads for several months without a lot of clarity." | Robin Kondracki (studio technician) | same | US | 2024-11-23 | same | MONEY (lease, liability), NOMGR | near-CLOSE | V |
| Q16 | "[I remember] how frantic we were all summer, asking: 'what are we going to do without pottery?" / "And we were looking on Facebook Marketplace, talking about buying a kiln to stuff in the basement of Robin's apartment." | Mollie Theis (member) [MEMBER-MGMT] | same | US | 2024-11-23 | same | NOMGR | near-CLOSE | V |
| Q17 | "We expect our electricity bills to quadruple from £14,000 a year when our fixed-rate tariff runs out shortly." / "We have an incoming VAT bill and must make decisions regarding the business." | Mark Giddens (co-owner; press release) | Chessell Pottery & café, Isle of Wight · PYOP/café | UK | 2023-03-12 | https://iwobserver.co.uk/chessell-a-pottery-no-more/ | MONEY (energy), BOOKS (VAT) | [$], pottery line closed | V |
| Q18 | "We had a local lady make the pottery which children could paint, but it's just unviable." / "…right here and now, we're struggling to make a profit." | same | same | UK | 2023-03-12 | same | MONEY | CLOSE (pottery-painting line) | V |
| Q19 | "Starting March 1, the option to suspend membership dues for one month each year will no longer be available." / "This change will help us plan for operational costs, improvements, and the future of our thriving studio." | management, unsigned post "What's Ahead in 2025" | The Potters' Studio, Berkeley CA · nonprofit membership studio | US | undated, c. early 2025 (accessed 2026-10-20) | https://www.thepottersstudio.org/whats-ahead-in-2025 | MONEY (cash-flow predictability, dues pauses) | — | V |
| Q20 | "Rack of Doom" post excerpt: "keep work moving through and out of the studio to make room for more work" / "their hard work will be forever lost" | management, unsigned blog listing | same | US | undated (accessed 2026-10-20) | https://www.thepottersstudio.org/studio-news | PIECE (unclaimed work, shelf space) | — | V (listing excerpt; post itself 404) |
| Q21 | Owners retired a large kiln costing ~£200 a firing; energy bills rose from £700 to £1,600 a month; clay and glaze up; pivot to café and teaching "citing huge increases in energy costs" | Wenlock Pottery owners (production pottery + café), via Express & Star | Wenlock Pottery, Much Wenlock, Shropshire · production pottery | UK | 2023-05-12 | https://www.expressandstar.com/whats-on/shropshire-pottery-celebrates-ps30000-arts-council-grant-to-diversify-5864904 | MONEY (energy), EQUIP (kiln retired), INV (material costs) | [$] | **U** (403; search snippet only) |

### Batch 3: The Potters Cast show notes (written Q&A excerpts of the guest's answers; no full transcripts)

| ID | Quote | Speaker (role) | Studio · segment | Ctry | Date | URL | Codes | Sev. | St |
|---|---|---|---|---|---|---|---|---|---|
| Q22 | "That is definitely answering people's e-mail with all kinds of questions" / "especially when people want to set up their own private events and classes" / "schedule rescheduling or setting up a makeup class." / "That's, we didn't think of, takes a lot of effort every day." | Jeffery Liang (owner) | Spiffy Pottery Studio, Menlo Park CA (opened Feb 2025) · membership/teaching studio | US | 2026-08-11 | https://thepotterscast.com/1253/ | COMMS, SCHED, BURN (admin load) | [HRS] daily | V |
| Q23 | On the commercial lease: "So when they send their contract for the building, it's over 30 pages in a language I don't understand." / "So that snowball into a big amount of amount of money and I really regret paying that." | same | same | US | 2026-08-11 | same | MONEY (lease, legal fees) | [$] | V |
| Q24 | "because, for example, during the summer, like right now, we do have fewer members" / "because people are out of town." | same | same | US | 2026-08-11 | same | MONEY (seasonal membership) | [MEM] seasonal | V |
| Q25 | "I would rather rethink my electrical layout for the wheels because that matters a lot." / "it might overpower the breaker box, whatever it's connected to." | same | same | US | 2026-08-11 | same | EQUIP (facility/electrical) | — | V |
| Q26 | "Sometimes teaching classes in the beginning got very repetitive." / "It kind of started to wear on me a little bit." / "I kind of hit some burnout and had to figure out how to overcome that." | Shelby Trahan (owner) | Jacques' Pottery Shop, Welsh LA · teaching studio (small town) | US | 2026-06-11 | https://thepotterscast.com/1236/ | BURN | [BURN] | V |
| Q27 | "Yeah, I cannot price here in my small community the way that other studios are able to price, and I'm okay with that." / "Um, January, February is always our slow season." / "So we save up and then we use that to get through our slower times." / "you're you're not going to be paying yourself as much as you could be making other places." | same | same | US | 2026-06-11 | same | MONEY (pricing, cash flow, owner pay) | — | V |
| Q28 | "I could go on a complete tangent about the war I have been through..." / "...but I currently don't have nice things to say." (about her website platform) | Sela Stazzone (owner) | Jean Ceramics (location not stated) · teaching studio + retail | not stated | 2025-10-14 | https://thepotterscast.com/1169/ | SOFT | — | V |
| Q29 | "And I had to get more beefier insurance under the business and the brick and mortar." | same | same | not stated | 2025-10-14 | same | SAFE (insurance) | [$] | V |
| Q30 | "I joke around that I practically live here and that I have an air mattress under my desk." / "I'm here all the time." | same | same | not stated | 2025-10-14 | same | BURN (hours) | [HRS] | V |
| Q31 | On rent: "Absolutely. It stresses me out all of the time." / "If I had the means to buy a building, Oh my God, I would do it yesterday." | Fawn Penn (founder/director) | The Digs Chicago · nonprofit community studio (~100 artists) | US | 2025-05-15 | https://thepotterscast.com/1132/ | MONEY (rent) | — | V |
| Q32 | "We definitely were scraping by pretty brutally for the first year but we have never not covered the cost." / "We did not receive grants for probably two years after we opened." | same | same | US | 2025-05-15 | same | MONEY (cash flow, grants) | — | V |
| Q33 | "Anyone in a studio management position is going to have a hard time doing their own practice." / "I don't have much time. I don't have much time at all." / "I have to really fight for the time that I have in the studio" / "It is always a struggle." | same | same | US | 2025-05-15 | same | BURN (admin vs own practice) | [HRS] | V |
| Q34 | "The banks did not help us out at all." | Courtney Brewer (co-founder) | The Clay Warehouse (theclaywarehouse.ca; BC per handle) · teaching/membership studio + supply shop | CA | 2024-01-23 | http://thepotterscast.com/inside-look-at-running-a-community-studio-courtney-brewer-scott-pierce-episode-997/ | MONEY (financing) | — | V |
| Q35 | "It was difficult." / tracking "where our money goes, whether it's inventory or into development of the studio" / "finding the right balance between selling a bunch of supplies and materials and also maintaining the studio." / "I think it does become difficult to grow but you know, it's a lot of lost sleep but we are making it happen." | Scott Pierce (co-founder) | same | CA | 2024-01-23 | same | BOOKS, INV, MONEY, BURN | [BURN] "lost sleep" | V |
| Q36 | "It was more of the admin stuff like the automations and the bookings and taking payments and everything." / "Just saving yourself time so that renting your kiln out doesn't become basically a time suck that makes it not profitable." / "…but they don't want to lose all of that time emailing back and forth." | Amy Noel (potter renting out her kiln; built and sells a kiln-rental app: **vendor interest**) | Wabi Scotia Pottery, Nova Scotia · production pottery / kiln rental | CA | 2026-08-18 | https://thepotterscast.com/1255/ | KILN, SOFT, COMMS, MONEY (payments) | [HRS] | V |
| Q37 | "So you have to pay for Replit for a monthly fee." / "And then we have a huge emailing system as well, which is a different provider, but we've hooked that up." / "So we have to pay for that monthly." / "And then all of your blood, sweat and tears on top of that, your time, which is the biggest thing." | same | same | CA | 2026-08-18 | same | SOFT (cost, stitching providers) | [$] | V |
| Q38 | "I think I've made enough, I'd say once I take away my fees for, you know, like kiln repairs and electricity," / "…I think I've made enough to almost cover one month of income for the year." | same | same | CA | 2026-08-18 | same | MONEY, EQUIP | — | V |
| Q39 | "Oh, there's endless tasks in the studio from Everything from cleaning, recycling, sponging, kiln packing, waxing, glazing" / "all of those menial tasks can absolutely be outsourced." / "bringing someone on is very close, but it's not at the forefront of my mind currently" | Grace Chaplin (owner) | Muckware Ceramics, Byron Bay hinterland NSW · production pottery | AU | 2026-09-10 | https://thepotterscast.com/1262/ | STAFF (what to hire for); tasks named: cleaning, kiln packing (Time section) | — | V |
| Q40 | "that's the piece of owning a business that's been surprising" / "thinking about having future employees, I am going to have to be the one overseeing things" / "that is a big learning curve for me" | Jordyn Brummett (co-owner) | The Vessel, Huntsville AL · community studio | US | 2022-06-23 (pre-2023) | http://thepotterscast.com/she-is-starting-a-community-clay-studio-jordyn-brummett-episode-845/ | STAFF | — | V |

### Batch 4: hired managers and technicians (The Potters Cast show notes)

| ID | Quote | Speaker (role) | Studio · segment | Ctry | Date | URL | Codes | Sev. | St |
|---|---|---|---|---|---|---|---|---|---|
| Q41 | "Financial failure." / "Not being able to pay the bills, keep up with the bookkeeping, all of that." / "I haven't had to manage that side of the business yet so I have some trepidation about that." | Bennett Graves (hired manager since 2018, alongside owner Dan Vito) | Fireborn Studios, Pittsburgh PA · teaching/community studio | US | 2025-06-05 | https://thepotterscast.com/1138/ | BOOKS, MONEY | fear of closure | V |
| Q42 | "Yeah, look for people to join your team who can kind of fill in the gaps." / "If you struggle with bookkeeping or struggle with scheduling, things like that." / "I definitely have plenty of areas where I could use a little support." / "I want to make pots!" | same | same | US | 2025-06-05 | same | STAFF (what to hire for), BOOKS, SCHED, BURN | — | V |
| Q43 | "We mop almost every day in the studio." / "If you go to a place where it's messy and it's everywhere then people will leave it everywhere." / "Giving things a home is a way to keep things organized, especially in the community studio." | Yesha Panchal (hired ceramics studio manager) | Hudgens Center for Art, Duluth GA · nonprofit art centre | US | 2025-01-30 | https://thepotterscast.com/1102/ | CLEAN, OUTPUTS (daily routine) | — | V |
| Q44 | "We have a routine, at the end of every session we mark things down and move things to an orphan shelf." / "Things will stay on the shelf for one more session and then we move it to the dumpster or recycle it" | same | same | US | 2025-01-30 | same | PIECE (unclaimed work), OUTPUTS | — | V |
| Q45 | "I close the doors because my studio is in the art center where I work so it is really hard to keep that time separate" | same | same | US | 2025-01-30 | same | BURN (job vs own practice) | — | V |
| Q46 | "I think my studio practice is definitely suffering a little bit from working the 9 to 5" / on a kiln he has designed and priced: "I just need the A-OK to buy all of the materials and build it." | Carter Pasma (studio manager/technician) | Weber State University, Ogden UT · university | US | 2026-06-02 | https://thepotterscast.com/1233/ | BURN, EQUIP (purchase approval) | — | V |

### Batch 5

| ID | Quote | Speaker (role) | Studio · segment | Ctry | Date | URL | Codes | Sev. | St |
|---|---|---|---|---|---|---|---|---|---|
| Q47 | "Unfortunately, the porcelain was not in stock, and there is a delay in shipment due to the weather." / "Queen City Clay told me they would let me know as soon as it comes back in stock." / "Supply is running a bit low on B-Mix, but I will be picking up more soon!" | AJ Phirman (hired studio manager; same source as Q01–Q06) | Fitton Center, Hamilton OH · nonprofit art centre | US | 2025-01-21 | https://www.fittoncenter.org/grouppage/pottery-studio/discussion/df72b03c-3a3b-46fd-adb5-313f33876610 | INV (stock-outs, reordering), COMMS | — | V |
| Q48 | "Due to recent supply chain shortages, orders over 100 lbs may take two+ weeks to process." / "Clay Studio Membership is currently full, and we are no longer taking names for the waiting list." | organisation (studio web page) | LHUCA Clay Studio, Lubbock TX · nonprofit art centre (mixes and sells clay) | US | page © 2026 (accessed 2026-10-20) | https://lhuca.org/clay | INV (supply), CAP | — | V |
| Q49 | "So we went for almost a year without charging people taxes." / "They didn't tell me that the website didn't include them." / "I didn't want to ruin my business over taxes." | Gabriela Angulo (owner, two studios) — **vendor marketing (QuickBooks customer story)** | Clay With Me, Toronto · teaching studio (workshops, camps, private events) | CA | 2025-01-10 | https://quickbooks.intuit.com/r/running-a-business/i-run-this-clay-with-me/ | BOOKS (sales tax), SOFT (website checkout) | [$] tax exposure | V |
| Q50 | "We cleaned everything up with QuickBooks." / "Every time I buy something, I take a picture of the receipt," / "QuickBooks has all the sales and reports, and every two or three months, we get together and we study our numbers." | same — **vendor marketing** | same | CA | 2025-01-10 | same | BOOKS (QuickBooks in use; positive) | — | V |
| Q51 | "At one point, during Covid, I was answering the phone, answering emails, trying to do it all." / "But I couldn't do it all." / "I was losing a lot of people because I wasn't answering the phone." / "I was so overwhelmed." | same — **vendor marketing** | same | CA | 2025-01-10 (refers to Covid period) | same | BURN, COMMS | [MEM] customers lost, [BURN] | V |
| Q52 | "one time I accidentally fused two classes from different locations into one," / "I ended up having 20 people come to the same studio, and there's only 10 wheels." / "The reviews were killing me; people were very upset." | same — **vendor marketing** | same | CA | 2025-01-10 | same | SCHED (multi-site booking error) | reputation | V |
| Q53 | "I made mistakes for many, many months until I said 'I need help,'" / "I said, 'I need a person who can organize this program.'" / "I hired a marketer to design some flyers and social media posts, and to do some local marketing so people could find us." / "I paid another person to create a new website." | same — **vendor marketing** | same | CA | 2025-01-10 | same | STAFF (what was hired out), MKT | — | V |
| Q54 | Athabasca Pottery Club paused lessons because an influx of members strained space and clay; the club makes its own clay (about two weeks a batch); the board limited lessons "until the supplies can be built back up" | board (co-op/club), via Town & Country Today | Athabasca Pottery Club, Alberta · cooperative/club | CA | undated | https://www.townandcountrytoday.com/entertainment-news/pottery-club-experiencing-growing-pains-1872482 | INV, CAP, NOMGR | lessons paused | **U** (403; search snippet) |

### Batch 6: owner newsletters (Substack) and Ceramics Monthly

Note: right after the Substack search behind this batch, the session's WebSearch budget ran out ("limit: 200 WebSearch calls shared by every agent in this session"). From then on, discovery mostly followed links from pages already opened; 3 later searches succeeded (see Gaps and limits).

| ID | Quote | Speaker (role) | Studio · segment | Ctry | Date | URL | Codes | Sev. | St |
|---|---|---|---|---|---|---|---|---|---|
| Q55 | "When I first opened the studio, I was in survival mode, and didn't establish or hold up systems." / "As we've grown, some things have gotten harder to manage." / "We are trying to tighten up on some of our policies." | Alexa (owner), in the studio newsletter | Alley Cat Ceramics, Indianapolis IN · membership/community studio | US | 2026-09-30 | https://alleycatceramics.substack.com/p/september-2026 | BURN (admin, no systems), CLEAN | — | V |
| Q56 | "Full access + storage members (including monitors) must have all supplies on their shelf." / "This is to help with cleaning and maintaining the space as well as keeping walkways clear." / "The lease states members will be fined for leaving items outside of allowed space," / "and we will start enforcing this in October." / "please do not touch items on member and monitor shelving unless you have been given permission to do so." | same | same | US | 2026-09-30 | same | PIECE (shelf space), CLEAN (shared-space behaviour) | fines | V |
| Q57 | "We have a new Studio Monitor. Please, everyone welcome Mik to our space." / "Nick has recently stepped down from his post as monitor for the last 4 years." | newsletter (Lizzie Bilbrey, editor) | same | US | 2026-09-30 | same | STAFF (monitor turnover) | — | V |
| Q58 | "Clay order form will be ready on the 1st, and everyone will get an email." / "Orders due by October 5." / "I'd like to place the order by October 6th!" | same | same | US | 2026-09-30 | same | INV (group clay order), OUTPUTS (supply order) | — | V |
| Q59 | Newsletter origin: "she was inspired to trade someone membership to start a monthly newsletter for Alley Cat" / "At Alley Cat, there is a brown notebook hanging on the cork board near the bathroom with a pen." | newsletter (Lizzie Bilbrey) | same | US | 2026-05-29 | https://alleycatceramics.substack.com/p/may-2026 | STAFF (work exchange), COMMS (paper notebook), OUTPUTS (newsletter) | — | V |
| Q60 | "Gomez is the small kiln and he is currently incapacitated, but talks for a grand reopening this June are in the works." / "Our big girl Gertie (the large kiln) was found abandoned in a dumpster in 2019." / "She has had one reconstructive computer surgery, but remains a powerhouse." | same | same | US | 2026-05-29 | same | EQUIP (kiln down, used kilns) | capacity lost | V |
| Q61 | "The pottery business has always operated on thin margins." / "Clay suppliers raise prices." / "Propane costs fluctuate." / "Shipping gets expensive." | Alford Wayman (owner; works a factory job alongside) | Creek Road Pottery, rural PA · production pottery (also classes) | US | 2026-03-23 | https://thepotterydailies.substack.com/p/taking-stock | MONEY, INV (material costs) | — | V |
| Q62 | "What I found surprising was how much I already had that I'd forgotten about." / "Materials stored in corners." / "Tools I bought years ago and never used." / "I'll start using some of those materials I found in corners instead of ordering new ones just because it's easier." | same | same | US | 2026-03-23 | same | INV (stock visibility, over-ordering) | — | V |
| Q63 | "I can't make them if I'm so exhausted from trying to do everything myself that I never actually sit at the wheel." | same | same | US | 2026-03-23 | same | BURN | [BURN] | V |
| Q64 | "I've written about kilns that won't fire and pots that crack and panic at tax time when I realize I'm 51…" / "They'll pay for clay and propane and kiln repairs…" / "Right now the numbers don't work." | same | same | US | 2026-04-27 | https://thepotterydailies.substack.com/p/im-asking-you-to-pay-for-this | EQUIP, BOOKS (tax time), MONEY | — | V |
| Q65 | "Pressure from our landlord and unsafe working conditions had us questioning" / "Plus, anyone who owns a studio business has the realization that if you don't own the land, you don't own much." / "Construction costs went well over our estimates" / "if not for an SBA loan and the help of over 70 volunteers, we would have failed." | Ben Clark and Andrew Osterburg (owners), first-person article | Queen City Clay, Cincinnati OH · teaching/membership studio (37 rental spaces, PYOP studio) | US | 2022-02 (pre-2023) | https://ceramicartsnetwork.org/ceramics-monthly/ceramics-monthly-article/clay-culture-moving-queen-city-clay | MONEY (rent/landlord, capex), STAFF (volunteers) | near-failure | V |
| Q66 | "Getting a commercial loan is tough as a small businessperson." / Joe on "being overwhelmed with the back and forth between both studios and staying sane and positive." / Hayley on "their nervousness relating to what people will and won't do as far as maintaining the studio." | Rain City Clay founders (Joe, Hayley; via writer) | Rain City Clay, Seattle WA · teaching/membership studio | US | 2022-06/08 (pre-2023) | https://ceramicartsnetwork.org/ceramics-monthly/ceramics-monthly-article/clay-culture-rain-city-clay | MONEY (financing), BURN, CLEAN (members maintaining studio) | — | V (partly writer's framing) |

### Batch 7: one hired manager's member board, read end to end (Fitton Center), plus equipment and co-op sources

**Fitton Center "Pottery Studio" group, 20 consecutive posts by the studio manager, 2025-08-08 to 2026-06-19** (pages 1–5 of https://fittoncenter.org/grouppage/pottery-studio/discussion). Topic tally (a post can carry more than one topic):

| Topic | Posts (of 20) | Dates |
|---|---|---|
| Schedule changes, closures, open-studio hours | 8 | 2026-06-19, 06-02, 05-19, 03-31, 01-13; 2025-12-16, 12-12, 08-08 |
| Supplies: restock, stock-out, discontinued, order placed | 7 | 2026-05-05, 03-31, 03-17, 02-27, 02-03; 2025-12-05, 09-16 |
| Firing status or delay | 4 | 2025-12-05, 10-07, 09-16, 08-08 |
| Member sales event (sign-ups) | 3 | 2025-10-21, 10-17, 09-02 |
| Manager absent, who covers | 2 | 2026-01-13, 2025-10-07 |
| Image only | 1 | 2025-09-09 |

Of the 7 supply posts, 4 report a stock-out, regional shortage, discontinued line or low stock (2026-03-31, 03-17, 02-03; 2025-09-16). An earlier stock-out post (2025-01-21, Q47) falls outside the 20-post window.

| ID | Quote | Speaker (role) | Studio · segment | Ctry | Date | URL | Codes | Sev. | St |
|---|---|---|---|---|---|---|---|---|---|
| Q67 | "Just a heads up that 112 speckled brown clay and bmix without grog are still out of stock at our supplier." / "In the meantime, I will see if there are any other local suppliers that have them in stock." / "I'm going to get a few boxes of Laguna 608 to hold us over until we can get 112 back in stock." | AJ Phirman (hired studio manager) | Fitton Center · nonprofit art centre | US | 2026-03-31 | https://www.fittoncenter.org/grouppage/pottery-studio/discussion (page 2) | INV (stock-out, substitute sourcing) | — | V |
| Q68 | "We only have one more bag of BMIX without grog in stock." / "Unfortunately, there is a shortage of the BMIX without grog in our region." | same | same | US | 2026-02-03 | same (page 3) | INV | — | V |
| Q69 | "I just wanted to give you a heads up that there are few glazes in the studio that have been discontinued" / "So, when they are gone, they are gone." | same | same | US | 2026-03-17 | same (page 2) | INV (glaze lines discontinued) | — | V |
| Q70 | "It seem like this tool is disappearing for unknown reason." / "If we do not know where they went, could we order some?" | Haruko Mizoguchi (instructor; covers kilns when the manager is out) | same | US | 2026-02-28 | same (page 2, comment) | INV (tool loss/shrinkage) | — | V |
| Q71 | "The combination of holidays (aka days the Fitton is closed) and increased volume coming through for firing," / "it is taking me some time to catch up!" / "I promise, I am running all kilns as full and often as I can." / "Also, I've recently placed a supply order." | AJ Phirman | same | US | 2025-12-05 | same (page 4) | KILN (seasonal backlog), INV, COMMS | delay | V |
| Q72 | "I had to leave early on Friday because I forgot some important diabetes medication." / "So, I was not able to load the kilns." / "I will be firing all three kilns today to try to get caught up!" | same | same | US | 2025-10-07 | same (page 4) | STAFF (single point of failure), KILN | delay | V |
| Q73 | "Noah is going to cover Studio+ for the next two Friday's and Haruko will be firing kilns while I am out." | same | same | US | 2026-01-13 | same (page 3) | STAFF (coverage), SCHED | — | V |
| Q74 | "We are running low on 112 speckled brown and bmix. I will be picking more up on Friday morning!" / "I did not fire kilns last Friday, because we didn't have enough to fill the kilns." | same | same | US | 2025-09-16 | same (page 5) | INV, KILN (waits for full loads) | — | V |
| Q75 | Holiday sale: "Sales will be completed and processed by the Fitton Center at a central "check-out" booth." | same | same | US | 2025-10-21 | same (page 4) | SALES (central checkout for member work) | — | V |
| Q76 | "The kiln panel malfunctioned and was completely destroyed." / "Now approximately $6,500 is required to cover the cost of a new panel" / "The kiln fires work for approximately 16 studio members ranging from hobbyists to professional ceramicists" / "who depend on the kiln services for their livelihoods" | Bettina Westwood (studio colleague; GoFundMe organiser for kiln owners Loren Kaplan and Darryl Barnes) | Akin Dupont Studios shared kiln, Toronto · shared/membership studio | CA | 2024-08-07 | https://www.gofundme.com/f/Lorens-kiln | EQUIP (controller failure) | [$] ~CA$6,500 | V |
| Q77 | Members "charged on July 1 for classes or memberships they could no longer use" (Hoodline paraphrase of members' Reddit posts); "GoFundMe campaigns have been launched for some instructors who lost shifts or pay" | members and instructors [MEMBER-MGMT], via Hoodline | Clay by the Bay, San Francisco | US | 2026-07-23 | https://hoodline.com/2026/07/clay-by-the-bay-kicked-to-the-curb-as-fisherman-s-wharf-pottery-hub-shuts-overnight-6730227/ | MONEY (billing after closure), STAFF | CLOSE | V (paraphrase; no direct quotes) |
| Q78 | Twelve volunteer work groups, e.g. Firing: "Work group members will work with the kiln chairs to maintain and fire studio kilns." / Glaze: "Work group members can assist with monitoring glazes – levels, proper viscosity, cleanliness of buckets." / Clay Distribution: "…help clay issuers the week prior to sessions starting to issue clay for each session." | guild (co-op organisers' page) | Hawaii Potters' Guild, Honolulu · cooperative/guild | US | undated (accessed 2026-10-20) | https://www.hawaiipottersguild.org/work-groups | NOMGR (chores by work group), KILN, INV, EQUIP | — | V (structure, not a complaint) |

Vendor framing, kept apart and not counted: Kiln Fire blog "The Awkward Money Conversations Nobody Warns You About When You Open a Pottery Studio" (2026-09-18) lists failed membership payments, members owing small firing balances ("Months later you notice that the member still owes $83.50 and has to ask them to pay it.") and unpaid private-event balances; "How Paint Your Own Pottery Studios Can Solve the Unclaimed Work Problem" (2026-03-10): "Storage space fills up quickly", "Staff have to spend time searching for pieces". Neither quotes a named studio. https://kilnfire.com/blog/how-to-have-awkward-money-conversations-with-customers · https://kilnfire.com/blog/how-pyop-studios-can-solve-unclaimed-problem

### Batch 8

| ID | Quote | Speaker (role) | Studio · segment | Ctry | Date | URL | Codes | Sev. | St |
|---|---|---|---|---|---|---|---|---|---|
| Q79 | "Prices for all classes have increased to combat the rising rent price and costs of goods." / "Those who have been with us for a while know our battle with the kilns and our HVAC." / "This issue is now a thing of the past!" | owner (class listing text) | Delores Pottery & Studios (location not stated; EDT times; a search snippet placed it in Virginia) · teaching studio + studio rentals | US | 2025-09 (session start 2025-09-10) | https://site.corsizio.com/event/68911d315b96ec183ba87607 | MONEY (rent, costs, pricing), EQUIP (kilns, HVAC) | relocation | V |
| Q80 | "The whole energy system is just broken," / "It's not just the cost of the energy, it's also the cost of all our suppliers… Every single thing has gone up" / "You get to the point where you can perhaps put 50p on to mitigate that increase." / "But you soon get to the top when your product becomes unaffordable." (energy bills "from around £900 to up to £2,500 a month" is the writer's paraphrase) | Jonathan Plant (co-founder) | Moorland Pottery, Stoke-on-Trent · production pottery | UK | 2025-01-25 | https://www.spectator.com.au/?p=10853612 | MONEY (energy, pricing ceiling), INV (supplier costs) | [$] | V |
| Q81 | "Keep track of fired goods throughout the studio, rotating them onto the Rack of Doom" / "discarding uncollected wares after 60 days." / "The studio uses lots of paper forms to track things like clay inventory, firing, orientations etc." / "The supply of forms should be checked daily and more forms should be printed if necessary." / "Ring up customer sales (clay sales, firing fees, day-passes, etc.) on the register (POS)." / "Online or phone ordering of clay, supplies and equipment when needed." / "The Studio Tech is not responsible for repairing kilns and other equipment." | employer (job posting for Studio Tech) | The Potters' Studio, Berkeley CA · nonprofit membership studio | US | 2022-02-28 (pre-2023) | https://www.ceramicsnow.org/jobs/ceramics-studio-technician-at-the-potters-studio | PIECE, INV (paper tracking, ordering), SALES (POS), OUTPUTS, STAFF | — | V |
| Q82 | "Students and members can ask a lot of questions about clay, glazes, firing, studio protocols, etc." | same | same | US | 2022-02-28 (pre-2023) | same | COMMS | — | V |
| Q83 | Canterbury Potters Association: "Our Tutors all have full classes for Term 3 with large waiting lists." / "We are reassessing our clays as some will not vitrify at Cone 6…" ; Hakatere Ceramics and Pottery Inc: "We, like the rest of NZ, have been inundated with requests for pottery lessons." / "Alert level 4 has thrown us in chaos again" | club organisers (regional report) | Canterbury clubs, NZ · cooperative/club | NZ | 2021-08 (pre-2023) | https://ceramicsnz.org/news/canterbury-west-coast-august-2021/ | CAP (full classes, waiting lists), INV (clay suitability); club-run | — | V |

**What studios hire managers and technicians to do (job postings; employer voice, not complaints).** Counted for the "Outputs" and "hired to do" questions only.

| Posting | Duties named (verbatim excerpts) | Date | URL |
|---|---|---|---|
| American Museum of Ceramic Art, Pomona CA: Ceramics Studio Manager, $62,000, 40 h/wk + evenings/weekends · nonprofit art centre | "Manage inventory needs to ensure seamless studio operations" · "Coordinate scheduling, loading, and unloading of gas and electric kilns; production of studio glazes;" · "Monitor, prioritize, and implement studio repairs and maintenance" · "Maintain administrative systems: calendar, POS program, timesheets, contracts," "class registration, attendance tracking, security, and kiln logs" · "Assist with studio budget development, expense tracking, and financial management" · "Update studio social media… (Facebook, Instagram, Linktree, Mailchimp)" · "Recruit and train staff" | 2025-04-28 | https://www.ceramicsnow.org/jobs/ceramics-studio-manager-at-the-american-museum-of-ceramic-art/ |
| Minot State University, ND: Ceramics Studio Manager, part-time · university | "This includes kiln firings, clay recycling, and general studio cleaning." · "order raw materials and supplies to support all courses." · "Work within a pre-determined operating budget." · "Orders and tracks clay and sculpture material usage." · "troubleshoot studio equipment" with "MSU Facilities and exterior vendors" · may hire "student workers to assist in the management of the ceramics studio" | 2026-08-11/15 | https://www.ceramicsnow.org/jobs/ceramics-studio-manager-at-minot-state-university/ |
| Flint Institute of Arts, MI: Art School Ceramics Programs Manager · nonprofit art school | "Equipment maintenance and improvement" · "Oversight of firing process" · "Instructor/technician training and mentoring" · "Firing and organization of student work" · "Recruitment of students and supporters" | 2025-11-17 | https://www.ceramicsnow.org/news/the-flint-institute-of-arts-is-hiring-an-art-school-ceramics-programs-manager/ |
| The Potters' Studio, Berkeley CA: Studio Tech, $18–20/h · nonprofit membership studio | see Q81–Q82 | 2022-02-28 (pre-2023) | https://www.ceramicsnow.org/jobs/ceramics-studio-technician-at-the-potters-studio |
| Phipps Center (Hudson WI), Creative Arts Workshop (New Haven CT), Claytek (Vancouver BC), a Waupaca WI studio | Search snippets only: Phipps "10 hours/week on average, fluctuating depending on studio needs and kiln schedule", duties include quarterly class schedules and maintaining inventory; CAW lists equipment repair and inventory; Claytek a 40 h blended technician/manager role at $22–30/h; Waupaca 10 h/wk at $20/h. Posting pages now 404 | various | **U** |

### Batch 9

| ID | Quote | Speaker (role) | Studio · segment | Ctry | Date | URL | Codes | Sev. | St |
|---|---|---|---|---|---|---|---|---|---|
| Q84 | "I never thought keeping a full staff would be such a challenge." / "It's a constant process of hiring and rehiring because most of our employees work part-time." / "…before opening, we hadn't really thought about how continuously we would need to work on staffing." | Jain (franchise co-owner) | Color Me Mine franchise (location not given in the excerpt) · PYOP | US | 2026-10-13 | https://www.entrepreneur.com/buying-a-franchise/heres-how-these-two-immigrants-started-their-own-color-me-mine-franchise | STAFF (turnover, part-time) | — | V |
| Q85 | "We don't currently have a full-time manager, so we handle staffing ourselves." / "Sometimes we're both at the studio; sometimes we alternate days." / "There's also glazing, payroll, social media, the calendar and other online work." / "Every day, something needs attention." | Choudhary (franchise co-owner) | same | US | 2026-10-13 | same | NOMGR, BURN (admin load), STAFF, BOOKS (payroll), MKT, SCHED | [HRS] daily | V |
| Q86 | "Calling customers when something happens to their pottery in the kiln." / "Telling them that is hard." / "It happens very rarely — we estimate less than 1% of pieces — but we still dread those calls." / "We offer a replacement piece and waive the studio fee…" | Jain / Choudhary | same | US | 2026-10-13 | same | PIECE (kiln damage), COMMS, MONEY (comps) | [$] small | V |
| Q87 | "We felt if we kept going this way we are either going to burn out or grow broke" / "I think as far as the wholesale the passion was definitely dwindling for me" / "because I was missing the creative aspect of it and it was just becoming a job." | Neil Lawrence (co-owner) | Island Clay Works (per contact link), Vancouver Island BC · production pottery → café + gallery studio | CA | 2025-03-11 | https://thepotterscast.com/1113/ | BURN, MONEY | [BURN] pivot | V |
| Q88 | "I think probably the finances because when you take on such a huge enterprise" / "and you have a family and you have a home it really comes down to dollars and sense." | Anita Lawrence (co-owner) | same | CA | 2025-03-11 | same | MONEY | — | V |
| Q89 | "There's only so many hours in the day, and if you're creating a curriculum," / "making sure all of the you know, leg work to make the workshops a physical reality, and advertising… and all of that," / "you still have to have time at the end of the day to throw the pots and make sure they don't get too dry" | Andrew Sartorius (program director, hired) | The Oki Doki Studio, Germantown NY · teaching studio (atmospheric firing workshops; 97 participants last year) | US | 2026-04-28 | https://thepotterscast.com/1223/ | BURN (admin vs own work), MKT, SCHED | [HRS] | V |
| Q90 | "We fire kilns constantly every day and, unfortunately, we don't have the capacity to rent them out at the moment." / "Firings run to a schedule and can't be brought forward for one person's deadline." / "Hours don't carry over, because that would make the system less stable and create congestion in the studio." | organisation (FAQ) | Turning Earth, London · membership/community studio | UK | undated (accessed 2026-10-20) | https://www.turningearth.org/faq | KILN (capacity), CAP, SCHED (capacity rules) | — | V |
| Q91 | "Technicians do the work that keeps the studio going" / "What they don't do is teach: when they're on duty they need to be available to everyone." / "We keep the price as low as we can for everyone by sharing the costs evenly" | same | same | UK | undated (accessed 2026-10-20) | same | STAFF (technician role), MONEY | — | V |
| Q92 | Spaces are kept clean to reduce "the risk to makers' lungs from air-borne silica." | organisation (home page) | same | UK | undated (accessed 2026-10-20) | https://www.turningearth.org/ | SAFE (silica), CLEAN | — | V |
| Q93 | "Canberra Potters has a number of areas in which we require volunteers." / volunteers "participate in CPS-organised activities such as our quarterly cleaning bees" / at events, handle "cash and card transactions" | society (volunteer page) | Canberra Potters Society, ACT · nonprofit society (community studio, gallery) | AU | undated (accessed 2026-10-20) | https://canberrapotters.com.au/volunteer/ | STAFF (volunteers), CLEAN, SALES | — | V (structure, not a complaint) |

Context, not counted: Oregon Potters Association president Noah W. Smith (The Potters Cast 1226, 2026-05-07) on a volunteer "point system… set up to give back to people" "that have invested a lot of their time into the organization" — an association, not a studio. Twisted Clay Studio (1195), The Tilted Kiln (1248), Clay Works of Vancouver (1256, 1270), The Goods Pottery (1260), Historic Yellow Springs and Workhouse Arts (Tales of a Red Clay Rambler 576, 570): show notes had no operational content. All Fired Up, Lynchburg VA (PYOP, closing 2026-08-28): owner cites marriage and a move, no operational reasons.

Context, not counted as problem quotes: The Kiln Rooms new Farringdon studio (Islington Tribune, 2024-01-26): founders want to make ceramics more accessible after school art cuts; no operational problems quoted. Dimboola Pottery, VIC (Wimmera Mail-Times/Warracknabeal Herald, 2025-06-10): owner George Khut, "There's not a lot of opportunities in regional Victoria for pottery classes,". Ceramics NZ regional updates for Aug 2026 (Otago, Wellington region, Central North Island) contained event news only.

Context, not counted as problem quotes: Swansea Pottery Collective (UK, 2026-05-18, volunteer-run not-for-profit, £50/month including materials and firing; founders could not continue after an evening course "without a kiln, tools, studio space" — reporter's narration) https://swanseabaynews.com/swansea-tv-pottery-stars-kaz-and-angharad-to-cut-the-ribbon-at-new-volunteer-run-studio-opening-this-saturday/ . 1280 Gallery, Brunswick AU (Substack, 2022-04-23): only narrator sentences about rent and demand, no owner quote. Cone 10 Charleston article is from 2010 (too old). Pittsburgh City Paper (Clay Pittsburgh) and SeekBusiness AU listing returned 403.

Not usable: The Maker's Playbook ep. 616 (Amy Roadman, 2025-04-11) and Wheel Talk ep. 230 (Noah Smith, 2023-12-23) have show notes only, no transcript; the notes are the host's words (burnout, "overworking") and are not counted. NVRC "2025 pottery studio changes" factsheet PDF would not decode.

### Batch 10

| ID | Quote | Speaker (role) | Studio · segment | Ctry | Date | URL | Codes | Sev. | St |
|---|---|---|---|---|---|---|---|---|---|
| Q94 | "Keep a kiln log." / "If your computer does not keep a kiln log for you, keep a clipboard." / "So that you know how many firing you have and change the thermocouple on a regular basis." / "If your firing times are getting long they are getting worn." / "Heating elements, relay, thermocouples at the same time." | Carl Mankert (kiln technician; his company sells and services studio equipment: **service vendor**) | Chicago Kiln Service, Chicago · serves studios and schools | US | 2025-11-13 | https://thepotterscast.com/1178/ | EQUIP (maintenance timed by firing count) | — | V |
| Q95 | Smallwood on the founding co-op: 'Dude, we can't afford all this stuff; let's go in together,' / writer: "has grown into a fully operational 501(c)(3) with 34 studio members," / "a packed class schedule and a waiting list that stretches 12 to 16 months." | Meg Smallwood (studio director) and the writer | John Bryan Community Pottery | US | 2026-07-10 | https://ysnews.com/news/2026/07/john-bryan-community-pottery-wheels-into-new-era | CAP (waitlist; writer's narration), MONEY | — | V |
| Q96 | "The aforementioned green glaze I now mix with one percent less copper." / "Adhering to the standard that is strictest in the world." | Gabriel Kline (director) | Odyssey ClayWorks, Asheville NC · community studio | US | 2025-11-04 | https://thepotterscast.com/1175/ | SAFE (food-safe studio glazes): practice, not a complaint | — | V (context) |
| Q97 | Committee minutes: "Signage was put up because there was a situation where a member was picking up other folks pieces regardless of the signs." / "There are those that believe our signs are not inviting for new members" (2022-10-04); January 2026 agenda names a kiln team lead, a "Shelf Cleaning Lead" and a purchasing lead | volunteer ceramics committee | Dallas Makerspace, TX · makerspace | US | 2022-10-04 (pre-2023); 2026-01 | https://dallasmakerspace.org/wiki/Ceramics_Committee_Meeting_20221004 · https://talk.dallasmakerspace.org/t/ceramics-january-2026-committee-meeting/123373 | PIECE, CLEAN, NOMGR | — | **U** (403; search snippet) |

---

# Analysis

## Totals

- **97 coded entries (Q01–Q97).** 90 are verified verbatim (each holds 1 to 7 quoted sentences), 4 are reporter paraphrase (Q09, Q10, Q11, Q77, marked "r") and 3 are UNVERIFIED snippets (Q21, Q54, Q97). Of the 90 verified, 3 describe structure or practice rather than a problem (Q78, Q93, Q96).
- **34 independent verified sources carry at least one problem statement** (register below), from **at least 40 named or identifiable people**. Another 3 are structure or context only (Hawaii Potters' Guild, Canberra Potters Society, Odyssey ClayWorks), plus 3 job postings and 3 unverified sources.
- **By date:** 27 of the 34 are dated 2023–26, 5 are pre-2023 (S04, S16, S24, S25, S29) and 2 are undated (S05, S32).
- **By country:** US 23, Canada 5, UK 3, Australia 1, New Zealand 1, not stated 1.
- **By segment:** membership/community 10, teaching 9, nonprofit art centre 5, production 5, PYOP 2, university 1, cooperative/club 1, kiln-service technician 1. Makerspace 0 verified (only U3) and municipal 0.
- **By lead role:** owners or founders lead 21 sources. Hired managers, directors or program directors lead 6 (S01, S02, S05, S17, S18, S31), plus the S03 ex-manager. Technicians lead 2 (S19, S34), plus S06's technician. Co-op/club organisers lead 2 (S26, S29). Unsigned organisational voice covers 3 (S08, S20, S32). Member voices that reveal a management failure (S03, S06) are tagged [MEMBER-MGMT].
- **By source type:** podcast show notes 14 (all The Potters Cast); news and magazines 9; studios' own pages, boards and newsletters 9; vendor-hosted 1 (S21); crowdfunding 1 (S26).

## Source register (audit trail for every count)

One source is one studio or organisation. Several people from the same studio count once.

| S | Source | Segment | Ctry | Year | Speaker roles | Quote IDs |
|---|---|---|---|---|---|---|
| S01 | Fitton Center for Creative Arts, Hamilton OH | nonprofit art centre | US | 2025–26 | hired studio manager; instructor | Q01–Q06, Q47, Q67–Q75 |
| S02 | John Bryan Community Pottery, Yellow Springs OH | nonprofit art centre (community pottery) | US | 2026 | hired studio director | Q07, Q08, Q95 |
| S03 | Clay by the Bay, San Francisco | membership/community | US | 2026 | owner (r), ex-manager (r), members (m, r) | Q09–Q11, Q77 |
| S04 | KClayton Studio, Overland Park KS | teaching (crafts) | US | 2022 | owner | Q12 |
| S05 | Pottery Northwest, Seattle | nonprofit art centre | US | year not shown | executive director | Q13 |
| S06 | Ilium Ceramics, Ilium CO | membership/community (member-run) | US | 2024 | owner-operator; technician; member (m) | Q14–Q16 |
| S07 | Chessell Pottery, Isle of Wight | PYOP/café | UK | 2023 | owner | Q17–Q18 |
| S08 | The Potters' Studio, Berkeley CA | membership/community (nonprofit) | US | c.2025; 2022 job ad | management (unsigned); employer | Q19–Q20, Q81–Q82 |
| S09 | Spiffy Pottery Studio, Menlo Park CA | membership/community | US | 2026 | owner | Q22–Q25 |
| S10 | Jacques' Pottery Shop, Welsh LA | teaching | US | 2026 | owner | Q26–Q27 |
| S11 | Jean Ceramics (location not stated) | teaching + retail | — | 2025 | owner | Q28–Q30 |
| S12 | The Digs Chicago | membership/community (nonprofit) | US | 2025 | founder/director | Q31–Q33 |
| S13 | The Clay Warehouse, BC | membership/community + supply shop | CA | 2024 | co-founders | Q34–Q35 |
| S14 | Wabi Scotia Pottery, Nova Scotia | production / kiln rental | CA | 2026 | owner (also sells an app) | Q36–Q38 |
| S15 | Muckware Ceramics, NSW | production | AU | 2026 | owner | Q39 |
| S16 | The Vessel, Huntsville AL | membership/community | US | 2022 | co-owner | Q40 |
| S17 | Fireborn Studios, Pittsburgh PA | teaching | US | 2025 | hired manager | Q41–Q42 |
| S18 | Hudgens Center for Art, Duluth GA | nonprofit art centre | US | 2025 | hired studio manager | Q43–Q45 |
| S19 | Weber State University, Ogden UT | university | US | 2026 | studio manager/technician | Q46 |
| S20 | LHUCA Clay Studio, Lubbock TX | nonprofit art centre | US | 2026 | organisation | Q48 |
| S21 | Clay With Me, Toronto | teaching | CA | 2025 | owner (vendor-hosted story) | Q49–Q53 |
| S22 | Alley Cat Ceramics, Indianapolis | membership/community | US | 2026 | owner; newsletter editor on work trade | Q55–Q60 |
| S23 | Creek Road Pottery, rural PA | production | US | 2026 | owner | Q61–Q64 |
| S24 | Queen City Clay, Cincinnati | teaching | US | 2022 | owners | Q65 |
| S25 | Rain City Clay, Seattle | teaching | US | 2022 | founders (partly writer) | Q66 |
| S26 | Akin Dupont Studios shared kiln, Toronto | membership/community (shared) | CA | 2024 | studio colleague, crowdfund organiser | Q76 |
| S27 | Delores Pottery & Studios (location not stated) | teaching + studio rentals | US | 2025 | owner | Q79 |
| S28 | Moorland Pottery, Stoke-on-Trent | production | UK | 2025 | co-founder | Q80 |
| S29 | Canterbury Potters Association; Hakatere Ceramics and Pottery Inc | cooperative/club | NZ | 2021 | club organisers | Q83 |
| S30 | Island Clay Works, Vancouver Island BC | production → café/gallery | CA | 2025 | co-owners | Q87–Q88 |
| S31 | The Oki Doki Studio, Germantown NY | teaching (workshops) | US | 2026 | program director | Q89 |
| S32 | Turning Earth, London | membership/community | UK | undated | organisation (FAQ) | Q90–Q92 |
| S33 | Color Me Mine franchise (location not in excerpt) | PYOP | US | 2026 | co-owners | Q84–Q86 |
| S34 | Chicago Kiln Service | kiln service (studios, schools) | US | 2025 | technician (service vendor) | Q94 |
| — | Hawaii Potters' Guild (structure) · Canberra Potters Society (structure) · Odyssey ClayWorks (context) | guild · society · community | US · AU · US | undated · undated · 2025 | organisers · society · director | Q78 · Q93 · Q96 |
| U1–U3 | Wenlock Pottery · Athabasca Pottery Club · Dallas Makerspace | production · club · makerspace | UK · CA · US | 2023 · undated · 2022/26 | owners · board · committee | Q21 · Q54 · Q97 |

## Category ranking (problem statements by independent source)

**Rule.** A source counts toward a category when at least one of its quotes describes something in that category as hard, slow, costly, broken or failing. An incident counts, such as a stock-out, a delay or a breakdown. A task that is only listed or performed (job duties, routine notices) goes under Outputs and is not counted here. Markers: (r) reported by a journalist; (m) member voice; (u) undated page.

| Rank | Code | Category | Sources (of 34) | Dated 2023–26 | Sources |
|---|---|---|---|---|---|
| 1 | MONEY | money: rent and leases, energy and inputs, pricing, cash flow, payments | **19** | 15 | S02, S03(r), S04, S05, S06, S07, S08, S09, S10, S12, S13, S14, S17, S23, S24, S25, S27, S28, S30 |
| 2 | BURN | owner burnout and admin load | **16** | 15 | S06, S09, S10, S11, S12, S13, S17, S18, S19, S21, S22, S23, S25, S30, S31, S33 |
| 3 | EQUIP | equipment breakdowns and maintenance | **10** | 10 | S01, S02, S09, S14, S19, S22, S23, S26, S27, S34 |
| 4 | STAFF | staffing, volunteers, work exchange, coverage | **9** | 7 | S01, S03(r), S15, S16, S17, S21, S22, S24, S33 |
| 5 | INV | inventory and supplies | **6** | 5 | S01, S13, S20, S23, S28, S29 |
| 6= | PIECE | members' pieces, unclaimed work, shelves | **5** | 4 (+S08 u) | S03(m,r), S08(u), S18, S22, S33 |
| 6= | SCHED | scheduling | **5** | 4 (+S32 u) | S01, S09, S17, S21, S32(u) |
| 6= | BOOKS | bookkeeping, accounting, taxes | **5** | 5 | S07, S13, S17, S21, S23 |
| 9= | COMMS | communication with members and customers | **4** | 4 | S09, S14, S21, S33 |
| 9= | CAP *(new)* | capacity and waitlists: demand exceeds kilns, wheels or space | **4** | 2 (+S32 u) | S02(r), S20, S29, S32(u) |
| 11= | KILN | kiln and firing logistics and backlog | **3** | 2 (+S32 u) | S01, S14, S32(u) |
| 11= | SOFT | software: platforms, stitched tools, cost | **3** | 3 | S11, S14, S21 |
| 11= | SAFE | compliance and safety | **3** | 1 (+S32 u) | S11 (insurance), S24 (unsafe building, 2022), S32 (silica) |
| 11= | CLEAN | cleanliness and shared-space behaviour | **3** | 2 | S18, S22, S25 |
| 15= | MKT | marketing and finding students | **2** | 2 | S21, S31 |
| 15= | NOMGR *(new)* | no manager: owner does everything, member-run, volunteers | **2** | 2 | S06, S33 (structure-only sources are in the No-manager section) |

Not counted: U1 Wenlock (MONEY, EQUIP), U2 Athabasca (INV, CAP, NOMGR), U3 Dallas Makerspace (PIECE, CLEAN, NOMGR).

**Inside MONEY (19).**
- Cash flow, financing or seasonality: 9 (S08, S09, S10, S12, S13, S17, S24, S25, S30).
- Rent, leases or landlords: 7 (S03 r, S04, S06, S09, S12, S24, S27).
- Energy and input costs: 5 (S02, S07, S14, S23, S28).
- Pricing and what the market will bear: 5 (S02, S05, S10, S27, S28).
- Collecting payments: 1 (S03, members billed after closure, r).
- Firing-fee fairness: 0.

**Inside BURN (16).**
- Running the studio crowds out the person's own making: 6 (S06, S12, S17, S18, S19, S31).
- Overwhelmed, exhausted or burnt out: 6 (S10, S13, S21, S23, S25, S30).
- Daily admin load with no let-up: 4 (S09, S11, S22, S33).

## Time: admin versus studio work

- **Hours per week: 0 of 34 sources give a number.** The only figures are in job postings:
  - AMOCA: 40 h/week plus evenings and weekends.
  - The Potters' Studio technician (2022): 16–32 h/week part-time.
  - Phipps Center: "10 hours/week on average" (U).
- **10 of 34 say admin displaces making or teaching:**
  - S06: "spending most of my time running the studio now, instead of actually doing ceramics!"
  - S09: "takes a lot of effort every day"
  - S11: "practically live here"
  - S12: "I don't have much time at all"
  - S17: "I want to make pots!"
  - S18: "really hard to keep that time separate"
  - S19: "my studio practice is definitely suffering"
  - S23: "never actually sit at the wheel"
  - S31: "only so many hours in the day"
  - S33: "Every day, something needs attention."
- **Tasks named as ones they want gone or have handed off (7 sources):**
  - email and phone questions; rescheduling and makeup classes; private-event set-up (S09)
  - emailing back and forth about kiln rental; bookings and taking payments, now automated (S14)
  - answering the phone and emails (S21)
  - bookkeeping and scheduling (S17)
  - cleaning, recycling, sponging, kiln packing, waxing and glazing (S15)
  - hiring and re-hiring, payroll, social media, "the calendar" and online work (S33)
  - curriculum logistics and advertising (S31)
  - not counted: routine maintenance handed to a community-college intern (WebFetch paraphrase of Ceramics Monthly "Staying Power", 2016; outside the window)
- **What they hired someone to do:**
  - S21: "a person who can organize this program", a marketer (flyers, social, local marketing) and a website builder; also has a manager.
  - S08: hired a Director of Studio Operations & Facilities and an inaugural Director of Finance & Administration (news listing). Its 2022 technician posting covers cleaning, glaze mixing, kiln loading, tracking fired work, the POS, paper forms and ordering, but "not responsible for repairing kilns".
  - S01: a studio manager; instructors cover kiln loading, supply inventory and cleaning when the manager is out.
  - S22: studio monitors, plus a member on work trade to write the newsletter.
  - S32: technicians who "do the work that keeps the studio going" and do not teach.
  - S33: part-time floor staff only, no full-time manager.
  - S15: considering a hire for menial production tasks.
  - S17: advises hiring to fill bookkeeping and scheduling gaps.
- **Job postings** (AMOCA, Minot State, Flint Institute of Arts; Batch 8): inventory, kiln scheduling, loading and logs, glaze production, repairs, POS, calendar, registration, attendance, budget and expense tracking, social media and Mailchimp, recruiting and training staff or student workers.

## Hypothesis topics: mention counts (any tone, verified sources)

| Topic | Problem mentions | Practice or neutral mentions | Positive | Dismissive | Notes |
|---|---|---|---|---|---|
| Inventory and supplies | 6 (S01, S13, S20, S23, S28, S29) | 5 (S22 monthly group clay order; S08 2022 paper forms and phone/online ordering; AMOCA "Manage inventory needs"; Minot "Orders and tracks clay"; Hawaii glaze-level and clay-distribution work groups) | 0 | **0** | Problems: supplier stock-outs, shortages or discontinued lines (S01: 4 posts on 2025-01-21, 2026-02-03, 03-17 and 03-31, plus a low-stock post on 2025-09-16; S20), price rises (S23, S28), forgotten stock and ordering "just because it's easier" (S23), money tied up in stock (S13), clay suitability (S29), tools going missing (S01 instructor). Better tracking would help S01 (running low), S23 and S13; it would not prevent supplier stock-outs |
| Equipment upkeep | 10 (S01, S02, S09, S14, S19, S22, S23, S26, S27, S34) | 5 (AMOCA repairs and kiln logs; Minot troubleshooting with vendors; Flint maintenance; Hawaii firing group maintains kilns; S08 tech "not responsible for repairing kilns") | 0 | **0** | Named failures: thermocouple (S01), kiln control panel ~CA$6,500 (S26), "kilns that won't fire" (S23), small kiln "incapacitated" (S22), "our battle with the kilns and our HVAC" (S27), wheel electrical layout (S09), equipment "coaxed for very long" and a wood kiln retired (S02), repairs eating kiln-rental income (S14) |
| Sales and payment systems | 2 (S21 website checkout left out sales tax; S03 members charged after closure, r) | 6 (S08 2022 POS register for clay, firing fees and day passes; AMOCA "POS program"; S01 central "check-out" booth at the member sale; Canberra volunteers handle "cash and card transactions"; S22 "Be sure to have your own way of taking payment"; S14 automated bookings and payments) | 0 | **0** | **Stripe named 0 times, Square 0, requests for several providers 0** |
| Bookkeeping, QuickBooks, taxes | 5 (S07 VAT bill; S13 tracking where money goes; S17 fear of not keeping up; S21 a year without charging tax; S23 "panic at tax time") | 3 (S33 payroll; AMOCA expense tracking; S08 hired a finance and admin director) | 1 (S21: "We cleaned everything up with QuickBooks.", vendor-hosted) | **0** | **QuickBooks named by 1 of 34 (vendor-hosted); 0 ask for an integration** |
| Scheduling | 5 (S01 schedule error; S09 daily rescheduling and makeups; S17 struggles with scheduling; S21 two classes "fused" into 20 people for 10 wheels; S32 hours rule to avoid congestion) + 4 capacity (CAP) | S01 8 of 20 posts are schedule or closure notices; AMOCA calendar, class registration and attendance; S33 "the calendar"; S22 class sign-up lists; S31 workshop logistics | 0 | **0** | 03 holds the only dismissive remark found in this research line ("We use Wix. It works for us."); not reused |

## No manager: what happens

- **Evidence:**
  - Verified: S06 Ilium (member-run until no one would sign the lease), S33 Color Me Mine (no full-time manager), S22 Alley Cat (owner plus volunteer monitors), S01 Fitton (manager absent).
  - Structure only: Hawaii Potters' Guild (Q78), Canberra Potters Society (Q93).
  - Pre-2023: S29 New Zealand clubs (2021).
  - Unverified: U2 Athabasca, U3 Dallas Makerspace.
- **What goes wrong (7 patterns):**
  1. **Nobody will carry the liability.** At Ilium the lease "hinged on who was going to be able to sign a commercial lease", and its terms were "concerning in terms of personal liability". The question was "looming over our heads for several months". A newcomer "ended up falling into it" and now spends most of his time running the studio (S06).
  2. **The owners absorb every role.** "We don't currently have a full-time manager, so we handle staffing ourselves." "Sometimes we're both at the studio; sometimes we alternate days." "Every day, something needs attention." (S33)
  3. **No systems until growth forces them.** "I was in survival mode, and didn't establish or hold up systems." The result is new shelf rules and fines enforced from October (S22).
  4. **One person is a single point of failure.** At Fitton, a broken ankle cost weeks, a forgotten medication cost a firing day, and trips need cover. Instructors take over kiln loading, inventory and cleaning, and firing is delayed (S01).
  5. **Chores are run as volunteer work groups.** Hawaii has 12: firing, glaze monitoring, clay distribution, pugging, registration, pot sales, social media and others. Canberra runs quarterly "cleaning bees" and has event volunteers handle card payments. Dallas Makerspace names a kiln team lead, a shelf-cleaning lead and a purchasing lead (U).
  6. **Rules are hard to enforce among peers.** At Dallas Makerspace (U, 2022) a member kept "picking up other folks pieces regardless of the signs", while others felt the signs were "not inviting". Alley Cat had to ask members not to touch other members' shelves.
  7. **Demand outruns volunteer capacity.** In 2021 the New Zealand clubs reported "full classes… with large waiting lists". Athabasca paused lessons when new members strained space and clay (U).

## Outputs they must produce

The baseline is 20 consecutive posts by the Fitton manager (2025-08-08 to 2026-06-19), plus other sources.

| Output | Evidence |
|---|---|
| Schedule, hours and closure notices | Fitton 8/20; Alley Cat class lists and "studio closures"; AMOCA calendar |
| Supply notices and supply orders | Fitton 7/20 (4 report a stock-out, regional shortage, discontinued line or low stock), plus a placed supply order (2025-12-05); Alley Cat monthly clay order form with a due date; Minot and Berkeley (2022) ordering duties |
| Firing status and delay notices | Fitton 4/20 posts; Alley Cat kiln status |
| Member sales events | Fitton 3/20 (sign-ups, central checkout); Alley Cat Members Market; Hawaii pot-sales work group |
| Newsletters | Alley Cat monthly Substack written on work trade; AMOCA social media and Mailchimp; Berkeley "Studio News" and monthly Member News |
| Policy updates and enforcement | Alley Cat (fines from October); Hudgens "orphan shelf" routine each session; Berkeley "Rack of Doom" (60 days in the 2022 posting) |
| Coverage notices (who covers when the manager is out) | Fitton 2/20 |
| Kiln logs and maintenance records | AMOCA posting; S34 "Keep a kiln log… change the thermocouple on a regular basis"; Hawaii firing group |
| Budgets and financial review | S21 reviews QuickBooks reports "every two or three months"; AMOCA budget development and expense tracking; Minot "pre-determined operating budget" |
| Payroll and timesheets | S33 payroll; AMOCA timesheets |
| Class registration and attendance | AMOCA |
| **Not found** | Monthly management or board reports (Berkeley's minutes PDF would not decode); anything about supplier invoices |

## Severity signals

| Signal | Count | Evidence |
|---|---|---|
| Closure, near-closure, or a line or asset closed | 8 verified + 1 U | Closed: S03 (2026). Storefront lost: S04 (2022). Pottery-painting line ended: S07 (2023). Wholesale dropped ("burn out or grow broke"): S30. Near-closure: S06 (2024); S24 "we would have failed" without an SBA loan and 70 volunteers (2022). Wood kiln retired: S02. Small kiln out of service: S22. U1: a large kiln decommissioned at ~£200 per firing |
| Money lost (stated or quantified) | 7 | S26 ~CA$6,500 kiln panel. S07 electricity "to quadruple from £14,000 a year". S28 energy bills "from around £900 to up to £2,500 a month" (writer's paraphrase). S09 lease attorney and realtor fees, "a big amount of amount of money". S21 "almost a year without charging people taxes". S03 unpaid rent from February (landlord filing, r). S14 kiln rental made "enough to almost cover one month of income for the year" after kiln repairs and electricity |
| Members or customers lost | 4 | S21: "I was losing a lot of people because I wasn't answering the phone."; 10 people turned away from a double-booked class and "The reviews were killing me". S09 summer dip. S03 members displaced and billed after closure (m, r). S06 members nearly lost the studio |
| Burnout in their own words | 7 | S10 "hit some burnout"; S30 "burn out or grow broke"; S23 "so exhausted"; S13 "a lot of lost sleep"; S21 "I was so overwhelmed."; S22 "survival mode"; S25 "overwhelmed" (2022) |
| Hours lost | 10 qualitative, 0 numeric | See Time |
| Staff lost or churning | 3 | S03 manager left over pay (r); S22 monitor stepped down after 4 years; S33 "a constant process of hiring and rehiring" |

## Agreement and disagreement with 01 and 03

- **Agrees with 03 on admin time.** 03's "I'm an artist, I don't love doing the admin stuff" fits a category that is #2 here (BURN, 16/34).
- **Agrees with 03 on overworked managers and congested shelves.** Fitton's backlog posts (Q71, Q72), Alley Cat's shelf fines (Q56) and Hudgens' orphan shelf (Q44) are new examples.
- **Disagrees with 01's ranking.** 01 put piece tracking #1, firing fees #2, shelves #3 and kiln scheduling #4. In owners' and managers' own words here, piece problems appear in 5 of 34 sources, kiln logistics in 3 and firing-fee fairness in 0, while money (rent, energy, pricing, cash flow) leads with 19. Source type explains part of this (see Gaps), but no owner here called piece tracking or fee calculation their biggest problem.
- **Agrees with 01 that supplier invoices are not a pain** (0/34 mention invoices) and that materials inventory sits low to middle (6/34, mostly supplier-side).
- **Agrees with 03 that software price is not the top complaint.** SOFT appears in 3 of 34. Only S14 complains about paying for several stitched tools, and S11 has a grievance with a website platform. 03's "nothing connects" theme gets one new example (S14, on its email system: "a different provider, but we've hooked that up").
- **Adds equipment upkeep**, which 01 and 03 did not rank. At 10/34 it is cited more often than firing logistics.
- **Payments.** 03 found four points of payment (drop-off, pickup, end of session, monthly invoice). Here nobody complains about payment rails, and the observed practice is one register or one central checkout.

---

## Hypothesis evidence

The ratings use: strong · moderate · weak · none · contradicts. Counts are over the 34 verified problem sources unless stated.

1. **Inventory tracking (clay, glaze, supplies): moderate for the pain, weak for tracking software as the fix.**
   - 6/34 sources report supply problems and 5 more describe inventory work as routine duty. 0 ask for inventory software and 0 are dismissive.
   - Most of the pain sits with suppliers: in S01 alone, 4 posts on stock-outs, shortages or discontinued glazes, plus 1 on low stock. Prices rise (S23, S28).
   - Tracking would help in 3 of 6 sources: low stock (S01), forgotten stock and re-ordering out of convenience (S23), money tied up in stock (S13).
   - Supply posts are 7 of the Fitton manager's 20 posts, so member-facing "out of stock / substitute / reordered" notices are real work.
   - Implication: stock levels with reorder reminders and a one-click member notice, not a full inventory system.
2. **Asset and equipment tracking: moderate. The pain is strong; direct demand for tracking is weak.**
   - Equipment problems are the #3 category (10/34), with costs (~CA$6,500 panel) and retired or out-of-service kilns (S02, S22).
   - Only 1 technician (S34: log firings to time thermocouple and element changes) and 1 job posting (AMOCA "kiln logs") describe tracking.
   - Implication: kiln records with firing counts, maintenance reminders (thermocouples, elements, relays), downtime status shown to members, and a repair log with costs. These fit naturally beside kiln logs.
3. **Sales integrations with several payment providers (Stripe, Square): weak for payments; none for multiple providers.**
   - 2/34 payment-related problems: S21 sales tax missing from website checkout, a configuration problem; S03 billing after closure, reported.
   - 6 practice mentions all show one register or one central checkout.
   - 0 sources name Stripe or Square and 0 ask for more than one provider.
   - The money pain that dominates (19/34) is rent, leases, energy and pricing, which payment integrations do not touch.
   - Implication: one payment path, correct sales tax, and recurring billing that can be paused or stopped (S08 removed dues pauses for planning reasons; S03 kept billing after closure). Multiple providers are not supported by this evidence.
4. **QuickBooks Online integration: weak.**
   - 5/34 report bookkeeping or tax pain: VAT (S07), tracking money (S13), keeping up (S17), uncharged sales tax (S21), tax-time panic (S23).
   - QuickBooks is named by 1 source (S21, a QuickBooks-hosted customer story). The owner is positive and uses it daily with an accountant.
   - 0 ask for an integration. The pains are tax set-up and keeping up, not moving data.
   - Implication: correct tax handling and clean exports (sales by category with tax, firing fees, memberships) come first. QBO sync is a later convenience.
5. **Scheduling: moderate.**
   - 5/34 report scheduling problems: a schedule error (S01), daily rescheduling and makeups (S09), scheduling as a weakness to hire for (S17), a double-booking across two sites that cost reviews (S21), and congestion rules (S32).
   - 4 more report capacity limits and waitlists (CAP), including 12–16 months at S02 (writer's figure).
   - Schedule and closure notices are 8 of the Fitton manager's 20 posts. 0 dismissive mentions in this sample.
   - Implication: self-service rescheduling and makeups, conflict checks across locations and rooms, broadcast closure notices, and waitlists.

## Other strong signals

1. **Money pressure means rent, leases and energy, not payment processing (19/34).**
   - Leases bring personal liability (S06), legal fees (S09) and constant stress (S12). One studio faced an eviction filing (S03, r), and another raised prices because of rent (S27).
   - UK energy costs hit both PYOP (S07) and production potteries (S28; U1).
   - A tool that shows true cost per class, per firing and per member would answer this better than more payment rails.
2. **Owner and manager time is the second-biggest theme (16/34).** In 6 sources the specific complaint is that running the studio crowds out the person's own making.
3. **The manager is a single point of failure (S01, read end to end).**
   - Injury, a forgotten medication or a trip delays firing, and instructors step in to cover kilns, inventory and cleaning.
   - "Stand in for a manager" therefore means handover: what is in which kiln, what is low, what members were told, and who covers which shift.
4. **Equipment upkeep outranks firing logistics in owners' own words (10 vs 3).**
5. **The communication load is real and generic.**
   - Answering emails and the phone: S09; S21, which lost customers; S14.
   - The Fitton manager's posts are mostly schedule (8/20) and supply (7/20) notices. Only 4 are about firing.
6. **Staffing churn and coverage (9/34).** Part-time turnover (S33), a monitor stepping down (S22), a pay dispute (S03, r) and hiring gaps (S15, S17). Work exchange is used for monitors and even the newsletter (S22).
7. **Studios manage with policies, not software.** Examples: shelf fines (S22), the orphan shelf (S18), the Rack of Doom at 60 days (S08, 2022), and hours that do not carry over (S32). Product rules that encode these (shelf allowances, expiry, notices) map onto what managers already do by hand.
8. **Capacity is a constraint (4/34).** S32 cannot rent out kilns. S02 has a 12–16-month waitlist (writer). S20 has closed its waitlist.

## Gaps and limits

- **No Reddit.** WebFetch refused every Reddit host this session ("unable to fetch from www.reddit.com", and likewise old.reddit.com and reddit.com). The RSS route used in 03 was closed, so 0 Reddit quotes are included. Reddit was 03's main owner-voice source.
- **The search budget ran out.** The 200-call WebSearch limit is shared by every agent in this session and ran out after 28 of this run's searches. Of 7 later attempts, 3 succeeded: one returned only vendor pages, one found the Dallas Makerspace minutes (403) and one found older co-op articles. After the search behind Batch 6, discovery came mostly from following links on pages already open: The Potters Cast archive pages 1–11, the Fitton board's pagination, the Ceramics Now jobs list, Turning Earth's site and Ceramics NZ links.
- **Blocked or unreadable pages** (listed, not cited as evidence):
  - 403: Ceramic Arts Daily forum, Dallas Makerspace forum and wiki, Town & Country Today (Athabasca), Express & Star (Wenlock), Pittsburgh City Paper (Clay Pittsburgh), SeekBusiness AU, Crowdfunder UK (North Tyneside), Las Vegas Review-Journal Neon (Clay Arts Vegas).
  - Domain not verifiable or not resolving: SmartCompany AU, arcpublishing CDN, artbeat.org.nz.
  - JavaScript-only: KERS extension reports.
  - PDFs that would not decode: the NVRC "2025 pottery studio changes" factsheet and the Berkeley member-meeting minutes. A 2025 "CCSA" kiln-firing-cost sheet did decode ("As studio owners, we need to know the cost of providing our products and services."), but its publisher could not be identified, so it is not cited.
  - 404: Phipps, Pottery Northwest and Alliance for the Arts job pages; Berkeley blog posts ("Rack of Doom", "Firing Slips FAQs"); LBHF news; NZCT.
  - 409: Octagon Arts.
- **Source-type bias.** 14 of 34 sources are podcast show notes and 9 are news or magazine pieces. Both favour founding stories, money and burnout. Only S01 gives a continuous operational record. Operational forums (Reddit, Facebook groups) would probably surface more kiln, piece and fee complaints, so KILN and PIECE are likely **understated** here compared with 01 and 03. Read the ranking as "what owners and managers volunteer when asked about running a studio", not as how often each problem occurs.
- **Geography.** US 23/34, Canada 5, UK 3 (+1 U), Australia 1, NZ 1 (pre-2023). The UK community-studio voice is a single organisational FAQ (Turning Earth). No UK owner of a membership studio was quoted.
- **Segments.** Makerspace 0 verified (U3 only) and municipal/recreation 0 (the NVRC PDF failed). PYOP has 2, university 1 and cooperative 1 (pre-2023), plus 2 structure-only pages.
- **No numeric hours-per-week** came from any owner (0/34).
- **Quote fidelity.**
  - WebFetch truncates each quoted span at 125 characters, so long sentences appear as fragments joined with "/" or "…".
  - Potters Cast "quotes" are the show's written Q&A excerpts, with spoken disfluencies kept.
  - 4 entries are reporter paraphrase (r) and are labelled.
- **Vendor-linked voices.** S21 is a QuickBooks customer story. S14's owner sells a kiln-rental app. S34 runs a kiln service business. All three are labelled. Kiln Fire blog posts were read but excluded (no named studios).
- **Dates.** S05 and S32 are undated, and most of S08's posts are undated (Q19 is from early 2025). Five sources predate 2023 and are labelled.
- **Counting rule.** Counting one source per studio understates breadth where several people spoke for one studio (S03, S06, S13, S30, S33).
- **No overlap with 01 and 03 quotes.** Two sources overlap with 03's: Berkeley (03 used its handbook and members zone; this file uses its news posts and a 2022 job ad) and the Color Me Mine article (03 used only the POS rollout fact; this file uses the owners' statements on staffing and admin).
