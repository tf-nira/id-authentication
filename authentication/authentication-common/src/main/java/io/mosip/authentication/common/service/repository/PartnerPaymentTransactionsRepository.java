package io.mosip.authentication.common.service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import io.mosip.authentication.common.service.entity.PartnerPaymentTransactions;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;

/**
 * 
 * @author Sowmya
 *
 */
@Repository
public interface PartnerPaymentTransactionsRepository extends BaseRepository<PartnerPaymentTransactions, String> {
	/**
     *Query only unprocessed transactions
     *Use pagination to avoid loading all records
     */
    Page<PartnerPaymentTransactions> findByIsProcessedFalse(Pageable pageable);
    
}
