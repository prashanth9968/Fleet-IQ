CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS geofences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- POLYGON, CIRCLE, ROUTE
    geom geometry(Geometry, 4326) NOT NULL,
    max_speed_kmh DECIMAL(6,2),
    max_dwell_minutes INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_geofences_tenant ON geofences(tenant_id);
CREATE INDEX idx_geofences_geom ON geofences USING GIST (geom);

CREATE TABLE IF NOT EXISTS geofence_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    geofence_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- ENTER, EXIT, DWELL_VIOLATION, OVERSPEED
    recorded_at TIMESTAMPTZ NOT NULL,
    speed_kmh DECIMAL(6,2),
    dwell_duration_seconds INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

SELECT create_hypertable('geofence_events', 'recorded_at', if_not_exists => true);
CREATE INDEX idx_geofence_events_vehicle ON geofence_events(vehicle_id, recorded_at DESC);
CREATE INDEX idx_geofence_events_tenant ON geofence_events(tenant_id, recorded_at DESC);
