-- V011__geofencing.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. geofences
CREATE TABLE geofences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    boundary GEOMETRY(Geometry, 4326) NOT NULL, -- PostGIS geometry (polygon, circular approximation, route string)
    type VARCHAR(30) NOT NULL CHECK (type IN ('POLYGON', 'CIRCLE', 'LINE_STRING', 'ROUTE')),
    center_latitude NUMERIC(10,7),
    center_longitude NUMERIC(10,7),
    radius_meters NUMERIC(8,2), -- utilized for circular geofences
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    category VARCHAR(30) NOT NULL DEFAULT 'OTHER' CHECK (category IN (
        'DEPOT', 'CUSTOMER_SITE', 'NO_GO_ZONE', 'BORDER', 'CHECKPOINT', 'OTHER'
    )),
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- 2. geofence_schedules
CREATE TABLE geofence_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    geofence_id UUID NOT NULL REFERENCES geofences(id) ON DELETE CASCADE,
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7), -- 1 = Monday, 7 = Sunday
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. geofence_events (Partitioned table - no foreign keys to partitioned tables in standard RDBMS usually, or to simplify partition detachment)
CREATE TABLE geofence_events (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    driver_id UUID,
    geofence_id UUID NOT NULL,
    event_type VARCHAR(20) NOT NULL CHECK (event_type IN ('ENTER', 'EXIT')),
    event_at TIMESTAMPTZ NOT NULL,
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    duration_seconds INT, -- calculated on exit event
    trip_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, event_at)
) PARTITION BY RANGE (event_at);

-- Create initial partitions for geofence_events (2026-01 through 2027-03)
CREATE TABLE geofence_events_y2026m01 PARTITION OF geofence_events FOR VALUES FROM ('2026-01-01 00:00:00+00') TO ('2026-02-01 00:00:00+00');
CREATE TABLE geofence_events_y2026m02 PARTITION OF geofence_events FOR VALUES FROM ('2026-02-01 00:00:00+00') TO ('2026-03-01 00:00:00+00');
CREATE TABLE geofence_events_y2026m03 PARTITION OF geofence_events FOR VALUES FROM ('2026-03-01 00:00:00+00') TO ('2026-04-01 00:00:00+00');
CREATE TABLE geofence_events_y2026m04 PARTITION OF geofence_events FOR VALUES FROM ('2026-04-01 00:00:00+00') TO ('2026-05-01 00:00:00+00');
CREATE TABLE geofence_events_y2026m05 PARTITION OF geofence_events FOR VALUES FROM ('2026-05-01 00:00:00+00') TO ('2026-06-01 00:00:00+00');
CREATE TABLE geofence_events_y2026m06 PARTITION OF geofence_events FOR VALUES FROM ('2026-06-01 00:00:00+00') TO ('2026-07-01 00:00:00+00');
CREATE TABLE geofence_events_y2026m07 PARTITION OF geofence_events FOR VALUES FROM ('2026-07-01 00:00:00+00') TO ('2026-08-01 00:00:00+00');
CREATE TABLE geofence_events_y2026m08 PARTITION OF geofence_events FOR VALUES FROM ('2026-08-01 00:00:00+00') TO ('2026-09-01 00:00:00+00');
CREATE TABLE geofence_events_y2026m09 PARTITION OF geofence_events FOR VALUES FROM ('2026-09-01 00:00:00+00') TO ('2026-10-01 00:00:00+00');
CREATE TABLE geofence_events_y2026m10 PARTITION OF geofence_events FOR VALUES FROM ('2026-10-01 00:00:00+00') TO ('2026-11-01 00:00:00+00');
CREATE TABLE geofence_events_y2026m11 PARTITION OF geofence_events FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE geofence_events_y2026m12 PARTITION OF geofence_events FOR VALUES FROM ('2026-12-01 00:00:00+00') TO ('2027-01-01 00:00:00+00');
CREATE TABLE geofence_events_y2027m01 PARTITION OF geofence_events FOR VALUES FROM ('2027-01-01 00:00:00+00') TO ('2027-02-01 00:00:00+00');
CREATE TABLE geofence_events_y2027m02 PARTITION OF geofence_events FOR VALUES FROM ('2027-02-01 00:00:00+00') TO ('2027-03-01 00:00:00+00');
CREATE TABLE geofence_events_y2027m03 PARTITION OF geofence_events FOR VALUES FROM ('2027-03-01 00:00:00+00') TO ('2027-04-01 00:00:00+00');

-- Apply updated_at triggers
CREATE TRIGGER trg_geofences_updated_at BEFORE UPDATE ON geofences FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_geofence_schedules_updated_at BEFORE UPDATE ON geofence_schedules FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes for performance
CREATE INDEX idx_geofences_tenant_active ON geofences(tenant_id, is_active);
CREATE INDEX idx_geofences_boundary ON geofences USING GIST(boundary);
CREATE INDEX idx_geofences_category ON geofences(tenant_id, category);

CREATE INDEX idx_geofence_schedules_day ON geofence_schedules(geofence_id, day_of_week);

-- Indexes on Partitioned Table (applies to all child partitions)
CREATE INDEX idx_geofence_events_tenant_veh ON geofence_events(tenant_id, vehicle_id, event_at DESC);
CREATE INDEX idx_geofence_events_tenant_geo ON geofence_events(tenant_id, geofence_id, event_at DESC);
CREATE INDEX idx_geofence_events_type_at ON geofence_events(tenant_id, event_type, event_at DESC);

-- Comments
COMMENT ON TABLE geofences IS 'PostGIS spatial boundaries defining operational and restricted zones';
COMMENT ON TABLE geofence_schedules IS 'Time and day-of-week active windows for geofence enforcement';
COMMENT ON TABLE geofence_events IS 'Partitioned event logs recording vehicles entering or leaving geofenced areas';
