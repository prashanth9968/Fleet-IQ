package com.fleetiq.driver.repository;

import com.fleetiq.driver.entity.DriverScoringRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverScoringRuleRepository extends JpaRepository<DriverScoringRule, UUID> {
    Optional<DriverScoringRule> findByTenantIdAndEventType(UUID tenantId, String eventType);
}
