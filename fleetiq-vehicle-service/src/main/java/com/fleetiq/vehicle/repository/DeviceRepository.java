package com.fleetiq.vehicle.repository;

import com.fleetiq.vehicle.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findAllByTenantIdAndDeletedAtIsNull(UUID tenantId);
    Optional<Device> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
    Optional<Device> findBySerialNumber(String serialNumber);
}
