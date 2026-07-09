-- ============================================================
-- FleetIQ Demo Seed Data (v4 - Dynamic, Idempotent)
-- Uses subqueries so it works even if partial data already exists
-- Login: admin@omega.com / Admin@123  |  Tenant: omega-logistics
-- ============================================================

-- ── 1. Subscription Plan ────────────────────────────────────
INSERT INTO subscription_plans (id, name, code, tier, price_per_vehicle_monthly, max_vehicles, max_users, features, is_active)
VALUES (
    'aaaaaaaa-0001-0000-0000-000000000001',
    'Enterprise', 'ENTERPRISE', 'ENTERPRISE',
    49.99, 500, 100,
    '{"realtime_tracking": true, "ai_scoring": true, "fuel_analytics": true}',
    true
)
ON CONFLICT (code) DO NOTHING;

-- ── 2. Tenant ───────────────────────────────────────────────
INSERT INTO tenants (id, name, slug, status, subscription_plan_id, max_vehicles, max_users, timezone, country_code)
VALUES (
    'bbbbbbbb-0001-0000-0000-000000000001',
    'Omega Logistics Pvt. Ltd.',
    'omega-logistics', 'ACTIVE',
    (SELECT id FROM subscription_plans WHERE code = 'ENTERPRISE' LIMIT 1),
    100, 50, 'Asia/Kolkata', 'IN'
)
ON CONFLICT (slug) DO NOTHING;

-- ── 3. Tenant Settings (uses actual tenant ID from DB) ──────
INSERT INTO tenant_settings (id, tenant_id)
SELECT
    'cccccccc-0001-0000-0000-000000000001',
    id
FROM tenants WHERE slug = 'omega-logistics'
ON CONFLICT (tenant_id) DO NOTHING;

-- ── 4. Roles ────────────────────────────────────────────────
INSERT INTO roles (id, name, display_name, is_system_role, hierarchy_level)
VALUES
    ('dddddddd-0001-0000-0000-000000000001', 'SUPER_ADMIN',   'Super Administrator', true, 100),
    ('dddddddd-0001-0000-0000-000000000002', 'FLEET_ADMIN',   'Fleet Administrator', true, 80),
    ('dddddddd-0001-0000-0000-000000000003', 'FLEET_MANAGER', 'Fleet Manager',       true, 60),
    ('dddddddd-0001-0000-0000-000000000004', 'DRIVER',        'Driver',              true, 20)
ON CONFLICT (name) DO NOTHING;

-- ── 5. Admin User ────────────────────────────────────────────
-- Password: Admin@123
INSERT INTO users (id, tenant_id, email, password_hash, first_name, last_name, phone, status, email_verified)
SELECT
    'eeeeeeee-0001-0000-0000-000000000001',
    t.id,
    'admin@omega.com',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
    'Admin', 'Omega', '+91 98765 00000', 'ACTIVE', true
FROM tenants t WHERE t.slug = 'omega-logistics'
ON CONFLICT (tenant_id, email) DO NOTHING;

-- Assign FLEET_ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT
    u.id,
    r.id
FROM users u, roles r
WHERE u.email = 'admin@omega.com'
  AND r.name = 'FLEET_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ── 6. Depots ────────────────────────────────────────────────
INSERT INTO depots (id, tenant_id, name, code, address, city, state, country)
SELECT 'ffffffff-0001-0000-0000-000000000001', t.id,
       'Bangalore Central Depot', 'BLR-01', 'Whitefield Industrial Area', 'Bangalore', 'Karnataka', 'India'
FROM tenants t WHERE t.slug = 'omega-logistics'
  AND NOT EXISTS (SELECT 1 FROM depots WHERE id = 'ffffffff-0001-0000-0000-000000000001');

INSERT INTO depots (id, tenant_id, name, code, address, city, state, country)
SELECT 'ffffffff-0001-0000-0000-000000000002', t.id,
       'Mumbai Hub', 'MUM-01', 'Andheri East MIDC', 'Mumbai', 'Maharashtra', 'India'
FROM tenants t WHERE t.slug = 'omega-logistics'
  AND NOT EXISTS (SELECT 1 FROM depots WHERE id = 'ffffffff-0001-0000-0000-000000000002');

-- ── 7. Vehicle Types ─────────────────────────────────────────
INSERT INTO vehicle_types (id, name, category, fuel_type, default_fuel_capacity_litres)
VALUES
    ('11111111-0001-0000-0000-000000000001', 'Heavy Truck 40T', 'TRUCK_HEAVY', 'DIESEL', 500.0),
    ('11111111-0001-0000-0000-000000000002', 'Mini Truck 3.5T',  'TRUCK_LIGHT', 'DIESEL', 120.0)
ON CONFLICT (name) DO NOTHING;

-- ── 8. Vehicles ──────────────────────────────────────────────
INSERT INTO vehicles (id, tenant_id, vehicle_type_id, depot_id, registration_number, make, model, year_of_manufacture, status, fuel_tank_capacity_litres, odometer_reading_km)
SELECT '22222222-0001-0000-0000-000000000001', t.id,
    (SELECT id FROM vehicle_types WHERE name = 'Heavy Truck 40T'),
    'ffffffff-0001-0000-0000-000000000001',
    'KA-01-MJ-1024', 'TATA', 'Prima 4930.S', 2022, 'ACTIVE', 400.0, 45210.5
FROM tenants t WHERE t.slug = 'omega-logistics'
ON CONFLICT (tenant_id, registration_number) DO NOTHING;

INSERT INTO vehicles (id, tenant_id, vehicle_type_id, depot_id, registration_number, make, model, year_of_manufacture, status, fuel_tank_capacity_litres, odometer_reading_km)
SELECT '22222222-0001-0000-0000-000000000002', t.id,
    (SELECT id FROM vehicle_types WHERE name = 'Heavy Truck 40T'),
    'ffffffff-0001-0000-0000-000000000001',
    'KA-03-MK-4512', 'Leyland', 'U-Truck 3718', 2021, 'ACTIVE', 350.0, 31250.2
FROM tenants t WHERE t.slug = 'omega-logistics'
ON CONFLICT (tenant_id, registration_number) DO NOTHING;

INSERT INTO vehicles (id, tenant_id, vehicle_type_id, depot_id, registration_number, make, model, year_of_manufacture, status, fuel_tank_capacity_litres, odometer_reading_km)
SELECT '22222222-0001-0000-0000-000000000003', t.id,
    (SELECT id FROM vehicle_types WHERE name = 'Heavy Truck 40T'),
    'ffffffff-0001-0000-0000-000000000002',
    'MH-12-NN-8899', 'Volvo', 'FMX 460', 2020, 'IN_MAINTENANCE', 450.0, 98120.4
FROM tenants t WHERE t.slug = 'omega-logistics'
ON CONFLICT (tenant_id, registration_number) DO NOTHING;

INSERT INTO vehicles (id, tenant_id, vehicle_type_id, depot_id, registration_number, make, model, year_of_manufacture, status, fuel_tank_capacity_litres, odometer_reading_km)
SELECT '22222222-0001-0000-0000-000000000004', t.id,
    (SELECT id FROM vehicle_types WHERE name = 'Mini Truck 3.5T'),
    'ffffffff-0001-0000-0000-000000000002',
    'DL-01-AA-5678', 'BharatBenz', '3528C', 2023, 'ACTIVE', 380.0, 18450.0
FROM tenants t WHERE t.slug = 'omega-logistics'
ON CONFLICT (tenant_id, registration_number) DO NOTHING;

-- ── 9. Drivers ───────────────────────────────────────────────
INSERT INTO drivers (id, tenant_id, first_name, last_name, employee_id, email, phone, license_number, license_expiry, status)
SELECT '33333333-0001-0000-0000-000000000001', t.id, 'Rajesh', 'Kumar',  'EMP-001', 'rajesh@omega.com', '+91 98765 43210', 'DL-KA-123456', '2028-10-15', 'ACTIVE'
FROM tenants t WHERE t.slug = 'omega-logistics'
ON CONFLICT (tenant_id, employee_id) DO NOTHING;

INSERT INTO drivers (id, tenant_id, first_name, last_name, employee_id, email, phone, license_number, license_expiry, status)
SELECT '33333333-0001-0000-0000-000000000002', t.id, 'Priya', 'Sharma', 'EMP-002', 'priya@omega.com',  '+91 87654 32109', 'DL-KA-654321', '2029-05-12', 'ACTIVE'
FROM tenants t WHERE t.slug = 'omega-logistics'
ON CONFLICT (tenant_id, employee_id) DO NOTHING;

INSERT INTO drivers (id, tenant_id, first_name, last_name, employee_id, email, phone, license_number, license_expiry, status)
SELECT '33333333-0001-0000-0000-000000000003', t.id, 'Suresh', 'Patel', 'EMP-003', 'suresh@omega.com', '+91 76543 21098', 'DL-MH-987654', '2027-11-20', 'ACTIVE'
FROM tenants t WHERE t.slug = 'omega-logistics'
ON CONFLICT (tenant_id, employee_id) DO NOTHING;

INSERT INTO drivers (id, tenant_id, first_name, last_name, employee_id, email, phone, license_number, license_expiry, status)
SELECT '33333333-0001-0000-0000-000000000004', t.id, 'Anjali', 'Singh', 'EMP-004', 'anjali@omega.com', '+91 65432 10987', 'DL-DL-456789', '2030-01-05', 'ON_LEAVE'
FROM tenants t WHERE t.slug = 'omega-logistics'
ON CONFLICT (tenant_id, employee_id) DO NOTHING;

-- ── 10. Driver Assignments ───────────────────────────────────
INSERT INTO driver_assignments (id, tenant_id, driver_id, vehicle_id, status, shift_start)
SELECT '44444444-0001-0000-0000-000000000001', t.id,
    (SELECT id FROM drivers WHERE employee_id = 'EMP-001'),
    (SELECT id FROM vehicles WHERE registration_number = 'KA-01-MJ-1024'),
    'ACTIVE', NOW()
FROM tenants t WHERE t.slug = 'omega-logistics'
  AND NOT EXISTS (SELECT 1 FROM driver_assignments WHERE id = '44444444-0001-0000-0000-000000000001');

INSERT INTO driver_assignments (id, tenant_id, driver_id, vehicle_id, status, shift_start)
SELECT '44444444-0001-0000-0000-000000000002', t.id,
    (SELECT id FROM drivers WHERE employee_id = 'EMP-002'),
    (SELECT id FROM vehicles WHERE registration_number = 'KA-03-MK-4512'),
    'ACTIVE', NOW()
FROM tenants t WHERE t.slug = 'omega-logistics'
  AND NOT EXISTS (SELECT 1 FROM driver_assignments WHERE id = '44444444-0001-0000-0000-000000000002');

INSERT INTO driver_assignments (id, tenant_id, driver_id, vehicle_id, status, shift_start)
SELECT '44444444-0001-0000-0000-000000000003', t.id,
    (SELECT id FROM drivers WHERE employee_id = 'EMP-003'),
    (SELECT id FROM vehicles WHERE registration_number = 'DL-01-AA-5678'),
    'ACTIVE', NOW()
FROM tenants t WHERE t.slug = 'omega-logistics'
  AND NOT EXISTS (SELECT 1 FROM driver_assignments WHERE id = '44444444-0001-0000-0000-000000000003');

-- ============================================================
-- ✅ Done! Login credentials:
--    URL:      https://fleet-iq-lilac.vercel.app/
--    Email:    admin@omega.com
--    Password: Admin@123
--    Tenant:   omega-logistics
-- ============================================================
