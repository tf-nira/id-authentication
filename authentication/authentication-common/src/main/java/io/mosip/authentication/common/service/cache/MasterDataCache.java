package io.mosip.authentication.common.service.cache;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import io.mosip.authentication.common.service.factory.RestRequestFactory;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.constant.IdAuthenticationErrorConstants;
import io.mosip.authentication.core.constant.RestServicesConstants;
import io.mosip.authentication.core.exception.IDDataValidationException;
import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class MasterDataCache {

	private static final String MASTERDATA_TITLES = "masterdata/titles";
	private static final String MASTERDATA_TEMPLATES = "masterdata/templates";

	private static Logger logger =
			IdaLogger.getLogger(MasterDataCache.class);

	@Autowired
	private RestRequestFactory restFactory;

	@Autowired
	@Qualifier("withSelfTokenWebclient")
	private RestHelper restHelper;

	@Autowired(required = false)
	private CacheManager cacheManager;

	@Value("${ida-cache-ttl-in-days:1}")
	private int cacheTtlInMinutes;

	private LocalDateTime lastCacheClearTime =
			LocalDateTime.now();

	private void validateCacheTTL() {

		if (cacheManager == null || cacheTtlInMinutes <= 0) {
			return;
		}

		long elapsed =
				ChronoUnit.MINUTES.between(
						lastCacheClearTime,
						LocalDateTime.now()
				);

		if (elapsed < cacheTtlInMinutes) {
			return;
		}

		logger.info(
				IdAuthCommonConstants.IDA,
				getClass().getSimpleName(),
				"CACHE_TTL",
				"TTL expired. Clearing caches"
		);

		clearCache(MASTERDATA_TITLES);
		clearCache(MASTERDATA_TEMPLATES);

		lastCacheClearTime =
				LocalDateTime.now();
	}

	private void clearCache(String cacheName) {

		Cache cache =
				cacheManager.getCache(cacheName);

		if (cache != null) {

			cache.clear();

			logger.info(
					IdAuthCommonConstants.IDA,
					getClass().getSimpleName(),
					"CACHE_EVICT",
					"Cleared cache -> " + cacheName
			);
		}
	}

	@Cacheable(cacheNames = MASTERDATA_TITLES)
	public Map<String, Object> getMasterDataTitles()
			throws IdAuthenticationBusinessException {

		validateCacheTTL();

		try {

			return restHelper.requestSync(
					restFactory.buildRequest(
							RestServicesConstants.TITLE_SERVICE,
							null,
							Map.class
					));

		} catch (IDDataValidationException | RestServiceException e) {

			throw new IdAuthenticationBusinessException(
					IdAuthenticationErrorConstants.UNABLE_TO_PROCESS,
					e
			);
		}
	}

	@Cacheable(
			cacheNames = MASTERDATA_TEMPLATES,
			key = "#template"
	)
	public Map<String, Object> getMasterDataTemplate(
			String template
	) throws IdAuthenticationBusinessException {

		validateCacheTTL();

		try {

			RestRequestDTO request =
					restFactory.buildRequest(
							RestServicesConstants.ID_MASTERDATA_TEMPLATE_SERVICE_MULTILANG,
							null,
							Map.class
					);

			request.setUri(
					request.getUri()
							.replace("{code}", template)
			);

			return restHelper.requestSync(request);

		} catch (Exception e) {

			throw new IdAuthenticationBusinessException(
					IdAuthenticationErrorConstants.UNABLE_TO_PROCESS,
					e
			);
		}
	}

	@CacheEvict(
			value = MASTERDATA_TEMPLATES,
			allEntries = true
	)
	public void clearMasterDataTemplateCache(String template) {

		logger.info(
				IdAuthCommonConstants.SESSION_ID,
				this.getClass().getSimpleName(),
				"clearMasterDataTemplateCache",
				"masterdata cache cleared for template code: " + template
		);
	}

	@CacheEvict(
			value = MASTERDATA_TITLES,
			allEntries = true
	)
	public void clearMasterDataTitlesCache() {

		logger.info(
				IdAuthCommonConstants.IDA,
				getClass().getSimpleName(),
				"CACHE_CLEAR",
				"Titles cache cleared"
		);
	}
}