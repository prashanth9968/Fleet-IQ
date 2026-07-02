package com.fleetiq.driver.controller;

import com.fleetiq.driver.config.TenantContext;
import com.fleetiq.driver.dto.AssignDriverRequest;
import com.fleetiq.driver.entity.DriverAssignment;
import com.fleetiq.driver.service.DriverAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/driver-assignments")
@RequiredArgsConstructor
public class DriverAssignmentController {

    private final DriverAssignmentService assignmentService;

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context (X-Tenant-ID) is missing or invalid");
        }
        return tenantId;
    }

    @PostMapping
    public ResponseEntity<DriverAssignment> assignDriver(@RequestBody AssignDriverRequest request) {
        UUID tenantId = getRequiredTenantId();
        DriverAssignment assignment = assignmentService.assignDriver(
                request.driverId(),
                request.vehicleId(),
                tenantId,
                request.assignedBy(),
                request.notes()
        );
        return ResponseEntity.ok(assignment);
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<DriverAssignment> endShift(@PathVariable("id") UUID id) {
        UUID tenantId = getRequiredTenantId();
        DriverAssignment assignment = assignmentService.endShift(id, tenantId);
        return ResponseEntity.ok(assignment);
    }
}
