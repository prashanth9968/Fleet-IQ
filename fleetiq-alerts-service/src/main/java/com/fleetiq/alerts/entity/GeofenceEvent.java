package com.fleetiq.alerts.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "geofence_events")
@Getter
@Setter
public class GeofenceEvent {

    @Id
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "geofence_id", nullable = false)
    private UUID geofenceId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "speed_kmh")
    private Double speedKmh;

    @Column(name = "dwell_duration_seconds")
    private Integer dwellDurationSeconds;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
