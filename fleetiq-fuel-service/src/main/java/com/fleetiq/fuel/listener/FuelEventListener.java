package com.fleetiq.fuel.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.fuel.dto.FuelEvent;
import com.fleetiq.fuel.entity.FuelReading;
import com.fleetiq.fuel.repository.FuelReadingRepository;
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
 * Kafka consumer for dedicated fuel sensor events published to the
 * {@code fuel.events} topic. Processes {@link FuelEventDto} payloads that
 * include a direct {@code fuelRateLitresPerMin} measurement from fuel-specific
 * sensor hardware, eliminating the need to derive fuel rate from level deltas.
 */
@Component
@Slf4j
public class FuelEventListener {

    private final FuelConsumptionService fuelConsumptionService;
    private final FuelTheftDetectionService fuelTheftDetectionService;
    private final FuelLeakDetectionService fuelLeakDetectionService;
    private final FuelIdleDetectionService fuelIdleDetectionService;
    private final FuelSensorFaultService fuelSensorFaultService;
    private final RefuelDetectionService refuelDetectionService;
    private final FuelReadingRepository fuelReadingRepository;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<UUID, VehicleFuelState> vehicleStates = new ConcurrentHashMap<>();

    public FuelEventListener(FuelConsumptionService fuelConsumptionService,
                              FuelTheftDetectionService fuelTheftDetectionService,
                              FuelLeakDetectionService fuelLeakDetectionService,
                              FuelIdleDetectionService fuelIdleDetectionService,
                              FuelSensorFaultService fuelSensorFaultService,
                              RefuelDetectionService refuelDetectionService,
                              FuelReadingRepository fuelReadingRepository,
                              ObjectMapper objectMapper) {
        this.fuelConsumptionService = fuelConsumptionService;
        this.fuelTheftDetectionService = fuelTheftDetectionService;
        this.fuelLeakDetectionService = fuelLeakDetectionService;
        this.fuelIdleDetectionService = fuelIdleDetectionService;
        this.fuelSensorFaultService = fuelSensorFaultService;
        this.refuelDetectionService = refuelDetectionService;
        this.fuelReadingRepository = fuelReadingRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "fuel.events", groupId = "fleetiq-fuel-group")
    public void listen(String message) {
        FuelEvent fuelEvent = null;

        try {
            fuelEvent = objectMapper.readValue(message, FuelEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize FuelEvent message: {}", message, e);
            return;
        }

        UUID vehicleId = fuelEvent.vehicleId();
        UUID tenantId = fuelEvent.tenantId();
        UUID deviceId = fuelEvent.deviceId();
        OffsetDateTime timestamp = fuelEvent.timestamp();
        Double fuelLevel = fuelEvent.fuelLevelLitres();
        Double fuelRate = fuelEvent.fuelRateLitresPerMin();
        Double speedKmh = fuelEvent.speedKmh();
        Integer engineRpm = fuelEvent.engineRpm();
        Boolean ignition = fuelEvent.ignition();

        try {
            MDC.put("tenantId", tenantId != null ? tenantId.toString() : "unknown");
            MDC.put("correlationId", fuelEvent.correlationId() != null ? fuelEvent.correlationId() : UUID.randomUUID().toString());
            MDC.put("vehicleId", vehicleId != null ? vehicleId.toString() : "unknown");

            if (vehicleId == null || tenantId == null || timestamp == null) {
                log.warn("Received FuelEvent with missing required fields; skipping. vehicleId={}, tenantId={}, timestamp={}",
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

            // Persist fuel reading
            persistFuelReading(vehicleId, tenantId, deviceId, fuelLevel, fuelRate,
                    speedKmh, engineRpm, ignition, timestamp);

            // === Detection pipeline ===

            // 1. Sensor fault first
            fuelSensorFaultService.evaluate(vehicleId, tenantId, state, fuelLevel, timestamp);

            // 2. High consumption (uses direct fuel rate from sensor)
            fuelConsumptionService.evaluate(vehicleId, tenantId, state, fuelRate, timestamp);

            // 3. Theft
            fuelTheftDetectionService.evaluate(vehicleId, tenantId, state);

            // 4. Leak
            fuelLeakDetectionService.evaluate(vehicleId, tenantId, state);

            // 5. Idle burn (uses direct fuel rate from sensor)
            fuelIdleDetectionService.evaluate(vehicleId, tenantId, state, fuelRate, timestamp);

            // 6. Refuel
            refuelDetectionService.evaluate(vehicleId, tenantId, state, fuelLevel, timestamp);

            // Update state with latest values
            state.setLastFuelLevel(fuelLevel);
            state.setLastFuelRate(fuelRate);
            state.setLastSpeedKmh(speedKmh);
            state.setLastEngineRpm(engineRpm);
            state.setLastIgnition(ignition);
            state.setLastUpdate(timestamp);

            // Add reading to sliding window
            if (fuelLevel != null) {
                state.addReading(fuelLevel, timestamp);
            }

        } catch (Exception e) {
            log.error("Error processing fuel event for vehicle={}: {}", vehicleId, e.getMessage(), e);
        } finally {
            MDC.remove("tenantId");
            MDC.remove("correlationId");
            MDC.remove("vehicleId");
        }
    }

    private void persistFuelReading(UUID vehicleId, UUID tenantId, UUID deviceId,
                                     Double fuelLevel, Double fuelRate, Double speedKmh,
                                     Integer engineRpm, Boolean ignition,
                                     OffsetDateTime timestamp) {
        try {
            FuelReading reading = new FuelReading();
            reading.setVehicleId(vehicleId);
            reading.setTenantId(tenantId);
            reading.setRecordedAt(timestamp);
            reading.setFuelLevelLitres(fuelLevel != null ? BigDecimal.valueOf(fuelLevel) : null);
            reading.setFuelRateLitresPerMin(fuelRate != null ? BigDecimal.valueOf(fuelRate) : null);
            reading.setSpeedKmh(speedKmh != null ? BigDecimal.valueOf(speedKmh) : null);
            reading.setEngineRpm(engineRpm);
            reading.setIgnition(ignition);
            reading.setSource("fuel.events");

            fuelReadingRepository.save(reading);
        } catch (Exception e) {
            log.error("Failed to persist fuel reading from fuel event for vehicle={}: {}", vehicleId, e.getMessage(), e);
        }
    }
}
