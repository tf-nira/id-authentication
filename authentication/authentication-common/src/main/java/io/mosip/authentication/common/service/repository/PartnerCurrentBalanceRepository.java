package io.mosip.authentication.common.service.repository;

import org.springframework.stereotype.Repository;

import io.mosip.authentication.common.service.entity.PartnerCurrentBalance;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;

/**
 * 
 * @author Sowmya
 *
 */
@Repository
public interface PartnerCurrentBalanceRepository extends BaseRepository<PartnerCurrentBalance, String> {

}