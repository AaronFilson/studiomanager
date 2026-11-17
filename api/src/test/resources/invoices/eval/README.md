# Invoice eval cases

Each folder is one document. `InvoiceEvalTests` scores the recorded answers in CI; `LiveInvoiceEval`
and `LiveDocumentServiceEval` read the files live and record the answers (docs/planning/16,
"Evaluation"; docs/planning/24).

| File | What it is |
|---|---|
| `expected.json` | The right answer, in `ClaudeInvoiceAnswer`'s shape |
| `invoice.jpg`, `.png` or `.pdf` | The document |
| `claude.json`, `azure.json`, `google.json` | Recorded answers. Check before committing that none holds a customer's name, address, phone or email |

Only documents that may be public go here: synthetic ones, and public ones whose license allows
it. Real invoices belong to the people who received them. The owner's own live in
`../private/`, which git ignores as a whole: files, answers and notes. They're read by the live
eval on the owner's machine and never reach GitHub or CI.

## How `expected.json` is written

- Values are what the document itself prints. `""` where it prints nothing.
- **No derived values.** A handwritten form whose Amount column holds the line total gets
  `lineTotal` and no `unitPrice`. Nothing is computed or corrected.
- **Crossed-out rows aren't lines.**
- **Shipping, handling and discounts printed as rows among the items stay lines.** `shipping` is
  filled only when it's printed outside the item rows, next to the subtotal and total.
- **Other papers in the photo aren't the invoice.** Till receipts, card slips and sheets showing
  through from underneath don't change the answer. Plan 24 adds payments, which will score them.
- **A value hidden in the photo is `""`.** It's scored on what can be seen.
- **A date with no year printed** gets the true year. The case's notes say where it comes from.
- **When "Retail" and "Your Price" are both printed,** the unit price is the one charged.
