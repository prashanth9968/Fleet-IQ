package com.fleetiq.health.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VehicleHealthEventDto(
        UUID vehicleId,
        UUID tenantId,
        UUID deviceId,
        Instant recordedAt,
        Double engineRpm,
        Double coolantTempC,
        Double oilPressureKpa,
        Double batteryVoltage,
        Double intakeAirTempC,
        Double throttlePositionPct,
        Double engineLoadPct,
        Double fuelTrimShortPct,
        Double fuelTrimLongPct,
        Double mafGps,
        Double catalystTempC,
        Double ambientAirTempC,
        Double odometerKm,
        Double engineRunHours,
        Double dpfSootLoadPct,
        Boolean dpfRegenStatus,
        Boolean checkEngineLight,
        List<String> activeDtcs,
        String metadata
) {}
