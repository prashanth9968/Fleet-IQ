package com.fleetiq.tracking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "devices")
@Getter
@Setter
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @Column(unique = true)
    private String imei;

    @Column(name = "device_type", nullable = false)
    private String deviceType;

    private String manufacturer;

    private String model;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Column(name = "hardware_version")
    private String hardwareVersion;

    @Column(name = "sim_iccid", length = 22)
    private String simIccid;

    @Column(name = "sim_phone_number", length = 20)
    private String simPhoneNumber;

    private String protocol;

    @Column(nullable = false)
    private String status = "INACTIVE";

    @Column(name = "last_communication_at")
    private OffsetDateTime lastCommunicationAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String configuration = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
