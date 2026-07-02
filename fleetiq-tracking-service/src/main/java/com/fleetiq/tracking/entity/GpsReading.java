package com.fleetiq.tracking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "gps_readings")
@IdClass(GpsReadingId.class)
@Getter
@Setter
public class GpsReading {

    @Id
    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Id
    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt = OffsetDateTime.now();

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "location", columnDefinition = "geography(Point, 4326)", insertable = false, updatable = false)
    private Point location;

    @Column(precision = 8, scale = 2)
    private BigDecimal altitude;

    @Column(name = "speed_kmh", precision = 6, scale = 2)
    private BigDecimal speedKmh;

    @Column(precision = 5, scale = 2)
    private BigDecimal heading;

    @Column(precision = 4, scale = 2)
    private BigDecimal hdop;

    @Column(name = "satellites")
    private Integer satellites;

    private Boolean ignition;

    @Column(name = "odometer_km", precision = 12, scale = 2)
    private BigDecimal odometerKm;

    @Column(name = "signal_strength")
    private Integer signalStrength;

    @Column(name = "is_buffered", nullable = false)
    private boolean isBuffered = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;
}
