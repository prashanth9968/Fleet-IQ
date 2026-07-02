CREATE TABLE fleet_reports (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    type VARCHAR(100) NOT NULL,
    date_range_start TIMESTAMPTZ NOT NULL,
    date_range_end TIMESTAMPTZ NOT NULL,
    format VARCHAR(50) NOT NULL,
    file_url VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE daily_fleet_reports (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    date DATE NOT NULL,
    distance_km NUMERIC(10, 2) NOT NULL,
    fuel_consumed_litres NUMERIC(10, 2) NOT NULL,
    avg_fuel_efficiency NUMERIC(10, 2) NOT NULL,
    safety_score NUMERIC(5, 2) NOT NULL,
    fault_count INT NOT NULL,
    utilization_pct NUMERIC(5, 2) NOT NULL,
    avg_trip_duration_mins NUMERIC(10, 2) NOT NULL,
    avg_idle_time_mins NUMERIC(10, 2) NOT NULL,
    fuel_cost NUMERIC(10, 2) NOT NULL,
    maintenance_cost NUMERIC(10, 2) NOT NULL,
    critical_alerts_count INT NOT NULL,
    co2_estimate_kg NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
