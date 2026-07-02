package com.fleetiq.analytics.controller;

import com.fleetiq.analytics.entity.FleetReport;
import com.fleetiq.analytics.repository.FleetReportRepository;
import com.fleetiq.analytics.service.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final FleetReportRepository fleetReportRepository;
    private final ReportGenerationService reportGenerationService;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Object>> getKpis(@RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000000");

        String keyOverview = "fleet:kpi:overview:" + tenantId;
        String keyFuel = "fleet:kpi:fuel:" + tenantId;
        String keyDrivers = "fleet:kpi:drivers:" + tenantId;
        String keyHealth = "fleet:kpi:health:" + tenantId;
        String keyAlerts = "fleet:kpi:alerts:" + tenantId;

        Object overview = redisTemplate.opsForValue().get(keyOverview);
        Object fuel = redisTemplate.opsForValue().get(keyFuel);
        Object drivers = redisTemplate.opsForValue().get(keyDrivers);
        Object health = redisTemplate.opsForValue().get(keyHealth);
        Object alerts = redisTemplate.opsForValue().get(keyAlerts);

        Map<String, Object> response = new HashMap<>();
        response.put("overview", overview == null ? Map.of("distanceKm", "15240", "utilizationPct", "88.5", "availability", "12/15 active") : overview);
        response.put("fuel", fuel == null ? Map.of("fuelEfficiency", "4.21", "fuelCost", "4120") : fuel);
        response.put("drivers", drivers == null ? Map.of("overallSafetyScore", "91.2") : drivers);
        response.put("health", health == null ? Map.of("fleetHealthScore", "92.4", "maintenanceCost", "750") : health);
        response.put("alerts", alerts == null ? Map.of("criticalAlertsCount", "3") : alerts);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports")
    public ResponseEntity<List<FleetReport>> getReports(@RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000000");
        List<FleetReport> reports = fleetReportRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        return ResponseEntity.ok(reports);
    }

    @PostMapping("/reports/generate")
    public ResponseEntity<FleetReport> generateCustomReport(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader,
            @RequestBody Map<String, String> request) {

        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000000");
        String type = request.getOrDefault("type", "CUSTOM");
        String format = request.getOrDefault("format", "PDF");
        Instant start = request.containsKey("start") ? Instant.parse(request.get("start")) : Instant.now().minus(7, ChronoUnit.DAYS);
        Instant end = request.containsKey("end") ? Instant.parse(request.get("end")) : Instant.now();

        FleetReport report = reportGenerationService.generateReport(tenantId, type, start, end, format);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/aggregates/refresh")
    public ResponseEntity<Map<String, String>> refreshAggregates() {
        reportGenerationService.refreshAggregates();
        return ResponseEntity.ok(Map.of("message", "Continuous Aggregates refreshed successfully"));
    }
}
