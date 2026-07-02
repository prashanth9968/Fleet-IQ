-- V010__maintenance.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. maintenance_schedules
CREATE TABLE maintenance_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    service_type VARCHAR(50) NOT NULL CHECK (service_type IN (
        'OIL_CHANGE', 'TYRE_ROTATION', 'TYRE_REPLACEMENT', 'BRAKE_INSPECTION', 
        'BRAKE_PAD_REPLACEMENT', 'AIR_FILTER', 'BATTERY_REPLACEMENT', 'TRANSMISSION_FLUID', 
        'COOLANT_FLUSH', 'SPARK_PLUG', 'BELT_REPLACEMENT', 'WHEEL_ALIGNMENT', 
        'AC_SERVICE', 'GENERAL_SERVICE', 'CUSTOM'
    )),
    custom_service_name VARCHAR(255), -- populated when service_type is CUSTOM
    interval_km NUMERIC(10,2),
    interval_days INT,
    interval_engine_hours NUMERIC(8,2),
    last_service_date DATE,
    last_service_odometer NUMERIC(12,2),
    last_service_engine_hours NUMERIC(10,2),
    next_due_date DATE,
    next_due_odometer NUMERIC(12,2),
    next_due_engine_hours NUMERIC(10,2),
    reminder_threshold_1_km NUMERIC(10,2) DEFAULT 1000,
    reminder_threshold_2_km NUMERIC(10,2) DEFAULT 200,
    reminder_threshold_1_days INT DEFAULT 14,
    reminder_threshold_2_days INT DEFAULT 3,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN (
        'ACTIVE', 'PAUSED', 'COMPLETED'
    )),
    is_overdue BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. maintenance_predictions
CREATE TABLE maintenance_predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    component VARCHAR(100) NOT NULL,
    failure_probability NUMERIC(5,4) NOT NULL CHECK (failure_probability BETWEEN 0.0000 AND 1.0000),
    predicted_failure_date DATE,
    predicted_failure_odometer NUMERIC(12,2),
    confidence_level NUMERIC(5,2),
    model_version VARCHAR(50),
    features_used JSONB,
    recommendation TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN (
        'ACTIVE', 'ACTIONED', 'EXPIRED', 'DISMISSED'
    )),
    actioned_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. work_orders
CREATE TABLE work_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    order_number VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN (
        'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'
    )),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN (
        'OPEN', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CANCELLED'
    )),
    assigned_to UUID REFERENCES users(id) ON DELETE SET NULL, -- assigned technician
    scheduled_date DATE,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    odometer_at_service NUMERIC(12,2),
    engine_hours_at_service NUMERIC(10,2),
    total_parts_cost NUMERIC(12,2) DEFAULT 0,
    total_labour_cost NUMERIC(12,2) DEFAULT 0,
    total_cost NUMERIC(12,2) DEFAULT 0,
    technician_notes TEXT,
    photos JSONB NOT NULL DEFAULT '[]', -- JSON array of photo URLs
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK (source IN (
        'MANUAL', 'SCHEDULED', 'PREDICTIVE'
    )),
    maintenance_schedule_id UUID REFERENCES maintenance_schedules(id) ON DELETE SET NULL,
    maintenance_prediction_id UUID REFERENCES maintenance_predictions(id) ON DELETE SET NULL,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, order_number)
);

-- 4. work_order_items
CREATE TABLE work_order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    item_type VARCHAR(20) NOT NULL CHECK (item_type IN ('PART', 'LABOUR', 'OTHER')),
    description VARCHAR(255) NOT NULL,
    part_number VARCHAR(100),
    quantity NUMERIC(8,2) DEFAULT 1,
    unit_cost NUMERIC(10,2) NOT NULL,
    total_cost NUMERIC(12,2) NOT NULL,
    vendor VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Apply updated_at triggers
CREATE TRIGGER trg_maintenance_schedules_updated_at BEFORE UPDATE ON maintenance_schedules FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_maintenance_predictions_updated_at BEFORE UPDATE ON maintenance_predictions FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_work_orders_updated_at BEFORE UPDATE ON work_orders FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes for performance
CREATE INDEX idx_maintenance_schedules_find ON maintenance_schedules(tenant_id, vehicle_id, status);
CREATE INDEX idx_maintenance_schedules_due ON maintenance_schedules(tenant_id, next_due_date) WHERE status = 'ACTIVE';
CREATE INDEX idx_maintenance_schedules_overdue ON maintenance_schedules(tenant_id, is_overdue) WHERE is_overdue = TRUE;

CREATE INDEX idx_maintenance_predictions_find ON maintenance_predictions(tenant_id, vehicle_id, status);
CREATE INDEX idx_maintenance_predictions_prob ON maintenance_predictions(tenant_id, failure_probability DESC) WHERE status = 'ACTIVE';

CREATE INDEX idx_work_orders_veh_status ON work_orders(tenant_id, vehicle_id, status);
CREATE INDEX idx_work_orders_status_priority ON work_orders(tenant_id, status, priority);
CREATE INDEX idx_work_orders_scheduled_date ON work_orders(tenant_id, scheduled_date);
CREATE INDEX idx_work_orders_assigned ON work_orders(tenant_id, assigned_to) WHERE status IN ('ASSIGNED', 'IN_PROGRESS');

CREATE INDEX idx_work_order_items_order ON work_order_items(work_order_id);

-- Comments
COMMENT ON TABLE maintenance_schedules IS 'Recurring preventive maintenance parameters per vehicle (distance or time intervals)';
COMMENT ON TABLE maintenance_predictions IS 'Predictive component failures flagged by Machine Learning models';
COMMENT ON TABLE work_orders IS 'Maintenance repair logs and scheduled workshop tasks';
COMMENT ON TABLE work_order_items IS 'Line item parts, labor hours, and misc costs associated with a work order';
