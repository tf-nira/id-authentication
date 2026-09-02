package io.mosip.authentication.internal.service.controller;

import java.util.UUID;

import io.mosip.authentication.core.exception.IDDataValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.authentication.common.service.helper.AuditHelper;
import io.mosip.authentication.core.constant.AuditEvents;
import io.mosip.authentication.core.constant.AuditModules;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.constant.IdAuthenticationErrorConstants;
import io.mosip.authentication.core.exception.IdAuthenticationAppException;
import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.core.indauth.dto.IdType;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.authentication.core.util.IdTypeUtil;
import io.mosip.authentication.internal.service.dto.IdentityCacheResponseDTO;
import io.mosip.authentication.internal.service.spi.IdentityCacheService;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The {@code IdentityCacheController} exposes the cached identity of an
 * individual, in decrypted form, against the given NIN.
 *
 * This is a demographic-only, read-only API: no biometric input is required and
 * no biometric data is returned, so the endpoint is not registered against any
 * of the auth/biometric filters in {@code InternalAuthFilterConfig}.
 */
@RestController
@Tag(name = "identity-cache-controller", description = "Identity Cache Controller")
public class IdentityCacheController {

	/** The logger. */
	private static Logger logger = IdaLogger.getLogger(IdentityCacheController.class);

	private static final String GET_CACHED_IDENTITY = "getCachedIdentity";

	private static final String NIN = "nin";

	@Autowired
	private IdentityCacheService identityCacheService;

	@Autowired
	private IdTypeUtil idTypeUtil;

	@Autowired
	private AuditHelper auditHelper;

	@Value("${ida.api.id.identity.cache:ida.identity.cache}")
	private String identityCacheApiId;

	@Value("${ida.api.version.identity.cache:1.0}")
	private String identityCacheApiVersion;

	/**
	 * Fetches the cached identity for the given NIN and returns the demographic
	 * attributes after decryption.
	 *
	 * @param nin the National Identification Number
	 * @return the response wrapper holding the decrypted identity
	 * @throws IdAuthenticationAppException if the NIN is invalid, not present in the
	 *                                      cache, or the data could not be decrypted
	 */
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostverifyidentity())")
	@GetMapping(path = "/cache/identity/{nin}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Retrieve Cached Identity", description = "Retrieves the decrypted demographic identity cached against the given NIN. No biometric data is required or returned.", tags = {
			"identity-cache-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Cached identity retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid NIN / Identity not available in cache", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "500", description = "Unable to process the request", content = @Content(schema = @Schema(hidden = true))) })
	public ResponseWrapper<IdentityCacheResponseDTO> getCachedIdentity(
			@Parameter(description = "National Identification Number", required = true) @PathVariable(NIN) String nin)
            throws IdAuthenticationAppException, IDDataValidationException {

		// Not storing the id hash in the audit entries, using a random reference
		// instead, consistent with the other internal read APIs.
		String auditRefId = UUID.randomUUID().toString();

		try {
			validateNin(nin);

			IdentityCacheResponseDTO identityCacheResponse = identityCacheService.getDecryptedIdentity(nin);

			auditHelper.audit(AuditModules.IDENTITY_CACHE, AuditEvents.RETRIEVE_IDENTITY_CACHE_REQUEST_RESPONSE,
					auditRefId, IdType.UIN, "identity cache retrieval status : true");

			ResponseWrapper<IdentityCacheResponseDTO> responseWrapper = new ResponseWrapper<>();
			responseWrapper.setId(identityCacheApiId);
			responseWrapper.setVersion(identityCacheApiVersion);
			responseWrapper.setResponse(identityCacheResponse);
			return responseWrapper;
		} catch (IdAuthenticationBusinessException e) {
			logger.error(IdAuthCommonConstants.SESSION_ID, this.getClass().getSimpleName(), GET_CACHED_IDENTITY,
					e.getErrorCode() + " - " + e.getErrorText());

			auditHelper.audit(AuditModules.IDENTITY_CACHE, AuditEvents.RETRIEVE_IDENTITY_CACHE_REQUEST_RESPONSE,
					auditRefId, IdType.UIN, e);

			throw new IdAuthenticationAppException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	/**
	 * Validates that the NIN is present and well formed before hitting the cache.
	 *
	 * @param nin the nin
	 * @throws IdAuthenticationBusinessException if the nin is missing or invalid
	 */
	private void validateNin(String nin) throws IdAuthenticationBusinessException {
		if (nin == null || nin.trim().isEmpty()) {
			throw new IdAuthenticationBusinessException(
					IdAuthenticationErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(IdAuthenticationErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(), NIN));
		}

		if (!idTypeUtil.validateUin(nin)) {
			throw new IdAuthenticationBusinessException(
					IdAuthenticationErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(IdAuthenticationErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), NIN));
		}
	}

}
