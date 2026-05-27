package io.mosip.authentication.common.service.websub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Value;
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
    public boolean isCacheTtlEnabled() {
		return isCacheEnabled() && cacheTtlInDays > 0;
	}

	protected int getCacheTtlInDays() {
		return cacheTtlInDays;
	}
}
