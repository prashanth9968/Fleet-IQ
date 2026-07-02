-- V014__camera_and_media.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. camera_devices
CREATE TABLE camera_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE SET NULL,
    camera_type VARCHAR(30) NOT NULL CHECK (camera_type IN (
        'ROAD_CAM', 'CABIN_CAM', 'SIDE_CAM', 'REAR_CAM', 'COMBINED'
    )),
    channel_number INT DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN (
        'ACTIVE', 'INACTIVE', 'FAULTY'
    )),
    last_health_check_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- 2. video_events
CREATE TABLE video_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    camera_device_id UUID REFERENCES camera_devices(id) ON DELETE SET NULL,
    driver_id UUID REFERENCES drivers(id) ON DELETE SET NULL,
    event_at TIMESTAMPTZ NOT NULL,
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN (
        'HARSH_BRAKING', 'HARSH_ACCELERATION', 'HARSH_CORNERING', 'SPEEDING', 'TAILGATING', 
        'DISTRACTED_DRIVING', 'DROWSINESS_DETECTED', 'COLLISION_WARNING', 'LANE_DEPARTURE', 
        'SOS_MANUAL', 'CRASH_DETECTED', 'GEOFENCE_VIOLATION'
    )),
    severity VARCHAR(20) NOT NULL CHECK (severity IN (
        'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'
    )),
    duration_seconds NUMERIC(6,2),
    speed_kmh NUMERIC(6,2),
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    trigger_source VARCHAR(20) NOT NULL DEFAULT 'DEVICE_AI' CHECK (trigger_source IN (
        'DEVICE_AI', 'PLATFORM_RULES', 'ACCELEROMETER', 'MANUAL'
    )),
    thumbnail_url TEXT,
    video_url TEXT,
    reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    review_notes TEXT,
    metadata JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. media_assets
CREATE TABLE media_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    video_event_id UUID REFERENCES video_events(id) ON DELETE SET NULL,
    asset_type VARCHAR(20) NOT NULL CHECK (asset_type IN ('VIDEO', 'IMAGE', 'AUDIO')),
    file_url TEXT NOT NULL,
    file_size_bytes BIGINT,
    mime_type VARCHAR(100),
    duration_seconds NUMERIC(6,2),
    resolution VARCHAR(20),
    camera_channel INT,
    storage_provider VARCHAR(20) NOT NULL CHECK (storage_provider IN (
        'AWS_S3', 'AZURE_BLOB', 'GCP_STORAGE', 'LOCAL_FS'
    )),
    retention_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Apply updated_at trigger to camera_devices
CREATE TRIGGER trg_camera_devices_updated_at BEFORE UPDATE ON camera_devices FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes for performance
CREATE INDEX idx_camera_devices_veh ON camera_devices(tenant_id, vehicle_id);
CREATE INDEX idx_camera_devices_lookup ON camera_devices(tenant_id, camera_type, status);

CREATE INDEX idx_video_events_veh_at ON video_events(tenant_id, vehicle_id, event_at DESC);
CREATE INDEX idx_video_events_type_at ON video_events(tenant_id, event_type, event_at DESC);
CREATE INDEX idx_video_events_pending_review ON video_events(tenant_id, severity, reviewed) WHERE reviewed = FALSE;

CREATE INDEX idx_media_assets_event ON media_assets(tenant_id, video_event_id);
CREATE INDEX idx_media_assets_veh_at ON media_assets(tenant_id, vehicle_id, created_at DESC);
CREATE INDEX idx_media_assets_type ON media_assets(tenant_id, asset_type);

-- Comments
COMMENT ON TABLE camera_devices IS 'Connected camera models (dashcams, cabin cameras) associated with tracking devices';
COMMENT ON TABLE video_events IS 'ADAS (Advanced Driver Assistance Systems) or DMS (Driver Monitoring Systems) events captured on-camera';
COMMENT ON TABLE media_assets IS 'Video files, images, or audio snippets uploaded from vehicle cameras';
