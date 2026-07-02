-- backup_recovery.sql
-- FleetIQ Fleet Management Platform
-- Database Backup, Archive, and Disaster Recovery Procedures

/*
================================================================================
1. WAL ARCHIVING & PHYSICAL BACKUP CONFIGURATION (postgresql.conf)
================================================================================
-- Configure WAL Archiving for continuous protection and Point-in-Time Recovery (PITR)

wal_level = replica
archive_mode = on
archive_command = 'aws s3 cp %p s3://fleetiq-database-backups/wal/%f'
archive_timeout = 600          -- Force WAL segment switch every 10 minutes
max_wal_senders = 10           -- Support replication connections
hot_standby = on               -- Allow read-only queries on replicas
*/

-- Create the backup logging table
CREATE TABLE IF NOT EXISTS backup_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    backup_type VARCHAR(20) NOT NULL, -- FULL, INCREMENTAL, WAL, LOGICAL
    status VARCHAR(20) NOT NULL,      -- IN_PROGRESS, COMPLETED, FAILED
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    size_bytes BIGINT,
    storage_path TEXT,
    retention_days INT DEFAULT 30,
    verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for log lookups
CREATE INDEX IF NOT EXISTS idx_backup_log_status ON backup_log(status, started_at DESC);


-- 2. PHYSICAL BASE BACKUP INITIATION FUNCTION
-- Note: Requires pg_start_backup() / pg_stop_backup() equivalent for PG 15+ (pg_backup_start / pg_backup_stop)
CREATE OR REPLACE FUNCTION fn_initiate_base_backup(p_label TEXT DEFAULT 'fleetiq_daily')
RETURNS TEXT AS $$
DECLARE
    v_backup_id UUID;
    v_start_lsn PG_LSN;
    v_backup_label TEXT;
BEGIN
    v_backup_id := gen_random_uuid();
    v_backup_label := p_label || '_' || to_char(NOW(), 'YYYYMMDD_HH24MISS');
    
    -- Insert backup start log entry
    INSERT INTO backup_log (id, backup_type, status, started_at, notes)
    VALUES (v_backup_id, 'FULL', 'IN_PROGRESS', NOW(), 'Label: ' || v_backup_label);

    -- Call PG 15+ backup start API (returns start LSN)
    SELECT pg_backup_start(v_backup_label, false) INTO v_start_lsn;
    
    RAISE NOTICE 'Base backup started. Label: %, LSN: %', v_backup_label, v_start_lsn;
    
    -- Return LSN and log ID to the external script executing pg_basebackup or tar
    RETURN json_build_object(
        'backup_log_id', v_backup_id,
        'start_lsn', v_start_lsn::text,
        'label', v_backup_label
    )::text;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- 3. PHYSICAL BASE BACKUP COMPLETION FUNCTION
CREATE OR REPLACE FUNCTION fn_complete_base_backup(
    p_backup_id UUID,
    p_size_bytes BIGINT,
    p_storage_path TEXT,
    p_status TEXT DEFAULT 'COMPLETED'
) RETURNS VOID AS $$
DECLARE
    v_stop_lsn PG_LSN;
BEGIN
    -- Call PG 15+ backup stop API
    SELECT pg_backup_stop(true) INTO v_stop_lsn;
    
    UPDATE backup_log
    SET status = p_status,
        completed_at = NOW(),
        size_bytes = p_size_bytes,
        storage_path = p_storage_path,
        notes = COALESCE(notes, '') || ' | Stopped at LSN: ' || v_stop_lsn::text
    WHERE id = p_backup_id;

    RAISE NOTICE 'Base backup completed. LSN: %', v_stop_lsn;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- 4. TENANT-LEVEL LOGICAL BACKUP/EXPORT FUNCTION
-- Dynamically exports all data related to a single tenant into CSV outputs
CREATE OR REPLACE FUNCTION fn_export_tenant_data(p_tenant_id UUID, p_output_dir TEXT)
RETURNS TABLE(table_name TEXT, row_count BIGINT) AS $$
DECLARE
    r RECORD;
    v_sql TEXT;
    v_rows BIGINT;
    v_backup_id UUID;
BEGIN
    -- Log the export event
    v_backup_id := gen_random_uuid();
    INSERT INTO backup_log (id, backup_type, status, started_at, storage_path, notes)
    VALUES (v_backup_id, 'LOGICAL', 'IN_PROGRESS', NOW(), p_output_dir, 'Logical export for tenant: ' || p_tenant_id::text);

    -- Ensure schema temporary table is created
    CREATE TEMP TABLE IF NOT EXISTS temp_export_stats (
        t_name TEXT,
        r_count BIGINT
    ) ON COMMIT DROP;

    -- Loop through all tables that contain a tenant_id column
    FOR r IN 
        SELECT c.table_name 
        FROM information_schema.columns c
        JOIN information_schema.tables t ON c.table_name = t.table_name AND c.table_schema = t.table_schema
        WHERE c.table_schema = 'public' 
          AND c.column_name = 'tenant_id' 
          AND t.table_type = 'BASE TABLE'
          AND t.table_name NOT IN ('audit_logs', 'login_audit_logs', 'geofence_events', 'notification_log', 'analytics_aggregates_daily') -- skip large partitioned tables for custom dump
    LOOP
        -- Build COPY query to export table data to server disk path (requires superuser/pg_write_server_files)
        v_sql := format('COPY (SELECT * FROM %I WHERE tenant_id = %L) TO %L WITH CSV HEADER', 
                        r.table_name, p_tenant_id, p_output_dir || '/' || r.table_name || '_export.csv');
        
        BEGIN
            EXECUTE v_sql;
            -- Get row count of export
            EXECUTE format('SELECT count(*) FROM %I WHERE tenant_id = %L', r.table_name, p_tenant_id) INTO v_rows;
            INSERT INTO temp_export_stats VALUES (r.table_name, v_rows);
        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING 'Failed to export table %: %', r.table_name, SQLERRM;
        END;
    END LOOP;

    -- Update backup logs status
    UPDATE backup_log
    SET status = 'COMPLETED',
        completed_at = NOW(),
        notes = notes || ' | Exported ' || (SELECT count(*) FROM temp_export_stats)::text || ' tables.'
    WHERE id = v_backup_id;

    RETURN QUERY SELECT t_name, r_count FROM temp_export_stats;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- 5. SCHEDULED MONITORING & RETENTION CLEANUP
-- Purges backup log metadata older than 1 year
CREATE OR REPLACE FUNCTION fn_cleanup_backup_logs()
RETURNS VOID AS $$
BEGIN
    DELETE FROM backup_log WHERE created_at < NOW() - INTERVAL '365 days';
END;
$$ LANGUAGE plpgsql;

-- Schedule log purge weekly
SELECT cron.schedule(
    'backup-log-cleanup',
    '0 4 * * 0', -- 4:00 AM on Sundays
    $$SELECT fn_cleanup_backup_logs()$$
);

/*
================================================================================
6. POINT-IN-TIME RECOVERY (PITR) RUNBOOK & DISASTER RECOVERY GUIDELINE
================================================================================
To restore the database to a specific millisecond in time:

Step 1: Stop the active PostgreSQL service
  $ pg_ctl -D /var/lib/postgresql/data stop

Step 2: Rename current data directory to prevent accidental data loss
  $ mv /var/lib/postgresql/data /var/lib/postgresql/data_broken

Step 3: Extract the latest Base Backup (tarball/physical snapshot) to a fresh directory
  $ mkdir /var/lib/postgresql/data
  $ tar -xf /var/lib/postgresql/backups/base_backup_20260606.tar -C /var/lib/postgresql/data
  $ chmod 700 /var/lib/postgresql/data

Step 4: Create 'signal' file to trigger recovery mode on startup
  For PG 12+ create an empty 'recovery.signal' file in data directory:
  $ touch /var/lib/postgresql/data/recovery.signal

Step 5: Define the recovery target configuration in postgresql.conf or postgresql.auto.conf:
  restore_command = 'aws s3 cp s3://fleetiq-database-backups/wal/%f %p'
  recovery_target_time = '2026-06-06 14:30:00+05:30' -- TARGET TIME (Toll booth outage or corrupt update boundary)
  recovery_target_action = 'promote'                  -- Promotes cluster to primary once target is reached

Step 6: Start PostgreSQL service (it will start in recovery mode, ingest WAL segments from S3, play up to time, and promote)
  $ pg_ctl -D /var/lib/postgresql/data start

Step 7: Monitor postgresql.log to confirm recovery completed successfully:
  "consistent recovery state reached at LSN..."
  "recovery stopping at restore point..."
  "restored backup piece..."
  "database system is ready to accept connections"
*/


/*
================================================================================
7. REPLICA PROMOTION & ACTIVE-STANDBY FAILOVER PROCEDURES
================================================================================
In a physical streaming replication topology, when the primary node crashes:

Step 1: Verify the primary is truly dead (fencing) to avoid split-brain scenario.
Step 2: Log into the standby node.
Step 3: Execute pg_ctl promote command to promote standby to primary:
  $ pg_ctl -D /var/lib/postgresql/data promote
  (Alternatively: SELECT pg_promote(); from an administrative SQL interface)
Step 4: Re-route application load balances (AWS Route53, pgpool, or haproxy) to the newly promoted node's IP.
*/
