-- V004__add_simulator_user.sql

INSERT INTO users (id, email, password_hash, first_name, last_name, status, email_verified)
VALUES (
  '55555555-0000-0000-0000-000000000000',
  'simulator@fleetiq.internal',
  crypt('simulator123', gen_salt('bf', 10)),
  'System',
  'Simulator',
  'ACTIVE',
  TRUE
) ON CONFLICT DO NOTHING;

INSERT INTO roles (id, name, display_name, description, is_system_role, hierarchy_level)
VALUES (
  '66666666-0000-0000-0000-000000000000',
  'SIMULATOR',
  'Telemetry Simulator',
  'System account for injecting simulator telemetry data',
  TRUE,
  99
) ON CONFLICT DO NOTHING;

INSERT INTO permissions (id, code, module, action, description)
VALUES 
  ('77777777-0000-0000-0000-000000000001', 'vehicles:update', 'vehicles', 'update', 'Used by simulator to update location')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT '66666666-0000-0000-0000-000000000000', id FROM permissions 
WHERE code IN ('vehicles:update')
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
VALUES (
  '55555555-0000-0000-0000-000000000000',
  '66666666-0000-0000-0000-000000000000'
) ON CONFLICT DO NOTHING;
