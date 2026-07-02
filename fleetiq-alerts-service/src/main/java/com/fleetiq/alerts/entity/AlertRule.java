package com.fleetiq.alerts.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "alert_rules")
@Getter
@Setter
public class AlertRule {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "priority_threshold")
    private String priorityThreshold;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "escalation_levels")
    private List<EscalationLevel> escalationLevels;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "channels")
    private List<String> channels;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
