package com.fleetiq.driver.repository;

import com.fleetiq.driver.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {
    List<Driver> findByTenantIdAndStatus(UUID tenantId, String status);
    List<Driver> findByTenantIdAndDeletedAtIsNull(UUID tenantId);
    Optional<Driver> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
