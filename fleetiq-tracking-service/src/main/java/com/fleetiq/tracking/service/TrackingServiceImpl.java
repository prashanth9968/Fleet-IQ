package com.fleetiq.tracking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.fleetiq.tracking.dto.AssignmentCacheEntry;
import com.fleetiq.tracking.dto.NormalizedTelemetry;
import com.fleetiq.tracking.dto.ProcessedTelemetry;
import com.fleetiq.tracking.entity.Device;
import com.fleetiq.tracking.entity.DeviceVehicleAssignment;
import com.fleetiq.tracking.repository.DeviceRepository;
import com.fleetiq.tracking.repository.DeviceVehicleAssignmentRepository;
import com.fleetiq.tracking.repository.GpsReadingRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class TrackingServiceImpl implements TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingServiceImpl.class);

    private final GpsReadingRepository gpsReadingRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceVehicleAssignmentRepository deviceVehicleAssignmentRepository;
    private final MetricsService metricsService;
    private final Cache<String, AssignmentCacheEntry> assignmentCache;
    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    private final BlockingQueue<NormalizedTelemetry> queue = new LinkedBlockingQueue<>();

    private static final String PROCESSED_TOPIC = "processed.telemetry";
    private static final String DLQ_TOPIC = "raw.telemetry.dlq";

    public TrackingServiceImpl(
            GpsReadingRepository gpsReadingRepository,
            DeviceRepository deviceRepository,
            DeviceVehicleAssignmentRepository deviceVehicleAssignmentRepository,
            MetricsService metricsService,
            Cache<String, AssignmentCacheEntry> assignmentCache,
            StringRedisTemplate redisTemplate,
            SimpMessagingTemplate simpMessagingTemplate,
            ObjectMapper objectMapper
    ) {
        this.gpsReadingRepository = gpsReadingRepository;
        this.deviceRepository = deviceRepository;
        this.deviceVehicleAssignmentRepository = deviceVehicleAssignmentRepository;
        this.metricsService = metricsService;
        this.assignmentCache = assignmentCache;
        this.redisTemplate = redisTemplate;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enqueue(NormalizedTelemetry telemetry) {
        if (telemetry != null) {
            metricsService.incrementReceived();
            queue.offer(telemetry);
        }
    }

    @Override
    public void clearQueue() {
        queue.clear();
    }

    @Scheduled(fixedDelay = 500)
    public void flushBuffer() {
        if (queue.isEmpty()) {
            return;
        }

        List<NormalizedTelemetry> batch = new ArrayList<>();
        queue.drainTo(batch, 500);

        if (!batch.isEmpty()) {
            processBatch(batch);
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void processBatch(List<NormalizedTelemetry> batch) {
        long startTime = System.nanoTime();
        int processedCount = 0;

        for (NormalizedTelemetry telemetry : batch) {
            String correlationId = telemetry.correlationId() != null ? telemetry.correlationId() : UUID.randomUUID().toString();
            MDC.put("traceId", correlationId);
            if (telemetry.tenantId() != null) {
                MDC.put("tenantId", telemetry.tenantId().toString());
            }

            try {
                // 1. Validation Checks
                if (isInvalid(telemetry)) {
                    log.warn("Telemetry validation failed. Routing to DLQ. Payload: {}", telemetry);
                    sendToDlq(telemetry, correlationId, "VALIDATION_FAILED", "Invalid coordinates or future timestamp");
                    metricsService.incrementFailed();
                    continue;
                }

                // 2. Resolve Active Assignment (with Caffeine Cache)
                AssignmentCacheEntry assignment = assignmentCache.get(telemetry.deviceId(), serialNumber -> {
                    Device device = deviceRepository.findBySerialNumber(serialNumber).orElse(null);
                    if (device != null) {
                        DeviceVehicleAssignment activeAssign = deviceVehicleAssignmentRepository
                                .findByDeviceIdAndUnassignedAtIsNull(device.getId()).orElse(null);
                        if (activeAssign != null) {
                            return new AssignmentCacheEntry(
                                    activeAssign.getVehicle().getTenant().getId(),
                                    activeAssign.getVehicle().getId(),
                                    device.getId()
                            );
                        }
                    }
                    return null;
                });

                if (assignment == null) {
                    log.warn("No active vehicle assignment found for device serial: {}. Routing to DLQ.", telemetry.deviceId());
                    sendToDlq(telemetry, correlationId, "MISSING_ASSIGNMENT", "No active vehicle assigned to device serial " + telemetry.deviceId());
                    metricsService.incrementFailed();
                    continue;
                }

                UUID vehicleId = assignment.vehicleId();
                UUID deviceUuid = assignment.deviceId();
                UUID tenantId = assignment.tenantId();
                MDC.put("tenantId", tenantId.toString());

                // 3. Database batch persist (with Idempotency ON CONFLICT DO NOTHING)
                int rows = gpsReadingRepository.insertIgnoreConflict(
                        vehicleId,
                        telemetry.timestamp(),
                        tenantId,
                        deviceUuid,
                        BigDecimal.valueOf(telemetry.latitude()),
                        BigDecimal.valueOf(telemetry.longitude()),
                        BigDecimal.valueOf(0.00), // altitude default
                        BigDecimal.valueOf(telemetry.speedKmh()),
                        BigDecimal.valueOf(0.00), // heading default
                        BigDecimal.valueOf(1.00), // hdop default
                        4, // satellites default
                        true, // ignition default
                        BigDecimal.valueOf(0.00), // odometer default
                        5, // signal_strength default
                        false, // is_buffered
                        "{}" // metadata
                );

                if (rows == 0) {
                    log.debug("Duplicate GPS point ignored for vehicle: {} at recorded_at: {}", vehicleId, telemetry.timestamp());
                }

                // 4. Publish to processed.telemetry Kafka topic
                ProcessedTelemetry processed = new ProcessedTelemetry(
                        tenantId,
                        vehicleId,
                        deviceUuid,
                        telemetry.timestamp(),
                        telemetry.latitude(),
                        telemetry.longitude(),
                        telemetry.speedKmh(),
                        telemetry.fuelLevelLitres(),
                        telemetry.engineRpm(),
                        correlationId
                );

                String processedJson = objectMapper.writeValueAsString(processed);

                ProducerRecord<String, String> record = new ProducerRecord<>(PROCESSED_TOPIC, vehicleId.toString(), processedJson);
                record.headers().add("tenant-id", tenantId.toString().getBytes(StandardCharsets.UTF_8));
                record.headers().add("correlation-id", correlationId.getBytes(StandardCharsets.UTF_8));
                record.headers().add("producer-id", "fleetiq-tracking-service".getBytes(StandardCharsets.UTF_8));

                redisTemplate.convertAndSend(record);

                // 5. Broadcast live update to WS STOMP topics
                simpMessagingTemplate.convertAndSend("/topic/vehicle/" + vehicleId, processed);
                simpMessagingTemplate.convertAndSend("/topic/fleet/" + tenantId, processed);

                processedCount++;

            } catch (Exception e) {
                log.error("Failed to process telemetry event", e);
                sendToDlq(telemetry, correlationId, "PROCESSING_ERROR", e.getMessage());
                metricsService.incrementFailed();
            } finally {
                MDC.remove("traceId");
                MDC.remove("tenantId");
            }
        }

        if (processedCount > 0) {
            metricsService.incrementProcessed(processedCount);
        }
        metricsService.getGpsWriteTimer().record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    }

    private boolean isInvalid(NormalizedTelemetry t) {
        if (t.latitude() == null || t.latitude() < -90.0 || t.latitude() > 90.0) {
            return true;
        }
        if (t.longitude() == null || t.longitude() < -180.0 || t.longitude() > 180.0) {
            return true;
        }
        if (t.timestamp() == null) {
            return true;
        }
        // Check future timestamp limit (e.g. 5 minutes in future)
        return t.timestamp().isAfter(OffsetDateTime.now().plusMinutes(5));
    }

    private void sendToDlq(NormalizedTelemetry telemetry, String correlationId, String reason, String errorMessage) {
        try {
            DlqEnvelope envelope = new DlqEnvelope(telemetry, reason, errorMessage, correlationId, System.currentTimeMillis());
            String dlqJson = objectMapper.writeValueAsString(envelope);
            
            ProducerRecord<String, String> record = new ProducerRecord<>(DLQ_TOPIC, correlationId, dlqJson);
            record.headers().add("correlation-id", correlationId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("producer-id", "fleetiq-tracking-service".getBytes(StandardCharsets.UTF_8));
            record.headers().add("error-reason", reason.getBytes(StandardCharsets.UTF_8));

            redisTemplate.convertAndSend(record);
        } catch (Exception e) {
            log.error("Failed to publish telemetry error message to DLQ", e);
        }
    }

    public record DlqEnvelope(
            NormalizedTelemetry telemetry,
            String reason,
            String errorMessage,
            String correlationId,
            long timestamp
    ) {}
}
