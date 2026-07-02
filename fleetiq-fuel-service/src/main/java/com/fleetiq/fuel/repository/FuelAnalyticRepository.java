package com.fleetiq.fuel.repository;

import com.fleetiq.fuel.entity.FuelAnalytic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FuelAnalyticRepository extends JpaRepository<FuelAnalytic, UUID> {
}
