-- Invoice answer schema v2: document type, dates with year provenance, payments, crossed-out
-- rows, other papers, reader notes, photo rotation, and per-line kind, serial numbers and
-- unclear flag (docs/planning/24-invoice-reading-upgrade-plan.md, "Answer schema v2").

ALTER TABLE invoice
    ADD COLUMN document_type        text    CHECK (document_type IN ('INVOICE', 'ORDER_FORM', 'RECEIPT',
                                                                      'PACKING_LIST', 'CREDIT_MEMO',
                                                                      'STATEMENT', 'QUOTE', 'OTHER')),
    ADD COLUMN order_number         text    CHECK (char_length(order_number) <= 60),
    ADD COLUMN due_date             date,
    ADD COLUMN amount_paid          numeric(12,2),
    ADD COLUMN amount_due           numeric(12,2),
    ADD COLUMN invoice_date_printed text    CHECK (char_length(invoice_date_printed) <= 60),
    ADD COLUMN invoice_date_year    text    CHECK (invoice_date_year IN ('PRINTED', 'FROM_PAGE', 'ASSUMED')),
    ADD COLUMN payments             jsonb   NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN crossed_out          jsonb   NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN other_papers         text    CHECK (char_length(other_papers) <= 300),
    ADD COLUMN notes                jsonb   NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN rotation             integer CHECK (rotation IN (0, 90, 180, 270));

ALTER TABLE invoice_line
    ADD COLUMN kind           text    NOT NULL DEFAULT 'ITEM'
        CHECK (kind IN ('ITEM', 'SHIPPING', 'FEE', 'DISCOUNT', 'DEPOSIT', 'OTHER')),
    ADD COLUMN serial_numbers jsonb   NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN unclear        boolean NOT NULL DEFAULT false;
