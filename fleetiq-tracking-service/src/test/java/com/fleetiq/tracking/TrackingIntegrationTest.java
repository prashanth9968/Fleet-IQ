package com.fleetiq.tracking;

import com.github.benmanes.caffeine.cache.Cache;
import com.fleetiq.tracking.dto.AssignmentCacheEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.tracking.dto.NormalizedTelemetry;
import com.fleetiq.tracking.dto.ProcessedTelemetry;
import com.fleetiq.tracking.entity.*;
import com.fleetiq.tracking.repository.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.fleetiq.tracking.service.TrackingServiceImpl;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class TrackingIntegrationTest extends AbstractIntegrationTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrackingIntegrationTest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GpsReadingRepository gpsReadingRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DeviceVehicleAssignmentRepository deviceVehicleAssignmentRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleTypeRepository vehicleTypeRepository;

    @Autowired
    private com.fleetiq.tracking.service.TrackingService trackingService;

    @Autowired
    private Cache<String, AssignmentCacheEntry> assignmentCache;

    private Tenant testTenant;
    private User testUser;
    private VehicleType testVehicleType;
    private Vehicle testVehicle;
    private Device testDevice;
    private KafkaConsumer<String, String> processedConsumer;
    private KafkaConsumer<String, String> dlqConsumer;

    @BeforeEach
    public void setUp() {
        // 1. Clear in-memory queue and invalidate assignment cache before each test
        trackingService.clearQueue();
        assignmentCache.invalidateAll();

        deviceVehicleAssignmentRepository.deleteAll();
        gpsReadingRepository.deleteAll();
        deviceRepository.deleteAll();
        vehicleRepository.deleteAll();
        vehicleTypeRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // 1. Create Tenant
        testTenant = new Tenant();
        testTenant.setName("Tracking Demo Corp");
        testTenant.setSlug("tracking-demo");
        testTenant.setStatus("ACTIVE");
        testTenant = tenantRepository.save(testTenant);

        // 2. Create User
        testUser = new User();
        testUser.setTenant(testTenant);
        testUser.setEmail("tester@trackingdemo.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setStatus("ACTIVE");
        testUser = userRepository.save(testUser);

        // 3. Create Vehicle Type
        testVehicleType = new VehicleType();
        testVehicleType.setName("Delivery Van");
        testVehicleType.setCategory("SEDAN");
        testVehicleType.setFuelType("PETROL");
        testVehicleType = vehicleTypeRepository.save(testVehicleType);

        // 4. Create Vehicle
        testVehicle = new Vehicle();
        testVehicle.setTenant(testTenant);
        testVehicle.setVehicleType(testVehicleType);
        testVehicle.setRegistrationNumber("TS-09-TR-1234");
        testVehicle.setStatus("ACTIVE");
        testVehicle = vehicleRepository.save(testVehicle);

        // 5. Create Device
        testDevice = new Device();
        testDevice.setTenant(testTenant);
        testDevice.setSerialNumber("DEV-123");
        testDevice.setDeviceType("GPS_TRACKER");
        testDevice.setStatus("ACTIVE");
        testDevice = deviceRepository.save(testDevice);

        // 6. Active Device Assignment
        DeviceVehicleAssignment assignment = new DeviceVehicleAssignment();
        assignment.setVehicle(testVehicle);
        assignment.setDevice(testDevice);
        assignment.setAssignedBy(testUser);
        assignment.setIsPrimary(true);
        deviceVehicleAssignmentRepository.save(assignment);

        // 7. Wait for any in-flight flushBuffer cycle to finish and settle,
        //    then create consumers positioned at the END of each topic so we
        //    only receive messages produced during THIS test.
        try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        processedConsumer = createConsumer("processed.telemetry");
        dlqConsumer = createConsumer("raw.telemetry.dlq");
    }

    @AfterEach
    public void tearDown() {
        if (processedConsumer != null) {
            processedConsumer.close();
        }
        if (dlqConsumer != null) {
            dlqConsumer.close();
        }
    }

    private KafkaConsumer<String, String> createConsumer(String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + topic + "-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        // Wait until partition assignment is fully established
        Set<org.apache.kafka.common.TopicPartition> assignment = new java.util.HashSet<>();
        for (int attempt = 0; attempt < 20 && assignment.isEmpty(); attempt++) {
            consumer.poll(Duration.ofMillis(100));
            assignment = consumer.assignment();
        }

        if (!assignment.isEmpty()) {
            // Synchronously fetch the current end offsets, then immediately seek to them.
            // Unlike seekToEnd() which is LAZY (resolves at next fetch), endOffsets() is
            // synchronous and seek() anchors the position immediately — no race condition.
            Map<org.apache.kafka.common.TopicPartition, Long> endOffsets = consumer.endOffsets(assignment);
            for (Map.Entry<org.apache.kafka.common.TopicPartition, Long> entry : endOffsets.entrySet()) {
                consumer.seek(entry.getKey(), entry.getValue());
            }
        }

        return consumer;
    }


    private BlockingQueue<ProcessedTelemetry> subscribeToWebSocketTopic(UUID vehicleId) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        // CRITICAL: use Spring's ObjectMapper (has JavaTimeModule) — the default one
        // cannot deserialize OffsetDateTime, causing silent handleFrame failures
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);

        String wsUrl = "ws://localhost:" + port + "/ws";
        StompSession session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {
            @Override
            public void handleException(StompSession session, StompCommand command,
                                        StompHeaders headers, byte[] payload, Throwable exception) {
                log.error("STOMP session exception: cmd={}", command, exception);
            }
            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                log.error("STOMP transport error", exception);
            }
        }).get(5, TimeUnit.SECONDS);

        BlockingQueue<ProcessedTelemetry> frames = new LinkedBlockingQueue<>();
        session.subscribe("/topic/vehicle/" + vehicleId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ProcessedTelemetry.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    frames.offer((ProcessedTelemetry) payload);
                } catch (Exception e) {
                    log.error("Error handling WebSocket STOMP frame", e);
                }
            }
        });
        // Allow the subscription to be fully registered on the broker before returning
        Thread.sleep(200);
        return frames;
    }

    @Test
    public void testTelemetryPipelineAndWebSocketBroadcast() throws Exception {
        // Step 1: Subscribe to STOMP WebSocket channel
        BlockingQueue<ProcessedTelemetry> wsQueue = subscribeToWebSocketTopic(testVehicle.getId());

        // Step 2: Publish normalized telemetry to raw.telemetry Kafka topic
        OffsetDateTime timestamp = OffsetDateTime.now();
        NormalizedTelemetry telemetry = new NormalizedTelemetry(
                testTenant.getId(),
                "DEV-123",
                timestamp,
                17.385,
                78.486,
                62.5,
                80.0,
                2200,
                UUID.randomUUID().toString()
        );

        String jsonPayload = objectMapper.writeValueAsString(telemetry);
        kafkaTemplate.send("raw.telemetry", "DEV-123", jsonPayload).get();

        // Step 3: Wait and Verify Database Persist (using polling due to scheduled batch flush)
        boolean persisted = false;
        for (int i = 0; i < 20; i++) {
            Thread.sleep(250);
            if (gpsReadingRepository.count() > 0) {
                persisted = true;
                break;
            }
        }
        assertThat(persisted).isTrue();

        List<GpsReading> dbReadings = gpsReadingRepository.findAllByVehicleIdAndRecordedAtBetween(
                testVehicle.getId(), timestamp.minusMinutes(1), timestamp.plusMinutes(1));
        assertThat(dbReadings).hasSize(1);
        assertThat(dbReadings.getFirst().getLatitude()).isCloseTo(BigDecimal.valueOf(17.385), org.assertj.core.data.Offset.offset(BigDecimal.valueOf(0.0001)));

        // Step 4: Verify Kafka processed.telemetry topic message and headers
        ConsumerRecords<String, String> processedRecords = processedConsumer.poll(Duration.ofSeconds(10));
        assertThat(processedRecords.count()).isGreaterThanOrEqualTo(1);

        ConsumerRecord<String, String> record = processedRecords.iterator().next();
        ProcessedTelemetry outTelemetry = objectMapper.readValue(record.value(), ProcessedTelemetry.class);
        assertThat(outTelemetry.vehicleId()).isEqualTo(testVehicle.getId());
        assertThat(outTelemetry.deviceId()).isEqualTo(testDevice.getId());
        assertThat(outTelemetry.speedKmh()).isEqualTo(62.5);

        // Verify headers
        assertThat(record.headers().lastHeader("tenant-id")).isNotNull();
        assertThat(new String(record.headers().lastHeader("tenant-id").value(), StandardCharsets.UTF_8)).isEqualTo(testTenant.getId().toString());
        assertThat(record.headers().lastHeader("producer-id")).isNotNull();

        // Step 5: Verify WebSocket STOMP frame broadcast
        ProcessedTelemetry wsFrame = wsQueue.poll(15, TimeUnit.SECONDS);
        if (wsFrame == null) {
            log.error("WebSocket STOMP frame not received within 15s. Queue size: {}", wsQueue.size());
        }
        assertThat(wsFrame).isNotNull();
        assertThat(wsFrame.vehicleId()).isEqualTo(testVehicle.getId());
        assertThat(wsFrame.speedKmh()).isEqualTo(62.5);
    }

    @Test
    public void testDeduplicationIgnoreConflicts() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        NormalizedTelemetry telemetry = new NormalizedTelemetry(
                testTenant.getId(),
                "DEV-123",
                timestamp,
                17.385,
                78.486,
                62.5,
                80.0,
                2200,
                UUID.randomUUID().toString()
        );

        String jsonPayload = objectMapper.writeValueAsString(telemetry);
        
        // Publish exactly same message twice
        kafkaTemplate.send("raw.telemetry", "DEV-123", jsonPayload).get();
        kafkaTemplate.send("raw.telemetry", "DEV-123", jsonPayload).get();

        // Wait for buffer flush
        Thread.sleep(2000);

        // Database should only have exactly 1 record due to primary key constraint and ON CONFLICT DO NOTHING
        assertThat(gpsReadingRepository.count()).isEqualTo(1);
    }

    @Test
    public void testMissingAssignmentRoutesToDlq() throws Exception {
        NormalizedTelemetry telemetry = new NormalizedTelemetry(
                testTenant.getId(),
                "DEV-UNASSIGNED", // Serial number not registered/assigned
                OffsetDateTime.now(),
                17.385,
                78.486,
                62.5,
                80.0,
                2200,
                UUID.randomUUID().toString()
        );

        String jsonPayload = objectMapper.writeValueAsString(telemetry);
        kafkaTemplate.send("raw.telemetry", "DEV-UNASSIGNED", jsonPayload).get();

        // Wait for database check
        Thread.sleep(2000);

        // Verify no write in database
        assertThat(gpsReadingRepository.count()).isEqualTo(0);

        // Verify message goes to DLQ topic
        ConsumerRecords<String, String> dlqRecords = dlqConsumer.poll(Duration.ofSeconds(10));
        assertThat(dlqRecords.count()).isGreaterThanOrEqualTo(1);

        ConsumerRecord<String, String> record = dlqRecords.iterator().next();
        TrackingServiceImpl.DlqEnvelope envelope = objectMapper.readValue(record.value(), TrackingServiceImpl.DlqEnvelope.class);
        
        assertThat(envelope.reason()).isEqualTo("MISSING_ASSIGNMENT");
        assertThat(envelope.telemetry().deviceId()).isEqualTo("DEV-UNASSIGNED");
    }

    @Test
    public void testValidationFailureRoutesToDlq() throws Exception {
        NormalizedTelemetry telemetry = new NormalizedTelemetry(
                testTenant.getId(),
                "DEV-123",
                OffsetDateTime.now().plusHours(12), // Future timestamp
                17.385,
                195.0, // Invalid longitude (> 180)
                62.5,
                80.0,
                2200,
                UUID.randomUUID().toString()
        );

        String jsonPayload = objectMapper.writeValueAsString(telemetry);
        kafkaTemplate.send("raw.telemetry", "DEV-123", jsonPayload).get();

        // Wait for database check
        Thread.sleep(2000);

        // Verify no write in database
        assertThat(gpsReadingRepository.count()).isEqualTo(0);

        // Verify message in DLQ
        ConsumerRecords<String, String> dlqRecords = dlqConsumer.poll(Duration.ofSeconds(10));
        assertThat(dlqRecords.count()).isGreaterThanOrEqualTo(1);

        ConsumerRecord<String, String> record = dlqRecords.iterator().next();
        TrackingServiceImpl.DlqEnvelope envelope = objectMapper.readValue(record.value(), TrackingServiceImpl.DlqEnvelope.class);
        
        assertThat(envelope.reason()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    public void testHighThroughputBatching1000Events() throws Exception {
        int eventCount = 1000;
        OffsetDateTime baseTime = OffsetDateTime.now();

        log.info("Starting high throughput test: sending {} events...", eventCount);
        for (int i = 0; i < eventCount; i++) {
            NormalizedTelemetry telemetry = new NormalizedTelemetry(
                    testTenant.getId(),
                    "DEV-123",
                    baseTime.minusSeconds(i),
                    17.0 + (i * 0.0001),
                    78.0 + (i * 0.0001),
                    50.0,
                    90.0,
                    2000,
                    UUID.randomUUID().toString()
            );
            String jsonPayload = objectMapper.writeValueAsString(telemetry);
            kafkaTemplate.send("raw.telemetry", "DEV-123", jsonPayload);
        }

        // Wait for asynchronous processing queues to drain (max 30 seconds)
        boolean allPersisted = false;
        for (int attempt = 0; attempt < 60; attempt++) {
            Thread.sleep(500);
            long count = gpsReadingRepository.count();
            log.info("High throughput progress: {}/{} events persisted", count, eventCount);
            if (count == eventCount) {
                allPersisted = true;
                break;
            }
        }
        assertThat(allPersisted).isTrue();
        log.info("High throughput test finished successfully. All {} events persisted.", eventCount);
    }

    @Test
    public void testMultiTenantIsolation() throws Exception {
        // ── Tenant B setup (Tenant A already set up in @BeforeEach) ─────────────
        Tenant tenantB = new Tenant();
        tenantB.setName("Tenant B Corp");
        tenantB.setSlug("tenant-b");
        tenantB.setStatus("ACTIVE");
        tenantB = tenantRepository.save(tenantB);

        User userB = new User();
        userB.setTenant(tenantB);
        userB.setEmail("admin@tenantb.com");
        userB.setFirstName("Jane");
        userB.setLastName("Smith");
        userB.setStatus("ACTIVE");
        userB = userRepository.save(userB);

        VehicleType vtB = new VehicleType();
        vtB.setName("Sedan B");
        vtB.setCategory("SEDAN");
        vtB.setFuelType("DIESEL");
        vtB = vehicleTypeRepository.save(vtB);

        Vehicle vehicleB = new Vehicle();
        vehicleB.setTenant(tenantB);
        vehicleB.setVehicleType(vtB);
        vehicleB.setRegistrationNumber("MH-01-TB-9999");
        vehicleB.setStatus("ACTIVE");
        vehicleB = vehicleRepository.save(vehicleB);

        Device deviceB = new Device();
        deviceB.setTenant(tenantB);
        deviceB.setSerialNumber("DEV-TENANT-B");
        deviceB.setDeviceType("GPS_TRACKER");
        deviceB.setStatus("ACTIVE");
        deviceB = deviceRepository.save(deviceB);

        DeviceVehicleAssignment assignB = new DeviceVehicleAssignment();
        assignB.setVehicle(vehicleB);
        assignB.setDevice(deviceB);
        assignB.setAssignedBy(userB);
        assignB.setIsPrimary(true);
        deviceVehicleAssignmentRepository.save(assignB);

        // ── Subscribe BOTH tenant WebSocket channels ─────────────────────────
        BlockingQueue<ProcessedTelemetry> queueA = subscribeToWebSocketTopic(testVehicle.getId());
        BlockingQueue<ProcessedTelemetry> queueB = subscribeToWebSocketTopic(vehicleB.getId());

        // ── Send telemetry ONLY for Tenant A's vehicle ───────────────────────
        NormalizedTelemetry telemetryA = new NormalizedTelemetry(
                testTenant.getId(),
                "DEV-123",            // Tenant A's device
                OffsetDateTime.now(),
                17.385, 78.486,
                60.0, 75.0, 2000,
                UUID.randomUUID().toString()
        );
        kafkaTemplate.send("raw.telemetry", "DEV-123",
                objectMapper.writeValueAsString(telemetryA)).get();

        // ── Wait for processing ───────────────────────────────────────────────
        boolean persisted = false;
        for (int i = 0; i < 20; i++) {
            Thread.sleep(250);
            if (gpsReadingRepository.count() > 0) { persisted = true; break; }
        }
        assertThat(persisted).isTrue();

        // ── Assert: Tenant A WebSocket receives the frame ─────────────────────
        ProcessedTelemetry frameA = queueA.poll(10, TimeUnit.SECONDS);
        assertThat(frameA).isNotNull();
        assertThat(frameA.vehicleId()).isEqualTo(testVehicle.getId());
        assertThat(frameA.tenantId()).isEqualTo(testTenant.getId());

        // ── Assert: Tenant B WebSocket receives NOTHING (isolation) ───────────
        ProcessedTelemetry frameB = queueB.poll(2, TimeUnit.SECONDS);
        assertThat(frameB)
                .as("Tenant B should NOT receive Tenant A's telemetry — SaaS isolation violation!")
                .isNull();

        // ── Assert: GPS reading belongs to Tenant A only ─────────────────────
        final UUID tenantBId = tenantB.getId();
        long tenantBCount = gpsReadingRepository.findAll().stream()
                .filter(r -> r.getTenantId().equals(tenantBId))
                .count();
        assertThat(tenantBCount).isZero();

        log.info("Multi-tenant isolation verified: Tenant B received 0 frames and 0 DB rows from Tenant A telemetry.");
    }
}
