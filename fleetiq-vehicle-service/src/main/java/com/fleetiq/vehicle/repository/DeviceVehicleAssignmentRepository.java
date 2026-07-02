package com.fleetiq.vehicle.repository;

import com.fleetiq.vehicle.entity.DeviceVehicleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceVehicleAssignmentRepository extends JpaRepository<DeviceVehicleAssignment, UUID> {
    Optional<DeviceVehicleAssignment> findByVehicleIdAndUnassignedAtIsNull(UUID vehicleId);
    Optional<DeviceVehicleAssignment> findByDeviceIdAndUnassignedAtIsNull(UUID deviceId);
    List<DeviceVehicleAssignment> findAllByVehicleId(UUID vehicleId);
}
