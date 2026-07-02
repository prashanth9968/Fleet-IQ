package com.fleetiq.analytics.repository;

import com.fleetiq.analytics.entity.FleetReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FleetReportRepository extends JpaRepository<FleetReport, UUID> {
    List<FleetReport> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
