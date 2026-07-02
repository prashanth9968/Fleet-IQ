CREATE TABLE IF NOT EXISTS fuel_anomaly_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    driver_id UUID,
    anomaly_type VARCHAR(50) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    fuel_drop_litres DECIMAL(8,2),
    duration_seconds INTEGER,
    speed_kmh_at_event DECIMAL(6,2),
    ignition_state BOOLEAN,
    confidence_score DECIMAL(3,2) DEFAULT 0.5,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_anomaly_vehicle ON fuel_anomaly_history(vehicle_id, detected_at DESC);
CREATE INDEX idx_anomaly_tenant ON fuel_anomaly_history(tenant_id, detected_at DESC);
CREATE INDEX idx_anomaly_type ON fuel_anomaly_history(anomaly_type, status);
