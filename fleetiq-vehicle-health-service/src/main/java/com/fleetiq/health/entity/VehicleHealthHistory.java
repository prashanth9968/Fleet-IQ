package com.fleetiq.health.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "vehicle_health_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleHealthHistory {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

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

    @Column(name = "odometer_km")
    private BigDecimal odometerKm;

    @Column(name = "engine_hours")
    private BigDecimal engineHours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;
}
