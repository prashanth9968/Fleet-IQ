package com.fleetiq.fuel.repository;

import com.fleetiq.fuel.entity.FuelReading;
import com.fleetiq.fuel.entity.FuelReadingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FuelReadingRepository extends JpaRepository<FuelReading, FuelReadingId> {

    List<FuelReading> findAllByVehicleIdAndRecordedAtBetween(UUID vehicleId, OffsetDateTime start, OffsetDateTime end);

    Optional<FuelReading> findTopByVehicleIdOrderByRecordedAtDesc(UUID vehicleId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT vehicle_type_id FROM vehicles WHERE id = :vehicleId", nativeQuery = true)
    Optional<UUID> findVehicleTypeIdByVehicleId(@org.springframework.data.repository.query.Param("vehicleId") UUID vehicleId);
}
