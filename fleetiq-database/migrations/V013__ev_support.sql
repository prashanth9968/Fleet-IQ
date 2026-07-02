-- V013__ev_support.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. ev_vehicle_details
CREATE TABLE ev_vehicle_details (
    vehicle_id UUID PRIMARY KEY REFERENCES vehicles(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    battery_capacity_kwh NUMERIC(8,2) NOT NULL,
    battery_chemistry VARCHAR(50),
    usable_capacity_kwh NUMERIC(8,2),
    max_charging_power_kw NUMERIC(6,2),
    charger_port_type VARCHAR(50), -- CCS2, CHAdeMO, Type 2, GB/T, etc.
    min_soc_pct NUMERIC(5,2) DEFAULT 20.0,
    max_soc_pct NUMERIC(5,2) DEFAULT 80.0,
    optimal_temp_min_c NUMERIC(4,1),
    optimal_temp_max_c NUMERIC(4,1),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. charging_stations
CREATE TABLE charging_stations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE SET NULL, -- Nullable for public networks
    name VARCHAR(255) NOT NULL,
    description TEXT,
    location GEOGRAPHY(Point, 4326),
    address TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'FAULTY', 'INACTIVE')),
    total_ports INT DEFAULT 1,
    charging_modes JSONB DEFAULT '[]', -- AC, DC Fast, etc.
    max_power_output_kw NUMERIC(6,2),
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- 3. charging_sessions
CREATE TABLE charging_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    charging_station_id UUID REFERENCES charging_stations(id) ON DELETE SET NULL,
    driver_id UUID REFERENCES drivers(id) ON DELETE SET NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    start_soc_pct NUMERIC(5,2),
    end_soc_pct NUMERIC(5,2),
    energy_delivered_kwh NUMERIC(8,2),
    max_charging_power_kw NUMERIC(6,2),
    cost NUMERIC(10,2),
    status VARCHAR(20) NOT NULL DEFAULT 'CHARGING' CHECK (status IN (
        'CHARGING', 'COMPLETED', 'FAULTED', 'ABORTED'
    )),
    error_code VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. battery_health_readings (Hypertable source - no foreign keys to avoid Timescale constraints)
CREATE TABLE battery_health_readings (
    vehicle_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    soc_pct NUMERIC(5,2),
    soh_pct NUMERIC(5,2),
    battery_temp_avg_c NUMERIC(4,1),
    battery_temp_max_c NUMERIC(4,1),
    cell_voltage_max_v NUMERIC(4,3),
    cell_voltage_min_v NUMERIC(4,3),
    charge_cycle_count INT,
    remaining_range_km NUMERIC(6,2),
    charger_status VARCHAR(20) CHECK (charger_status IN (
        'DISCONNECTED', 'CONNECTED', 'CHARGING', 'FULL', 'FAULT'
    )),
    power_draw_kw NUMERIC(6,2),
    energy_consumed_wh NUMERIC(12,2),
    regen_energy_wh NUMERIC(12,2),
    PRIMARY KEY (vehicle_id, recorded_at)
);

-- Apply updated_at triggers
CREATE TRIGGER trg_ev_vehicle_details_updated_at BEFORE UPDATE ON ev_vehicle_details FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_charging_stations_updated_at BEFORE UPDATE ON charging_stations FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_charging_sessions_updated_at BEFORE UPDATE ON charging_sessions FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes for performance
CREATE INDEX idx_ev_vehicle_details_tenant ON ev_vehicle_details(tenant_id);

CREATE INDEX idx_charging_stations_tenant_status ON charging_stations(tenant_id, status);
CREATE INDEX idx_charging_stations_location ON charging_stations USING GIST(location);
CREATE INDEX idx_charging_stations_public ON charging_stations(is_public) WHERE is_public = TRUE;

CREATE INDEX idx_charging_sessions_lookup ON charging_sessions(tenant_id, vehicle_id, started_at DESC);
CREATE INDEX idx_charging_sessions_status ON charging_sessions(tenant_id, status);
CREATE INDEX idx_charging_sessions_station ON charging_sessions(tenant_id, charging_station_id);

-- Comments
COMMENT ON TABLE ev_vehicle_details IS 'Electric Vehicle specific hardware profile details linked to parent vehicle';
COMMENT ON TABLE charging_stations IS 'Private or public EV charging hubs and port capacities';
COMMENT ON TABLE charging_sessions IS 'Transactions tracking state-of-charge delta and power delivery for EV vehicle refuels';
COMMENT ON TABLE battery_health_readings IS 'Raw high-frequency battery pack performance metrics (SoC, SoH, temperature, cell voltages)';
