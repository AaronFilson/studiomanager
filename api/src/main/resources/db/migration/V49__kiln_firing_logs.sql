-- Optional firing-log telemetry imported from kiln controllers (Bartlett Genesis, Orton/TAP,
-- or generic CSV). Columns stay nullable so existing firings and manually logged loads are
-- unaffected; firing already carries RLS and app_rw grants from V6.

ALTER TABLE firing
    ADD COLUMN peak_temp         numeric(7,2) CHECK (peak_temp >= 0),
    ADD COLUMN temp_unit         text         CHECK (temp_unit IN ('F', 'C')),
    ADD COLUMN duration_minutes  integer      CHECK (duration_minutes >= 0),
    ADD COLUMN error_code        text         CHECK (char_length(error_code) <= 40),
    ADD COLUMN log_source        text         CHECK (log_source IN ('GENESIS', 'TAP', 'OTHER')),
    ADD COLUMN temperature_curve jsonb;
