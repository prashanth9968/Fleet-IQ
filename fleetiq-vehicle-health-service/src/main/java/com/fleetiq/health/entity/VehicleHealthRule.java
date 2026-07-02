package com.fleetiq.health.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "vehicle_health_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleHealthRule {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rule_type", nullable = false, length = 100)
    private String ruleType;

    @Column(length = 50)
    private String severity;

    @Column(nullable = false)
    private BigDecimal deduction;

    @Column(name = "threshold_value")
    private BigDecimal thresholdValue;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;
}
