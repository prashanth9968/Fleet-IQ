-- V023__seed_data.sql
-- FleetIQ Fleet Management Platform
-- Target: PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x

-- 1. Subscription Plans (3 tiers from PRD)
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
-- Vehicles module
('vehicles:view', 'vehicles', 'view', 'View vehicles list and metadata'),
('vehicles:create', 'vehicles', 'create', 'Add new vehicles to the fleet'),
('vehicles:update', 'vehicles', 'update', 'Modify vehicle configurations'),
('vehicles:delete', 'vehicles', 'delete', 'Soft-delete vehicles from inventory'),
('vehicles:view_live', 'vehicles', 'view_live', 'Monitor live vehicle status and coordinates'),
('vehicles:view_health', 'vehicles', 'view_health', 'Access real-time vehicle OBD-II parameters'),

-- Drivers module
('drivers:view', 'drivers', 'view', 'View driver profiles and contact logs'),
('drivers:create', 'drivers', 'create', 'Onboard new drivers'),
('drivers:update', 'drivers', 'update', 'Edit driver profiles'),
('drivers:delete', 'drivers', 'delete', 'Deactivate or soft-delete drivers'),
('drivers:view_score', 'drivers', 'view_score', 'Access driver safety score calculations'),
('drivers:assign', 'drivers', 'assign', 'Assign drivers to vehicles'),

-- Tracking module
('tracking:view_live', 'tracking', 'view_live', 'Access real-time map views'),
('tracking:view_history', 'tracking', 'view_history', 'Playback historical GPS routes'),
('tracking:share_link', 'tracking', 'share_link', 'Generate external public tracking link'),
('tracking:view_heatmap', 'tracking', 'view_heatmap', 'Access route occupancy hot-spots'),

-- Fuel module
('fuel:view', 'fuel', 'view', 'Access fuel readings and consumption logs'),
('fuel:view_events', 'fuel', 'view_events', 'View refuel and fuel theft alerts'),
('fuel:manage_thresholds', 'fuel', 'manage_thresholds', 'Modify tank level alert parameters'),
('fuel:view_cards', 'fuel', 'view_cards', 'View fuel card balances and transaction matching'),

-- Geofences module
('geofences:view', 'geofences', 'view', 'View geofences on map'),
('geofences:create', 'geofences', 'create', 'Create circular or polygon zones'),
('geofences:update', 'geofences', 'update', 'Modify boundary shapes and active hours'),
('geofences:delete', 'geofences', 'delete', 'Remove geofences'),
('geofences:view_events', 'geofences', 'view_events', 'View geofence entrance and exit logs'),
('geofences:import', 'geofences', 'import', 'Bulk upload KML/GeoJSON boundaries'),

-- Alerts module
('alerts:view', 'alerts', 'view', 'Access live alarms panel'),
('alerts:acknowledge', 'alerts', 'acknowledge', 'Mark alerts as acknowledged'),
('alerts:dismiss', 'alerts', 'dismiss', 'Dismiss alerts with comments'),
('alerts:configure_rules', 'alerts', 'configure_rules', 'Set severity thresholds and alerts definitions'),

-- Maintenance module
('maintenance:view', 'maintenance', 'view', 'View maintenance dashboard'),
('maintenance:create_schedule', 'maintenance', 'create_schedule', 'Establish preventive service triggers'),
('maintenance:manage_predictions', 'maintenance', 'manage_predictions', 'Access component health predictive graphs'),
('maintenance:create_work_order', 'maintenance', 'create_work_order', 'Create workshop repair jobs'),
('maintenance:update_work_order', 'maintenance', 'update_work_order', 'Update technician notes and parts log'),
('maintenance:view_cost', 'maintenance', 'view_cost', 'Access workshop invoice dashboards'),

-- Reports module
('reports:view', 'reports', 'view', 'View reports builder templates'),
('reports:create', 'reports', 'create', 'Run manual PDF/CSV reports'),
('reports:schedule', 'reports', 'schedule', 'Establish report email automation rules'),
('reports:export', 'reports', 'export', 'Export raw tabular outputs'),

-- Analytics module
('analytics:view_dashboard', 'analytics', 'view_dashboard', 'Access executives analytics charts'),
('analytics:configure_widgets', 'analytics', 'configure_widgets', 'Rearrange dashboard layout tiles'),

-- Admin module
('admin:manage_users', 'admin', 'manage_users', 'Create and modify tenant user profiles'),
('admin:manage_roles', 'admin', 'manage_roles', 'Modify role-permission sets'),
('admin:manage_tenants', 'admin', 'manage_tenants', 'Platform-wide tenant billing and provisioning'),
('admin:manage_billing', 'admin', 'manage_billing', 'Access billing details and pay invoices'),
('admin:view_audit_logs', 'admin', 'view_audit_logs', 'Access system security and change audit trails'),
('admin:manage_api_keys', 'admin', 'manage_api_keys', 'Generate programmatic API tokens'),

-- EV module
('ev:view', 'ev', 'view', 'Access EV battery and SOC monitoring'),
('ev:manage_charging', 'ev', 'manage_charging', 'Configure charging station schedules'),
('ev:view_battery_health', 'ev', 'view_battery_health', 'View battery degradation (SOH) history'),

-- Documents module
('documents:view', 'documents', 'view', 'View compliance vehicle/driver certificates'),
('documents:upload', 'documents', 'upload', 'Upload file attachments for validation'),
('documents:verify', 'documents', 'verify', 'Mark documents as validated or rejected'),
('documents:delete', 'documents', 'delete', 'Delete documents'),

-- Camera module
('camera:view', 'camera', 'view', 'View active dashcam feeds and event clips'),
('camera:review_events', 'camera', 'review_events', 'Review ADAS/DMS warning clips'),
('camera:manage_devices', 'camera', 'manage_devices', 'Onboard camera hardware channels'),

-- Firmware module
('firmware:view', 'firmware', 'view', 'View OTA firmware versions'),
('firmware:deploy', 'firmware', 'deploy', 'Trigger bulk firmware rollouts'),
('firmware:manage_packages', 'firmware', 'manage_packages', 'Upload OTA package binaries'),

-- Notifications module
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

-- Depot Manager mappings (restricted to depot operations, but gets view and select rights)
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


-- 5. Vehicle Types (20 types)
INSERT INTO vehicle_types (id, name, category, fuel_type, default_fuel_capacity_litres, default_fuel_consumption_rate, icon) VALUES
('a7b8c9d0-0001-4000-8000-000000000001', 'Standard Sedan (Petrol)', 'SEDAN', 'PETROL', 50.00, 7.50, 'car-sedan'),
('a7b8c9d0-0002-4000-8000-000000000002', 'Standard SUV (Diesel)', 'SUV', 'DIESEL', 65.00, 9.20, 'car-suv'),
('a7b8c9d0-0003-4000-8000-000000000003', 'City Hatchback', 'HATCHBACK', 'PETROL', 40.00, 6.00, 'car-hatchback'),
('a7b8c9d0-0004-4000-8000-000000000004', 'Light Duty Delivery Truck', 'TRUCK_LIGHT', 'DIESEL', 120.00, 14.50, 'truck-delivery'),
('a7b8c9d0-0005-4000-8000-000000000005', 'Medium Freight Truck', 'TRUCK_MEDIUM', 'DIESEL', 250.00, 22.00, 'truck-medium'),
('a7b8c9d0-0006-4000-8000-000000000006', 'Heavy Hauler (Multi-Axle)', 'TRUCK_HEAVY', 'DIESEL', 450.00, 32.50, 'truck-heavy'),
('a7b8c9d0-0007-4000-8000-000000000007', 'Intercity Coach Bus', 'BUS', 'DIESEL', 350.00, 28.00, 'bus-coach'),
('a7b8c9d0-0008-4000-8000-000000000008', 'School/Staff Mini Bus', 'MINI_BUS', 'CNG', 150.00, 18.00, 'bus-mini'),
('a7b8c9d0-0009-4000-8000-000000000009', 'Field Operations Bike', 'TWO_WHEELER', 'PETROL', 12.00, 2.50, 'bike'),
('a7b8c9d0-0010-4000-8000-000000000010', 'Urban Cargo Loader (3W)', 'THREE_WHEELER', 'CNG', 45.00, 5.00, 'rickshaw'),
('a7b8c9d0-0011-4000-8000-000000000011', 'Backhoe Loader Excavator', 'CONSTRUCTION', 'DIESEL', 160.00, 12.00, 'excavator'),
('a7b8c9d0-0012-4000-8000-000000000012', 'Agricultural Harvester Tractor', 'AGRICULTURAL', 'DIESEL', 100.00, 8.50, 'tractor'),
('a7b8c9d0-0013-4000-8000-000000000013', 'Long-Range EV Car', 'EV_CAR', 'ELECTRIC', NULL, 15.00, 'ev-car'),
('a7b8c9d0-0014-4000-8000-000000000014', 'Electric Transit Bus', 'EV_BUS', 'ELECTRIC', NULL, 110.00, 'ev-bus'),
('a7b8c9d0-0015-4000-8000-000000000015', 'Electric Last-Mile Van', 'EV_TRUCK', 'ELECTRIC', NULL, 28.00, 'ev-van'),
('a7b8c9d0-0016-4000-8000-000000000016', 'Electric Delivery Scooter', 'EV_TWO_WHEELER', 'ELECTRIC', NULL, 4.00, 'ev-scooter'),
('a7b8c9d0-0017-4000-8000-000000000017', 'Electric Auto Cargo (3W)', 'EV_THREE_WHEELER', 'ELECTRIC', NULL, 12.00, 'ev-loader'),
('a7b8c9d0-0018-4000-8000-000000000018', 'Hybrid Commuter Sedan', 'SEDAN', 'HYBRID', 45.00, 4.80, 'car-hybrid'),
('a7b8c9d0-0019-4000-8000-000000000019', 'Hybrid Courier Van', 'SUV', 'HYBRID', 60.00, 6.20, 'van-hybrid'),
('a7b8c9d0-0020-4000-8000-000000000020', 'Hydrogen Fuel Cell Bus', 'BUS', 'HYDROGEN', 30.00, 1.20, 'bus-hydrogen');


-- 6. DTC Library (~100 codes)
INSERT INTO dtc_library (code, system, severity, description, possible_causes, recommended_action, sae_standard) VALUES
-- Powertrain Fuel/Air codes (P0100-P0199)
('P0100', 'ENGINE', 'MEDIUM', 'Mass or Volume Air Flow A Circuit Malfunction', 'Dirty MAF sensor, vacuum leaks, wiring damage', 'Clean MAF sensor, inspect intake ducting for leaks', 'J1979'),
('P0101', 'ENGINE', 'MEDIUM', 'Mass or Volume Air Flow A Circuit Range/Performance', 'Faulty MAF sensor, intake restrictions, vacuum leak', 'Inspect and clean air filter, replace MAF sensor if faulty', 'J1979'),
('P0110', 'ENGINE', 'LOW', 'Intake Air Temperature Sensor 1 Circuit Malfunction', 'Faulty IAT sensor, open/shorted wiring', 'Test sensor resistance, check connector pin security', 'J1979'),
('P0113', 'ENGINE', 'LOW', 'Intake Air Temperature Sensor 1 Circuit High Input', 'Failed IAT sensor, open ground circuit', 'Check connection at IAT, replace sensor', 'J1979'),
('P0115', 'ENGINE', 'HIGH', 'Engine Coolant Temperature Sensor 1 Malfunction', 'Faulty ECT sensor, low coolant level, stuck thermostat', 'Check coolant level, test thermostat operation, replace ECT', 'J1979'),
('P0117', 'ENGINE', 'HIGH', 'Engine Coolant Temperature Sensor 1 Circuit Low Input', 'Failed ECT sensor, shorted wiring', 'Inspect ECT wiring harness, replace ECT sensor', 'J1979'),
('P0118', 'ENGINE', 'HIGH', 'Engine Coolant Temperature Sensor 1 Circuit High Input', 'Open ECT sensor wiring, failed ECT sensor', 'Test ECT connector voltages, replace ECT sensor', 'J1979'),
('P0120', 'ENGINE', 'MEDIUM', 'Throttle/Pedal Position Sensor/Switch A Circuit Malfunction', 'Faulty TPS, damaged throttle body wiring', 'Inspect throttle body wiring, calibrate throttle sensor', 'J1979'),
('P0121', 'ENGINE', 'HIGH', 'Throttle/Pedal Position Sensor/Switch A Range/Performance', 'Throttle body build-up, worn throttle potentiometer', 'Clean throttle body plate, replace throttle position sensor', 'J1979'),
('P0122', 'ENGINE', 'HIGH', 'Throttle/Pedal Position Sensor/Switch A Circuit Low Input', 'Faulty TPS, wiring short to ground', 'Test TPS output voltage using OBD scan tool, repair wiring', 'J1979'),
('P0125', 'ENGINE', 'MEDIUM', 'Insufficient Coolant Temp for Closed Loop Fuel Control', 'Low coolant level, stuck-open thermostat, faulty ECT', 'Check coolant level, replace engine cooling thermostat', 'J1979'),
('P0128', 'ENGINE', 'LOW', 'Coolant Thermostat Coolant Temp Below Regulating Temp', 'Stuck-open thermostat, faulty coolant temperature sensor', 'Replace engine coolant thermostat', 'J1979'),
('P0130', 'ENGINE', 'MEDIUM', 'O2 Sensor Circuit Malfunction (Bank 1, Sensor 1)', 'Failed O2 sensor, exhaust leak near sensor, wiring failure', 'Inspect O2 sensor wiring, test sensor heater coil resistance', 'J1979'),
('P0133', 'ENGINE', 'LOW', 'O2 Sensor Circuit Slow Response (Bank 1, Sensor 1)', 'Contaminated O2 sensor tip, exhaust leak, outdated ECU code', 'Replace pre-catalyst O2 sensor', 'J1979'),
('P0134', 'ENGINE', 'MEDIUM', 'O2 Sensor Circuit No Activity Detected (Bank 1, Sensor 1)', 'Open circuit in wiring, failed O2 sensor element', 'Test O2 wiring harness, replace O2 sensor', 'J1979'),
('P0135', 'ENGINE', 'LOW', 'O2 Sensor Heater Circuit Malfunction (Bank 1, Sensor 1)', 'Blown heater circuit fuse, failed heater element in sensor', 'Check O2 sensor fuse, replace O2 sensor', 'J1979'),
('P0136', 'ENGINE', 'LOW', 'O2 Sensor Circuit Malfunction (Bank 1, Sensor 2)', 'Failed post-catalyst O2 sensor, wiring damage', 'Test wiring to rear O2 sensor, replace sensor', 'J1979'),
('P0141', 'ENGINE', 'LOW', 'O2 Sensor Heater Circuit Malfunction (Bank 1, Sensor 2)', 'Internal sensor heater failed', 'Replace rear O2 sensor', 'J1979'),
('P0171', 'ENGINE', 'HIGH', 'System Too Lean (Bank 1)', 'Vacuum leaks, weak fuel pump, clogged fuel injectors, bad MAF', 'Smoke test intake for vacuum leaks, check fuel pressure', 'J1979'),
('P0172', 'ENGINE', 'HIGH', 'System Too Rich (Bank 1)', 'Faulty fuel pressure regulator, leaky fuel injectors, dirty MAF', 'Check fuel pressure regulator, inspect injectors for leakage', 'J1979'),
('P0174', 'ENGINE', 'HIGH', 'System Too Lean (Bank 2)', 'Vacuum leaks, weak fuel pump, dirty MAF', 'Inspect intake boot on bank 2, test fuel pressure', 'J1979'),
('P0175', 'ENGINE', 'HIGH', 'System Too Rich (Bank 2)', 'Leaky injectors on bank 2, fuel pressure too high', 'Check fuel rail pressure, test bank 2 injectors', 'J1979'),
('P0190', 'ENGINE', 'HIGH', 'Fuel Rail Pressure Sensor A Circuit Malfunction', 'Faulty fuel rail pressure sensor, clogged fuel filter, wiring fault', 'Measure fuel pressure manually, test sensor, replace filter', 'J1979'),

-- Powertrain Fuel Injector/Misfire codes (P0200-P0399)
('P0200', 'ENGINE', 'CRITICAL', 'Injector Circuit Malfunction', 'Wiring harness fault, failed fuel injector, faulty PCM', 'Inspect fuel injector wiring harness, test injector resistance', 'J1979'),
('P0201', 'ENGINE', 'CRITICAL', 'Injector Circuit Malfunction - Cylinder 1', 'Failed injector cylinder 1, bad wire harness connector', 'Test resistance on injector 1, check harness connector pin', 'J1979'),
('P0202', 'ENGINE', 'CRITICAL', 'Injector Circuit Malfunction - Cylinder 2', 'Failed injector cylinder 2', 'Test injector 2 resistance, replace injector if open/shorted', 'J1979'),
('P0203', 'ENGINE', 'CRITICAL', 'Injector Circuit Malfunction - Cylinder 3', 'Failed injector cylinder 3', 'Test injector 3, replace injector', 'J1979'),
('P0204', 'ENGINE', 'CRITICAL', 'Injector Circuit Malfunction - Cylinder 4', 'Failed injector cylinder 4', 'Test injector 4, replace injector', 'J1979'),
('P0300', 'ENGINE', 'CRITICAL', 'Random/Multiple Cylinder Misfire Detected', 'Worn spark plugs, bad ignition coils, low fuel pressure, vacuum leak', 'Inspect spark plugs and coils, test fuel rail pressure', 'J1979'),
('P0301', 'ENGINE', 'CRITICAL', 'Cylinder 1 Misfire Detected', 'Bad spark plug cylinder 1, bad ignition coil 1, low compression', 'Swap coil 1 to coil 2 to isolate, replace spark plug 1', 'J1979'),
('P0302', 'ENGINE', 'CRITICAL', 'Cylinder 2 Misfire Detected', 'Bad spark plug cylinder 2, bad ignition coil 2', 'Swap coil 2 to coil 3 to isolate, inspect spark plug 2', 'J1979'),
('P0303', 'ENGINE', 'CRITICAL', 'Cylinder 3 Misfire Detected', 'Bad spark plug cylinder 3, bad ignition coil 3', 'Swap coil 3 to coil 4 to isolate, inspect spark plug 3', 'J1979'),
('P0304', 'ENGINE', 'CRITICAL', 'Cylinder 4 Misfire Detected', 'Bad spark plug cylinder 4, bad ignition coil 4', 'Inspect spark plug 4, replace cylinder 4 ignition coil', 'J1979'),
('P0325', 'ENGINE', 'MEDIUM', 'Knock Sensor 1 Circuit Malfunction (Bank 1)', 'Failed knock sensor, corroded sensor connector, broken harness wire', 'Test knock sensor circuit resistance, replace knock sensor', 'J1979'),
('P0335', 'ENGINE', 'CRITICAL', 'Crankshaft Position Sensor A Circuit Malfunction', 'Broken sensor reluctor wheel, faulty sensor, engine wiring harness', 'Inspect crank sensor connector, test crank sensor pulse wave', 'J1979'),
('P0340', 'ENGINE', 'HIGH', 'Camshaft Position Sensor A Circuit Malfunction (Bank 1)', 'Failed camshaft sensor, bad timing chain alignment, bad wiring', 'Test camshaft position sensor, verify engine timing marks', 'J1979'),

-- Powertrain Emissions/Auxiliary (P0400-P0599)
('P0401', 'ENGINE', 'MEDIUM', 'Exhaust Gas Recirculation (EGR) Flow Insufficient Detected', 'Clogged EGR passage, failed EGR valve, faulty EGR sensor', 'Clean EGR passages, test EGR valve solenoid function', 'J1979'),
('P0402', 'ENGINE', 'MEDIUM', 'Exhaust Gas Recirculation (EGR) Flow Excessive Detected', 'Stuck-open EGR valve, failed EGR controller', 'Replace EGR valve assembly', 'J1979'),
('P0420', 'ENGINE', 'LOW', 'Catalyst System Efficiency Below Threshold (Bank 1)', 'Failed catalytic converter, exhaust leaks, rear O2 sensor reading bad', 'Inspect exhaust system for leaks, test catalyst temp, replace cat', 'J1979'),
('P0430', 'ENGINE', 'LOW', 'Catalyst System Efficiency Below Threshold (Bank 2)', 'Failed catalytic converter bank 2', 'Test catalytic converter bank 2, inspect for exhaust leaks', 'J1979'),
('P0440', 'ENGINE', 'LOW', 'Evaporative Emission Control System Malfunction', 'Loose gas cap, faulty charcoal canister, leaking EVAP hoses', 'Tighten gas cap, smoke-test EVAP hoses for vapor leaks', 'J1979'),
('P0442', 'ENGINE', 'LOW', 'Evaporative Emission Control System Leak Detected (Small Leak)', 'Loose fuel cap, minor crack in EVAP hose, faulty purge valve', 'Inspect gas cap seal, perform EVAP system smoke test', 'J1979'),
('P0455', 'ENGINE', 'MEDIUM', 'Evaporative Emission Control System Leak Detected (Gross Leak)', 'Gas cap missing/uncapped, loose purge solenoid hose', 'Inspect gas cap presence and seal, inspect EVAP purge lines', 'J1979'),
('P0460', 'ENGINE', 'LOW', 'Fuel Level Sensor A Circuit Malfunction', 'Corroded fuel pump/sender wiring, bad fuel level sending unit', 'Inspect fuel pump top hat connector, replace fuel sending unit', 'J1979'),
('P0500', 'ENGINE', 'HIGH', 'Vehicle Speed Sensor A Malfunction', 'Failed vehicle speed sensor, bad gear in transmission, bad wiring', 'Inspect speed sensor harness, test speed sensor output pulse', 'J1979'),
('P0505', 'ENGINE', 'MEDIUM', 'Idle Control System Malfunction', 'Dirty IAC valve, vacuum leaks, faulty IAC actuator', 'Clean throttle body and Idle Air Control valve, check vacuum', 'J1979'),
('P0562', 'ENGINE', 'HIGH', 'System Voltage Low', 'Failed alternator, weak battery, loose alternator belt', 'Test alternator output voltage under load, check battery cells', 'J1979'),
('P0563', 'ENGINE', 'HIGH', 'System Voltage High', 'Failed alternator voltage regulator', 'Test alternator voltage regulator, replace alternator assembly', 'J1979'),

-- Transmission and Computer (P0600-P0799)
('P0603', 'ENGINE', 'HIGH', 'Internal Control Module Keep Alive Memory (KAM) Error', 'Battery disconnected recently, weak battery, internal PCM vault', 'Clear code and retest, check battery connections, replace PCM', 'J1979'),
('P0606', 'ENGINE', 'CRITICAL', 'Control Module Processor (PCM) Fault', 'Internal PCM processor failure, corrupt PCM software flash', 'Reflash PCM software, replace PCM/ECM module', 'J1979'),
('P0700', 'TRANSMISSION', 'HIGH', 'Transmission Control System Malfunction (MIL Request)', 'Faulty transmission sensor, TCM fault, bad internal solenoid', 'Scan Transmission Control Module (TCM) for sub-codes', 'J1979'),
('P0705', 'TRANSMISSION', 'MEDIUM', 'Transmission Range Sensor Circuit Malfunction (PRNDL Input)', 'Corroded park/neutral switch, loose transmission selector linkage', 'Clean range switch plug, adjust shift linkage mechanism', 'J1979'),
('P0715', 'TRANSMISSION', 'HIGH', 'Input/Turbine Speed Sensor A Circuit Malfunction', 'Failed transmission input speed sensor, faulty wiring', 'Test sensor resistance, check transmission fluid level/condition', 'J1979'),
('P0720', 'TRANSMISSION', 'HIGH', 'Output Speed Sensor Circuit Malfunction', 'Failed transmission output speed sensor, speed signal loss', 'Replace transmission output speed sensor', 'J1979'),
('P0730', 'TRANSMISSION', 'HIGH', 'Incorrect Gear Ratio', 'Low transmission fluid, worn clutches, faulty shift solenoid', 'Check transmission fluid level and smell (burnt?), test solenoids', 'J1979'),
('P0740', 'TRANSMISSION', 'HIGH', 'Torque Converter Clutch Circuit Malfunction', 'Failed lock-up solenoid, torque converter internal clutch wear', 'Replace torque converter lockup solenoid, flush trans fluid', 'J1979'),
('P0750', 'TRANSMISSION', 'HIGH', 'Shift Solenoid A Malfunction', 'Failed shift solenoid A, clogged fluid passage, harness damage', 'Inspect TCM wire harness, replace internal shift solenoid A', 'J1979'),

-- Body codes (B0xxx)
('B0001', 'BODY', 'HIGH', 'Driver Frontal Stage 1 Aerbag Deployment Control Malfunction', 'Clock spring open circuit, faulty airbag module, wiring fault', 'Inspect clock spring, test airbag deployment loop resistance', 'J2012'),
('B0010', 'BODY', 'HIGH', 'Passenger Frontal Stage 1 Aerbag Control Malfunction', 'Failed passenger airbag connector, open circuit wiring', 'Verify passenger airbag switch circuit, replace passenger airbag', 'J2012'),
('B0080', 'BODY', 'LOW', 'Driver Seatbelt Tensioner Sensor Malfunction', 'Corroded seatbelt buckle connector, faulty buckle switch', 'Clean connectors under driver seat, check buckle switch state', 'J2012'),
('B0081', 'BODY', 'LOW', 'Passenger Seatbelt Tensioner Sensor Malfunction', 'Passenger occupancy seat pad sensor failure, buckle sensor fault', 'Inspect passenger occupancy sensor pad under seat cushion', 'J2012'),
('B0090', 'BODY', 'MEDIUM', 'Rollover Sensor Malfunction', 'Failed chassis roll sensor module, mounting bolts loose', 'Check rollover sensor module mounting torque, verify TCM codes', 'J2012'),
('B0200', 'BODY', 'LOW', 'AC Cabin Temperature Sensor Circuit Malfunction', 'Dust build-up on cabin temp thermistor, failed sensor unit', 'Clean cabin temp sensor intake, replace cabin temperature sensor', 'J2012'),
('B0201', 'BODY', 'LOW', 'AC Ambient Temperature Sensor Malfunction', 'Corroded ambient temp sensor near radiator, broken harness wire', 'Inspect front bumper temperature sensor, replace sensor', 'J2012'),
('B0300', 'BODY', 'LOW', 'AC Blend Door Actuator Malfunction', 'Stripped plastic gears in actuator, failed blend door motor', 'Replace heater core blend door actuator motor assembly', 'J2012'),

-- Chassis codes (C0xxx)
('C0035', 'CHASSIS', 'HIGH', 'Left Front Wheel Speed Sensor Malfunction', 'Corroded hub sensor connector, damaged tone ring, failed VSS', 'Clean speed sensor connector, replace front left wheel speed sensor', 'J2012'),
('C0040', 'CHASSIS', 'HIGH', 'Right Front Wheel Speed Sensor Malfunction', 'Damaged speed sensor harness, worn hub bearings', 'Inspect right front hub bearing assembly, replace ABS sensor', 'J2012'),
('C0045', 'CHASSIS', 'HIGH', 'Left Rear Wheel Speed Sensor Malfunction', 'ABS sensor failure, dirt accumulation in reluctor ring', 'Clean rear ABS reluctor ring, replace rear left wheel speed sensor', 'J2012'),
('C0050', 'CHASSIS', 'HIGH', 'Right Rear Wheel Speed Sensor Malfunction', 'ABS sensor failure rear right', 'Clean tone ring, replace ABS sensor', 'J2012'),
('C0060', 'CHASSIS', 'CRITICAL', 'ABS Left Front Solenoid Malfunction', 'ABS modulator valve block electrical fault, bad ABS control module', 'Test ABS pump modulator valves, rebuild ABS module', 'J2012'),
('C0070', 'CHASSIS', 'CRITICAL', 'ABS Right Front Solenoid Malfunction', 'ABS modulator block solenoid coil open circuit', 'Replace ABS pump modulator block assembly', 'J2012'),
('C0110', 'CHASSIS', 'CRITICAL', 'ABS Pump Motor Circuit Malfunction', 'Failed ABS pump motor brushes, blown ABS pump high amp fuse', 'Replace ABS pump fuse, replace ABS hydraulic unit', 'J2012'),
('C0121', 'CHASSIS', 'HIGH', 'ABS Valve Relay Circuit Malfunction', 'Failed internal ABS relay, bad grounding wire to chassis', 'Inspect ABS control module ground path, replace ABS module', 'J2012'),
('C0200', 'CHASSIS', 'HIGH', 'Brake Fluid Pressure Sensor Circuit Malfunction', 'Failed brake master cylinder pressure sensor, low brake fluid level', 'Top up brake fluid, inspect and replace brake pressure sensor', 'J2012'),
('C0300', 'CHASSIS', 'HIGH', 'Steering Angle Sensor Malfunction', 'Worn steering column clock spring, steering wheel misalignment', 'Align front wheels, calibrate Steering Angle Sensor (SAS)', 'J2012'),

-- Network codes (U0xxx)
('U0001', 'NETWORK', 'HIGH', 'High Speed CAN Communication Bus Malfunction', 'Shorted CAN-high or CAN-low bus line, failed module on network', 'Disconnect modules sequentially to isolate network short circuit', 'J1979'),
('U0100', 'NETWORK', 'CRITICAL', 'Lost Communication with ECM/PCM', 'Blown ECM fuse, failed main relay, severed CAN wiring to engine', 'Verify engine ECU power supply, test CAN bus resistance (60 ohms)', 'J1979'),
('U0101', 'NETWORK', 'HIGH', 'Lost Communication with TCM', 'TCM harness disconnected, failed transmission control module', 'Verify TCM connector lock, verify TCM fuse, replace TCM', 'J1979'),
('U0121', 'NETWORK', 'HIGH', 'Lost Communication with ABS Control Module', 'ABS control module plug loose, blown ABS fuse, ABS module dead', 'Check ABS power supply fuses, check CAN wires to ABS connector', 'J1979'),
('U0140', 'NETWORK', 'MEDIUM', 'Lost Communication with Body Control Module (BCM)', 'BCM module fault, network communications failure', 'Scan BCM for diagnostics, verify BCM power supply', 'J1979'),
('U0155', 'NETWORK', 'LOW', 'Lost Communication with Instrument Panel Cluster (IPC)', 'Loose cluster plug, failed instrument cluster processor', 'Remove dash trim, verify dashboard connection seating', 'J1979'),
('U0200', 'NETWORK', 'LOW', 'Lost Communication with Driver Door Module', 'Broken wires in door hinge boot, failed window switch node', 'Inspect door wire harness bellows, replace window/door module', 'J1979'),
('U0401', 'NETWORK', 'HIGH', 'Invalid Data Received from ECM/PCM', 'ECU sensor failure causing out-of-range CAN signals', 'Verify engine sensor health (ECT, MAP, TPS) on scanner', 'J1979');


-- 7. Document Types
INSERT INTO document_types (id, name, code, description, applies_to, is_mandatory, default_validity_months) VALUES
(gen_random_uuid(), 'Registration Certificate', 'RC', 'Official vehicle registration certificate issued by RTO', 'VEHICLE', true, 180),
(gen_random_uuid(), 'Vehicle Insurance Policy', 'INSURANCE', 'Third-party or comprehensive motor insurance policy certificate', 'VEHICLE', true, 12),
(gen_random_uuid(), 'Pollution Under Control Certificate', 'PUC', 'Emissions compliance certificate', 'VEHICLE', true, 6),
(gen_random_uuid(), 'National Permit', 'PERMIT', 'All India Permit for interstate cargo transport', 'VEHICLE', false, 12),
(gen_random_uuid(), 'Fitness Certificate', 'FITNESS_CERTIFICATE', 'Vehicle mechanical roadworthiness certificate', 'VEHICLE', true, 12),
(gen_random_uuid(), 'Road Tax Receipt', 'ROAD_TAX', 'State road tax payment receipt', 'VEHICLE', false, 12),
(gen_random_uuid(), 'Commercial Driving License', 'DRIVER_LICENSE', 'Commercial vehicle driving license issued by RTO', 'DRIVER', true, 60),
(gen_random_uuid(), 'Driver Medical Fitness', 'DRIVER_MEDICAL', 'Official medical physical clearance report', 'DRIVER', false, 24),
(gen_random_uuid(), 'Driver Training Certificate', 'DRIVER_TRAINING', 'Defensive driving and safety training completion certificate', 'DRIVER', false, 36),
(gen_random_uuid(), 'Vehicle Photo', 'VEHICLE_PHOTO', 'Visual record of vehicle body condition', 'VEHICLE', false, NULL);


-- 8. Demo Tenant Environment (FleetIQ Demo Corp)
-- 8.1 Tenant
INSERT INTO tenants (id, name, slug, domain, status, subscription_plan_id, max_vehicles, max_users, settings, timezone, country_code) VALUES
('b2c3d4e5-1111-4000-8000-000000000001', 'FleetIQ Demo Corp', 'fleetiq-demo', 'demo.fleetiq.com', 'ACTIVE', 'a1b2c3d4-0003-4000-8000-000000000003', 500, 100, '{"billing_active": true}', 'Asia/Kolkata', 'IN');

-- 8.2 Tenant Settings
INSERT INTO tenant_settings (id, tenant_id, default_speed_unit, default_fuel_unit, default_distance_unit, default_temperature_unit, fuel_theft_threshold_litres, fuel_theft_time_window_sec, fuel_consumption_alert_rate, harsh_accel_threshold, harsh_brake_threshold, harsh_corner_threshold, speeding_tolerance_pct, idle_timeout_minutes, data_retention_gps_days, data_retention_fuel_days, data_retention_obd_days) VALUES
('b2c3d4e5-2222-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'KMH', 'LITRES', 'KM', 'C', 5.00, 120, 1.20, 2.50, -3.00, 2.50, 10, 5, 90, 90, 60);

-- 8.3 Tenant Subscription
INSERT INTO tenant_subscriptions (id, tenant_id, plan_id, status, started_at, expires_at, trial_ends_at, billing_cycle) VALUES
('b2c3d4e5-3333-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'a1b2c3d4-0003-4000-8000-000000000003', 'TRIAL', NOW() - INTERVAL '5 days', NOW() + INTERVAL '25 days', NOW() + INTERVAL '25 days', 'MONTHLY');

-- 8.4 Depots
INSERT INTO depots (id, tenant_id, name, code, address, city, state, country, postal_code, location, contact_phone, contact_email, capacity, is_active) VALUES
('c3d4e5f6-1111-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'Mumbai HQ Depot', 'MUM-DEP-01', 'Main Port Area, Sector 5', 'Mumbai', 'Maharashtra', 'India', '400001', ST_SetSRID(ST_MakePoint(72.8777, 19.0760), 4326)::geography, '+919999999991', 'mumbai@demo.com', 150, true),
('c3d4e5f6-2222-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'Delhi Branch Depot', 'DEL-DEP-02', 'Okhla Industrial Area, Phase 3', 'New Delhi', 'Delhi', 'India', '110020', ST_SetSRID(ST_MakePoint(77.2090, 28.6139), 4326)::geography, '+919999999992', 'delhi@demo.com', 80, true);

-- 8.5 Users (Admin, Fleet Manager, Driver)
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

-- 8.6 Drivers
INSERT INTO drivers (id, tenant_id, user_id, depot_id, employee_id, first_name, last_name, phone, email, license_number, license_type, license_expiry, status, hire_date) VALUES
('f6a7b8c9-1111-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'e5f6a7b8-3333-4000-8000-000000000001', 'c3d4e5f6-1111-4000-8000-000000000001', 'EMP-DRV-001', 'Amit', 'Kumar', '+918888888883', 'driver1@demo.com', 'DL-MH01-2020-01001', 'MCWG_LMV_HMV', '2030-01-01', 'ACTIVE', '2024-01-15'),
('f6a7b8c9-2222-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'e5f6a7b8-3333-4000-8000-000000000002', 'c3d4e5f6-1111-4000-8000-000000000001', 'EMP-DRV-002', 'Rajesh', 'Yadav', '+918888888884', 'driver2@demo.com', 'DL-MH01-2021-02002', 'LMV_HMV', '2031-05-12', 'ACTIVE', '2024-03-20'),
('f6a7b8c9-3333-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'e5f6a7b8-3333-4000-8000-000000000003', 'c3d4e5f6-2222-4000-8000-000000000001', 'EMP-DRV-003', 'Vikram', 'Singh', '+918888888885', 'driver3@demo.com', 'DL-DL03-2019-03003', 'HMV_TRANS', '2029-08-14', 'ACTIVE', '2023-11-10');

-- 8.7 Vehicles (5 vehicles)
INSERT INTO vehicles (id, tenant_id, vehicle_type_id, depot_id, registration_number, vin, chassis_number, make, model, year_of_manufacture, status, odometer_reading_km, engine_hours) VALUES
('b8c9d0e1-1111-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'a7b8c9d0-0001-4000-8000-000000000001', 'c3d4e5f6-1111-4000-8000-000000000001', 'MH-01-DE-1234', 'MUMBSDN9876543210', 'CHASSIS-SDN-1001', 'Tata Motors', 'Tigor', 2022, 'ACTIVE', 24500.00, 850.50),
('b8c9d0e1-2222-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'a7b8c9d0-0006-4000-8000-000000000006', 'c3d4e5f6-1111-4000-8000-000000000001', 'MH-12-TR-9999', 'MUMBTK88887777666', 'CHASSIS-TRK-2002', 'Ashok Leyland', 'U-Truck 3718', 2021, 'ACTIVE', 115200.00, 4200.10),
('b8c9d0e1-3333-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'a7b8c9d0-0008-4000-8000-000000000008', 'c3d4e5f6-2222-4000-8000-000000000001', 'DL-3C-BU-5555', 'DELHICNG876543210', 'CHASSIS-BUS-3003', 'Eicher Motors', 'Skyline Pro', 2023, 'ACTIVE', 18600.00, 950.00),
('b8c9d0e1-4222-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'a7b8c9d0-0013-4000-8000-000000000013', 'c3d4e5f6-1111-4000-8000-000000000001', 'MH-03-EV-0001', 'MUMBEV99999111111', 'CHASSIS-EVC-4004', 'BYD', 'e6', 2023, 'ACTIVE', 12300.00, 310.20),
('b8c9d0e1-5222-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'a7b8c9d0-0016-4000-8000-000000000016', 'c3d4e5f6-2222-4000-8000-000000000001', 'DL-2S-EV-8888', 'DELHIEVB888877777', 'CHASSIS-EVB-5005', 'Ather Energy', '450X', 2022, 'ACTIVE', 8900.00, NULL);

-- Add EV specific details for EV vehicles
INSERT INTO ev_vehicle_details (vehicle_id, tenant_id, battery_capacity_kwh, battery_chemistry, usable_capacity_kwh, max_charging_power_kw, charger_port_type) VALUES
('b8c9d0e1-4222-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 71.70, 'LFP', 70.00, 60.00, 'CCS2'),
('b8c9d0e1-5222-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 3.70, 'NMC', 3.50, 3.30, 'Ather Connector');

-- 8.8 Devices (5 devices)
INSERT INTO devices (id, tenant_id, serial_number, imei, device_type, manufacturer, model, status) VALUES
('c9d0e1f2-1111-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'TEL-GPS-001001', '864201020304051', 'GPS_TRACKER', 'Teltonika', 'FMB120', 'ACTIVE'),
('c9d0e1f2-2222-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'TEL-OBD-002002', '864201020304052', 'OBD_DONGLE', 'Teltonika', 'FMC001', 'ACTIVE'),
('c9d0e1f2-3333-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'QUE-GPS-003003', '864201020304053', 'GPS_TRACKER', 'Queclink', 'GV300', 'ACTIVE'),
('c9d0e1f2-4444-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'CON-OBD-004004', '864201020304054', 'OBD_DONGLE', 'Concox', 'OBD22', 'ACTIVE'),
('c9d0e1f2-5555-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'TEL-EVT-005005', '864201020304055', 'GPS_TRACKER', 'Teltonika', 'FTM740', 'ACTIVE');

-- Add Device Twins
INSERT INTO device_twins (id, tenant_id, device_id, firmware_version, sync_status, desired_config, reported_config) VALUES
(gen_random_uuid(), 'b2c3d4e5-1111-4000-8000-000000000001', 'c9d0e1f2-1111-4000-8000-000000000001', 'v1.2.3', 'IN_SYNC', '{"ping_interval_sec": 30}', '{"ping_interval_sec": 30}'),
(gen_random_uuid(), 'b2c3d4e5-1111-4000-8000-000000000001', 'c9d0e1f2-2222-4000-8000-000000000001', 'v1.0.4', 'IN_SYNC', '{"ping_interval_sec": 30}', '{"ping_interval_sec": 30}'),
(gen_random_uuid(), 'b2c3d4e5-1111-4000-8000-000000000001', 'c9d0e1f2-3333-4000-8000-000000000001', 'v2.1.0', 'IN_SYNC', '{"ping_interval_sec": 30}', '{"ping_interval_sec": 30}'),
(gen_random_uuid(), 'b2c3d4e5-1111-4000-8000-000000000001', 'c9d0e1f2-4444-4000-8000-000000000001', 'v3.1.2', 'IN_SYNC', '{"ping_interval_sec": 30}', '{"ping_interval_sec": 30}'),
(gen_random_uuid(), 'b2c3d4e5-1111-4000-8000-000000000001', 'c9d0e1f2-5555-4000-8000-000000000001', 'v1.4.0', 'IN_SYNC', '{"ping_interval_sec": 30}', '{"ping_interval_sec": 30}');

-- Device vehicle assignments
INSERT INTO device_vehicle_assignments (device_id, vehicle_id, assigned_by) VALUES
('c9d0e1f2-1111-4000-8000-000000000001', 'b8c9d0e1-1111-4000-8000-000000000001', 'e5f6a7b8-2222-4000-8000-000000000001'),
('c9d0e1f2-2222-4000-8000-000000000001', 'b8c9d0e1-2222-4000-8000-000000000001', 'e5f6a7b8-2222-4000-8000-000000000001'),
('c9d0e1f2-3333-4000-8000-000000000001', 'b8c9d0e1-3333-4000-8000-000000000001', 'e5f6a7b8-2222-4000-8000-000000000001'),
('c9d0e1f2-4444-4000-8000-000000000001', 'b8c9d0e1-4222-4000-8000-000000000001', 'e5f6a7b8-2222-4000-8000-000000000001'),
('c9d0e1f2-5555-4000-8000-000000000001', 'b8c9d0e1-5222-4000-8000-000000000001', 'e5f6a7b8-2222-4000-8000-000000000001');

-- 8.9 Driver assignments
INSERT INTO driver_assignments (driver_id, vehicle_id, shift_start, assigned_by) VALUES
('f6a7b8c9-1111-4000-8000-000000000001', 'b8c9d0e1-1111-4000-8000-000000000001', NOW() - INTERVAL '4 hours', 'e5f6a7b8-2222-4000-8000-000000000001'),
('f6a7b8c9-2222-4000-8000-000000000001', 'b8c9d0e1-2222-4000-8000-000000000001', NOW() - INTERVAL '2 hours', 'e5f6a7b8-2222-4000-8000-000000000001'),
('f6a7b8c9-3333-4000-8000-000000000001', 'b8c9d0e1-3333-4000-8000-000000000001', NOW() - INTERVAL '1 hour', 'e5f6a7b8-2222-4000-8000-000000000001');

-- 8.10 Geofences
INSERT INTO geofences (id, tenant_id, name, description, boundary, type, center_latitude, center_longitude, radius_meters, category) VALUES
('d0e1f2a3-1111-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'Mumbai HQ Circular Fence', '500m radius around HQ depot', ST_SetSRID(ST_MakePoint(72.8777, 19.0760), 4326)::geometry, 'CIRCLE', 19.0760, 72.8777, 500.00, 'DEPOT'),
('d0e1f2a3-2222-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'Delhi Yard Polygon', 'Chassis yard border box', ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(77.200 28.610, 77.210 28.610, 77.210 28.620, 77.200 28.620, 77.200 28.610)')), 4326), 'POLYGON', NULL, NULL, NULL, 'DEPOT');

-- 8.11 Vehicle Groups
INSERT INTO vehicle_groups (id, tenant_id, name, description, color) VALUES
('e1f2a3b4-1111-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'All Vehicles', 'Main folder containing all fleet units', '#3357FF'),
('e1f2a3b4-2222-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'Heavy Trucks Group', 'Heavy hauling commercial multi-axle units', '#FF5733'),
('e1f2a3b4-3333-4000-8000-000000000001', 'b2c3d4e5-1111-4000-8000-000000000001', 'EV Fleet Group', 'Battery electric units', '#33FF57');

-- Group memberships
INSERT INTO vehicle_group_members (vehicle_group_id, vehicle_id) VALUES
('e1f2a3b4-1111-4000-8000-000000000001', 'b8c9d0e1-1111-4000-8000-000000000001'),
('e1f2a3b4-1111-4000-8000-000000000001', 'b8c9d0e1-2222-4000-8000-000000000001'),
('e1f2a3b4-1111-4000-8000-000000000001', 'b8c9d0e1-3333-4000-8000-000000000001'),
('e1f2a3b4-1111-4000-8000-000000000001', 'b8c9d0e1-4222-4000-8000-000000000001'),
('e1f2a3b4-1111-4000-8000-000000000001', 'b8c9d0e1-5222-4000-8000-000000000001'),
('e1f2a3b4-2222-4000-8000-000000000001', 'b8c9d0e1-2222-4000-8000-000000000001'),
('e1f2a3b4-3333-4000-8000-000000000001', 'b8c9d0e1-4222-4000-8000-000000000001'),
('e1f2a3b4-3333-4000-8000-000000000001', 'b8c9d0e1-5222-4000-8000-000000000001');
