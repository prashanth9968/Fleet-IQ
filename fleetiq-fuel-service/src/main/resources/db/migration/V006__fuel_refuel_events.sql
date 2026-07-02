CREATE TABLE IF NOT EXISTS fuel_refuel_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    driver_id UUID,
    refueled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fuel_before_litres DECIMAL(8,2),
    fuel_after_litres DECIMAL(8,2),
    fuel_added_litres DECIMAL(8,2),
    location_lat DECIMAL(10,7),
    location_lng DECIMAL(10,7),
    cost_total DECIMAL(10,2),
    cost_per_litre DECIMAL(8,4),
    odometer_km DECIMAL(12,2),
    source VARCHAR(20) DEFAULT 'AUTO_DETECTED',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_refuel_vehicle ON fuel_refuel_events(vehicle_id, refueled_at DESC);
CREATE INDEX idx_refuel_tenant ON fuel_refuel_events(tenant_id, refueled_at DESC);
