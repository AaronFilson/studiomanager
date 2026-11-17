# Invoice eval sources: datasets, public documents, fixtures, handwriting, templates

_Working notes, 2026-11-16. Everything was accessed on that date. Written for "Where more come from"
in [24-invoice-reading-upgrade-plan.md](../24-invoice-reading-upgrade-plan.md). The ranked
recommendation is at the end._

## How this was checked

- Tools: WebSearch, WebFetch, Read, Write, Edit, Glob, Grep. No shell, no subagents.
- **Verified** means I read it today on the source's own page: the Hugging Face card or file tree, the
  Zenodo record, or the GitHub README, LICENSE or file listing. **Secondary** means a third-party page
  said it. **UNVERIFIED** means only a search engine's text extract said it.
- **I opened no PDFs.** WebFetch saves a PDF as a binary file outside the repo, which the repo rules
  forbid. Early on, one arXiv paper PDF was fetched by mistake. The tool saved it under the Claude
  project's `tool-results` folder, not in the repo, and it was unreadable anyway. Every claim about
  what a PDF contains comes from search extracts and is marked UNVERIFIED.
- Fetch labels used below:
  - `git`: plain files in a git repo, with no LFS.
  - `git+lfs`: a Hugging Face repo. Images are LFS/Xet objects, so a plain `git clone` without
    git-lfs gets pointer files. I did not re-check HF's current Xet-to-git-lfs behaviour.
  - `parquet`: the images sit inside parquet files. Git can fetch them, but they need decoding.
  - `zip`: an HTTPS archive only, so a person has to download it.
  - `form`: access by request.

## 1. Public datasets with field-level ground truth

| Dataset | What, how many | Fields annotated | Image type | Language / currency | License (status) | Where and how to fetch |
|---|---|---|---|---|---|---|
| **CORD v2** | 1,000 shop and restaurant receipts (800/100/100) | **Line items**: `menu.nm`, `cnt`, `unitprice` (often absent; e.g. `@11000`), `price` (the line total), sub-items and voids. Also subtotal, discount, service, tax, total, cash, change and card. Values are strings: `60.000`, `24,000`. | Camera photos of paper receipts. I inferred that from the receipt-ROI field in each JSON; the README doesn't say "photo". | Indonesian / IDR | **CC BY 4.0**, verified on the GitHub README and HF card | `naver-clova-ix/cord-v2` is 6 parquet files, 2.31 GB (`parquet`). `Voxel51/consolidated_receipt_dataset` is in FiftyOne layout, with PNGs under `data/` plus `samples.json`, 1.3 GB, **train split only (800)**, CC BY 4.0 (`git+lfs`). The GitHub repo `clovaai/cord` has no data. |
| **SROIE 2019** | 1,000 scanned receipts. The GitHub mirror splits them 600 trainval / 400 test; HF has 626/361; Voxel51 has 712. | **Header only**: company, date, address, total. Word boxes and text. | Scans | English / MYR. The Malaysian origin is from memory; no page I read today said so. | **Not verified at source.** The RRC site failed TLS and needs a login. MMOCR's `dataset_zoo/sroie/metafile.yml` says CC BY 4.0, and the HF mirrors tag CC BY 4.0 (secondary). | `git`: `github.com/zzzDavid/ICDAR-2019-SROIE` holds `data/img`, `data/box` and `data/key`, with key files renamed `000.json`... Its MIT license covers the code only. `Voxel51/scanned_receipts` is 421 MB (`git+lfs`). |
| **WildReceipt** | 1,740 receipt photos (HF: 1.27k/472). The doctr docstring says 1,765. | 25 key/value box classes, about 50-69k boxes: store name, address, tel, date, time, product item/quantity/price, subtotal, tax, tips, total. **Boxes are not grouped into line items.** | Photos "in the wild" | English / not checked | **None stated.** MMOCR metafile: `Type: N/A`. The HF mirror `Theivaprakasham/wildreceipt` tags apache-2.0, which is the uploader's claim. | `download.openmmlab.com/mmocr/data/wildreceipt.tar` (`zip`-like). HF mirror format not checked. |
| **FATURA** | 10,000 invoice JPGs from 50 templates, 200 per template | 24 box classes. Header fields include seller, buyer, number, date, due date, PO, subtotal, tax, discount, total and amount due. **Line items are one `TABLE` box**, not separate fields. | Synthetic, clean renders | Not stated in the paper or the record | **CC BY 4.0**, verified on Zenodo record 8261508 | `FATURA.zip`, 363.5 MB, Zenodo only (`zip`) |
| **DocILE** | 6,680 annotated real business documents (53.7% FCC political-ad files, 46.3% UCSF Industry Documents), plus 100,000 synthetic and about 932k unlabeled | 55 field classes across KILE (key fields) and **LIR line items**: description, quantity, price and more. The full list is in the supplementary material, which I didn't read. | Scans and digital PDFs | English / currency in metadata, distribution not given | **No public license text.** Access is through a request form "for research purposes" (verified on docile.rossum.ai). The code is MIT. | Token-gated download script (`form`) |
| **katanaml-org/invoices-donut-data-v1** | 501 images (425/50/26), from the Mendeley "Samples of electronic invoices" set | **Line items**: `item_desc`, `item_qty` (`"2,00"`), `item_net_price`, `item_net_worth`, `item_vat`, `item_gross_worth`. Header: number, date, seller, client, tax IDs, IBAN. Summary: net, VAT, gross (`"$ 889,20"`). | Synthetic digital renders | English text, `$` with decimal commas | HF card: **MIT**. Source: Mendeley `tnj49gpmtz` v2, **CC BY 4.0** (verified). | `parquet`, 198 MB |
| **VRDU ad-buy forms** | **641 real** TV political ad-buy documents (orders, invoices, contracts) from FCC public files. The sibling set has 1,915 DOJ FARA registration forms. | Header: advertiser, agency, contract_num, flight_from/to, gross_amount, product, tv_address, property. Repeated **line_item**: channel, program_desc, program_start/end_date, sub_amount. `meta.json` sets per-field match rules. | Real filed PDFs. I didn't check page counts or the scan versus digital mix. | US English / USD | **No LICENSE file** (404) and none in the README. The repo was archived 2026-04-19. The underlying documents are federal public filings. | `git`: `github.com/google-research-datasets/vrdu`, `ad-buy-form/main/pdfs/` plus `dataset.jsonl.gz`. No `.gitattributes`, so no LFS. |
| **RVL-CDIP (invoice class)** | 25,000 invoice-class images out of 400k | **None**, class label only | Grayscale scans, longest side at most 1,000 px | English / mostly USD, assumed | HF card says "other". It is a subset of IIT-CDIP / Legacy Tobacco Documents, so their terms apply (verified). | 37 GB tar.gz, or HF at 38.8 GB |
| **DocVQA** | Question/answer pairs over UCSF Industry Documents pages, some of them invoices | **No field labels.** QA only, with no document-type label. | Scans | English | RRC terms are behind a login I couldn't read. HF repackages tag apache-2.0 or MIT (secondary). | RRC login, or HF `parquet` |
| **INV-CDIP** (Salesforce) | **350 labeled real invoices**, plus about 200k unlabeled, from the UCSF tobacco collections | Header only, 7 fields: invoice no., PO, invoice date, due date, amount due, total, total tax, with key and value boxes | Scans with stamps and handwriting | English / USD | **CC BY-NC 4.0** (verified in the README) | GitHub holds the annotations plus a downloader that pulls images from industrydocuments.ucsf.edu |
| **drew-ipp/invoice-extraction-benchmark** (InvoiceParser Pro, Oct 2026) | 181 documents: 113 text-layer PDFs, 53 JPEG, 15 PNG; 220 pages, about 37 MB. Categories: clean 31, currency 19, tax 14, **credit 10**, **multipage 10**, **freight 16**, scan 15, **photo 45**, **handwritten 8**, hard 13. | `vendor_name`, `invoice_number`, `invoice_date`, `due_date`, `currency`, `document_type`, `subtotal`, `tax`, `total`, `should_flag`, `references`. `lines[]` holds **only `description` + `amount`** (+`alt_amounts`): no qty, unit price or SKU. **No shipping field**: shipping is left out of both `lines` and `subtotal`. | Synthetic. Damage is simulated: photos have 15 types × 3 grades (perspective, rotation, blur, low light, shadow, crumples, coffee stains, pen marks, screen photos...). Scans have 5 looks × 3 grades (grayscale, noise, fax, punch holes, stamps). Handwritten docs are cash-book and delivery-note forms filled in with the Windows Ink Free font. | English / USD, EUR, GBP, ZAR, NAD, ZMW | **Data CC BY 4.0, code MIT** (README; `DATA_LICENSE` + `LICENSE`). Credit: "InvoiceParser Pro, Invoice Extraction Benchmark v1" plus a link. | `git`: `github.com/DrewKraken/invoice-extraction-benchmark`, where `.gitattributes` marks pdf/jpg/png `binary`, with no LFS. The HF mirror holds `corpus/v1`. |
| **Voxel51/high-quality-invoice-images-for-ocr** (Kaggle) | 8,181 invoice JPEGs, about 1650×2300; 1,489 have JSON | Header, **line items** (description, quantity, total price), subtotal, tax, discount, total, payment | Synthetic, per the card | English / not stated | HF tag **ODbL** ("Original license from Kaggle source"). I didn't check the Kaggle page. | FiftyOne layout with images under `data/`, 2.38 GB (`git+lfs`) |
| **CORU / ReceiptSense** (2024) | 20k receipts for KIE, 30k OCR images, 10k item rows, 1,265 QA images | Merchant, date, receipt no., total. Items: name, price, quantity, category. | "Noisy and degraded" receipts collected through an app | Arabic 54%, English 26%, mixed 20% | **Conflict**: the card text says MIT, the metadata says cc-by-4.0, and the GitHub repo has no license | Zips and CSV on HF (`zip`) |
| **ExpressExpense SRD (free 200)** | 200 US restaurant receipts | None from the vendor. Add-ons: **Zenodo 13688441** (2024, CC BY 4.0) has OCR labels and document corners for 200 hand-captured images. **Humans in the Loop** (CC0) has 192 images, 3,839 boxes in 10 classes, including item info, subtotal, tax and total. | The vendor says "high-resolution scanned images"; the Zenodo record says "hand-photographed" | English / USD | Vendor page: "free for use under The MIT License (MIT)", and asks for a citation (verified). Zenodo add-on: CC BY 4.0. HITL: CC0. | Vendor zip, 19.2 MB. Zenodo `Dataset.zip`, 19.0 MB. HITL behind a form. (`zip`) |
| **JensWalter/my-receipts** | The owner's own receipts as PDF scans, 2017-2024, many countries. `2019/us` alone has retail 15 (Walgreens, CVS, Safeway, Whole Foods, Old Navy...), restaurant 19 and hotel folios 4, plus cafe, transport and tourist folders. | **None.** We would label them. | **Real** POS/thermal receipts and hotel folios, scanned | Mixed; the US folders are USD | **"CC0 - public domain"** (README, verified) | `git`: `github.com/JensWalter/my-receipts`. No `.gitattributes`, so no LFS. Also on Kaggle as `jenswalter/receipts`. |
| **Zenodo 6371710** | 813 invoices and receipts from a private company | Header only: seller name, address and tax ID, buyer tax ID, date, total, tax, document reference | Not stated | Portuguese / not stated | **CC BY 4.0** (verified) | Two zips, 410 MB of images (`zip`) |
| **Inv3D** (KIT, 2023) | Synthetic and real high-resolution invoice images with warp meshes, for unwarping | No KIE labels. It has flat and warped pairs plus an "information delta" text image. | Photos and renders | Not checked | **CC BY-NC-SA 4.0**, and some files are academic-use only | RADAR4KIT archives, about 128 GB for one package |

Also seen and set aside:
- `nanonets/key_information_extraction`: apache-2.0, parquet, under 1k rows, tagged receipts. I didn't
  check its content.
- `llamaindex/ExtractBench` (2026): apache-2.0 tag, 370 enterprise documents (4,869 pages), mixed real
  and synthetic. It isn't confirmed to contain invoices.
- AMuRD: research-only, per secondary sources.
- `DuckerMaster/Thai-Synth-Receipts`: MIT, synthetic, Thai.
- `mychen76/invoices-and-receipts_ocr_v1`: no license.
- `longmaodata/Invoice-annotation`: HTTP 401, so gated.
- Roboflow Universe "invoice"/"receipt" projects: CC BY 4.0 tags, but they are detection boxes only
  and download through a Roboflow account.

Notes for our `expected.json` shape:
- Every source needs a hand-checked conversion, and the numbers need normalising first:
  - CORD uses `.` or `,` as thousands separators, and `unitprice` is often absent.
  - katanaml uses decimal commas.
  - The drew-ipp credit notes print negatives as brackets, a `CR` suffix or a leading minus.
- **Deriving shipping from drew-ipp.** Its keys have no shipping field, so
  shipping = total - subtotal - tax wherever no other charge applies.
- **Filling in drew-ipp lines.** Its lines give only an amount. Qty, unit and unit price have to
  be read off the page.

## 2. Real ceramics-supplier invoices in public records

**Result: I could not confirm a single public document that shows a ceramics supplier's invoice,
quote or packing slip.** That is short of the 10+ target. These searches turned up nothing:

- Vendor names with "invoice", "quote", "Sold To", "Ship Via" and "Remit", across BoardDocs,
  Granicus, CivicPlus and `.gov` hosts.
- Vendor street addresses with invoice words, e.g. "14400 Lomitas" and "1101 Stinson".
- DocumentCloud and MuckRock: the API returned 403, and a site-restricted search found nothing.
- CourtListener: the API returned 401.
- Wikimedia Commons: only 18th-century bill-heads.
- Scribd, Yumpu and Issuu: only catalogs.

What public bodies post is **payment registers, bill lists, PO lists and bid tabulations**. These name
the vendors, with amounts and sometimes invoice numbers, but they don't show the invoice. Packets that
attach scanned invoices are often image-only, so text search can't find them.

### Leads: public records that point at specific invoices (all UNVERIFIED)

Each row is a real search hit, and the PDF was not opened. "Reported" is what the search tool's text
extract said. I spot-checked two of the identifiers, `SI283529` and `INV-8391`, with exact-match
searches, and neither came back. **Treat the invoice numbers as hints and confirm them in the PDF.**

| Public body | Document | Vendor | Reported |
|---|---|---|---|
| St. Lucie County School Board (FL) | [Payment of Bills, Jan 2026](https://go.boarddocs.com/fl/stlucie/Board.nsf/files/DRELZ758DE1B/$file/Payment%20of%20Bills%20-%20Jan%202026.pdf) | Laguna Clay | $765.00 on 01/15/2026, invoice ref "SI283529" |
| Pittsburgh Public Schools (PA) | [Daily Payments, April 2026](https://go.boarddocs.com/pa/pghboe/Board.nsf/files/DUBMAS58E013/$file/Daily%20Payments%20April%202026.pdf) | Standard Ceramic Supply | invoice "INV4241", $340.00, 4/30/2026 |
| Santa Barbara City College (CA) | [Check register, Dec 2023](https://go.boarddocs.com/ca/sbcc/Board.nsf/files/D4GPMP6518CD/$file/Board%20Report%20-%20Check%20RegisterDec%202023.pdf) | Laguna Clay | payments on 12/14 and 12/21/2023 for moist clay, pallets, shipping and glaze materials |
| Pasadena Area CCD (CA) | [Purchasing transactions, Feb 2019](https://www.boarddocs.com/ca/pasadena/Board.nsf/files/B9A5VW115171/$file/Feb%2020-B%20Purchasing%20Transactions.pdf) | Laguna Clay | PO P1901422, $2,389.53 |
| Madison Metropolitan SD (WI) | [Check register, 06.05.2026](https://go.boarddocs.com/wi/mmsd/Board.nsf/files/DUUN7L5E8F8A/$file/06.05.2026%20Check%20Register.pdf) | Laguna Clay | check 1227417, $641.18, 05/19/2026 |
| City of Columbia (SC), Parks | [Truth in Spending, June 2025](https://finance.columbiasc.gov/wp-content/uploads/2025/07/June-2025-TIS.pdf) | Laguna Clay | moist clay for ceramic camp, about $4,318. Similar entries in the [Nov 2019](https://finance.columbiasc.gov/wp-content/uploads/2021/01/November-2019-TIS.pdf) and [Oct 2023](https://finance.columbiasc.gov/wp-content/uploads/2023/11/October-2023-TIS.pdf) reports. |
| Town of Jonesborough (TN), McKinney Center | BMA packets: [3-9-15](https://www.jonesboroughtn.org/wp-content/uploads/3-9-15_Regular_Meeting.pdf), [12-14-15](https://www.jonesboroughtn.org/wp-content/uploads/12-14-15_Regular_Meeting.pdf), [7-11-16](https://www.jonesboroughtn.org/wp-content/uploads/7-11-16_Regular_Meeting.pdf), [10-14-19](https://www.jonesboroughtn.org/wp-content/uploads/10-14-19_Regular_Meeting.pdf) | Highwater Clays | $327.93, $141.85, $129.00 and $95.00. Two separate searches agreed. |
| Federal Way Public Schools (WA) | [General Fund warrants 317570-317613](https://go.boarddocs.com/wa/fwsd210/Board.nsf/files/DQ2QS86A9F49/$file/1-A-05_General%20Fund%20Warrants%20317570%20through%20317613.pdf) | CAC Clay Supplies, the BBB-listed alternate name of Clay Art Center | "INV-8391", clay, glazes and underglazes, $1,498.72 |
| Edmonds SD (WA) | [Agenda packet, 10-23-18](https://go.boarddocs.com/wa/edmonds/Board.nsf/files/C6GVDY804386/$file/AGENDApacket__10-23-18_1248_433.pdf) | Seattle Pottery Supply | $943.84, 09/24/2018 |
| San Diego Unified (CA) | [POs issued, July 2023](https://go.boarddocs.com/ca/sandi/Board.nsf/files/CVV4HA0B9B33/$file/Purchase%20Orders%20Issued%20by%20Delegation%20of%20Authority_July%202023.pdf) | Skutt (dealer not named) | KMT1231 kiln and repair parts |
| Shore Regional HSD (NJ) | [Bill list, Feb 24 2025](https://go.boarddocs.com/nj/shrsd/Board.nsf/files/DE3PK764F2FF/$file/Bill%20List%20-%20February%2024,%202025.PDF) | Skutt | KMT1027-3 kiln, $5,034.29, PO P202401432 |
| Oswego CUSD 308 (IL) | [Bill list, 11-17-2025](https://go.boarddocs.com/il/oswego308/Board.nsf/files/DNDSMQ735E4A/$file/Bill%20List%2011-17-2025.pdf) | Skutt kiln parts through B&G Supplies | $1,641.84, invoice dated 8/29/2025 |
| Kenai Peninsula Borough SD (AK) | [Re-Cap Bid #130-11](https://kpbsd.org/WorkArea/DownloadAsset.aspx?id=21401&ItemId=21401&transitionUi=1) | Bailey Pottery Equipment, among others | bid tab with item numbers and unit prices (kiln filters, etc.) |
| Erie SD (PA) | [Signed Blick agreement, 2020-21 art bid](https://go.boarddocs.com/pa/eriesd/Board.nsf/files/BPTNQ560F46E/$file/Signed.BLICK.AGREEMENT.BID20-21%20ART%20SUPPLIES.pdf) | Blick (clay from M & M Pottery) | clay lines with item no., unit price and quantity |

Status: registers like these are public records. An invoice copy obtained under a records law is also a
public record, but the vendor still owns its layout and logo. Invoices are mostly facts.

### Vendor pages that show how their paperwork works (facts for templates, not documents)

- **Big Ceramic Store** ([FAQ](https://bigceramicstore.com/pages/help)): for kilns that aren't 120 V,
  it emails a detailed order confirmation, and the order goes ahead only after the customer replies.
  The FAQ gives a check remittance address in West Lebanon, NH.
- **Laguna Clay**:
  - Its [Order Now](https://www.lagunaclay.com/pages/order-now) page is a quote-request form.
  - Its [damage/returns PDF](https://cdn.shopify.com/s/files/1/0664/2050/6866/files/damage_returns_Warranty.pdf?v=1754501021)
    asks for "a copy of your invoice", the bill of lading and the paid freight bill.
  - That PDF sits on `cdn.shopify.com`, so Laguna's store is on Shopify. That is my inference.
    URL patterns like `/pages/` and `/collections/` suggest the same for Sheffield Pottery, Seattle
    Pottery Supply, Highwater, Standard Ceramic and Big Ceramic Store, which I didn't confirm.
  - This backs the plan's Shopify order-confirmation / packing-slip template.
- **Sheffield Pottery**:
  - The [schools page](https://www.sheffield-pottery.com/pages/ceramics-supplies-for-school-bids-and-contract-pricing)
    mentions direct quotes and cooperative contract pricing.
  - Its shipping policy wants the invoice number on returns and a signed PO for school pickups.
  - Its [Equipment Ordering Checklist](https://www.sheffield-pottery.com/pages/equipment-ordering-checklist)
    is a web form.
- **Axner**: schools with authorized POs get prepaid freight "at the actual cost", so expect a freight
  line on school invoices.
- **Minnesota Clay** ([policies](https://www.mnclay.com/policy.aspx)): takes POs by fax or mail and
  wants an AP contact.
- **Clay-King**: school and government POs on Net 30.
- **The Ceramic Shop**: POs need item codes, bill-to, ship-to and an AP contact.
- **AMACO**: web orders are paid in full before shipping.
- **Uline**: "Invoice Me" is Net 30, freight is billed after shipping, and there is a
  [Find Invoice](https://www.uline.com/MyAccount/FindInvoice) page. I found no public sample image.
- **Shopify**: the [packing slip variable reference](https://help.shopify.com/en/manual/orders/packing-slips-variable-list)
  and the [Order Printer template docs](https://help.shopify.com/en/manual/orders/order-printer/customizing-order-templates)
  give the fields a Shopify packing slip or invoice can show. Use them as a layout reference only.

Real SKUs and prices for synthetic line items:
- [Laguna catalog section 9](https://www.yumpu.com/en/document/view/11660023/catalog-section-9-laguna-clay)
- [AMACO catalog](https://www.yumpu.com/en/document/view/27139545/download-a-pdf-amaco)
- [Clay Art Center Mini Catalog 08-25-20](https://clayartcenter.net/wp-content/uploads/Mini-Catalog-082520.pdf)
- [Skutt 2021 kiln catalog](https://skutt.com/images/Ceramic-Kiln-Catalog-2021.pdf)
- [Minnesota Clay catalog](https://www.mnclay.com/catalog.aspx)
- [Standard Ceramic catalog](https://www.dogwoodceramics.com/product/standard/standard-ceramic-supply-catalog.htm)
- Blick pages for [Laguna WS-5 WC397](https://www.dickblick.com/products/laguna-ws-5-moist-clay-wc397/)
  and [Redstone WC420](https://www.dickblick.com/products/laguna-redstone-clay-wc420/)
- Sheffield's [Coyote glaze page](https://www.sheffield-pottery.com/products/coyote-clay-and-color-vibro-color-glazes)
  (SKU CG200P)

Use the facts only. In committed synthetic invoices, use fictional vendor names and no logos, so
nothing looks like a real vendor's document.

### How to actually get real ceramics invoices

1. **The owner's own paperwork.** The private set already has 5 cases. Nothing public replaces it.
2. **Ask a vendor** for a sample invoice or packing slip with written permission to publish. Sheffield
   Pottery's schools contact, Bailey or Laguna are candidates. This is the only clean path to a
   public, committable ceramics invoice.
3. **Public-records requests** for the invoices behind the leads above. Examples: Laguna invoices from
   St. Lucie County Schools, Madison MMSD or Columbia SC Parks; Highwater from Jonesborough; CAC/Clay
   Art Center from Federal Way. Keep what comes back in the private set unless the vendor agrees, or
   after redaction.

## 3. Sample invoices bundled in code repos, with expected outputs

| Repo and path | What | Expected output | License | Fetch |
|---|---|---|---|---|
| [invoice-x/invoice2data](https://github.com/invoice-x/invoice2data) `tests/compare/` | 12 cases from 16 input files (`.pdf`, `.png`, `.txt`). Real invoices from AWS (2014), Flipkart, Oyo, QualityHosting, coolblue ×2, Free fiber, Saeco, Netpresse and Orlen (txt only). Odoo-demo invoices: `AzureInterior` and `SammyMaystoneLinesTest`. | A `.json` per case. Keys vary by template: issuer, amount, amount_untaxed, date, invoice_number, currency, and `lines` (name, price_unit) in some. AWS: amount 4.11, 4 lines. | **MIT** (verified). The PDFs are contributors' real invoices, with no separate grant. | `git` |
| [Azure-Samples/cognitive-services-REST-api-samples](https://github.com/Azure-Samples/cognitive-services-REST-api-samples) `curl/form-recognizer/` | `sample-invoice.pdf`, `Invoice-6.pdf`, `simple-invoice.png`, `invoice_sample.jpg`, `invoice-logic-apps-tutorial.pdf`, `contoso-receipt.png`, `contoso-allinone.jpg` | Machine output: `sample-invoice-output.json` (API v2.1.0), `analyze-result-invoice-6.pdf.json` and `receipt-result.json`, plus `Invoice_1.pdf.labels.json` (hand labels). **`sample-invoice.pdf` is an amount-due case**: Contoso INV-100, subtotal 100.00, tax 10.00, total 110.00, previous unpaid balance 500.00, **amount due 610.00**. The JSON's one line reads qty 1, unit price 1, amount 100.00, a mismatch to check against the page. | **MIT** (LICENSE.md verified) | `git` |
| [Azure-Samples/document-intelligence-code-samples](https://github.com/Azure-Samples/document-intelligence-code-samples) `Data/invoice/`, `Data/receipt/` | Invoices: English (plain, Britain ×2, India, KVP), Dutch, French, German, Italian, Portuguese, Spanish and a KVP variant, plus `invoice.pdf`, `sample-invoice.pdf` and zipped labeled sample data. Receipts: `receipt-with-tips.png`, `receipt-hotel.png`, `receipt-app-like.png`, `receipt-invoice-like.png`, Japanese, French and Spanish. | Some outputs: `sample-invoice-output.json`, `receipt-result.json`, the Invoice_1 labels | **MIT** (verified) | `git`. The listing was truncated, so there may be more files. |
| [aws-samples/amazon-textract-textractor](https://github.com/aws-samples/amazon-textract-textractor) `tests/fixtures/` | `invoice.png`, `receipt.jpg`, `receipt_no_summary.png`, `sample-invoice.pdf` | `saved_api_responses/test_analyze_expense_*.json`: AnalyzeExpense machine output. Which input each file belongs to wasn't confirmed. | **Apache-2.0** (verified) | `git` |
| [ZUGFeRD/corpus](https://github.com/ZUGFeRD/corpus) | "Real, sample and test" e-invoices: ZUGFeRD v1/v2, Factur-X, XRechnung, FatturaPA, PEPPOL. Some have deliberate errors. | **The XML embedded in each PDF is exact ground truth**, including line items: qty, unit, net price, totals and tax. | **Apache-2.0** (verified) | `git`. Mostly German and EUR. |
| [DrewKraken/invoice-extraction-benchmark](https://github.com/DrewKraken/invoice-extraction-benchmark) | See section 1. It includes a scorer: cents-exact amounts, ISO dates, and lines greedy-matched by amount and description similarity of at least 0.6. | `corpus/v1/answers/*.json` | Data CC BY 4.0, code MIT | `git` |
| [katanaml/sparrow](https://github.com/katanaml/sparrow) | `invoice.pdf` and others, with JSON shown in the README | README only | **GPL-3.0** plus "Free for open source projects and organizations under $5M revenue", with dual licensing. **Avoid.** | `git` |
| [mindee/client-lib-test-data](https://github.com/mindee/client-lib-test-data) `v1/products/invoices`, `expense_receipts` | A few samples (`invoice_10p.pdf`, `us_receipt.jpg`) with API responses | Machine output | **No license found.** **Avoid.** | `git` |
| Google Document AI samples | Sample invoices sit in the GCS bucket `gs://cloud-samples-data/documentai/`, e.g. `invoice.pdf` and codelab `google_invoice.pdf`, not in git (secondary) | Docs show sample outputs | License for the sample files not found | GCS over HTTPS. Not `git`. |

## 4. Handwritten invoice, order-form and receipt datasets

**No public dataset of handwritten invoices, carbonless order forms or receipts with field labels and
a usable license turned up.** The closest things:

- **drew-ipp/invoice-extraction-benchmark, "handwritten" (8 docs, CC BY 4.0):** synthetic cash-book and
  delivery-note forms "filled in" with Windows Ink Free at three legibility grades. The font isn't
  redistributable, which is a reason to use OFL fonts in our generator.
- **GNHK** (GoodNotes, ICDAR 2021): 687 camera-captured images of English handwriting "in the wild",
  with text and line transcriptions and no key fields. CC BY 4.0, per the
  [GitHub README](https://github.com/GoodNotes/GNHK-dataset). The repo holds no data. The download
  link points to goodnotes.com/gnhk, which today showed only marketing content, so **the download
  location is UNVERIFIED**.
- **FUNSD**: 199 noisy scanned forms with some handwriting. Its terms are "solely for non-commercial,
  research and educational purposes" (secondary). **XFUND** is CC BY-NC-SA 4.0 (secondary). Both are
  forms, not invoices.
- **NIST SD 2 / SD 6**: synthesized tax forms. SD 6 has hand-print. 5.6k binary pages each, free
  download. Terms not checked, and they aren't invoices.
- **CM1** (2025, arXiv 2505.04214): historic handwritten forms (names, birthdates). License unknown.
- **[tibendadavis/handwritten-forms-vlm-bench](https://github.com/tibendadavis/handwritten-forms-vlm-bench)**
  (MIT): a method to copy. People write values onto blank DHIS2 health forms, photograph them, and
  score VLMs. Its roadmap lists the dataset as future work.
- `bernardadhitya/handwritten-form-ocr-ie-json-dataset`: a compilation of FUNSD, IAM, RVL-CDIP,
  cheques and others, "for research and educational purposes". **Avoid.**

The owner's handwritten order form, written by real staff, stays the gold case. To get more, fill blank
carbonless-style forms by hand in a few different hands, photograph them over till receipts, and write
the answers at fill time. The synthetic generator can then add volume.

## 5. Templates, fonts and degradation tools for the generator

**Handwriting and receipt fonts.** These come from the `google/fonts` repo; folders verified today. The
folder name is the license: `ofl/` means SIL OFL 1.1 and holds `OFL.txt`, `apache/` means Apache-2.0
and holds `LICENSE.txt`. The repo is large, so fetch only these folders with a sparse `git` checkout.

| Font | Folder | Use |
|---|---|---|
| Caveat (variable weight) | `ofl/caveat` | Quick handwriting |
| Kalam (Light, Regular, Bold) | `ofl/kalam` | Neat print hand, three weights for different writers |
| Patrick Hand | `ofl/patrickhand` | Print hand |
| Indie Flower | `ofl/indieflower` | Loose hand |
| Nanum Pen Script | `ofl/nanumpenscript` | Messy pen |
| Homemade Apple | `apache/homemadeapple` | Cursive |
| Rock Salt | `apache/rocksalt` | Hard to read, for the "hard" grade |
| Just Another Hand | `apache/justanotherhand` | Narrow, fast hand |
| VT323 | `ofl/vt323` | Dot-matrix or thermal till look |

Other likely OFL candidates I didn't open: Shadows Into Light, Gochi Hand, Reenie Beanie, Architects
Daughter, Covered By Your Grace, Courier Prime, Share Tech Mono.

**Invoice templates:**
- [sparksuite/simple-html-invoice-template](https://github.com/sparksuite/simple-html-invoice-template):
  MIT, verified. A single HTML file, which suits the plan's Playwright rendering.
- Typst templates, licenses from listings (secondary): quarto-invoice (MIT), invoice-pro (MIT),
  erictapen/typst-invoice (MIT-0), invoice-maker (ISC).
- LibreOffice "JustInvoice" (CC-BY, per the extensions-site listing).
- I found no open carbonless order-book template. Lay out a two-part counter form in HTML: qty,
  description, "Amount" column, totals box, signature line.

**Degradation tools:**
- [Augraphy](https://github.com/sparkfish/augraphy): MIT, verified. Folding, BleedThrough, ShadowCast,
  PageBorder, InkBleed, LowInk lines, DirtyRollers, Letterpress, Geometric. It is Python, and its README
  calls camera-photo effects future work. Use it only if the generator can call Python; otherwise copy
  the ideas into Playwright/canvas.
- [microsoft/genalog](https://github.com/microsoft/genalog): MIT, HTML templates plus scan-like noise.
  It is in maintenance mode.
- [FelixHertlein/inv3d-generator](https://github.com/FelixHertlein/inv3d-generator): MIT unless a
  subfolder says otherwise. The licenses of its bundled fonts and templates aren't stated.

## 6. Which source covers which hard case

| Case | Public sources | Gap? |
|---|---|---|
| Digital PDF | Azure samples, invoice2data, ZUGFeRD corpus, drew-ipp "clean" | No |
| Scanned paper | JensWalter (real), VRDU (real, if scanned), SROIE, drew-ipp "scan" (simulated) | No |
| Phone photo: rotation, perspective, folds | CORD (real), ExpressExpense + Zenodo 13688441 (real), drew-ipp "photo" (simulated) | Partly. No real ceramics photos. |
| Overlapping papers, receipts on top, bleed-through | None found | **Yes**: private set plus the generator |
| Handwritten carbon-copy order form, Amount = line total, no unit price | drew-ipp "handwritten" (8, synthetic) | **Yes**: private set plus the generator |
| POS/thermal till receipt | JensWalter US retail and restaurant, CORD, SROIE, ExpressExpense | No |
| Card-terminal slip | Possibly inside JensWalter restaurant scans (not checked). CORD has `creditcardprice` fields. | Mostly |
| Packing list, no prices | None found | **Yes** |
| E-commerce confirmation (Shopify, Amazon, Square) | Azure `receipt-app-like.png` / `receipt-invoice-like.png` only, judged by file name, not opened | **Yes**: generator from the Shopify variable docs |
| Multi-page | drew-ipp "multipage" (10), VRDU ad-buy, ZUGFeRD | No |
| Credit memo / return | drew-ipp "credit" (10: brackets, CR suffix, minus signs) | Synthetic only |
| Shipping/handling as lines | drew-ipp "freight" (16: delivery, freight, handling, fuel surcharge, S&H aliases) | Synthetic only |
| Partial payment, amount due vs total | Azure `sample-invoice.pdf` (110 vs 610), drew-ipp `clean-us-credit-applied` and `clean-us-overpaid-invoice` | Thin |

## 7. Ranked recommendation

### Use first

1. **drew-ipp / DrewKraken invoice-extraction-benchmark.** CC BY 4.0, plain `git`. It is the only
   public set with credit notes, freight and handling lines, amount-due cases, multi-page,
   simulated photos and scans, and handwritten forms in one place, with cent-exact keys.
   - **Take:** about 12 US/USD documents across clean, freight, credit, multipage, photo (severe),
     scan and handwritten.
   - **Costs:** it is synthetic and vendor-made, and one month old. Its lines have no qty, unit
     price or SKU, and it has no shipping field, so each answer needs re-checking into our shape.
2. **JensWalter/my-receipts.** CC0, plain `git`, real scanned US till receipts and hotel folios. No
   labels, so we write them.
   - **Take:** about 8 (3 retail, 3 restaurant, 2 hotel).
3. **CORD v2 via `Voxel51/consolidated_receipt_dataset`.** CC BY 4.0, `git+lfs`. Real camera photos
   with line items, where `price` is the line total and `unitprice` is often missing, which matches
   our order-form pattern.
   - **Take:** about 4.
   - **Costs:** IDR, with thousands separators to normalise.
4. **Microsoft and AWS samples plus invoice2data.** MIT and Apache-2.0, plain `git`, digital PDFs, with
   machine outputs to diff against.
   - **Take:** Azure `sample-invoice.pdf` (the amount-due case), Azure `receipt-with-tips.png`,
     textractor `receipt.jpg`, and invoice2data `AmazonWebServices.pdf` or `FlipkartInvoice.pdf`.
     That's 4.
5. **VRDU ad-buy forms.** Plain `git`, 641 real US ad-buy invoices and orders with line items. Many
   are probably multi-page; I didn't check.
   - **Take:** about 4.
   - **Costs:** no license file, so **private set only**.

That's about 32. Add 8 from the owner's own ceramics paperwork, the 5 existing plus 3 more, for a set
of about 40. Optional swaps:
- ExpressExpense receipts (MIT, real US restaurant photos) for more photos. They come as a zip, so a
  person downloads them.
- A ZUGFeRD PDF for an exact-XML line-item case.

### Unusable or not worth it, and why

- **DocILE.** Access is by request "for research purposes" and no license is published. It can't be
  committed, and it is research-scoped.
- **INV-CDIP.** CC BY-NC 4.0, and header fields only.
- **FUNSD and XFUND.** Non-commercial, and they are forms, not invoices.
- **Inv3D.** CC BY-NC-SA, about 128 GB, and it has no KIE labels.
- **RVL-CDIP and DocVQA.** No field labels, low resolution (RVL-CDIP is at most 1,000 px), and unclear
  terms.
- **Sparrow.** GPL-3.0 plus a revenue condition.
- **Mindee test data.** No license.
- **bernardadhitya compilation.** Research and educational use only.
- **SROIE and WildReceipt.** Their licenses aren't stated by the owners. SROIE is header-only and
  WildReceipt is unordered boxes. Private use at most.
- **FATURA.** CC BY 4.0 is fine, but the line items are a single table box and it is zip-only. Low
  value.
- **Voxel51/Kaggle HQ invoices.** ODbL share-alike, synthetic. Low value.
- **CORU.** Conflicting license, mostly Arabic.

### Public repo versus local-only

- **Can be committed to the public set, with attribution in the eval README.**
  - JensWalter/my-receipts: CC0, no credit required.
  - drew-ipp: CC BY 4.0. Credit "InvoiceParser Pro, Invoice Extraction Benchmark v1" plus a link.
  - CORD via Voxel51: CC BY 4.0. Credit Clova AI / NAVER.
  - Azure samples: MIT, keep the notice.
  - textractor: Apache-2.0, keep the notice.
  - invoice2data: MIT. Low risk, but the PDFs are third parties' real invoices.
  - ZUGFeRD corpus: Apache-2.0.
  - ExpressExpense images: MIT, plus their citation request.
  - Zenodo 13688441 labels: CC BY 4.0.
  - HITL boxes: CC0.
  - Zenodo 6371710: CC BY 4.0.
  - Google Fonts: ship OFL fonts with `OFL.txt`.
- **Local-only, in `invoices/private/`.**
  - The owner's invoices.
  - VRDU, which has no license.
  - DocILE, INV-CDIP, SROIE and WildReceipt.
  - Anything from a public-records request, until the vendor agrees or it is redacted.
  - Vendor catalogs and Shopify templates: reference only.

### Gaps nothing public fills

- Real ceramics-supplier invoices.
- Carbonless handwritten order forms.
- Packing lists.
- Overlapping or bleed-through photos.

Those come from the owner's photos, a vendor's permission, or the generator.
