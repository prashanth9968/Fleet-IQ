-- V019__indexes.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. gps_readings hypertable indexes
-- Speeds up "last seen" and historical query paths for individual vehicles
CREATE INDEX IF NOT EXISTS idx_gps_readings_veh_recorded ON gps_readings(vehicle_id, recorded_at DESC);
-- Accelerates cross-vehicle fleet level queries for a specific tenant
CREATE INDEX IF NOT EXISTS idx_gps_readings_tenant_recorded ON gps_readings(tenant_id, recorded_at DESC);
-- PostGIS spatial index for geo-queries (containment, intersection, geofencing)
CREATE INDEX IF NOT EXISTS idx_gps_readings_location ON gps_readings USING GIST(location);

-- 2. fuel_readings hypertable indexes
-- Speeds up fuel level history queries for individual vehicles
CREATE INDEX IF NOT EXISTS idx_fuel_readings_veh_recorded ON fuel_readings(vehicle_id, recorded_at DESC);
-- Accelerates fuel logs scans for a tenant
CREATE INDEX IF NOT EXISTS idx_fuel_readings_tenant_recorded ON fuel_readings(tenant_id, recorded_at DESC);

-- 3. obd_readings hypertable indexes
-- Speeds up sensor dashboards and diagnostics history per vehicle
CREATE INDEX IF NOT EXISTS idx_obd_readings_veh_recorded ON obd_readings(vehicle_id, recorded_at DESC);
-- Accelerates engine metric analytics across tenant fleet
CREATE INDEX IF NOT EXISTS idx_obd_readings_tenant_recorded ON obd_readings(tenant_id, recorded_at DESC);

-- 4. driving_events hypertable indexes
-- Speeds up driver safety alerts history for specific vehicles
CREATE INDEX IF NOT EXISTS idx_driving_events_veh_at ON driving_events(vehicle_id, event_at DESC);
-- Speeds up driver score calculations and reports for a specific driver
CREATE INDEX IF NOT EXISTS idx_driving_events_tenant_driver_at ON driving_events(tenant_id, driver_id, event_at DESC);
-- Accelerates alerts count dashboards grouped by incident types (e.g. speeding, harsh brake)
CREATE INDEX IF NOT EXISTS idx_driving_events_tenant_type_at ON driving_events(tenant_id, event_type, event_at DESC);
-- Links specific safety events back to the driving trip they occurred in
CREATE INDEX IF NOT EXISTS idx_driving_events_trip_at ON driving_events(trip_id, event_at) WHERE trip_id IS NOT NULL;

-- 5. battery_health_readings hypertable indexes
-- Speeds up battery health trend dashboards per EV vehicle
CREATE INDEX IF NOT EXISTS idx_battery_readings_veh_recorded ON battery_health_readings(vehicle_id, recorded_at DESC);
-- Accelerates EV charge status reports across tenant fleet
CREATE INDEX IF NOT EXISTS idx_battery_readings_tenant_recorded ON battery_health_readings(tenant_id, recorded_at DESC);

-- Add database documentation for these indexes
COMMENT ON INDEX idx_gps_readings_veh_recorded IS 'Accelerates queries for GPS telemetry data points on a per-vehicle basis sorted by time';
COMMENT ON INDEX idx_gps_readings_tenant_recorded IS 'Supports tenant-wide mapping dashboard historical queries';
COMMENT ON INDEX idx_gps_readings_location IS 'Enables rapid spatial GIS containment checks using PostGIS geography';
COMMENT ON INDEX idx_fuel_readings_veh_recorded IS 'Supports fuel consumption timeline chart rendering per vehicle';
COMMENT ON INDEX idx_obd_readings_veh_recorded IS 'Supports OBD-II engine metrics timeline chart rendering per vehicle';
COMMENT ON INDEX idx_driving_events_tenant_driver_at IS 'Supports compiling driver safety scoreboards and violation history';
COMMENT ON INDEX idx_driving_events_trip_at IS 'Maps harsh driving events directly to the specific trips they occurred in';
COMMENT ON INDEX idx_battery_readings_veh_recorded IS 'Supports EV battery health and state-of-charge chart rendering';
