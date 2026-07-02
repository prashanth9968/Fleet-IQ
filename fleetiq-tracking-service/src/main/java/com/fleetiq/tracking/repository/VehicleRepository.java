package com.fleetiq.tracking.repository;

import com.fleetiq.tracking.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    Optional<Vehicle> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
