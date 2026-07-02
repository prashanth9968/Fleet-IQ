package com.fleetiq.alerts.repository;

import com.fleetiq.alerts.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {
    List<AlertRule> findByTenantIdAndIsActiveTrue(UUID tenantId);
}
