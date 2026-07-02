package com.fleetiq.fuel.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CaffeineCache thresholdCache() {
        return new CaffeineCache("thresholdCache",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());
    }

    @Bean
    public CaffeineCache baselineCache() {
        return new CaffeineCache("baselineCache",
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .recordStats()
                        .build());
    }

    @Bean
    public CacheManager cacheManager(CaffeineCache thresholdCache, CaffeineCache baselineCache) {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(thresholdCache, baselineCache));
        return cacheManager;
    }
}
