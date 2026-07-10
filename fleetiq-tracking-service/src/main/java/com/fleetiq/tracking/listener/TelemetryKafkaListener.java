package com.fleetiq.tracking.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.tracking.dto.NormalizedTelemetry;
import com.fleetiq.tracking.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TelemetryKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(TelemetryKafkaListener.class);

    private final TrackingService trackingService;
    private final ObjectMapper objectMapper;

    public TelemetryKafkaListener(TrackingService trackingService, ObjectMapper objectMapper) {
        this.trackingService = trackingService;
        this.objectMapper = objectMapper;
    }

    // Redis Listener Pending
    public void listen(String message) {
        try {
            NormalizedTelemetry telemetry = objectMapper.readValue(message, NormalizedTelemetry.class);
            trackingService.enqueue(telemetry);
        } catch (Exception e) {
            log.error("Failed to deserialize telemetry message from raw.telemetry: {}", message, e);
            // In a production app, we would log or send the raw string directly to a dead-letter broker
        }
    }
}
