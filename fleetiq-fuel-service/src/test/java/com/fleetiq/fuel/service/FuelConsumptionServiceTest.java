package com.fleetiq.fuel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleetiq.fuel.entity.FuelAnomalyHistory;
import com.fleetiq.fuel.entity.FuelThreshold;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuelConsumptionServiceTest {

    @Mock private FuelThresholdRepository fuelThresholdRepository;
    @Mock private FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private FuelConsumptionService service;
    private UUID vehicleId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        service = new FuelConsumptionService(fuelThresholdRepository, fuelAnomalyHistoryRepository, kafkaTemplate, mapper);
        vehicleId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("HIGH_CONSUMPTION alert triggers after sustained 30+ seconds above threshold")
    void testHighConsumptionAlertAfterSustainedPeriod() {
        when(fuelThresholdRepository.findByVehicleIdAndAlertType(any(), any())).thenReturn(Optional.empty());
        when(fuelAnomalyHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);

        OffsetDateTime t0 = OffsetDateTime.now().minusSeconds(40);
        OffsetDateTime t1 = t0.plusSeconds(35);

        // First call: starts tracking
        service.evaluate(vehicleId, tenantId, state, 1.5, t0);
        assertThat(state.getHighConsumptionStart()).isNotNull();
        assertThat(state.getConsecutiveHighReadings()).isEqualTo(1);

        // Second call: 35 seconds later, should trigger alert
        service.evaluate(vehicleId, tenantId, state, 1.5, t1);

        ArgumentCaptor<FuelAnomalyHistory> captor = ArgumentCaptor.forClass(FuelAnomalyHistory.class);
        verify(fuelAnomalyHistoryRepository).save(captor.capture());
        FuelAnomalyHistory anomaly = captor.getValue();
        assertThat(anomaly.getAnomalyType()).isEqualTo("HIGH_CONSUMPTION");
        assertThat(anomaly.getVehicleId()).isEqualTo(vehicleId);
        assertThat(anomaly.getTenantId()).isEqualTo(tenantId);
        assertThat(anomaly.getDurationSeconds()).isGreaterThanOrEqualTo(30);

        verify(kafkaTemplate).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }

    @Test
    @DisplayName("No alert when fuel rate drops below threshold before 30s")
    void testNoAlertWhenRateDropsBelowThreshold() {
        when(fuelThresholdRepository.findByVehicleIdAndAlertType(any(), any())).thenReturn(Optional.empty());

        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);

        OffsetDateTime t0 = OffsetDateTime.now();

        // First call: high rate, starts tracking
        service.evaluate(vehicleId, tenantId, state, 1.5, t0);
        assertThat(state.getHighConsumptionStart()).isNotNull();

        // Second call: rate drops below threshold
        service.evaluate(vehicleId, tenantId, state, 0.5, t0.plusSeconds(10));
        assertThat(state.getHighConsumptionStart()).isNull();
        assertThat(state.getConsecutiveHighReadings()).isZero();

        verify(fuelAnomalyHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Uses vehicle-specific threshold when configured")
    void testVehicleSpecificThreshold() {
        FuelThreshold threshold = new FuelThreshold();
        threshold.setThresholdValue(BigDecimal.valueOf(2.0));
        when(fuelThresholdRepository.findByVehicleIdAndAlertType(vehicleId, "HIGH_CONSUMPTION"))
                .thenReturn(Optional.of(threshold));

        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(vehicleId);
        state.setTenantId(tenantId);

        // 1.5 L/min is below vehicle threshold of 2.0
        service.evaluate(vehicleId, tenantId, state, 1.5, OffsetDateTime.now());
        assertThat(state.getHighConsumptionStart()).isNull();
    }

    @Test
    @DisplayName("Null fuel rate is ignored gracefully")
    void testNullFuelRateIgnored() {
        VehicleFuelState state = new VehicleFuelState();
        service.evaluate(vehicleId, tenantId, state, null, OffsetDateTime.now());
        verify(fuelThresholdRepository, never()).findByVehicleIdAndAlertType(any(), any());
    }
}
