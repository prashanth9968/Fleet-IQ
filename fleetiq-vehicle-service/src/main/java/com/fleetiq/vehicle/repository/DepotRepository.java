package com.fleetiq.vehicle.repository;

import com.fleetiq.vehicle.entity.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepotRepository extends JpaRepository<Depot, UUID> {
    List<Depot> findAllByTenantIdAndDeletedAtIsNull(UUID tenantId);
    Optional<Depot> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
