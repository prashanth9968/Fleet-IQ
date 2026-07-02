package com.fleetiq.alerts.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.alerts.dto.UnifiedAlertEvent;
import com.fleetiq.alerts.service.AlertRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertListener {

    private final ObjectMapper objectMapper;
    private final AlertRoutingService alertRoutingService;

    // Listen to unified system alerts (e.g. Geofence Evaluation emits these)
    @KafkaListener(topics = "system.alerts", groupId = "fleetiq-alerts-group")
    public void onSystemAlert(String payload) {
        try {
            UnifiedAlertEvent alert = objectMapper.readValue(payload, UnifiedAlertEvent.class);
            alertRoutingService.processAlert(alert);
        } catch (Exception e) {
            log.error("Failed to route system.alert: {}", e.getMessage());
        }
    }

    // Listen to fuel alerts (legacy integration into Unified model)
    @KafkaListener(topics = {"fuel.alerts", "fuel.anomalies"}, groupId = "fleetiq-alerts-group")
    public void onFuelAlert(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            
            // Map legacy fuel anomalies to UnifiedAlertEvent
            String alertType = node.has("anomalyType") ? node.get("anomalyType").asText() : node.get("alertType").asText();
            UUID tenantId = UUID.fromString(node.get("tenantId").asText());
            UUID vehicleId = UUID.fromString(node.get("vehicleId").asText());
            
            String priority = "MEDIUM"; // Default
            if ("THEFT".equals(alertType) || "FUEL_SENSOR_FAULT".equals(alertType)) {
                priority = "CRITICAL";
            }
            
            String message = node.has("message") ? node.get("message").asText() : "Fuel anomaly detected";
            
            OffsetDateTime detectedAt = OffsetDateTime.parse(node.get("detectedAt").asText());
            
            UnifiedAlertEvent event = new UnifiedAlertEvent(
                    tenantId, vehicleId, "fleetiq-fuel-service", alertType, priority, message, detectedAt, new HashMap<>(), UUID.randomUUID().toString()
            );
            
            alertRoutingService.processAlert(event);
            
        } catch (Exception e) {
            log.error("Failed to map fuel alert to unified event: {}", e.getMessage());
        }
    }
}
