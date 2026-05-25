package io.mosip.authentication.common.service.cache;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.stereotype.Service;

import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.kernel.core.logger.spi.Logger;

@Service
public class CacheInspectorService {

    private static final Logger logger =
            IdaLogger.getLogger(CacheInspectorService.class);

    @Autowired(required = false)
    private CacheManager cacheManager;

    public void printAllCacheData() {

        if (cacheManager == null) {
            logger.warn(
                    IdAuthCommonConstants.IDA,
                    this.getClass().getSimpleName(),
                    "CACHE_DUMP",
                    "CacheManager not available"
            );
            return;
        }

        logger.info(
                IdAuthCommonConstants.IDA,
                this.getClass().getSimpleName(),
                "CACHE_DUMP",
                "Starting Cache Dump"
        );

        for (String cacheName : cacheManager.getCacheNames()) {

            Cache cache = cacheManager.getCache(cacheName);

            if (cache == null) {
                continue;
            }

            logger.info(
                    IdAuthCommonConstants.IDA,
                    this.getClass().getSimpleName(),
                    "CACHE_DUMP",
                    "Cache Name : " + cacheName
            );

            if (cache instanceof ConcurrentMapCache) {

                Map<Object, Object> nativeCache =
                        ((ConcurrentMapCache) cache).getNativeCache();

                nativeCache.forEach((key, value) ->
                        logger.info(
                                IdAuthCommonConstants.IDA,
                                this.getClass().getSimpleName(),
                                "CACHE_ENTRY",
                                key + " -> " + value
                        )
                );

            } else {

                logger.info(
                        IdAuthCommonConstants.IDA,
                        this.getClass().getSimpleName(),
                        "CACHE_DUMP",
                        "Provider : " + cache.getClass().getName()
                );
            }
        }

        logger.info(
                IdAuthCommonConstants.IDA,
                this.getClass().getSimpleName(),
                "CACHE_DUMP",
                "End Cache Dump"
        );
    }
}