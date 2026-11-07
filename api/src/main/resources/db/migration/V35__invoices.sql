-- The supplies inbox: supplier invoices read into drafts that staff check and confirm
-- (docs/planning/16-invoice-ocr-plan.md steps 2, 4 and 5; ADR 18).

-- The studio's consent covers every platform-paid reader (Google Document AI, Azure AI Document
-- Intelligence), not only Google, and none of them is a fallback any more: the admin chooses which
-- run for a studio (docs/planning/20, "Invoice readers").
ALTER TABLE ai_provider_key RENAME COLUMN allow_google_fallback TO allow_platform_readers;

-- The job that reads uploaded invoices, as its own system account (ADR 16)
INSERT INTO account (id, issuer, subject, display_name, kind) VALUES
    ('5157e000-0000-4000-8000-000000000007', 'urn:studiomanager:system', 'invoice-reader',
     'System: invoice reader', 'SYSTEM');

CREATE TABLE invoice (
    id              uuid          PRIMARY KEY,
    studio_id       uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    -- UPLOADING until the file is checked; READING while a reader has it; READY when the numbers
    -- add up; NEEDS_LOOK when they don't, or when nobody could read it; CONFIRMED once stock is in
    status          text          NOT NULL CHECK (status IN ('UPLOADING', 'READING', 'READY', 'NEEDS_LOOK', 'CONFIRMED')),
    file_type       text          NOT NULL CHECK (file_type IN ('PDF', 'JPEG', 'PNG')),
    byte_size       bigint        CHECK (byte_size > 0),
    -- Which reader filled the draft; null when staff typed it in
    reader          text          CHECK (reader IN ('CLAUDE', 'GOOGLE_DOCUMENT_AI', 'AZURE_DOCUMENT_INTELLIGENCE')),
    -- Why it wasn't read automatically, shown to staff
    read_note       text          CHECK (char_length(read_note) <= 300),
    vendor_name     text          CHECK (char_length(vendor_name) <= 120),
    invoice_number  text          CHECK (char_length(invoice_number) <= 60),
    invoice_date    date,
    subtotal        numeric(12,2),
    shipping        numeric(12,2),
    tax             numeric(12,2),
    total           numeric(12,2),
    -- The reader's own answer, kept so a mistake can be traced; purged after 90 days
    raw             jsonb,
    uploaded_by     uuid          NOT NULL REFERENCES account (id),
    created_at      timestamptz   NOT NULL,
    confirmed_by    uuid          REFERENCES account (id),
    confirmed_at    timestamptz,
    version         bigint        NOT NULL
);

CREATE INDEX invoice_studio_created_idx ON invoice (studio_id, created_at DESC);

CREATE TABLE invoice_line (
    id              uuid          PRIMARY KEY,
    studio_id       uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    invoice_id      uuid          NOT NULL REFERENCES invoice (id) ON DELETE CASCADE,
    position        integer       NOT NULL CHECK (position >= 0),
    description     text          NOT NULL CHECK (char_length(description) BETWEEN 1 AND 200),
    sku             text          CHECK (char_length(sku) <= 60),
    quantity        numeric(12,3),
    unit            text          CHECK (char_length(unit) <= 30),
    unit_price      numeric(12,4),
    line_total      numeric(12,2),
    -- The supply item it receives into; null for lines that aren't stock (a pallet fee)
    supply_item_id  uuid          REFERENCES supply_item (id) ON DELETE SET NULL,
    -- How the item was found: SKU or DESCRIPTION from an alias, NAME by similarity, STAFF by hand
    match_source    text          CHECK (match_source IN ('ALIAS_SKU', 'ALIAS_DESCRIPTION', 'NAME', 'STAFF'))
);

-- Not unique on position: saving a draft replaces its lines, and Hibernate inserts the new ones
-- before it deletes the old
CREATE INDEX invoice_line_invoice_idx ON invoice_line (invoice_id, position);

-- Each time a reader ran on an invoice: what it cost and how long it took. The platform-paid
-- readers' monthly page caps are counted from here.
CREATE TABLE invoice_read (
    id              uuid          PRIMARY KEY,
    studio_id       uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    invoice_id      uuid          NOT NULL REFERENCES invoice (id) ON DELETE CASCADE,
    reader          text          NOT NULL CHECK (reader IN ('CLAUDE', 'GOOGLE_DOCUMENT_AI', 'AZURE_DOCUMENT_INTELLIGENCE')),
    outcome         text          NOT NULL CHECK (outcome IN ('READ', 'REFUSED_CREDENTIALS', 'UNAVAILABLE', 'UNREADABLE')),
    model           text          CHECK (char_length(model) <= 80),
    pages           integer       CHECK (pages >= 0),
    input_tokens    bigint        CHECK (input_tokens >= 0),
    output_tokens   bigint        CHECK (output_tokens >= 0),
    cost_usd        numeric(10,6) CHECK (cost_usd >= 0),
    elapsed_ms      bigint        NOT NULL CHECK (elapsed_ms >= 0),
    created_at      timestamptz   NOT NULL
);

CREATE INDEX invoice_read_studio_month_idx ON invoice_read (studio_id, reader, created_at);

-- What a vendor's line means in this studio's supplies, learned when staff confirm an invoice.
-- Keyed by the vendor and either the line's SKU or its normalized description.
CREATE TABLE vendor_alias (
    id              uuid          PRIMARY KEY,
    studio_id       uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    vendor_key      text          NOT NULL CHECK (char_length(vendor_key) BETWEEN 1 AND 120),
    kind            text          NOT NULL CHECK (kind IN ('SKU', 'DESCRIPTION')),
    line_key        text          NOT NULL CHECK (char_length(line_key) BETWEEN 1 AND 200),
    supply_item_id  uuid          NOT NULL REFERENCES supply_item (id) ON DELETE CASCADE,
    learned_by      uuid          NOT NULL REFERENCES account (id),
    learned_at      timestamptz   NOT NULL,
    CONSTRAINT vendor_alias_key UNIQUE (studio_id, vendor_key, kind, line_key)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON invoice, invoice_line, vendor_alias TO app_rw;
GRANT SELECT, INSERT ON invoice_read TO app_rw;

ALTER TABLE invoice ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoice_line ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoice_read ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_alias ENABLE ROW LEVEL SECURITY;

CREATE POLICY invoice_in_tenant ON invoice FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY invoice_line_in_tenant ON invoice_line FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY invoice_read_in_tenant ON invoice_read FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY vendor_alias_in_tenant ON vendor_alias FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
