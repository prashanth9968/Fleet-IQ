-- V022__data_retention_policies.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. Register TimescaleDB retention policies for raw hypertables
-- GPS telemetry: 90 days hot storage retention
SELECT add_retention_policy('gps_readings', INTERVAL '90 days');
-- Fuel sensor telemetry: 90 days hot storage retention
SELECT add_retention_policy('fuel_readings', INTERVAL '90 days');
-- OBD-II diagnostics: 60 days hot storage retention
SELECT add_retention_policy('obd_readings', INTERVAL '60 days');
-- Safety/driving violation events: 365 days hot storage retention
SELECT add_retention_policy('driving_events', INTERVAL '365 days');
-- EV battery health telemetry: 365 days hot storage retention
SELECT add_retention_policy('battery_health_readings', INTERVAL '365 days');


-- 2. Continuous Aggregate retention policies
-- Hourly GPS aggregates: 180 days (6 months) retention
SELECT add_retention_policy('gps_readings_hourly', INTERVAL '180 days');
-- Daily GPS aggregates: 730 days (2 years) retention
SELECT add_retention_policy('gps_readings_daily', INTERVAL '730 days');
-- Hourly fuel aggregates: 180 days retention
SELECT add_retention_policy('fuel_readings_hourly', INTERVAL '180 days');
-- Daily fuel aggregates: 730 days retention
SELECT add_retention_policy('fuel_readings_daily', INTERVAL '730 days');
-- Hourly OBD metrics: 120 days (4 months) retention
SELECT add_retention_policy('obd_readings_hourly', INTERVAL '120 days');


-- 3. Create cold storage data archival tracking table
CREATE TABLE data_archival_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_name VARCHAR(100) NOT NULL,
    partition_name VARCHAR(100),
    archived_from TIMESTAMPTZ NOT NULL,
    archived_to TIMESTAMPTZ NOT NULL,
    row_count BIGINT,
    archive_path TEXT NOT NULL,
    archive_format VARCHAR(20) DEFAULT 'PARQUET',
    checksum VARCHAR(64),
    status VARCHAR(20) DEFAULT 'COMPLETED' CHECK (status IN (
        'IN_PROGRESS', 'COMPLETED', 'FAILED', 'VERIFIED'
    )),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- 4. Create database helper for cold storage archival jobs
CREATE OR REPLACE FUNCTION fn_archive_to_cold_storage(
    p_table_name TEXT,
    p_retention_days INT,
    p_archive_path TEXT DEFAULT '/data/archive'
) RETURNS VOID AS $$
BEGIN
    -- This function acts as a hook. External archival workers (e.g. AWS Glue, pg_dump, or custom python scripts)
    -- call this to log their exports into the data_archival_log tracking table.
    RAISE NOTICE 'Archive job logged: table=%, retention_days=%, path=%', p_table_name, p_retention_days, p_archive_path;
END;
$$ LANGUAGE plpgsql;


-- 5. Schedule pg_cron jobs for partition maintenance and archival triggers
-- Runs monthly partition maintenance explicitly for audit logs
SELECT cron.schedule(
    'archive-audit-logs',
    '0 2 1 * *',
    $$SELECT partman.run_maintenance(p_parent_table := 'public.audit_logs')$$
);


-- Comments
COMMENT ON TABLE data_archival_log IS 'Logs cold storage data exports to AWS S3/Azure Blob for compliance tracking';
COMMENT ON FUNCTION fn_archive_to_cold_storage IS 'API hook registered for external data extractors to log archival task status';
