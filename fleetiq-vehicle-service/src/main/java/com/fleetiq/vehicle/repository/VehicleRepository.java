package com.fleetiq.vehicle.repository;

import com.fleetiq.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    List<Vehicle> findAllByTenantIdAndDeletedAtIsNull(UUID tenantId);
    Optional<Vehicle> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
    Optional<Vehicle> findByRegistrationNumberAndTenantIdAndDeletedAtIsNull(String registrationNumber, UUID tenantId);
    boolean existsByRegistrationNumberAndTenantIdAndDeletedAtIsNull(String registrationNumber, UUID tenantId);
}
