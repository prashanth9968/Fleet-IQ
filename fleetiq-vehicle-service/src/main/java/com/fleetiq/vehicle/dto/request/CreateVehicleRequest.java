package com.fleetiq.vehicle.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateVehicleRequest(
    @NotBlank(message = "Registration number is required")
    @Size(max = 20, message = "Registration number must not exceed 20 characters")
    String registrationNumber,

    @NotNull(message = "Vehicle type ID is required")
    String vehicleTypeId, // UUID String

    String depotId, // Nullable UUID String

    @Size(max = 17, message = "VIN must not exceed 17 characters")
    String vin,

    String chassisNumber,
    String engineNumber,
    String make,
    String model,

    @Min(value = 1900, message = "Year of manufacture must be realistic")
    Integer yearOfManufacture,

    String color,

    @DecimalMin(value = "0.0", message = "Fuel tank capacity must be positive")
    BigDecimal fuelTankCapacityLitres,

    @DecimalMin(value = "0.0", message = "Odometer reading must be positive")
    BigDecimal odometerReadingKm,

    @DecimalMin(value = "0.0", message = "Engine hours must be positive")
    BigDecimal engineHours,

    String status, // ACTIVE, INACTIVE, IN_MAINTENANCE...
    LocalDate acquisitionDate,
    BigDecimal acquisitionCost,
    LocalDate insuranceExpiry,
    LocalDate permitExpiry,
    LocalDate fitnessExpiry
) {}
