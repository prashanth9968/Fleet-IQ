-- V006__drivers.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. drivers
CREATE TABLE drivers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL, -- optional link to system user
    depot_id UUID REFERENCES depots(id) ON DELETE SET NULL,
    employee_id VARCHAR(50),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    license_number VARCHAR(50),
    license_type VARCHAR(20),
    license_expiry DATE,
    date_of_birth DATE,
    blood_group VARCHAR(5),
    emergency_contact_name VARCHAR(200),
    emergency_contact_phone VARCHAR(20),
    photo_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN (
        'ACTIVE', 'INACTIVE', 'SUSPENDED', 'ON_LEAVE'
    )),
    hire_date DATE,
    termination_date DATE,
    metadata JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE (tenant_id, employee_id)
);

-- 2. driver_assignments
CREATE TABLE driver_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    shift_start TIMESTAMPTZ NOT NULL,
    shift_end TIMESTAMPTZ,
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN (
        'ACTIVE', 'COMPLETED', 'CANCELLED'
    )),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. driver_safety_scores
CREATE TABLE driver_safety_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    period_type VARCHAR(10) NOT NULL CHECK (period_type IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    overall_score NUMERIC(5,2) NOT NULL CHECK (overall_score BETWEEN 0.0 AND 100.0),
    harsh_accel_score NUMERIC(5,2),
    harsh_brake_score NUMERIC(5,2),
    harsh_corner_score NUMERIC(5,2),
    speeding_score NUMERIC(5,2),
    fatigue_score NUMERIC(5,2),
    seatbelt_score NUMERIC(5,2),
    total_trips INT DEFAULT 0,
    total_distance_km NUMERIC(10,2) DEFAULT 0,
    total_driving_hours NUMERIC(8,2) DEFAULT 0,
    total_events INT DEFAULT 0,
    fuel_efficiency_score NUMERIC(5,2),
    peer_percentile NUMERIC(5,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (driver_id, period_type, period_start)
);

-- 4. driving_events (Hypertable source - no foreign keys to avoid Timescale constraints)
CREATE TABLE driving_events (
    vehicle_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    driver_id UUID,
    event_type VARCHAR(50) NOT NULL, -- HARSH_ACCELERATION, HARSH_BRAKING, HARSH_CORNERING, SPEEDING, FATIGUE, SEATBELT_VIOLATION, PHONE_USE
    event_at TIMESTAMPTZ NOT NULL,
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    speed_kmh NUMERIC(6,2),
    speed_limit_kmh NUMERIC(6,2),
    magnitude NUMERIC(5,2), -- for harsh events, in g-force
    duration_seconds INT,
    road_name VARCHAR(255),
    trip_id UUID,
    metadata JSONB,
    PRIMARY KEY (vehicle_id, event_at)
);

-- Apply updated_at triggers
CREATE TRIGGER trg_drivers_updated_at BEFORE UPDATE ON drivers FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_driver_assignments_updated_at BEFORE UPDATE ON driver_assignments FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes for performance
CREATE INDEX idx_drivers_tenant_status ON drivers(tenant_id, status);
CREATE INDEX idx_drivers_tenant_emp ON drivers(tenant_id, employee_id);
CREATE INDEX idx_drivers_tenant_depot ON drivers(tenant_id, depot_id);

CREATE INDEX idx_driver_assignments_veh_status ON driver_assignments(tenant_id, vehicle_id, status);
CREATE INDEX idx_driver_assignments_drv_status ON driver_assignments(tenant_id, driver_id, status);

CREATE INDEX idx_driver_safety_scores_leaderboard ON driver_safety_scores(tenant_id, period_start, overall_score DESC);

-- Comments
COMMENT ON TABLE drivers IS 'Profiles of human vehicle operators including credentials and contact info';
COMMENT ON TABLE driver_assignments IS 'Shift allocations matching drivers to vehicles';
COMMENT ON TABLE driver_safety_scores IS 'Historical driving score cards calculated daily, weekly, or monthly';
COMMENT ON TABLE driving_events IS 'Raw driving safety violations (harsh braking, cornering, speeding) captured from devices';
