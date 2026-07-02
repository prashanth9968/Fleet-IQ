package com.fleetiq.alerts.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.alerts.dto.ProcessedTelemetry;
import com.fleetiq.alerts.service.GeofenceEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryListener {

    private final ObjectMapper objectMapper;
    private final GeofenceEvaluationService geofenceEvaluationService;

    @KafkaListener(topics = "processed.telemetry", groupId = "fleetiq-alerts-group")
    public void onProcessedTelemetry(String payload) {
        try {
            ProcessedTelemetry telemetry = objectMapper.readValue(payload, ProcessedTelemetry.class);
            if (telemetry.latitude() != null && telemetry.longitude() != null) {
                geofenceEvaluationService.evaluate(telemetry);
            }
        } catch (Exception e) {
            log.error("Failed to process telemetry for geofencing: {}", e.getMessage());
        }
    }
}
