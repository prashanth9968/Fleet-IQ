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
class FuelIdleDetectionServiceTest {

    @Mock private FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private FuelIdleDetectionService service;
    private UUID vehicleId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        service = new FuelIdleDetectionService(fuelAnomalyHistoryRepository, kafkaTemplate, mapper);
        vehicleId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("IDLE_BURN alert triggers after 10+ minutes of idle with high fuel burn")
    void testIdleBurnAlertAfterSustainedIdle() {
        when(fuelAnomalyHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastEngineRpm(800);       // engine running
        state.setLastSpeedKmh(0.0);        // stationary
        state.setLastIgnition(true);

        OffsetDateTime t0 = OffsetDateTime.now().minusMinutes(12);

        // First call: starts idle tracking
        service.evaluate(vehicleId, tenantId, state, 0.5, t0);
        assertThat(state.getIdleStart()).isNotNull();

        // Second call: 11 minutes later, should trigger
        service.evaluate(vehicleId, tenantId, state, 0.5, t0.plusMinutes(11));

        ArgumentCaptor<FuelAnomalyHistory> captor = ArgumentCaptor.forClass(FuelAnomalyHistory.class);
        verify(fuelAnomalyHistoryRepository).save(captor.capture());
        FuelAnomalyHistory anomaly = captor.getValue();
        assertThat(anomaly.getAnomalyType()).isEqualTo("IDLE_BURN");

        verify(kafkaTemplate).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }

    @Test
    @DisplayName("Idle tracking resets when vehicle starts moving")
    void testIdleResetsWhenMoving() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastEngineRpm(800);
        state.setLastSpeedKmh(0.0);

        OffsetDateTime t0 = OffsetDateTime.now();

        // Start idle tracking
        service.evaluate(vehicleId, tenantId, state, 0.5, t0);
        assertThat(state.getIdleStart()).isNotNull();

        // Vehicle starts moving
        state.setLastSpeedKmh(30.0);
        service.evaluate(vehicleId, tenantId, state, 0.5, t0.plusMinutes(5));
        assertThat(state.getIdleStart()).isNull();

        verify(fuelAnomalyHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("No idle alert when engine is off (RPM = 0)")
    void testNoIdleAlertWhenEngineOff() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastEngineRpm(0);         // engine off
        state.setLastSpeedKmh(0.0);

        service.evaluate(vehicleId, tenantId, state, 0.5, OffsetDateTime.now());
        assertThat(state.getIdleStart()).isNull();
    }

    @Test
    @DisplayName("No idle alert when fuel rate below idle threshold")
    void testNoIdleAlertBelowThreshold() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastEngineRpm(800);
        state.setLastSpeedKmh(0.0);

        // 0.1 L/min is below idle threshold of 0.3 L/min
        service.evaluate(vehicleId, tenantId, state, 0.1, OffsetDateTime.now());
        assertThat(state.getIdleStart()).isNull();
    }
}
