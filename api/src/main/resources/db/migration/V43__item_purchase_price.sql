-- Remember the last purchase price paid for each supply item when an invoice is confirmed
-- (docs/planning/24-invoice-reading-upgrade-plan.md).

ALTER TABLE supply_item
    ADD COLUMN last_purchase_unit_price numeric(12,4) CHECK (last_purchase_unit_price >= 0),
    ADD COLUMN last_purchased_at        timestamptz;
