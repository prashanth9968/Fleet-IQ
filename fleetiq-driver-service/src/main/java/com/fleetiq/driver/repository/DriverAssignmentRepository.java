package com.fleetiq.driver.repository;

import com.fleetiq.driver.entity.DriverAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverAssignmentRepository extends JpaRepository<DriverAssignment, UUID> {

    @Query("SELECT da FROM DriverAssignment da WHERE da.vehicleId = :vehicleId AND da.status = 'ACTIVE'")
    Optional<DriverAssignment> findActiveAssignmentByVehicleId(@Param("vehicleId") UUID vehicleId);

    @Query("SELECT da FROM DriverAssignment da WHERE da.driverId = :driverId AND da.status = 'ACTIVE'")
    Optional<DriverAssignment> findActiveAssignmentByDriverId(@Param("driverId") UUID driverId);
}
