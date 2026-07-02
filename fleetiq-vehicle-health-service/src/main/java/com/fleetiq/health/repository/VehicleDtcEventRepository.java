package com.fleetiq.health.repository;

import com.fleetiq.health.entity.VehicleDtcEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleDtcEventRepository extends JpaRepository<VehicleDtcEvent, UUID> {
    List<VehicleDtcEvent> findByVehicleIdAndIsActiveTrue(UUID vehicleId);
}
