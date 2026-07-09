package com.fleetiq.fuel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class})
@EnableScheduling
@EnableCaching
public class FuelServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FuelServiceApplication.class, args);
    }
}
