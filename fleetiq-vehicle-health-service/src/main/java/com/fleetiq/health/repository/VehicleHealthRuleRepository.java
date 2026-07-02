package com.fleetiq.health.repository;

import com.fleetiq.health.entity.VehicleHealthRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleHealthRuleRepository extends JpaRepository<VehicleHealthRule, UUID> {
    Optional<VehicleHealthRule> findByTenantIdAndRuleType(UUID tenantId, String ruleType);
}
