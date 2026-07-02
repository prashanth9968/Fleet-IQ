package com.fleetiq.vehicle.repository;

import com.fleetiq.vehicle.entity.DriverAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverAssignmentRepository extends JpaRepository<DriverAssignment, UUID> {
    Optional<DriverAssignment> findByVehicleIdAndStatus(UUID vehicleId, String status);
    Optional<DriverAssignment> findByDriverIdAndStatus(UUID driverId, String status);
    List<DriverAssignment> findAllByVehicleId(UUID vehicleId);
}
