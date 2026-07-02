package com.fleetiq.health.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "dtc_library")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtcLibrary {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 100)
    private String system;

    @Column(length = 50)
    private String severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "possible_causes", columnDefinition = "TEXT")
    private String possible_causes;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommended_action;

    @Column(name = "sae_standard", length = 100)
    private String saeStandard;
}
