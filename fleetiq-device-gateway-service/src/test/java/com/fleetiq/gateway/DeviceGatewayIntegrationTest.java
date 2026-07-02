package com.fleetiq.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.gateway.client.AuthServiceClient;
import com.fleetiq.gateway.dto.NormalizedTelemetry;
import com.fleetiq.gateway.service.TelemetryProcessor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

public class DeviceGatewayIntegrationTest extends AbstractIntegrationTest {

    @MockBean
    private AuthServiceClient authServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    private MqttClient mqttClient;
    private KafkaConsumer<String, String> telemetryConsumer;
    private KafkaConsumer<String, String> dlqConsumer;

    @BeforeEach
    public void setUp() throws Exception {
        // Set up MQTT client to publish test messages to HiveMQ
        mqttClient = new MqttClient("tcp://" + hivemq.getHost() + ":" + hivemq.getMqttPort(), "test-publisher-" + UUID.randomUUID());
        mqttClient.connect();

        // Set up Kafka Consumers
        telemetryConsumer = createConsumer("raw.telemetry");
        dlqConsumer = createConsumer("raw.telemetry.dlq");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.disconnect();
            mqttClient.close();
        }
        if (telemetryConsumer != null) {
            telemetryConsumer.close();
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
        return consumer;
    }

    @Test
    public void testSuccessfulTelemetryIngestion() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String apiKey = "flq_live_test_api_key_123456789";
        
        // Mock active API key verification
        Mockito.when(authServiceClient.verifyKey(apiKey))
                .thenReturn(new AuthServiceClient.VerificationResult(true, tenantId, List.of("telemetry:write"), 120));

        String rawPayload = "{" +
                "\"apiKey\":\"" + apiKey + "\"," +
                "\"deviceId\":\"DEV-101\"," +
                "\"timestamp\":\"2026-06-06T12:00:00Z\"," +
                "\"latitude\":17.385," +
                "\"longitude\":78.486," +
                "\"speed\":62.5," +
                "\"fuelLevel\":80.2," +
                "\"engineRpm\":2200" +
                "}";

        // Publish raw telemetry payload to MQTT broker
        MqttMessage message = new MqttMessage(rawPayload.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);
        mqttClient.publish("telemetry/dev-101", message);

        // Poll Kafka raw.telemetry topic to verify normalization
        ConsumerRecords<String, String> records = telemetryConsumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo("DEV-101");

        NormalizedTelemetry normalized = objectMapper.readValue(record.value(), NormalizedTelemetry.class);
        assertThat(normalized.tenantId()).isEqualTo(tenantId);
        assertThat(normalized.deviceId()).isEqualTo("DEV-101");
        assertThat(normalized.latitude()).isEqualTo(17.385);
        assertThat(normalized.longitude()).isEqualTo(78.486);
        assertThat(normalized.speedKmh()).isEqualTo(62.5);
        assertThat(normalized.fuelLevelLitres()).isEqualTo(80.2);
        assertThat(normalized.engineRpm()).isEqualTo(2200);

        // Verify standard trace and context headers
        assertThat(record.headers().lastHeader("tenant-id")).isNotNull();
        assertThat(new String(record.headers().lastHeader("tenant-id").value(), StandardCharsets.UTF_8)).isEqualTo(tenantId.toString());
        assertThat(record.headers().lastHeader("correlation-id")).isNotNull();
        assertThat(record.headers().lastHeader("producer-id")).isNotNull();
        assertThat(new String(record.headers().lastHeader("producer-id").value(), StandardCharsets.UTF_8)).isEqualTo("fleetiq-device-gateway-service");
    }

    @Test
    public void testIngestionAuthenticationFailureRoutesToDlq() throws Exception {
        String apiKey = "flq_live_invalid_key";
        
        // Mock failed API key verification
        Mockito.when(authServiceClient.verifyKey(anyString()))
                .thenReturn(new AuthServiceClient.VerificationResult(false, null, List.of(), null));

        String rawPayload = "{" +
                "\"apiKey\":\"" + apiKey + "\"," +
                "\"deviceId\":\"DEV-202\"," +
                "\"timestamp\":\"2026-06-06T12:00:00Z\"," +
                "\"latitude\":17.385," +
                "\"longitude\":78.486," +
                "\"speed\":62.5," +
                "\"fuelLevel\":80.2," +
                "\"engineRpm\":2200" +
                "}";

        MqttMessage message = new MqttMessage(rawPayload.getBytes(StandardCharsets.UTF_8));
        mqttClient.publish("telemetry/dev-202", message);

        // Verify message goes to DLQ topic
        ConsumerRecords<String, String> dlqRecords = dlqConsumer.poll(Duration.ofSeconds(10));
        assertThat(dlqRecords.count()).isGreaterThanOrEqualTo(1);

        ConsumerRecord<String, String> record = dlqRecords.iterator().next();
        TelemetryProcessor.DlqEnvelope envelope = objectMapper.readValue(record.value(), TelemetryProcessor.DlqEnvelope.class);
        
        assertThat(envelope.reason()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(envelope.rawPayload()).contains("DEV-202");
    }

    @Test
    public void testMalformedPayloadRoutesToDlq() throws Exception {
        // Publish malformed JSON (missing closing bracket)
        String malformedPayload = "{\"apiKey\":\"flq_abc\", \"deviceId\":\"DEV-303\"";

        MqttMessage message = new MqttMessage(malformedPayload.getBytes(StandardCharsets.UTF_8));
        mqttClient.publish("telemetry/dev-303", message);

        ConsumerRecords<String, String> dlqRecords = dlqConsumer.poll(Duration.ofSeconds(10));
        assertThat(dlqRecords.count()).isGreaterThanOrEqualTo(1);

        ConsumerRecord<String, String> record = dlqRecords.iterator().next();
        TelemetryProcessor.DlqEnvelope envelope = objectMapper.readValue(record.value(), TelemetryProcessor.DlqEnvelope.class);

        assertThat(envelope.reason()).isEqualTo("MALFORMED_JSON");
        assertThat(envelope.rawPayload()).isEqualTo(malformedPayload);
    }
}
