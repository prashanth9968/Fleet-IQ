package com.fleetiq.alerts.repository;

import com.fleetiq.alerts.entity.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, UUID> {
    List<AlertHistory> findByVehicleIdAndStatus(UUID vehicleId, String status);
}
