CREATE TABLE IF NOT EXISTS fuel_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    driver_id UUID,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    total_fuel_used_litres DECIMAL(10,2),
    total_distance_km DECIMAL(12,2),
    efficiency_litres_per_100km DECIMAL(8,2),
    efficiency_km_per_litre DECIMAL(8,2),
    idle_fuel_used_litres DECIMAL(8,2),
    highway_fuel_used_litres DECIMAL(8,2),
    cost_per_km DECIMAL(8,4),
    cost_per_trip DECIMAL(10,2),
    deviation_from_baseline_pct DECIMAL(6,2),
    baseline_efficiency_km_per_litre DECIMAL(8,2),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_fuel_analytics_vehicle ON fuel_analytics(vehicle_id, period_start DESC);
CREATE INDEX idx_fuel_analytics_tenant ON fuel_analytics(tenant_id, period_start DESC);
CREATE INDEX idx_fuel_analytics_driver ON fuel_analytics(driver_id, period_start DESC);
