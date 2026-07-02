package com.fleetiq.driver.service;

import com.fleetiq.driver.entity.DriverAssignment;
import com.fleetiq.driver.repository.DriverAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DriverAssignmentService {

    private final DriverAssignmentRepository assignmentRepository;

    public DriverAssignment assignDriver(UUID driverId, UUID vehicleId, UUID tenantId, UUID assignedBy, String notes) {
        // Validate active assignments
        Optional<DriverAssignment> activeDriverAssg = assignmentRepository.findActiveAssignmentByDriverId(driverId);
        if (activeDriverAssg.isPresent()) {
            throw new IllegalStateException("Driver is already assigned to a vehicle in an active shift");
        }

        Optional<DriverAssignment> activeVehicleAssg = assignmentRepository.findActiveAssignmentByVehicleId(vehicleId);
        if (activeVehicleAssg.isPresent()) {
            throw new IllegalStateException("Vehicle is already assigned to another driver in an active shift");
        }

        DriverAssignment assignment = DriverAssignment.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .driverId(driverId)
                .vehicleId(vehicleId)
                .shiftStart(Instant.now())
                .assignedBy(assignedBy)
                .status("ACTIVE")
                .notes(notes)
                .build();

        log.info("Assigning driver ID: {} to vehicle ID: {} for tenant {}", driverId, vehicleId, tenantId);
        return assignmentRepository.save(assignment);
    }

    public DriverAssignment endShift(UUID assignmentId, UUID tenantId) {
        DriverAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        if (!assignment.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Access denied");
        }

        if (!"ACTIVE".equals(assignment.getStatus())) {
            throw new IllegalStateException("Assignment is already inactive");
        }

        assignment.setStatus("COMPLETED");
        assignment.setShiftEnd(Instant.now());
        log.info("Ending shift assignment ID: {} for tenant {}", assignmentId, tenantId);
        return assignmentRepository.save(assignment);
    }
}
