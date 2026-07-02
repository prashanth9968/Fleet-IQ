package com.fleetiq.health.repository;

import com.fleetiq.health.entity.MaintenancePrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenancePredictionRepository extends JpaRepository<MaintenancePrediction, UUID> {
    List<MaintenancePrediction> findByVehicleIdAndStatus(UUID vehicleId, String status);
}
