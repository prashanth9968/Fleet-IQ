package com.fleetiq.health.repository;

import com.fleetiq.health.entity.VehicleHealthScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleHealthScoreRepository extends JpaRepository<VehicleHealthScore, UUID> {
    Optional<VehicleHealthScore> findByVehicleId(UUID vehicleId);
}
