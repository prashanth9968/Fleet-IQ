-- maintenance_procedures.sql
-- FleetIQ Fleet Management Platform
-- Database Routine Maintenance & Health Check Functions

-- 1. Routine VACUUM & ANALYZE on Non-Partitioned, High-Write Tables
CREATE OR REPLACE FUNCTION fn_vacuum_analyze_all()
RETURNS VOID AS $$
DECLARE
    r RECORD;
BEGIN
    -- Only run vacuum on standard non-partitioned tables with heavy inserts/updates
    FOR r IN 
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'public' 
          AND table_type = 'BASE TABLE'
          AND table_name NOT IN (
              'gps_readings', 'fuel_readings', 'obd_readings', 'driving_events', 'battery_health_readings', -- handled by TimescaleDB
              'geofence_events', 'notification_log', 'analytics_aggregates_daily', 'audit_logs', 'login_audit_logs' -- partitioned tables
          )
    LOOP
        EXECUTE format('VACUUM ANALYZE public.%I', r.table_name);
    END LOOP;
END;
$$ LANGUAGE plpgsql;


-- 2. Detect & Reindex Bloated Indexes
-- Identifies B-tree indexes where size exceeds a normal threshold (e.g. over 30% bloat)
CREATE OR REPLACE FUNCTION fn_reindex_bloated()
RETURNS TABLE(index_name TEXT, table_name TEXT, bloat_pct INT) AS $$
DECLARE
    r RECORD;
BEGIN
    -- Using simple query to find index size vs table size to flag possible bloat candidates
    -- Reindexes them concurrently to prevent blocking queries
    FOR r IN 
        SELECT
            i.relname AS idx_name,
            t.relname AS tbl_name,
            (pg_relation_size(i.oid) * 100 / pg_relation_size(t.oid)) AS ratio
        FROM pg_class t
        JOIN pg_index x ON t.oid = x.indrelid
        JOIN pg_class i ON i.oid = x.indexrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'public'
          AND t.relkind = 'r'
          AND i.relam = 403 -- B-tree index
          AND pg_relation_size(t.oid) > 50000000 -- only look at tables > 50MB
          AND (pg_relation_size(i.oid) * 100 / pg_relation_size(t.oid)) > 80 -- index size is disproportionately large
    LOOP
        BEGIN
            EXECUTE format('REINDEX INDEX CONCURRENTLY public.%I', r.idx_name);
            index_name := r.idx_name::TEXT;
            table_name := r.tbl_name::TEXT;
            bloat_pct := r.ratio::INT;
            RETURN NEXT;
        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING 'Could not reindex %: %', r.idx_name, SQLERRM;
        END;
    END LOOP;
END;
$$ LANGUAGE plpgsql;


-- 3. Size auditing report for database tables and indexes
CREATE OR REPLACE FUNCTION fn_check_table_sizes()
RETURNS TABLE(
    rel_name TEXT,
    rel_type TEXT,
    size_pretty TEXT,
    size_bytes BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.relname::TEXT AS rel_name,
        CASE WHEN c.relkind = 'r' THEN 'TABLE'::TEXT ELSE 'INDEX'::TEXT END AS rel_type,
        pg_size_pretty(pg_relation_size(c.oid))::TEXT AS size_pretty,
        pg_relation_size(c.oid) AS size_bytes
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' 
      AND c.relkind IN ('r', 'i')
      AND c.relname NOT LIKE 'pg_%'
    ORDER BY size_bytes DESC
    LIMIT 30;
END;
$$ LANGUAGE plpgsql;


-- 4. Check TimescaleDB Hypertable Chunk Statistics
CREATE OR REPLACE FUNCTION fn_check_hypertable_stats()
RETURNS TABLE(
    hypertable_name TEXT,
    total_chunks INT,
    compressed_chunks INT,
    uncompressed_size TEXT,
    compressed_size TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        h.hypertable_name::TEXT,
        count(c.chunk_name)::INT AS total_chunks,
        count(CASE WHEN c.is_compressed = TRUE THEN 1 END)::INT AS compressed_chunks,
        pg_size_pretty(sum(c.table_bytes))::TEXT AS uncompressed_size,
        pg_size_pretty(sum(c.compressed_bytes))::TEXT AS compressed_size
    FROM timescaledb_information.hypertables h
    LEFT JOIN timescaledb_information.chunks c ON c.hypertable_name = h.hypertable_name
    GROUP BY h.hypertable_name;
END;
$$ LANGUAGE plpgsql;


-- 5. Audit pg_partman partition pre-creation health
-- Raises alert if child partitions are not pre-created correctly for next month
CREATE OR REPLACE FUNCTION fn_check_partition_health()
RETURNS TABLE(parent_table TEXT, current_partitions_count INT, missing_premade BOOLEAN) AS $$
DECLARE
    r RECORD;
    v_count INT;
    v_premake INT;
BEGIN
    FOR r IN SELECT parent_table, premake FROM partman.part_config LOOP
        EXECUTE format('
            SELECT count(*)::INT 
            FROM pg_inherits i
            JOIN pg_class c ON c.oid = i.inhrelid
            JOIN pg_class p ON p.oid = i.inhparent
            WHERE p.relname = split_part(%L, ''.'', 2)
        ', r.parent_table) INTO v_count;
        
        parent_table := r.parent_table::TEXT;
        current_partitions_count := v_count;
        missing_premade := (v_count < r.premake);
        RETURN NEXT;
    END LOOP;
END;
$$ LANGUAGE plpgsql;


-- 6. Purge Expired User Sessions
CREATE OR REPLACE FUNCTION fn_cleanup_expired_sessions()
RETURNS INT AS $$
DECLARE
    v_deleted INT;
BEGIN
    DELETE FROM user_sessions 
    WHERE expires_at < NOW() 
       OR revoked_at IS NOT NULL;
    
    GET DIAGNOSTICS v_deleted = ROW_COUNT;
    RETURN v_deleted;
END;
$$ LANGUAGE plpgsql;


-- 7. Flag Device Twins as Stale/Offline (no communication in 24h)
CREATE OR REPLACE FUNCTION fn_cleanup_stale_device_twins()
RETURNS INT AS $$
DECLARE
    v_updated INT;
BEGIN
    UPDATE device_twins
    SET sync_status = 'UNKNOWN',
        error_message = 'No communication received from hardware tracker in over 24 hours.'
    WHERE last_seen_at < NOW() - INTERVAL '24 hours'
      AND sync_status != 'UNKNOWN';

    GET DIAGNOSTICS v_updated = ROW_COUNT;
    RETURN v_updated;
END;
$$ LANGUAGE plpgsql;


-- 8. Scan and Update Document Expiry Status
CREATE OR REPLACE FUNCTION fn_update_document_expiry_status()
RETURNS TABLE(expired_count INT, expiring_count INT) AS $$
DECLARE
    v_exp INT := 0;
    v_warn INT := 0;
BEGIN
    -- Mark active documents as EXPIRED
    UPDATE documents
    SET status = 'EXPIRED'
    WHERE expiry_date < CURRENT_DATE
      AND status NOT IN ('EXPIRED', 'REJECTED');
    GET DIAGNOSTICS v_exp = ROW_COUNT;

    -- Mark active documents as EXPIRING (e.g. within 30 days)
    UPDATE documents
    SET status = 'EXPIRING'
    WHERE expiry_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '30 days'
      AND status = 'ACTIVE';
    GET DIAGNOSTICS v_warn = ROW_COUNT;

    expired_count := v_exp;
    expiring_count := v_warn;
    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;


-- 9. Recalculate and Flag Overdue Maintenance schedules
CREATE OR REPLACE FUNCTION fn_recalculate_maintenance_overdue()
RETURNS INT AS $$
DECLARE
    v_updated INT;
BEGIN
    -- Join active maintenance schedules with latest vehicle readings to check triggers
    UPDATE maintenance_schedules ms
    SET is_overdue = TRUE
    FROM vehicles v
    WHERE ms.vehicle_id = v.id
      AND ms.status = 'ACTIVE'
      AND ms.is_overdue = FALSE
      AND (
          (ms.next_due_date IS NOT NULL AND ms.next_due_date < CURRENT_DATE)
          OR (ms.next_due_odometer IS NOT NULL AND ms.next_due_odometer < v.odometer_reading_km)
          OR (ms.next_due_engine_hours IS NOT NULL AND ms.next_due_engine_hours < v.engine_hours)
      );

    GET DIAGNOSTICS v_updated = ROW_COUNT;
    RETURN v_updated;
END;
$$ LANGUAGE plpgsql;


-- 10. Schedule Maintenance Tasks using pg_cron
-- Run vacuum/analyze every Sunday at 1:00 AM UTC
SELECT cron.schedule('vacuum-analyze-job', '0 1 * * 0', $$SELECT fn_vacuum_analyze_all()$$);

-- Clean up expired sessions daily at 2:00 AM UTC
SELECT cron.schedule('expired-sessions-purge-job', '0 2 * * *', $$SELECT fn_cleanup_expired_sessions()$$);

-- Flag stale device twins hourly (at minute 5)
SELECT cron.schedule('stale-devices-check-job', '5 * * * *', $$SELECT fn_cleanup_stale_device_twins()$$);

-- Update document expiry status daily at 2:30 AM UTC
SELECT cron.schedule('document-expiry-update-job', '30 2 * * *', $$SELECT fn_update_document_expiry_status()$$);

-- Recalculate overdue maintenance schedules daily at 3:00 AM UTC
SELECT cron.schedule('maintenance-overdue-recalc-job', '0 3 * * *', $$SELECT fn_recalculate_maintenance_overdue()$$);
