package io.mosip.authentication.common.service.repository;

import org.springframework.stereotype.Repository;

import io.mosip.authentication.common.service.entity.AuthCharges;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;

/**
 * 
 * @author Sowmya
 *
 */
@Repository
public interface AuthChargesRepository extends BaseRepository<AuthCharges, String> {


}
