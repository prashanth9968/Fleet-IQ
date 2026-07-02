-- V020__partitioning.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. Register partitioned tables in pg_partman for automated partition pre-creation
-- geofence_events
SELECT partman.create_parent(
    p_parent_table := 'public.geofence_events',
    p_control := 'event_at',
    p_type := 'range',
    p_interval := '1 month',
    p_premake := 3
);

-- notification_log
SELECT partman.create_parent(
    p_parent_table := 'public.notification_log',
    p_control := 'sent_at',
    p_type := 'range',
    p_interval := '1 month',
    p_premake := 3
);

-- analytics_aggregates_daily
SELECT partman.create_parent(
    p_parent_table := 'public.analytics_aggregates_daily',
    p_control := 'agg_date',
    p_type := 'range',
    p_interval := '1 month',
    p_premake := 3
);

-- audit_logs
SELECT partman.create_parent(
    p_parent_table := 'public.audit_logs',
    p_control := 'created_at',
    p_type := 'range',
    p_interval := '1 month',
    p_premake := 3
);

-- login_audit_logs
SELECT partman.create_parent(
    p_parent_table := 'public.login_audit_logs',
    p_control := 'created_at',
    p_type := 'range',
    p_interval := '1 month',
    p_premake := 3
);


-- 2. Schedule pg_partman maintenance job via pg_cron
-- Runs daily at 3:00 AM UTC to generate partitions 3 months ahead and apply retention if configured
SELECT cron.schedule(
    'partman-maintenance',
    '0 3 * * *',
    $$SELECT partman.run_maintenance()$$
);


-- 3. Procedure to detach partitions concurrently (for non-blocking archival)
CREATE OR REPLACE FUNCTION fn_detach_old_partition(
    p_parent_table TEXT,
    p_partition_name TEXT
) RETURNS VOID AS $$
BEGIN
    -- Detach partition concurrently to allow concurrent reads/writes on other partitions
    EXECUTE format('ALTER TABLE %I DETACH PARTITION %I CONCURRENTLY', p_parent_table, p_partition_name);
    RAISE NOTICE 'Partition % successfully detached from parent table %', p_partition_name, p_parent_table;
END;
$$ LANGUAGE plpgsql;


COMMENT ON FUNCTION fn_detach_old_partition(TEXT, TEXT) IS 'Safely and concurrently detaches a partition from its parent table for archiving';
