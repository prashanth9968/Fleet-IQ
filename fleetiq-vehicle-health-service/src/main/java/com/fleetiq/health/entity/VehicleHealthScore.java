package com.fleetiq.health.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicle_health_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleHealthScore {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vehicle_id", nullable = false, unique = true)
    private UUID vehicleId;

    @Column(name = "overall_score")
    private BigDecimal overallScore;

    @Column(name = "engine_score")
    private BigDecimal engineScore;

    @Column(name = "transmission_score")
    private BigDecimal transmissionScore;

    @Column(name = "electrical_score")
    private BigDecimal electricalScore;

    @Column(name = "brake_score")
    private BigDecimal brakeScore;

    @Column(name = "battery_score")
    private BigDecimal batteryScore;

    @Column(name = "tyre_score")
    private BigDecimal tyreScore;

    @Column(name = "emission_score")
    private BigDecimal emissionScore;

    @Column(name = "cooling_score")
    private BigDecimal coolingScore;

    @Column(name = "active_dtc_count", nullable = false)
    private int activeDtcCount;

    @Column(name = "pending_dtc_count", nullable = false)
    private int pendingDtcCount;

    @Column(name = "last_calculated_at", nullable = false)
    private Instant lastCalculatedAt;
}
