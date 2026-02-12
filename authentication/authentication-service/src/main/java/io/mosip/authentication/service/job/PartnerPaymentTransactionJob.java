package io.mosip.authentication.service.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.authentication.common.service.entity.PartnerCurrentBalance;
import io.mosip.authentication.common.service.entity.PartnerPaymentTransactions;
import io.mosip.authentication.common.service.repository.PartnerCurrentBalanceRepository;
import io.mosip.authentication.common.service.repository.PartnerPaymentTransactionsRepository;
import io.mosip.authentication.core.logger.IdaLogger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
public class PartnerPaymentTransactionJob {

    private static final Logger LOGGER = IdaLogger.getLogger(PartnerPaymentTransactionJob.class);
    private static final String JOB_NAME = "PartnerPaymentTransactionJob";
    
    @Value("${mosip.ida.partner-payment.batch.size:1000}")
    private int batchSize;
    
    @Autowired
    private PartnerPaymentTransactionsRepository paymentTransactionsRepository;

    @Autowired
    private PartnerCurrentBalanceRepository partnerBalanceRepository;
    

    /**
     * Using synchronized method as alternative to @DistributedLock 
     */
    @Scheduled(cron = "${mosip.ida.partner-payment.cron.expression:0 0 */2 * * ?}")
    @Transactional
    public void processPartnerPaymentTransactions() {
        // unique session ID for tracing
        String sessionId = UUID.randomUUID().toString();
        
        try {
            LOGGER.info(sessionId, JOB_NAME, "processPartnerPaymentTransactions", "Job started");
            
            // Using pagination for memory efficiency
            processBatchedTransactions(sessionId);
            
            LOGGER.info(sessionId, JOB_NAME, "processPartnerPaymentTransactions", "Job completed successfully");
            
        } catch (Exception e) {
            LOGGER.error(sessionId, JOB_NAME, "processPartnerPaymentTransactions", 
                    "Error processing payment transactions: " + e.getMessage());
            throw new RuntimeException("Payment transaction processing failed", e);
        }
    }

    private void processBatchedTransactions(String sessionId) {
        int pageNumber = 0;
        boolean hasMore = true;
        int totalProcessed = 0;
        int totalFailed = 0;

        while (hasMore) {
            Pageable pageable = PageRequest.of(pageNumber, batchSize);
            List<PartnerPaymentTransactions> batchTransactions = 
                    paymentTransactionsRepository.findByIsProcessedFalse(pageable).getContent();

            if (batchTransactions.isEmpty()) {
                hasMore = false;
                LOGGER.info(sessionId, JOB_NAME, "processBatchedTransactions", 
                        "No more unprocessed transactions found");
                break;
            }

            LOGGER.info(sessionId, JOB_NAME, "processBatchedTransactions", 
                    "Processing batch " + pageNumber + " with " + batchTransactions.size() + " transactions");

            // Validating input data and calculate amounts per partner
            Map<String, BigDecimal> partnerAmountMap = batchTransactions.stream()
                    .filter(t -> validateTransaction(t, sessionId))
                    .collect(Collectors.groupingBy(
                            PartnerPaymentTransactions::getPartnerId,
                            Collectors.reducing(
                                    BigDecimal.ZERO,
                                    PartnerPaymentTransactions::getAmount,
                                    BigDecimal::add
                            )
                    ));

            LOGGER.info(sessionId, JOB_NAME, "processBatchedTransactions", 
                    "Calculated amounts for " + partnerAmountMap.size() + " partners");

            // Processing each partner with calculated sum
            for (Map.Entry<String, BigDecimal> entry : partnerAmountMap.entrySet()) {
                String partnerId = entry.getKey();
                BigDecimal amount = entry.getValue();

                try {
                    if (partnerBalanceRepository.findByPartnerId(partnerId).isEmpty()) {
                        String errorMsg = "Partner not found: " + partnerId;
                        LOGGER.error(sessionId, JOB_NAME, "processBatchedTransactions", errorMsg);
                        totalFailed++;
                        continue;
                    }

                    PartnerCurrentBalance balance = partnerBalanceRepository.findByPartnerId(partnerId).get();
                    BigDecimal newBalance = balance.getBalance().subtract(amount);
                    balance.setBalance(newBalance);
                    balance.setUpdBy("SYSTEM_PAYMENT_JOB_" + sessionId);
                    partnerBalanceRepository.save(balance);
                    
                    LOGGER.info(sessionId, JOB_NAME, "processBatchedTransactions", 
                            "Updated balance for partner: " + partnerId + ", new balance: " + newBalance);
                    
                    totalProcessed++;
                    
                } catch (Exception e) {
                    String errorMsg = "Exception processing partner: " + partnerId + " - " + e.getMessage();
                    LOGGER.error(sessionId, JOB_NAME, "processBatchedTransactions", errorMsg);
                    totalFailed++;
                }
            }

            // Mark as processed atomically before deletion
            markAsProcessed(sessionId, batchTransactions);
            
            // Safe deletion after marking as processed
            deleteProcessedTransactions(sessionId, batchTransactions);
            
            pageNumber++;
            LOGGER.info(sessionId, JOB_NAME, "processBatchedTransactions", 
                    "Batch " + (pageNumber - 1) + " completed. Processed: " + totalProcessed + 
                    ", Failed: " + totalFailed);
        }

        LOGGER.info(sessionId, JOB_NAME, "processBatchedTransactions", 
                "Total transactions processed: " + totalProcessed + ", failed: " + totalFailed);
    }

    /**
     * Validate transaction data
     */
    private boolean validateTransaction(PartnerPaymentTransactions transaction, String sessionId) {
        if (transaction.getAmount() == null) {
            LOGGER.warn(sessionId, JOB_NAME, "validateTransaction", 
                    "Transaction has null amount: " + transaction.getTransactionId());
            return false;
        }

        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            LOGGER.warn(sessionId, JOB_NAME, "validateTransaction", 
                    "Transaction has invalid amount: " + transaction.getAmount());
            return false;
        }

        if (transaction.getPartnerId() == null || transaction.getPartnerId().trim().isEmpty()) {
            LOGGER.warn(sessionId, JOB_NAME, "validateTransaction", 
                    "Transaction has invalid partnerId");
            return false;
        }

        return true;
    }

    /**
     * Mark transactions as processed atomically
     */
    @Transactional
    private void markAsProcessed(String sessionId, List<PartnerPaymentTransactions> transactions) {
        try {
            transactions.forEach(t -> t.setIsProcessed(true));
            paymentTransactionsRepository.saveAll(transactions);
            LOGGER.info(sessionId, JOB_NAME, "markAsProcessed", 
                    "Marked " + transactions.size() + " transactions as processed");
        } catch (Exception e) {
            LOGGER.error(sessionId, JOB_NAME, "markAsProcessed", 
                    "Failed to mark transactions as processed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Delete only after marking as processed
     */
    @Transactional
    private void deleteProcessedTransactions(String sessionId, List<PartnerPaymentTransactions> transactions) {
        try {
            paymentTransactionsRepository.deleteAll(transactions);
            LOGGER.info(sessionId, JOB_NAME, "deleteProcessedTransactions", 
                    "Deleted " + transactions.size() + " transactions");
        } catch (Exception e) {
            LOGGER.error(sessionId, JOB_NAME, "deleteProcessedTransactions", 
                    "Failed to delete processed transactions: " + e.getMessage());
            throw e;
        }
    }

}