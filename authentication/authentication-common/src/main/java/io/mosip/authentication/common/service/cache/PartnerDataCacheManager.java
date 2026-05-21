package io.mosip.authentication.common.service.cache;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import io.mosip.authentication.common.service.entity.ApiKeyData;
import io.mosip.authentication.common.service.entity.MispLicenseData;
import io.mosip.authentication.common.service.entity.OIDCClientData;
import io.mosip.authentication.common.service.entity.PartnerData;
import io.mosip.authentication.common.service.entity.PartnerMapping;
import io.mosip.authentication.common.service.entity.PolicyData;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Manages partner-related Spring caches: inspect keys/values, evict, and log summaries.
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
	 * Logs all cache names, keys, and summarized values for the given partner.
	 */
	public void logPartnerCacheState(String phase, String partnerId) {
		if (cacheManager == null) {
			logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "logPartnerCacheState",
					"phase=" + phase + ", partnerId=" + partnerId + ", cacheManager=not_available");
			return;
		}
		for (String cacheName : PARTNER_CACHE_NAMES) {
			Cache cache = cacheManager.getCache(cacheName);
			if (cache == null) {
				logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "logPartnerCacheState",
						"phase=" + phase + ", partnerId=" + partnerId + ", cache=" + cacheName + ", status=not_configured");
				continue;
			}
			Object nativeCache = cache.getNativeCache();
			if (nativeCache instanceof ConcurrentMap) {
				ConcurrentMap<?, ?> cacheMap = (ConcurrentMap<?, ?>) nativeCache;
				logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "logPartnerCacheState",
						"phase=" + phase + ", partnerId=" + partnerId + ", cache=" + cacheName + ", keyCount="
								+ cacheMap.size());
				cacheMap.forEach((key, value) -> logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(),
						"logPartnerCacheState",
						"phase=" + phase + ", partnerId=" + partnerId + ", cache=" + cacheName + ", key=" + key
								+ ", value=" + summarizeCacheValue(value, partnerId)));
			} else {
				logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "logPartnerCacheState",
						"phase=" + phase + ", partnerId=" + partnerId + ", cache=" + cacheName
								+ ", nativeCacheType=" + nativeCache.getClass().getSimpleName());
			}
		}
	}

	/**
	 * Clears all partner-related caches via CacheManager.
	 */
	public void evictAllPartnerCaches() {
		if (cacheManager == null) {
			logger.warn(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "evictAllPartnerCaches",
					"CacheManager not available; partner caches not evicted");
			return;
		}
		for (String cacheName : PARTNER_CACHE_NAMES) {
			Cache cache = cacheManager.getCache(cacheName);
			if (cache != null) {
				cache.clear();
			}
		}
		logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "evictAllPartnerCaches",
				"Evicted partner caches: " + String.join(", ", PARTNER_CACHE_NAMES));
	}

	/**
	 * Logs partner fields in a compact, audit-friendly format (no certificate payload).
	 */
	public void logPartnerDataSummary(String source, PartnerData partnerData) {
		if (partnerData == null) {
			logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "logPartnerDataSummary",
					"source=" + source + ", partnerData=null");
			return;
		}
		logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "logPartnerDataSummary",
				"source=" + source + ", partnerId=" + partnerData.getPartnerId() + ", partnerName="
						+ partnerData.getPartnerName() + ", partnerStatus=" + partnerData.getPartnerStatus()
						+ ", requiresPayment=" + partnerData.getRequiresPayment() + ", partnerAuthType="
						+ partnerData.getPartnerAuthType() + ", partnerGroup=" + partnerData.getPartnerGroup()
						+ ", isDeleted=" + partnerData.isDeleted() + ", certificatePresent="
						+ hasCertificate(partnerData));
	}

	private String summarizeCacheValue(Object value, String partnerId) {
		if (value == null) {
			return "null";
		}
		Object unwrapped = unwrapOptional(value);
		if (unwrapped instanceof PartnerData) {
			return formatPartnerData((PartnerData) unwrapped, "cache");
		}
		if (unwrapped instanceof PartnerMapping) {
			return formatPartnerMapping((PartnerMapping) unwrapped);
		}
		if (unwrapped instanceof PolicyData) {
			return formatPolicyData((PolicyData) unwrapped);
		}
		if (unwrapped instanceof ApiKeyData) {
			return formatApiKeyData((ApiKeyData) unwrapped);
		}
		if (unwrapped instanceof MispLicenseData) {
			return formatMispLicenseData((MispLicenseData) unwrapped);
		}
		if (unwrapped instanceof OIDCClientData) {
			return formatOidcClientData((OIDCClientData) unwrapped);
		}
		if (unwrapped instanceof Map) {
			return "Map(size=" + ((Map<?, ?>) unwrapped).size() + ")";
		}
		return unwrapped.getClass().getSimpleName() + "(partnerIdFilter=" + partnerId + ")";
	}

	private Object unwrapOptional(Object value) {
		if (value instanceof Optional) {
			Optional<?> optional = (Optional<?>) value;
			return optional.orElse(null);
		}
		return value;
	}

	private String formatPartnerData(PartnerData partnerData, String source) {
		return "PartnerData[" + source + ", partnerId=" + partnerData.getPartnerId() + ", partnerName="
				+ partnerData.getPartnerName() + ", partnerStatus=" + partnerData.getPartnerStatus()
				+ ", requiresPayment=" + partnerData.getRequiresPayment() + ", isDeleted="
				+ partnerData.isDeleted() + ", certificatePresent=" + hasCertificate(partnerData) + "]";
	}

	private String formatPartnerMapping(PartnerMapping mapping) {
		StringBuilder sb = new StringBuilder("PartnerMapping[partnerId=").append(mapping.getPartnerId())
				.append(", policyId=").append(mapping.getPolicyId()).append(", apiKeyId=").append(mapping.getApiKeyId())
				.append(", isDeleted=").append(mapping.isDeleted());
		if (Objects.nonNull(mapping.getPartnerData())) {
			sb.append(", cachedPartnerStatus=").append(mapping.getPartnerData().getPartnerStatus());
			sb.append(", cachedPartnerName=").append(mapping.getPartnerData().getPartnerName());
		}
		if (Objects.nonNull(mapping.getApiKeyData())) {
			sb.append(", apiKeyStatus=").append(mapping.getApiKeyData().getApiKeyStatus());
			sb.append(", apiKeyExpiresOn=").append(mapping.getApiKeyData().getApiKeyExpiresOn());
		}
		if (Objects.nonNull(mapping.getPolicyData())) {
			sb.append(", policyStatus=").append(mapping.getPolicyData().getPolicyStatus());
			sb.append(", policyName=").append(mapping.getPolicyData().getPolicyName());
		}
		return sb.append("]").toString();
	}

	private String formatPolicyData(PolicyData policyData) {
		return "PolicyData[policyId=" + policyData.getPolicyId() + ", policyName=" + policyData.getPolicyName()
				+ ", policyStatus=" + policyData.getPolicyStatus() + ", isDeleted=" + policyData.isDeleted() + "]";
	}

	private String formatApiKeyData(ApiKeyData apiKeyData) {
		return "ApiKeyData[apiKeyId=" + apiKeyData.getApiKeyId() + ", apiKeyStatus=" + apiKeyData.getApiKeyStatus()
				+ ", apiKeyExpiresOn=" + apiKeyData.getApiKeyExpiresOn() + ", isDeleted=" + apiKeyData.isDeleted()
				+ "]";
	}

	private String formatMispLicenseData(MispLicenseData mispLicenseData) {
		return "MispLicenseData[mispId=" + mispLicenseData.getMispId() + ", mispStatus="
				+ mispLicenseData.getMispStatus() + ", mispExpiresOn=" + mispLicenseData.getMispExpiresOn()
				+ ", isDeleted=" + mispLicenseData.isDeleted() + "]";
	}

	private String formatOidcClientData(OIDCClientData oidcClientData) {
		return "OIDCClientData[clientId=" + oidcClientData.getClientId() + ", clientName="
				+ oidcClientData.getClientName() + ", clientStatus=" + oidcClientData.getClientStatus()
				+ ", partnerId=" + oidcClientData.getPartnerId() + ", isDeleted=" + oidcClientData.isDeleted() + "]";
	}

	private boolean hasCertificate(PartnerData partnerData) {
		try {
			return Objects.nonNull(partnerData.getCertificateData())
					&& !partnerData.getCertificateData().isEmpty();
		} catch (Exception e) {
			return false;
		}
	}
}
