package io.mosip.authentication.common.service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author Sowmya
 *
 */
@NoArgsConstructor
@Data
@Table(name = "partner_payment_transactions", schema = "ida")
@Entity
public class PartnerPaymentTransactions {

	@Id
	@NotNull
	@Column(name = "transaction_id")
	private String transactionId;


	@NotNull
	@Column(name = "partner_id")
	private String partnerId;

	@NotNull
	@Column(name = "amount", precision = 19, scale = 2)
	private BigDecimal amount;


	@NotNull
	@Column(name = "log_dtimes")
	private LocalDateTime logDTimes;
}
