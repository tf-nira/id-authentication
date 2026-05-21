package io.mosip.authentication.common.service.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Central partner cache eviction via Spring Cache and CacheManager.
 */
@Component
public class PartnerDataCacheManager {

	private static final Logger logger = IdaLogger.getLogger(PartnerDataCacheManager.class);

	private static final String[] PARTNER_CACHE_NAMES = {
			IdAuthCommonConstants.PARTNER_API_KEY_DATA,
			IdAuthCommonConstants.PARTNER_API_KEY_POLICY_ID_DATA,
			IdAuthCommonConstants.POLICY_DATA,
			IdAuthCommonConstants.PARTNER_DATA,
			IdAuthCommonConstants.MISP_LIC_DATA,
			IdAuthCommonConstants.OIDC_CLIENT_DATA
	};

	@Autowired(required = false)
	private CacheManager cacheManager;

	/**
	 * Evicts all partner-related caches. Invoked after partner DB updates and from
	 * auth/otp WebSub cache-evict callbacks.
	 */
	@CacheEvict(value = {
			IdAuthCommonConstants.PARTNER_API_KEY_DATA,
			IdAuthCommonConstants.PARTNER_API_KEY_POLICY_ID_DATA,
			IdAuthCommonConstants.POLICY_DATA,
			IdAuthCommonConstants.PARTNER_DATA,
			IdAuthCommonConstants.MISP_LIC_DATA,
			IdAuthCommonConstants.OIDC_CLIENT_DATA }, allEntries = true)
	public void evictAllPartnerCaches() {
		if (cacheManager != null) {
			for (String cacheName : PARTNER_CACHE_NAMES) {
				Cache cache = cacheManager.getCache(cacheName);
				if (cache != null) {
					cache.clear();
				}
			}
		}
		logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "evictAllPartnerCaches",
				"Partner caches evicted: " + String.join(", ", PARTNER_CACHE_NAMES));
	}
}
