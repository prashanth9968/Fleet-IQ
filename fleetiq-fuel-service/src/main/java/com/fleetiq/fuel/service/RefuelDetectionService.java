package com.fleetiq.fuel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.fuel.dto.RefuelEventDto;
import com.fleetiq.fuel.entity.FuelRefuelEvent;
import com.fleetiq.fuel.repository.FuelRefuelEventRepository;
import com.fleetiq.fuel.state.VehicleFuelState;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Refuel detection service. Identifies fuel fill events by detecting sudden
 * increases in fuel level exceeding 5 litres within a 10-minute window.
 * Persists a {@link FuelRefuelEvent} entity and publishes a
 * {@link RefuelEventDto} to the {@code fuel.refuels} Kafka topic.
 */
@Service
@Slf4j
public class RefuelDetectionService {

    private static final String REFUEL_TOPIC = "fuel.refuels";
    private static final double REFUEL_THRESHOLD_LITRES = 5.0;
    private static final long REFUEL_WINDOW_MINUTES = 10;

    private final FuelRefuelEventRepository fuelRefuelEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public RefuelDetectionService(FuelRefuelEventRepository fuelRefuelEventRepository,
                                  KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.fuelRefuelEventRepository = fuelRefuelEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void evaluate(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                         Double currentFuelLevel, OffsetDateTime timestamp) {
        if (currentFuelLevel == null) {
            return;
        }

        try {
            MDC.put("tenantId", tenantId.toString());
            MDC.put("vehicleId", vehicleId.toString());

            if (state.getLastFuelLevel() == null) {
                log.debug("No previous fuel level for vehicle={}; skipping refuel detection", vehicleId);
                return;
            }

            double increase = currentFuelLevel - state.getLastFuelLevel();

            if (increase <= REFUEL_THRESHOLD_LITRES) {
                return;
            }

            // Check time window: only flag if last update was within 10 minutes
            if (state.getLastUpdate() != null) {
                Duration sinceLast = Duration.between(state.getLastUpdate(), timestamp);
                if (sinceLast.toMinutes() > REFUEL_WINDOW_MINUTES) {
                    log.debug("Fuel increase of {} L for vehicle={} spans {} minutes; outside refuel window",
                            String.format("%.2f", increase), vehicleId, sinceLast.toMinutes());
                    return;
                }
            }

            log.info("Refuel detected for vehicle={}: increase={} L (before={} L, after={} L)",
                    vehicleId, String.format("%.2f", increase),
                    String.format("%.2f", state.getLastFuelLevel()),
                    String.format("%.2f", currentFuelLevel));

            FuelRefuelEvent event = buildRefuelEvent(vehicleId, tenantId, state,
                    currentFuelLevel, increase, timestamp);
            fuelRefuelEventRepository.save(event);

            publishRefuel(event, vehicleId, tenantId, state, currentFuelLevel, increase, timestamp);
        } catch (Exception e) {
            log.error("Error evaluating refuel for vehicle={}: {}", vehicleId, e.getMessage(), e);
        } finally {
            MDC.remove("tenantId");
            MDC.remove("vehicleId");
        }
    }

    private FuelRefuelEvent buildRefuelEvent(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                                              double currentFuelLevel, double increase,
                                              OffsetDateTime timestamp) {
        FuelRefuelEvent event = new FuelRefuelEvent();
        event.setVehicleId(vehicleId);
        event.setTenantId(tenantId);
        event.setRefueledAt(timestamp);
        event.setFuelBeforeLitres(BigDecimal.valueOf(state.getLastFuelLevel()));
        event.setFuelAfterLitres(BigDecimal.valueOf(currentFuelLevel));
        event.setFuelAddedLitres(BigDecimal.valueOf(increase));
        event.setLocationLat(state.getLastLatitude() != null
                ? BigDecimal.valueOf(state.getLastLatitude()) : null);
        event.setLocationLng(state.getLastLongitude() != null
                ? BigDecimal.valueOf(state.getLastLongitude()) : null);
        event.setOdometerKm(state.getLastOdometerKm() != null
                ? BigDecimal.valueOf(state.getLastOdometerKm()) : null);
        event.setSource("AUTO_DETECTED");
        event.setCreatedAt(OffsetDateTime.now());
        return event;
    }

    private void publishRefuel(FuelRefuelEvent event, UUID vehicleId, UUID tenantId,
                                VehicleFuelState state, double currentFuelLevel,
                                double increase, OffsetDateTime timestamp) {
        try {
            String correlationId = UUID.randomUUID().toString();
            RefuelEventDto dto = new RefuelEventDto(
                    tenantId,
                    vehicleId,
                    timestamp,
                    state.getLastFuelLevel(),
                    currentFuelLevel,
                    increase,
                    state.getLastLatitude(),
                    state.getLastLongitude(),
                    state.getLastOdometerKm(),
                    correlationId
            );

            String json = objectMapper.writeValueAsString(dto);
            ProducerRecord<String, String> record = new ProducerRecord<>(REFUEL_TOPIC, vehicleId.toString(), json);
            record.headers().add("tenant-id", tenantId.toString().getBytes(StandardCharsets.UTF_8));
            record.headers().add("event-type", "REFUEL".getBytes(StandardCharsets.UTF_8));
            record.headers().add("correlation-id", correlationId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("producer-id", "fleetiq-fuel-service".getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(record);
            log.debug("Published refuel event to {} for vehicle={}, volume={} L",
                    REFUEL_TOPIC, vehicleId, String.format("%.2f", increase));
        } catch (Exception e) {
            log.error("Failed to publish refuel event for vehicle={}: {}", vehicleId, e.getMessage(), e);
        }
    }
}
