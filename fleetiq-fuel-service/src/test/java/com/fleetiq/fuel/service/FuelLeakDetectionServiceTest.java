package com.fleetiq.fuel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleetiq.fuel.entity.FuelAnomalyHistory;
import com.fleetiq.fuel.repository.FuelAnomalyHistoryRepository;
import com.fleetiq.fuel.state.VehicleFuelState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuelLeakDetectionServiceTest {

    @Mock private FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private FuelLeakDetectionService service;
    private UUID vehicleId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        service = new FuelLeakDetectionService(fuelAnomalyHistoryRepository, kafkaTemplate, mapper);
        vehicleId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("LEAK alert triggers on gradual fuel loss while vehicle is moving over 10 minutes")
    void testLeakAlertOnGradualLossWhileMoving() {
        when(fuelAnomalyHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastSpeedKmh(50.0);      // moving normally
        state.setLastIgnition(true);       // ignition on

        OffsetDateTime t0 = OffsetDateTime.now().minusMinutes(12);

        // Gradual decline over 12 minutes: 80, 79.2, 78.4, ..., 70.4
        // Total drop = 9.6L over 12 min = 0.8 L/min
        for (int i = 0; i <= 12; i++) {
            state.addReading(80.0 - (i * 0.8), t0.plusMinutes(i));
        }

        service.evaluate(vehicleId, tenantId, state);

        ArgumentCaptor<FuelAnomalyHistory> captor = ArgumentCaptor.forClass(FuelAnomalyHistory.class);
        verify(fuelAnomalyHistoryRepository).save(captor.capture());
        FuelAnomalyHistory anomaly = captor.getValue();
        assertThat(anomaly.getAnomalyType()).isEqualTo("LEAK");

        verify(kafkaTemplate).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }

    @Test
    @DisplayName("LEAK suppressed when sensor fault is active")
    void testLeakSuppressedWithSensorFault() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastSpeedKmh(50.0);
        state.setLastIgnition(true);
        state.setSensorFaultActive(true);

        OffsetDateTime t0 = OffsetDateTime.now().minusMinutes(12);
        for (int i = 0; i < 9; i++) {
            state.addReading(80.0 - i, t0.plusMinutes(i * 12 / 8));
        }

        service.evaluate(vehicleId, tenantId, state);
        verify(fuelAnomalyHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("No LEAK alert when vehicle is stationary (theft pattern)")
    void testNoLeakAlertWhenStationary() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastSpeedKmh(0.0);       // stationary
        state.setLastIgnition(false);

        OffsetDateTime t0 = OffsetDateTime.now().minusMinutes(12);
        for (int i = 0; i < 9; i++) {
            state.addReading(80.0 - i, t0.plusMinutes(i * 12 / 8));
        }

        service.evaluate(vehicleId, tenantId, state);
        verify(fuelAnomalyHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("No LEAK alert with too few readings")
    void testNoLeakWithFewReadings() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastSpeedKmh(50.0);
        state.setLastIgnition(true);

        state.addReading(80.0, OffsetDateTime.now().minusMinutes(5));
        state.addReading(75.0, OffsetDateTime.now());

        service.evaluate(vehicleId, tenantId, state);
        verify(fuelAnomalyHistoryRepository, never()).save(any());
    }
}
