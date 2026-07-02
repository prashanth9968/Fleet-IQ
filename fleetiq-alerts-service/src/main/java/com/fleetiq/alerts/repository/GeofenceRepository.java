package com.fleetiq.alerts.repository;

import com.fleetiq.alerts.entity.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, UUID> {
    List<Geofence> findByTenantId(UUID tenantId);
}
