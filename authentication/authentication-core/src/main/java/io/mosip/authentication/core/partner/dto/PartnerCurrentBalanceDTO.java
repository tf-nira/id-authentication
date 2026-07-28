package io.mosip.authentication.core.partner.dto;

import lombok.Data;

/**
 * 
 * @author Sowmya
 *
 */
@Data
public class PartnerCurrentBalanceDTO {

	private String partnerId;

	private Double balance;
}
