package com.fleetiq.vehicle.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleResponse(
    UUID id,
    UUID tenantId,
    String registrationNumber,
    String vin,
    String make,
    String model,
    Integer yearOfManufacture,
    String color,
    BigDecimal fuelTankCapacityLitres,
    BigDecimal odometerReadingKm,
    BigDecimal engineHours,
    String status,
    LocalDate acquisitionDate,
    
    // Type details
    UUID vehicleTypeId,
    String vehicleTypeName,
    String vehicleCategory,
    String fuelType,

    // Depot details
    UUID depotId,
    String depotName,

    // Assigned Device details (if any)
    UUID activeDeviceId,
    String activeDeviceSerial,
    String activeDeviceType,

    // Active Driver details (if any)
    UUID activeDriverId,
    String activeDriverName,

    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
