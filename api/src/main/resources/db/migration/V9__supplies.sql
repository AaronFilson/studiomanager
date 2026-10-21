-- Clay and supplies: what the studio stocks, how much is on hand, and buying more.
--
-- Not general inventory: the research found ordering in 30 of 39 manager postings and 30 of 40
-- technician postings, with no tool named and par levels kept on paper ("maintain 3-4 barrels",
-- "notify the manager when materials are low"). So: items with a par level, a ledger of stock
-- movements, purchase requests that someone approves, and clay sold to members on their balance.

CREATE TABLE supply_item (
    id               uuid          PRIMARY KEY,
    studio_id        uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    name             text          NOT NULL CHECK (char_length(name) BETWEEN 1 AND 80),
    category         text          NOT NULL CHECK (category IN ('CLAY', 'GLAZE', 'GLAZE_MATERIAL', 'TOOL', 'KILN_SUPPLY',
                                                                'CLEANING', 'OTHER')),
    -- What one unit is: "25 lb bag", "kg", "pint"
    unit             text          NOT NULL CHECK (char_length(unit) BETWEEN 1 AND 30),
    supplier         text          CHECK (char_length(supplier) <= 80),
    lead_time_days   integer       CHECK (lead_time_days BETWEEN 0 AND 365),
    -- Reorder when stock falls to this
    par_level        numeric(12,3) CHECK (par_level >= 0),
    reorder_quantity numeric(12,3) CHECK (reorder_quantity > 0),
    -- Set when members can buy it (bags of clay, tools)
    sale_price       numeric(12,2) CHECK (sale_price >= 0),
    tax_category     text          CHECK (char_length(tax_category) BETWEEN 1 AND 40),
    -- What to tell members to use while it's out, such as another clay body
    substitute       text          CHECK (char_length(substitute) <= 120),
    archived         boolean       NOT NULL DEFAULT false,
    version          bigint        NOT NULL,
    CONSTRAINT supply_item_studio_name_key UNIQUE (studio_id, name)
);

CREATE TABLE stock_movement (
    id         uuid          PRIMARY KEY,
    studio_id  uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    item_id    uuid          NOT NULL REFERENCES supply_item (id) ON DELETE CASCADE,
    kind       text          NOT NULL CHECK (kind IN ('RECEIVED', 'SOLD', 'USED', 'COUNTED')),
    -- Signed: received is positive, sold and used negative, a count's correction either way
    quantity   numeric(12,3) NOT NULL CHECK (quantity <> 0),
    member_id  uuid          REFERENCES account (id),
    note       text          CHECK (char_length(note) <= 200),
    created_by uuid          NOT NULL REFERENCES account (id),
    created_at timestamptz   NOT NULL
);

CREATE INDEX stock_movement_item_idx ON stock_movement (item_id, created_at);

CREATE TABLE purchase_request (
    id           uuid          PRIMARY KEY,
    studio_id    uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    item_id      uuid          NOT NULL REFERENCES supply_item (id) ON DELETE CASCADE,
    quantity     numeric(12,3) NOT NULL CHECK (quantity > 0),
    note         text          CHECK (char_length(note) <= 200),
    status       text          NOT NULL CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'ORDERED', 'RECEIVED')),
    requested_by uuid          NOT NULL REFERENCES account (id),
    requested_at timestamptz   NOT NULL,
    decided_by   uuid          REFERENCES account (id),
    decided_at   timestamptz,
    version      bigint        NOT NULL
);

CREATE INDEX purchase_request_status_idx ON purchase_request (studio_id, status);

-- Sales of supplies to members post to the ledger as their own kind of entry
ALTER TABLE journal_entry DROP CONSTRAINT journal_entry_kind_check;
ALTER TABLE journal_entry ADD CONSTRAINT journal_entry_kind_check CHECK (kind IN (
    'PIECE_FEE', 'RETAIL_SALE', 'PAYMENT', 'STORED_VALUE_SALE', 'STORED_VALUE_USE', 'STORED_VALUE_EXPIRY', 'PAYOUT',
    'REFUND', 'ADJUSTMENT'));

GRANT SELECT, INSERT, UPDATE ON supply_item, purchase_request TO app_rw;
GRANT SELECT, INSERT ON stock_movement TO app_rw;

ALTER TABLE supply_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE stock_movement ENABLE ROW LEVEL SECURITY;
ALTER TABLE purchase_request ENABLE ROW LEVEL SECURITY;

CREATE POLICY supply_item_in_tenant ON supply_item FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY stock_movement_in_tenant ON stock_movement FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY purchase_request_in_tenant ON purchase_request FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
