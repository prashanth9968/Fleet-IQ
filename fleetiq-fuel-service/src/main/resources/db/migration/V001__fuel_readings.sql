CREATE TABLE IF NOT EXISTS fuel_readings (
    id UUID DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    fuel_level_litres DECIMAL(8,2),
    fuel_rate_litres_per_min DECIMAL(8,4),
    odometer_km DECIMAL(12,2),
    speed_kmh DECIMAL(6,2),
    engine_rpm INTEGER,
    ignition BOOLEAN DEFAULT true,
    source VARCHAR(50) DEFAULT 'processed.telemetry',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (vehicle_id, recorded_at)
);

-- TimescaleDB hypertable
SELECT create_hypertable('fuel_readings', 'recorded_at', if_not_exists => true);

CREATE INDEX idx_fuel_readings_tenant ON fuel_readings(tenant_id, recorded_at DESC);
CREATE INDEX idx_fuel_readings_vehicle ON fuel_readings(vehicle_id, recorded_at DESC);
