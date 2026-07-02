package com.fleetiq.driver.controller;

import com.fleetiq.driver.config.TenantContext;
import com.fleetiq.driver.entity.Driver;
import com.fleetiq.driver.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context (X-Tenant-ID) is missing or invalid");
        }
        return tenantId;
    }

    @GetMapping
    public ResponseEntity<List<Driver>> getAllDrivers() {
        UUID tenantId = getRequiredTenantId();
        return ResponseEntity.ok(driverService.getAllDrivers(tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Driver> getDriver(@PathVariable("id") UUID id) {
        UUID tenantId = getRequiredTenantId();
        return ResponseEntity.ok(driverService.getDriverById(id, tenantId));
    }

    @PostMapping
    public ResponseEntity<Driver> createDriver(@RequestBody Driver driver) {
        UUID tenantId = getRequiredTenantId();
        return ResponseEntity.ok(driverService.createDriver(driver, tenantId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Driver> updateDriver(@PathVariable("id") UUID id, @RequestBody Driver driver) {
        UUID tenantId = getRequiredTenantId();
        return ResponseEntity.ok(driverService.updateDriver(id, driver, tenantId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(@PathVariable("id") UUID id) {
        UUID tenantId = getRequiredTenantId();
        driverService.deleteDriver(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
