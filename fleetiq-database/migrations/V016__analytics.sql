-- V016__analytics.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. analytics_aggregates_daily (Partitioned table - no foreign keys to partitioned tables)
CREATE TABLE analytics_aggregates_daily (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    vehicle_id UUID,
    driver_id UUID,
    agg_date DATE NOT NULL,
    metric_type VARCHAR(50) NOT NULL CHECK (metric_type IN (
        'DISTANCE', 'DURATION', 'FUEL_CONSUMED', 'HARSH_ACCEL_COUNT', 'HARSH_BRAKE_COUNT', 
        'HARSH_CORNER_COUNT', 'SPEEDING_DURATION', 'IDLE_DURATION', 'ENERGY_CONSUMED_EV', 
        'CHARGING_DURATION_EV', 'TRIP_COUNT', 'SAFETY_SCORE', 'FUEL_COST', 'MAINTENANCE_COST'
    )),
    value NUMERIC(16,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, agg_date)
) PARTITION BY RANGE (agg_date);

-- Create initial partitions for analytics_aggregates_daily (2026-01 through 2027-03)
CREATE TABLE analytics_aggregates_daily_y2026m01 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE analytics_aggregates_daily_y2026m02 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE analytics_aggregates_daily_y2026m03 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE analytics_aggregates_daily_y2026m04 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE analytics_aggregates_daily_y2026m05 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE analytics_aggregates_daily_y2026m06 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE analytics_aggregates_daily_y2026m07 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE analytics_aggregates_daily_y2026m08 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE analytics_aggregates_daily_y2026m09 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
CREATE TABLE analytics_aggregates_daily_y2026m10 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
CREATE TABLE analytics_aggregates_daily_y2026m11 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE analytics_aggregates_daily_y2026m12 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');
CREATE TABLE analytics_aggregates_daily_y2027m01 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2027-01-01') TO ('2027-02-01');
CREATE TABLE analytics_aggregates_daily_y2027m02 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2027-02-01') TO ('2027-03-01');
CREATE TABLE analytics_aggregates_daily_y2027m03 PARTITION OF analytics_aggregates_daily FOR VALUES FROM ('2027-03-01') TO ('2027-04-01');

-- 2. scheduled_reports
CREATE TABLE scheduled_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    report_type VARCHAR(50) NOT NULL CHECK (report_type IN (
        'FLEET_SUMMARY', 'TRIP_HISTORY', 'FUEL_USAGE', 'DRIVER_BEHAVIOUR', 
        'GEOFENCE_ACTIVITY', 'MAINTENANCE_LOG', 'ALERT_LOG', 'EV_ANALYTICS'
    )),
    parameters JSONB NOT NULL DEFAULT '{}',
    cron_expression VARCHAR(50) NOT NULL,
    format VARCHAR(10) NOT NULL CHECK (format IN ('PDF', 'CSV', 'XLSX')),
    recipients JSONB NOT NULL DEFAULT '[]', -- JSON array of email strings
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- 3. report_executions
CREATE TABLE report_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scheduled_report_id UUID REFERENCES scheduled_reports(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'RUNNING', 'COMPLETED', 'FAILED'
    )),
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    file_url TEXT,
    file_size_bytes BIGINT,
    error_message TEXT,
    triggered_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. dashboard_widgets
CREATE TABLE dashboard_widgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    widget_type VARCHAR(50) NOT NULL, -- MAP, SCORE_LEADERBOARD, VEHICLE_STATUS_PIE, ALERTS_COUNT, FUEL_CONSUMED_TREND etc.
    x_position INT NOT NULL,
    y_position INT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    configuration JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Apply updated_at triggers
CREATE TRIGGER trg_scheduled_reports_updated_at BEFORE UPDATE ON scheduled_reports FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_dashboard_widgets_updated_at BEFORE UPDATE ON dashboard_widgets FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes on Partitioned Table (applies to all child partitions)
CREATE INDEX idx_analytics_aggregates_daily_lookup ON analytics_aggregates_daily(tenant_id, metric_type, agg_date DESC);
CREATE INDEX idx_analytics_aggregates_daily_veh ON analytics_aggregates_daily(tenant_id, vehicle_id, agg_date DESC) WHERE vehicle_id IS NOT NULL;
CREATE INDEX idx_analytics_aggregates_daily_drv ON analytics_aggregates_daily(tenant_id, driver_id, agg_date DESC) WHERE driver_id IS NOT NULL;

-- Regular indexes
CREATE INDEX idx_scheduled_reports_active ON scheduled_reports(tenant_id, is_active);
CREATE INDEX idx_scheduled_reports_type ON scheduled_reports(tenant_id, report_type);

CREATE INDEX idx_report_executions_status ON report_executions(tenant_id, status);
CREATE INDEX idx_report_executions_report ON report_executions(scheduled_report_id);

CREATE INDEX idx_dashboard_widgets_user ON dashboard_widgets(tenant_id, user_id);

-- Comments
COMMENT ON TABLE analytics_aggregates_daily IS 'Partitioned pre-computed daily rollups of fleet performance metrics for analytics dashboards';
COMMENT ON TABLE scheduled_reports IS 'Report generation profiles with cron schedule specifications';
COMMENT ON TABLE report_executions IS 'Individual outputs and error details of generated PDF/Excel reports';
COMMENT ON TABLE dashboard_widgets IS 'Layout coordinates and parameters of user-defined analytical dashboard tiles';
