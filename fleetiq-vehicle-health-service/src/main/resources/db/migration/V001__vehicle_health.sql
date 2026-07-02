-- Create dtc_library table
CREATE TABLE dtc_library (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    system VARCHAR(100),
    severity VARCHAR(50),
    description TEXT,
    possible_causes TEXT,
    recommended_action TEXT,
    sae_standard VARCHAR(100)
);

-- Create vehicle_dtc_events table
CREATE TABLE vehicle_dtc_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    dtc_id UUID REFERENCES dtc_library(id),
    detected_at TIMESTAMPTZ NOT NULL,
    cleared_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    odometer_at_detection NUMERIC(12, 2),
    freeze_frame_data JSONB
);

-- Create vehicle_health_scores table
CREATE TABLE vehicle_health_scores (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    vehicle_id UUID NOT NULL UNIQUE,
    overall_score NUMERIC(5, 2),
    engine_score NUMERIC(5, 2),
    transmission_score NUMERIC(5, 2),
    electrical_score NUMERIC(5, 2),
    brake_score NUMERIC(5, 2),
    battery_score NUMERIC(5, 2),
    tyre_score NUMERIC(5, 2),
    emission_score NUMERIC(5, 2),
    cooling_score NUMERIC(5, 2),
    active_dtc_count INT NOT NULL DEFAULT 0,
    pending_dtc_count INT NOT NULL DEFAULT 0,
    last_calculated_at TIMESTAMPTZ NOT NULL
);

-- Create vehicle_health_history table
CREATE TABLE vehicle_health_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    overall_score NUMERIC(5, 2),
    engine_score NUMERIC(5, 2),
    transmission_score NUMERIC(5, 2),
    electrical_score NUMERIC(5, 2),
    brake_score NUMERIC(5, 2),
    battery_score NUMERIC(5, 2),
    tyre_score NUMERIC(5, 2),
    emission_score NUMERIC(5, 2),
    cooling_score NUMERIC(5, 2),
    active_dtc_count INT NOT NULL DEFAULT 0,
    odometer_km NUMERIC(12, 2),
    engine_hours NUMERIC(12, 2),
    metadata JSONB
);

-- Create vehicle_health_rules table
CREATE TABLE vehicle_health_rules (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    rule_type VARCHAR(100) NOT NULL,
    severity VARCHAR(50),
    deduction NUMERIC(5, 2) NOT NULL,
    threshold_value NUMERIC(12, 2),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT unique_tenant_rule_type UNIQUE (tenant_id, rule_type)
);

-- Create maintenance_schedules table
CREATE TABLE maintenance_schedules (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    custom_service_name VARCHAR(150),
    interval_km NUMERIC(12, 2),
    interval_days INT,
    interval_engine_hours NUMERIC(12, 2),
    last_service_date DATE,
    last_service_odometer NUMERIC(12, 2),
    last_service_engine_hours NUMERIC(12, 2),
    next_due_date DATE,
    next_due_odometer NUMERIC(12, 2),
    next_due_engine_hours NUMERIC(12, 2),
    status VARCHAR(50) NOT NULL,
    is_overdue BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create maintenance_predictions table
CREATE TABLE maintenance_predictions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    component VARCHAR(100) NOT NULL,
    failure_probability NUMERIC(5, 2) NOT NULL,
    predicted_failure_date DATE,
    predicted_failure_odometer NUMERIC(12, 2),
    confidence_level NUMERIC(5, 2),
    model_version VARCHAR(50),
    features_used JSONB,
    recommendation TEXT,
    status VARCHAR(50) NOT NULL
);

-- Create work_orders table
CREATE TABLE work_orders (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    order_number VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    priority VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    assigned_to UUID,
    scheduled_date DATE,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    odometer_at_service NUMERIC(12, 2),
    engine_hours_at_service NUMERIC(12, 2)
);

-- Seed default DTC library codes
INSERT INTO dtc_library (id, code, system, severity, description, possible_causes, recommended_action, sae_standard) VALUES
('d144fbfe-25e2-4be1-90a6-16e53a25bf68', 'P0300', 'Engine', 'CRITICAL', 'Random/Multiple Cylinder Misfire Detected', 'Faulty spark plugs, bad fuel injectors, vacuum leak, low compression', 'Inspect spark plugs, ignition coils, and fuel injectors', 'SAE J2012'),
('f88b0a9b-640a-4299-8cfb-d017b2b62b1a', 'P0115', 'Cooling', 'HIGH', 'Engine Coolant Temperature Sensor 1 Circuit Malfunction', 'Failed ECT sensor, corroded wiring, low engine coolant level, stuck thermostat', 'Check ECT sensor resistance, wiring harness, and coolant level', 'SAE J2012'),
('d5d9962a-0a7d-419b-b0b3-0570b5030282', 'P0520', 'Engine', 'CRITICAL', 'Engine Oil Pressure Sensor/Switch Circuit Malfunction', 'Low engine oil level, faulty oil pressure sensor, internal engine wear', 'Check oil level immediately, test oil pressure with mechanical gauge, replace sensor', 'SAE J2012'),
('c775796b-4e89-4bc2-8874-7bd0fdf8f47d', 'P0562', 'Electrical', 'HIGH', 'System Voltage Low', 'Alternator failure, weak/dead battery, high resistance in battery cables', 'Test battery and charging system, clean battery terminals', 'SAE J2012');

-- Seed default rules for tenant UUID '00000000-0000-0000-0000-000000000000'
INSERT INTO vehicle_health_rules (id, tenant_id, rule_type, severity, deduction, threshold_value, enabled) VALUES
('b335359a-115f-4d69-bb99-f472851cf571', '00000000-0000-0000-0000-000000000000', 'COOLANT_TEMP', 'HIGH', 15.00, 105.00, TRUE),
('e983416b-76f8-4e1b-b4fe-dfb01e389e6c', '00000000-0000-0000-0000-000000000000', 'BATTERY_VOLTAGE', 'HIGH', 10.00, 11.50, TRUE),
('a260840b-71db-40a2-a9b0-449e7552fe80', '00000000-0000-0000-0000-000000000000', 'OIL_PRESSURE', 'CRITICAL', 25.00, 20.00, TRUE),
('c860c49b-75e1-4c12-a7f4-b258e7f9e80e', '00000000-0000-0000-0000-000000000000', 'ENGINE_LOAD', 'MEDIUM', 5.00, 90.00, TRUE);
