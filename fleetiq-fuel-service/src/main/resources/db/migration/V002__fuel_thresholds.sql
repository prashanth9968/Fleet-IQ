CREATE TABLE IF NOT EXISTS fuel_thresholds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID,
    vehicle_type_id UUID,
    tenant_id UUID NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    threshold_value DECIMAL(10,4) NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'L_PER_MIN',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT chk_threshold_target CHECK (vehicle_id IS NOT NULL OR vehicle_type_id IS NOT NULL)
);

CREATE INDEX idx_fuel_thresholds_vehicle ON fuel_thresholds(vehicle_id);
CREATE INDEX idx_fuel_thresholds_type ON fuel_thresholds(vehicle_type_id);
CREATE INDEX idx_fuel_thresholds_tenant ON fuel_thresholds(tenant_id);
