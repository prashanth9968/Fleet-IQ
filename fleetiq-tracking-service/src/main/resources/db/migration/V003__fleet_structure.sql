-- V003__fleet_structure.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. depots
CREATE TABLE depots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    postal_code VARCHAR(20),
    location GEOMETRY(Point, 4326),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(255),
    capacity INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- 2. vehicle_groups
CREATE TABLE vehicle_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    color VARCHAR(7), -- Hex code for map markers, e.g. #FF5733
    parent_group_id UUID REFERENCES vehicle_groups(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- 3. vehicle_group_members
-- Note: FK constraint on vehicle_id will be added in V004 after vehicles table is created.
CREATE TABLE vehicle_group_members (
    vehicle_group_id UUID NOT NULL REFERENCES vehicle_groups(id) ON DELETE CASCADE,
    vehicle_id UUID NOT NULL,
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (vehicle_group_id, vehicle_id)
);

-- 4. user_depot_assignments
CREATE TABLE user_depot_assignments (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    depot_id UUID NOT NULL REFERENCES depots(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    PRIMARY KEY (user_id, depot_id)
);

-- Apply updated_at triggers
CREATE TRIGGER trg_depots_updated_at BEFORE UPDATE ON depots FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_vehicle_groups_updated_at BEFORE UPDATE ON vehicle_groups FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes for performance and geospatial queries
CREATE INDEX idx_depots_tenant ON depots(tenant_id);
CREATE INDEX idx_depots_location ON depots USING GIST(location);
CREATE INDEX idx_vehicle_groups_tenant ON vehicle_groups(tenant_id);

-- Comments
COMMENT ON TABLE depots IS 'Physical hubs/stations where vehicles are stationed and drivers operate';
COMMENT ON TABLE vehicle_groups IS 'Logical groupings of vehicles for access control and bulk operations';
COMMENT ON TABLE vehicle_group_members IS 'Many-to-many relationship mapping vehicles to their vehicle groups';
COMMENT ON TABLE user_depot_assignments IS 'Mapping of depot managers and supervisors to physical depots they manage';
