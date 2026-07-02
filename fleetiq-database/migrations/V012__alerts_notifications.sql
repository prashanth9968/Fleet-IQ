-- V012__alerts_notifications.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. alerts
CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    alert_type VARCHAR(50) NOT NULL CHECK (alert_type IN (
        'SPEEDING', 'HARSH_ACCEL', 'HARSH_BRAKE', 'HARSH_CORNER', 'IDLE_TIMEOUT', 
        'GEOFENCE_ENTER', 'GEOFENCE_EXIT', 'FUEL_THEFT', 'FUEL_DRAIN', 'ENGINE_TEMP_HIGH', 
        'BATTERY_VOLTAGE_LOW', 'CHECK_ENGINE_DTC', 'MAINTENANCE_DUE', 'DOCUMENT_EXPIRY', 
        'CRASH_DETECTED', 'SOS_TRIGGERED', 'CAMERA_DISCONNECTED', 'ADAS_FORWARD_COLLISION', 
        'ADAS_LANE_DEPARTURE', 'DMS_DISTRACTION', 'DMS_DROWSINESS', 'CHARGING_FAULT', 
        'BATTERY_TEMP_HIGH', 'OTHER'
    )),
    severity VARCHAR(20) NOT NULL CHECK (severity IN (
        'CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'
    )),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN (
        'OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'AUTO_RESOLVED'
    )),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    address TEXT,
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE CASCADE,
    driver_id UUID REFERENCES drivers(id) ON DELETE SET NULL,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    geofence_id UUID REFERENCES geofences(id) ON DELETE SET NULL,
    trip_id UUID REFERENCES trips(id) ON DELETE SET NULL,
    duration_seconds INT,
    value_measured NUMERIC(12,2),
    value_threshold NUMERIC(12,2),
    metadata JSONB NOT NULL DEFAULT '{}',
    acknowledged_by UUID REFERENCES users(id) ON DELETE SET NULL,
    acknowledged_at TIMESTAMPTZ,
    resolved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    resolved_at TIMESTAMPTZ,
    resolved_notes TEXT,
    escalated_to UUID REFERENCES users(id) ON DELETE SET NULL,
    escalated_at TIMESTAMPTZ,
    escalation_level INT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. notification_rules
CREATE TABLE notification_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    alert_type VARCHAR(50) NOT NULL, -- references alerts.alert_type
    severity VARCHAR(20) CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO')), -- NULL = all severities
    vehicle_group_id UUID REFERENCES vehicle_groups(id) ON DELETE SET NULL,
    depot_id UUID REFERENCES depots(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    escalation_timeout_min INT,
    escalation_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. notification_rule_recipients
CREATE TABLE notification_rule_recipients (
    rule_id UUID NOT NULL REFERENCES notification_rules(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_channels JSONB NOT NULL DEFAULT '["EMAIL"]', -- e.g. ["EMAIL", "SMS", "PUSH"]
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (rule_id, user_id)
);

-- 4. notification_channels
CREATE TABLE notification_channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    channel_type VARCHAR(20) NOT NULL CHECK (channel_type IN (
        'EMAIL', 'SMS', 'PUSH', 'WHATSAPP', 'WEBHOOK', 'SLACK'
    )),
    configuration JSONB NOT NULL DEFAULT '{}', -- credentials, routing endpoints etc.
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, channel_type)
);

-- 5. notification_log (Partitioned table - no foreign keys on partitioned tables)
CREATE TABLE notification_log (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    alert_id UUID,
    recipient_user_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN (
        'PUSH', 'SMS', 'EMAIL', 'WHATSAPP', 'WEBHOOK'
    )),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'SENT', 'DELIVERED', 'FAILED', 'BOUNCED'
    )),
    sent_at TIMESTAMPTZ NOT NULL,
    delivered_at TIMESTAMPTZ,
    failure_reason TEXT,
    provider_message_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, sent_at)
) PARTITION BY RANGE (sent_at);

-- Create initial partitions for notification_log (2026-01 through 2027-03)
CREATE TABLE notification_log_y2026m01 PARTITION OF notification_log FOR VALUES FROM ('2026-01-01 00:00:00+00') TO ('2026-02-01 00:00:00+00');
CREATE TABLE notification_log_y2026m02 PARTITION OF notification_log FOR VALUES FROM ('2026-02-01 00:00:00+00') TO ('2026-03-01 00:00:00+00');
CREATE TABLE notification_log_y2026m03 PARTITION OF notification_log FOR VALUES FROM ('2026-03-01 00:00:00+00') TO ('2026-04-01 00:00:00+00');
CREATE TABLE notification_log_y2026m04 PARTITION OF notification_log FOR VALUES FROM ('2026-04-01 00:00:00+00') TO ('2026-05-01 00:00:00+00');
CREATE TABLE notification_log_y2026m05 PARTITION OF notification_log FOR VALUES FROM ('2026-05-01 00:00:00+00') TO ('2026-06-01 00:00:00+00');
CREATE TABLE notification_log_y2026m06 PARTITION OF notification_log FOR VALUES FROM ('2026-06-01 00:00:00+00') TO ('2026-07-01 00:00:00+00');
CREATE TABLE notification_log_y2026m07 PARTITION OF notification_log FOR VALUES FROM ('2026-07-01 00:00:00+00') TO ('2026-08-01 00:00:00+00');
CREATE TABLE notification_log_y2026m08 PARTITION OF notification_log FOR VALUES FROM ('2026-08-01 00:00:00+00') TO ('2026-09-01 00:00:00+00');
CREATE TABLE notification_log_y2026m09 PARTITION OF notification_log FOR VALUES FROM ('2026-09-01 00:00:00+00') TO ('2026-10-01 00:00:00+00');
CREATE TABLE notification_log_y2026m10 PARTITION OF notification_log FOR VALUES FROM ('2026-10-01 00:00:00+00') TO ('2026-11-01 00:00:00+00');
CREATE TABLE notification_log_y2026m11 PARTITION OF notification_log FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE notification_log_y2026m12 PARTITION OF notification_log FOR VALUES FROM ('2026-12-01 00:00:00+00') TO ('2027-01-01 00:00:00+00');
CREATE TABLE notification_log_y2027m01 PARTITION OF notification_log FOR VALUES FROM ('2027-01-01 00:00:00+00') TO ('2027-02-01 00:00:00+00');
CREATE TABLE notification_log_y2027m02 PARTITION OF notification_log FOR VALUES FROM ('2027-02-01 00:00:00+00') TO ('2027-03-01 00:00:00+00');
CREATE TABLE notification_log_y2027m03 PARTITION OF notification_log FOR VALUES FROM ('2027-03-01 00:00:00+00') TO ('2027-04-01 00:00:00+00');

-- Apply updated_at triggers
CREATE TRIGGER trg_alerts_updated_at BEFORE UPDATE ON alerts FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_notification_rules_updated_at BEFORE UPDATE ON notification_rules FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_notification_channels_updated_at BEFORE UPDATE ON notification_channels FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes for performance
CREATE INDEX idx_alerts_open_severity ON alerts(tenant_id, severity, status) WHERE status = 'OPEN';
CREATE INDEX idx_alerts_type_at ON alerts(tenant_id, alert_type, created_at DESC);
CREATE INDEX idx_alerts_veh_at ON alerts(tenant_id, vehicle_id, created_at DESC);
CREATE INDEX idx_alerts_tenant_at ON alerts(tenant_id, created_at DESC);

CREATE INDEX idx_notification_rules_lookup ON notification_rules(tenant_id, alert_type, is_active);

-- Indexes on Partitioned Table (applies to all child partitions)
CREATE INDEX idx_notification_log_alert ON notification_log(tenant_id, alert_id);
CREATE INDEX idx_notification_log_pending ON notification_log(status, sent_at) WHERE status = 'PENDING';
CREATE INDEX idx_notification_log_user_at ON notification_log(tenant_id, recipient_user_id, sent_at DESC);

-- Comments
COMMENT ON TABLE alerts IS 'System-wide active alerts generated by the rules engine';
COMMENT ON TABLE notification_rules IS 'Routing profiles that link alert conditions to target user recipients';
COMMENT ON TABLE notification_rule_recipients IS 'Recipient user registry mapped to alerts routing rules';
COMMENT ON TABLE notification_channels IS 'Vendor endpoints configured for sending notifications (e.g. Twilio, SendGrid)';
COMMENT ON TABLE notification_log IS 'Partitioned transaction log tracking delivery status of dispatched messages';
