package com.fleetiq.health.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.health.dto.MaintenanceEventDto;
import com.fleetiq.health.entity.MaintenancePrediction;
import com.fleetiq.health.entity.VehicleHealthHistory;
import com.fleetiq.health.entity.WorkOrder;
import com.fleetiq.health.repository.MaintenancePredictionRepository;
import com.fleetiq.health.repository.VehicleHealthHistoryRepository;
import com.fleetiq.health.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class HealthController {

    private final VehicleHealthHistoryRepository healthHistoryRepository;
    private final MaintenancePredictionRepository predictionRepository;
    private final WorkOrderRepository workOrderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping("/health/history/{vehicleId}")
    public ResponseEntity<List<VehicleHealthHistory>> getHealthHistory(@PathVariable UUID vehicleId) {
        Instant end = Instant.now();
        Instant start = end.minus(30, ChronoUnit.DAYS);
        List<VehicleHealthHistory> history = healthHistoryRepository
                .findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(vehicleId, start, end);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/health/predictions/{vehicleId}")
    public ResponseEntity<List<MaintenancePrediction>> getPredictions(@PathVariable UUID vehicleId) {
        List<MaintenancePrediction> predictions = predictionRepository.findByVehicleIdAndStatus(vehicleId, "ACTIVE");
        return ResponseEntity.ok(predictions);
    }

    @GetMapping("/maintenance/work-orders/{vehicleId}")
    public ResponseEntity<List<WorkOrder>> getWorkOrders(@PathVariable UUID vehicleId) {
        List<WorkOrder> orders = workOrderRepository.findByVehicleIdAndStatus(vehicleId, "ACTIVE");
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/maintenance/complete-work-order")
    public ResponseEntity<Map<String, String>> completeWorkOrder(@RequestBody Map<String, UUID> request) {
        UUID workOrderId = request.get("workOrderId");
        if (workOrderId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "workOrderId parameter is required"));
        }

        return workOrderRepository.findById(workOrderId)
                .map(workOrder -> {
                    workOrder.setStatus("COMPLETED");
                    workOrder.setCompletedAt(Instant.now());
                    workOrderRepository.save(workOrder);

                    // Publish WORK_ORDER_COMPLETED event
                    MaintenanceEventDto event = new MaintenanceEventDto(
                            workOrder.getTenantId(),
                            workOrder.getVehicleId(),
                            "WORK_ORDER_COMPLETED",
                            workOrderId,
                            "Work Order completed successfully: " + workOrder.getTitle(),
                            Instant.now()
                    );
                    try {
                        kafkaTemplate.send("maintenance.events", objectMapper.writeValueAsString(event));
                    } catch (Exception e) {
                        log.error("Failed to serialize work order completion event", e);
                    }

                    log.info("Completed work order ID: {}, vehicle ID: {}", workOrderId, workOrder.getVehicleId());
                    return ResponseEntity.ok(Map.of("message", "Work order resolved successfully"));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("message", "Work order not found")));
    }
}
