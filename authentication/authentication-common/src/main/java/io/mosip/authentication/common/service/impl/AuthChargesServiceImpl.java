package io.mosip.authentication.common.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.authentication.common.service.entity.AuthCharges;
import io.mosip.authentication.common.service.repository.AuthChargesRepository;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.authentication.core.partner.dto.AuthChargesDTO;
import io.mosip.authentication.core.spi.authcharges.service.AuthChargesService;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * 
 * @author Sowmya
 *
 */
@Service
public class AuthChargesServiceImpl implements AuthChargesService {

	@Autowired
	private AuthChargesRepository authChargesRepository;

	/** The logger. */
	private static Logger logger = IdaLogger.getLogger(AuthChargesServiceImpl.class);

	@Override
	public List<AuthChargesDTO> findActiveAuthCharges() {

		List<AuthCharges> authCharges = authChargesRepository.findByIsActiveTrue();
		List<AuthChargesDTO> dtoList = authCharges.stream().map(this::fetchAuthChargesDTO)
				.collect(Collectors.toList());
		return dtoList;
	}

	public  AuthChargesDTO fetchAuthChargesDTO(AuthCharges authCharges) {
		AuthChargesDTO authChargesDTO = new AuthChargesDTO();
		authChargesDTO.setTypeCode(authCharges.getTypeCode());
		authChargesDTO.setSubTypeCode(authCharges.getSubTypeCode());
		authChargesDTO.setAmount(authCharges.getAmount());
		authChargesDTO.setEffectiveFrom(authCharges.getEffectiveFrom());
		authChargesDTO.setEffectiveTo(authCharges.getEffectiveTo());
		authChargesDTO.setActive(authCharges.isActive());
		return authChargesDTO;
	}

}
