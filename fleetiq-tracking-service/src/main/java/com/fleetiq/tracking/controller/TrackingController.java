package com.fleetiq.tracking.controller;

import com.fleetiq.tracking.config.TenantContext;
import com.fleetiq.tracking.entity.GpsReading;
import com.fleetiq.tracking.exception.ResourceNotFoundException;
import com.fleetiq.tracking.exception.UnauthorizedException;
import com.fleetiq.tracking.repository.GpsReadingRepository;
import com.fleetiq.tracking.repository.VehicleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tracking")
@Tag(name = "Vehicle Tracking & History", description = "Endpoints for fetching historical telemetry and route playbacks")
public class TrackingController {

    private final GpsReadingRepository gpsReadingRepository;
    private final VehicleRepository vehicleRepository;

    public TrackingController(
            GpsReadingRepository gpsReadingRepository,
            VehicleRepository vehicleRepository
    ) {
        this.gpsReadingRepository = gpsReadingRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping("/vehicles/{vehicleId}/history")
    @PreAuthorize("hasAuthority('tracking:view_history')")
    @Operation(summary = "Get historical GPS coordinate readings for a vehicle")
    public ResponseEntity<List<GpsReading>> getVehicleHistory(
            @PathVariable("vehicleId") UUID vehicleId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new UnauthorizedException("Tenant context (X-Tenant-ID) is missing or invalid");
        }

        // Verify vehicle belongs to tenant to enforce multi-tenancy
        vehicleRepository.findByIdAndTenantIdAndDeletedAtIsNull(vehicleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found or does not belong to this tenant"));

        List<GpsReading> history = gpsReadingRepository.findAllByVehicleIdAndRecordedAtBetween(vehicleId, start, end);
        return ResponseEntity.ok(history);
    }
}
