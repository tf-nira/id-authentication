package io.mosip.authentication.otp.service.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
public class OTPCaffeineCacheConfig {
    @Value("${ida-cache-ttl:5m}")
    private String cacheTtl;
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        Duration ttl = DurationStyle.detectAndParse(cacheTtl);
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(ttl)
                        .maximumSize(10000)
        );
        return cacheManager;
    }
}