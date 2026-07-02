package com.fleetiq.tracking.listener;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebSocketEventListener {

    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public WebSocketEventListener(MeterRegistry registry) {
        Gauge.builder("websocket_connections_active", activeConnections, AtomicInteger::get)
                .description("Number of active STOMP WebSocket connections")
                .register(registry);
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        activeConnections.incrementAndGet();
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        activeConnections.decrementAndGet();
    }

    public int getActiveConnections() {
        return activeConnections.get();
    }
}
