-- V007__gps_telemetry.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. gps_readings (Hypertable source - no foreign keys to avoid Timescale constraints)
CREATE TABLE gps_readings (
    vehicle_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    device_id UUID NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    latitude NUMERIC(10,7) NOT NULL,
    longitude NUMERIC(10,7) NOT NULL,
    location GEOGRAPHY(Point, 4326), -- Auto-computed by trigger
    altitude NUMERIC(8,2),
    speed_kmh NUMERIC(6,2),
    heading NUMERIC(5,2),
    hdop NUMERIC(4,2),
    satellites SMALLINT,
    ignition BOOLEAN,
    odometer_km NUMERIC(12,2),
    signal_strength SMALLINT,
    is_buffered BOOLEAN NOT NULL DEFAULT FALSE,
    metadata JSONB,
    PRIMARY KEY (vehicle_id, recorded_at)
);

-- 2. trips
CREATE TABLE trips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    driver_id UUID REFERENCES drivers(id) ON DELETE SET NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    start_latitude NUMERIC(10,7),
    start_longitude NUMERIC(10,7),
    start_address TEXT,
    end_latitude NUMERIC(10,7),
    end_longitude NUMERIC(10,7),
    end_address TEXT,
    distance_km NUMERIC(10,2),
    max_speed_kmh NUMERIC(6,2),
    avg_speed_kmh NUMERIC(6,2),
    fuel_consumed_litres NUMERIC(8,2),
    fuel_efficiency_kmpl NUMERIC(6,2),
    fuel_cost NUMERIC(10,2),
    idle_duration_seconds INT,
    driving_duration_seconds INT,
    total_duration_seconds INT,
    harsh_accel_count INT DEFAULT 0,
    harsh_brake_count INT DEFAULT 0,
    harsh_corner_count INT DEFAULT 0,
    speeding_count INT DEFAULT 0,
    speeding_duration_seconds INT DEFAULT 0,
    energy_consumed_kwh NUMERIC(8,2), -- for EVs
    energy_recovered_kwh NUMERIC(8,2), -- regen braking
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN (
        'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
    )),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. trip_segments
CREATE TABLE trip_segments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    segment_order INT NOT NULL,
    start_lat NUMERIC(10,7) NOT NULL,
    start_lng NUMERIC(10,7) NOT NULL,
    end_lat NUMERIC(10,7) NOT NULL,
    end_lng NUMERIC(10,7) NOT NULL,
    distance_km NUMERIC(8,2),
    duration_seconds INT,
    avg_speed_kmh NUMERIC(6,2),
    road_name VARCHAR(255),
    speed_limit_kmh NUMERIC(6,2),
    segment_type VARCHAR(20) CHECK (segment_type IN ('DRIVING', 'IDLE', 'STOPPED')),
    encoded_polyline TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Trigger to auto-compute PostGIS geography from latitude and longitude
CREATE OR REPLACE FUNCTION fn_gps_readings_set_location()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
        NEW.location := ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326)::geography;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_gps_readings_location
BEFORE INSERT ON gps_readings
FOR EACH ROW EXECUTE FUNCTION fn_gps_readings_set_location();

-- Apply updated_at trigger to trips
CREATE TRIGGER trg_trips_updated_at BEFORE UPDATE ON trips FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes for performance
CREATE INDEX idx_trips_tenant_veh_started ON trips(tenant_id, vehicle_id, started_at);
CREATE INDEX idx_trips_tenant_started ON trips(tenant_id, started_at);
CREATE INDEX idx_trips_tenant_drv_started ON trips(tenant_id, driver_id, started_at);
CREATE INDEX idx_trips_tenant_status ON trips(tenant_id, status);

CREATE INDEX idx_trip_segments_trip_order ON trip_segments(trip_id, segment_order);

-- Comments
COMMENT ON TABLE gps_readings IS 'Raw telemetry data points logged by vehicle trackers (spatial point stream)';
COMMENT ON TABLE trips IS 'Aggregated driving trips with metrics like fuel consumed, distance, speed, and safety event counts';
COMMENT ON TABLE trip_segments IS 'Individual parts of a trip, broken down by state (driving, idling, stopped)';
