package com.fleetiq.vehicle.controller;

import com.fleetiq.vehicle.dto.request.AssignDeviceRequest;
import com.fleetiq.vehicle.dto.request.AssignDriverRequest;
import com.fleetiq.vehicle.dto.request.CreateVehicleRequest;
import com.fleetiq.vehicle.dto.response.VehicleResponse;
import com.fleetiq.vehicle.entity.User;
import com.fleetiq.vehicle.exception.ResourceNotFoundException;
import com.fleetiq.vehicle.repository.UserRepository;
import com.fleetiq.vehicle.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicle Management", description = "Endpoints for vehicle inventory CRUD, and device/driver assignments")
public class VehicleController {

    private final VehicleService vehicleService;
    private final UserRepository userRepository;

    public VehicleController(VehicleService vehicleService, UserRepository userRepository) {
        this.vehicleService = vehicleService;
        this.userRepository = userRepository;
    }

    private UUID getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('vehicles:create')")
    @Operation(summary = "Add a new vehicle to the fleet")
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody CreateVehicleRequest request) {
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('vehicles:view')")
    @Operation(summary = "List all vehicles for the current tenant")
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        List<VehicleResponse> response = vehicleService.getAllVehicles();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('vehicles:view')")
    @Operation(summary = "Get vehicle details by ID")
    public ResponseEntity<VehicleResponse> getVehicle(@PathVariable("id") UUID id) {
        VehicleResponse response = vehicleService.getVehicle(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('vehicles:update')")
    @Operation(summary = "Update vehicle details")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateVehicleRequest request
    ) {
        VehicleResponse response = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('vehicles:delete')")
    @Operation(summary = "Soft-delete a vehicle from inventory")
    public ResponseEntity<Void> deleteVehicle(@PathVariable("id") UUID id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign-device")
    @PreAuthorize("hasAuthority('vehicles:update')")
    @Operation(summary = "Assign a tracking hardware device to a vehicle")
    public ResponseEntity<Void> assignDevice(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AssignDeviceRequest request
    ) {
        UUID userId = getCurrentUserId();
        vehicleService.assignDevice(id, request, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/assign-driver")
    @PreAuthorize("hasAuthority('drivers:assign')")
    @Operation(summary = "Assign a driver shift to a vehicle")
    public ResponseEntity<Void> assignDriver(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AssignDriverRequest request
    ) {
        UUID userId = getCurrentUserId();
        vehicleService.assignDriver(id, request, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/location")
    @Operation(summary = "Update vehicle real-time location (used by simulator)")
    public ResponseEntity<Void> updateLocation(
            @PathVariable("id") UUID id,
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam("speed") Double speed,
            @RequestParam("heading") Double heading
    ) {
        vehicleService.updateLocation(id, lat, lng, speed, heading);
        return ResponseEntity.ok().build();
    }
}
