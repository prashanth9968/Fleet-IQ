package com.fleetiq.health.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "maintenance_predictions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenancePrediction {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(nullable = false, length = 100)
    private String component;

    @Column(name = "failure_probability", nullable = false)
    private BigDecimal failureProbability;

    @Column(name = "predicted_failure_date")
    private LocalDate predictedFailureDate;

    @Column(name = "predicted_failure_odometer")
    private BigDecimal predictedFailureOdometer;

    @Column(name = "confidence_level")
    private BigDecimal confidenceLevel;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features_used")
    private Map<String, Object> featuresUsed;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(nullable = false, length = 50)
    private String status;
}
