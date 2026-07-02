package com.fleetiq.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.gateway.client.AuthServiceClient;
import com.fleetiq.gateway.dto.NormalizedTelemetry;
import com.fleetiq.gateway.dto.TelemetryPayload;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class TelemetryProcessor {

    private static final Logger log = LoggerFactory.getLogger(TelemetryProcessor.class);

    private final AuthServiceClient authServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TELEMETRY_TOPIC = "raw.telemetry";
    private static final String DLQ_TOPIC = "raw.telemetry.dlq";

    public TelemetryProcessor(
            AuthServiceClient authServiceClient,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.authServiceClient = authServiceClient;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void processMessage(String rawJson) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("traceId", correlationId);
        
        try {
            TelemetryPayload payload;
            try {
                payload = objectMapper.readValue(rawJson, TelemetryPayload.class);
            } catch (Exception e) {
                log.warn("Payload is not valid JSON. Routing to DLQ. Error: {}", e.getMessage());
                sendToDlq(rawJson, correlationId, "MALFORMED_JSON", null);
                return;
            }

            // 1. Schema Validation
            if (isMalformed(payload)) {
                log.warn("Payload schema validation failed. Routing to DLQ. Payload: {}", payload);
                sendToDlq(rawJson, correlationId, "SCHEMA_VALIDATION_FAILED", null);
                return;
            }

            // 2. Authentication Verification
            AuthServiceClient.VerificationResult authResult = authServiceClient.verifyKey(payload.apiKey());
            if (!authResult.isValid()) {
                log.warn("Authentication failed for API key prefix: {}. Routing to DLQ.", 
                        payload.apiKey().length() > 12 ? payload.apiKey().substring(0, 12) : "invalid");
                sendToDlq(rawJson, correlationId, "AUTHENTICATION_FAILED", null);
                return;
            }

            UUID tenantId = authResult.tenantId();
            MDC.put("tenantId", tenantId.toString());

            // 3. Normalization (assuming incoming speed is kmh, fuel is litres)
            NormalizedTelemetry normalized = new NormalizedTelemetry(
                    tenantId,
                    payload.deviceId(),
                    payload.timestamp(),
                    payload.latitude(),
                    payload.longitude(),
                    payload.speed(),
                    payload.fuelLevel(),
                    payload.engineRpm(),
                    correlationId
            );

            String normalizedJson = objectMapper.writeValueAsString(normalized);

            // 4. Produce to raw.telemetry
            ProducerRecord<String, String> record = new ProducerRecord<>(TELEMETRY_TOPIC, payload.deviceId(), normalizedJson);
            record.headers().add("tenant-id", tenantId.toString().getBytes(StandardCharsets.UTF_8));
            record.headers().add("correlation-id", correlationId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("producer-id", "fleetiq-device-gateway-service".getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(record).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish telemetry to Kafka topic: {}", TELEMETRY_TOPIC, ex);
                } else {
                    log.debug("Successfully published telemetry to Kafka partition {} offset {}", 
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Unexpected error during telemetry processing", e);
            sendToDlq(rawJson, correlationId, "UNEXPECTED_ERROR", e.getMessage());
        } finally {
            MDC.remove("traceId");
            MDC.remove("tenantId");
        }
    }

    private boolean isMalformed(TelemetryPayload p) {
        return p.apiKey() == null || p.apiKey().trim().isEmpty()
                || p.deviceId() == null || p.deviceId().trim().isEmpty()
                || p.timestamp() == null
                || p.latitude() == null
                || p.longitude() == null;
    }

    private void sendToDlq(String rawPayload, String correlationId, String reason, String errorMessage) {
        try {
            DlqEnvelope envelope = new DlqEnvelope(rawPayload, reason, errorMessage, correlationId, System.currentTimeMillis());
            String dlqJson = objectMapper.writeValueAsString(envelope);
            
            ProducerRecord<String, String> record = new ProducerRecord<>(DLQ_TOPIC, correlationId, dlqJson);
            record.headers().add("correlation-id", correlationId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("producer-id", "fleetiq-device-gateway-service".getBytes(StandardCharsets.UTF_8));
            record.headers().add("error-reason", reason.getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(record);
            log.info("Successfully routed malformed payload to DLQ. Reason: {}", reason);
        } catch (Exception ex) {
            log.error("Failed to send payload to DLQ topic", ex);
        }
    }

    public record DlqEnvelope(
            String rawPayload,
            String reason,
            String errorMessage,
            String correlationId,
            long timestamp
    ) {}
}
