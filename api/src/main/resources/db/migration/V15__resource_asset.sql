-- A calendar resource can be a maintained asset (a kiln, a slab roller, the spray booth), so that
-- while the asset is out of service it can't be booked, except for its repair or a closure
-- (docs/planning/06-wave2-maintenance-plan.md, next slices).

ALTER TABLE resource ADD COLUMN asset_id uuid REFERENCES asset (id) ON DELETE SET NULL;
