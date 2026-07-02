package com.fleetiq.analytics.repository;

import com.fleetiq.analytics.entity.DailyFleetReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DailyFleetReportRepository extends JpaRepository<DailyFleetReport, UUID> {
    List<DailyFleetReport> findByTenantIdAndDateBetweenOrderByDateAsc(UUID tenantId, LocalDate start, LocalDate end);
}
