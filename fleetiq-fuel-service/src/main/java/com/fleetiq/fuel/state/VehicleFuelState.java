package com.fleetiq.fuel.state;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

import lombok.Getter;
import lombok.Setter;

/**
 * Per-vehicle in-memory sliding window state object. NOT an entity — this is a
 * POJO held in a ConcurrentHashMap keyed by vehicleId. Tracks the latest known
 * telemetry values, sensor fault state, and a sliding window of recent fuel
 * readings for anomaly detection across multiple Kafka listener invocations.
 */
@Getter
@Setter
public class VehicleFuelState {

    private UUID vehicleId;
    private UUID tenantId;
    private UUID vehicleTypeId;

    // Latest known values
    private Double lastFuelLevel;
    private Double lastFuelRate;
    private Double lastSpeedKmh;
    private Integer lastEngineRpm;
    private Boolean lastIgnition;
    private OffsetDateTime lastUpdate;
    private Double lastOdometerKm;
    private Double lastLatitude;
    private Double lastLongitude;

    // High consumption tracking
    private OffsetDateTime highConsumptionStart;
    private int consecutiveHighReadings;

    // Idle burn tracking
    private OffsetDateTime idleStart;

    // Sensor fault tracking
    private Double stuckSensorValue;
    private OffsetDateTime stuckSensorSince;
    private boolean sensorFaultActive;

    // Sliding window of recent fuel levels for leak/theft detection
    private final ConcurrentLinkedDeque<FuelSnapshot> recentReadings = new ConcurrentLinkedDeque<>();

    public static final int MAX_WINDOW_SIZE = 120; // keep last 120 readings

    public void addReading(double fuelLevel, OffsetDateTime timestamp) {
        recentReadings.addLast(new FuelSnapshot(fuelLevel, timestamp));
        while (recentReadings.size() > MAX_WINDOW_SIZE) {
            recentReadings.removeFirst();
        }
    }

    public record FuelSnapshot(double fuelLevel, OffsetDateTime timestamp) {}
}
