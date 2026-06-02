package io.mosip.authentication.otp.service.config;

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
    @Value("${ida-cache-maxsize:10000}")
    private int maxSize;
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(DurationStyle.detectAndParse(cacheTtl))
                        .maximumSize(maxSize)
        );
        return cacheManager;
    }
}