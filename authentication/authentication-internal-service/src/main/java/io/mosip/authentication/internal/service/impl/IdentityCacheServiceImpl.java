package io.mosip.authentication.internal.service.impl;

import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.IDA_ZERO_KNOWLEDGE_UNENCRYPTED_CREDENTIAL_ATTRIBUTES;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.authentication.common.service.repository.IdentityCacheRepository;
import io.mosip.authentication.common.service.transaction.manager.IdAuthSecurityManager;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.constant.IdAuthenticationErrorConstants;
import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.core.indauth.dto.IdType;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.authentication.internal.service.dto.IdentityCacheResponseDTO;
import io.mosip.authentication.internal.service.spi.IdentityCacheService;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;

/**
 * Reads the identity record cached in the ida.identity_cache table for a NIN
 * and returns the demographic attributes after zero-knowledge decryption.
 *
 * Only demographic data is read - the biometric column is never loaded, so this
 * flow needs no biometric input and produces no biometric output.
 */
@Service
public class IdentityCacheServiceImpl implements IdentityCacheService {

    /** The logger. */
    private static Logger logger = IdaLogger.getLogger(IdentityCacheServiceImpl.class);

    private static final String GET_DECRYPTED_IDENTITY = "getDecryptedIdentity";

    /** Index of the columns selected by IdentityCacheRepository#findDemoDataById. */
    private static final int IDX_ID = 0;
    private static final int IDX_DEMO_DATA = 1;
    private static final int IDX_EXPIRY = 2;
    private static final int IDX_TXN_LIMIT = 3;
    private static final int IDX_TOKEN = 4;
    private static final int IDX_IS_DELETED = 9;

    @Autowired
    private IdentityCacheRepository identityRepo;

    @Autowired
    private IdAuthSecurityManager securityManager;

    @Autowired
    private ObjectMapper mapper;

    /** Attributes stored in plain text, i.e. not to be zk-decrypted. */
    @Value("${" + IDA_ZERO_KNOWLEDGE_UNENCRYPTED_CREDENTIAL_ATTRIBUTES + ":#{null}}")
    private String zkUnEncryptedCredAttribs;

    /*
     * (non-Javadoc)
     *
     * @see io.mosip.authentication.internal.service.spi.IdentityCacheService#
     * getDecryptedIdentity(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public IdentityCacheResponseDTO getDecryptedIdentity(String nin) throws IdAuthenticationBusinessException {
        String hashedId = hashId(nin);

        try {
            List<Object[]> demoData = identityRepo.findDemoDataById(hashedId);
            if (demoData == null || demoData.isEmpty()) {
                logger.error(IdAuthCommonConstants.SESSION_ID, this.getClass().getSimpleName(),
                        GET_DECRYPTED_IDENTITY, "Id not found in identity cache");
                throw new IdAuthenticationBusinessException(
                        IdAuthenticationErrorConstants.ID_NOT_AVAILABLE.getErrorCode(),
                        String.format(IdAuthenticationErrorConstants.ID_NOT_AVAILABLE.getErrorMessage(),
                                IdType.UIN.getType()));
            }

            Object[] data = demoData.get(0);

            if (data[IDX_IS_DELETED] instanceof Boolean && (Boolean) data[IDX_IS_DELETED]) {
                logger.error(IdAuthCommonConstants.SESSION_ID, this.getClass().getSimpleName(),
                        GET_DECRYPTED_IDENTITY, "Identity cache record is marked as deleted");
                throw new IdAuthenticationBusinessException(
                        IdAuthenticationErrorConstants.ID_NOT_AVAILABLE.getErrorCode(),
                        String.format(IdAuthenticationErrorConstants.ID_NOT_AVAILABLE.getErrorMessage(),
                                IdType.UIN.getType()));
            }

            LocalDateTime expiryTimestamp = toLocalDateTime(data[IDX_EXPIRY]);
            if (Objects.nonNull(expiryTimestamp)
                    && DateUtils.before(expiryTimestamp, DateUtils.getUTCCurrentDateTime())) {
                logger.error(IdAuthCommonConstants.SESSION_ID, this.getClass().getSimpleName(),
                        GET_DECRYPTED_IDENTITY, "Identity expired/deactivated/revoked/blocked");
                throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.UIN_DEACTIVATED_BLOCKED);
            }

            byte[] demographicData = (byte[]) data[IDX_DEMO_DATA];
            Map<String, String> demoDataMap = mapper.readValue(demographicData, Map.class);

            IdentityCacheResponseDTO response = new IdentityCacheResponseDTO();
            response.setIdHash(String.valueOf(data[IDX_ID]));
            response.setToken(Objects.nonNull(data[IDX_TOKEN]) ? String.valueOf(data[IDX_TOKEN]) : null);
            response.setExpiryTimestamp(expiryTimestamp);
            response.setTransactionLimit(Objects.nonNull(data[IDX_TXN_LIMIT])
                    ? Integer.parseInt(String.valueOf(data[IDX_TXN_LIMIT]))
                    : null);
            response.setIdentity(decryptConfiguredAttributes(nin, demoDataMap));
            return response;
        } catch (IOException | DataAccessException | TransactionException | JDBCConnectionException e) {
            logger.error(IdAuthCommonConstants.SESSION_ID, this.getClass().getSimpleName(), GET_DECRYPTED_IDENTITY,
                    ExceptionUtils.getStackTrace(e));
            throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.UNABLE_TO_PROCESS, e);
        }
    }

    /**
     * Hashes the id the same way the credential store does while caching it.
     *
     * @param nin the nin
     * @return the hashed id
     * @throws IdAuthenticationBusinessException the id authentication business
     *                                           exception
     */
    private String hashId(String nin) throws IdAuthenticationBusinessException {
        try {
            return securityManager.hash(nin);
        } catch (IdAuthenticationBusinessException e) {
            logger.error(IdAuthCommonConstants.SESSION_ID, this.getClass().getSimpleName(), GET_DECRYPTED_IDENTITY,
                    ExceptionUtils.getStackTrace(e));
            throw new IdAuthenticationBusinessException(
                    IdAuthenticationErrorConstants.ID_NOT_AVAILABLE.getErrorCode(),
                    String.format(IdAuthenticationErrorConstants.ID_NOT_AVAILABLE.getErrorMessage(),
                            IdType.UIN.getType()),
                    e);
        }
    }

    /**
     * Decrypts the attributes that were zk-encrypted while storing, leaving the
     * attributes configured as unencrypted untouched. Mirrors the behaviour of
     * IdServiceImpl so that the values returned here match what the auth flows see.
     *
     * @param id      the id used as the zk key identifier
     * @param dataMap the stored attribute map
     * @return the decrypted attribute map
     * @throws IdAuthenticationBusinessException if decryption fails
     */
    private Map<String, Object> decryptConfiguredAttributes(String id, Map<String, String> dataMap)
            throws IdAuthenticationBusinessException {
        List<String> zkUnEncryptedAttributes = getZkUnEncryptedAttributes().stream().map(String::toLowerCase)
                .collect(Collectors.toList());

        Map<Boolean, Map<String, String>> partitionedMap = dataMap.entrySet().stream()
                .collect(Collectors.partitioningBy(
                        entry -> !zkUnEncryptedAttributes.contains(entry.getKey().toLowerCase()),
                        Collectors.toMap(Entry::getKey, Entry::getValue)));

        Map<String, String> dataToDecrypt = partitionedMap.get(true);
        Map<String, String> plainData = partitionedMap.get(false);

        Map<String, String> decryptedData;
        try {
            decryptedData = dataToDecrypt.isEmpty() ? Map.of() : securityManager.zkDecrypt(id, dataToDecrypt);
        } catch (IdAuthenticationBusinessException e) {
            logger.error(IdAuthCommonConstants.SESSION_ID, this.getClass().getSimpleName(),
                    "decryptConfiguredAttributes", ExceptionUtils.getStackTrace(e));
            throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.UNABLE_TO_PROCESS, e);
        }

        Map<String, String> finalDataStr = new LinkedHashMap<>();
        finalDataStr.putAll(plainData);
        finalDataStr.putAll(decryptedData);

        Map<String, Object> identity = new LinkedHashMap<>();
        for (Entry<String, String> entry : finalDataStr.entrySet()) {
            identity.put(entry.getKey(), parseIfJson(entry.getValue()));
        }
        return identity;
    }

    /**
     * Converts the stored string value into a JSON node when it holds a JSON object
     * or array, so that the response is well formed instead of escaped strings.
     *
     * @param value the value
     * @return the parsed object, or the original value
     */
    private Object parseIfJson(String value) {
        if (Objects.isNull(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            try {
                return mapper.readValue(trimmed.getBytes(), Object.class);
            } catch (IOException e) {
                logger.error(IdAuthCommonConstants.SESSION_ID, this.getClass().getSimpleName(), "parseIfJson",
                        ExceptionUtils.getStackTrace(e));
                return value;
            }
        }
        return value;
    }

    /**
     * Gets the list of attributes that are not zk-encrypted, from config.
     *
     * @return the zk unencrypted attributes
     */
    private List<String> getZkUnEncryptedAttributes() {
        return Optional.ofNullable(zkUnEncryptedCredAttribs).stream().flatMap(str -> Stream.of(str.split(",")))
                .map(String::trim).filter(str -> !str.isEmpty()).collect(Collectors.toList());
    }

    /**
     * Converts the expiry timestamp column value to a LocalDateTime, tolerating the
     * different types the JPA provider may return for the projection.
     *
     * @param value the column value
     * @return the local date time, or null
     */
    private LocalDateTime toLocalDateTime(Object value) {
        if (Objects.isNull(value)) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        return LocalDateTime.parse(String.valueOf(value));
    }

}