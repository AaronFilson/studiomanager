-- Firing-fee schedules. A schedule is never edited: saving a change inserts the next version,
-- and every piece keeps the version its fee was priced with, so any past fee can be explained.
--
-- One rule per version. Rates are decimals in the studio's currency, per one unit of the rule:
--   PER_VOLUME  rate per cubic inch, cubic centimetre or litre, after rounding each dimension
--   PER_WEIGHT  rate per pound or kilogram
--   FLAT        rate per piece

CREATE TABLE fee_schedule (
    id           uuid          PRIMARY KEY,
    studio_id    uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    version      integer       NOT NULL CHECK (version > 0),
    rule_type    text          NOT NULL CHECK (rule_type IN ('PER_VOLUME', 'PER_WEIGHT', 'FLAT')),
    rate         numeric(12,4) NOT NULL CHECK (rate >= 0),
    unit         text          CHECK (unit IN ('CUBIC_INCH', 'CUBIC_CENTIMETRE', 'LITRE', 'POUND', 'KILOGRAM')),
    rounding     text          NOT NULL CHECK (rounding IN ('NONE', 'HALF_UNIT', 'WHOLE_UNIT')),
    minimum      numeric(12,2) NOT NULL CHECK (minimum >= 0),
    tax_category text          NOT NULL CHECK (char_length(tax_category) BETWEEN 1 AND 40),
    created_by   uuid          NOT NULL REFERENCES account (id),
    created_at   timestamptz   NOT NULL,
    CONSTRAINT fee_schedule_studio_version_key UNIQUE (studio_id, version),
    CONSTRAINT fee_schedule_unit_matches_rule CHECK (
        (rule_type = 'PER_VOLUME' AND unit IN ('CUBIC_INCH', 'CUBIC_CENTIMETRE', 'LITRE'))
        OR (rule_type = 'PER_WEIGHT' AND unit IN ('POUND', 'KILOGRAM'))
        OR (rule_type = 'FLAT' AND unit IS NULL))
);

GRANT SELECT, INSERT ON fee_schedule TO app_rw;

ALTER TABLE fee_schedule ENABLE ROW LEVEL SECURITY;

CREATE POLICY fee_schedule_in_tenant ON fee_schedule
    FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id()))
    WITH CHECK (studio_id = (SELECT app.current_studio_id()));
