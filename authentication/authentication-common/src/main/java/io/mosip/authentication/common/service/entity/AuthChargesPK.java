package io.mosip.authentication.common.service.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author Sowmya
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthChargesPK implements Serializable {
	
	private static final long serialVersionUID = -5486043175814831027L;

	public String typeCode;
	
	public String subTypeCode;

	public LocalDateTime effectiveFrom;

}
