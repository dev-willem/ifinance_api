package com.willembergfilho.ifinance.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class CaffeineConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${spring.cache.caffeine.spec:maximumSize=500,expireAfterWrite=3600s}") String spec) {
        CaffeineCacheManager manager = new CaffeineCacheManager("indexRates");
        manager.setCaffeine(Caffeine.from(spec));
        return manager;
    }
}
