package io.mosip.authentication.common.service.cache;

import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.stereotype.Service;

@Service
public class CacheInspectorService {

    private final CacheManager cacheManager;

    public CacheInspectorService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void printAllCacheData() {

        System.out.println("--- Starting Cache Dump ---");

        if (cacheManager == null) {
            System.out.println("CacheManager bean not available");
            return;
        }

        for (String cacheName : cacheManager.getCacheNames()) {

            Cache cache = cacheManager.getCache(cacheName);

            if (cache == null) {
                continue;
            }

            System.out.println("\nCache Name: " + cacheName);

            if (cache instanceof ConcurrentMapCache) {

                Map<Object, Object> nativeCache =
                        ((ConcurrentMapCache) cache).getNativeCache();

                if (nativeCache.isEmpty()) {
                    System.out.println("  [EMPTY]");
                } else {
                    nativeCache.forEach((key, value) ->
                            System.out.println(
                                    "  Key: " + key
                                            + " | Value: " + value
                            ));
                }

            } else {

                System.out.println(
                        "  [Notice] Unsupported cache type: "
                                + cache.getClass().getName()
                );
            }
        }

        System.out.println("\n--- End of Cache Dump ---");
    }
}