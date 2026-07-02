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
@Table(name = "fuel_refuel_events")
@Getter
@Setter
public class FuelRefuelEvent {

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

    @Column(name = "refueled_at", nullable = false)
    private OffsetDateTime refueledAt;

    @Column(name = "fuel_before_litres", precision = 8, scale = 2)
    private BigDecimal fuelBeforeLitres;

    @Column(name = "fuel_after_litres", precision = 8, scale = 2)
    private BigDecimal fuelAfterLitres;

    @Column(name = "fuel_added_litres", precision = 8, scale = 2)
    private BigDecimal fuelAddedLitres;

    @Column(name = "location_lat", precision = 10, scale = 7)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 10, scale = 7)
    private BigDecimal locationLng;

    @Column(name = "cost_total", precision = 10, scale = 2)
    private BigDecimal costTotal;

    @Column(name = "cost_per_litre", precision = 8, scale = 4)
    private BigDecimal costPerLitre;

    @Column(name = "odometer_km", precision = 12, scale = 2)
    private BigDecimal odometerKm;

    @Column(name = "source", length = 20)
    private String source;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
