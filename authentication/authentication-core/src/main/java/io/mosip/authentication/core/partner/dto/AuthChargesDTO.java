package io.mosip.authentication.core.partner.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 
 * @author Sowmya
 *
 */
@Data
public class AuthChargesDTO {

	private String typeCode;

	private String subTypeCode;

	private Double amount;

	private LocalDateTime effectiveFrom;

	private LocalDateTime effectiveTo;

	private boolean isActive;

}
