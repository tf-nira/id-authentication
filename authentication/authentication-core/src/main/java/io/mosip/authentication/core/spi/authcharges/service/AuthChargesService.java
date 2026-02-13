package io.mosip.authentication.core.spi.authcharges.service;

import java.util.List;

import io.mosip.authentication.core.partner.dto.AuthChargesDTO;

/**
 * 
 * @author Sowmya
 *
 */

public interface AuthChargesService {

	public List<AuthChargesDTO> findActiveAuthCharges();

}
