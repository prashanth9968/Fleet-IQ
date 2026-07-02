package com.fleetiq.fuel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleetiq.fuel.entity.FuelBaseline;
import com.fleetiq.fuel.entity.FuelReading;
import com.fleetiq.fuel.repository.FuelBaselineRepository;
import com.fleetiq.fuel.repository.FuelReadingRepository;
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
class FuelAnalyticsServiceTest {

    @Mock private FuelReadingRepository fuelReadingRepository;
    @Mock private FuelBaselineRepository fuelBaselineRepository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private FuelAnalyticsService service;
    private UUID vehicleId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        service = new FuelAnalyticsService(fuelReadingRepository, fuelBaselineRepository, kafkaTemplate, mapper);
        vehicleId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Analytics published when valid previous reading exists with distance and consumption")
    void testAnalyticsPublishedWithValidData() {
        FuelReading previous = new FuelReading();
        previous.setOdometerKm(BigDecimal.valueOf(10000.0));
        previous.setFuelLevelLitres(BigDecimal.valueOf(50.0));
        when(fuelReadingRepository.findTopByVehicleIdOrderByRecordedAtDesc(vehicleId))
                .thenReturn(Optional.of(previous));
        when(fuelBaselineRepository.findByVehicleTypeIdAndTenantId(any(), any()))
                .thenReturn(Optional.empty());

        // Current: odometer 10100, fuel 40 → consumed 10L over 100km = 10 L/100km
        service.recordReading(vehicleId, tenantId, 40.0, 10100.0, 60.0, OffsetDateTime.now());

        verify(kafkaTemplate).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }

    @Test
    @DisplayName("No analytics when no previous reading exists")
    void testNoAnalyticsWithoutPreviousReading() {
        when(fuelReadingRepository.findTopByVehicleIdOrderByRecordedAtDesc(vehicleId))
                .thenReturn(Optional.empty());

        service.recordReading(vehicleId, tenantId, 40.0, 10100.0, 60.0, OffsetDateTime.now());

        verify(kafkaTemplate, never()).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }

    @Test
    @DisplayName("No analytics when fuel level or odometer is null")
    void testNoAnalyticsWithNullValues() {
        service.recordReading(vehicleId, tenantId, null, 10100.0, 60.0, OffsetDateTime.now());
        service.recordReading(vehicleId, tenantId, 40.0, null, 60.0, OffsetDateTime.now());

        verify(fuelReadingRepository, never()).findTopByVehicleIdOrderByRecordedAtDesc(any());
    }

    @Test
    @DisplayName("No analytics when fuel was added (refuel between readings)")
    void testNoAnalyticsOnRefuel() {
        FuelReading previous = new FuelReading();
        previous.setOdometerKm(BigDecimal.valueOf(10000.0));
        previous.setFuelLevelLitres(BigDecimal.valueOf(30.0)); // was lower
        when(fuelReadingRepository.findTopByVehicleIdOrderByRecordedAtDesc(vehicleId))
                .thenReturn(Optional.of(previous));

        // Current fuel level is higher → refuel happened, fuelConsumed < 0
        service.recordReading(vehicleId, tenantId, 50.0, 10100.0, 60.0, OffsetDateTime.now());

        verify(kafkaTemplate, never()).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }

    @Test
    @DisplayName("Analytics calculates baseline deviation when baseline exists")
    void testBaselineDeviationCalculation() {
        FuelReading previous = new FuelReading();
        previous.setOdometerKm(BigDecimal.valueOf(10000.0));
        previous.setFuelLevelLitres(BigDecimal.valueOf(50.0));
        when(fuelReadingRepository.findTopByVehicleIdOrderByRecordedAtDesc(vehicleId))
                .thenReturn(Optional.of(previous));

        FuelBaseline baseline = new FuelBaseline();
        baseline.setExpectedEfficiencyKmPerLitre(BigDecimal.valueOf(5.0)); // 5 km/L baseline
        when(fuelBaselineRepository.findByVehicleTypeIdAndTenantId(any(), any()))
                .thenReturn(Optional.of(baseline));

        // 10L consumed over 100km = 10 km/L actual vs 5 km/L baseline = +100% deviation
        service.recordReading(vehicleId, tenantId, 40.0, 10100.0, 60.0, OffsetDateTime.now());

        verify(kafkaTemplate).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }
}
