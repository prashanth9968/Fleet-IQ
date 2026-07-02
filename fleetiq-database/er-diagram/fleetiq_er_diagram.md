# FleetIQ — Complete Entity Relationship Diagram

> **Version:** 1.0.0  
> **Tables:** ~70 + 5 hypertables  
> **Target:** PostgreSQL 16 + TimescaleDB 2.x + PostGIS 3.x  
> **Render:** Any Mermaid-compatible viewer (GitHub, VS Code, Mermaid Live Editor)

---

## Full ER Diagram

```mermaid
erDiagram

    %% ==========================================
    %% MULTI-TENANCY & SUBSCRIPTION
    %% ==========================================

    subscription_plans {
        UUID id PK
        VARCHAR name
        VARCHAR code UK
        VARCHAR tier "BASIC | PROFESSIONAL | ENTERPRISE"
        NUMERIC price_per_vehicle_monthly
        INT max_vehicles
        INT max_users
        INT max_geofences
        JSONB features
        BOOLEAN is_active
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    tenants {
        UUID id PK
        VARCHAR name
        VARCHAR slug UK
        VARCHAR domain
        TEXT logo_url
        VARCHAR status "ACTIVE | SUSPENDED | TRIAL | CANCELLED"
        UUID subscription_plan_id FK
        INT max_vehicles
        INT max_users
        JSONB settings
        VARCHAR timezone
        VARCHAR country_code
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
        TIMESTAMPTZ deleted_at
    }

    tenant_subscriptions {
        UUID id PK
        UUID tenant_id FK
        UUID plan_id FK
        VARCHAR status "ACTIVE | TRIAL | EXPIRED | CANCELLED"
        TIMESTAMPTZ started_at
        TIMESTAMPTZ expires_at
        TIMESTAMPTZ trial_ends_at
        VARCHAR billing_cycle
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    tenant_settings {
        UUID id PK
        UUID tenant_id FK "UK"
        VARCHAR default_speed_unit
        VARCHAR default_fuel_unit
        NUMERIC fuel_theft_threshold_litres
        INT fuel_theft_time_window_sec
        NUMERIC fuel_consumption_alert_rate
        NUMERIC harsh_accel_threshold
        NUMERIC harsh_brake_threshold
        INT data_retention_gps_days
        INT data_retention_fuel_days
        INT data_retention_obd_days
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    subscription_plans ||--o{ tenants : "has"
    tenants ||--|| tenant_settings : "has"
    tenants ||--o{ tenant_subscriptions : "subscribes"
    subscription_plans ||--o{ tenant_subscriptions : "used_by"

    %% ==========================================
    %% AUTH & RBAC
    %% ==========================================

    roles {
        UUID id PK
        VARCHAR name UK
        VARCHAR display_name
        TEXT description
        BOOLEAN is_system_role
        INT hierarchy_level
        TIMESTAMPTZ created_at
    }

    permissions {
        UUID id PK
        VARCHAR code UK
        VARCHAR module
        VARCHAR action
        TEXT description
        TIMESTAMPTZ created_at
    }

    role_permissions {
        UUID role_id FK "PK"
        UUID permission_id FK "PK"
    }

    users {
        UUID id PK
        UUID tenant_id FK
        VARCHAR email
        VARCHAR password_hash
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR phone
        VARCHAR status "ACTIVE | INACTIVE | LOCKED | PENDING"
        BOOLEAN email_verified
        TIMESTAMPTZ last_login_at
        INT failed_login_attempts
        VARCHAR language
        VARCHAR timezone
        JSONB notification_preferences
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
        TIMESTAMPTZ deleted_at
    }

    user_roles {
        UUID user_id FK "PK"
        UUID role_id FK "PK"
        UUID assigned_by FK
        TIMESTAMPTZ assigned_at
    }

    api_keys {
        UUID id PK
        UUID tenant_id FK
        VARCHAR name
        VARCHAR key_hash UK
        VARCHAR key_prefix
        JSONB scopes
        INT rate_limit_per_minute
        TIMESTAMPTZ expires_at
        BOOLEAN is_active
        UUID created_by FK
        TIMESTAMPTZ created_at
    }

    user_sessions {
        UUID id PK
        UUID user_id FK
        VARCHAR refresh_token_hash
        JSONB device_info
        INET ip_address
        TIMESTAMPTZ expires_at
        TIMESTAMPTZ created_at
        TIMESTAMPTZ revoked_at
    }

    roles ||--o{ role_permissions : "has"
    permissions ||--o{ role_permissions : "granted_to"
    tenants ||--o{ users : "has"
    users ||--o{ user_roles : "has"
    roles ||--o{ user_roles : "assigned_to"
    tenants ||--o{ api_keys : "has"
    users ||--o{ user_sessions : "has"
    users ||--o{ api_keys : "created"

    %% ==========================================
    %% FLEET STRUCTURE
    %% ==========================================

    depots {
        UUID id PK
        UUID tenant_id FK
        VARCHAR name
        VARCHAR code
        TEXT address
        VARCHAR city
        VARCHAR state
        VARCHAR country
        GEOGRAPHY location "Point 4326"
        VARCHAR contact_phone
        INT capacity
        BOOLEAN is_active
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
        TIMESTAMPTZ deleted_at
    }

    vehicle_groups {
        UUID id PK
        UUID tenant_id FK
        VARCHAR name
        TEXT description
        VARCHAR color
        UUID parent_group_id FK "self-ref"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
        TIMESTAMPTZ deleted_at
    }

    vehicle_group_members {
        UUID vehicle_group_id FK "PK"
        UUID vehicle_id FK "PK"
        TIMESTAMPTZ added_at
    }

    user_depot_assignments {
        UUID user_id FK "PK"
        UUID depot_id FK "PK"
        TIMESTAMPTZ assigned_at
        UUID assigned_by FK
    }

    tenants ||--o{ depots : "has"
    tenants ||--o{ vehicle_groups : "has"
    vehicle_groups ||--o{ vehicle_groups : "parent"
    users ||--o{ user_depot_assignments : "assigned"
    depots ||--o{ user_depot_assignments : "has"

    %% ==========================================
    %% VEHICLES & DEVICES
    %% ==========================================

    vehicle_types {
        UUID id PK
        VARCHAR name UK
        VARCHAR category "SEDAN | SUV | TRUCK_HEAVY | BUS | EV_CAR | ..."
        VARCHAR fuel_type "PETROL | DIESEL | CNG | ELECTRIC | HYBRID"
        NUMERIC default_fuel_capacity_litres
        NUMERIC default_fuel_consumption_rate
        VARCHAR icon
        TIMESTAMPTZ created_at
    }

    vehicles {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_type_id FK
        UUID depot_id FK
        VARCHAR registration_number "UK per tenant"
        VARCHAR vin
        VARCHAR make
        VARCHAR model
        INT year_of_manufacture
        NUMERIC fuel_tank_capacity_litres
        NUMERIC odometer_reading_km
        NUMERIC engine_hours
        VARCHAR status "ACTIVE | INACTIVE | IN_MAINTENANCE | RETIRED"
        DATE acquisition_date
        NUMERIC acquisition_cost
        DATE insurance_expiry
        JSONB metadata
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
        TIMESTAMPTZ deleted_at
    }

    devices {
        UUID id PK
        UUID tenant_id FK
        VARCHAR serial_number UK
        VARCHAR imei UK
        VARCHAR device_type "GPS_TRACKER | OBD_DONGLE | FUEL_SENSOR | DASHCAM | ..."
        VARCHAR manufacturer
        VARCHAR model
        VARCHAR firmware_version
        VARCHAR sim_iccid
        VARCHAR protocol "MQTT | HTTP | TCP"
        VARCHAR status "ACTIVE | INACTIVE | FAULTY | DECOMMISSIONED"
        TIMESTAMPTZ last_communication_at
        JSONB configuration
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
        TIMESTAMPTZ deleted_at
    }

    device_vehicle_assignments {
        UUID id PK
        UUID device_id FK
        UUID vehicle_id FK
        TIMESTAMPTZ assigned_at
        TIMESTAMPTZ unassigned_at
        UUID assigned_by FK
        BOOLEAN is_primary
        TIMESTAMPTZ created_at
    }

    vehicle_types ||--o{ vehicles : "categorizes"
    tenants ||--o{ vehicles : "owns"
    depots ||--o{ vehicles : "houses"
    tenants ||--o{ devices : "owns"
    devices ||--o{ device_vehicle_assignments : "assigned"
    vehicles ||--o{ device_vehicle_assignments : "has"
    vehicles ||--o{ vehicle_group_members : "member_of"
    vehicle_groups ||--o{ vehicle_group_members : "contains"

    %% ==========================================
    %% DEVICE TWINS & OTA
    %% ==========================================

    device_twins {
        UUID id PK
        UUID tenant_id FK
        UUID device_id FK "UK"
        VARCHAR firmware_version
        VARCHAR hardware_version
        JSONB desired_config
        JSONB reported_config
        VARCHAR desired_firmware_version
        VARCHAR sync_status "IN_SYNC | PENDING | SYNCING | FAILED"
        TIMESTAMPTZ last_sync_at
        TIMESTAMPTZ last_seen_at
        TEXT error_message
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    firmware_packages {
        UUID id PK
        VARCHAR version
        VARCHAR device_manufacturer
        VARCHAR device_model
        TEXT file_url
        BIGINT file_size_bytes
        VARCHAR checksum_sha256
        TEXT release_notes
        BOOLEAN is_critical
        VARCHAR status "DRAFT | TESTING | RELEASED | DEPRECATED"
        TIMESTAMPTZ released_at
        UUID created_by FK
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    firmware_deployments {
        UUID id PK
        UUID tenant_id FK
        UUID firmware_package_id FK
        VARCHAR name
        VARCHAR strategy "ROLLING | IMMEDIATE | SCHEDULED | CANARY"
        TIMESTAMPTZ scheduled_at
        VARCHAR status "PENDING | IN_PROGRESS | COMPLETED | FAILED | CANCELLED"
        INT total_targets
        INT success_count
        INT failure_count
        UUID created_by FK
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    firmware_deployment_targets {
        UUID id PK
        UUID deployment_id FK
        UUID device_id FK
        VARCHAR status "PENDING | DOWNLOADING | INSTALLING | SUCCESS | FAILED"
        VARCHAR previous_version
        TEXT error_message
        INT retry_count
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    devices ||--|| device_twins : "has_twin"
    tenants ||--o{ device_twins : "owns"
    firmware_packages ||--o{ firmware_deployments : "deployed_via"
    firmware_deployments ||--o{ firmware_deployment_targets : "targets"
    devices ||--o{ firmware_deployment_targets : "receives"

    %% ==========================================
    %% DRIVERS
    %% ==========================================

    drivers {
        UUID id PK
        UUID tenant_id FK
        UUID user_id FK
        UUID depot_id FK
        VARCHAR employee_id "UK per tenant"
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR phone
        VARCHAR license_number
        VARCHAR license_type
        DATE license_expiry
        VARCHAR status "ACTIVE | INACTIVE | SUSPENDED | ON_LEAVE"
        DATE hire_date
        JSONB metadata
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
        TIMESTAMPTZ deleted_at
    }

    driver_assignments {
        UUID id PK
        UUID tenant_id FK
        UUID driver_id FK
        UUID vehicle_id FK
        TIMESTAMPTZ shift_start
        TIMESTAMPTZ shift_end
        UUID assigned_by FK
        VARCHAR status "ACTIVE | COMPLETED | CANCELLED"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    driver_safety_scores {
        UUID id PK
        UUID tenant_id FK
        UUID driver_id FK
        VARCHAR period_type "DAILY | WEEKLY | MONTHLY"
        DATE period_start
        DATE period_end
        NUMERIC overall_score "0-100"
        NUMERIC harsh_accel_score
        NUMERIC harsh_brake_score
        NUMERIC speeding_score
        NUMERIC fatigue_score
        INT total_trips
        NUMERIC total_distance_km
        NUMERIC peer_percentile
        TIMESTAMPTZ created_at
    }

    driving_events {
        UUID vehicle_id "PK (hypertable)"
        UUID tenant_id
        UUID driver_id
        VARCHAR event_type "HARSH_ACCELERATION | HARSH_BRAKING | SPEEDING | ..."
        TIMESTAMPTZ event_at "PK (hypertable)"
        NUMERIC latitude
        NUMERIC longitude
        NUMERIC speed_kmh
        NUMERIC magnitude
        INT duration_seconds
        UUID trip_id
        JSONB metadata
    }

    tenants ||--o{ drivers : "employs"
    depots ||--o{ drivers : "based_at"
    users ||--o| drivers : "login_as"
    drivers ||--o{ driver_assignments : "assigned"
    vehicles ||--o{ driver_assignments : "driven_by"
    drivers ||--o{ driver_safety_scores : "scored"
    drivers ||--o{ driving_events : "generates"
    vehicles ||--o{ driving_events : "records"

    %% ==========================================
    %% GPS & TRACKING (TimescaleDB)
    %% ==========================================

    gps_readings {
        UUID vehicle_id "PK (hypertable)"
        UUID tenant_id
        UUID device_id
        TIMESTAMPTZ recorded_at "PK (hypertable)"
        TIMESTAMPTZ received_at
        NUMERIC latitude
        NUMERIC longitude
        GEOGRAPHY location "Point 4326"
        NUMERIC altitude
        NUMERIC speed_kmh
        NUMERIC heading
        NUMERIC hdop
        SMALLINT satellites
        BOOLEAN ignition
        NUMERIC odometer_km
        BOOLEAN is_buffered
    }

    trips {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_id FK
        UUID driver_id FK
        TIMESTAMPTZ started_at
        TIMESTAMPTZ ended_at
        NUMERIC distance_km
        NUMERIC max_speed_kmh
        NUMERIC avg_speed_kmh
        NUMERIC fuel_consumed_litres
        NUMERIC fuel_efficiency_kmpl
        INT idle_duration_seconds
        INT driving_duration_seconds
        INT harsh_accel_count
        INT harsh_brake_count
        INT speeding_count
        NUMERIC energy_consumed_kwh
        VARCHAR status "IN_PROGRESS | COMPLETED | CANCELLED"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    trip_segments {
        UUID id PK
        UUID trip_id FK
        INT segment_order
        NUMERIC distance_km
        INT duration_seconds
        NUMERIC avg_speed_kmh
        VARCHAR road_name
        NUMERIC speed_limit_kmh
        VARCHAR segment_type "DRIVING | IDLE | STOPPED"
        TEXT encoded_polyline
        TIMESTAMPTZ created_at
    }

    vehicles ||--o{ gps_readings : "emits"
    vehicles ||--o{ trips : "makes"
    drivers ||--o{ trips : "drives"
    trips ||--o{ trip_segments : "composed_of"

    %% ==========================================
    %% FUEL MONITORING
    %% ==========================================

    fuel_readings {
        UUID vehicle_id "PK (hypertable)"
        UUID tenant_id
        UUID device_id
        TIMESTAMPTZ recorded_at "PK (hypertable)"
        NUMERIC fuel_level_litres
        NUMERIC fuel_level_pct
        NUMERIC consumption_rate_lpm
        INT engine_rpm
        BOOLEAN ignition
        NUMERIC speed_kmh
    }

    fuel_events {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_id FK
        UUID driver_id FK
        VARCHAR event_type "FILL | THEFT_SUSPECTED | DRAIN | CONSUMPTION_ALERT"
        TIMESTAMPTZ event_at
        NUMERIC latitude
        NUMERIC longitude
        NUMERIC fuel_level_before_litres
        NUMERIC fuel_level_after_litres
        NUMERIC volume_litres
        NUMERIC confidence_score "0-100"
        VARCHAR status "PENDING | CONFIRMED | DISMISSED | AUTO_RESOLVED"
        UUID reviewed_by FK
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    fuel_cards {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_id FK
        VARCHAR card_number "UK per tenant"
        VARCHAR provider "HPCL | BPCL | IOCL | SHELL"
        VARCHAR status "ACTIVE | INACTIVE | BLOCKED"
        NUMERIC daily_limit
        NUMERIC monthly_limit
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    fuel_thresholds {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_type_id FK
        UUID vehicle_id FK
        NUMERIC consumption_rate_threshold_lpm
        NUMERIC theft_drop_threshold_litres
        INT theft_time_window_seconds
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    vehicles ||--o{ fuel_readings : "reports"
    vehicles ||--o{ fuel_events : "triggers"
    vehicles ||--o{ fuel_cards : "has"
    vehicle_types ||--o{ fuel_thresholds : "default_for"
    vehicles ||--o{ fuel_thresholds : "override_for"

    %% ==========================================
    %% VEHICLE HEALTH (TimescaleDB + PostgreSQL)
    %% ==========================================

    obd_readings {
        UUID vehicle_id "PK (hypertable)"
        UUID tenant_id
        UUID device_id
        TIMESTAMPTZ recorded_at "PK (hypertable)"
        INT engine_rpm
        NUMERIC coolant_temp_c
        NUMERIC oil_pressure_kpa
        NUMERIC battery_voltage
        NUMERIC throttle_position_pct
        NUMERIC engine_load_pct
        NUMERIC odometer_km
        NUMERIC engine_run_hours
        BOOLEAN check_engine_light
    }

    dtc_library {
        UUID id PK
        VARCHAR code UK
        VARCHAR system "ENGINE | TRANSMISSION | ABS | SRS | BODY | ..."
        VARCHAR severity "CRITICAL | HIGH | MEDIUM | LOW | INFO"
        TEXT description
        TEXT possible_causes
        TEXT recommended_action
        VARCHAR sae_standard
        TIMESTAMPTZ created_at
    }

    vehicle_dtc_events {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_id FK
        UUID dtc_id FK
        TIMESTAMPTZ detected_at
        TIMESTAMPTZ cleared_at
        BOOLEAN is_active
        NUMERIC odometer_at_detection
        JSONB freeze_frame_data
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    vehicle_health_scores {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_id FK "UK"
        NUMERIC overall_score "0-100"
        NUMERIC engine_score
        NUMERIC transmission_score
        NUMERIC brake_score
        NUMERIC battery_score
        NUMERIC tyre_score
        NUMERIC electrical_score
        INT active_dtc_count
        TIMESTAMPTZ last_calculated_at
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    vehicle_health_history {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_id FK
        TIMESTAMPTZ recorded_at
        NUMERIC overall_score
        NUMERIC engine_score
        NUMERIC transmission_score
        NUMERIC brake_score
        NUMERIC battery_score
        INT active_dtc_count
        NUMERIC odometer_km
        NUMERIC engine_hours
        JSONB metadata
        TIMESTAMPTZ created_at
    }

    vehicles ||--o{ obd_readings : "emits"
    vehicles ||--o{ vehicle_dtc_events : "triggers"
    dtc_library ||--o{ vehicle_dtc_events : "references"
    vehicles ||--|| vehicle_health_scores : "current_health"
    vehicles ||--o{ vehicle_health_history : "health_over_time"

    %% ==========================================
    %% MAINTENANCE
    %% ==========================================

    maintenance_schedules {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_id FK
        VARCHAR service_type "OIL_CHANGE | TYRE_ROTATION | BRAKE_INSPECTION | ..."
        NUMERIC interval_km
        INT interval_days
        DATE last_service_date
        NUMERIC last_service_odometer
        DATE next_due_date
        NUMERIC next_due_odometer
        BOOLEAN is_overdue
        VARCHAR status "ACTIVE | PAUSED | COMPLETED"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    maintenance_predictions {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_id FK
        VARCHAR component
        NUMERIC failure_probability "0.0000-1.0000"
        DATE predicted_failure_date
        NUMERIC confidence_level
        VARCHAR model_version
        TEXT recommendation
        VARCHAR status "ACTIVE | ACTIONED | EXPIRED | DISMISSED"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    work_orders {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_id FK
        VARCHAR order_number "UK per tenant"
        VARCHAR title
        VARCHAR priority "CRITICAL | HIGH | MEDIUM | LOW"
        VARCHAR status "OPEN | ASSIGNED | IN_PROGRESS | COMPLETED | CANCELLED"
        UUID assigned_to FK
        DATE scheduled_date
        NUMERIC total_parts_cost
        NUMERIC total_labour_cost
        NUMERIC total_cost
        VARCHAR source "MANUAL | SCHEDULED | PREDICTIVE"
        UUID maintenance_schedule_id FK
        UUID maintenance_prediction_id FK
        UUID created_by FK
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    work_order_items {
        UUID id PK
        UUID work_order_id FK
        VARCHAR item_type "PART | LABOUR | OTHER"
        VARCHAR description
        NUMERIC quantity
        NUMERIC unit_cost
        NUMERIC total_cost
        TIMESTAMPTZ created_at
    }

    vehicles ||--o{ maintenance_schedules : "has"
    vehicles ||--o{ maintenance_predictions : "predicted"
    vehicles ||--o{ work_orders : "serviced"
    maintenance_schedules ||--o{ work_orders : "triggers"
    maintenance_predictions ||--o{ work_orders : "triggers"
    work_orders ||--o{ work_order_items : "contains"
    users ||--o{ work_orders : "assigned_to"

    %% ==========================================
    %% GEOFENCING
    %% ==========================================

    geofences {
        UUID id PK
        UUID tenant_id FK
        VARCHAR name
        VARCHAR fence_type "CIRCULAR | POLYGON | CORRIDOR"
        GEOMETRY boundary "Geometry 4326"
        NUMERIC center_lat
        NUMERIC center_lng
        NUMERIC radius_meters
        VARCHAR category "DEPOT | CUSTOMER | RESTRICTED | DELIVERY_ZONE | ..."
        NUMERIC speed_limit_kmh
        INT max_dwell_minutes
        BOOLEAN is_active
        UUID created_by FK
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
        TIMESTAMPTZ deleted_at
    }

    geofence_schedules {
        UUID id PK
        UUID geofence_id FK
        SMALLINT day_of_week "0-6"
        TIME start_time
        TIME end_time
        BOOLEAN is_active
        TIMESTAMPTZ created_at
    }

    geofence_events {
        UUID id PK
        UUID tenant_id
        UUID geofence_id
        UUID vehicle_id
        UUID driver_id
        VARCHAR event_type "ENTRY | EXIT"
        TIMESTAMPTZ event_at "PARTITION KEY"
        NUMERIC latitude
        NUMERIC longitude
        NUMERIC speed_kmh
        INT dwell_duration_seconds
        TIMESTAMPTZ created_at
    }

    tenants ||--o{ geofences : "defines"
    geofences ||--o{ geofence_schedules : "active_during"
    geofences ||--o{ geofence_events : "triggers"
    vehicles ||--o{ geofence_events : "crosses"

    %% ==========================================
    %% ALERTS & NOTIFICATIONS
    %% ==========================================

    alerts {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_id FK
        UUID driver_id FK
        UUID device_id FK
        VARCHAR alert_type "FUEL_THEFT | SPEEDING | DTC_CRITICAL | ..."
        VARCHAR severity "CRITICAL | HIGH | MEDIUM | LOW | INFO"
        VARCHAR status "OPEN | ACKNOWLEDGED | RESOLVED | DISMISSED"
        VARCHAR title
        NUMERIC latitude
        NUMERIC longitude
        JSONB payload
        UUID geofence_id FK
        UUID trip_id FK
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    notification_rules {
        UUID id PK
        UUID tenant_id FK
        VARCHAR name
        VARCHAR alert_type
        VARCHAR min_severity
        UUID vehicle_group_id FK
        UUID depot_id FK
        BOOLEAN is_active
        INT cooldown_minutes
        INT escalation_minutes
        UUID escalation_user_id FK
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    notification_rule_recipients {
        UUID id PK
        UUID rule_id FK
        UUID user_id FK
        JSONB channels "PUSH | SMS | EMAIL | WHATSAPP | WEBHOOK"
        TIMESTAMPTZ created_at
    }

    notification_channels {
        UUID id PK
        UUID tenant_id FK
        VARCHAR channel_type "PUSH | SMS | EMAIL | WHATSAPP | WEBHOOK"
        VARCHAR provider
        JSONB configuration
        BOOLEAN is_active
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    notification_log {
        UUID id "PK (partitioned)"
        UUID tenant_id
        UUID alert_id
        UUID recipient_user_id
        VARCHAR channel
        VARCHAR status "PENDING | SENT | DELIVERED | FAILED | BOUNCED"
        VARCHAR provider_message_id
        TIMESTAMPTZ sent_at "PARTITION KEY"
        TIMESTAMPTZ delivered_at
    }

    tenants ||--o{ alerts : "has"
    vehicles ||--o{ alerts : "triggers"
    tenants ||--o{ notification_rules : "configures"
    notification_rules ||--o{ notification_rule_recipients : "notifies"
    tenants ||--o{ notification_channels : "has"
    alerts ||--o{ notification_log : "dispatched_via"

    %% ==========================================
    %% EV SUPPORT
    %% ==========================================

    ev_vehicle_details {
        UUID id PK
        UUID vehicle_id FK "UK"
        UUID tenant_id FK
        NUMERIC battery_capacity_kwh
        NUMERIC max_range_km
        VARCHAR battery_chemistry "NMC | LFP | NCA"
        VARCHAR charge_port_type "CCS2 | CHADEMO | TYPE2"
        NUMERIC max_dc_charge_kw
        VARCHAR ocpp_charge_point_id
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    charging_stations {
        UUID id PK
        UUID tenant_id FK
        VARCHAR name
        VARCHAR operator
        NUMERIC latitude
        NUMERIC longitude
        GEOGRAPHY location "Point 4326"
        JSONB connector_types
        NUMERIC pricing_per_kwh
        VARCHAR status "ACTIVE | OFFLINE | MAINTENANCE"
        BOOLEAN is_public
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    charging_sessions {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_id FK
        UUID charging_station_id FK
        UUID driver_id FK
        TIMESTAMPTZ started_at
        TIMESTAMPTZ ended_at
        NUMERIC soc_start_pct
        NUMERIC soc_end_pct
        NUMERIC energy_added_kwh
        NUMERIC cost
        VARCHAR charge_type "AC | DC"
        VARCHAR status "IN_PROGRESS | COMPLETED | INTERRUPTED | FAILED"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    battery_health_readings {
        UUID vehicle_id "PK (hypertable)"
        UUID tenant_id
        TIMESTAMPTZ recorded_at "PK (hypertable)"
        NUMERIC soc_pct
        NUMERIC soh_pct
        NUMERIC battery_temp_avg_c
        INT charge_cycle_count
        NUMERIC remaining_range_km
        VARCHAR charger_status "DISCONNECTED | CONNECTED | CHARGING | FULL | FAULT"
        NUMERIC power_draw_kw
        NUMERIC regen_energy_wh
    }

    vehicles ||--|| ev_vehicle_details : "ev_specs"
    tenants ||--o{ charging_stations : "operates"
    vehicles ||--o{ charging_sessions : "charges"
    charging_stations ||--o{ charging_sessions : "hosts"
    vehicles ||--o{ battery_health_readings : "reports"

    %% ==========================================
    %% CAMERA & MEDIA
    %% ==========================================

    camera_devices {
        UUID id PK
        UUID tenant_id FK
        UUID device_id FK
        UUID vehicle_id FK
        VARCHAR camera_type "DASHCAM_FRONT | DMS | ADAS | ..."
        VARCHAR manufacturer
        VARCHAR model
        VARCHAR resolution
        VARCHAR status "ACTIVE | INACTIVE | FAULTY"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    video_events {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_id FK
        UUID camera_device_id FK
        UUID driver_id FK
        VARCHAR event_type "COLLISION | DRIVER_DISTRACTION | LANE_DEPARTURE | ..."
        TIMESTAMPTZ event_at
        INT duration_seconds
        NUMERIC latitude
        NUMERIC longitude
        VARCHAR severity "CRITICAL | HIGH | MEDIUM | LOW"
        NUMERIC ai_confidence
        BOOLEAN reviewed
        TIMESTAMPTZ created_at
    }

    media_assets {
        UUID id PK
        UUID tenant_id FK
        UUID vehicle_id FK
        UUID video_event_id FK
        VARCHAR asset_type "VIDEO | IMAGE | THUMBNAIL | AUDIO"
        VARCHAR storage_provider "S3 | GCS | AZURE_BLOB"
        TEXT storage_path
        VARCHAR file_name
        VARCHAR mime_type
        BIGINT file_size_bytes
        BOOLEAN is_archived
        TIMESTAMPTZ created_at
    }

    vehicles ||--o{ camera_devices : "equipped_with"
    camera_devices ||--o{ video_events : "captures"
    video_events ||--o{ media_assets : "produces"
    vehicles ||--o{ media_assets : "associated_with"

    %% ==========================================
    %% DOCUMENT MANAGEMENT
    %% ==========================================

    document_types {
        UUID id PK
        VARCHAR name UK
        VARCHAR code UK
        VARCHAR applies_to "VEHICLE | DRIVER | BOTH"
        BOOLEAN is_mandatory
        INT default_validity_days
        INT reminder_days_before
        TIMESTAMPTZ created_at
    }

    documents {
        UUID id PK
        UUID tenant_id FK
        UUID document_type_id FK
        UUID vehicle_id FK
        UUID driver_id FK
        VARCHAR document_number
        DATE issued_date
        DATE expiry_date
        VARCHAR issuing_authority
        TEXT file_url
        VARCHAR status "ACTIVE | EXPIRED | RENEWED | REVOKED"
        UUID verified_by FK
        UUID uploaded_by FK
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
        TIMESTAMPTZ deleted_at
    }

    document_expiry_alerts {
        UUID id PK
        UUID tenant_id FK
        UUID document_id FK
        VARCHAR alert_type "REMINDER_30 | REMINDER_15 | REMINDER_7 | REMINDER_1 | EXPIRED"
        DATE scheduled_for
        TIMESTAMPTZ sent_at
        UUID alert_id FK
        TIMESTAMPTZ created_at
    }

    document_types ||--o{ documents : "classifies"
    vehicles ||--o{ documents : "has"
    drivers ||--o{ documents : "has"
    documents ||--o{ document_expiry_alerts : "triggers"

    %% ==========================================
    %% ANALYTICS
    %% ==========================================

    analytics_aggregates_daily {
        UUID id "PK (partitioned)"
        UUID tenant_id
        UUID vehicle_id
        UUID driver_id
        UUID depot_id
        DATE agg_date "PARTITION KEY"
        VARCHAR metric_type
        NUMERIC value
        VARCHAR unit
        JSONB metadata
        TIMESTAMPTZ created_at
    }

    scheduled_reports {
        UUID id PK
        UUID tenant_id FK
        VARCHAR name
        VARCHAR report_type "FLEET_UTILISATION | FUEL_EFFICIENCY | TCO | ..."
        JSONB filters
        VARCHAR frequency "DAILY | WEEKLY | MONTHLY"
        JSONB recipients
        JSONB formats
        BOOLEAN is_active
        UUID created_by FK
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    report_executions {
        UUID id PK
        UUID scheduled_report_id FK
        UUID tenant_id FK
        VARCHAR report_type
        VARCHAR status "PENDING | GENERATING | COMPLETED | FAILED"
        JSONB file_urls
        INT record_count
        INT generation_time_ms
        UUID generated_by FK
        TIMESTAMPTZ created_at
        TIMESTAMPTZ completed_at
    }

    dashboard_widgets {
        UUID id PK
        UUID tenant_id FK
        UUID user_id FK
        VARCHAR widget_type
        VARCHAR title
        INT position_x
        INT position_y
        INT width
        INT height
        JSONB configuration
        BOOLEAN is_visible
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    tenants ||--o{ scheduled_reports : "configures"
    scheduled_reports ||--o{ report_executions : "generates"
    tenants ||--o{ dashboard_widgets : "customizes"
    users ||--o{ dashboard_widgets : "owns"

    %% ==========================================
    %% AUDIT
    %% ==========================================

    audit_logs {
        UUID id "PK (partitioned)"
        UUID tenant_id
        UUID user_id
        VARCHAR action "INSERT | UPDATE | DELETE | SELECT | LOGIN"
        VARCHAR table_name
        UUID record_id
        JSONB old_values
        JSONB new_values
        INET ip_address
        VARCHAR request_id
        TIMESTAMPTZ created_at "PARTITION KEY"
    }

    login_audit_logs {
        UUID id "PK (partitioned)"
        UUID user_id
        VARCHAR email
        VARCHAR event_type "LOGIN_SUCCESS | LOGIN_FAILED | LOGOUT | ..."
        INET ip_address
        JSONB device_info
        VARCHAR failure_reason
        TIMESTAMPTZ created_at "PARTITION KEY"
    }

    data_access_logs {
        UUID id PK
        UUID tenant_id
        UUID user_id
        VARCHAR resource_type "driver_pii | vehicle_location | ..."
        UUID resource_id
        VARCHAR access_type "VIEW | EXPORT | SHARE | PRINT"
        JSONB fields_accessed
        INET ip_address
        TIMESTAMPTZ created_at
    }

    %% ==========================================
    %% CONTINUOUS AGGREGATES (TimescaleDB)
    %% ==========================================

    gps_readings_hourly {
        UUID vehicle_id
        UUID tenant_id
        TIMESTAMPTZ bucket "1 hour"
        NUMERIC avg_speed_kmh
        NUMERIC max_speed_kmh
        BIGINT point_count
        NUMERIC distance_km
    }

    gps_readings_daily {
        UUID vehicle_id
        UUID tenant_id
        TIMESTAMPTZ bucket "1 day"
        NUMERIC avg_speed_kmh
        NUMERIC max_speed_kmh
        BIGINT point_count
        NUMERIC distance_km
    }

    fuel_readings_hourly {
        UUID vehicle_id
        UUID tenant_id
        TIMESTAMPTZ bucket "1 hour"
        NUMERIC avg_fuel_level
        NUMERIC avg_consumption_rate
        BIGINT reading_count
    }

    fuel_readings_daily {
        UUID vehicle_id
        UUID tenant_id
        TIMESTAMPTZ bucket "1 day"
        NUMERIC avg_fuel_level
        NUMERIC avg_consumption_rate
        BIGINT reading_count
    }

    obd_readings_hourly {
        UUID vehicle_id
        UUID tenant_id
        TIMESTAMPTZ bucket "1 hour"
        NUMERIC avg_engine_rpm
        NUMERIC avg_coolant_temp
        NUMERIC min_battery_voltage
        BIGINT reading_count
    }

    gps_readings ||--o{ gps_readings_hourly : "aggregated_to"
    gps_readings ||--o{ gps_readings_daily : "aggregated_to"
    fuel_readings ||--o{ fuel_readings_hourly : "aggregated_to"
    fuel_readings ||--o{ fuel_readings_daily : "aggregated_to"
    obd_readings ||--o{ obd_readings_hourly : "aggregated_to"
```

---

## Entity Count Summary

| Category | Regular Tables | Hypertables | Partitioned Tables | Continuous Aggregates |
|---|---|---|---|---|
| Multi-Tenancy | 4 | — | — | — |
| Auth & RBAC | 7 | — | — | — |
| Fleet Structure | 4 | — | — | — |
| Vehicles & Devices | 4 | — | — | — |
| Device Twins & OTA | 4 | — | — | — |
| Drivers | 3 | 1 (driving_events) | — | — |
| GPS / Tracking | 2 | 1 (gps_readings) | — | 2 (hourly, daily) |
| Fuel Monitoring | 3 | 1 (fuel_readings) | — | 2 (hourly, daily) |
| Vehicle Health | 4 | 1 (obd_readings) | — | 1 (hourly) |
| Maintenance | 4 | — | — | — |
| Geofencing | 2 | — | 1 (geofence_events) | — |
| Alerts & Notifications | 4 | — | 1 (notification_log) | — |
| EV Support | 3 | 1 (battery_health) | — | — |
| Camera & Media | 3 | — | — | — |
| Document Management | 3 | — | — | — |
| Analytics | 3 | — | 1 (analytics_agg) | — |
| Audit | 1 | — | 2 (audit, login_audit) | — |
| Data Access | 1 | — | — | — |
| **TOTAL** | **54** | **5** | **5** | **5** |
