-- V011__create_driver_scoring_rules.sql
-- Create driver scoring rules table

CREATE TABLE IF NOT EXISTS driver_scoring_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    event_type VARCHAR(100) NOT NULL,
    points INT NOT NULL,
    maximum_daily_penalty INT,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, event_type)
);

CREATE TRIGGER trg_driver_scoring_rules_updated_at 
BEFORE UPDATE ON driver_scoring_rules 
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
