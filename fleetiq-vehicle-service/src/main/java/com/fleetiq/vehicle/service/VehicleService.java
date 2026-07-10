package com.fleetiq.vehicle.service;

import com.fleetiq.vehicle.dto.request.AssignDeviceRequest;
import com.fleetiq.vehicle.dto.request.AssignDriverRequest;
import com.fleetiq.vehicle.dto.request.CreateVehicleRequest;
import com.fleetiq.vehicle.dto.response.VehicleResponse;

import java.util.List;
import java.util.UUID;

public interface VehicleService {
    VehicleResponse createVehicle(CreateVehicleRequest request);
    VehicleResponse getVehicle(UUID id);
    List<VehicleResponse> getAllVehicles();
    VehicleResponse updateVehicle(UUID id, CreateVehicleRequest request);
    void deleteVehicle(UUID id);
    void assignDevice(UUID vehicleId, AssignDeviceRequest request, UUID assignedByUserId);
    void assignDriver(UUID vehicleId, AssignDriverRequest request, UUID assignedByUserId);
    void updateLocation(UUID id, Double lat, Double lng, Double speed, Double heading);
}
