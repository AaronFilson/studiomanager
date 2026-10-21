-- Spare parts used in a repair, drawn from supplies (docs/planning/06-wave2-maintenance-plan.md,
-- next slices). The research found the cost of a repair kept as one number, with no link to the
-- spare element taken from stock. Each part used is a USED stock movement in supplies; this table
-- links it to the record, so the history says which element went in, and stock goes down.

CREATE TABLE maintenance_record_part (
    record_id uuid          NOT NULL REFERENCES maintenance_record (id) ON DELETE CASCADE,
    studio_id uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    item_id   uuid          NOT NULL REFERENCES supply_item (id),
    quantity  numeric(12,3) NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (record_id, item_id)
);

GRANT SELECT, INSERT ON maintenance_record_part TO app_rw;

ALTER TABLE maintenance_record_part ENABLE ROW LEVEL SECURITY;

CREATE POLICY maintenance_record_part_in_tenant ON maintenance_record_part FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
