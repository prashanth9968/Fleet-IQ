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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fuel_baselines")
@Getter
@Setter
public class FuelBaseline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "vehicle_type_id", nullable = false)
    private UUID vehicleTypeId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "expected_efficiency_km_per_litre", precision = 8, scale = 2)
    private BigDecimal expectedEfficiencyKmPerLitre;

    @Column(name = "normal_idle_burn_l_per_min", precision = 8, scale = 4)
    private BigDecimal normalIdleBurnLPerMin;

    @Column(name = "normal_highway_burn_l_per_min", precision = 8, scale = 4)
    private BigDecimal normalHighwayBurnLPerMin;

    @Column(name = "normal_city_burn_l_per_min", precision = 8, scale = 4)
    private BigDecimal normalCityBurnLPerMin;

    @Column(name = "tank_capacity_litres", precision = 8, scale = 2)
    private BigDecimal tankCapacityLitres;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
