package com.fleetiq.tracking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_types")
@Getter
@Setter
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(name = "fuel_type", nullable = false)
    private String fuelType;

    @Column(name = "default_fuel_capacity_litres", precision = 8, scale = 2)
    private BigDecimal defaultFuelCapacityLitres;

    @Column(name = "default_fuel_consumption_rate", precision = 6, scale = 2)
    private BigDecimal defaultFuelConsumptionRate;

    private String icon;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
