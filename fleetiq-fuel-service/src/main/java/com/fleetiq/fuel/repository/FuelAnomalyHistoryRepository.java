package com.fleetiq.fuel.repository;

import com.fleetiq.fuel.entity.FuelAnomalyHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FuelAnomalyHistoryRepository extends JpaRepository<FuelAnomalyHistory, UUID> {

    Optional<FuelAnomalyHistory> findByVehicleIdAndAnomalyTypeAndStatus(UUID vehicleId, String anomalyType, String status);
}
