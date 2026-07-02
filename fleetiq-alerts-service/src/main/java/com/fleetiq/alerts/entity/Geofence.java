package com.fleetiq.alerts.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Geometry;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "geofences")
@Getter
@Setter
public class Geofence {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(columnDefinition = "geometry(Geometry,4326)", nullable = false)
    private Geometry geom;

    @Column(name = "max_speed_kmh")
    private Double maxSpeedKmh;

    @Column(name = "max_dwell_minutes")
    private Integer maxDwellMinutes;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
