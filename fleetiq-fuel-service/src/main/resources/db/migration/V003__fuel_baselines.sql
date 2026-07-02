CREATE TABLE IF NOT EXISTS fuel_baselines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_type_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    expected_efficiency_km_per_litre DECIMAL(8,2),
    normal_idle_burn_l_per_min DECIMAL(8,4),
    normal_highway_burn_l_per_min DECIMAL(8,4),
    normal_city_burn_l_per_min DECIMAL(8,4),
    tank_capacity_litres DECIMAL(8,2),
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_fuel_baselines_type ON fuel_baselines(vehicle_type_id, tenant_id);
