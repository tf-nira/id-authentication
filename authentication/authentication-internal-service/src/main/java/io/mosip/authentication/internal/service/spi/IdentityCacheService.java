package io.mosip.authentication.internal.service.spi;

import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.internal.service.dto.IdentityCacheResponseDTO;

/**
 * Fetches the identity record cached in the IDA identity_cache table and
 * returns its demographic attributes in decrypted (plain) form.
 */
public interface IdentityCacheService {

    /**
     * Gets the decrypted demographic identity data cached against the given NIN.
     *
     * @param nin the National Identification Number
     * @return the decrypted identity data along with the cache record metadata
     * @throws IdAuthenticationBusinessException if the NIN is not present in the
     *                                           cache, the record has expired, or
     *                                           the data could not be decrypted
     */
    IdentityCacheResponseDTO getDecryptedIdentity(String nin) throws IdAuthenticationBusinessException;

}