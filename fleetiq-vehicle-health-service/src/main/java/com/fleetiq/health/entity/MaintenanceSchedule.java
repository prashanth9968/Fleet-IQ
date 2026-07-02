package com.fleetiq.health.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "maintenance_schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceSchedule {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "service_type", nullable = false, length = 100)
    private String serviceType;

    @Column(name = "custom_service_name", length = 150)
    private String customServiceName;

    @Column(name = "interval_km")
    private BigDecimal intervalKm;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "interval_engine_hours")
    private BigDecimal intervalEngineHours;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "last_service_odometer")
    private BigDecimal lastServiceOdometer;

    @Column(name = "last_service_engine_hours")
    private BigDecimal lastServiceEngineHours;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "next_due_odometer")
    private BigDecimal nextDueOdometer;

    @Column(name = "next_due_engine_hours")
    private BigDecimal nextDueEngineHours;

    @Column(nullable = false, length = 50)
    private String status;

    @Builder.Default
    @Column(name = "is_overdue", nullable = false)
    private boolean isOverdue = false;
}
