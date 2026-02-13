package io.mosip.authentication.common.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

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
	
	private volatile List<AuthChargesDTO> cachedAuthCharges = Collections.emptyList();

	/** The logger. */
	private static Logger logger = IdaLogger.getLogger(AuthChargesServiceImpl.class);

	@PostConstruct
	public void init() {
		reloadAuthCharges();
	}

	public void reloadAuthCharges() {
		// need to call this when we add more records to this table through api or
		// update this table
		logger.info("Reloading active auth charges...");

		List<AuthChargesDTO> updatedList = authChargesRepository.findByIsActiveTrue().stream()
				.map(this::fetchAuthChargesDTO).collect(Collectors.toList());


		cachedAuthCharges = Collections.unmodifiableList(updatedList);

		logger.info("Auth charges cache reloaded successfully.");
	}

	@Override
	public List<AuthChargesDTO> findActiveAuthCharges() {

		return cachedAuthCharges;
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
