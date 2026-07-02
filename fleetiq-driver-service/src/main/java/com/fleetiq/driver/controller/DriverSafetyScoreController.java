package com.fleetiq.driver.controller;

import com.fleetiq.driver.config.TenantContext;
import com.fleetiq.driver.entity.DriverSafetyScore;
import com.fleetiq.driver.repository.DriverSafetyScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverSafetyScoreController {

    private final DriverSafetyScoreRepository safetyScoreRepository;

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context (X-Tenant-ID) is missing or invalid");
        }
        return tenantId;
    }

    @GetMapping("/{driverId}/safety-scores")
    public ResponseEntity<List<DriverSafetyScore>> getDriverScores(
            @PathVariable("driverId") UUID driverId,
            @RequestParam(name = "periodType", defaultValue = "MONTHLY") String periodType
    ) {
        return ResponseEntity.ok(safetyScoreRepository.findByDriverIdAndPeriodTypeOrderByPeriodStartDesc(driverId, periodType));
    }

    @GetMapping("/safety-leaderboard")
    public ResponseEntity<List<DriverSafetyScore>> getLeaderboard(
            @RequestParam(name = "periodType", defaultValue = "MONTHLY") String periodType
    ) {
        UUID tenantId = getRequiredTenantId();
        return ResponseEntity.ok(safetyScoreRepository.findByTenantIdAndPeriodTypeOrderByOverallScoreDesc(tenantId, periodType));
    }
}
