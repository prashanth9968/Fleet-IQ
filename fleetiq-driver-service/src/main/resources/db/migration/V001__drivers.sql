-- PostgreSQL 16 compliant migrations

CREATE TABLE drivers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID,
    depot_id UUID,
    employee_id VARCHAR(50),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(255),
    license_number VARCHAR(100),
    license_type VARCHAR(50),
    license_expiry DATE,
    date_of_birth DATE,
    blood_group VARCHAR(10),
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(50),
    photo_url VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    hire_date DATE,
    termination_date DATE,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE driver_assignments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    driver_id UUID NOT NULL REFERENCES drivers(id),
    vehicle_id UUID NOT NULL,
    shift_start TIMESTAMP WITH TIME ZONE NOT NULL,
    shift_end TIMESTAMP WITH TIME ZONE,
    assigned_by UUID,
    status VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE driver_scoring_rules (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    points INT NOT NULL,
    maximum_daily_penalty INT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_tenant_event_type UNIQUE (tenant_id, event_type)
);

CREATE TABLE driver_safety_scores (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    driver_id UUID NOT NULL REFERENCES drivers(id),
    period_type VARCHAR(50) NOT NULL,
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    overall_score NUMERIC(5, 2) NOT NULL,
    harsh_accel_score NUMERIC(5, 2),
    harsh_brake_score NUMERIC(5, 2),
    harsh_corner_score NUMERIC(5, 2),
    speeding_score NUMERIC(5, 2),
    fatigue_score NUMERIC(5, 2),
    seatbelt_score NUMERIC(5, 2),
    total_trips INT NOT NULL DEFAULT 0,
    total_distance_km NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_driving_hours NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_events INT NOT NULL DEFAULT 0,
    fuel_efficiency_score NUMERIC(5, 2),
    peer_percentile NUMERIC(5, 2),
    ai_predicted_fatigue_probability NUMERIC(5, 4),
    ai_insurance_risk_score NUMERIC(5, 2),
    ai_accident_probability NUMERIC(5, 4),
    coaching_suggestions JSONB,
    trend_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE driving_events (
    vehicle_id UUID NOT NULL,
    event_at TIMESTAMP WITH TIME ZONE NOT NULL,
    tenant_id UUID NOT NULL,
    driver_id UUID REFERENCES drivers(id),
    event_type VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    speed_kmh DOUBLE PRECISION,
    speed_limit_kmh DOUBLE PRECISION,
    magnitude DOUBLE PRECISION,
    duration_seconds INT,
    road_name VARCHAR(255),
    trip_id UUID,
    metadata JSONB,
    PRIMARY KEY (vehicle_id, event_at)
);

-- Seed default driver scoring rules for a default template tenant
INSERT INTO driver_scoring_rules (id, tenant_id, event_type, points, maximum_daily_penalty, enabled, created_at, updated_at) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '00000000-0000-0000-0000-000000000000', 'HARSH_ACCELERATION', 5, 50, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', '00000000-0000-0000-0000-000000000000', 'HARSH_BRAKING', 8, 80, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', '00000000-0000-0000-0000-000000000000', 'HARSH_CORNERING', 6, 60, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', '00000000-0000-0000-0000-000000000000', 'SPEEDING', 10, 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', '00000000-0000-0000-0000-000000000000', 'FATIGUE', 20, 200, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', '00000000-0000-0000-0000-000000000000', 'SEATBELT_VIOLATION', 5, 50, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
