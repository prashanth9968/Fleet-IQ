package com.fleetiq.driver.service;

import com.fleetiq.driver.entity.Driver;
import com.fleetiq.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DriverService {

    private final DriverRepository driverRepository;

    public List<Driver> getAllDrivers(UUID tenantId) {
        return driverRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    public Driver getDriverById(UUID id, UUID tenantId) {
        return driverRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found or access denied"));
    }

    public Driver createDriver(Driver driver, UUID tenantId) {
        driver.setId(UUID.randomUUID());
        driver.setTenantId(tenantId);
        driver.setDeletedAt(null);
        if (driver.getStatus() == null) {
            driver.setStatus("ACTIVE");
        }
        log.info("Creating driver: {} {} for tenant {}", driver.getFirstName(), driver.getLastName(), tenantId);
        return driverRepository.save(driver);
    }

    public Driver updateDriver(UUID id, Driver driverDetails, UUID tenantId) {
        Driver existing = getDriverById(id, tenantId);

        existing.setFirstName(driverDetails.getFirstName());
        existing.setLastName(driverDetails.getLastName());
        existing.setUserId(driverDetails.getUserId());
        existing.setDepotId(driverDetails.getDepotId());
        existing.setEmployeeId(driverDetails.getEmployeeId());
        existing.setPhone(driverDetails.getPhone());
        existing.setEmail(driverDetails.getEmail());
        existing.setLicenseNumber(driverDetails.getLicenseNumber());
        existing.setLicenseType(driverDetails.getLicenseType());
        existing.setLicenseExpiry(driverDetails.getLicenseExpiry());
        existing.setDateOfBirth(driverDetails.getDateOfBirth());
        existing.setBloodGroup(driverDetails.getBloodGroup());
        existing.setEmergencyContactName(driverDetails.getEmergencyContactName());
        existing.setEmergencyContactPhone(driverDetails.getEmergencyContactPhone());
        existing.setPhotoUrl(driverDetails.getPhotoUrl());
        existing.setStatus(driverDetails.getStatus());
        existing.setHireDate(driverDetails.getHireDate());
        existing.setTerminationDate(driverDetails.getTerminationDate());
        existing.setMetadata(driverDetails.getMetadata());

        log.info("Updating driver: {} {} for tenant {}", existing.getFirstName(), existing.getLastName(), tenantId);
        return driverRepository.save(existing);
    }

    public void deleteDriver(UUID id, UUID tenantId) {
        Driver existing = getDriverById(id, tenantId);
        existing.setDeletedAt(Instant.now());
        existing.setStatus("INACTIVE");
        driverRepository.save(existing);
        log.info("Soft-deleted driver ID: {} for tenant {}", id, tenantId);
    }
}
