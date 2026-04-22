package io.mosip.authentication.authtypelockfilter.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import io.mosip.authentication.authfilter.exception.IdAuthenticationFilterException;
import io.mosip.authentication.authfilter.spi.IMosipAuthFilter;
import io.mosip.authentication.common.service.impl.match.BioAuthType;
import io.mosip.authentication.common.service.util.AuthTypeUtil;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.constant.IdAuthenticationErrorConstants;
import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.core.indauth.dto.AuthRequestDTO;
import io.mosip.authentication.core.indauth.dto.IdentityInfoDTO;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.authentication.core.spi.authtype.status.service.AuthtypeStatusService;
import io.mosip.authentication.core.spi.indauth.match.AuthType;
import io.mosip.authentication.core.spi.indauth.match.IdInfoFetcher;
import io.mosip.authentication.core.spi.indauth.match.MatchType;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * The Class AuthTypeLockFilterImpl - implementation of auth filter for
 * validating AuthType locked/unlocked status for an individual in the
 * authentication request.
 * 
 * @author Loganathan Sekar
 */
public class AuthTypeLockFilterImpl implements IMosipAuthFilter {
	
	private static Logger logger = IdaLogger.getLogger(AuthTypeLockFilterImpl.class);
	
	@Autowired
	private AuthtypeStatusService authTypeStatusService;
	
	@Autowired
	private IdInfoFetcher idInfoFetcher;

	/**
	 * Inits the.
	 */
	public void init() throws IdAuthenticationFilterException {
	}

	/**
	 * Test method that executes predicate test condition on the given arguments
	 *
	 * @param authRequest  the auth request
	 * @param identityData the identity data
	 * @param properties   the properties
	 * @throws IdAuthenticationBusinessException 
	 */
	public void validate(AuthRequestDTO authRequest, Map<String, List<IdentityInfoDTO>> identityData,
			Map<String, Object> properties) throws IdAuthenticationFilterException {
		String token = (String)properties.get(IdAuthCommonConstants.TOKEN);
		logger.info("AuthTypeLockFilterImpl.validate - ========== AUTH VALIDATION START ==========");
		logger.info("AuthTypeLockFilterImpl.validate - Token from identity_entity: " + token);
		logger.info("AuthTypeLockFilterImpl.validate - Auth request type: " + authRequest.getRequestedAuthType());
		validateAuthTypeStatus(authRequest, token);
		logger.info("AuthTypeLockFilterImpl.validate - ========== AUTH VALIDATION END ==========");
	}
	
	private void validateAuthTypeStatus(AuthRequestDTO authRequestDTO, String token) throws IdAuthenticationFilterException {
		try {
			logger.info("AuthTypeLockFilterImpl.validateAuthTypeStatus - FETCHING lock status for token: " + token);
			List<AuthtypeStatus> authtypeStatusList = authTypeStatusService
					.fetchAuthtypeStatus(token);
			logger.info("AuthTypeLockFilterImpl.validateAuthTypeStatus - Got " + (authtypeStatusList != null ? authtypeStatusList.size() : 0) + " status records");
			if (Objects.nonNull(authtypeStatusList) && !authtypeStatusList.isEmpty()) {
				for (AuthtypeStatus authTypeStatus : authtypeStatusList) {
					logger.info("AuthTypeLockFilterImpl.validateAuthTypeStatus - CHECKING: authType=" + authTypeStatus.getAuthType() + 
						", subType=" + authTypeStatus.getAuthSubType() + ", locked=" + authTypeStatus.getLocked());
					validateAuthTypeStatus(authRequestDTO, authTypeStatus, authtypeStatusList);
				}
			} else {
				logger.info("AuthTypeLockFilterImpl.validateAuthTypeStatus - NO lock records found for token: " + token);
			}
		} catch (IdAuthenticationFilterException e) {
			logger.info("AuthTypeLockFilterImpl.validateAuthTypeStatus - THROWING AUTH_TYPE_LOCKED: " + e.getErrorCode());
			throw e;
		} catch (IdAuthenticationBusinessException e) {
			logger.error("AuthTypeLockFilterImpl.validateAuthTypeStatus - Error: " + e.getErrorCode() + ", " + e.getErrorText());
			throw new IdAuthenticationFilterException(e.getErrorCode(), e.getErrorText(), e.getCause());
		}
	}

	private void validateAuthTypeStatus(AuthRequestDTO authRequestDTO, AuthtypeStatus authTypeStatus,List<AuthtypeStatus> authtypeStatusList)
			throws IdAuthenticationFilterException {
		logger.debug("AuthTypeLockFilterImpl.validateAuthTypeStatus - Processing authType=" + authTypeStatus.getAuthType() + 
			", locked=" + authTypeStatus.getLocked());
		if (authTypeStatus.getLocked()) {
			logger.debug("AuthTypeLockFilterImpl.validateAuthTypeStatus - Auth type is LOCKED, checking request type");
			if (AuthTypeUtil.isDemo(authRequestDTO)
					&& authTypeStatus.getAuthType().equalsIgnoreCase(MatchType.Category.DEMO.getType())) {
				logger.info("AuthTypeLockFilterImpl.validateAuthTypeStatus - Throwing exception for locked DEMO");
				throw new IdAuthenticationFilterException(
						IdAuthenticationErrorConstants.AUTH_TYPE_LOCKED.getErrorCode(),
						String.format(IdAuthenticationErrorConstants.AUTH_TYPE_LOCKED.getErrorMessage(),
								MatchType.Category.DEMO.getType()));
			}

			else if (AuthTypeUtil.isBio(authRequestDTO)
					&& authTypeStatus.getAuthType().equalsIgnoreCase(MatchType.Category.BIO.getType())) {
				for (AuthType authType : BioAuthType.getSingleBioAuthTypes().toArray(s -> new AuthType[s])) {
					if (authType.getType().equalsIgnoreCase(authTypeStatus.getAuthSubType())) {
						if (authType.isAuthTypeEnabled(authRequestDTO, idInfoFetcher)) {
							logger.info("AuthTypeLockFilterImpl.validateAuthTypeStatus - Throwing exception for locked BIO-" + authType.getType());
							throw new IdAuthenticationFilterException(
									IdAuthenticationErrorConstants.AUTH_TYPE_LOCKED.getErrorCode(),
									String.format(IdAuthenticationErrorConstants.AUTH_TYPE_LOCKED.getErrorMessage(),
											MatchType.Category.BIO.getType() + "-" + authType.getType()));
						} else {
							break;
						}
					}
				}
			}

			else if (AuthTypeUtil.isOtp(authRequestDTO))
				if (authTypeStatus.getAuthType().equalsIgnoreCase(MatchType.Category.OTP.getType())
						&& (authTypeStatus.getAuthSubType() == null || authTypeStatus.getAuthSubType().isEmpty())) {
					logger.info("AuthTypeLockFilterImpl.validateAuthTypeStatus - Throwing exception for locked OTP");
					throw new IdAuthenticationFilterException(
							IdAuthenticationErrorConstants.AUTH_TYPE_LOCKED.getErrorCode(),
							String.format(IdAuthenticationErrorConstants.AUTH_TYPE_LOCKED.getErrorMessage(),
									MatchType.Category.OTP.getType()));
				} else {
					if (authTypeStatus.getAuthSubType() != null && !authTypeStatus.getAuthSubType().isEmpty()
							&& (authTypeStatus.getAuthSubType().equalsIgnoreCase(IdAuthCommonConstants.PHONE_NUMBER)
									|| authTypeStatus.getAuthSubType().equalsIgnoreCase(IdAuthCommonConstants.EMAIL))
							&& authTypeStatus.getLocked().equals(true)) {
						logger.debug("AuthTypeLockFilterImpl.validateAuthTypeStatus - Checking Email/Phone OTP lock");
						Optional<AuthtypeStatus> otherSubOtpTypeLocked = authtypeStatusList.stream()
								.filter(authTypeStatus1 -> authTypeStatus1.getAuthType() != null
										&& authTypeStatus1.getAuthType()
												.equalsIgnoreCase(MatchType.Category.OTP.getType())
										&& authTypeStatus1.getAuthSubType() != null
										&& !authTypeStatus1.getAuthSubType()
												.equalsIgnoreCase(authTypeStatus.getAuthSubType())
										&& authTypeStatus1.getLocked())
								.findAny();
						if (otherSubOtpTypeLocked.isPresent()) {
							logger.info("AuthTypeLockFilterImpl.validateAuthTypeStatus - Throwing exception for locked OTP (other subtype)");
							throw new IdAuthenticationFilterException(
									IdAuthenticationErrorConstants.AUTH_TYPE_LOCKED.getErrorCode(),
									String.format(IdAuthenticationErrorConstants.AUTH_TYPE_LOCKED.getErrorMessage(),
											MatchType.Category.OTP.getType()));
						}

					}
				}

			else if (AuthTypeUtil.isPin(authRequestDTO)
					&& authTypeStatus.getAuthType().equalsIgnoreCase(MatchType.Category.SPIN.getType())) {
				logger.info("AuthTypeLockFilterImpl.validateAuthTypeStatus - Throwing exception for locked PIN");
				throw new IdAuthenticationFilterException(
						IdAuthenticationErrorConstants.AUTH_TYPE_LOCKED.getErrorCode(),
						String.format(IdAuthenticationErrorConstants.AUTH_TYPE_LOCKED.getErrorMessage(),
								MatchType.Category.SPIN.getType()));
			}
		} else {
			logger.debug("AuthTypeLockFilterImpl.validateAuthTypeStatus - Auth type is NOT locked, allowing");
		}
	}
}
