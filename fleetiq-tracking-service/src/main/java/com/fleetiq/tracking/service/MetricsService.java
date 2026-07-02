package com.fleetiq.tracking.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class MetricsService {

    private final Counter telemetryReceivedCounter;
    private final Counter telemetryProcessedCounter;
    private final Counter telemetryFailedCounter;
    private final Timer gpsWriteTimer;

    public MetricsService(MeterRegistry registry) {
        this.telemetryReceivedCounter = Counter.builder("telemetry_received_total")
                .description("Total telemetry events received from Kafka")
                .register(registry);
        this.telemetryProcessedCounter = Counter.builder("telemetry_processed_total")
                .description("Total telemetry events successfully processed")
                .register(registry);
        this.telemetryFailedCounter = Counter.builder("telemetry_failed_total")
                .description("Total telemetry events that failed processing")
                .register(registry);
        this.gpsWriteTimer = Timer.builder("gps_write_latency_ms")
                .description("Time taken to write batch to TimescaleDB")
                .register(registry);
    }

    public void incrementReceived() {
        telemetryReceivedCounter.increment();
    }

    public void incrementProcessed(double count) {
        telemetryProcessedCounter.increment(count);
    }

    public void incrementFailed() {
        telemetryFailedCounter.increment();
    }

    public Timer getGpsWriteTimer() {
        return gpsWriteTimer;
    }
}
