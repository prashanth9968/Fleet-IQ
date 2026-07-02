package com.fleetiq.fuel.repository;

import com.fleetiq.fuel.entity.FuelThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FuelThresholdRepository extends JpaRepository<FuelThreshold, UUID> {

    Optional<FuelThreshold> findByVehicleIdAndAlertType(UUID vehicleId, String alertType);

    Optional<FuelThreshold> findByVehicleTypeIdAndAlertTypeAndTenantId(UUID vehicleTypeId, String alertType, UUID tenantId);
}
