-- V003__seed_auth_data.sql
-- Seed data specifically for Auth Service (plans, tenants, users, roles, permissions)

-- 1. Subscription Plans (3 tiers)
INSERT INTO subscription_plans (id, name, code, tier, price_per_vehicle_monthly, max_vehicles, max_users, max_geofences, features, is_active) VALUES
('a1b2c3d4-0001-4000-8000-000000000001', 'Basic', 'BASIC', 'BASIC', 99.00, 50, 10, 100, '{"live_tracking": true, "fuel_monitoring": true, "basic_reports": true, "geofencing": true, "driver_scoring": false, "predictive_maintenance": false, "ev_analytics": false, "api_access": false, "custom_reports": false, "whatsapp_alerts": false}', true),
('a1b2c3d4-0002-4000-8000-000000000002', 'Professional', 'PROFESSIONAL', 'PROFESSIONAL', 199.00, 500, 50, 1000, '{"live_tracking": true, "fuel_monitoring": true, "basic_reports": true, "geofencing": true, "driver_scoring": true, "predictive_maintenance": true, "ev_analytics": false, "api_access": true, "custom_reports": true, "whatsapp_alerts": true, "fuel_theft_detection": true, "video_telematics": false}', true),
('a1b2c3d4-0003-4000-8000-000000000003', 'Enterprise', 'ENTERPRISE', 'ENTERPRISE', 349.00, NULL, NULL, 10000, '{"live_tracking": true, "fuel_monitoring": true, "basic_reports": true, "geofencing": true, "driver_scoring": true, "predictive_maintenance": true, "ev_analytics": true, "api_access": true, "custom_reports": true, "whatsapp_alerts": true, "fuel_theft_detection": true, "video_telematics": true, "ai_coaching": true, "sso": true, "dedicated_support": true}', true);


-- 2. System Roles (10 roles from PRD)
INSERT INTO roles (id, name, display_name, description, is_system_role, hierarchy_level) VALUES
('d4e5f6a7-0001-4000-8000-000000000001', 'SUPER_ADMIN', 'Super Administrator', 'Overall platform administration bypass', true, 0),
('d4e5f6a7-0002-4000-8000-000000000002', 'TENANT_ADMIN', 'Tenant Administrator', 'Administrative control for a specific tenant organization', true, 1),
('d4e5f6a7-0003-4000-8000-000000000003', 'FLEET_MANAGER', 'Fleet Manager', 'Oversees general vehicles operation, routes and alerts', true, 2),
('d4e5f6a7-0004-4000-8000-000000000004', 'DEPOT_MANAGER', 'Depot Manager', 'Manages a specific depot and assigned vehicle/driver roster', true, 3),
('d4e5f6a7-0005-4000-8000-000000000005', 'OPERATIONS_SUPERVISOR', 'Operations Supervisor', 'Monitors live routes and handles real-time alerts', true, 3),
('d4e5f6a7-0006-4000-8000-000000000006', 'MAINTENANCE_MANAGER', 'Maintenance Manager', 'Plans preventive schedules and oversees vehicle workshop logs', true, 3),
('d4e5f6a7-0007-4000-8000-000000000007', 'WORKSHOP_TECHNICIAN', 'Workshop Technician', 'Executes work orders and uploads service reports', true, 4),
('d4e5f6a7-0008-4000-8000-000000000008', 'FINANCE_MANAGER', 'Finance Manager', 'Reviews fuel card logs, toll receipts, and maintenance costs', true, 3),
('d4e5f6a7-0009-4000-8000-000000000009', 'DRIVER', 'Driver', 'Vehicle operator with self-access to trips and scorecards', true, 5),
('d4e5f6a7-0010-4000-8000-000000000010', 'API_USER', 'API Integration User', 'Programmatic account for external developer systems', true, 3);


-- 3. Permissions (~60 permissions)
INSERT INTO permissions (code, module, action, description) VALUES
('vehicles:view', 'vehicles', 'view', 'View vehicles list and metadata'),
('vehicles:create', 'vehicles', 'create', 'Add new vehicles to the fleet'),
('vehicles:update', 'vehicles', 'update', 'Modify vehicle configurations'),
('vehicles:delete', 'vehicles', 'delete', 'Soft-delete vehicles from inventory'),
('vehicles:view_live', 'vehicles', 'view_live', 'Monitor live vehicle status and coordinates'),
('vehicles:view_health', 'vehicles', 'view_health', 'Access real-time vehicle OBD-II parameters'),
('drivers:view', 'drivers', 'view', 'View driver profiles and contact logs'),
('drivers:create', 'drivers', 'create', 'Onboard new drivers'),
('drivers:update', 'drivers', 'update', 'Edit driver profiles'),
('drivers:delete', 'drivers', 'delete', 'Deactivate or soft-delete drivers'),
('drivers:view_score', 'drivers', 'view_score', 'Access driver safety score calculations'),
('drivers:assign', 'drivers', 'assign', 'Assign drivers to vehicles'),
('tracking:view_live', 'tracking', 'view_live', 'Access real-time map views'),
('tracking:view_history', 'tracking', 'view_history', 'Playback historical GPS routes'),
('tracking:share_link', 'tracking', 'share_link', 'Generate external public tracking link'),
('tracking:view_heatmap', 'tracking', 'view_heatmap', 'Access route occupancy hot-spots'),
('fuel:view', 'fuel', 'view', 'Access fuel readings and consumption logs'),
('fuel:view_events', 'fuel', 'view_events', 'View refuel and fuel theft alerts'),
('fuel:manage_thresholds', 'fuel', 'manage_thresholds', 'Modify tank level alert parameters'),
('fuel:view_cards', 'fuel', 'view_cards', 'View fuel card balances and transaction matching'),
('geofences:view', 'geofences', 'view', 'View geofences on map'),
('geofences:create', 'geofences', 'create', 'Create circular or polygon zones'),
('geofences:update', 'geofences', 'update', 'Modify boundary shapes and active hours'),
('geofences:delete', 'geofences', 'delete', 'Remove geofences'),
('geofences:view_events', 'geofences', 'view_events', 'View geofence entrance and exit logs'),
('geofences:import', 'geofences', 'import', 'Bulk upload KML/GeoJSON boundaries'),
('alerts:view', 'alerts', 'view', 'Access live alarms panel'),
('alerts:acknowledge', 'alerts', 'acknowledge', 'Mark alerts as acknowledged'),
('alerts:dismiss', 'alerts', 'dismiss', 'Dismiss alerts with comments'),
('alerts:configure_rules', 'alerts', 'configure_rules', 'Set severity thresholds and alerts definitions'),
('maintenance:view', 'maintenance', 'view', 'View maintenance dashboard'),
('maintenance:create_schedule', 'maintenance', 'create_schedule', 'Establish preventive service triggers'),
('maintenance:manage_predictions', 'maintenance', 'manage_predictions', 'Access component health predictive graphs'),
('maintenance:create_work_order', 'maintenance', 'create_work_order', 'Create workshop repair jobs'),
('maintenance:update_work_order', 'maintenance', 'update_work_order', 'Update technician notes and parts log'),
('maintenance:view_cost', 'maintenance', 'view_cost', 'Access workshop invoice dashboards'),
('reports:view', 'reports', 'view', 'View reports builder templates'),
('reports:create', 'reports', 'create', 'Run manual PDF/CSV reports'),
('reports:schedule', 'reports', 'schedule', 'Establish report email automation rules'),
('reports:export', 'reports', 'export', 'Export raw tabular outputs'),
('analytics:view_dashboard', 'analytics', 'view_dashboard', 'Access executives analytics charts'),
('analytics:configure_widgets', 'analytics', 'configure_widgets', 'Rearrange dashboard layout tiles'),
('admin:manage_users', 'admin', 'manage_users', 'Create and modify tenant user profiles'),
('admin:manage_roles', 'admin', 'manage_roles', 'Modify role-permission sets'),
('admin:manage_tenants', 'admin', 'manage_tenants', 'Platform-wide tenant billing and provisioning'),
('admin:manage_billing', 'admin', 'manage_billing', 'Access billing details and pay invoices'),
('admin:view_audit_logs', 'admin', 'view_audit_logs', 'Access system security and change audit trails'),
('admin:manage_api_keys', 'admin', 'manage_api_keys', 'Generate programmatic API tokens'),
('ev:view', 'ev', 'view', 'Access EV battery and SOC monitoring'),
('ev:manage_charging', 'ev', 'manage_charging', 'Configure charging station schedules'),
('ev:view_battery_health', 'ev', 'view_battery_health', 'View battery degradation (SOH) history'),
('documents:view', 'documents', 'view', 'View compliance vehicle/driver certificates'),
('documents:upload', 'documents', 'upload', 'Upload file attachments for validation'),
('documents:verify', 'documents', 'verify', 'Mark documents as validated or rejected'),
('documents:delete', 'documents', 'delete', 'Delete documents'),
('camera:view', 'camera', 'view', 'View active dashcam feeds and event clips'),
('camera:review_events', 'camera', 'review_events', 'Review ADAS/DMS warning clips'),
('camera:manage_devices', 'camera', 'manage_devices', 'Onboard camera hardware channels'),
('firmware:view', 'firmware', 'view', 'View OTA firmware versions'),
('firmware:deploy', 'firmware', 'deploy', 'Trigger bulk firmware rollouts'),
('firmware:manage_packages', 'firmware', 'manage_packages', 'Upload OTA package binaries'),
('notifications:view', 'notifications', 'view', 'View personal push notifications inbox'),
('notifications:configure', 'notifications', 'configure', 'Configure personal notification categories'),
('notifications:manage_channels', 'notifications', 'manage_channels', 'Configure SMS/Email gateway credentials');


-- 4. Dynamic Role-Permission mappings
-- Super Admin gets everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'd4e5f6a7-0001-4000-8000-000000000001', id FROM permissions;

-- Tenant Admin gets everything except global platform administration
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'd4e5f6a7-0002-4000-8000-000000000002', id FROM permissions 
WHERE code != 'admin:manage_tenants';

-- Fleet Manager mappings
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'd4e5f6a7-0003-4000-8000-000000000003', id FROM permissions 
WHERE module IN ('vehicles', 'drivers', 'tracking', 'fuel', 'geofences', 'alerts', 'reports', 'analytics', 'ev', 'documents', 'notifications')
AND code NOT IN ('vehicles:delete', 'drivers:delete', 'geofences:delete', 'documents:delete', 'admin:manage_roles');

-- Depot Manager mappings
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'd4e5f6a7-0004-4000-8000-000000000004', id FROM permissions 
WHERE code IN (
    'vehicles:view', 'vehicles:view_live', 'vehicles:view_health',
    'drivers:view', 'drivers:view_score', 'drivers:assign',
    'tracking:view_live', 'tracking:view_history',
    'alerts:view', 'alerts:acknowledge',
    'maintenance:view', 'maintenance:create_work_order',
    'reports:view', 'reports:create', 'reports:export',
    'documents:view', 'documents:upload',
    'camera:view'
);

-- Operations Supervisor mappings
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'd4e5f6a7-0005-4000-8000-000000000005', id FROM permissions 
WHERE code IN (
    'vehicles:view', 'vehicles:view_live',
    'tracking:view_live', 'tracking:view_history', 'tracking:share_link',
    'alerts:view', 'alerts:acknowledge', 'alerts:dismiss',
    'geofences:view', 'geofences:view_events',
    'camera:view', 'camera:review_events'
);

-- Maintenance Manager mappings
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'd4e5f6a7-0006-4000-8000-000000000006', id FROM permissions 
WHERE module IN ('maintenance', 'ev') OR code IN (
    'vehicles:view', 'vehicles:view_health', 'vehicles:update',
    'devices:view', 'documents:view', 'documents:upload', 'documents:verify'
);

-- Workshop Technician mappings
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'd4e5f6a7-0007-4000-8000-000000000007', id FROM permissions 
WHERE code IN (
    'vehicles:view', 'vehicles:view_health',
    'maintenance:view', 'maintenance:update_work_order'
);

-- Finance Manager mappings
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'd4e5f6a7-0008-4000-8000-000000000008', id FROM permissions 
WHERE code IN (
    'vehicles:view', 'fuel:view', 'fuel:view_events', 'fuel:view_cards',
    'maintenance:view', 'maintenance:view_cost',
    'reports:view', 'reports:create', 'reports:export',
    'analytics:view_dashboard'
);

-- Driver mappings
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'd4e5f6a7-0009-4000-8000-000000000009', id FROM permissions 
WHERE code IN (
    'vehicles:view', 'drivers:view_score',
    'tracking:view_live',
    'fuel:view',
    'notifications:view', 'notifications:configure'
);

-- API Integration User mappings
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'd4e5f6a7-0010-4000-8000-000000000010', id FROM permissions 
WHERE code IN (
    'vehicles:view', 'vehicles:view_live', 'vehicles:view_health',
    'tracking:view_live', 'tracking:view_history',
    'alerts:view', 'fuel:view'
);


-- 5. Demo Tenant Environment (FleetIQ Demo Corp)
-- 5.1 Tenant
INSERT INTO tenants (id, name, slug, domain, status, subscription_plan_id, max_vehicles, max_users, settings, timezone, country_code) VALUES
('b2c3d4e5-1111-4000-8000-000000000001', 'FleetIQ Demo Corp', 'fleetiq-demo', 'demo.fleetiq.com', 'ACTIVE', 'a1b2c3d4-0003-4000-8000-000000000003', 500, 100, '{"billing_active": true}', 'Asia/Kolkata', 'IN');

-- 5.2 Tenant Settings
INSERT INTO tenant_settings (id, tenant_id, default_speed_unit, default_fuel_unit, default_distance_unit, default_temperature_unit, fuel_theft_threshold_litres, fuel_theft_time_window_sec, fuel_consumption_alert_rate, harsh_accel_threshold, harsh_brake_threshold, harsh_corner_threshold, speeding_tolerance_pct, idle_timeout_minutes, data_retention_gps_days, data_retention_fuel_days, data_retention_obd_days) VALUES
('b2c3d4e5-2222-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'KMH', 'LITRES', 'KM', 'C', 5.00, 120, 1.20, 2.50, -3.00, 2.50, 10, 5, 90, 90, 60);

-- 5.3 Tenant Subscription
INSERT INTO tenant_subscriptions (id, tenant_id, plan_id, status, started_at, expires_at, trial_ends_at, billing_cycle) VALUES
('b2c3d4e5-3333-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'a1b2c3d4-0003-4000-8000-000000000003', 'TRIAL', NOW() - INTERVAL '5 days', NOW() + INTERVAL '25 days', NOW() + INTERVAL '25 days', 'MONTHLY');

-- 5.4 Users (Admin, Fleet Manager, Driver)
INSERT INTO users (id, tenant_id, email, password_hash, first_name, last_name, phone, status, email_verified) VALUES
('e5f6a7b8-1111-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'admin@demo.com', '$2b$12$DummyHashForSecurityVerificationAdminUserPassedOk', 'Ramesh', 'Sharma', '+918888888881', 'ACTIVE', true),
('e5f6a7b8-2222-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'manager@demo.com', '$2b$12$DummyHashForSecurityVerificationManagerUserPassedOk', 'Sunita', 'Patel', '+918888888882', 'ACTIVE', true),
('e5f6a7b8-3333-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'driver1@demo.com', '$2b$12$DummyHashForSecurityVerificationDriverUserPassedOk', 'Amit', 'Kumar', '+918888888883', 'ACTIVE', true),
('e5f6a7b8-3333-4000-8000-000000000002', 'b2c3d4e5-1111-4000-8000-000000000001', 'driver2@demo.com', '$2b$12$DummyHashForSecurityVerificationDriverUserPassedOk', 'Rajesh', 'Yadav', '+918888888884', 'ACTIVE', true),
('e5f6a7b8-3333-4000-8000-000000000003', 'b2c3d4e5-1111-4000-8000-000000000001', 'driver3@demo.com', '$2b$12$DummyHashForSecurityVerificationDriverUserPassedOk', 'Vikram', 'Singh', '+918888888885', 'ACTIVE', true);

-- Assign User Roles
INSERT INTO user_roles (user_id, role_id, assigned_by) VALUES
('e5f6a7b8-1111-4000-8000-000000000001', 'd4e5f6a7-0002-4000-8000-000000000002', 'e5f6a7b8-1111-4000-8000-000000000001'), -- admin is Tenant Admin
('e5f6a7b8-2222-4000-8000-000000000001', 'd4e5f6a7-0003-4000-8000-000000000003', 'e5f6a7b8-1111-4000-8000-000000000001'), -- Sunita is Fleet Manager
('e5f6a7b8-3333-4000-8000-000000000001', 'd4e5f6a7-0009-4000-8000-000000000009', 'e5f6a7b8-2222-4000-8000-000000000001'), -- Amit is Driver
('e5f6a7b8-3333-4000-8000-000000000002', 'd4e5f6a7-0009-4000-8000-000000000009', 'e5f6a7b8-2222-4000-8000-000000000001'), -- Rajesh is Driver
('e5f6a7b8-3333-4000-8000-000000000003', 'd4e5f6a7-0009-4000-8000-000000000009', 'e5f6a7b8-2222-4000-8000-000000000001'); -- Vikram is Driver
