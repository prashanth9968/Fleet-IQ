package com.fleetiq.analytics.entity;

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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_fleet_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyFleetReport {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "distance_km", nullable = false)
    private BigDecimal distanceKm;

    @Column(name = "fuel_consumed_litres", nullable = false)
    private BigDecimal fuelConsumedLitres;

    @Column(name = "avg_fuel_efficiency", nullable = false)
    private BigDecimal avgFuelEfficiency;

    @Column(name = "safety_score", nullable = false)
    private BigDecimal safetyScore;

    @Column(name = "fault_count", nullable = false)
    private Integer faultCount;

    @Column(name = "utilization_pct", nullable = false)
    private BigDecimal utilizationPct;

    @Column(name = "avg_trip_duration_mins", nullable = false)
    private BigDecimal avgTripDurationMins;

    @Column(name = "avg_idle_time_mins", nullable = false)
    private BigDecimal avgIdleTimeMins;

    @Column(name = "fuel_cost", nullable = false)
    private BigDecimal fuelCost;

    @Column(name = "maintenance_cost", nullable = false)
    private BigDecimal maintenanceCost;

    @Column(name = "critical_alerts_count", nullable = false)
    private Integer criticalAlertsCount;

    @Column(name = "co2_estimate_kg", nullable = false)
    private BigDecimal co2EstimateKg;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
