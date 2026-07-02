package com.fleetiq.driver.repository;

import com.fleetiq.driver.entity.DrivingEvent;
import com.fleetiq.driver.entity.DrivingEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DrivingEventRepository extends JpaRepository<DrivingEvent, DrivingEventId> {
}
