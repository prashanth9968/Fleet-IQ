package com.fleetiq.fuel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fuel_readings")
@IdClass(FuelReadingId.class)
@Getter
@Setter
public class FuelReading {

    @Column(name = "id")
    private UUID id;

    @Id
    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Id
    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "fuel_level_litres", precision = 8, scale = 2)
    private BigDecimal fuelLevelLitres;

    @Column(name = "fuel_rate_litres_per_min", precision = 8, scale = 4)
    private BigDecimal fuelRateLitresPerMin;

    @Column(name = "odometer_km", precision = 12, scale = 2)
    private BigDecimal odometerKm;

    @Column(name = "speed_kmh", precision = 6, scale = 2)
    private BigDecimal speedKmh;

    @Column(name = "engine_rpm")
    private Integer engineRpm;

    @Column(name = "ignition")
    private Boolean ignition;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
