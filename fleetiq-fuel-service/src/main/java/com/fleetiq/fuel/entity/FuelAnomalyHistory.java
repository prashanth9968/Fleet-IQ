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
@Table(name = "fuel_anomaly_history")
@Getter
@Setter
public class FuelAnomalyHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "anomaly_type", nullable = false, length = 50)
    private String anomalyType;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "fuel_drop_litres", precision = 8, scale = 2)
    private BigDecimal fuelDropLitres;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "speed_kmh_at_event", precision = 6, scale = 2)
    private BigDecimal speedKmhAtEvent;

    @Column(name = "ignition_state")
    private Boolean ignitionState;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
