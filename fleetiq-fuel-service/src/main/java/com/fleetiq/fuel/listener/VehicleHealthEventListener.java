package com.fleetiq.fuel.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.fuel.dto.VehicleHealthEvent;
import com.fleetiq.fuel.state.VehicleFuelState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka consumer for vehicle health events. Enriches the per-vehicle
 * {@link VehicleFuelState} with engine RPM, coolant temperature, and ignition
 * state from OBD/CAN bus telemetry. This supplementary data enables the idle
 * burn and sensor fault detection services to make more informed decisions.
 */
@Component
@Slf4j
public class VehicleHealthEventListener {

    private final ObjectMapper objectMapper;

    /**
     * Shared state map — injected or referenced from the same application context.
     * In production, this would be a shared bean or state store. For this service,
     * each listener maintains its own map and the services operate on the state
     * passed to them. In a single-instance deployment, you would wire a shared
     * ConcurrentHashMap bean.
     */
    private final ConcurrentHashMap<UUID, VehicleFuelState> vehicleStates = new ConcurrentHashMap<>();

    public VehicleHealthEventListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Provides access to the vehicle states map so it can be shared with other
     * listeners or wired through configuration.
     */
    public ConcurrentHashMap<UUID, VehicleFuelState> getVehicleStates() {
        return vehicleStates;
    }

    // Redis Listener Pending
    public void listen(String message) {
        VehicleHealthEvent healthEvent = null;

        try {
            healthEvent = objectMapper.readValue(message, VehicleHealthEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize VehicleHealthEvent message: {}", message, e);
            return;
        }

        UUID vehicleId = healthEvent.vehicleId();
        UUID tenantId = healthEvent.tenantId();

        try {
            MDC.put("tenantId", tenantId != null ? tenantId.toString() : "unknown");
            MDC.put("vehicleId", vehicleId != null ? vehicleId.toString() : "unknown");

            if (vehicleId == null || tenantId == null) {
                log.warn("Received VehicleHealthEvent with missing required fields; skipping. vehicleId={}, tenantId={}",
                        vehicleId, tenantId);
                return;
            }

            VehicleFuelState state = vehicleStates.computeIfAbsent(vehicleId, id -> {
                VehicleFuelState newState = new VehicleFuelState();
                newState.setVehicleId(id);
                newState.setTenantId(tenantId);
                return newState;
            });

            // Update engine RPM
            if (healthEvent.engineRpm() != null) {
                state.setLastEngineRpm(healthEvent.engineRpm());
                log.trace("Updated engine RPM for vehicle={}: {}", vehicleId, healthEvent.engineRpm());
            }

            // Update ignition state
            if (healthEvent.ignition() != null) {
                state.setLastIgnition(healthEvent.ignition());
                log.trace("Updated ignition state for vehicle={}: {}", vehicleId, healthEvent.ignition());
            }

            // Update speed if provided
            if (healthEvent.speedKmh() != null) {
                state.setLastSpeedKmh(healthEvent.speedKmh());
            }

            // Update timestamp
            if (healthEvent.timestamp() != null) {
                state.setLastUpdate(healthEvent.timestamp());
            }

            log.debug("Vehicle health state updated for vehicle={}: rpm={}, ignition={}, speed={}",
                    vehicleId, healthEvent.engineRpm(), healthEvent.ignition(), healthEvent.speedKmh());

        } catch (Exception e) {
            log.error("Error processing vehicle health event for vehicle={}: {}", vehicleId, e.getMessage(), e);
        } finally {
            MDC.remove("tenantId");
            MDC.remove("vehicleId");
        }
    }
}
