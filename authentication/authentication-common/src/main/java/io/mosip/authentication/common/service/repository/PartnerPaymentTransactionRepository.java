package io.mosip.authentication.common.service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import io.mosip.authentication.common.service.entity.PartnerPaymentTransactions;


@Repository
public interface PartnerPaymentTransactionRepository extends JpaRepository<PartnerPaymentTransactions, String> {

	
	/**
     *Query only unprocessed transactions
     *Use pagination to avoid loading all records
     */
    Page<PartnerPaymentTransactions> findByIsProcessedFalse(Pageable pageable);
    
    /**
     * For batch fetching partner balances
     */
    @Query("SELECT COUNT(t) FROM PartnerPaymentTransaction t WHERE t.isProcessed = false")
    Long countUnprocessedTransactions();
    

}