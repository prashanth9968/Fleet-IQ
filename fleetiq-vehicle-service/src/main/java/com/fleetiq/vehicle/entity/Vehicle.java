package com.fleetiq.vehicle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "registration_number"})
})
@Getter
@Setter
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id")
    private Depot depot;

    @Column(name = "registration_number", nullable = false, length = 20)
    private String registrationNumber;

    @Column(length = 17)
    private String vin;

    @Column(name = "chassis_number", length = 50)
    private String chassisNumber;

    @Column(name = "engine_number", length = 50)
    private String engineNumber;

    private String make;

    private String model;

    @Column(name = "year_of_manufacture")
    private Integer yearOfManufacture;

    private String color;

    @Column(name = "fuel_tank_capacity_litres", precision = 8, scale = 2)
    private BigDecimal fuelTankCapacityLitres;

    @Column(name = "odometer_reading_km", precision = 12, scale = 2)
    private BigDecimal odometerReadingKm = BigDecimal.ZERO;

    @Column(name = "engine_hours", precision = 10, scale = 2)
    private BigDecimal engineHours = BigDecimal.ZERO;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(name = "acquisition_cost", precision = 14, scale = 2)
    private BigDecimal acquisitionCost;

    @Column(name = "insurance_expiry")
    private LocalDate insuranceExpiry;

    @Column(name = "permit_expiry")
    private LocalDate permitExpiry;

    @Column(name = "fitness_expiry")
    private LocalDate fitnessExpiry;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "last_service_odometer", precision = 12, scale = 2)
    private BigDecimal lastServiceOdometer;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
