# 18. Supplier invoices are read by swappable readers, checked, and confirmed by a person

- **Status:** accepted
- **Date:** 2026-11-06

## Context

A studio gets a few supplier invoices a week, for clay, glaze materials and kiln parts, and someone
retypes each into supplies. Plan [16](../planning/16-invoice-ocr-plan.md) asks for an inbox that
reads them; plan [20](../planning/20-pre-production-cleanup-plan.md#invoice-readers-claude-google-and-azure-side-by-side)
adds Google Document AI and Azure AI Document Intelligence as platform-paid readers that the admin
turns on, compared side by side with Claude.

Three things shaped the decision:
- **An invoice is untrusted input.** Its text could try to instruct a model, and any reader can
  misread a number.
- **The readers differ** in who pays (the studio's own Anthropic key, or the platform), how they
  are reached, and what they return, but staff should see one draft and one review screen.
- **Stock and money** must change only when a person says so.

## Decision

**One interface, one draft.** `InvoiceReader` (`kind`, `canReadForBoundStudio`, `read`) has one
implementation per provider. Each maps its own output into the same `InvoiceDraft`. The gate,
the checks, the matcher, the review and the eval are shared and know nothing about providers.

**Claude, on the studio's own key.**
- One Messages request: the file as a document or image block, and structured output
  (`output_config.format`, derived by the Java SDK from the `ClaudeInvoiceAnswer` record). No
  tools, no follow-up turns. Forced `tool_choice` isn't used: it's a 400 on Claude Opus 5.5.
- Every value in the answer is a string: amounts stay exact decimals, and nothing is parsed until
  it's checked.
- The model id and its prices are settings (`studiomanager.invoices.claude.*`). Each run records
  tokens, estimated cost and time in `invoice_read`.
- A 401 or 403 marks the key refused, as "Test key" does. The SDK retries 429 and 5xx with
  backoff, three times.

**Which reader runs.** `InvoiceReaders` picks the first reader that can read for the studio.
Nothing falls back automatically: if that reader fails, the invoice becomes an empty draft with the
reason, typed in by hand next to the image. The platform readers (lane F1g) plug in as more
`InvoiceReader` beans; the admin's per-studio order replaces the fixed order in that one class.
The studio's consent for them is `ai_provider_key.allow_platform_readers` (renamed from
`allow_google_fallback` in V35).

**The platform's readers (lane F1g).**
- **Google Document AI's Invoice Parser** over REST, with Spring's `RestClient` and only
  `google-auth-library-oauth2-http`, not the gRPC client. Keyless from Azure: `IdentityPoolCredentials`
  with an `IdentityPoolSubjectTokenSupplier` (`String getSubjectToken(ExternalAccountSupplierContext)`,
  checked against 1.46.0) that hands over the managed identity's Entra token from `azure-identity`.
  Google's STS trades it for a token that impersonates a service account holding
  `roles/documentai.apiUser` (`infra/gcp`).
- **Azure AI Document Intelligence's** `prebuilt-invoice` (API 2024-11-30) as the managed identity,
  with *Cognitive Services User* and key auth off. Its result is polled only on the account's own
  endpoint, since the token goes with each poll. The model has no shipping field.
- Both ask for only what the mapping reads, and keep that as the raw answer. Failures name the
  provider and status, never the body.
- **The admin decides** (one `invoice-readers` key in the settings store, ADR 24): each reader on or
  off, for no studios, chosen ones or all, under monthly page caps per studio and for the
  platform, plus a per-studio order that `InvoiceReaders` follows. Defaults: on for the demo
  studio only. A reader also needs the owner's `allow_platform_readers` and its own settings.
  Pages are counted from `invoice_read`; the platform total and the admin's per-studio view cross
  tenants through two `SECURITY DEFINER` functions (V38) that return only counts.
- **Compare readers** runs one invoice through every reader the studio may use, records each run
  like any other, and leaves the draft alone.

**The gate and the checks.**
- `InvoiceDrafts.sanitize` cleans text and cuts it short, drops amounts beyond 9,999,999.99,
  rounds to the column's scale, and drops dates before 2000 or more than a year ahead. What it
  drops it notes, for staff; it never guesses.
- `InvoiceChecks` requires the lines to add up to the subtotal within a cent per line, and the
  subtotal plus tax and shipping to the total within a cent. A draft that doesn't add up is
  `NEEDS_LOOK`, not corrected.

**Matching.** In order: an alias for this vendor and the line's SKU; an alias for this vendor and
the line's description; then the supply item whose name is most contained in the description, by
trigram similarity, at 0.6 or more and with no tie. Aliases go first because staff confirmed them;
they're learned on every confirmation (an upsert in `vendor_alias`). Similarity runs in Java, not
`pg_trgm`: a studio has tens of items, and the extension would need allow-listing on Azure's
managed Postgres.

**A person confirms.** Confirming receives each matched line's quantity into stock through the
supplies module, in the same transaction, and remembers the aliases. Before that, nothing changes.

**Running it.** Completing the upload checks the file's size (10 MB) and its first bytes against
the type it was started as, then publishes `InvoiceReceived`. The reading job runs off the request
thread as the `INVOICE_READER` system account (ADR 16), bound to the studio. It reads with no
transaction open, so no database connection waits on the provider. The inbox is staff-only, and no
API token reaches it (`TokenPolicy`).

**The eval.** `InvoiceScorer` scores a draft against a hand-written answer: numbers and dates
exact, text normalized, a line right when its quantity, unit price and total all match.
`InvoiceEvalTests` scores recorded answers in CI, for free; `LiveInvoiceEval` runs live with a key
and writes a dated report into `docs/evals/`.

## Consequences

- **A reader is small to add.** F1g writes the adapters and their credentials; nothing else
  changes.
- **The model can only fill in a form.** It has no tools, its answer is held to a schema, and every
  value is checked again after that. The worst it can do is a wrong draft that someone has to fix.
- **Mistakes are traceable.** The raw answer is kept with the invoice.
- **Not done yet:**
  - purging the raw answer after 90 days, and sweeping uploads that never arrived;
  - the optional payable on confirmation;
  - a page count before reading (a PDF over Claude's limit comes back as unreadable);
  - the server-side refusal fallback for Claude Opus 5.5;
  - a Modulith scenario test for `InvoiceReceived` (the API test covers the event end to end);
  - checking the Entra token's issuer and audience against Google's pool provider on the first
    deploy (docs/runbooks/google-document-ai.md, step 6);
  - consent for a studio with no Anthropic key: it's stored with the key today.

## In an interview

"An invoice is untrusted input sent to a model, so the model gets one job and one output schema,
with no tools. Every value comes back as a string and goes through a gate that checks decimals,
dates and lengths, then the numbers have to add up, and a person confirms before anything touches
stock. The readers sit behind one interface, so Claude on the studio's own key and two cloud
document services produce the same draft, and an eval in CI scores them all against hand-labelled
invoices."
