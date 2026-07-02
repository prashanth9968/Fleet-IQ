-- V021__row_level_security.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. Create database roles if they do not exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'fleetiq_app') THEN
        CREATE ROLE fleetiq_app;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'fleetiq_readonly') THEN
        CREATE ROLE fleetiq_readonly;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'fleetiq_admin') THEN
        CREATE ROLE fleetiq_admin;
    END IF;
END
$$;

-- 2. PL/pgSQL procedures to automate standard RLS applications across schema
CREATE OR REPLACE PROCEDURE pr_enable_tenant_rls(p_table TEXT, p_tenant_col TEXT DEFAULT 'tenant_id') AS $$
BEGIN
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', p_table);
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', p_table);
    
    EXECUTE format('DROP POLICY IF EXISTS tenant_isolation_policy ON %I', p_table);
    EXECUTE format('DROP POLICY IF EXISTS super_admin_bypass ON %I', p_table);
    
    IF p_tenant_col = 'id' THEN
        EXECUTE format('
            CREATE POLICY tenant_isolation_policy ON %I FOR ALL
            USING (id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)
            WITH CHECK (id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)
        ', p_table);
    ELSIF p_tenant_col = 'tenant_id_nullable' THEN
        EXECUTE format('
            CREATE POLICY tenant_isolation_policy ON %I FOR ALL
            USING (tenant_id IS NULL OR tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)
            WITH CHECK (tenant_id IS NULL OR tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)
        ', p_table);
    ELSE
        EXECUTE format('
            CREATE POLICY tenant_isolation_policy ON %I FOR ALL
            USING (tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)
            WITH CHECK (tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)
        ', p_table);
    END IF;

    EXECUTE format('
        CREATE POLICY super_admin_bypass ON %I FOR ALL
        USING (current_setting(''app.current_user_role'', true) = ''SUPER_ADMIN'')
        WITH CHECK (current_setting(''app.current_user_role'', true) = ''SUPER_ADMIN'')
    ', p_table);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE PROCEDURE pr_enable_indirect_tenant_rls(p_table TEXT, p_join_query TEXT) AS $$
BEGIN
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', p_table);
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', p_table);
    
    EXECUTE format('DROP POLICY IF EXISTS tenant_isolation_policy ON %I', p_table);
    EXECUTE format('DROP POLICY IF EXISTS super_admin_bypass ON %I', p_table);
    
    EXECUTE format('
        CREATE POLICY tenant_isolation_policy ON %I FOR ALL
        USING (%s)
        WITH CHECK (%s)
    ', p_table, p_join_query, p_join_query);
    
    EXECUTE format('
        CREATE POLICY super_admin_bypass ON %I FOR ALL
        USING (current_setting(''app.current_user_role'', true) = ''SUPER_ADMIN'')
        WITH CHECK (current_setting(''app.current_user_role'', true) = ''SUPER_ADMIN'')
    ', p_table);
END;
$$ LANGUAGE plpgsql;


-- 3. Execute procedures on all tenant-scoped tables
CALL pr_enable_tenant_rls('tenants', 'id');
CALL pr_enable_tenant_rls('tenant_subscriptions');
CALL pr_enable_tenant_rls('tenant_settings');
CALL pr_enable_tenant_rls('users');
CALL pr_enable_tenant_rls('api_keys');
CALL pr_enable_tenant_rls('depots');
CALL pr_enable_tenant_rls('vehicle_groups');
CALL pr_enable_tenant_rls('vehicles');
CALL pr_enable_tenant_rls('devices');
CALL pr_enable_tenant_rls('device_twins');
CALL pr_enable_tenant_rls('firmware_deployments', 'tenant_id_nullable');
CALL pr_enable_tenant_rls('drivers');
CALL pr_enable_tenant_rls('driver_assignments');
CALL pr_enable_tenant_rls('driver_safety_scores');
CALL pr_enable_tenant_rls('driving_events');
CALL pr_enable_tenant_rls('gps_readings');
CALL pr_enable_tenant_rls('trips');
CALL pr_enable_tenant_rls('fuel_readings');
CALL pr_enable_tenant_rls('fuel_events');
CALL pr_enable_tenant_rls('fuel_cards');
CALL pr_enable_tenant_rls('fuel_thresholds');
CALL pr_enable_tenant_rls('obd_readings');
CALL pr_enable_tenant_rls('vehicle_dtc_events');
CALL pr_enable_tenant_rls('vehicle_health_scores');
CALL pr_enable_tenant_rls('vehicle_health_history');
CALL pr_enable_tenant_rls('maintenance_schedules');
CALL pr_enable_tenant_rls('maintenance_predictions');
CALL pr_enable_tenant_rls('work_orders');
CALL pr_enable_tenant_rls('geofences');
CALL pr_enable_tenant_rls('geofence_events');
CALL pr_enable_tenant_rls('alerts');
CALL pr_enable_tenant_rls('notification_rules');
CALL pr_enable_tenant_rls('notification_channels');
CALL pr_enable_tenant_rls('notification_log');
CALL pr_enable_tenant_rls('ev_vehicle_details');
CALL pr_enable_tenant_rls('charging_stations', 'tenant_id_nullable');
CALL pr_enable_tenant_rls('charging_sessions');
CALL pr_enable_tenant_rls('battery_health_readings');
CALL pr_enable_tenant_rls('camera_devices');
CALL pr_enable_tenant_rls('video_events');
CALL pr_enable_tenant_rls('media_assets');
CALL pr_enable_tenant_rls('documents');
CALL pr_enable_tenant_rls('document_expiry_alerts');
CALL pr_enable_tenant_rls('analytics_aggregates_daily');
CALL pr_enable_tenant_rls('scheduled_reports');
CALL pr_enable_tenant_rls('report_executions');
CALL pr_enable_tenant_rls('dashboard_widgets');
CALL pr_enable_tenant_rls('audit_logs');

-- Apply RLS to indirect relational tables (mapping tables or child components)
CALL pr_enable_indirect_tenant_rls('user_roles', 'user_id IN (SELECT id FROM users WHERE tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)');
CALL pr_enable_indirect_tenant_rls('user_sessions', 'user_id IN (SELECT id FROM users WHERE tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)');
CALL pr_enable_indirect_tenant_rls('vehicle_group_members', 'vehicle_group_id IN (SELECT id FROM vehicle_groups WHERE tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)');
CALL pr_enable_indirect_tenant_rls('user_depot_assignments', 'user_id IN (SELECT id FROM users WHERE tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)');
CALL pr_enable_indirect_tenant_rls('firmware_deployment_targets', 'device_id IN (SELECT id FROM devices WHERE tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)');
CALL pr_enable_indirect_tenant_rls('trip_segments', 'trip_id IN (SELECT id FROM trips WHERE tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)');
CALL pr_enable_indirect_tenant_rls('work_order_items', 'work_order_id IN (SELECT id FROM work_orders WHERE tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)');
CALL pr_enable_indirect_tenant_rls('geofence_schedules', 'geofence_id IN (SELECT id FROM geofences WHERE tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)');
CALL pr_enable_indirect_tenant_rls('notification_rule_recipients', 'rule_id IN (SELECT id FROM notification_rules WHERE tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)');
CALL pr_enable_indirect_tenant_rls('login_audit_logs', 'user_id IS NULL OR user_id IN (SELECT id FROM users WHERE tenant_id = COALESCE(nullif(current_setting(''app.current_tenant_id'', true), ''''), null)::UUID)');


-- 4. Clean up registration procedures so they are not left in public schema
DROP PROCEDURE pr_enable_tenant_rls(TEXT, TEXT);
DROP PROCEDURE pr_enable_indirect_tenant_rls(TEXT, TEXT);


-- 5. Additional policies for Driver self-access
CREATE POLICY driver_self_access ON drivers
  FOR SELECT
  USING (
    current_setting('app.current_user_role', true) = 'DRIVER'
    AND id = COALESCE(nullif(current_setting('app.current_driver_id', true), ''), null)::UUID
  );

CREATE POLICY driver_self_access ON driver_safety_scores
  FOR SELECT
  USING (
    current_setting('app.current_user_role', true) = 'DRIVER'
    AND driver_id = COALESCE(nullif(current_setting('app.current_driver_id', true), ''), null)::UUID
  );

CREATE POLICY driver_self_access ON driving_events
  FOR SELECT
  USING (
    current_setting('app.current_user_role', true) = 'DRIVER'
    AND driver_id = COALESCE(nullif(current_setting('app.current_driver_id', true), ''), null)::UUID
  );

CREATE POLICY driver_self_access ON trips
  FOR SELECT
  USING (
    current_setting('app.current_user_role', true) = 'DRIVER'
    AND driver_id = COALESCE(nullif(current_setting('app.current_driver_id', true), ''), null)::UUID
  );


-- 6. Role grant permissions
GRANT USAGE ON SCHEMA public TO fleetiq_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO fleetiq_app;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO fleetiq_app;

GRANT USAGE ON SCHEMA public TO fleetiq_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO fleetiq_readonly;
