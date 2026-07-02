package com.fleetiq.driver.repository;

import com.fleetiq.driver.entity.DriverSafetyScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DriverSafetyScoreRepository extends JpaRepository<DriverSafetyScore, UUID> {
    List<DriverSafetyScore> findByDriverIdAndPeriodTypeOrderByPeriodStartDesc(UUID driverId, String periodType);
    List<DriverSafetyScore> findByTenantIdAndPeriodTypeOrderByOverallScoreDesc(UUID tenantId, String periodType);
}
