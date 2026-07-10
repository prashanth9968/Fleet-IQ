package com.fleetiq.alerts.service;

import com.fleetiq.alerts.dto.UnifiedAlertEvent;
import com.fleetiq.alerts.entity.AlertHistory;
import com.fleetiq.alerts.entity.AlertRule;
import com.fleetiq.alerts.entity.EscalationLevel;
import com.fleetiq.alerts.repository.AlertHistoryRepository;
import com.fleetiq.alerts.repository.AlertRuleRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertRoutingService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final StringRedisTemplate redisTemplate;

    // Deduplication cache: key = vehicleId + alertType
    private final Cache<String, Boolean> recentAlertsCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .build();

    public void processAlert(UnifiedAlertEvent event) {
        String dedupKey = event.vehicleId() + ":" + event.alertType();

        // 1. Deduplication
        if (recentAlertsCache.getIfPresent(dedupKey) != null) {
            log.debug("Deduplicating alert {} for vehicle {}", event.alertType(), event.vehicleId());
            return;
        }
        recentAlertsCache.put(dedupKey, true);

        // 2. Fetch Rules
        List<AlertRule> rules = alertRuleRepository.findByTenantIdAndIsActiveTrue(event.tenantId());
        
        // Find matching rules (for simplicity, we assume if the rule priority matches or is lower than the alert's priority, it triggers)
        // In a real system, we'd have a priority enum or weight.
        boolean matched = false;
        for (AlertRule rule : rules) {
            // Check if rule applies (simplified)
            if (rule.getRuleName().contains(event.alertType()) || "ALL".equals(rule.getRuleName())) {
                dispatchChannels(rule, event, 0);
                matched = true;
            }
        }

        // 3. Persist Alert History
        AlertHistory history = new AlertHistory();
        history.setTenantId(event.tenantId());
        history.setVehicleId(event.vehicleId());
        history.setAlertType(event.alertType());
        history.setPriority(event.priority());
        history.setMessage(event.message());
        history.setStatus("OPEN");
        history.setEscalationLevel(0);
        history.setCreatedAt(event.detectedAt());
        history.setUpdatedAt(OffsetDateTime.now());
        alertHistoryRepository.save(history);

        // 4. Analytics Hook
        redisTemplate.convertAndSend("alert.analytics", event);

        if (!matched) {
            log.info("Alert {} stored but no routing rules matched.", event.alertType());
        }
    }

    private void dispatchChannels(AlertRule rule, UnifiedAlertEvent event, int escalationLevel) {
        List<String> channels = rule.getChannels();
        if (channels != null) {
            for (String channel : channels) {
                // Placeholder for actual API integrations
                log.info("Dispatching {} alert to channel: {}", event.priority(), channel);
            }
        }
    }

    // 5. Escalation Engine
    @Scheduled(fixedRate = 60000) // Run every minute
    public void processEscalations() {
        log.debug("Running Escalation Engine...");
        
        // In a real system we would find only OPEN alerts and join with rules.
        // Simplified approach for this phase.
    }
}
