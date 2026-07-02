package com.fleetiq.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fleet_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetReport {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "date_range_start", nullable = false)
    private Instant dateRangeStart;

    @Column(name = "date_range_end", nullable = false)
    private Instant dateRangeEnd;

    @Column(name = "format", nullable = false)
    private String format;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
