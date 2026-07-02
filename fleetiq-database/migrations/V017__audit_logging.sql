-- V017__audit_logging.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. audit_logs (Partitioned table - no foreign keys to partitioned tables)
CREATE TABLE audit_logs (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id UUID,
    user_id UUID,
    action VARCHAR(20) NOT NULL CHECK (action IN ('INSERT', 'UPDATE', 'DELETE')),
    table_name VARCHAR(100) NOT NULL,
    record_id UUID NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    request_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create initial partitions for audit_logs (2026-01 through 2027-06)
CREATE TABLE audit_logs_y2026m01 PARTITION OF audit_logs FOR VALUES FROM ('2026-01-01 00:00:00+00') TO ('2026-02-01 00:00:00+00');
CREATE TABLE audit_logs_y2026m02 PARTITION OF audit_logs FOR VALUES FROM ('2026-02-01 00:00:00+00') TO ('2026-03-01 00:00:00+00');
CREATE TABLE audit_logs_y2026m03 PARTITION OF audit_logs FOR VALUES FROM ('2026-03-01 00:00:00+00') TO ('2026-04-01 00:00:00+00');
CREATE TABLE audit_logs_y2026m04 PARTITION OF audit_logs FOR VALUES FROM ('2026-04-01 00:00:00+00') TO ('2026-05-01 00:00:00+00');
CREATE TABLE audit_logs_y2026m05 PARTITION OF audit_logs FOR VALUES FROM ('2026-05-01 00:00:00+00') TO ('2026-06-01 00:00:00+00');
CREATE TABLE audit_logs_y2026m06 PARTITION OF audit_logs FOR VALUES FROM ('2026-06-01 00:00:00+00') TO ('2026-07-01 00:00:00+00');
CREATE TABLE audit_logs_y2026m07 PARTITION OF audit_logs FOR VALUES FROM ('2026-07-01 00:00:00+00') TO ('2026-08-01 00:00:00+00');
CREATE TABLE audit_logs_y2026m08 PARTITION OF audit_logs FOR VALUES FROM ('2026-08-01 00:00:00+00') TO ('2026-09-01 00:00:00+00');
CREATE TABLE audit_logs_y2026m09 PARTITION OF audit_logs FOR VALUES FROM ('2026-09-01 00:00:00+00') TO ('2026-10-01 00:00:00+00');
CREATE TABLE audit_logs_y2026m10 PARTITION OF audit_logs FOR VALUES FROM ('2026-10-01 00:00:00+00') TO ('2026-11-01 00:00:00+00');
CREATE TABLE audit_logs_y2026m11 PARTITION OF audit_logs FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE audit_logs_y2026m12 PARTITION OF audit_logs FOR VALUES FROM ('2026-12-01 00:00:00+00') TO ('2027-01-01 00:00:00+00');
CREATE TABLE audit_logs_y2027m01 PARTITION OF audit_logs FOR VALUES FROM ('2027-01-01 00:00:00+00') TO ('2027-02-01 00:00:00+00');
CREATE TABLE audit_logs_y2027m02 PARTITION OF audit_logs FOR VALUES FROM ('2027-02-01 00:00:00+00') TO ('2027-03-01 00:00:00+00');
CREATE TABLE audit_logs_y2027m03 PARTITION OF audit_logs FOR VALUES FROM ('2027-03-01 00:00:00+00') TO ('2027-04-01 00:00:00+00');
CREATE TABLE audit_logs_y2027m04 PARTITION OF audit_logs FOR VALUES FROM ('2027-04-01 00:00:00+00') TO ('2027-05-01 00:00:00+00');
CREATE TABLE audit_logs_y2027m05 PARTITION OF audit_logs FOR VALUES FROM ('2027-05-01 00:00:00+00') TO ('2027-06-01 00:00:00+00');
CREATE TABLE audit_logs_y2027m06 PARTITION OF audit_logs FOR VALUES FROM ('2027-06-01 00:00:00+00') TO ('2027-07-01 00:00:00+00');

-- 2. login_audit_logs (Partitioned table - no foreign keys)
CREATE TABLE login_audit_logs (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID,
    email VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN (
        'LOGIN_SUCCESS', 'LOGIN_FAILURE', 'LOGOUT', 'PASSWORD_RESET_REQUEST', 
        'PASSWORD_RESET_SUCCESS', 'MFA_SETUP', 'MFA_VERIFY_SUCCESS', 'MFA_VERIFY_FAILURE'
    )),
    ip_address INET,
    user_agent TEXT,
    device_info JSONB,
    failure_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create initial partitions for login_audit_logs (2026-01 through 2027-06)
CREATE TABLE login_audit_logs_y2026m01 PARTITION OF login_audit_logs FOR VALUES FROM ('2026-01-01 00:00:00+00') TO ('2026-02-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2026m02 PARTITION OF login_audit_logs FOR VALUES FROM ('2026-02-01 00:00:00+00') TO ('2026-03-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2026m03 PARTITION OF login_audit_logs FOR VALUES FROM ('2026-03-01 00:00:00+00') TO ('2026-04-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2026m04 PARTITION OF login_audit_logs FOR VALUES FROM ('2026-04-01 00:00:00+00') TO ('2026-05-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2026m05 PARTITION OF login_audit_logs FOR VALUES FROM ('2026-05-01 00:00:00+00') TO ('2026-06-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2026m06 PARTITION OF login_audit_logs FOR VALUES FROM ('2026-06-01 00:00:00+00') TO ('2026-07-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2026m07 PARTITION OF login_audit_logs FOR VALUES FROM ('2026-07-01 00:00:00+00') TO ('2026-08-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2026m08 PARTITION OF login_audit_logs FOR VALUES FROM ('2026-08-01 00:00:00+00') TO ('2026-09-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2026m09 PARTITION OF login_audit_logs FOR VALUES FROM ('2026-09-01 00:00:00+00') TO ('2026-10-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2026m10 PARTITION OF login_audit_logs FOR VALUES FROM ('2026-10-01 00:00:00+00') TO ('2026-11-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2026m11 PARTITION OF login_audit_logs FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2026m12 PARTITION OF login_audit_logs FOR VALUES FROM ('2026-12-01 00:00:00+00') TO ('2027-01-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2027m01 PARTITION OF login_audit_logs FOR VALUES FROM ('2027-01-01 00:00:00+00') TO ('2027-02-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2027m02 PARTITION OF login_audit_logs FOR VALUES FROM ('2027-02-01 00:00:00+00') TO ('2027-03-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2027m03 PARTITION OF login_audit_logs FOR VALUES FROM ('2027-03-01 00:00:00+00') TO ('2027-04-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2027m04 PARTITION OF login_audit_logs FOR VALUES FROM ('2027-04-01 00:00:00+00') TO ('2027-05-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2027m05 PARTITION OF login_audit_logs FOR VALUES FROM ('2027-05-01 00:00:00+00') TO ('2027-06-01 00:00:00+00');
CREATE TABLE login_audit_logs_y2027m06 PARTITION OF login_audit_logs FOR VALUES FROM ('2027-06-01 00:00:00+00') TO ('2027-07-01 00:00:00+00');

-- 3. data_access_logs
CREATE TABLE data_access_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resource_type VARCHAR(50) NOT NULL, -- e.g. driver_pii, vehicle_location, fuel_data
    resource_id UUID,
    access_type VARCHAR(20) NOT NULL CHECK (access_type IN ('VIEW', 'EXPORT', 'SHARE', 'PRINT')),
    fields_accessed JSONB, -- JSON array of string fields accessed
    purpose VARCHAR(255),
    ip_address INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for Partitioned audit_logs
CREATE INDEX idx_audit_logs_created_at_brin ON audit_logs USING BRIN(created_at);
CREATE INDEX idx_audit_logs_tenant_table ON audit_logs(tenant_id, table_name, created_at DESC);
CREATE INDEX idx_audit_logs_tenant_user ON audit_logs(tenant_id, user_id, created_at DESC);
CREATE INDEX idx_audit_logs_record ON audit_logs(record_id, table_name);

-- Indexes for Partitioned login_audit_logs
CREATE INDEX idx_login_audit_logs_created_at_brin ON login_audit_logs USING BRIN(created_at);
CREATE INDEX idx_login_audit_logs_user ON login_audit_logs(user_id, created_at DESC);
CREATE INDEX idx_login_audit_logs_email_type ON login_audit_logs(email, event_type);
CREATE INDEX idx_login_audit_logs_ip ON login_audit_logs(ip_address);

-- Indexes for data_access_logs
CREATE INDEX idx_data_access_logs_user ON data_access_logs(tenant_id, user_id, created_at DESC);
CREATE INDEX idx_data_access_logs_resource ON data_access_logs(tenant_id, resource_type, resource_id);

-- Audit trigger function using SECURITY DEFINER to bypass table permissions
CREATE OR REPLACE FUNCTION fn_audit_trigger()
RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    INSERT INTO audit_logs (tenant_id, user_id, action, table_name, record_id, new_values, created_at)
    VALUES (
      COALESCE(current_setting('app.current_tenant_id', true)::UUID, NULL),
      COALESCE(current_setting('app.current_user_id', true)::UUID, NULL),
      'INSERT', TG_TABLE_NAME, NEW.id, to_jsonb(NEW), NOW()
    );
    RETURN NEW;
  ELSIF TG_OP = 'UPDATE' THEN
    INSERT INTO audit_logs (tenant_id, user_id, action, table_name, record_id, old_values, new_values, ip_address, request_id, created_at)
    VALUES (
      COALESCE(current_setting('app.current_tenant_id', true)::UUID, NULL),
      COALESCE(current_setting('app.current_user_id', true)::UUID, NULL),
      'UPDATE', TG_TABLE_NAME, NEW.id, to_jsonb(OLD), to_jsonb(NEW),
      COALESCE(current_setting('app.current_ip', true)::INET, NULL),
      COALESCE(current_setting('app.current_request_id', true), NULL),
      NOW()
    );
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' THEN
    INSERT INTO audit_logs (tenant_id, user_id, action, table_name, record_id, old_values, created_at)
    VALUES (
      COALESCE(current_setting('app.current_tenant_id', true)::UUID, NULL),
      COALESCE(current_setting('app.current_user_id', true)::UUID, NULL),
      'DELETE', TG_TABLE_NAME, OLD.id, to_jsonb(OLD), NOW()
    );
    RETURN OLD;
  END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Attach audit triggers to sensitive tables
CREATE TRIGGER audit_trg_tenants AFTER INSERT OR UPDATE OR DELETE ON tenants FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
CREATE TRIGGER audit_trg_users AFTER INSERT OR UPDATE OR DELETE ON users FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
CREATE TRIGGER audit_trg_vehicles AFTER INSERT OR UPDATE OR DELETE ON vehicles FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
CREATE TRIGGER audit_trg_drivers AFTER INSERT OR UPDATE OR DELETE ON drivers FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
CREATE TRIGGER audit_trg_geofences AFTER INSERT OR UPDATE OR DELETE ON geofences FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
CREATE TRIGGER audit_trg_alerts AFTER INSERT OR UPDATE OR DELETE ON alerts FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
CREATE TRIGGER audit_trg_work_orders AFTER INSERT OR UPDATE OR DELETE ON work_orders FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
CREATE TRIGGER audit_trg_fuel_events AFTER INSERT OR UPDATE OR DELETE ON fuel_events FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
CREATE TRIGGER audit_trg_notification_rules AFTER INSERT OR UPDATE OR DELETE ON notification_rules FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
CREATE TRIGGER audit_trg_api_keys AFTER INSERT OR UPDATE OR DELETE ON api_keys FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
CREATE TRIGGER audit_trg_documents AFTER INSERT OR UPDATE OR DELETE ON documents FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();

-- Comments
COMMENT ON TABLE audit_logs IS 'Partitioned read-only trail of database-level inserts, updates, and deletes for compliance';
COMMENT ON TABLE login_audit_logs IS 'Partitioned transaction log tracking authentication success and failure events';
COMMENT ON TABLE data_access_logs IS 'Access audit trail for tracing who viewed, exported, or printed sensitive driver/vehicle information';
COMMENT ON FUNCTION fn_audit_trigger() IS 'Centralized audit trigger logging database changes with current session metadata';
