-- V008__fuel_monitoring.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. fuel_readings (Hypertable source - no foreign keys to avoid Timescale constraints)
CREATE TABLE fuel_readings (
    vehicle_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    device_id UUID NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fuel_level_litres NUMERIC(8,2),
    fuel_level_pct NUMERIC(5,2),
    consumption_rate_lpm NUMERIC(6,3), -- Litres per minute
    engine_rpm INT,
    ignition BOOLEAN,
    speed_kmh NUMERIC(6,2),
    PRIMARY KEY (vehicle_id, recorded_at)
);

-- 2. fuel_events
CREATE TABLE fuel_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    driver_id UUID REFERENCES drivers(id) ON DELETE SET NULL,
    event_type VARCHAR(30) NOT NULL CHECK (event_type IN (
        'FILL', 'THEFT_SUSPECTED', 'DRAIN', 'CONSUMPTION_ALERT'
    )),
    event_at TIMESTAMPTZ NOT NULL,
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    address TEXT,
    fuel_level_before_litres NUMERIC(8,2),
    fuel_level_after_litres NUMERIC(8,2),
    volume_litres NUMERIC(8,2),
    fuel_cost NUMERIC(10,2),
    fuel_station_name VARCHAR(255),
    confidence_score NUMERIC(5,2), -- 0-100 for theft events
    consumption_rate_lpm NUMERIC(6,3), -- for consumption alerts
    ignition_state BOOLEAN,
    vehicle_speed_kmh NUMERIC(6,2),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'CONFIRMED', 'DISMISSED', 'AUTO_RESOLVED'
    )),
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    review_notes TEXT,
    receipt_image_url TEXT,
    fuel_card_transaction_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. fuel_cards
CREATE TABLE fuel_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE SET NULL,
    card_number VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL CHECK (provider IN (
        'HPCL', 'BPCL', 'IOCL', 'SHELL', 'OTHER'
    )),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN (
        'ACTIVE', 'INACTIVE', 'BLOCKED'
    )),
    daily_limit NUMERIC(10,2),
    monthly_limit NUMERIC(10,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, card_number)
);

-- 4. fuel_thresholds
CREATE TABLE fuel_thresholds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_type_id UUID REFERENCES vehicle_types(id) ON DELETE CASCADE,
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE CASCADE, -- overrides type-level if set
    consumption_rate_threshold_lpm NUMERIC(6,3) NOT NULL DEFAULT 1.0,
    theft_drop_threshold_litres NUMERIC(6,2) NOT NULL DEFAULT 5.0,
    theft_time_window_seconds INT NOT NULL DEFAULT 120,
    fill_rise_threshold_litres NUMERIC(6,2) NOT NULL DEFAULT 10.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Apply updated_at triggers
CREATE TRIGGER trg_fuel_events_updated_at BEFORE UPDATE ON fuel_events FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_fuel_cards_updated_at BEFORE UPDATE ON fuel_cards FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_fuel_thresholds_updated_at BEFORE UPDATE ON fuel_thresholds FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes for performance
CREATE INDEX idx_fuel_events_veh_at ON fuel_events(tenant_id, vehicle_id, event_at DESC);
CREATE INDEX idx_fuel_events_type_status ON fuel_events(tenant_id, event_type, status);
CREATE INDEX idx_fuel_events_at ON fuel_events(tenant_id, event_at DESC);
CREATE INDEX idx_fuel_cards_veh ON fuel_cards(tenant_id, vehicle_id);

-- Comments
COMMENT ON TABLE fuel_readings IS 'Raw high-frequency fuel level data from tank level sensors';
COMMENT ON TABLE fuel_events IS 'Transactions and anomalies relating to fuel fills, drains, and thefts';
COMMENT ON TABLE fuel_cards IS 'Fleet fuel cards linked to specific vehicles with limits';
COMMENT ON TABLE fuel_thresholds IS 'Configured limits for detecting abnormal fuel consumption or thefts';
