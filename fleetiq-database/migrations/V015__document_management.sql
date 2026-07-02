-- V015__document_management.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. document_types (Global reference table - not tenant-specific)
CREATE TABLE document_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE, -- RC, INSURANCE, PUC, PERMIT, FITNESS_CERTIFICATE, ROAD_TAX, DRIVER_LICENSE, DRIVER_MEDICAL, etc.
    description TEXT,
    applies_to VARCHAR(20) NOT NULL CHECK (applies_to IN ('VEHICLE', 'DRIVER')),
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    default_validity_months INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. documents
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    document_type_id UUID NOT NULL REFERENCES document_types(id),
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE CASCADE,
    driver_id UUID REFERENCES drivers(id) ON DELETE CASCADE,
    document_number VARCHAR(100),
    file_url TEXT NOT NULL,
    issue_date DATE,
    expiry_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN (
        'ACTIVE', 'EXPIRING', 'EXPIRED', 'PENDING_VERIFICATION', 'REJECTED'
    )),
    uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    verified_by UUID REFERENCES users(id) ON DELETE SET NULL,
    verified_at TIMESTAMPTZ,
    verification_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_documents_owner CHECK (vehicle_id IS NOT NULL OR driver_id IS NOT NULL)
);

-- 3. document_expiry_alerts
CREATE TABLE document_expiry_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    alert_type VARCHAR(50) NOT NULL CHECK (alert_type IN (
        'EXPIRY_30_DAYS', 'EXPIRY_15_DAYS', 'EXPIRY_7_DAYS', 'EXPIRY_1_DAY', 'EXPIRED'
    )),
    scheduled_for TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    alert_id UUID REFERENCES alerts(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (document_id, alert_type)
);

-- Apply updated_at triggers
CREATE TRIGGER trg_documents_updated_at BEFORE UPDATE ON documents FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_document_expiry_alerts_updated_at BEFORE UPDATE ON document_expiry_alerts FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes for performance
CREATE INDEX idx_document_types_code ON document_types(code);
CREATE INDEX idx_document_types_applies ON document_types(applies_to);

CREATE INDEX idx_documents_veh_type ON documents(tenant_id, vehicle_id, document_type_id);
CREATE INDEX idx_documents_drv_type ON documents(tenant_id, driver_id, document_type_id);
CREATE INDEX idx_documents_expiry ON documents(tenant_id, expiry_date) WHERE expiry_date IS NOT NULL AND status = 'ACTIVE';
CREATE INDEX idx_documents_status ON documents(tenant_id, status);

CREATE INDEX idx_document_expiry_alerts_scheduled ON document_expiry_alerts(tenant_id, scheduled_for) WHERE sent_at IS NULL;
CREATE INDEX idx_document_expiry_alerts_doc ON document_expiry_alerts(document_id);

-- Comments
COMMENT ON TABLE document_types IS 'Metadata definitions for regulatory fleet documents (e.g. Registration, Pollution, Driver License)';
COMMENT ON TABLE documents IS 'Uploaded legal documents with validity dates associated with drivers or vehicles';
COMMENT ON TABLE document_expiry_alerts IS 'Scheduled alert triggers for automated document renewal warnings';
