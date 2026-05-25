package io.mosip.authentication.common.service.integration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.transaction.Transactional;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.authentication.common.service.entity.ApiKeyData;
import io.mosip.authentication.common.service.entity.MispLicenseData;
import io.mosip.authentication.common.service.entity.OIDCClientData;
import io.mosip.authentication.common.service.entity.PartnerBalanceHistory;
import io.mosip.authentication.common.service.entity.PartnerCurrentBalance;
import io.mosip.authentication.common.service.entity.PartnerData;
import io.mosip.authentication.common.service.entity.PartnerMapping;
import io.mosip.authentication.common.service.entity.PartnerPaymentTransactions;
import io.mosip.authentication.common.service.entity.PolicyData;
import io.mosip.authentication.common.service.repository.ApiKeyDataRepository;
import io.mosip.authentication.common.service.repository.MispLicenseDataRepository;
import io.mosip.authentication.common.service.repository.OIDCClientDataRepository;
import io.mosip.authentication.common.service.repository.PartnerBalanceHistoryRepository;
import io.mosip.authentication.common.service.repository.PartnerCurrentBalanceRepository;
import io.mosip.authentication.common.service.repository.PartnerDataRepository;
import io.mosip.authentication.common.service.repository.PartnerMappingRepository;
import io.mosip.authentication.common.service.repository.PartnerPaymentTransactionsRepository;
import io.mosip.authentication.common.service.repository.PolicyDataRepository;
import io.mosip.authentication.common.service.transaction.manager.IdAuthSecurityManager;
import io.mosip.authentication.common.service.websub.impl.PartnerPaymentStatusEventPublisher;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.constant.IdAuthenticationErrorConstants;
import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.authentication.core.partner.dto.MispPolicyDTO;
import io.mosip.authentication.core.partner.dto.OIDCClientDTO;
import io.mosip.authentication.core.partner.dto.PartnerCurrentBalanceDTO;
import io.mosip.authentication.core.partner.dto.PartnerPolicyResponseDTO;
import io.mosip.authentication.core.partner.dto.PolicyDTO;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.websub.model.EventModel;

/**
 * This class Partner Service Manager connects to partner service to validate
 * and get the policy file for a given partnerID, partner api key and misp
 * license key.
 *
 * @author Nagarjuna
 *
 */
@Component
@Transactional
public class PartnerServiceManager {

	private static final Logger logger = IdaLogger.getLogger(PartnerServiceManager.class);

	/** The Constant API_KEY_DATA. */
	private static final String API_KEY_DATA = "apiKeyData";

	/** The Constant PARTNER_DATA. */
	private static final String PARTNER_DATA = "partnerData";

	/** The Constant POLICY_DATA. */
	private static final String POLICY_DATA = "policyData";

	/** The Constant PARTNER_AMOUNT. */
	private static final String PARTNER_AMOUNT = "creditedAmount";

	/** The Constant AMOUNT_CREDITED. */
	private static final boolean AMOUNT_CREDITED = true;

	/** The Constant PARTNER_PRN. */
	private static final String PARTNER_PRN = "prnData";
	
	/** The Constant PARTNER_BALANCE_DATA. */
	private static final String PARTNER_BALANCE_DATA = "updatedBalanceData";
	
	/** The auth transaction status event publisher. */
	@Autowired
	private PartnerPaymentStatusEventPublisher partnerPaymentStatusEventPublisher;
	
	/** The Constant MISP_LICENSE_DATA. */
	private static final String MISP_LICENSE_DATA = "mispLicenseData";

	/** The Constant OIDC_CLIENT_DATA. */
	private static final String OIDC_CLIENT_DATA = "clientData";

	/** The partner mapping repo. */
	@Autowired
	private PartnerMappingRepository partnerMappingRepo;

	/** The partner data repo. */
	@Autowired
	private PartnerDataRepository partnerDataRepo;
	
	/** The partner data repo. */
	@Autowired
	private PartnerCurrentBalanceRepository partnerCurrentBalanceRepo;
	
	/** The partner data repo. */
	@Autowired
	private PartnerBalanceHistoryRepository partnerBalanceHistoryRepo;
	
	/** The policy data repo. */
	@Autowired
	private PolicyDataRepository policyDataRepo;

	/** The api key repo. */
	@Autowired
	private ApiKeyDataRepository apiKeyRepo;

	/** The misp lic data repo. */
	@Autowired
	private MispLicenseDataRepository mispLicDataRepo;

	@Autowired
	private OIDCClientDataRepository oidcClientDataRepo; 

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	/** The security manager. */
	@Autowired
	private IdAuthSecurityManager securityManager;

	@Autowired
	private PartnerPaymentTransactionsRepository partnerPaymentTransactionsRepository;

	@Autowired
	private CacheManager cacheManager;



	/**
	 * Validate and get policy.
	 *
	 * @param partnerId the partner id
	 * @param partner_api_key the partner api key
	 * @param misp_license_key the misp license key
	 * @param certificateNeeded the certificate needed
	 * @return the partner policy response DTO
	 * @throws IdAuthenticationBusinessException the id authentication business exception
	 */
	public PartnerPolicyResponseDTO validateAndGetPolicy(String partnerId, String partner_api_key, String misp_license_key,
									boolean certificateNeeded, String headerCertificateThumbprint, boolean certValidationNeeded) 
									throws IdAuthenticationBusinessException {
		Optional<PartnerMapping> partnerMappingDataOptional = partnerMappingRepo.findByPartnerIdAndApiKeyId(partnerId, partner_api_key);
		Optional<MispLicenseData> mispLicOptional = mispLicDataRepo.findByLicenseKey(misp_license_key);
		Optional<OIDCClientData> oidcClientData = oidcClientDataRepo.findByClientId(partner_api_key);
		logger.info(
				IdAuthCommonConstants.IDA,
				this.getClass().getSimpleName(),
				"CACHE_BEFORE_VALIDATION",
				"Printing cache contents before validation"
		);
		printAllCacheData();
		validatePartnerMappingDetails(partnerMappingDataOptional, mispLicOptional, headerCertificateThumbprint, certValidationNeeded, oidcClientData);
		PartnerPolicyResponseDTO response = new PartnerPolicyResponseDTO();
		PartnerMapping partnerMapping = partnerMappingDataOptional.get();
		PartnerData partnerData = partnerMapping.getPartnerData();
		PolicyData policyData = partnerMapping.getPolicyData();
		ApiKeyData apiKeyData = partnerMapping.getApiKeyData();
		MispLicenseData mispLicenseData = mispLicOptional.get();
		response.setPolicyId(policyData.getPolicyId());
		response.setPolicyName(policyData.getPolicyName());
		response.setPolicy(mapper.convertValue(policyData.getPolicy(), PolicyDTO.class));
		response.setPolicyDescription(policyData.getPolicyDescription());
		response.setPolicyStatus(policyData.getPolicyStatus().contentEquals("ACTIVE"));
		response.setPartnerId(partnerData.getPartnerId());
		response.setPartnerName(partnerData.getPartnerName());
		if (certificateNeeded) {
			response.setCertificateData(partnerData.getCertificateData());
		}
		response.setPolicyExpiresOn(policyData.getPolicyExpiresOn());
		if (Objects.nonNull(apiKeyData)) {
			response.setApiKeyExpiresOn(apiKeyData.getApiKeyExpiresOn());
		}
		response.setMispExpiresOn(mispLicenseData.getMispExpiresOn());

		String mispPolicyId = mispLicenseData.getPolicyId();
		if (Objects.nonNull(mispPolicyId)) {
			response.setMispPolicyId(mispPolicyId);
			Optional<PolicyData> mispPolicyDataOpt = policyDataRepo.findByPolicyId(mispPolicyId);
			if(mispPolicyDataOpt.isPresent()) {
				PolicyData mispPolicyData = mispPolicyDataOpt.get();
				response.setMispPolicy(mapper.convertValue(mispPolicyData.getPolicy(), MispPolicyDTO.class));
			}
		}
		if (oidcClientData.isPresent()){
			String[] authContextRefs = oidcClientData.get().getAuthContextRefs();
			String[] userClaims = oidcClientData.get().getUserClaims();
			response.setOidcClientDto(new OIDCClientDTO(authContextRefs, userClaims));
		}
		response.setRequiresPayment(partnerData.getRequiresPayment());
		return response;
	}

	/**
	 * Validate partner mapping details.
	 *
	 * @param partnerMappingDataOptional the partner mapping data optional
	 * @param mispLicOptional the misp lic optional
	 * @throws IdAuthenticationBusinessException the id authentication business exception
	 */
	private void validatePartnerMappingDetails(Optional<PartnerMapping> partnerMappingDataOptional,
											   Optional<MispLicenseData> mispLicOptional, String headerCertificateThumbprint, 
											   boolean certValidationNeeded, Optional<OIDCClientData> oidcClientData) throws IdAuthenticationBusinessException {
		if (partnerMappingDataOptional.isPresent() && !partnerMappingDataOptional.get().isDeleted()) {
			PartnerMapping partnerMapping = partnerMappingDataOptional.get();
			if (partnerMapping.getPartnerData().isDeleted()) {
				throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.PARTNER_NOT_REGISTERED.getErrorCode(),
						IdAuthenticationErrorConstants.PARTNER_NOT_REGISTERED.getErrorMessage());
			}
			if (!partnerMapping.getPartnerData().getPartnerStatus().contentEquals("ACTIVE")) {
				throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.PARTNER_DEACTIVATED.getErrorCode(),
						IdAuthenticationErrorConstants.PARTNER_DEACTIVATED.getErrorMessage());
			}
			if (partnerMapping.getPolicyData().isDeleted()) {
				throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.INVALID_POLICY_ID.getErrorCode(),
						IdAuthenticationErrorConstants.INVALID_POLICY_ID.getErrorMessage());
			}
			if (!partnerMapping.getPolicyData().getPolicyStatus().contentEquals("ACTIVE")) {
				throw new IdAuthenticationBusinessException(
						IdAuthenticationErrorConstants.PARTNER_POLICY_NOT_ACTIVE.getErrorCode(),
						IdAuthenticationErrorConstants.PARTNER_POLICY_NOT_ACTIVE.getErrorMessage());
			}
			if (partnerMapping.getPolicyData().getPolicyCommenceOn().isAfter(DateUtils.getUTCCurrentDateTime())
					|| partnerMapping.getPolicyData().getPolicyExpiresOn()
					.isBefore(DateUtils.getUTCCurrentDateTime())) {
				throw new IdAuthenticationBusinessException(
						IdAuthenticationErrorConstants.PARTNER_POLICY_NOT_ACTIVE.getErrorCode(),
						IdAuthenticationErrorConstants.PARTNER_POLICY_NOT_ACTIVE.getErrorMessage());
			}
			// Checking not null because in case of OIDC client id, API key data will not be available.
			if (Objects.nonNull(partnerMapping.getApiKeyData())) {
				if (partnerMapping.getApiKeyData().isDeleted()) {
					throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.PARTNER_NOT_REGISTERED.getErrorCode(),
							IdAuthenticationErrorConstants.PARTNER_NOT_REGISTERED.getErrorMessage());
				}
				if (!partnerMapping.getApiKeyData().getApiKeyStatus().contentEquals("ACTIVE")) {
					throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.PARTNER_DEACTIVATED.getErrorCode(),
							IdAuthenticationErrorConstants.PARTNER_DEACTIVATED.getErrorMessage());
				}
				if (partnerMapping.getApiKeyData().getApiKeyCommenceOn().isAfter(DateUtils.getUTCCurrentDateTime())
						|| partnerMapping.getApiKeyData().getApiKeyExpiresOn()
						.isBefore(DateUtils.getUTCCurrentDateTime())) {
					throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.PARTNER_API_EXPIRED.getErrorCode(),
							IdAuthenticationErrorConstants.PARTNER_API_EXPIRED.getErrorMessage());
				}
			} else {
				logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "OIDC_client_validation", 
					"Checking for OIDC client exists or not");
				if (!oidcClientData.isPresent()){
					logger.error(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "OIDC_client_validation", 
						"OIDC client mapping not found in DB: " + partnerMapping.getApiKeyData());
					throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.OIDC_CLIENT_NOT_FOUND.getErrorCode(),
							IdAuthenticationErrorConstants.OIDC_CLIENT_NOT_FOUND.getErrorMessage());
				}
				if (!oidcClientData.get().getClientStatus().contentEquals("ACTIVE")) {
					throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.OIDC_CLIENT_DEACTIVATED.getErrorCode(),
							IdAuthenticationErrorConstants.OIDC_CLIENT_DEACTIVATED.getErrorMessage());
				}
				if (oidcClientData.get().isDeleted()) {
					throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.OIDC_CLIENT_NOT_REGISTERED.getErrorCode(),
							IdAuthenticationErrorConstants.OIDC_CLIENT_NOT_REGISTERED.getErrorMessage());
				}
			}
			if (certValidationNeeded && Objects.nonNull(headerCertificateThumbprint)) {

				logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "signature-header-certificate", 
					"Header Certificate: " + headerCertificateThumbprint);
				boolean partnerCertMatched = isCertificateMatching(headerCertificateThumbprint, partnerMapping.getPartnerId());
				// Setting default value to true because mispPartner cert matching will be done after partner certificate match.
				// if partner certificate matches, not required to match misp certificate/idp certificate 
				boolean mispPartnerCertMatched = true;
				// check signature header certificate is matching with IdP service partner certificate before throwing error.
				if (!partnerCertMatched) {
					if (mispLicOptional.isPresent()) {
						MispLicenseData mispLicenseData = mispLicOptional.get();
						mispPartnerCertMatched = isCertificateMatching(headerCertificateThumbprint, mispLicenseData.getMispId());
					} else {
						// misp not present. throw partner cert not matched exception because first matching partner certificate and  
						// then validating misp details/idp service.
						// misp partner not found so setting mispPartnerCertMatched value to false.
						mispPartnerCertMatched = false;
					}
				}
				/*
				 Test Scenario's: (values for partnerCertMatched & mispPartnerCertMatched)
				 1 - partner certificate matches -> true & true => below if condition will not be satisfied. No Exception 
				 2 - partner certificate not matched and misp not found -> false & false => below if condition will be satisfied, throws exception
				 3 - partner certificate not matched and misp cert found and matched -> false & true => below if condition will not be satisfied. No Exception
				 4 - partner certificate not matched and misp cert found and not matched -> false & false => below if condition will be satisfied, throws exception
				 */
				if (!partnerCertMatched && !mispPartnerCertMatched) {
					throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.PARTNER_CERTIFICATE_NOT_MATCHED.getErrorCode(),
										IdAuthenticationErrorConstants.PARTNER_CERTIFICATE_NOT_MATCHED.getErrorMessage());
				}
			}
			if (mispLicOptional.isPresent()) {
				MispLicenseData mispLicenseData = mispLicOptional.get();
				if (mispLicenseData.isDeleted()) {
					throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.INVALID_LICENSEKEY.getErrorCode(),
							IdAuthenticationErrorConstants.INVALID_LICENSEKEY.getErrorMessage());
				}
				if (!mispLicenseData.getMispStatus().contentEquals("ACTIVE")) {
					throw new IdAuthenticationBusinessException(
							IdAuthenticationErrorConstants.LICENSEKEY_SUSPENDED.getErrorCode(),
							IdAuthenticationErrorConstants.LICENSEKEY_SUSPENDED.getErrorMessage());
				}
				if (mispLicenseData.getMispCommenceOn().isAfter(DateUtils.getUTCCurrentDateTime())) {
					// TODO need to throw different exception for misp not active
					throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.INVALID_LICENSEKEY.getErrorCode(),
							IdAuthenticationErrorConstants.INVALID_LICENSEKEY.getErrorMessage());
				}
				if (mispLicenseData.getMispExpiresOn().isBefore(DateUtils.getUTCCurrentDateTime())) {
					throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.LICENSEKEY_EXPIRED.getErrorCode(),
							IdAuthenticationErrorConstants.LICENSEKEY_EXPIRED.getErrorMessage());
				}
			} else {
				throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.INVALID_LICENSEKEY.getErrorCode(),
						IdAuthenticationErrorConstants.INVALID_LICENSEKEY.getErrorMessage());
			}
		} else {
			throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.PARTNER_NOT_REGISTERED.getErrorCode(),
					IdAuthenticationErrorConstants.PARTNER_NOT_REGISTERED.getErrorMessage());
		}
	}

	private boolean isCertificateMatching(String headerCertificateThumbprint, String partnerId) {
		Optional<PartnerData> partnerData = partnerDataRepo.findByPartnerId(partnerId);
		if (partnerData.isPresent()) {
			try {
				CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
				X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(
										new ByteArrayInputStream(partnerData.get().getCertificateData().getBytes()));
				String dbPartnerCertThumbprint = DigestUtils.sha256Hex(x509Cert.getEncoded()).toUpperCase();
				return headerCertificateThumbprint.equals(dbPartnerCertThumbprint);
			} catch (CertificateException e) {
				logger.warn(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "isCertificateMatching", 
					"Warn - Comparing header certificate with DB Certificate.");
			}
		}
		return false;
	}

	/**
	 * Handle api key approved.
	 *
	 * @param eventModel the event model
	 * @throws JsonParseException the json parse exception
	 * @throws JsonMappingException the json mapping exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@CacheEvict(value = { IdAuthCommonConstants.PARTNER_API_KEY_DATA,
			IdAuthCommonConstants.PARTNER_API_KEY_POLICY_ID_DATA, IdAuthCommonConstants.POLICY_DATA,
			IdAuthCommonConstants.PARTNER_DATA, IdAuthCommonConstants.MISP_LIC_DATA, IdAuthCommonConstants.OIDC_CLIENT_DATA },
			allEntries = true)
	public void handleApiKeyApproved(EventModel eventModel) throws JsonParseException, JsonMappingException, IOException {
		PartnerMapping mapping = new PartnerMapping();
		PartnerData partnerEventData = mapper.convertValue(eventModel.getEvent().getData().get(PARTNER_DATA),
				PartnerData.class);
		mapping.setPartnerId(partnerEventData.getPartnerId());
		partnerEventData.setCreatedBy(getCreatedBy(eventModel));
		partnerEventData.setCrDTimes(DateUtils.getUTCCurrentDateTime());
		ApiKeyData apiKeyEventData = mapper.convertValue(eventModel.getEvent().getData().get(API_KEY_DATA),
				ApiKeyData.class);
		mapping.setApiKeyId(apiKeyEventData.getApiKeyId());
		apiKeyEventData.setCreatedBy(getCreatedBy(eventModel));
		apiKeyEventData.setCrDTimes(DateUtils.getUTCCurrentDateTime());
		PolicyData policyEventData = mapper.convertValue(eventModel.getEvent().getData().get(POLICY_DATA),
				PolicyData.class);
		mapping.setPolicyId(policyEventData.getPolicyId());
		policyEventData.setCreatedBy(getCreatedBy(eventModel));
		policyEventData.setCrDTimes(DateUtils.getUTCCurrentDateTime());
		mapping.setCreatedBy(getCreatedBy(eventModel));
		mapping.setCrDTimes(DateUtils.getUTCCurrentDateTime());
		partnerDataRepo.save(partnerEventData);
		apiKeyRepo.save(apiKeyEventData);
		policyDataRepo.save(policyEventData);
		partnerMappingRepo.save(mapping);
	}
	


	/**
	 * Gets the created by.
	 *
	 * @param eventModel the event model
	 * @return the created by
	 */
	private String getCreatedBy(EventModel eventModel) {
		//Get user from session
		String user = securityManager.getUser();
		if (user == null || user.isEmpty()) {
			//Get publisher from event
			String publisher = eventModel.getPublisher();
			if (publisher == null || publisher.isEmpty()) {
				//return as created by IDA
				return IdAuthCommonConstants.IDA;
			} else {
				return publisher;
			}
		} else {
			return user;
		}
	}

	/**
	 * Handle api key updated.
	 *
	 * @param eventModel the event model
	 * @throws JsonParseException the json parse exception
	 * @throws JsonMappingException the json mapping exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@CacheEvict(value = { IdAuthCommonConstants.PARTNER_API_KEY_DATA,
			IdAuthCommonConstants.PARTNER_API_KEY_POLICY_ID_DATA, IdAuthCommonConstants.POLICY_DATA,
			IdAuthCommonConstants.PARTNER_DATA, IdAuthCommonConstants.MISP_LIC_DATA, IdAuthCommonConstants.OIDC_CLIENT_DATA },
			allEntries = true)
	public void handleApiKeyUpdated(EventModel eventModel)
			throws JsonParseException, JsonMappingException, IOException {
		ApiKeyData apiKeyEventData = mapper.convertValue(eventModel.getEvent().getData().get(API_KEY_DATA),
				ApiKeyData.class);
		Optional<ApiKeyData> apiKeyDataOptional = apiKeyRepo.findById(apiKeyEventData.getApiKeyId());
		if (apiKeyDataOptional.isPresent()) {
			ApiKeyData apiKeyData = apiKeyDataOptional.get();
			apiKeyData.setApiKeyCommenceOn(apiKeyEventData.getApiKeyCommenceOn());
			apiKeyData.setApiKeyExpiresOn(apiKeyEventData.getApiKeyExpiresOn());
			apiKeyData.setApiKeyStatus(apiKeyEventData.getApiKeyStatus());
			apiKeyData.setUpdatedBy(getCreatedBy(eventModel));
			apiKeyData.setUpdDTimes(DateUtils.getUTCCurrentDateTime());
			apiKeyRepo.save(apiKeyData);
		} else {
			apiKeyEventData.setCreatedBy(getCreatedBy(eventModel));
			apiKeyEventData.setCrDTimes(DateUtils.getUTCCurrentDateTime());
			apiKeyRepo.save(apiKeyEventData);
		}
	}

	/**
	 * Update partner data.
	 *
	 * @param eventModel the event model
	 */
	@CacheEvict(value = { IdAuthCommonConstants.PARTNER_API_KEY_DATA,
			IdAuthCommonConstants.PARTNER_API_KEY_POLICY_ID_DATA, IdAuthCommonConstants.POLICY_DATA,
			IdAuthCommonConstants.PARTNER_DATA, IdAuthCommonConstants.MISP_LIC_DATA, IdAuthCommonConstants.OIDC_CLIENT_DATA },
			allEntries = true)
	public void updatePartnerData(EventModel eventModel) {

		PartnerData partnerEventData =
				mapper.convertValue(
						eventModel.getEvent().getData().get(PARTNER_DATA),
						PartnerData.class);

		Optional<PartnerData> partnerDataOptional =
				partnerDataRepo.findById(partnerEventData.getPartnerId());

		if (partnerDataOptional.isPresent()) {
			PartnerData partnerData1 = partnerDataOptional.get();

		logger.info(
				IdAuthCommonConstants.IDA,
				this.getClass().getSimpleName(),
				"updatePartnerData",
				"Updating Partner Status -> PartnerId: "
						+ partnerData1.getPartnerId()
						+ ", Incoming Status: "
						+ partnerData1.getPartnerStatus()
		);
		}

		if (partnerDataOptional.isPresent()) {

			PartnerData partnerData = partnerDataOptional.get();

			partnerData.setPartnerName(partnerEventData.getPartnerName());
			partnerData.setCertificateData(partnerEventData.getCertificateData());
			partnerData.setPartnerStatus(partnerEventData.getPartnerStatus());
			partnerData.setRequiresPayment(partnerEventData.getRequiresPayment());
			partnerData.setPartnerAuthType(partnerEventData.getPartnerAuthType());
			partnerData.setPartnerGroup(partnerEventData.getPartnerGroup());
			partnerData.setUpdatedBy(getCreatedBy(eventModel));
			partnerData.setUpdDTimes(DateUtils.getUTCCurrentDateTime());

			partnerDataRepo.save(partnerData);

			logger.info(
					IdAuthCommonConstants.IDA,
					this.getClass().getSimpleName(),
					"updatePartnerData",
					"Partner Updated Successfully -> PartnerId: "
							+ partnerData.getPartnerId()
							+ ", Updated Status: "
							+ partnerData.getPartnerStatus()
			);

		} else {

			partnerEventData.setCreatedBy(getCreatedBy(eventModel));
			partnerEventData.setCrDTimes(DateUtils.getUTCCurrentDateTime());

			partnerDataRepo.save(partnerEventData);
		}

		// Manual eviction
		if (Objects.nonNull(cacheManager)) {

			logger.info(
					IdAuthCommonConstants.IDA,
					this.getClass().getSimpleName(),
					"CACHE_EVICT",
					"Starting cache eviction"
			);

			evictPartnerCaches();

			logger.info(
					IdAuthCommonConstants.IDA,
					this.getClass().getSimpleName(),
					"CACHE_EVICT",
					"Printing cache contents after eviction"
			);
		}
		printAllCacheData();
	}

	private void evictPartnerCaches() {

		String[] caches = {
				IdAuthCommonConstants.PARTNER_API_KEY_DATA,
				IdAuthCommonConstants.PARTNER_API_KEY_POLICY_ID_DATA,
				IdAuthCommonConstants.POLICY_DATA,
				IdAuthCommonConstants.PARTNER_DATA,
				IdAuthCommonConstants.MISP_LIC_DATA,
				IdAuthCommonConstants.OIDC_CLIENT_DATA
		};

		for (String cacheName : caches) {
			if (cacheManager.getCache(cacheName) != null) {
				Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
			}
		}

		logger.info(
				IdAuthCommonConstants.IDA,
				this.getClass().getSimpleName(),
				"CACHE_EVICT",
				"Partner caches evicted successfully"
		);
	}

	private void printAllCacheData() {

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

				if (nativeCache.isEmpty()) {

					logger.info(
							IdAuthCommonConstants.IDA,
							this.getClass().getSimpleName(),
							"CACHE_DUMP",
							"Cache is empty"
					);

				} else {

					nativeCache.forEach((key, value) ->
							logger.info(
									IdAuthCommonConstants.IDA,
									this.getClass().getSimpleName(),
									"CACHE_ENTRY",
									key + " -> " + value
							)
					);
				}

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

	/**
	 * Update policy data.
	 *
	 * @param eventModel the event model
	 */
	@CacheEvict(value = { IdAuthCommonConstants.PARTNER_API_KEY_DATA,
			IdAuthCommonConstants.PARTNER_API_KEY_POLICY_ID_DATA, IdAuthCommonConstants.POLICY_DATA,
			IdAuthCommonConstants.PARTNER_DATA, IdAuthCommonConstants.MISP_LIC_DATA, IdAuthCommonConstants.OIDC_CLIENT_DATA },
			allEntries = true)
	public void updatePolicyData(EventModel eventModel) {
		PolicyData policyEventData = mapper.convertValue(eventModel.getEvent().getData().get(POLICY_DATA), PolicyData.class);
		Optional<PolicyData> policyDataOptional = policyDataRepo.findById(policyEventData.getPolicyId());
		if (policyDataOptional.isPresent()) {
			PolicyData policyData = policyDataOptional.get();
			policyData.setUpdatedBy(getCreatedBy(eventModel));
			policyData.setUpdDTimes(DateUtils.getUTCCurrentDateTime());
			policyData.setPolicyId(policyEventData.getPolicyId());
			policyData.setPolicy(policyEventData.getPolicy());
			policyData.setPolicyName(policyEventData.getPolicyName());
			policyData.setPolicyStatus(policyEventData.getPolicyStatus());
			policyData.setPolicyDescription(policyEventData.getPolicyDescription());
			policyData.setPolicyCommenceOn(policyEventData.getPolicyCommenceOn());
			policyData.setPolicyExpiresOn(policyEventData.getPolicyExpiresOn());
			policyDataRepo.save(policyData);
		} else {
			policyEventData.setCreatedBy(getCreatedBy(eventModel));
			policyEventData.setCrDTimes(DateUtils.getUTCCurrentDateTime());
			policyDataRepo.save(policyEventData);
		}
	}

	/**
	 * Update partner Amount.
	 *
	 * @param eventModel the event model
	 */
	public void updatePartnerAmount(EventModel eventModel) {
	    Map<String, Object> eventData = eventModel.getEvent().getData();
		Double creditedAmount= (Double) eventData.get(PARTNER_AMOUNT);
	    PartnerCurrentBalance partnerEventData = mapper.convertValue(eventData.get(PARTNER_BALANCE_DATA), PartnerCurrentBalance.class);
		Optional<PartnerCurrentBalance> partnerDataOptional = partnerCurrentBalanceRepo
				.findById(partnerEventData.getPartnerId());
		String partnerPrn = mapper.convertValue(eventData.get(PARTNER_PRN), String.class);
		
	    logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "PARTNER_AMOUNT", 
				"Updating Partner Current Balance & History" + partnerEventData.getPartnerId() + ", Credited Amount : " + creditedAmount);
	    
	    if (partnerDataOptional.isPresent()) {
	        PartnerCurrentBalance partnerData = partnerDataOptional.get();
			    Double currentBalance = Optional.ofNullable(partnerData.getBalance())
					.orElse(0.0);
			    currentBalance += creditedAmount;
			    partnerData.setBalance(currentBalance);
	        partnerData.setUpdBy(getCreatedBy(eventModel));
	        partnerData.setUpdDTimes(DateUtils.getUTCCurrentDateTime());
			    partnerCurrentBalanceRepo.save(partnerData);
	    } else {
	        PartnerCurrentBalance newPartner = new PartnerCurrentBalance();
	        newPartner.setPartnerId(partnerEventData.getPartnerId());
	        newPartner.setBalance(creditedAmount);
	        newPartner.setCrBy(partnerEventData.getCrBy());
	        newPartner.setCrDTimes(DateUtils.getUTCCurrentDateTime());
			    partnerCurrentBalanceRepo.save(newPartner);
	    }
	    
	    PartnerBalanceHistory partnerBalance = new PartnerBalanceHistory();
	    partnerBalance.setTransactionId(partnerPrn);
	    partnerBalance.setPartnerId(partnerEventData.getPartnerId());
	    partnerBalance.setBalance(creditedAmount);
	    partnerBalance.setCrBy(partnerEventData.getCrBy());
	    partnerBalance.setCrDTimes(DateUtils.getUTCCurrentDateTime());
      partnerBalanceHistoryRepo.save(partnerBalance);
      partnerPaymentStatusEventPublisher.publishEvent(partnerPrn, creditedAmount, AMOUNT_CREDITED, partnerEventData.getPartnerId());
	}

	/**
	 * Update misp license data.
	 *
	 * @param eventModel the event model
	 */
	@CacheEvict(value = { IdAuthCommonConstants.PARTNER_API_KEY_DATA,
			IdAuthCommonConstants.PARTNER_API_KEY_POLICY_ID_DATA, IdAuthCommonConstants.POLICY_DATA,
			IdAuthCommonConstants.PARTNER_DATA, IdAuthCommonConstants.MISP_LIC_DATA, IdAuthCommonConstants.OIDC_CLIENT_DATA },
			allEntries = true)
	public void updateMispLicenseData(EventModel eventModel) {
		Map<String, Object> eventDataMap = eventModel.getEvent().getData();
		MispLicenseData mispLicenseEventData = mapper.convertValue(eventDataMap.get(MISP_LICENSE_DATA), MispLicenseData.class);
		PolicyData policyEventData = null;
		if (eventDataMap.containsKey(POLICY_DATA)) {
			// First Add/Update the Policy details
			updatePolicyData(eventModel);
			// Later adding the policy id for MISP partner. 
			policyEventData = mapper.convertValue(eventDataMap.get(POLICY_DATA), PolicyData.class);
			mispLicenseEventData.setPolicyId(policyEventData.getPolicyId());
		}

		Optional<MispLicenseData> mispLicenseDataOptional = mispLicDataRepo.findById(mispLicenseEventData.getMispId());
		if (mispLicenseDataOptional.isPresent()) {
			MispLicenseData mispLicenseData = mispLicenseDataOptional.get();
			mispLicenseData.setUpdatedBy(getCreatedBy(eventModel));
			mispLicenseData.setUpdDTimes(DateUtils.getUTCCurrentDateTime());
			mispLicenseData.setMispId(mispLicenseEventData.getMispId());
			mispLicenseData.setLicenseKey(mispLicenseEventData.getLicenseKey());
			mispLicenseData.setMispCommenceOn(mispLicenseEventData.getMispCommenceOn());
			mispLicenseData.setMispExpiresOn(mispLicenseEventData.getMispExpiresOn());
			mispLicenseData.setMispStatus(mispLicenseEventData.getMispStatus());
			mispLicenseData.setPolicyId(mispLicenseEventData.getPolicyId());
			mispLicDataRepo.save(mispLicenseData);
		} else {
			mispLicenseEventData.setCreatedBy(getCreatedBy(eventModel));
			mispLicenseEventData.setCrDTimes(DateUtils.getUTCCurrentDateTime());
			mispLicDataRepo.save(mispLicenseEventData);
		}
	}

	/**
	 * Add OIDC client data.
	 *
	 * @param eventModel the event model
	 */
	@CacheEvict(value = { IdAuthCommonConstants.PARTNER_API_KEY_DATA,
			IdAuthCommonConstants.PARTNER_API_KEY_POLICY_ID_DATA, IdAuthCommonConstants.POLICY_DATA,
			IdAuthCommonConstants.PARTNER_DATA, IdAuthCommonConstants.MISP_LIC_DATA, IdAuthCommonConstants.OIDC_CLIENT_DATA },
			allEntries = true)
	public void createOIDCClientData(EventModel eventModel) throws IdAuthenticationBusinessException {
		// OIDC client handling is different from API key.
		// For API key there is no update available, API key will always be created.
		Map<String, Object> eventDataMap = eventModel.getEvent().getData();
		
		// First Add/Update the Policy details
		PolicyData policyData = null;
		if (eventDataMap.containsKey(POLICY_DATA)) {
			updatePolicyData(eventModel);
			policyData = mapper.convertValue(eventDataMap.get(POLICY_DATA), PolicyData.class);
		} else {
			throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.POLICY_DATA_NOT_FOUND_EVENT_DATA.getErrorCode(),
				IdAuthenticationErrorConstants.POLICY_DATA_NOT_FOUND_EVENT_DATA.getErrorMessage());
		}
		// Second Add/Update the Partner details
		PartnerData partnerData = null;
		if (eventDataMap.containsKey(PARTNER_DATA)) {
			updatePartnerData(eventModel);
			partnerData = mapper.convertValue(eventDataMap.get(PARTNER_DATA), PartnerData.class);
		} else {
			throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.PARTNER_DATA_NOT_FOUND_EVENT_DATA.getErrorCode(),
					IdAuthenticationErrorConstants.PARTNER_DATA_NOT_FOUND_EVENT_DATA.getErrorMessage());
		}

		if (!eventDataMap.containsKey(OIDC_CLIENT_DATA)) {
			throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.OIDC_CLIENT_DATA_NOT_FOUND_EVENT_DATA.getErrorCode(),
					IdAuthenticationErrorConstants.OIDC_CLIENT_DATA_NOT_FOUND_EVENT_DATA.getErrorMessage());
		}

		OIDCClientData oidcClientEventData = mapper.convertValue(eventDataMap.get(OIDC_CLIENT_DATA), OIDCClientData.class);
		Optional<OIDCClientData> oidcClientDataOpt = oidcClientDataRepo.findByClientId(oidcClientEventData.getClientId());
		if (oidcClientDataOpt.isPresent()) {
			throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.OIDC_CLIENT_DATA_ALREADY_EXIST.getErrorCode(),
					IdAuthenticationErrorConstants.OIDC_CLIENT_DATA_ALREADY_EXIST.getErrorMessage());
		} else {
			oidcClientEventData.setCreatedBy(getCreatedBy(eventModel));
			oidcClientEventData.setCrDTimes(DateUtils.getUTCCurrentDateTime());
			oidcClientEventData.setPartnerId(partnerData.getPartnerId());
			oidcClientDataRepo.save(oidcClientEventData);
		}

		String partnerId = partnerData.getPartnerId();
		String policyId = policyData.getPolicyId();
		String oidcClientId = oidcClientEventData.getClientId();
		
		logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "OIDC_CLIENT_EVENT", 
				"Adding OIDC client mapping. Partner Id: " + partnerId + ", Policy Id: " + policyId + ", OIDC Clinet Id: " + oidcClientId);
		Optional<PartnerMapping> partnerMappingOpt = partnerMappingRepo.findByPartnerIdAndApiKeyIdAndPolicyId(partnerId, oidcClientId, policyId);
		if (!partnerMappingOpt.isPresent()) {
			// Adding the mapping only if it's not available in DB.
			PartnerMapping partnerMapping = new PartnerMapping();
			partnerMapping.setPartnerId(partnerId);
			partnerMapping.setPolicyId(policyId);
			partnerMapping.setApiKeyId(oidcClientId);
			partnerMapping.setCreatedBy(getCreatedBy(eventModel));
			partnerMapping.setCrDTimes(DateUtils.getUTCCurrentDateTime());
			partnerMappingRepo.save(partnerMapping);
		}
	}

	/**
	 * Update OIDC client data.
	 *
	 * @param eventModel the event model
	 */
	@CacheEvict(value = { IdAuthCommonConstants.PARTNER_API_KEY_DATA,
			IdAuthCommonConstants.PARTNER_API_KEY_POLICY_ID_DATA, IdAuthCommonConstants.POLICY_DATA,
			IdAuthCommonConstants.PARTNER_DATA, IdAuthCommonConstants.MISP_LIC_DATA, IdAuthCommonConstants.OIDC_CLIENT_DATA },
			allEntries = true)
	public void updateOIDCClientData(EventModel eventModel) throws IdAuthenticationBusinessException {
		Map<String, Object> eventDataMap = eventModel.getEvent().getData();
		
		// Policy Data will not be allowed to update after creation of OIDC Client.
		// Second Update the Partner details
		String partnerId = "";
		if (eventDataMap.containsKey(PARTNER_DATA)) {
			updatePartnerData(eventModel);
			PartnerData partnerData = mapper.convertValue(eventDataMap.get(PARTNER_DATA), PartnerData.class);
			partnerId = partnerData.getPartnerId();
		} 

		if (!eventDataMap.containsKey(OIDC_CLIENT_DATA)) {
			throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.OIDC_CLIENT_DATA_NOT_FOUND_EVENT_DATA.getErrorCode(),
					IdAuthenticationErrorConstants.OIDC_CLIENT_DATA_NOT_FOUND_EVENT_DATA.getErrorMessage());
		}

		OIDCClientData oidcClientEventData = mapper.convertValue(eventDataMap.get(OIDC_CLIENT_DATA), OIDCClientData.class);
		Optional<OIDCClientData> oidcClientDataOpt = oidcClientDataRepo.findByClientId(oidcClientEventData.getClientId());
		if (oidcClientDataOpt.isPresent()) {
			OIDCClientData oidcClientData = oidcClientDataOpt.get();
			String dbPartnerId = oidcClientData.getPartnerId();
			if (partnerId.length() != 0 && !partnerId.equals(dbPartnerId)) {
				throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.OIDC_CLIENT_DATA_INVALID_PARTNER.getErrorCode(),
					IdAuthenticationErrorConstants.OIDC_CLIENT_DATA_INVALID_PARTNER.getErrorMessage());
			}
			oidcClientData.setUpdatedBy(getCreatedBy(eventModel));
			oidcClientData.setUpdDTimes(DateUtils.getUTCCurrentDateTime());
			oidcClientData.setClientName(oidcClientEventData.getClientName());
			oidcClientData.setClientStatus(oidcClientEventData.getClientStatus());
			oidcClientData.setUserClaims(oidcClientEventData.getUserClaims());
			oidcClientData.setAuthContextRefs(oidcClientEventData.getAuthContextRefs());
			oidcClientData.setClientAuthMethods(oidcClientEventData.getClientAuthMethods());
			oidcClientDataRepo.save(oidcClientData);
		}

		logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "OIDC_CLIENT_EVENT", 
				"Updated OIDC client. OIDC Clinet Id: " + oidcClientEventData.getClientId());
	}

	public void addPartnerPaymentTransaction(String partnerId, Double amount)
			throws IdAuthenticationBusinessException {
		try {
		PartnerPaymentTransactions partnerPaymentTransactions = new PartnerPaymentTransactions();
		partnerPaymentTransactions.setTransactionId(UUID.randomUUID().toString());
		partnerPaymentTransactions.setPartnerId(partnerId);
		partnerPaymentTransactions.setLogDTimes(LocalDateTime.now());
		partnerPaymentTransactions.setAmount(amount);
		partnerPaymentTransactions.setIsProcessed(false);
		partnerPaymentTransactionsRepository.save(partnerPaymentTransactions);
		logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "PARTNER_PAYMENT",
				"Adding new partner payment transaction" + partnerId + ",  Amount : " + amount);
	} catch (Exception e) {
		throw new IdAuthenticationBusinessException(
				IdAuthenticationErrorConstants.DATABASE_ERROR.getErrorCode(),
				IdAuthenticationErrorConstants.DATABASE_ERROR.getErrorMessage());
	}

	}

	public PartnerCurrentBalanceDTO getPartnerCurrentBalance(String partnerId) {
		Optional<PartnerCurrentBalance> partnerDataOptional = partnerCurrentBalanceRepo
				.findById(partnerId);
		PartnerCurrentBalanceDTO partnerCurrentBalanceDTO = null;
	    if (partnerDataOptional.isPresent()) {
	        PartnerCurrentBalance partnerData = partnerDataOptional.get();
			partnerCurrentBalanceDTO = new PartnerCurrentBalanceDTO();
			partnerCurrentBalanceDTO.setPartnerId(partnerId);
			partnerCurrentBalanceDTO.setBalance(partnerData.getBalance());
		}
		return partnerCurrentBalanceDTO;

	}
}
