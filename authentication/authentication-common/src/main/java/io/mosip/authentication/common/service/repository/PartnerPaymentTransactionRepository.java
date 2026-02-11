package io.mosip.authentication.common.service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import io.mosip.authentication.common.service.entity.PartnerPaymentTransaction;


@Repository
public interface PartnerPaymentTransactionRepository extends JpaRepository<PartnerPaymentTransaction, String> {

	
	/**
     *Query only unprocessed transactions
     *Use pagination to avoid loading all records
     */
    Page<PartnerPaymentTransaction> findByIsProcessedFalse(Pageable pageable);
    
    /**
     * For batch fetching partner balances
     */
    @Query("SELECT COUNT(t) FROM PartnerPaymentTransaction t WHERE t.isProcessed = false")
    Long countUnprocessedTransactions();
    
    /**
     * For cleanup of very old processed transactions (optional)
     */
    @Query(value = "DELETE FROM partner_payment_transactions WHERE is_processed = true AND log_dtimes < DATE_SUB(NOW(), INTERVAL 30 DAY)", nativeQuery = true)
    void deleteOldProcessedTransactions();

}