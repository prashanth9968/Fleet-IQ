package com.fleetiq.alerts.service;

import com.fleetiq.alerts.entity.Geofence;
import com.fleetiq.alerts.repository.GeofenceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeofenceCacheService {

    private final GeofenceRepository geofenceRepository;
    
    // In-memory spatial index per tenant
    private final Map<UUID, STRtree> tenantSpatialIndexes = new ConcurrentHashMap<>();
    
    // In-memory quick lookup for Geofence metadata
    private final Map<UUID, Geofence> geofenceCache = new ConcurrentHashMap<>();

    private final GeometryFactory geometryFactory = new GeometryFactory();

    @PostConstruct
    public void init() {
        log.info("Initializing Geofence Spatial Cache from PostGIS...");
        List<Geofence> allGeofences = geofenceRepository.findAll();
        
        for (Geofence geofence : allGeofences) {
            addGeofenceToCache(geofence);
        }
        
        // Build the trees
        tenantSpatialIndexes.values().forEach(STRtree::build);
        
        log.info("Loaded {} geofences into in-memory spatial index.", allGeofences.size());
    }

    public void addGeofenceToCache(Geofence geofence) {
        geofenceCache.put(geofence.getId(), geofence);
        
        tenantSpatialIndexes.computeIfAbsent(geofence.getTenantId(), k -> new STRtree())
                .insert(geofence.getGeom().getEnvelopeInternal(), geofence);
    }

    public void refreshTenantCache(UUID tenantId) {
        log.info("Refreshing geofence cache for tenant: {}", tenantId);
        List<Geofence> tenantGeofences = geofenceRepository.findByTenantId(tenantId);
        
        STRtree newTree = new STRtree();
        for (Geofence g : tenantGeofences) {
            geofenceCache.put(g.getId(), g);
            newTree.insert(g.getGeom().getEnvelopeInternal(), g);
        }
        newTree.build();
        
        tenantSpatialIndexes.put(tenantId, newTree);
    }

    /**
     * Finds all geofences that intersect with the given coordinate.
     */
    public List<Geofence> findIntersectingGeofences(UUID tenantId, double latitude, double longitude) {
        STRtree tree = tenantSpatialIndexes.get(tenantId);
        if (tree == null) {
            return List.of();
        }

        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        Envelope searchEnvelope = point.getEnvelopeInternal();

        @SuppressWarnings("unchecked")
        List<Geofence> candidates = tree.query(searchEnvelope);

        List<Geofence> intersecting = new ArrayList<>();
        for (Geofence candidate : candidates) {
            if (candidate.getGeom().contains(point)) {
                intersecting.add(candidate);
            }
        }

        return intersecting;
    }

    public Geofence getGeofence(UUID geofenceId) {
        return geofenceCache.get(geofenceId);
    }
}
