package com.fleetiq.fuel.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleFuelStateTest {

    @Test
    @DisplayName("Sliding window caps at MAX_WINDOW_SIZE readings")
    void testSlidingWindowCapacity() {
        VehicleFuelState state = new VehicleFuelState();
        state.setVehicleId(UUID.randomUUID());
        state.setTenantId(UUID.randomUUID());

        OffsetDateTime t0 = OffsetDateTime.now().minusMinutes(200);

        // Add 150 readings (exceeds MAX_WINDOW_SIZE = 120)
        for (int i = 0; i < 150; i++) {
            state.addReading(50.0 + i * 0.1, t0.plusMinutes(i));
        }

        assertThat(state.getRecentReadings()).hasSize(VehicleFuelState.MAX_WINDOW_SIZE);
    }

    @Test
    @DisplayName("Sliding window maintains FIFO order — oldest readings evicted first")
    void testSlidingWindowFIFO() {
        VehicleFuelState state = new VehicleFuelState();
        OffsetDateTime t0 = OffsetDateTime.now().minusMinutes(200);

        for (int i = 0; i < 130; i++) {
            state.addReading(10.0 + i, t0.plusMinutes(i));
        }

        // Oldest should be index 10 (first 10 evicted to cap at 120)
        VehicleFuelState.FuelSnapshot oldest = state.getRecentReadings().getFirst();
        assertThat(oldest.fuelLevel()).isEqualTo(20.0); // 10.0 + 10

        VehicleFuelState.FuelSnapshot newest = state.getRecentReadings().getLast();
        assertThat(newest.fuelLevel()).isEqualTo(139.0); // 10.0 + 129
    }

    @Test
    @DisplayName("State tracks vehicleTypeId correctly")
    void testVehicleTypeIdTracking() {
        VehicleFuelState state = new VehicleFuelState();
        UUID vehicleTypeId = UUID.randomUUID();
        state.setVehicleTypeId(vehicleTypeId);
        assertThat(state.getVehicleTypeId()).isEqualTo(vehicleTypeId);
    }

    @Test
    @DisplayName("High consumption tracking fields work correctly")
    void testHighConsumptionTracking() {
        VehicleFuelState state = new VehicleFuelState();
        OffsetDateTime now = OffsetDateTime.now();

        state.setHighConsumptionStart(now);
        state.setConsecutiveHighReadings(5);

        assertThat(state.getHighConsumptionStart()).isEqualTo(now);
        assertThat(state.getConsecutiveHighReadings()).isEqualTo(5);
    }

    @Test
    @DisplayName("Sensor fault state fields work correctly")
    void testSensorFaultState() {
        VehicleFuelState state = new VehicleFuelState();

        state.setSensorFaultActive(true);
        state.setStuckSensorValue(50.0);
        state.setStuckSensorSince(OffsetDateTime.now());

        assertThat(state.isSensorFaultActive()).isTrue();
        assertThat(state.getStuckSensorValue()).isEqualTo(50.0);
        assertThat(state.getStuckSensorSince()).isNotNull();
    }
}
