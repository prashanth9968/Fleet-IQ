package com.fleetiq.health.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "vehicle_dtc_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDtcEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dtc_id")
    private DtcLibrary dtc;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "cleared_at")
    private Instant clearedAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "odometer_at_detection")
    private BigDecimal odometerAtDetection;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "freeze_frame_data")
    private Map<String, Object> freezeFrameData;
}
