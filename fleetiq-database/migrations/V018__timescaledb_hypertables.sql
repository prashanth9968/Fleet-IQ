-- V018__timescaledb_hypertables.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. Convert tables to TimescaleDB hypertables
-- gps_readings: 4-hour chunks, space partitioned by vehicle_id (64 partitions)
SELECT create_hypertable('gps_readings', 'recorded_at', chunk_time_interval => INTERVAL '4 hours');
SELECT add_dimension('gps_readings', 'vehicle_id', number_partitions => 64);

-- fuel_readings: 4-hour chunks, space partitioned by vehicle_id (64 partitions)
SELECT create_hypertable('fuel_readings', 'recorded_at', chunk_time_interval => INTERVAL '4 hours');
SELECT add_dimension('fuel_readings', 'vehicle_id', number_partitions => 64);

-- obd_readings: 6-hour chunks, space partitioned by vehicle_id (32 partitions)
SELECT create_hypertable('obd_readings', 'recorded_at', chunk_time_interval => INTERVAL '6 hours');
SELECT add_dimension('obd_readings', 'vehicle_id', number_partitions => 32);

-- driving_events: 1-day chunks, space partitioned by vehicle_id (16 partitions)
SELECT create_hypertable('driving_events', 'event_at', chunk_time_interval => INTERVAL '1 day');
SELECT add_dimension('driving_events', 'vehicle_id', number_partitions => 16);

-- battery_health_readings: 1-day chunks, space partitioned by vehicle_id (16 partitions)
SELECT create_hypertable('battery_health_readings', 'recorded_at', chunk_time_interval => INTERVAL '1 day');
SELECT add_dimension('battery_health_readings', 'vehicle_id', number_partitions => 16);


-- 2. Configure hypertable compression policies (24 hours for telemetry, 48 hours for events)
-- gps_readings
ALTER TABLE gps_readings SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'vehicle_id, tenant_id',
    timescaledb.compress_orderby = 'recorded_at DESC'
);
SELECT add_compression_policy('gps_readings', INTERVAL '24 hours');

-- fuel_readings
ALTER TABLE fuel_readings SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'vehicle_id, tenant_id',
    timescaledb.compress_orderby = 'recorded_at DESC'
);
SELECT add_compression_policy('fuel_readings', INTERVAL '24 hours');

-- obd_readings
ALTER TABLE obd_readings SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'vehicle_id, tenant_id',
    timescaledb.compress_orderby = 'recorded_at DESC'
);
SELECT add_compression_policy('obd_readings', INTERVAL '24 hours');

-- driving_events
ALTER TABLE driving_events SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'vehicle_id, tenant_id',
    timescaledb.compress_orderby = 'event_at DESC'
);
SELECT add_compression_policy('driving_events', INTERVAL '48 hours');

-- battery_health_readings
ALTER TABLE battery_health_readings SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'vehicle_id, tenant_id',
    timescaledb.compress_orderby = 'recorded_at DESC'
);
SELECT add_compression_policy('battery_health_readings', INTERVAL '48 hours');


-- 3. Define Continuous Aggregates
-- Continuous Aggregate: gps_readings_hourly
CREATE MATERIALIZED VIEW gps_readings_hourly
WITH (timescaledb.continuous) AS
SELECT
    vehicle_id,
    tenant_id,
    time_bucket('1 hour', recorded_at) AS bucket,
    AVG(speed_kmh) AS avg_speed_kmh,
    MAX(speed_kmh) AS max_speed_kmh,
    MIN(speed_kmh) AS min_speed_kmh,
    COUNT(*) AS point_count,
    FIRST(latitude, recorded_at) AS first_lat,
    FIRST(longitude, recorded_at) AS first_lng,
    LAST(latitude, recorded_at) AS last_lat,
    LAST(longitude, recorded_at) AS last_lng,
    (MAX(odometer_km) - MIN(odometer_km)) AS distance_km
FROM gps_readings
GROUP BY vehicle_id, tenant_id, bucket
WITH NO DATA;

SELECT add_continuous_aggregate_policy('gps_readings_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour'
);

-- Continuous Aggregate: gps_readings_daily
CREATE MATERIALIZED VIEW gps_readings_daily
WITH (timescaledb.continuous) AS
SELECT
    vehicle_id,
    tenant_id,
    time_bucket('1 day', recorded_at) AS bucket,
    AVG(speed_kmh) AS avg_speed_kmh,
    MAX(speed_kmh) AS max_speed_kmh,
    MIN(speed_kmh) AS min_speed_kmh,
    COUNT(*) AS point_count,
    FIRST(latitude, recorded_at) AS first_lat,
    FIRST(longitude, recorded_at) AS first_lng,
    LAST(latitude, recorded_at) AS last_lat,
    LAST(longitude, recorded_at) AS last_lng,
    (MAX(odometer_km) - MIN(odometer_km)) AS distance_km
FROM gps_readings
GROUP BY vehicle_id, tenant_id, bucket
WITH NO DATA;

SELECT add_continuous_aggregate_policy('gps_readings_daily',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '6 hours'
);

-- Continuous Aggregate: fuel_readings_hourly
CREATE MATERIALIZED VIEW fuel_readings_hourly
WITH (timescaledb.continuous) AS
SELECT
    vehicle_id,
    tenant_id,
    time_bucket('1 hour', recorded_at) AS bucket,
    AVG(fuel_level_litres) AS avg_fuel_level,
    MIN(fuel_level_litres) AS min_fuel_level,
    MAX(fuel_level_litres) AS max_fuel_level,
    AVG(consumption_rate_lpm) AS avg_consumption_rate,
    MAX(consumption_rate_lpm) AS max_consumption_rate,
    COUNT(*) AS reading_count
FROM fuel_readings
GROUP BY vehicle_id, tenant_id, bucket
WITH NO DATA;

SELECT add_continuous_aggregate_policy('fuel_readings_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour'
);

-- Continuous Aggregate: fuel_readings_daily
CREATE MATERIALIZED VIEW fuel_readings_daily
WITH (timescaledb.continuous) AS
SELECT
    vehicle_id,
    tenant_id,
    time_bucket('1 day', recorded_at) AS bucket,
    AVG(fuel_level_litres) AS avg_fuel_level,
    MIN(fuel_level_litres) AS min_fuel_level,
    MAX(fuel_level_litres) AS max_fuel_level,
    AVG(consumption_rate_lpm) AS avg_consumption_rate,
    MAX(consumption_rate_lpm) AS max_consumption_rate,
    COUNT(*) AS reading_count
FROM fuel_readings
GROUP BY vehicle_id, tenant_id, bucket
WITH NO DATA;

SELECT add_continuous_aggregate_policy('fuel_readings_daily',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '6 hours'
);

-- Continuous Aggregate: obd_readings_hourly
CREATE MATERIALIZED VIEW obd_readings_hourly
WITH (timescaledb.continuous) AS
SELECT
    vehicle_id,
    tenant_id,
    time_bucket('1 hour', recorded_at) AS bucket,
    AVG(engine_rpm) AS avg_engine_rpm,
    MAX(engine_rpm) AS max_engine_rpm,
    AVG(coolant_temp_c) AS avg_coolant_temp,
    MAX(coolant_temp_c) AS max_coolant_temp,
    AVG(battery_voltage) AS avg_battery_voltage,
    MIN(battery_voltage) AS min_battery_voltage,
    AVG(engine_load_pct) AS avg_engine_load,
    COUNT(*) AS reading_count
FROM obd_readings
GROUP BY vehicle_id, tenant_id, bucket
WITH NO DATA;

SELECT add_continuous_aggregate_policy('obd_readings_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour'
);


-- 4. Indexes on Continuous Aggregates for rapid lookup
CREATE INDEX idx_gps_hourly_veh_bucket ON gps_readings_hourly(vehicle_id, bucket DESC);
CREATE INDEX idx_gps_hourly_tenant_bucket ON gps_readings_hourly(tenant_id, bucket DESC);

CREATE INDEX idx_gps_daily_veh_bucket ON gps_readings_daily(vehicle_id, bucket DESC);
CREATE INDEX idx_gps_daily_tenant_bucket ON gps_readings_daily(tenant_id, bucket DESC);

CREATE INDEX idx_fuel_hourly_veh_bucket ON fuel_readings_hourly(vehicle_id, bucket DESC);
CREATE INDEX idx_fuel_hourly_tenant_bucket ON fuel_readings_hourly(tenant_id, bucket DESC);

CREATE INDEX idx_fuel_daily_veh_bucket ON fuel_readings_daily(vehicle_id, bucket DESC);
CREATE INDEX idx_fuel_daily_tenant_bucket ON fuel_readings_daily(tenant_id, bucket DESC);

CREATE INDEX idx_obd_hourly_veh_bucket ON obd_readings_hourly(vehicle_id, bucket DESC);
CREATE INDEX idx_obd_hourly_tenant_bucket ON obd_readings_hourly(tenant_id, bucket DESC);

-- Comments
COMMENT ON MATERIALIZED VIEW gps_readings_hourly IS 'TimescaleDB continuous aggregate summarizing hourly speed, distance, and coordinates per vehicle';
COMMENT ON MATERIALIZED VIEW gps_readings_daily IS 'TimescaleDB continuous aggregate summarizing daily speed, distance, and coordinates per vehicle';
COMMENT ON MATERIALIZED VIEW fuel_readings_hourly IS 'TimescaleDB continuous aggregate summarizing hourly fuel levels and usage per vehicle';
COMMENT ON MATERIALIZED VIEW fuel_readings_daily IS 'TimescaleDB continuous aggregate summarizing daily fuel levels and usage per vehicle';
COMMENT ON MATERIALIZED VIEW obd_readings_hourly IS 'TimescaleDB continuous aggregate summarizing hourly OBD-II diagnostics per vehicle';
