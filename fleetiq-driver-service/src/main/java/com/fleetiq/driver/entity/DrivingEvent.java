package com.fleetiq.driver.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driving_events")
@IdClass(DrivingEventId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrivingEvent {

    @Id
    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Id
    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "speed_kmh")
    private Double speedKmh;

    @Column(name = "speed_limit_kmh")
    private Double speedLimitKmh;

    @Column(name = "magnitude")
    private Double magnitude;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "road_name", length = 255)
    private String roadName;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
}
