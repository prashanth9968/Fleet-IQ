package com.fleetiq.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@EnableIntegration
public class DeviceGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeviceGatewayApplication.class, args);
    }
}
