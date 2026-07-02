package com.fleetiq.vehicle.repository;

import com.fleetiq.vehicle.entity.VehicleGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleGroupRepository extends JpaRepository<VehicleGroup, UUID> {
    List<VehicleGroup> findAllByTenantIdAndDeletedAtIsNull(UUID tenantId);
    Optional<VehicleGroup> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
