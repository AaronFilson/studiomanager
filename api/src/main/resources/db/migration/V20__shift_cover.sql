-- Finding cover for a shift (docs/planning/02-wave1-plan.md, M3's leftovers). Whoever has a
-- shift can offer it; they stay on it until someone else takes it, so the slot is never left
-- uncovered by asking. Members who could work it hear about the offer.

ALTER TABLE shift ADD COLUMN offered_at timestamptz;

ALTER TABLE shift ADD CONSTRAINT shift_offer_needs_holder CHECK (offered_at IS NULL OR account_id IS NOT NULL);

ALTER TABLE notification DROP CONSTRAINT notification_kind_check;
ALTER TABLE notification ADD CONSTRAINT notification_kind_check
    CHECK (kind IN ('PIECES_READY', 'STUDIO_NOTICE', 'MAINTENANCE_DUE', 'PROBLEM_REPORTED', 'SHIFT_COVER'));
