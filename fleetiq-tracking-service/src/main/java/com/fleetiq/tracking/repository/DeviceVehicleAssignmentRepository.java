package com.fleetiq.tracking.repository;

import com.fleetiq.tracking.entity.DeviceVehicleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceVehicleAssignmentRepository extends JpaRepository<DeviceVehicleAssignment, UUID> {

    @Query("SELECT a FROM DeviceVehicleAssignment a " +
           "JOIN FETCH a.vehicle v " +
           "JOIN FETCH v.tenant " +
           "WHERE a.device.id = :deviceId AND a.unassignedAt IS NULL")
    Optional<DeviceVehicleAssignment> findByDeviceIdAndUnassignedAtIsNull(@Param("deviceId") UUID deviceId);
}
