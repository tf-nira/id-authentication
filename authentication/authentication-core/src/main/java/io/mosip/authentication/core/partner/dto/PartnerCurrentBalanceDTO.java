package io.mosip.authentication.core.partner.dto;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 
 * @author Sowmya
 *
 */
@Data
public class PartnerCurrentBalanceDTO {

	private String partnerId;

	private BigDecimal balance;
}
