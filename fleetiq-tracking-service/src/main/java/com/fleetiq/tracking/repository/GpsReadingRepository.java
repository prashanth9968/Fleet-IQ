package com.fleetiq.tracking.repository;

import com.fleetiq.tracking.entity.GpsReading;
import com.fleetiq.tracking.entity.GpsReadingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface GpsReadingRepository extends JpaRepository<GpsReading, GpsReadingId> {

    List<GpsReading> findAllByVehicleIdAndRecordedAtBetween(UUID vehicleId, OffsetDateTime start, OffsetDateTime end);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO gps_readings (vehicle_id, recorded_at, tenant_id, device_id, latitude, longitude, altitude, speed_kmh, heading, hdop, satellites, ignition, odometer_km, signal_strength, is_buffered, metadata) " +
            "VALUES (:vehicleId, :recordedAt, :tenantId, :deviceId, :latitude, :longitude, :altitude, :speedKmh, :heading, :hdop, :satellites, :ignition, :odometerKm, :signalStrength, :isBuffered, CAST(:metadata AS jsonb)) " +
            "ON CONFLICT (vehicle_id, recorded_at) DO NOTHING", nativeQuery = true)
    int insertIgnoreConflict(
            @Param("vehicleId") UUID vehicleId,
            @Param("recordedAt") OffsetDateTime recordedAt,
            @Param("tenantId") UUID tenantId,
            @Param("deviceId") UUID deviceId,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("altitude") BigDecimal altitude,
            @Param("speedKmh") BigDecimal speedKmh,
            @Param("heading") BigDecimal heading,
            @Param("hdop") BigDecimal hdop,
            @Param("satellites") Integer satellites,
            @Param("ignition") Boolean ignition,
            @Param("odometerKm") BigDecimal odometerKm,
            @Param("signalStrength") Integer signalStrength,
            @Param("isBuffered") boolean isBuffered,
            @Param("metadata") String metadata
    );
}
