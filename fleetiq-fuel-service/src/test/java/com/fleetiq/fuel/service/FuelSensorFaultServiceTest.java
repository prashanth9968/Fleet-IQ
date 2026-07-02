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
class FuelSensorFaultServiceTest {

    @Mock private FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private FuelSensorFaultService service;
    private UUID vehicleId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        service = new FuelSensorFaultService(fuelAnomalyHistoryRepository, kafkaTemplate, mapper);
        vehicleId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("SENSOR_FAULT (stuck) triggers when fuel level unchanged for 2+ hours while moving")
    void testStuckSensorFaultWhileMoving() {
        when(fuelAnomalyHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastSpeedKmh(60.0);  // moving

        OffsetDateTime t0 = OffsetDateTime.now().minusHours(3);

        // First reading sets the stuck value
        service.evaluate(vehicleId, tenantId, state, 50.0, t0);
        assertThat(state.getStuckSensorValue()).isEqualTo(50.0);
        
        // Second reading starts the stuck timer
        service.evaluate(vehicleId, tenantId, state, 50.0, t0.plusSeconds(1));
        assertThat(state.getStuckSensorSince()).isEqualTo(t0.plusSeconds(1));

        // Same value 3 hours later while moving → should trigger
        service.evaluate(vehicleId, tenantId, state, 50.0, t0.plusHours(3));

        assertThat(state.isSensorFaultActive()).isTrue();

        ArgumentCaptor<FuelAnomalyHistory> captor = ArgumentCaptor.forClass(FuelAnomalyHistory.class);
        verify(fuelAnomalyHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getAnomalyType()).isEqualTo("SENSOR_FAULT");

        verify(kafkaTemplate).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }

    @Test
    @DisplayName("No stuck fault when fuel level changes")
    void testNoStuckFaultWhenLevelChanges() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastSpeedKmh(60.0);

        OffsetDateTime t0 = OffsetDateTime.now().minusHours(3);

        service.evaluate(vehicleId, tenantId, state, 50.0, t0);
        service.evaluate(vehicleId, tenantId, state, 48.0, t0.plusHours(3));

        assertThat(state.isSensorFaultActive()).isFalse();
        assertThat(state.getStuckSensorValue()).isEqualTo(48.0);
        verify(fuelAnomalyHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("SENSOR_FAULT (erratic) triggers on wild fuel level oscillations within 60 seconds")
    void testErraticSensorFaultOnWildOscillations() {
        when(fuelAnomalyHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);

        OffsetDateTime t0 = OffsetDateTime.now().minusSeconds(50);

        // Wild oscillations: 120L → 40L → 130L → 20L → 150L → 30L → 120L → 50L → 140L → 10L
        double[] levels = {120, 40, 130, 20, 150, 30, 120, 50, 140, 10};
        for (int i = 0; i < levels.length; i++) {
            state.addReading(levels[i], t0.plusSeconds(i * 5));
        }

        service.evaluate(vehicleId, tenantId, state, 10.0, t0.plusSeconds(45));

        assertThat(state.isSensorFaultActive()).isTrue();

        verify(fuelAnomalyHistoryRepository).save(any());
        verify(kafkaTemplate).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }

    @Test
    @DisplayName("No erratic fault with stable readings")
    void testNoErraticFaultWithStableReadings() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);

        OffsetDateTime t0 = OffsetDateTime.now().minusSeconds(50);

        // Stable readings: 50L ± 0.5L
        for (int i = 0; i < 10; i++) {
            state.addReading(50.0 + (i % 2 == 0 ? 0.3 : -0.3), t0.plusSeconds(i * 5));
        }

        service.evaluate(vehicleId, tenantId, state, 50.0, t0.plusSeconds(45));

        assertThat(state.isSensorFaultActive()).isFalse();
        verify(fuelAnomalyHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Null fuel level is ignored gracefully")
    void testNullFuelLevelIgnored() {
        VehicleFuelState state = new VehicleFuelState();
        service.evaluate(vehicleId, tenantId, state, null, OffsetDateTime.now());
        verify(fuelAnomalyHistoryRepository, never()).save(any());
    }
}
