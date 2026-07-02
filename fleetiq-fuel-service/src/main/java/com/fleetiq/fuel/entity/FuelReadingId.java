package com.fleetiq.fuel.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class FuelReadingId implements Serializable {

    private UUID vehicleId;
    private OffsetDateTime recordedAt;

    public FuelReadingId() {
    }

    public FuelReadingId(UUID vehicleId, OffsetDateTime recordedAt) {
        this.vehicleId = vehicleId;
        this.recordedAt = recordedAt;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(OffsetDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FuelReadingId that = (FuelReadingId) o;
        return Objects.equals(vehicleId, that.vehicleId) &&
                Objects.equals(recordedAt, that.recordedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicleId, recordedAt);
    }
}
