package com.fleetiq.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_settings")
@Getter
@Setter
public class TenantSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    @Column(name = "default_speed_unit")
    private String defaultSpeedUnit = "KMH";

    @Column(name = "default_fuel_unit")
    private String defaultFuelUnit = "LITRES";

    @Column(name = "default_distance_unit")
    private String defaultDistanceUnit = "KM";

    @Column(name = "default_temperature_unit")
    private String defaultTemperatureUnit = "C";

    @Column(name = "fuel_theft_threshold_litres")
    private BigDecimal fuelTheftThresholdLitres = BigDecimal.valueOf(5.0);

    @Column(name = "fuel_theft_time_window_sec")
    private Integer fuelTheftTimeWindowSec = 120;

    @Column(name = "fuel_consumption_alert_rate")
    private BigDecimal fuelConsumptionAlertRate = BigDecimal.valueOf(1.0);

    @Column(name = "harsh_accel_threshold")
    private BigDecimal harshAccelThreshold = BigDecimal.valueOf(2.5);

    @Column(name = "harsh_brake_threshold")
    private BigDecimal harshBrakeThreshold = BigDecimal.valueOf(-3.0);

    @Column(name = "harsh_corner_threshold")
    private BigDecimal harshCornerThreshold = BigDecimal.valueOf(2.5);

    @Column(name = "speeding_tolerance_pct")
    private Integer speedingTolerancePct = 10;

    @Column(name = "idle_timeout_minutes")
    private Integer idleTimeoutMinutes = 5;

    @Column(name = "data_retention_gps_days")
    private Integer dataRetentionGpsDays = 90;

    @Column(name = "data_retention_fuel_days")
    private Integer dataRetentionFuelDays = 90;

    @Column(name = "data_retention_obd_days")
    private Integer dataRetentionObdDays = 60;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
