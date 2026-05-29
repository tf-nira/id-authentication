package io.mosip.authentication.service.config;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
public class ServiceCaffeineCacheConfig {

    @Value("${ida-cache-ttl-in-minutes:5}")
    private long cacheTtlInMinutes;

    @Bean
    public CacheManager cacheManager() {

        CaffeineCacheManager cacheManager =
                new CaffeineCacheManager();

        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(
                                cacheTtlInMinutes,
                                TimeUnit.MINUTES)
                        .maximumSize(10000)
        );

        return cacheManager;
    }
}
