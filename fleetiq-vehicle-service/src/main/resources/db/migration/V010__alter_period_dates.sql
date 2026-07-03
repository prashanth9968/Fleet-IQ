-- V010__alter_period_dates.sql
-- Change period_start and period_end from DATE to TIMESTAMPTZ to match Java Instant type

ALTER TABLE driver_safety_scores 
  ALTER COLUMN period_start TYPE TIMESTAMPTZ USING period_start::TIMESTAMPTZ,
  ALTER COLUMN period_end TYPE TIMESTAMPTZ USING period_end::TIMESTAMPTZ;
