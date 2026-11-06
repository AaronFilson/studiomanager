# 16. Supplies inbox: reading invoices with the studio's own AI key, and Google as the backup

_Planned 2026-11-05. This is item 6 in [08](08-wave2-queue.md), and gap 7 in the
[resume review](15-resume-review.md). It follows the glaze book._

## Why

A studio receives a few invoices a week, for clay, glaze materials, kiln parts and stains.
Someone retypes each one into supplies and the books. The supplies inbox reads the invoice and
proposes the lines. Staff check them, and nothing posts until they confirm.

## Shape

1. Staff drop a PDF or a photo into the inbox. It uploads straight to Blob Storage (ADR 12),
   with a size limit of 10 MB and up to 10 pages.
2. The upload commits an `InvoiceReceived` event. A listener reads the invoice off the request
   thread, acting as a new system actor, `INVOICE_READER` (ADR 16, with a `STAFF` ceiling).
3. **A reader** turns the file into a structured draft: the vendor, invoice number, date, lines
   (description, SKU, quantity, unit, unit price, line total), shipping, tax and total.
4. **A matcher** links each line to a supply item. It tries the SKU first, then the name using
   `pg_trgm` similarity, then aliases remembered from earlier invoices from the same vendor.
5. **Staff review** the draft against the image, side by side. They fix lines, pick items, and
   confirm. Confirming receives the stock (stock movements). Optionally it also records the bill
   as a payable through the existing ledger, so the books stay double-entry.
6. **Checks** run before anything is shown as ready:
   - the lines must add up to the subtotal, and subtotal plus tax plus shipping to the total,
     within 1¢ per line;
   - a total that doesn't add up flags the draft "needs a look", and nothing is silently
     corrected.

## Readers, in order

| Order | Reader | When | Credentials |
|---|---|---|---|
| 1 | **The studio's own AI key** (Anthropic first, then OpenAI) | The studio has saved a key | The studio's, encrypted at rest |
| 2 | **Google Document AI, Invoice Parser** | No studio key, or the studio's provider failed and the studio allows the fallback | The platform's GCP project, keyless (see below) |
| 3 | **By hand** | Both are off or failed | None. The inbox opens an empty draft next to the image. |

- **Behind one interface.** `InvoiceReader.read(file) -> InvoiceDraft`, with one adapter per
  provider. Each adapter maps its output into the same `InvoiceDraft`, and the matcher, the
  checks and the review are shared.
- **LLM adapter.**
  - The file goes in as a document or image block.
  - The output is forced into a JSON schema: tool use with one tool, or structured output where
    the provider supports it.
  - There are no other tools, nothing that browses, and no follow-up turns.
  - The model id is a setting, so it can be raised without a deploy.
  - Tokens and cost are recorded for each run.
- **Google adapter.**
  - Document AI's Invoice Parser returns typed entities (`supplier_name`, `invoice_id`,
    `line_item/description`, `line_item/amount` and so on) with confidences.
  - A low confidence marks that field for review.
  - It's the fallback because it's purpose-built and predictable, but weaker on handwritten or
    odd layouts.
  - **Unverified:** the current processor version, the per-page price, and the Java client.
    Check them before step 3.
- **Keyless GCP from Azure.** Workload identity federation trusts the Container App's managed
  identity, so no service-account key is stored (the same idea as ADR 6's GitHub OIDC). It's
  set up in Terraform.
- **Platform spend is capped.** The Google fallback costs the platform money, so there's a
  per-studio monthly page allowance, kept in a table and checked before each call.

## The studio's key

- **Who can set it:** only the owner, in Settings, under "AI". They pick the provider and paste
  the key. Its last 4 characters are shown after that, and the key itself never comes back to
  the client.
- **"Test key"** makes one tiny call and reports whether it worked.
- **Stored encrypted** with AES-GCM under a key-encryption key from Key Vault, bound to the
  studio id as associated data. This is the same pattern as the webhook secrets, so a row
  copied to another studio won't decrypt.
- **Never logged.** The key is redacted from logs and OpenTelemetry attributes, and a test
  checks that the redaction holds.
- **Off by default.** Turning it on shows what leaves the studio: the invoice image, sent to the
  chosen provider under the studio's own account and terms. Removing the key deletes it, and
  doesn't just disable it.
- **Failures:**
  - On a 401 or 403 from the provider, the key is marked bad and the owner is told once, with
    the same "disabled with a reason" pattern as webhooks.
  - On a 429 or 5xx, the read retries with backoff. After that it falls back if the studio
    allows it, and otherwise opens a draft to fill in by hand.

## Safety

- **The invoice is untrusted input.** The text on it could try to instruct the model. The model
  can only answer in the schema and can't take an action. Every value is validated: amounts are
  decimals within limits, dates are sane, and text is short.
- **A person approves every change.** Nothing changes stock or the books until a staff member
  confirms. The draft keeps the reader's raw output, so a mistake can be traced.
- **Data minimization.** Only the file is sent, with no member data. Raw output is kept for 90
  days and then purged by a system job.
- **The usual tenancy.** The inbox tables carry RLS like everything else, and the listener runs
  bound to one studio.

## Evaluation (the resume part)

- **A fixture set** of 20–30 real invoices, with the owner's own redacted where needed, plus
  synthetic ones. Each has hand-written expected JSON.
- **Score per field:** exact for numbers and dates, normalized for text. A line counts as right
  when its quantity, unit price and total all match.
- **CI** runs the scorer against recorded provider responses, so it's free and repeatable. A
  manual `eval` task runs live against a real key and writes a dated report into
  `docs/evals/`.
- **Numbers for the README:** field accuracy per provider, the share that needs no edits, and
  the median seconds and cents per invoice.

## Data (V27)

- `ai_provider_key`: one row per studio, holding the provider, the encrypted key, its last 4
  characters, the status, the reason it was disabled, and who set it and when.
- `invoice`: `studio_id`, `blob_path`, `status` (`READING`, `READY`, `NEEDS_LOOK`, `CONFIRMED`,
  `FAILED`), `reader` (`STUDIO_KEY`, `GOOGLE`, `MANUAL`), the vendor and its fields, `raw jsonb`,
  the cost, and who confirmed it and when.
- `invoice_line`, holding the matched `supply_item_id`; and `vendor_alias`, keyed by vendor and
  text, pointing at a supply item.
- `ai_usage`: per studio and month, the pages and tokens used, against the allowance.

## Steps

1. Keys: the table, encryption, Settings UI, "Test key", redaction, and ADR 18 (bring your own AI
   key).
2. The inbox, upload and LLM reader. The schema and validation, the draft and review screen, the
   checks, and confirming into stock.
3. The Google fallback: the adapter, Terraform for workload identity federation, the
   allowance, and the order the readers run in.
4. The matcher and vendor aliases. Optionally record the bill as a payable.
5. The eval set, the scorer in CI, a live eval report, and the README numbers.

## Status

**Step 1, 2026-11-06, on `wave2-tokens-mcp` (V28): the studio's own key.**
- **Storage:**
  - `ai_provider_key` has one row per studio. The key is AES-256-GCM encrypted under
    `studiomanager.ai.key-encryption-key`, with the studio id as associated data.
  - Azure refuses to start without the key-encryption key. Like the webhook key, it still needs
    wiring from Key Vault at first deploy.
- **Rules:** only the owner can set, test or remove the key, and no API token can reach it
  (`TokenPolicy`: `ai-key` is "never"). The key is never returned, only its last four
  characters.
- **"Test key"** lists models through the official Anthropic Java SDK (`anthropic-java` 3.6.0),
  which spends no tokens. A 401 or 403 marks the key REJECTED with the reason; anything else
  leaves it UNTESTED with a "try again". No database connection is held during the call.
- **Settings:** the owner has an "AI for invoices" card, with the Google fallback opt-in.
- **Only Anthropic, for now.** The plan mentioned OpenAI as a second provider. It's left out
  until someone asks for it.
- **To check against a real key:** which header the SDK sends the key in. The test stub accepts
  both `x-api-key` and `Authorization: Bearer`.
- **Next:** step 2, the inbox and the Claude reader. Use structured outputs
  (`output_config.format`) or a `strict` tool with `tool_choice: auto`. Forced `tool_choice` is a
  400 on Claude Opus 5.5.

## In an interview

"Invoices are untrusted input sent to an LLM, so the model gets one job and one output schema,
with no tools. Every value is validated and checked to add up, and a person confirms before
anything touches stock or the ledger. The studio brings its own key, encrypted per tenant. If
that key fails, a purpose-built OCR service takes over, under a spending cap. I measure it with
an eval set in CI, so a model or prompt change shows up as a change in accuracy."
