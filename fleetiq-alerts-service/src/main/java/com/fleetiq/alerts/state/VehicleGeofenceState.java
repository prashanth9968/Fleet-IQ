package com.fleetiq.alerts.state;

import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Coordinate;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class VehicleGeofenceState {
    private UUID vehicleId;
    private UUID tenantId;

    // Track which geofences the vehicle is currently inside
    private Set<UUID> currentGeofences = new HashSet<>();

    // Track entry time for dwell calculations
    private Map<UUID, OffsetDateTime> entryTimes = new HashMap<>();

    // Track the last event (ENTER/EXIT) for state changes
    private Map<UUID, String> lastEvents = new HashMap<>();

    // Track if a dwell alert has already been fired to prevent spam
    private Map<UUID, Boolean> dwellAlertFired = new HashMap<>();

    // Track previous coordinate for route deviation or trajectory analysis
    private Coordinate previousCoordinate;

    public void enterGeofence(UUID geofenceId, OffsetDateTime timestamp) {
        currentGeofences.add(geofenceId);
        entryTimes.put(geofenceId, timestamp);
        lastEvents.put(geofenceId, "ENTER");
        dwellAlertFired.put(geofenceId, false);
    }

    public void exitGeofence(UUID geofenceId, OffsetDateTime timestamp) {
        currentGeofences.remove(geofenceId);
        entryTimes.remove(geofenceId);
        lastEvents.put(geofenceId, "EXIT");
        dwellAlertFired.remove(geofenceId);
    }

    public boolean isInside(UUID geofenceId) {
        return currentGeofences.contains(geofenceId);
    }
}
