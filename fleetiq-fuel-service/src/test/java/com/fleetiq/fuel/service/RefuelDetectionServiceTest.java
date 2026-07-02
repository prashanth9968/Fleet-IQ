package com.fleetiq.fuel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleetiq.fuel.entity.FuelRefuelEvent;
import com.fleetiq.fuel.repository.FuelRefuelEventRepository;
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
class RefuelDetectionServiceTest {

    @Mock private FuelRefuelEventRepository fuelRefuelEventRepository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private RefuelDetectionService service;
    private UUID vehicleId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        service = new RefuelDetectionService(fuelRefuelEventRepository, kafkaTemplate, mapper);
        vehicleId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Refuel detected when fuel level increases > 5L within 10 minutes")
    void testRefuelDetection() {
        when(fuelRefuelEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastFuelLevel(40.0);
        state.setLastUpdate(OffsetDateTime.now().minusMinutes(5));
        state.setLastLatitude(12.9716);
        state.setLastLongitude(77.5946);

        service.evaluate(vehicleId, tenantId, state, 70.0, OffsetDateTime.now());

        ArgumentCaptor<FuelRefuelEvent> captor = ArgumentCaptor.forClass(FuelRefuelEvent.class);
        verify(fuelRefuelEventRepository).save(captor.capture());
        FuelRefuelEvent event = captor.getValue();
        assertThat(event.getFuelBeforeLitres().doubleValue()).isEqualTo(40.0);
        assertThat(event.getFuelAfterLitres().doubleValue()).isEqualTo(70.0);
        assertThat(event.getFuelAddedLitres().doubleValue()).isEqualTo(30.0);

        verify(kafkaTemplate).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }

    @Test
    @DisplayName("No refuel event when increase is below threshold")
    void testNoRefuelBelowThreshold() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastFuelLevel(40.0);
        state.setLastUpdate(OffsetDateTime.now().minusMinutes(5));

        // Only 3L increase, below 5L threshold
        service.evaluate(vehicleId, tenantId, state, 43.0, OffsetDateTime.now());

        verify(fuelRefuelEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("No refuel event when time gap exceeds 10-minute window")
    void testNoRefuelOutsideWindow() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastFuelLevel(40.0);
        state.setLastUpdate(OffsetDateTime.now().minusMinutes(15)); // 15 min gap

        service.evaluate(vehicleId, tenantId, state, 70.0, OffsetDateTime.now());

        verify(fuelRefuelEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("No refuel when previous fuel level is null (first reading)")
    void testNoRefuelOnFirstReading() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastFuelLevel(null);

        service.evaluate(vehicleId, tenantId, state, 70.0, OffsetDateTime.now());

        verify(fuelRefuelEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Null current fuel level is ignored gracefully")
    void testNullCurrentFuelLevelIgnored() {
        VehicleFuelState state = new VehicleFuelState();
        state.setLastFuelLevel(40.0);

        service.evaluate(vehicleId, tenantId, state, null, OffsetDateTime.now());
        verify(fuelRefuelEventRepository, never()).save(any());
    }
}
