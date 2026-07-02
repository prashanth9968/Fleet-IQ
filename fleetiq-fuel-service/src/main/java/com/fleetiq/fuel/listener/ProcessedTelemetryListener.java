package com.fleetiq.fuel.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.fuel.dto.ProcessedTelemetry;
import com.fleetiq.fuel.entity.FuelReading;
import com.fleetiq.fuel.repository.FuelReadingRepository;
import com.fleetiq.fuel.service.FuelAnalyticsService;
import com.fleetiq.fuel.service.FuelConsumptionService;
import com.fleetiq.fuel.service.FuelIdleDetectionService;
import com.fleetiq.fuel.service.FuelLeakDetectionService;
import com.fleetiq.fuel.service.FuelSensorFaultService;
import com.fleetiq.fuel.service.FuelTheftDetectionService;
import com.fleetiq.fuel.service.RefuelDetectionService;
import com.fleetiq.fuel.state.VehicleFuelState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Primary Kafka consumer for processed telemetry events from the tracking
 * service. Orchestrates the full fuel analysis pipeline: persists raw readings,
 * runs all detection services (sensor fault → consumption → theft → leak → idle
 * → refuel), and maintains per-vehicle in-memory state.
 * <p>
 * Sensor fault evaluation runs first so that the {@code sensorFaultActive} flag
 * can suppress downstream theft and leak false positives.
 */
@Component
@Slf4j
public class ProcessedTelemetryListener {

    private final FuelConsumptionService fuelConsumptionService;
    private final FuelTheftDetectionService fuelTheftDetectionService;
    private final FuelLeakDetectionService fuelLeakDetectionService;
    private final FuelIdleDetectionService fuelIdleDetectionService;
    private final FuelSensorFaultService fuelSensorFaultService;
    private final RefuelDetectionService refuelDetectionService;
    private final FuelAnalyticsService fuelAnalyticsService;
    private final FuelReadingRepository fuelReadingRepository;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<UUID, VehicleFuelState> vehicleStates = new ConcurrentHashMap<>();

    public ProcessedTelemetryListener(FuelConsumptionService fuelConsumptionService,
                                       FuelTheftDetectionService fuelTheftDetectionService,
                                       FuelLeakDetectionService fuelLeakDetectionService,
                                       FuelIdleDetectionService fuelIdleDetectionService,
                                       FuelSensorFaultService fuelSensorFaultService,
                                       RefuelDetectionService refuelDetectionService,
                                       FuelAnalyticsService fuelAnalyticsService,
                                       FuelReadingRepository fuelReadingRepository,
                                       ObjectMapper objectMapper) {
        this.fuelConsumptionService = fuelConsumptionService;
        this.fuelTheftDetectionService = fuelTheftDetectionService;
        this.fuelLeakDetectionService = fuelLeakDetectionService;
        this.fuelIdleDetectionService = fuelIdleDetectionService;
        this.fuelSensorFaultService = fuelSensorFaultService;
        this.refuelDetectionService = refuelDetectionService;
        this.fuelAnalyticsService = fuelAnalyticsService;
        this.fuelReadingRepository = fuelReadingRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "processed.telemetry", groupId = "fleetiq-fuel-group")
    public void listen(String message) {
        ProcessedTelemetry telemetry = null;

        try {
            telemetry = objectMapper.readValue(message, ProcessedTelemetry.class);
        } catch (Exception e) {
            log.error("Failed to deserialize ProcessedTelemetry message: {}", message, e);
            return;
        }

        UUID vehicleId = telemetry.vehicleId();
        UUID tenantId = telemetry.tenantId();
        OffsetDateTime timestamp = telemetry.timestamp();
        Double fuelLevel = telemetry.fuelLevelLitres();
        Double speedKmh = telemetry.speedKmh();
        Integer engineRpm = telemetry.engineRpm();
        String correlationId = telemetry.correlationId();

        try {
            MDC.put("tenantId", tenantId != null ? tenantId.toString() : "unknown");
            MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
            MDC.put("vehicleId", vehicleId != null ? vehicleId.toString() : "unknown");

            if (vehicleId == null || tenantId == null || timestamp == null) {
                log.warn("Received ProcessedTelemetry with missing required fields; skipping. vehicleId={}, tenantId={}, timestamp={}",
                        vehicleId, tenantId, timestamp);
                return;
            }

            // Get or create vehicle fuel state
            VehicleFuelState state = vehicleStates.computeIfAbsent(vehicleId, id -> {
                VehicleFuelState newState = new VehicleFuelState();
                newState.setVehicleId(id);
                newState.setTenantId(tenantId);
                return newState;
            });

            // Retrieve and cache vehicle type ID if not already cached
            if (state.getVehicleTypeId() == null) {
                state.setVehicleTypeId(fuelReadingRepository.findVehicleTypeIdByVehicleId(vehicleId).orElse(null));
            }

            // Persist fuel reading
            persistFuelReading(telemetry, state);

            // Derive fuel rate from state (approximation from level delta and time delta)
            Double fuelRate = deriveFuelRate(state, fuelLevel, timestamp);

            // === Detection pipeline (order matters) ===

            // 1. Sensor fault first — sets sensorFaultActive flag for downstream suppression
            fuelSensorFaultService.evaluate(vehicleId, tenantId, state, fuelLevel, timestamp);

            // 2. High consumption
            fuelConsumptionService.evaluate(vehicleId, tenantId, state, fuelRate, timestamp);

            // 3. Theft (suppressed if sensorFaultActive)
            fuelTheftDetectionService.evaluate(vehicleId, tenantId, state);

            // 4. Leak (suppressed if sensorFaultActive)
            fuelLeakDetectionService.evaluate(vehicleId, tenantId, state);

            // 5. Idle burn
            fuelIdleDetectionService.evaluate(vehicleId, tenantId, state, fuelRate, timestamp);

            // 6. Refuel
            refuelDetectionService.evaluate(vehicleId, tenantId, state, fuelLevel, timestamp);

            // 7. Analytics (non-critical)
            fuelAnalyticsService.recordReading(vehicleId, tenantId, fuelLevel,
                    state.getLastOdometerKm(), speedKmh, timestamp);

            // Update state with latest values
            state.setLastFuelLevel(fuelLevel);
            state.setLastFuelRate(fuelRate);
            state.setLastSpeedKmh(speedKmh);
            state.setLastEngineRpm(engineRpm);
            state.setLastUpdate(timestamp);
            state.setLastLatitude(telemetry.latitude());
            state.setLastLongitude(telemetry.longitude());

            // Add reading to sliding window
            if (fuelLevel != null) {
                state.addReading(fuelLevel, timestamp);
            }

        } catch (Exception e) {
            log.error("Error processing telemetry for vehicle={}: {}", vehicleId, e.getMessage(), e);
        } finally {
            MDC.remove("tenantId");
            MDC.remove("correlationId");
            MDC.remove("vehicleId");
        }
    }

    private void persistFuelReading(ProcessedTelemetry telemetry, VehicleFuelState state) {
        try {
            FuelReading reading = new FuelReading();
            reading.setVehicleId(telemetry.vehicleId());
            reading.setTenantId(telemetry.tenantId());
            reading.setRecordedAt(telemetry.timestamp());
            reading.setFuelLevelLitres(telemetry.fuelLevelLitres() != null
                    ? BigDecimal.valueOf(telemetry.fuelLevelLitres()) : null);
            reading.setSpeedKmh(telemetry.speedKmh() != null
                    ? BigDecimal.valueOf(telemetry.speedKmh()) : null);
            reading.setEngineRpm(telemetry.engineRpm());
            
            // Read odometer and ignition from state if available
            reading.setIgnition(state.getLastIgnition() != null ? state.getLastIgnition() : true);
            if (state.getLastOdometerKm() != null) {
                reading.setOdometerKm(BigDecimal.valueOf(state.getLastOdometerKm()));
            }

            fuelReadingRepository.save(reading);
        } catch (Exception e) {
            log.error("Failed to persist fuel reading for vehicle={}: {}", telemetry.vehicleId(), e.getMessage(), e);
        }
    }

    /**
     * Derives an approximate fuel consumption rate (L/min) from the delta between
     * the current and previous fuel levels divided by the time elapsed.
     */
    private Double deriveFuelRate(VehicleFuelState state, Double currentFuelLevel,
                                   OffsetDateTime timestamp) {
        if (currentFuelLevel == null || state.getLastFuelLevel() == null || state.getLastUpdate() == null) {
            return null;
        }

        double fuelDelta = state.getLastFuelLevel() - currentFuelLevel;
        long secondsElapsed = java.time.Duration.between(state.getLastUpdate(), timestamp).getSeconds();

        if (secondsElapsed <= 0 || fuelDelta <= 0) {
            return 0.0;
        }

        return (fuelDelta / secondsElapsed) * 60.0; // Convert to L/min
    }
}
