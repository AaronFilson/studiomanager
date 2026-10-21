-- Firings on the calendar (docs/planning/02-wave1-plan.md, M3's leftovers). When a kiln starts
-- firing, any calendar resource that is that kiln gets a FIRING booking: 24 hours at first, then
-- trimmed to when it was unloaded, or removed if the load was cancelled. The booking points at
-- its firing so it can be found again.

ALTER TABLE booking ADD COLUMN firing_id uuid UNIQUE REFERENCES firing (id) ON DELETE CASCADE;

ALTER TABLE booking ADD CONSTRAINT booking_firing_kind CHECK (firing_id IS NULL OR kind = 'FIRING');

-- The app can now trim a firing's booking when the kiln is unloaded
GRANT UPDATE ON booking TO app_rw;
