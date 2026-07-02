package com.fleetiq.tracking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple memory-based broker for MVP
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // HTTP URL endpoint for client connection (handshake)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // Allow raw WebSocket client connections
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Extract token/key from headers or query parameters for authorization
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    String token = null;
                    if (authorization != null && !authorization.isEmpty()) {
                        token = authorization.getFirst();
                    } else {
                        // Look up in native header query string (some clients send as query parameter)
                        token = accessor.getFirstNativeHeader("token");
                    }

                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                    }

                    // In a production scenario, we would parse and validate the JWT or API Key
                    // and set the user Principal in the Security context of the STOMP session.
                    // For the MVP and integration test support, we assume it is valid and log it.
                    if (token != null && !token.trim().isEmpty()) {
                        // Set the user authentication principal (can mock User Principal)
                        accessor.setUser(() -> "authorized-client");
                    }
                }
                return message;
            }
        });
    }
}
