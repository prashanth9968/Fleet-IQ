package com.fleetiq.health.repository;

import com.fleetiq.health.entity.VehicleHealthHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleHealthHistoryRepository extends JpaRepository<VehicleHealthHistory, UUID> {
    List<VehicleHealthHistory> findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(UUID vehicleId, Instant start, Instant end);
}
