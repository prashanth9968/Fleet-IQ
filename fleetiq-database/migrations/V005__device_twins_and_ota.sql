-- V005__device_twins_and_ota.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. device_twins
CREATE TABLE device_twins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    device_id UUID NOT NULL REFERENCES devices(id) UNIQUE,
    firmware_version VARCHAR(50),
    hardware_version VARCHAR(50),
    desired_config JSONB NOT NULL DEFAULT '{}',
    reported_config JSONB NOT NULL DEFAULT '{}',
    desired_firmware_version VARCHAR(50),
    sync_status VARCHAR(20) NOT NULL DEFAULT 'IN_SYNC' CHECK (sync_status IN (
        'IN_SYNC', 'PENDING', 'SYNCING', 'FAILED', 'UNKNOWN'
    )),
    last_sync_at TIMESTAMPTZ,
    last_seen_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. firmware_packages
CREATE TABLE firmware_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version VARCHAR(50) NOT NULL,
    device_manufacturer VARCHAR(100) NOT NULL,
    device_model VARCHAR(100) NOT NULL,
    file_url TEXT NOT NULL,
    file_size_bytes BIGINT,
    checksum_sha256 VARCHAR(64) NOT NULL,
    release_notes TEXT,
    is_critical BOOLEAN DEFAULT FALSE,
    min_hardware_version VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN (
        'DRAFT', 'TESTING', 'RELEASED', 'DEPRECATED'
    )),
    released_at TIMESTAMPTZ,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (version, device_manufacturer, device_model)
);

-- 3. firmware_deployments
CREATE TABLE firmware_deployments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id), -- NULL means platform-wide deployment
    firmware_package_id UUID NOT NULL REFERENCES firmware_packages(id),
    name VARCHAR(255) NOT NULL,
    strategy VARCHAR(20) NOT NULL DEFAULT 'ROLLING' CHECK (strategy IN (
        'ROLLING', 'IMMEDIATE', 'SCHEDULED', 'CANARY'
    )),
    scheduled_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'
    )),
    total_targets INT DEFAULT 0,
    success_count INT DEFAULT 0,
    failure_count INT DEFAULT 0,
    rollback_on_failure_pct INT DEFAULT 10,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. firmware_deployment_targets
CREATE TABLE firmware_deployment_targets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    deployment_id UUID NOT NULL REFERENCES firmware_deployments(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'DOWNLOADING', 'INSTALLING', 'SUCCESS', 'FAILED', 'ROLLED_BACK'
    )),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    previous_version VARCHAR(50),
    error_message TEXT,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (deployment_id, device_id)
);

-- Apply updated_at triggers
CREATE TRIGGER trg_device_twins_updated_at BEFORE UPDATE ON device_twins FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_firmware_packages_updated_at BEFORE UPDATE ON firmware_packages FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_firmware_deployments_updated_at BEFORE UPDATE ON firmware_deployments FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_firmware_deployment_targets_updated_at BEFORE UPDATE ON firmware_deployment_targets FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes for performance
CREATE INDEX idx_device_twins_sync_status ON device_twins(sync_status) WHERE sync_status != 'IN_SYNC';
CREATE INDEX idx_device_twins_tenant ON device_twins(tenant_id);
CREATE INDEX idx_firmware_packages_lookup ON firmware_packages(device_manufacturer, device_model, status);
CREATE INDEX idx_firmware_deployments_tenant_status ON firmware_deployments(tenant_id, status);
CREATE INDEX idx_firmware_deployment_targets_lookup ON firmware_deployment_targets(deployment_id, status);

-- Comments
COMMENT ON TABLE device_twins IS 'Stores the desired and reported configurations and states for IoT tracking devices';
COMMENT ON TABLE firmware_packages IS 'Available binary OTA packages metadata';
COMMENT ON TABLE firmware_deployments IS 'Rollout sessions for OTA package updates';
COMMENT ON TABLE firmware_deployment_targets IS 'Individual tracking devices targeted in a firmware rollout';
