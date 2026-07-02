package com.fleetiq.health.repository;

import com.fleetiq.health.entity.DtcLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DtcLibraryRepository extends JpaRepository<DtcLibrary, UUID> {
    Optional<DtcLibrary> findByCode(String code);
}
