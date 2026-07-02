package com.fleetiq.driver.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrivingEventId implements Serializable {
    private UUID vehicleId;
    private Instant eventAt;
}
