package io.mosip.authentication.common.service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

	@Query("select pcb from PartnerCurrentBalance pcb where pcb.partnerId = :partnerId")
	Optional<PartnerCurrentBalance> findByPartnerId(@Param("partnerId") String partnerId);
	
	List<PartnerCurrentBalance> findByPartnerIdIn(List<String> partnerIds);

}