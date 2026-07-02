CREATE TABLE IF NOT EXISTS alert_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    priority_threshold VARCHAR(50) DEFAULT 'MEDIUM', -- Trigger for >= this priority
    escalation_levels JSONB, -- Array of {delay_minutes, target}
    channels JSONB, -- Array of channels: EMAIL, SMS, PUSH, WHATSAPP
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_alert_rules_tenant ON alert_rules(tenant_id);

CREATE TABLE IF NOT EXISTS alert_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    alert_type VARCHAR(100) NOT NULL,
    priority VARCHAR(50) NOT NULL, -- CRITICAL, HIGH, MEDIUM, LOW, INFO
    message TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- OPEN, ACKNOWLEDGED, RESOLVED
    escalation_level INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_alert_history_tenant ON alert_history(tenant_id, created_at DESC);
CREATE INDEX idx_alert_history_vehicle ON alert_history(vehicle_id, created_at DESC);
