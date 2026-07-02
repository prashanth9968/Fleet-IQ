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
@Table(name = "fuel_analytics")
@Getter
@Setter
public class FuelAnalytic {

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

    @Column(name = "period_start", nullable = false)
    private OffsetDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private OffsetDateTime periodEnd;

    @Column(name = "total_fuel_used_litres", precision = 10, scale = 2)
    private BigDecimal totalFuelUsedLitres;

    @Column(name = "total_distance_km", precision = 12, scale = 2)
    private BigDecimal totalDistanceKm;

    @Column(name = "efficiency_litres_per_100km", precision = 8, scale = 2)
    private BigDecimal efficiencyLitresPer100km;

    @Column(name = "efficiency_km_per_litre", precision = 8, scale = 2)
    private BigDecimal efficiencyKmPerLitre;

    @Column(name = "idle_fuel_used_litres", precision = 8, scale = 2)
    private BigDecimal idleFuelUsedLitres;

    @Column(name = "highway_fuel_used_litres", precision = 8, scale = 2)
    private BigDecimal highwayFuelUsedLitres;

    @Column(name = "cost_per_km", precision = 8, scale = 4)
    private BigDecimal costPerKm;

    @Column(name = "cost_per_trip", precision = 10, scale = 2)
    private BigDecimal costPerTrip;

    @Column(name = "deviation_from_baseline_pct", precision = 6, scale = 2)
    private BigDecimal deviationFromBaselinePct;

    @Column(name = "baseline_efficiency_km_per_litre", precision = 8, scale = 2)
    private BigDecimal baselineEfficiencyKmPerLitre;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
