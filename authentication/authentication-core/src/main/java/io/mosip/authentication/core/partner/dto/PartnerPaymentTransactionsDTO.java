package io.mosip.authentication.core.partner.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 
 * @author Sowmya
 *
 */
@Data
public class PartnerPaymentTransactionsDTO {

	private String partnerId;

	private BigDecimal amount;

	private String transactionId;

	private LocalDateTime logDTimes;
}
