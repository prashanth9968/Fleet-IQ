package com.fleetiq.health.repository;

import com.fleetiq.health.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    List<WorkOrder> findByVehicleIdAndStatus(UUID vehicleId, String status);
}
