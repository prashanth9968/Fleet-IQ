-- V004__vehicles_and_devices.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. vehicle_types
CREATE TABLE vehicle_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL CHECK (category IN (
        'SEDAN', 'SUV', 'HATCHBACK', 'TRUCK_LIGHT', 'TRUCK_MEDIUM', 'TRUCK_HEAVY', 
        'BUS', 'MINI_BUS', 'TWO_WHEELER', 'THREE_WHEELER', 'CONSTRUCTION', 'AGRICULTURAL', 
        'EV_CAR', 'EV_BUS', 'EV_TRUCK', 'EV_TWO_WHEELER', 'EV_THREE_WHEELER'
    )),
    fuel_type VARCHAR(20) NOT NULL CHECK (fuel_type IN (
        'PETROL', 'DIESEL', 'CNG', 'LPG', 'ELECTRIC', 'HYBRID', 'HYDROGEN'
    )),
    default_fuel_capacity_litres NUMERIC(8,2),
    default_fuel_consumption_rate NUMERIC(6,2), -- L/100km baseline
    icon VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. vehicles
CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_type_id UUID NOT NULL REFERENCES vehicle_types(id),
    depot_id UUID REFERENCES depots(id) ON DELETE SET NULL,
    registration_number VARCHAR(20) NOT NULL,
    vin VARCHAR(17),
    chassis_number VARCHAR(50),
    engine_number VARCHAR(50),
    make VARCHAR(100),
    model VARCHAR(100),
    year_of_manufacture INT,
    color VARCHAR(50),
    fuel_tank_capacity_litres NUMERIC(8,2),
    odometer_reading_km NUMERIC(12,2) DEFAULT 0,
    engine_hours NUMERIC(10,2) DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN (
        'ACTIVE', 'INACTIVE', 'IN_MAINTENANCE', 'RETIRED', 'SOLD'
    )),
    acquisition_date DATE,
    acquisition_cost NUMERIC(14,2),
    insurance_expiry DATE,
    permit_expiry DATE,
    fitness_expiry DATE,
    last_service_date DATE,
    last_service_odometer NUMERIC(12,2),
    metadata JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE (tenant_id, registration_number)
);

-- 3. devices
CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    serial_number VARCHAR(100) NOT NULL UNIQUE,
    imei VARCHAR(20) UNIQUE,
    device_type VARCHAR(30) NOT NULL CHECK (device_type IN (
        'GPS_TRACKER', 'OBD_DONGLE', 'FUEL_SENSOR', 'CAN_READER', 
        'DASHCAM', 'DMS_CAMERA', 'ADAS_CAMERA', 'COMBINED'
    )),
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    firmware_version VARCHAR(50),
    hardware_version VARCHAR(50),
    sim_iccid VARCHAR(22),
    sim_phone_number VARCHAR(20),
    protocol VARCHAR(30) CHECK (protocol IN ('MQTT', 'HTTP', 'TCP', 'UDP', 'COAP')),
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE' CHECK (status IN (
        'ACTIVE', 'INACTIVE', 'FAULTY', 'DECOMMISSIONED'
    )),
    last_communication_at TIMESTAMPTZ,
    configuration JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- 4. device_vehicle_assignments
CREATE TABLE device_vehicle_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    unassigned_at TIMESTAMPTZ, -- NULL means currently assigned
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    is_primary BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add the deferred FK constraint for vehicle_group_members that we skipped in V003
ALTER TABLE vehicle_group_members 
ADD CONSTRAINT fk_vehicle_group_members_vehicles FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE;

-- Apply updated_at triggers
CREATE TRIGGER trg_vehicles_updated_at BEFORE UPDATE ON vehicles FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_devices_updated_at BEFORE UPDATE ON devices FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes
CREATE INDEX idx_vehicles_tenant_reg ON vehicles(tenant_id, registration_number);
CREATE INDEX idx_vehicles_tenant_status ON vehicles(tenant_id, status);
CREATE INDEX idx_vehicles_tenant_depot ON vehicles(tenant_id, depot_id);
CREATE INDEX idx_vehicles_reg_trgm ON vehicles USING GIN(registration_number gin_trgm_ops);

CREATE INDEX idx_devices_tenant_serial ON devices(tenant_id, serial_number);
CREATE INDEX idx_devices_tenant_type_status ON devices(tenant_id, device_type, status);
CREATE INDEX idx_devices_imei ON devices(imei);

CREATE INDEX idx_device_vehicle_assignments_find ON device_vehicle_assignments(vehicle_id, unassigned_at);

-- Comments
COMMENT ON TABLE vehicle_types IS 'Metadata definitions for vehicle classifications and baselines';
COMMENT ON TABLE vehicles IS 'Vehicle inventory list tracked under a specific tenant';
COMMENT ON TABLE devices IS 'Hardware tracker inventory (OBD dongles, dashcams, GPS units)';
COMMENT ON TABLE device_vehicle_assignments IS 'Links hardware devices to specific vehicles over time';
