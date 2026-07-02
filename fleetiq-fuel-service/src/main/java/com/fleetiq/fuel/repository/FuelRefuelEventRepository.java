package com.fleetiq.fuel.repository;

import com.fleetiq.fuel.entity.FuelRefuelEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FuelRefuelEventRepository extends JpaRepository<FuelRefuelEvent, UUID> {
}
