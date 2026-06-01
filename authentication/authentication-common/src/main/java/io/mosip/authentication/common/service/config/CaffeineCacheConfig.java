package io.mosip.authentication.common.service.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
public class CaffeineCacheConfig {
    @Value("${ida-cache-ttl:5m}")
    private String cacheTtl;


    private Duration getDuration() {

        String ttl = cacheTtl.trim().toLowerCase();

        if (ttl.endsWith("s")) {
            return Duration.ofSeconds(
                    Long.parseLong(ttl.replace("s", ""))
            );
        }

        if (ttl.endsWith("m")) {
            return Duration.ofMinutes(
                    Long.parseLong(ttl.replace("m", ""))
            );
        }

        if (ttl.endsWith("h")) {
            return Duration.ofHours(
                    Long.parseLong(ttl.replace("h", ""))
            );
        }

        throw new IllegalArgumentException(
                "Invalid TTL format : " + ttl
        );
    }
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(getDuration())
                        .maximumSize(10000)
        );
        return cacheManager;
    }
}
