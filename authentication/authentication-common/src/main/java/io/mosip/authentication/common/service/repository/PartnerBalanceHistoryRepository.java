package io.mosip.authentication.common.service.repository;

import org.springframework.stereotype.Repository;

import io.mosip.authentication.common.service.entity.PartnerBalanceHistory;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
/**
 * 
 * @author Jagadeesh
 *
 */
@Repository
public interface PartnerBalanceHistoryRepository extends BaseRepository<PartnerBalanceHistory, String> {


}