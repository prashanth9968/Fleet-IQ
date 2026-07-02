package com.fleetiq.tracking.service;

import com.fleetiq.tracking.dto.NormalizedTelemetry;

import java.util.List;

public interface TrackingService {
    void enqueue(NormalizedTelemetry telemetry);
    void processBatch(List<NormalizedTelemetry> batch);
    void clearQueue();
}
