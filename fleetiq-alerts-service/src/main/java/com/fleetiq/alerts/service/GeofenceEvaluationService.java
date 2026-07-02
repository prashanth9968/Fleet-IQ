package com.fleetiq.alerts.service;

import com.fleetiq.alerts.dto.ProcessedTelemetry;
import com.fleetiq.alerts.dto.UnifiedAlertEvent;
import com.fleetiq.alerts.entity.Geofence;
import com.fleetiq.alerts.entity.GeofenceEvent;
import com.fleetiq.alerts.repository.GeofenceEventRepository;
import com.fleetiq.alerts.state.VehicleGeofenceState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.locationtech.jts.geom.Coordinate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeofenceEvaluationService {

    private final GeofenceCacheService geofenceCacheService;
    private final GeofenceEventRepository geofenceEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Per-vehicle state cache
    private final Map<UUID, VehicleGeofenceState> vehicleStates = new ConcurrentHashMap<>();

    public void evaluate(ProcessedTelemetry telemetry) {
        UUID vehicleId = telemetry.vehicleId();
        UUID tenantId = telemetry.tenantId();

        VehicleGeofenceState state = vehicleStates.computeIfAbsent(vehicleId, id -> {
            VehicleGeofenceState s = new VehicleGeofenceState();
            s.setVehicleId(vehicleId);
            s.setTenantId(tenantId);
            return s;
        });

        List<Geofence> intersectingGeofences = geofenceCacheService.findIntersectingGeofences(
                tenantId, telemetry.latitude(), telemetry.longitude());

        Set<UUID> currentIntersectingIds = intersectingGeofences.stream()
                .map(Geofence::getId)
                .collect(Collectors.toSet());

        // 1. Detect EXIT events
        Set<UUID> exitedIds = state.getCurrentGeofences().stream()
                .filter(id -> !currentIntersectingIds.contains(id))
                .collect(Collectors.toSet());

        for (UUID exitedId : exitedIds) {
            handleExit(state, exitedId, telemetry);
        }

        // 2. Detect ENTER events
        Set<UUID> enteredIds = currentIntersectingIds.stream()
                .filter(id -> !state.isInside(id))
                .collect(Collectors.toSet());

        for (UUID enteredId : enteredIds) {
            handleEnter(state, enteredId, telemetry);
        }

        // 3. Evaluate continuous conditions (Dwell Time, Speed) inside active geofences
        for (Geofence geofence : intersectingGeofences) {
            evaluateConditions(state, geofence, telemetry);
        }

        // Update previous coordinate
        state.setPreviousCoordinate(new Coordinate(telemetry.longitude(), telemetry.latitude()));
    }

    private void handleEnter(VehicleGeofenceState state, UUID geofenceId, ProcessedTelemetry t) {
        state.enterGeofence(geofenceId, t.timestamp());
        
        GeofenceEvent event = new GeofenceEvent();
        event.setVehicleId(state.getVehicleId());
        event.setTenantId(state.getTenantId());
        event.setGeofenceId(geofenceId);
        event.setEventType("ENTER");
        event.setRecordedAt(t.timestamp());
        event.setSpeedKmh(t.speedKmh());
        event.setCreatedAt(OffsetDateTime.now());
        geofenceEventRepository.save(event);

        log.info("Vehicle {} ENTERED geofence {}", state.getVehicleId(), geofenceId);
        
        // Publish unified event
        publishUnifiedEvent(state.getTenantId(), state.getVehicleId(), "GEOFENCE_ENTER", "INFO",
                "Vehicle entered geofence", Map.of("geofenceId", geofenceId));
    }

    private void handleExit(VehicleGeofenceState state, UUID geofenceId, ProcessedTelemetry t) {
        OffsetDateTime entryTime = state.getEntryTimes().get(geofenceId);
        int dwellDurationSeconds = 0;
        if (entryTime != null) {
            dwellDurationSeconds = (int) Duration.between(entryTime, t.timestamp()).getSeconds();
        }

        state.exitGeofence(geofenceId, t.timestamp());

        GeofenceEvent event = new GeofenceEvent();
        event.setVehicleId(state.getVehicleId());
        event.setTenantId(state.getTenantId());
        event.setGeofenceId(geofenceId);
        event.setEventType("EXIT");
        event.setRecordedAt(t.timestamp());
        event.setSpeedKmh(t.speedKmh());
        event.setDwellDurationSeconds(dwellDurationSeconds);
        event.setCreatedAt(OffsetDateTime.now());
        geofenceEventRepository.save(event);

        log.info("Vehicle {} EXITED geofence {}. Dwell time: {}s", state.getVehicleId(), geofenceId, dwellDurationSeconds);

        publishUnifiedEvent(state.getTenantId(), state.getVehicleId(), "GEOFENCE_EXIT", "INFO",
                "Vehicle exited geofence", Map.of("geofenceId", geofenceId, "dwellSeconds", dwellDurationSeconds));
    }

    private void evaluateConditions(VehicleGeofenceState state, Geofence geofence, ProcessedTelemetry t) {
        // Evaluate Dwell Time Violation
        if (geofence.getMaxDwellMinutes() != null && geofence.getMaxDwellMinutes() > 0) {
            OffsetDateTime entryTime = state.getEntryTimes().get(geofence.getId());
            if (entryTime != null) {
                long dwellMinutes = Duration.between(entryTime, t.timestamp()).toMinutes();
                boolean alertFired = state.getDwellAlertFired().getOrDefault(geofence.getId(), false);

                if (dwellMinutes >= geofence.getMaxDwellMinutes() && !alertFired) {
                    state.getDwellAlertFired().put(geofence.getId(), true);
                    
                    GeofenceEvent event = new GeofenceEvent();
                    event.setVehicleId(state.getVehicleId());
                    event.setTenantId(state.getTenantId());
                    event.setGeofenceId(geofence.getId());
                    event.setEventType("DWELL_VIOLATION");
                    event.setRecordedAt(t.timestamp());
                    event.setDwellDurationSeconds((int) (dwellMinutes * 60));
                    geofenceEventRepository.save(event);

                    log.warn("Vehicle {} violated max dwell time in geofence {}", state.getVehicleId(), geofence.getId());
                    publishUnifiedEvent(state.getTenantId(), state.getVehicleId(), "EXCESSIVE_DWELL_TIME", "MEDIUM",
                            "Vehicle exceeded maximum dwell time", Map.of("geofenceId", geofence.getId(), "dwellMinutes", dwellMinutes));
                }
            }
        }

        // Evaluate Speed Limits within Geofence
        if (geofence.getMaxSpeedKmh() != null && t.speedKmh() != null && t.speedKmh() > geofence.getMaxSpeedKmh()) {
            GeofenceEvent event = new GeofenceEvent();
            event.setVehicleId(state.getVehicleId());
            event.setTenantId(state.getTenantId());
            event.setGeofenceId(geofence.getId());
            event.setEventType("OVERSPEED");
            event.setRecordedAt(t.timestamp());
            event.setSpeedKmh(t.speedKmh());
            geofenceEventRepository.save(event);

            log.warn("Vehicle {} overspeeding ({} km/h) in geofence {} (max {} km/h)", 
                    state.getVehicleId(), t.speedKmh(), geofence.getId(), geofence.getMaxSpeedKmh());
                    
            publishUnifiedEvent(state.getTenantId(), state.getVehicleId(), "OVERSPEED_IN_GEOFENCE", "HIGH",
                    "Vehicle exceeded speed limit inside geofence", 
                    Map.of("geofenceId", geofence.getId(), "speed", t.speedKmh(), "maxSpeed", geofence.getMaxSpeedKmh()));
        }
    }

    private void publishUnifiedEvent(UUID tenantId, UUID vehicleId, String alertType, String priority, String message, Map<String, Object> metadata) {
        UnifiedAlertEvent alert = new UnifiedAlertEvent(
                tenantId, vehicleId, "fleetiq-alerts-service", alertType, priority, message, OffsetDateTime.now(), metadata, UUID.randomUUID().toString()
        );
        // We publish geofence anomalies back to the unified alert queue so the AlertRoutingService can pick them up
        kafkaTemplate.send("system.alerts", alert);
    }
}
