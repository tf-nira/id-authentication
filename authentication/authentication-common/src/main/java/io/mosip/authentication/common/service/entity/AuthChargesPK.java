package io.mosip.authentication.common.service.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

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


	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AuthChargesPK))
			return false;
		AuthChargesPK that = (AuthChargesPK) o;
		return Objects.equals(typeCode, that.typeCode) && Objects.equals(subTypeCode, that.subTypeCode)
				&& Objects.equals(effectiveFrom, that.effectiveFrom);
	}

	@Override
	public int hashCode() {
		return Objects.hash(typeCode, subTypeCode, effectiveFrom);
	}

}
