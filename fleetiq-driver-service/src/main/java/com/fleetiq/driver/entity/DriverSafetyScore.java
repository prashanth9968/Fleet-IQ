package com.fleetiq.driver.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_safety_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverSafetyScore {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "period_type", nullable = false, length = 50)
    private String periodType;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "overall_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "harsh_accel_score", precision = 5, scale = 2)
    private BigDecimal harshAccelScore;

    @Column(name = "harsh_brake_score", precision = 5, scale = 2)
    private BigDecimal harshBrakeScore;

    @Column(name = "harsh_corner_score", precision = 5, scale = 2)
    private BigDecimal harshCornerScore;

    @Column(name = "speeding_score", precision = 5, scale = 2)
    private BigDecimal speedingScore;

    @Column(name = "fatigue_score", precision = 5, scale = 2)
    private BigDecimal fatigueScore;

    @Column(name = "seatbelt_score", precision = 5, scale = 2)
    private BigDecimal seatbeltScore;

    @Column(name = "total_trips", nullable = false)
    private Integer totalTrips;

    @Column(name = "total_distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDistanceKm;

    @Column(name = "total_driving_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDrivingHours;

    @Column(name = "total_events", nullable = false)
    private Integer totalEvents;

    @Column(name = "fuel_efficiency_score", precision = 5, scale = 2)
    private BigDecimal fuelEfficiencyScore;

    @Column(name = "peer_percentile", precision = 5, scale = 2)
    private BigDecimal peerPercentile;

    @Column(name = "ai_predicted_fatigue_probability", precision = 5, scale = 4)
    private BigDecimal aiPredictedFatigueProbability;

    @Column(name = "ai_insurance_risk_score", precision = 5, scale = 2)
    private BigDecimal aiInsuranceRiskScore;

    @Column(name = "ai_accident_probability", precision = 5, scale = 4)
    private BigDecimal aiAccidentProbability;

    @Column(name = "coaching_suggestions", columnDefinition = "jsonb")
    private String coachingSuggestions;

    @Column(name = "trend_data", columnDefinition = "jsonb")
    private String trendData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
