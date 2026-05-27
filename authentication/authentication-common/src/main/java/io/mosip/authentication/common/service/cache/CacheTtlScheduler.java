package io.mosip.authentication.common.service.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.mosip.authentication.common.service.websub.CacheUpdatingWebsubInitializer;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.kernel.core.logger.spi.Logger;

import java.util.Objects;

@Component
@Profile("!test")
public class CacheTtlScheduler {

    /** Cache TTL in days */
    @Value("${ida-cache-ttl-in-days:1}")
    private int cacheTtlInDays;

    private static final Logger logger =
            IdaLogger.getLogger(CacheTtlScheduler.class);

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired
    private CacheUpdatingWebsubInitializer cacheConfig;

    @Scheduled(
            fixedDelayString =
                    "#{1000}"
    )
    public void clearCachesByTTL() {

        if (!cacheConfig.isCacheTtlEnabled()) {
            return;
        }

        if (cacheManager == null) {
            logger.warn(
                    IdAuthCommonConstants.IDA,
                    getClass().getSimpleName(),
                    "CACHE_TTL",
                    "CacheManager unavailable"
            );
            return;
        }

        logger.info(
                IdAuthCommonConstants.IDA,
                getClass().getSimpleName(),
                "CACHE_TTL",
                "Starting scheduled cache eviction"
        );

        for (String cacheName : cacheManager.getCacheNames()) {

            if (cacheManager.getCache(cacheName) != null) {

                Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();

                logger.info(
                        IdAuthCommonConstants.IDA,
                        getClass().getSimpleName(),
                        "CACHE_EVICT",
                        "Evicted cache -> " + cacheName
                );
            }
        }
    }
}