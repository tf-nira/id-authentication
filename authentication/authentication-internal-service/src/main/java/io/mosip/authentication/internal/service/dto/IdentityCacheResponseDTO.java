package io.mosip.authentication.internal.service.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The response payload of the identity cache retrieval API. It carries the
 * decrypted demographic attributes held in the <code>ida.identity_cache</code>
 * table for a given NIN, along with the non-sensitive metadata of the cached
 * record.
 *
 * Note: the raw NIN is deliberately not echoed back, and biometric data is not
 * part of this response.
 */
@Getter
@Setter
@NoArgsConstructor
public class IdentityCacheResponseDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	/** The hash of the NIN, which is the primary key of the identity cache record. */
	private String idHash;

	/** The token id associated with the identity. */
	private String token;

	/** The decrypted demographic attributes. */
	private Map<String, Object> identity;

	/** The expiry timestamp of the cached record, if any. */
	private LocalDateTime expiryTimestamp;

	/** The remaining transaction limit of the cached record, if any. */
	private Integer transactionLimit;

}
