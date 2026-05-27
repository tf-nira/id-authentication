package io.mosip.authentication.common.service.websub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.util.StringUtils;

import java.util.Objects;

/**
 * The Class CacheUpdatingWebsubInitializer.
 * @author Loganathan Sekar
 */
@Component
public abstract class CacheUpdatingWebsubInitializer extends BaseIDAWebSubInitializer {

	/** The cache type. */
	@Value("${spring.cache.type:simple}")
	public String cacheType;

	/** Cache TTL in days */
	@Value("${ida-cache-ttl-in-days:1}")
	private int cacheTtlInDays;

    @Autowired
    private CacheManager cacheManager;


    /**
	 * Checks if is cache enabled.
	 *
	 * @return true, if is cache enabled
	 */
	protected boolean isCacheEnabled() {
		return !StringUtils.equalsIgnoreCase(cacheType, "none");
	}

	/**
	 * Checks whether scheduled cache eviction should run.
	 *
	 * value <= 0 → disabled
	 */
	protected boolean isCacheTtlEnabled() {
		return isCacheEnabled() && cacheTtlInDays > 0;
	}

	/**
	 * Returns configured cache TTL.
	 */
	protected int getCacheTtlInDays() {
		return cacheTtlInDays;
	}

	@Scheduled(
			//fixedDelayString = "#{${ida-cache-ttl-in-days:1} * 24 * 60 * 60 * 1000}" below in minutes.
			fixedDelayString = "#{${ida-cache-ttl-in-days:1} * 1000}"
	)
	public void clearCachesByTTL() {

		if (!isCacheTtlEnabled() || cacheManager == null) {
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
						"Cleared cache: " + cacheName
				);
			}
		}
	}
}
