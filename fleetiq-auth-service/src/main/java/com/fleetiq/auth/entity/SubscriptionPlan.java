package com.fleetiq.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    private String description;

    @Column(nullable = false)
    private String tier;

    @Column(name = "price_per_vehicle_monthly", nullable = false)
    private BigDecimal pricePerVehicleMonthly;

    @Column(name = "max_vehicles")
    private Integer maxVehicles;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_geofences")
    private Integer maxGeofences;

    @Column(columnDefinition = "jsonb")
    private String features;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
