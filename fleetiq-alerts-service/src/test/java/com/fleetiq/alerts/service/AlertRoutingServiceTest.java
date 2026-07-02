package com.fleetiq.alerts.service;

import com.fleetiq.alerts.dto.UnifiedAlertEvent;
import com.fleetiq.alerts.entity.AlertHistory;
import com.fleetiq.alerts.entity.AlertRule;
import com.fleetiq.alerts.repository.AlertHistoryRepository;
import com.fleetiq.alerts.repository.AlertRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlertRoutingServiceTest {

    private AlertRuleRepository ruleRepository;
    private AlertHistoryRepository historyRepository;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private AlertRoutingService service;

    private UUID tenantId = UUID.randomUUID();
    private UUID vehicleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ruleRepository = mock(AlertRuleRepository.class);
        historyRepository = mock(AlertHistoryRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        service = new AlertRoutingService(ruleRepository, historyRepository, kafkaTemplate);
    }

    @Test
    void testAlertDeduplication() {
        UnifiedAlertEvent event1 = new UnifiedAlertEvent(tenantId, vehicleId, "source", "FUEL_THEFT", "CRITICAL", "msg", OffsetDateTime.now(), new HashMap<>(), "c1");
        UnifiedAlertEvent event2 = new UnifiedAlertEvent(tenantId, vehicleId, "source", "FUEL_THEFT", "CRITICAL", "msg", OffsetDateTime.now(), new HashMap<>(), "c2");

        service.processAlert(event1);
        service.processAlert(event2); // Should be deduped

        // History repository should only save ONCE
        verify(historyRepository, times(1)).save(any(AlertHistory.class));
        verify(kafkaTemplate, times(1)).send(eq("alert.analytics"), any(UnifiedAlertEvent.class));
    }

    @Test
    void testAlertRouting() {
        AlertRule rule = new AlertRule();
        rule.setRuleName("ALL");
        rule.setChannels(List.of("EMAIL", "SMS"));
        when(ruleRepository.findByTenantIdAndIsActiveTrue(tenantId)).thenReturn(List.of(rule));

        UnifiedAlertEvent event = new UnifiedAlertEvent(tenantId, vehicleId, "source", "OVERSPEED_IN_GEOFENCE", "HIGH", "msg", OffsetDateTime.now(), new HashMap<>(), "c1");
        service.processAlert(event);

        verify(historyRepository).save(argThat(h -> "OVERSPEED_IN_GEOFENCE".equals(h.getAlertType()) && "OPEN".equals(h.getStatus())));
        verify(kafkaTemplate).send(eq("alert.analytics"), eq(event));
    }
}
