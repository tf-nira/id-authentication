package io.mosip.authentication.common.service.controller;

import static io.mosip.authentication.core.constant.IdAuthCommonConstants.APIKEY_APPROVED;
import static io.mosip.authentication.core.constant.IdAuthCommonConstants.MISP_LICENSE_GENERATED;
import static io.mosip.authentication.core.constant.IdAuthCommonConstants.MISP_LICENSE_UPDATED;
import static io.mosip.authentication.core.constant.IdAuthCommonConstants.OIDC_CLIENT_CREATED;
import static io.mosip.authentication.core.constant.IdAuthCommonConstants.OIDC_CLIENT_UPDATED;
import static io.mosip.authentication.core.constant.IdAuthCommonConstants.PARTNER_API_KEY_UPDATED_EVENT_NAME;
import static io.mosip.authentication.core.constant.IdAuthCommonConstants.PARTNER_UPDATED_EVENT_NAME;
import static io.mosip.authentication.core.constant.IdAuthCommonConstants.POLICY_UPDATED_EVENT_NAME;
import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.IDA_WEBSUB_PARTNER_SERVICE_CALLBACK_SECRET;
import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.IDA_WEBSUB_TOPIC_PMP_MISP_LICENSE_GENERATED;
import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.IDA_WEBSUB_TOPIC_PMP_MISP_LICENSE_UPDATED;
import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.IDA_WEBSUB_TOPIC_PMP_OIDC_CLIENT_CREATED;
import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.IDA_WEBSUB_TOPIC_PMP_OIDC_CLIENT_UPDATED;
import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.IDA_WEBSUB_TOPIC_PMP_PARTNER_API_KEY_APPROVED;
import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.IDA_WEBSUB_TOPIC_PMP_PARTNER_API_KEY_UPDATED;
import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.IDA_WEBSUB_TOPIC_PMP_PARTNER_UPDATED;
import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.IDA_WEBSUB_TOPIC_PMP_POLICY_UPDATED;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.authentication.common.service.cache.PartnerDataCacheManager;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.websub.api.annotation.PreAuthenticateContentAndVerifyIntent;
import io.swagger.v3.oas.annotations.Hidden;

/**
 * WebSub callbacks that only evict partner caches (no DB updates).
 * Used by authentication-service and otp-service so their local caches stay in sync
 * when partner data is updated via internal-service.
 */
@Hidden
@RestController
public class PartnerCacheEvictController {

	private static final Logger logger = IdaLogger.getLogger(PartnerCacheEvictController.class);

	@Autowired
	private PartnerDataCacheManager partnerDataCacheManager;

	@PostMapping(value = "/callback/partnermanagement/" + APIKEY_APPROVED, consumes = "application/json")
	@PreAuthenticateContentAndVerifyIntent(secret = "${" + IDA_WEBSUB_PARTNER_SERVICE_CALLBACK_SECRET
			+ "}", callback = "${ida-websub-partner-service-apikey-approved-callback-relative-url}", topic = "${"
			+ IDA_WEBSUB_TOPIC_PMP_PARTNER_API_KEY_APPROVED + "}")
	public void handleApiKeyApprovedEvent(@RequestBody EventModel eventModel) {
		evictPartnerCaches(APIKEY_APPROVED);
	}

	@PostMapping(value = "/callback/partnermanagement/" + PARTNER_UPDATED_EVENT_NAME, consumes = "application/json")
	@PreAuthenticateContentAndVerifyIntent(secret = "${" + IDA_WEBSUB_PARTNER_SERVICE_CALLBACK_SECRET
			+ "}", callback = "${ida-websub-partner-service-partner-updated-callback-relative-url}", topic = "${"
			+ IDA_WEBSUB_TOPIC_PMP_PARTNER_UPDATED + "}")
	public void handlePartnerUpdated(@RequestBody EventModel eventModel) {
		evictPartnerCaches(PARTNER_UPDATED_EVENT_NAME);
	}

	@PostMapping(value = "/callback/partnermanagement/" + POLICY_UPDATED_EVENT_NAME, consumes = "application/json")
	@PreAuthenticateContentAndVerifyIntent(secret = "${" + IDA_WEBSUB_PARTNER_SERVICE_CALLBACK_SECRET
			+ "}", callback = "${ida-websub-partner-service-policy-updated-callback-relative-url}", topic = "${"
			+ IDA_WEBSUB_TOPIC_PMP_POLICY_UPDATED + "}")
	public void handlePolicyUpdated(@RequestBody EventModel eventModel) {
		evictPartnerCaches(POLICY_UPDATED_EVENT_NAME);
	}

	@PostMapping(value = "/callback/partnermanagement/" + PARTNER_API_KEY_UPDATED_EVENT_NAME, consumes = "application/json")
	@PreAuthenticateContentAndVerifyIntent(secret = "${" + IDA_WEBSUB_PARTNER_SERVICE_CALLBACK_SECRET
			+ "}", callback = "${ida-websub-partner-service-partner-api-key-updated-callback-relative-url}", topic = "${"
			+ IDA_WEBSUB_TOPIC_PMP_PARTNER_API_KEY_UPDATED + "}")
	public void handlePartnerApiKeyUpdated(@RequestBody EventModel eventModel) {
		evictPartnerCaches(PARTNER_API_KEY_UPDATED_EVENT_NAME);
	}

	@PostMapping(value = "/callback/partnermanagement/" + MISP_LICENSE_GENERATED, consumes = "application/json")
	@PreAuthenticateContentAndVerifyIntent(secret = "${" + IDA_WEBSUB_PARTNER_SERVICE_CALLBACK_SECRET
			+ "}", callback = "${ida-websub-partner-service-misp-license-generated-callback-relative-url}", topic = "${"
			+ IDA_WEBSUB_TOPIC_PMP_MISP_LICENSE_GENERATED + "}")
	public void handleMispLicenseGeneratedEvent(@RequestBody EventModel eventModel) {
		evictPartnerCaches(MISP_LICENSE_GENERATED);
	}

	@PostMapping(value = "/callback/partnermanagement/" + MISP_LICENSE_UPDATED, consumes = "application/json")
	@PreAuthenticateContentAndVerifyIntent(secret = "${" + IDA_WEBSUB_PARTNER_SERVICE_CALLBACK_SECRET
			+ "}", callback = "${ida-websub-partner-service-misp-license-updated-callback-relative-url}", topic = "${"
			+ IDA_WEBSUB_TOPIC_PMP_MISP_LICENSE_UPDATED + "}")
	public void handleMispUpdatedEvent(@RequestBody EventModel eventModel) {
		evictPartnerCaches(MISP_LICENSE_UPDATED);
	}

	@PostMapping(value = "/callback/partnermanagement/" + OIDC_CLIENT_CREATED, consumes = "application/json")
	@PreAuthenticateContentAndVerifyIntent(secret = "${" + IDA_WEBSUB_PARTNER_SERVICE_CALLBACK_SECRET
			+ "}", callback = "${ida-websub-partner-service-oidc-client-created-callback-relative-url}", topic = "${"
			+ IDA_WEBSUB_TOPIC_PMP_OIDC_CLIENT_CREATED + "}")
	public void handleOIDCClientCreatedEvent(@RequestBody EventModel eventModel) {
		evictPartnerCaches(OIDC_CLIENT_CREATED);
	}

	@PostMapping(value = "/callback/partnermanagement/" + OIDC_CLIENT_UPDATED, consumes = "application/json")
	@PreAuthenticateContentAndVerifyIntent(secret = "${" + IDA_WEBSUB_PARTNER_SERVICE_CALLBACK_SECRET
			+ "}", callback = "${ida-websub-partner-service-oidc-client-updated-callback-relative-url}", topic = "${"
			+ IDA_WEBSUB_TOPIC_PMP_OIDC_CLIENT_UPDATED + "}")
	public void handleOIDCClientUpdatedEvent(@RequestBody EventModel eventModel) {
		evictPartnerCaches(OIDC_CLIENT_UPDATED);
	}

	private void evictPartnerCaches(String eventName) {
		logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "evictPartnerCaches",
				eventName + " received - evicting partner caches on this service instance");
		partnerDataCacheManager.evictAllPartnerCaches();
	}
}
