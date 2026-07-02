package com.fleetiq.fuel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleetiq.fuel.entity.FuelAnomalyHistory;
import com.fleetiq.fuel.repository.FuelAnomalyHistoryRepository;
import com.fleetiq.fuel.repository.FuelThresholdRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuelTheftDetectionServiceTest {

    @Mock private FuelThresholdRepository fuelThresholdRepository;
    @Mock private FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private FuelTheftDetectionService service;
    private UUID vehicleId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        service = new FuelTheftDetectionService(fuelThresholdRepository, fuelAnomalyHistoryRepository, kafkaTemplate, mapper);
        vehicleId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("THEFT alert triggers when fuel drops > 5L with ignition off and vehicle stationary")
    void testTheftAlertOnRapidDropWhileStationary() {
        when(fuelThresholdRepository.findByVehicleIdAndAlertType(any(), any())).thenReturn(Optional.empty());
        when(fuelAnomalyHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastSpeedKmh(0.0);       // stationary
        state.setLastIgnition(false);      // ignition off

        OffsetDateTime t0 = OffsetDateTime.now().minusMinutes(3);

        // Add readings showing 8L drop over 3 minutes
        state.addReading(80.0, t0);
        state.addReading(78.0, t0.plusSeconds(30));
        state.addReading(75.0, t0.plusSeconds(90));
        state.addReading(72.0, t0.plusMinutes(3));

        service.evaluate(vehicleId, tenantId, state);

        ArgumentCaptor<FuelAnomalyHistory> captor = ArgumentCaptor.forClass(FuelAnomalyHistory.class);
        verify(fuelAnomalyHistoryRepository).save(captor.capture());
        FuelAnomalyHistory anomaly = captor.getValue();
        assertThat(anomaly.getAnomalyType()).isEqualTo("THEFT");
        assertThat(anomaly.getFuelDropLitres().doubleValue()).isGreaterThan(5.0);

        verify(kafkaTemplate).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }

    @Test
    @DisplayName("No THEFT alert when vehicle is moving")
    void testNoTheftAlertWhenVehicleMoving() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastSpeedKmh(60.0);      // moving
        state.setLastIgnition(true);       // ignition on

        OffsetDateTime t0 = OffsetDateTime.now().minusMinutes(3);
        state.addReading(80.0, t0);
        state.addReading(72.0, t0.plusMinutes(3));

        service.evaluate(vehicleId, tenantId, state);
        verify(fuelAnomalyHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("THEFT alert suppressed when sensor fault is active")
    void testTheftSuppressedWithSensorFault() {
        when(fuelThresholdRepository.findByVehicleIdAndAlertType(any(), any())).thenReturn(Optional.empty());

        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastSpeedKmh(0.0);
        state.setLastIgnition(false);
        state.setSensorFaultActive(true);  // sensor fault active

        OffsetDateTime t0 = OffsetDateTime.now().minusMinutes(3);
        state.addReading(80.0, t0);
        state.addReading(72.0, t0.plusMinutes(3));

        service.evaluate(vehicleId, tenantId, state);
        verify(fuelAnomalyHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("No THEFT alert when drop is below threshold")
    void testNoTheftAlertBelowThreshold() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);
        state.setLastSpeedKmh(0.0);
        state.setLastIgnition(false);

        OffsetDateTime t0 = OffsetDateTime.now().minusMinutes(3);
        state.addReading(80.0, t0);
        state.addReading(77.0, t0.plusMinutes(3)); // only 3L drop, below 5L threshold

        service.evaluate(vehicleId, tenantId, state);
        verify(fuelAnomalyHistoryRepository, never()).save(any());
    }
}
