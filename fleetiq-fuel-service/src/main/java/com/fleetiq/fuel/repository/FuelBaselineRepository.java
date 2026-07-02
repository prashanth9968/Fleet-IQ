package com.fleetiq.fuel.repository;

import com.fleetiq.fuel.entity.FuelBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FuelBaselineRepository extends JpaRepository<FuelBaseline, UUID> {

    Optional<FuelBaseline> findByVehicleTypeIdAndTenantId(UUID vehicleTypeId, UUID tenantId);
}
