package com.fleetiq.fuel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fuel_thresholds")
@Getter
@Setter
public class FuelThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "vehicle_type_id")
    private UUID vehicleTypeId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    @Column(name = "threshold_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal thresholdValue;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
