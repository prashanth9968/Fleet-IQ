package com.fleetiq.health.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.health.dto.MaintenanceEventDto;
import com.fleetiq.health.dto.VehicleHealthAlertDto;
import com.fleetiq.health.dto.VehicleHealthEventDto;
import com.fleetiq.health.entity.*;
import com.fleetiq.health.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthConsumer {

    private final DtcLibraryRepository dtcLibraryRepository;
    private final VehicleDtcEventRepository dtcEventRepository;
    private final VehicleHealthScoreRepository healthScoreRepository;
    private final VehicleHealthHistoryRepository healthHistoryRepository;
    private final VehicleHealthRuleRepository healthRuleRepository;
    private final MaintenanceScheduleRepository maintenanceScheduleRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "vehicle.health.events", groupId = "fleetiq-health-group")
    @Transactional
    public void onHealthEvent(String payload) {
        try {
            VehicleHealthEventDto dto = objectMapper.readValue(payload, VehicleHealthEventDto.class);
            Instant eventAt = dto.recordedAt() != null ? dto.recordedAt() : Instant.now();

            // 1. Process Active DTCs
            processActiveDtcs(dto.tenantId(), dto.vehicleId(), dto.activeDtcs(), eventAt, dto.odometerKm());

            // 2. Fetch Configurable Health Rules
            double coolantThreshold = getThreshold(dto.tenantId(), "COOLANT_TEMP", 105.00);
            double batteryThreshold = getThreshold(dto.tenantId(), "BATTERY_VOLTAGE", 11.80);
            double oilThreshold = getThreshold(dto.tenantId(), "OIL_PRESSURE", 150.00);
            double loadThreshold = getThreshold(dto.tenantId(), "ENGINE_LOAD", 95.00);

            double coolantDeduction = getDeduction(dto.tenantId(), "COOLANT_TEMP", 15.00);
            double batteryDeduction = getDeduction(dto.tenantId(), "BATTERY_VOLTAGE", 10.00);
            double oilDeduction = getDeduction(dto.tenantId(), "OIL_PRESSURE", 20.00);
            double loadDeduction = getDeduction(dto.tenantId(), "ENGINE_LOAD", 5.00);

            // 3. Compute Sub-component Scores (Base 100)
            int coolingScore = 100;
            int batteryScore = 100;
            int engineScore = 100;
            int electricalScore = 100;
            int transmissionScore = 100;
            int brakeScore = 100;
            int tyreScore = 100;
            int emissionScore = 100;

            // Apply Sensor Penalties
            if (dto.coolantTempC() != null && dto.coolantTempC() > coolantThreshold) {
                coolingScore -= coolantDeduction;
                emitAlert(dto.tenantId(), dto.vehicleId(), "ENGINE_OVERHEAT", "CRITICAL",
                        "Engine coolant temperature spiked to " + dto.coolantTempC() + "°C", eventAt, dto.coolantTempC(), coolantThreshold);
            }
            if (dto.batteryVoltage() != null && dto.batteryVoltage() < batteryThreshold) {
                batteryScore -= batteryDeduction;
                emitAlert(dto.tenantId(), dto.vehicleId(), "LOW_BATTERY", "HIGH",
                        "System battery voltage low: " + dto.batteryVoltage() + "V", eventAt, dto.batteryVoltage(), batteryThreshold);
            }
            if (dto.oilPressureKpa() != null && dto.oilPressureKpa() < oilThreshold) {
                engineScore -= oilDeduction;
                emitAlert(dto.tenantId(), dto.vehicleId(), "LOW_OIL_PRESSURE", "CRITICAL",
                        "Engine oil pressure critical: " + dto.oilPressureKpa() + " kPa", eventAt, dto.oilPressureKpa(), oilThreshold);
            }
            if (dto.engineLoadPct() != null && dto.engineLoadPct() > loadThreshold) {
                engineScore -= loadDeduction;
                emitAlert(dto.tenantId(), dto.vehicleId(), "HIGH_ENGINE_LOAD", "MEDIUM",
                        "Engine load threshold crossed: " + dto.engineLoadPct() + "%", eventAt, dto.engineLoadPct(), loadThreshold);
            }

            // Apply DTC specific penalties
            List<VehicleDtcEvent> activeEvents = dtcEventRepository.findByVehicleIdAndIsActiveTrue(dto.vehicleId());
            for (VehicleDtcEvent dtcEvent : activeEvents) {
                String code = dtcEvent.getDtc().getCode();
                switch (code) {
                    case "P0300", "P0520" -> engineScore -= 25;
                    case "P0115" -> coolingScore -= 15;
                    case "P0562" -> electricalScore -= 15;
                }
            }

            // Keep scores bounded 0-100
            coolingScore = Math.max(0, coolingScore);
            batteryScore = Math.max(0, batteryScore);
            engineScore = Math.max(0, engineScore);
            electricalScore = Math.max(0, electricalScore);

            // Compute overall score
            double overall = (coolingScore + batteryScore + engineScore + electricalScore + transmissionScore + brakeScore + tyreScore + emissionScore) / 8.0;

            // 4. Update VehicleHealthScore
            updateHealthScores(dto.tenantId(), dto.vehicleId(), overall, engineScore, transmissionScore, electricalScore, brakeScore, batteryScore, tyreScore, emissionScore, coolingScore, activeEvents.size(), eventAt);

            // 5. Update Daily Timeline History
            updateDailyHistory(dto.tenantId(), dto.vehicleId(), overall, engineScore, transmissionScore, electricalScore, brakeScore, batteryScore, tyreScore, emissionScore, coolingScore, activeEvents.size(), dto.odometerKm(), dto.engineRunHours(), eventAt);

            // 6. Evaluate Maintenance schedules (SERVICE_DUE alert check)
            checkMaintenanceDue(dto.tenantId(), dto.vehicleId(), dto.odometerKm(), eventAt);

        } catch (Exception e) {
            log.error("Error evaluating vehicle health telemetry: {}", e.getMessage(), e);
        }
    }

    @Cacheable(value = "healthRules", key = "#tenantId.toString() + ':' + #ruleType")
    public Optional<VehicleHealthRule> getHealthRule(UUID tenantId, String ruleType) {
        return healthRuleRepository.findByTenantIdAndRuleType(tenantId, ruleType);
    }

    private double getThreshold(UUID tenantId, String ruleType, double defaultValue) {
        return getHealthRule(tenantId, ruleType)
                .map(r -> r.getThresholdValue().doubleValue())
                .orElse(defaultValue);
    }

    private double getDeduction(UUID tenantId, String ruleType, double defaultValue) {
        return getHealthRule(tenantId, ruleType)
                .map(r -> r.getDeduction().doubleValue())
                .orElse(defaultValue);
    }

    private void processActiveDtcs(UUID tenantId, UUID vehicleId, List<String> codes, Instant eventAt, Double odometer) {
        if (codes == null) return;

        // Clear existing events not in the new active list
        List<VehicleDtcEvent> activeEvents = dtcEventRepository.findByVehicleIdAndIsActiveTrue(vehicleId);
        for (VehicleDtcEvent active : activeEvents) {
            if (!codes.contains(active.getDtc().getCode())) {
                active.setActive(false);
                active.setClearedAt(eventAt);
                dtcEventRepository.save(active);
                log.info("Cleared DTC {} for vehicle ID: {}", active.getDtc().getCode(), vehicleId);
            }
        }

        // Add new events
        for (String code : codes) {
            boolean alreadyActive = activeEvents.stream().anyMatch(e -> e.getDtc().getCode().equals(code));
            if (!alreadyActive) {
                Optional<DtcLibrary> dtcOpt = dtcLibraryRepository.findByCode(code);
                if (dtcOpt.isPresent()) {
                    DtcLibrary dtc = dtcOpt.get();
                    VehicleDtcEvent dtcEvent = VehicleDtcEvent.builder()
                            .id(UUID.randomUUID())
                            .tenantId(tenantId)
                            .vehicleId(vehicleId)
                            .dtc(dtc)
                            .detectedAt(eventAt)
                            .isActive(true)
                            .odometerAtDetection(odometer != null ? BigDecimal.valueOf(odometer) : BigDecimal.ZERO)
                            .build();

                    dtcEventRepository.save(dtcEvent);
                    log.warn("Detected DTC code {} (Severity: {}) on vehicle {}", code, dtc.getSeverity(), vehicleId);

                    emitAlert(tenantId, vehicleId, "CRITICAL_DTC", dtc.getSeverity(),
                            "Active trouble code injected: " + code + " - " + dtc.getDescription(), eventAt, 1.0, 0.0);
                }
            }
        }
    }

    private void updateHealthScores(UUID tenantId, UUID vehicleId, double overall, int engine, int trans, int elect, int brake, int battery, int tyre, int emission, int cooling, int activeDtcs, Instant eventAt) {
        VehicleHealthScore score = healthScoreRepository.findByVehicleId(vehicleId)
                .orElseGet(() -> VehicleHealthScore.builder()
                        .id(UUID.randomUUID())
                        .tenantId(tenantId)
                        .vehicleId(vehicleId)
                        .build());

        score.setOverallScore(BigDecimal.valueOf(overall).setScale(2, RoundingMode.HALF_UP));
        score.setEngineScore(BigDecimal.valueOf(engine));
        score.setTransmissionScore(BigDecimal.valueOf(trans));
        score.setElectricalScore(BigDecimal.valueOf(elect));
        score.setBrakeScore(BigDecimal.valueOf(brake));
        score.setBatteryScore(BigDecimal.valueOf(battery));
        score.setTyreScore(BigDecimal.valueOf(tyre));
        score.setEmissionScore(BigDecimal.valueOf(emission));
        score.setCoolingScore(BigDecimal.valueOf(cooling));
        score.setActiveDtcCount(activeDtcs);
        score.setLastCalculatedAt(eventAt);

        try {
            healthScoreRepository.save(score);
            // Publish to health analytics
            kafkaTemplate.send("vehicle.health.analytics", objectMapper.writeValueAsString(score));
        } catch (Exception e) {
            log.error("Failed to serialize health analytics payload", e);
        }
    }

    private void updateDailyHistory(UUID tenantId, UUID vehicleId, double overall, int engine, int trans, int elect, int brake, int battery, int tyre, int emission, int cooling, int activeDtcs, Double odometer, Double engineHours, Instant eventAt) {
        LocalDate today = LocalDate.ofInstant(eventAt, ZoneOffset.UTC);
        Instant dayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<VehicleHealthHistory> historyList = healthHistoryRepository.findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(vehicleId, dayStart, dayEnd);
        VehicleHealthHistory history = historyList.isEmpty() 
                ? VehicleHealthHistory.builder()
                        .id(UUID.randomUUID())
                        .tenantId(tenantId)
                        .vehicleId(vehicleId)
                        .recordedAt(eventAt)
                        .build()
                : historyList.get(0);

        history.setOverallScore(BigDecimal.valueOf(overall).setScale(2, RoundingMode.HALF_UP));
        history.setEngineScore(BigDecimal.valueOf(engine));
        history.setTransmissionScore(BigDecimal.valueOf(trans));
        history.setElectricalScore(BigDecimal.valueOf(elect));
        history.setBrakeScore(BigDecimal.valueOf(brake));
        history.setBatteryScore(BigDecimal.valueOf(battery));
        history.setTyreScore(BigDecimal.valueOf(tyre));
        history.setEmissionScore(BigDecimal.valueOf(emission));
        history.setCoolingScore(BigDecimal.valueOf(cooling));
        history.setActiveDtcCount(activeDtcs);
        history.setOdometerKm(odometer != null ? BigDecimal.valueOf(odometer) : BigDecimal.ZERO);
        history.setEngineHours(engineHours != null ? BigDecimal.valueOf(engineHours) : BigDecimal.ZERO);

        healthHistoryRepository.save(history);
    }

    private void checkMaintenanceDue(UUID tenantId, UUID vehicleId, Double currentOdometer, Instant eventAt) {
        if (currentOdometer == null) return;

        List<MaintenanceSchedule> schedules = maintenanceScheduleRepository.findByVehicleIdAndStatus(vehicleId, "ACTIVE");
        for (MaintenanceSchedule schedule : schedules) {
            BigDecimal nextDueOdom = schedule.getNextDueOdometer();
            if (nextDueOdom != null && BigDecimal.valueOf(currentOdometer).compareTo(nextDueOdom) >= 0 && !schedule.isOverdue()) {
                schedule.setOverdue(true);
                maintenanceScheduleRepository.save(schedule);

                // Publish SERVICE_DUE event
                MaintenanceEventDto maintenanceEvent = new MaintenanceEventDto(
                        tenantId,
                        vehicleId,
                        "SERVICE_DUE",
                        null,
                        "Service due: threshold limit crossed on schedule type " + schedule.getServiceType(),
                        eventAt
                );
                try {
                    kafkaTemplate.send("maintenance.events", objectMapper.writeValueAsString(maintenanceEvent));
                } catch (Exception e) {
                    log.error("Failed to serialize maintenance event payload", e);
                }

                emitAlert(tenantId, vehicleId, "SERVICE_DUE", "MEDIUM",
                        "Scheduled maintenance milestone crossed for vehicle. Service due: " + schedule.getServiceType(), eventAt, currentOdometer, nextDueOdom.doubleValue());
            }
        }
    }

    private void emitAlert(UUID tenantId, UUID vehicleId, String alertType, String severity, String msg, Instant detectedAt, Double value, Double threshold) {
        VehicleHealthAlertDto alert = new VehicleHealthAlertDto(tenantId, vehicleId, alertType, severity, msg, detectedAt, value, threshold);
        try {
            kafkaTemplate.send("vehicle.health.alerts", objectMapper.writeValueAsString(alert));
        } catch (Exception e) {
            log.error("Failed to serialize health alert payload", e);
        }
    }
}
