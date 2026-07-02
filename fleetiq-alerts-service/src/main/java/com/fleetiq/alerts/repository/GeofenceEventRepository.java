package com.fleetiq.alerts.repository;

import com.fleetiq.alerts.entity.GeofenceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GeofenceEventRepository extends JpaRepository<GeofenceEvent, UUID> {
}
