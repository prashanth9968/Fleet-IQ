-- V002__tenant_and_auth.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. subscription_plans
CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    tier VARCHAR(20) NOT NULL CHECK (tier IN ('BASIC', 'PROFESSIONAL', 'ENTERPRISE')),
    price_per_vehicle_monthly NUMERIC(10,2) NOT NULL,
    max_vehicles INT,
    max_users INT,
    max_geofences INT DEFAULT 1000,
    features JSONB NOT NULL DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. tenants
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    domain VARCHAR(255),
    logo_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'TRIAL', 'CANCELLED')),
    subscription_plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    max_vehicles INT DEFAULT 50,
    max_users INT DEFAULT 20,
    settings JSONB NOT NULL DEFAULT '{}',
    timezone VARCHAR(50) DEFAULT 'Asia/Kolkata',
    country_code VARCHAR(3) DEFAULT 'IN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- 3. tenant_subscriptions
CREATE TABLE tenant_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'TRIAL', 'EXPIRED', 'CANCELLED')),
    started_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    trial_ends_at TIMESTAMPTZ,
    billing_cycle VARCHAR(20) CHECK (billing_cycle IN ('MONTHLY', 'QUARTERLY', 'ANNUAL')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. tenant_settings
CREATE TABLE tenant_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) UNIQUE,
    default_speed_unit VARCHAR(10) DEFAULT 'KMH' CHECK (default_speed_unit IN ('KMH', 'MPH')),
    default_fuel_unit VARCHAR(10) DEFAULT 'LITRES' CHECK (default_fuel_unit IN ('LITRES', 'GALLONS')),
    default_distance_unit VARCHAR(10) DEFAULT 'KM' CHECK (default_distance_unit IN ('KM', 'MILES')),
    default_temperature_unit VARCHAR(5) DEFAULT 'C' CHECK (default_temperature_unit IN ('C', 'F')),
    fuel_theft_threshold_litres NUMERIC(6,2) DEFAULT 5.0,
    fuel_theft_time_window_sec INT DEFAULT 120,
    fuel_consumption_alert_rate NUMERIC(6,2) DEFAULT 1.0,
    harsh_accel_threshold NUMERIC(5,2) DEFAULT 2.5,
    harsh_brake_threshold NUMERIC(5,2) DEFAULT -3.0,
    harsh_corner_threshold NUMERIC(5,2) DEFAULT 2.5,
    speeding_tolerance_pct INT DEFAULT 10,
    idle_timeout_minutes INT DEFAULT 5,
    data_retention_gps_days INT DEFAULT 90,
    data_retention_fuel_days INT DEFAULT 90,
    data_retention_obd_days INT DEFAULT 60,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. roles
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
    hierarchy_level INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. permissions
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    module VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 7. role_permissions
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- 8. users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id), -- NULL for super admin
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING')),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMPTZ,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMPTZ,
    language VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(50) DEFAULT 'Asia/Kolkata',
    notification_preferences JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE (tenant_id, email)
);

-- 9. user_roles
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_by UUID REFERENCES users(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

-- 10. api_keys
CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    key_prefix VARCHAR(10) NOT NULL,
    scopes JSONB NOT NULL DEFAULT '[]',
    rate_limit_per_minute INT DEFAULT 60,
    expires_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ
);

-- 11. user_sessions
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(255) NOT NULL,
    device_info JSONB,
    ip_address INET,
    user_agent TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ
);

-- Trigger applications for updated_at
CREATE TRIGGER trg_subscription_plans_updated_at BEFORE UPDATE ON subscription_plans FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_tenants_updated_at BEFORE UPDATE ON tenants FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_tenant_subscriptions_updated_at BEFORE UPDATE ON tenant_subscriptions FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_tenant_settings_updated_at BEFORE UPDATE ON tenant_settings FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Indexes for performance
CREATE INDEX idx_users_tenant_email ON users(tenant_id, email);
CREATE INDEX idx_users_tenant_status ON users(tenant_id, status);
CREATE INDEX idx_api_keys_tenant_active ON api_keys(tenant_id, is_active);
CREATE INDEX idx_user_sessions_user_expires ON user_sessions(user_id, expires_at);

-- Comments
COMMENT ON TABLE subscription_plans IS 'Platform subscription plan tiers and resource limit profiles';
COMMENT ON TABLE tenants IS 'Organization accounts representing clients/customers in the system';
COMMENT ON TABLE tenant_subscriptions IS 'History of plans assigned to each tenant';
COMMENT ON TABLE tenant_settings IS 'Custom configuration overrides and parameters per tenant';
COMMENT ON TABLE roles IS 'Predefined and custom authorization roles (10 system roles)';
COMMENT ON TABLE permissions IS 'Fine-grained permissions linked to modules and actions';
COMMENT ON TABLE role_permissions IS 'Mapping table linking roles to their corresponding permissions';
COMMENT ON TABLE users IS 'User accounts belonging to tenants or system admins';
COMMENT ON TABLE user_roles IS 'Mapping table linking users to their assigned roles';
COMMENT ON TABLE api_keys IS 'Secure credentials used for programmatic API access';
COMMENT ON TABLE user_sessions IS 'Active user session and refresh token details';
