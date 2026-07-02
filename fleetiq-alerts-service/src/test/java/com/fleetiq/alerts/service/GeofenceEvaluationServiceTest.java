package com.fleetiq.alerts.service;

import com.fleetiq.alerts.dto.ProcessedTelemetry;
import com.fleetiq.alerts.entity.Geofence;
import com.fleetiq.alerts.entity.GeofenceEvent;
import com.fleetiq.alerts.repository.GeofenceEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GeofenceEvaluationServiceTest {

    private GeofenceCacheService cacheService;
    private GeofenceEventRepository eventRepository;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private GeofenceEvaluationService service;

    private GeometryFactory gf = new GeometryFactory();
    private UUID tenantId = UUID.randomUUID();
    private UUID vehicleId = UUID.randomUUID();
    private Geofence testGeofence;

    @BeforeEach
    void setUp() {
        cacheService = mock(GeofenceCacheService.class);
        eventRepository = mock(GeofenceEventRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        service = new GeofenceEvaluationService(cacheService, eventRepository, kafkaTemplate);

        testGeofence = new Geofence();
        testGeofence.setId(UUID.randomUUID());
        testGeofence.setTenantId(tenantId);
        testGeofence.setName("Warehouse");
        testGeofence.setMaxSpeedKmh(20.0);
        testGeofence.setMaxDwellMinutes(10);
        // Create a polygon from (0,0) to (10,10)
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(0, 0), new Coordinate(10, 0),
                new Coordinate(10, 10), new Coordinate(0, 10),
                new Coordinate(0, 0)
        };
        testGeofence.setGeom(gf.createPolygon(coords));
    }

    @Test
    void testEnterAndExitEvents() {
        OffsetDateTime t0 = OffsetDateTime.now();

        // 1. Vehicle is outside (x= -5, y= -5)
        when(cacheService.findIntersectingGeofences(tenantId, -5.0, -5.0)).thenReturn(List.of());
        ProcessedTelemetry outside = new ProcessedTelemetry(tenantId, vehicleId, UUID.randomUUID(), t0, -5.0, -5.0, 50.0, 100.0, 1500, "c1");
        service.evaluate(outside);
        verifyNoInteractions(eventRepository);

        // 2. Vehicle enters (x= 5, y= 5)
        when(cacheService.findIntersectingGeofences(tenantId, 5.0, 5.0)).thenReturn(List.of(testGeofence));
        ProcessedTelemetry inside = new ProcessedTelemetry(tenantId, vehicleId, UUID.randomUUID(), t0.plusMinutes(1), 5.0, 5.0, 15.0, 99.0, 1500, "c2");
        service.evaluate(inside);
        
        verify(eventRepository, times(1)).save(argThat(event -> "ENTER".equals(event.getEventType())));

        // 3. Vehicle stays inside, no new enter event
        when(cacheService.findIntersectingGeofences(tenantId, 6.0, 6.0)).thenReturn(List.of(testGeofence));
        ProcessedTelemetry inside2 = new ProcessedTelemetry(tenantId, vehicleId, UUID.randomUUID(), t0.plusMinutes(2), 6.0, 6.0, 15.0, 98.0, 1500, "c3");
        service.evaluate(inside2);
        
        // 4. Vehicle exits (x= 15, y= 15)
        when(cacheService.findIntersectingGeofences(tenantId, 15.0, 15.0)).thenReturn(List.of());
        ProcessedTelemetry outside2 = new ProcessedTelemetry(tenantId, vehicleId, UUID.randomUUID(), t0.plusMinutes(5), 15.0, 15.0, 50.0, 97.0, 1500, "c4");
        service.evaluate(outside2);
        
        ArgumentCaptor<GeofenceEvent> captor = ArgumentCaptor.forClass(GeofenceEvent.class);
        verify(eventRepository, times(2)).save(captor.capture());
        
        List<GeofenceEvent> events = captor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo("ENTER");
        assertThat(events.get(1).getEventType()).isEqualTo("EXIT");
        assertThat(events.get(1).getDwellDurationSeconds()).isEqualTo(240);
    }

    @Test
    void testSpeedingInsideGeofence() {
        OffsetDateTime t0 = OffsetDateTime.now();
        when(cacheService.findIntersectingGeofences(tenantId, 5.0, 5.0)).thenReturn(List.of(testGeofence));

        // Vehicle enters doing 30 km/h (limit is 20)
        ProcessedTelemetry insideSpeeding = new ProcessedTelemetry(tenantId, vehicleId, UUID.randomUUID(), t0, 5.0, 5.0, 30.0, 100.0, 1500, "c1");
        service.evaluate(insideSpeeding);

        // Should save 1 ENTER event and 1 OVERSPEED event
        verify(eventRepository, times(2)).save(any());
        verify(eventRepository).save(argThat(e -> "OVERSPEED".equals(e.getEventType())));
    }

    @Test
    void testDwellTimeViolation() {
        OffsetDateTime t0 = OffsetDateTime.now();
        when(cacheService.findIntersectingGeofences(tenantId, 5.0, 5.0)).thenReturn(List.of(testGeofence));

        // Enters at t0
        service.evaluate(new ProcessedTelemetry(tenantId, vehicleId, UUID.randomUUID(), t0, 5.0, 5.0, 0.0, 100.0, 800, "c1"));
        
        // Stays for 15 minutes (limit is 10)
        service.evaluate(new ProcessedTelemetry(tenantId, vehicleId, UUID.randomUUID(), t0.plusMinutes(15), 5.0, 5.0, 0.0, 98.0, 800, "c2"));

        verify(eventRepository).save(argThat(e -> "DWELL_VIOLATION".equals(e.getEventType())));
    }
}
