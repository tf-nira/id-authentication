package io.mosip.authentication.common.service.cache;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

public class CacheInspectorService {

    @Autowired
    private CacheManager cacheManager;

    public void printAllCacheData() {
        System.out.println("--- Starting Cache Dump ---");
        // Iterate through all cache names
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                System.out.println("\nCache Name: " + cacheName);
                // For ConcurrentMapCache (Default)
                if (cache instanceof ConcurrentMapCache) {
                    Map<Object, Object> nativeCache = ((ConcurrentMapCache) cache).getNativeCache();
                    nativeCache.forEach((key, value) -> {
                        System.out.println("  Key: " + key + " | Value: " + value);
                    });
                } else {
                    // For other providers (Redis, Caffeine), you may need to
                    // cast to their specific native implementations.
                    System.out.println("  [Notice] Native inspection not implemented for " + cache.getClass().getName());
                }
            }
        }
        System.out.println("\n--- End of Cache Dump ---");
    }
}