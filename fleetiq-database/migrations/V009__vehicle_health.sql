-- V009__vehicle_health.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. obd_readings (Hypertable source - no foreign keys to avoid Timescale constraints)
CREATE TABLE obd_readings (
    vehicle_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    device_id UUID NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    engine_rpm INT,
    coolant_temp_c NUMERIC(5,1),
    oil_pressure_kpa NUMERIC(7,2),
    battery_voltage NUMERIC(5,2),
    intake_air_temp_c NUMERIC(5,1),
    throttle_position_pct NUMERIC(5,2),
    engine_load_pct NUMERIC(5,2),
    fuel_trim_short_pct NUMERIC(6,2),
    fuel_trim_long_pct NUMERIC(6,2),
    maf_gps NUMERIC(8,2), -- Mass Air Flow grams/sec
    catalyst_temp_c NUMERIC(6,1),
    ambient_air_temp_c NUMERIC(5,1),
    odometer_km NUMERIC(12,2),
    engine_run_hours NUMERIC(10,2),
    dpf_soot_load_pct NUMERIC(5,2),
    dpf_regen_status BOOLEAN,
    check_engine_light BOOLEAN,
    metadata JSONB,
    PRIMARY KEY (vehicle_id, recorded_at)
);

-- 2. dtc_library (Global reference - not tenant-specific)
CREATE TABLE dtc_library (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(10) NOT NULL UNIQUE,
    system VARCHAR(30) NOT NULL CHECK (system IN (
        'ENGINE', 'TRANSMISSION', 'ABS', 'SRS', 'BODY', 'CHASSIS', 'NETWORK', 'HYBRID_EV'
    )),
    severity VARCHAR(20) NOT NULL CHECK (severity IN (
        'CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'
    )),
    description TEXT NOT NULL,
    possible_causes TEXT,
    recommended_action TEXT,
    sae_standard VARCHAR(20), -- J1979, J2012, etc.
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. vehicle_dtc_events
CREATE TABLE vehicle_dtc_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    dtc_id UUID NOT NULL REFERENCES dtc_library(id) ON DELETE CASCADE,
    detected_at TIMESTAMPTZ NOT NULL,
    cleared_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    odometer_at_detection NUMERIC(12,2),
    freeze_frame_data JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. vehicle_health_scores
CREATE TABLE vehicle_health_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE UNIQUE,
    overall_score NUMERIC(5,2) NOT NULL CHECK (overall_score BETWEEN 0.0 AND 100.0),
    engine_score NUMERIC(5,2),
    transmission_score NUMERIC(5,2),
    brake_score NUMERIC(5,2),
    battery_score NUMERIC(5,2),
    tyre_score NUMERIC(5,2),
    electrical_score NUMERIC(5,2),
    active_dtc_count INT DEFAULT 0,
    pending_dtc_count INT DEFAULT 0,
    last_calculated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. vehicle_health_history
CREATE TABLE vehicle_health_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    recorded_at TIMESTAMPTZ NOT NULL,
    overall_score NUMERIC(5,2) NOT NULL,
    engine_score NUMERIC(5,2),
    transmission_score NUMERIC(5,2),
    brake_score NUMERIC(5,2),
    battery_score NUMERIC(5,2),
    tyre_score NUMERIC(5,2),
    electrical_score NUMERIC(5,2),
    active_dtc_count INT DEFAULT 0,
    odometer_km NUMERIC(12,2),
    engine_hours NUMERIC(10,2),
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Apply updated_at triggers
CREATE TRIGGER trg_vehicle_dtc_events_updated_at BEFORE UPDATE ON vehicle_dtc_events FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_vehicle_health_scores_updated_at BEFORE UPDATE ON vehicle_health_scores FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes for performance
CREATE INDEX idx_dtc_library_code ON dtc_library USING HASH(code);
CREATE INDEX idx_dtc_library_sys_sev ON dtc_library(system, severity);

CREATE INDEX idx_vehicle_dtc_events_active ON vehicle_dtc_events(tenant_id, vehicle_id, is_active);
CREATE INDEX idx_vehicle_dtc_events_detected ON vehicle_dtc_events(tenant_id, detected_at DESC);

CREATE INDEX idx_vehicle_health_scores_tenant_score ON vehicle_health_scores(tenant_id, overall_score);
CREATE INDEX idx_vehicle_health_history_veh_at ON vehicle_health_history(vehicle_id, recorded_at DESC);
CREATE INDEX idx_vehicle_health_history_tenant_at ON vehicle_health_history(tenant_id, recorded_at DESC);

-- Comments
COMMENT ON TABLE obd_readings IS 'Raw high-frequency engine sensor and diagnostic telemetry from vehicles';
COMMENT ON TABLE dtc_library IS 'Diagnostic Trouble Code (DTC) library containing definitions and troubleshooting instructions';
COMMENT ON TABLE vehicle_dtc_events IS 'Instances of active and resolved check engine warning codes triggered on vehicles';
COMMENT ON TABLE vehicle_health_scores IS 'Calculated current health ratings (0-100) per vehicle based on active issues';
COMMENT ON TABLE vehicle_health_history IS 'Time-series archive of vehicle health scores for trends and predictions';
