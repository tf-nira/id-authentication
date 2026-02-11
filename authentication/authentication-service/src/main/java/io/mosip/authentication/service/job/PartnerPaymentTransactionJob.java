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
import io.mosip.authentication.common.service.entity.PartnerPaymentTransaction;
import io.mosip.authentication.common.service.entity.PaymentProcessingAudit;
import io.mosip.authentication.common.service.repository.PartnerCurrentBalanceRepository;
import io.mosip.authentication.common.service.repository.PartnerPaymentTransactionRepository;
import io.mosip.authentication.common.service.repository.PaymentProcessingAuditRepository;
import io.mosip.authentication.core.logger.IdaLogger;

import java.time.LocalDateTime;
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
    
    @Value("${mosip.ida.partner-payment.min-balance:0.0}")
    private Double minimumBalance;

    @Autowired
    private PartnerPaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PartnerCurrentBalanceRepository partnerBalanceRepository;
    
    @Autowired
    private PaymentProcessingAuditRepository auditRepository;

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
            recordAudit(sessionId, null, null, "FAILED", e.getMessage());
            notifyAdministrators("Payment Processing Job Failed", e, sessionId);
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
            List<PartnerPaymentTransaction> batchTransactions = 
                    paymentTransactionRepository.findByIsProcessedFalse(pageable).getContent();

            if (batchTransactions.isEmpty()) {
                hasMore = false;
                LOGGER.info(sessionId, JOB_NAME, "processBatchedTransactions", 
                        "No more unprocessed transactions found");
                break;
            }

            LOGGER.info(sessionId, JOB_NAME, "processBatchedTransactions", 
                    "Processing batch " + pageNumber + " with " + batchTransactions.size() + " transactions");

            // Batch fetch partner balances in single query
            List<String> partnerIds = batchTransactions.stream()
                    .map(PartnerPaymentTransaction::getPartnerId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<String, PartnerCurrentBalance> balanceMap = 
                    partnerBalanceRepository.findByPartnerIdIn(partnerIds).stream()
                    .collect(Collectors.toMap(PartnerCurrentBalance::getPartnerId, b -> b));

            // Validate input data
            Map<String, Double> partnerAmountMap = batchTransactions.stream()
                    .filter(t -> validateTransaction(t, sessionId))
                    .collect(Collectors.groupingBy(
                            PartnerPaymentTransaction::getPartnerId,
                            Collectors.summingDouble(PartnerPaymentTransaction::getAmount)
                    ));

            LOGGER.info(sessionId, JOB_NAME, "processBatchedTransactions", 
                    "Calculated amounts for " + partnerAmountMap.size() + " partners");

            // Process with validation and auditing
            for (Map.Entry<String, Double> entry : partnerAmountMap.entrySet()) {
                String partnerId = entry.getKey();
                Double amount = entry.getValue();

                try {
                    PartnerCurrentBalance balance = balanceMap.get(partnerId);
                    
                    if (balance == null) {
                        String errorMsg = "Partner not found: " + partnerId;
                        LOGGER.error(sessionId, JOB_NAME, "processBatchedTransactions", errorMsg);
                        recordAudit(sessionId, partnerId, amount, "FAILED_PARTNER_NOT_FOUND", errorMsg);
                        totalFailed++;
                        continue;
                    }

                    // Validate balance won't go below a threshold
                    Double newBalance = balance.getBalance() - amount;
                    if (newBalance < minimumBalance) {
                        String errorMsg = String.format(
                            "Insufficient balance for partner %s: Current=%.2f, Required=%.2f",
                            partnerId, balance.getBalance(), amount);
                        LOGGER.error(sessionId, JOB_NAME, "processBatchedTransactions", errorMsg);
                        recordAudit(sessionId, partnerId, amount, "FAILED_INSUFFICIENT_BALANCE", errorMsg);
                        totalFailed++;
                        continue;
                    }

                    // Perform update
                    balance.setBalance(newBalance);
                    balance.setUpdDtimes(LocalDateTime.now());
                    balance.setUpdBy("SYSTEM_PAYMENT_JOB_" + sessionId);
                    partnerBalanceRepository.save(balance);
                    
                    LOGGER.info(sessionId, JOB_NAME, "processBatchedTransactions", 
                            "Updated balance for partner: " + partnerId + ", new balance: " + newBalance);
                    
                    recordAudit(sessionId, partnerId, amount, "SUCCESS", null);
                    totalProcessed++;
                    
                } catch (Exception e) {
                    String errorMsg = "Exception processing partner: " + partnerId + " - " + e.getMessage();
                    LOGGER.error(sessionId, JOB_NAME, "processBatchedTransactions", errorMsg);
                    recordAudit(sessionId, partnerId, amount, "FAILED_EXCEPTION", errorMsg);
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
    private boolean validateTransaction(PartnerPaymentTransaction transaction, String sessionId) {
        if (transaction.getAmount() == null) {
            LOGGER.warn(sessionId, JOB_NAME, "validateTransaction", 
                    "Transaction has null amount: " + transaction.getTransactionId());
            recordAudit(sessionId, transaction.getPartnerId(), null, "SKIPPED_NULL_AMOUNT", 
                    "Amount is null");
            return false;
        }

        if (transaction.getAmount() <= 0) {
            LOGGER.warn(sessionId, JOB_NAME, "validateTransaction", 
                    "Transaction has invalid amount: " + transaction.getAmount());
            recordAudit(sessionId, transaction.getPartnerId(), transaction.getAmount(), 
                    "SKIPPED_INVALID_AMOUNT", "Amount is not positive");
            return false;
        }

        if (transaction.getPartnerId() == null || transaction.getPartnerId().trim().isEmpty()) {
            LOGGER.warn(sessionId, JOB_NAME, "validateTransaction", 
                    "Transaction has invalid partnerId");
            recordAudit(sessionId, null, transaction.getAmount(), "SKIPPED_INVALID_PARTNER_ID", 
                    "Partner ID is null or empty");
            return false;
        }

        return true;
    }

    /**
     * Mark transactions as processed atomically
     */
    @Transactional
    private void markAsProcessed(String sessionId, List<PartnerPaymentTransaction> transactions) {
        try {
            transactions.forEach(t -> t.setIsProcessed(true));
            paymentTransactionRepository.saveAll(transactions);
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
    private void deleteProcessedTransactions(String sessionId, List<PartnerPaymentTransaction> transactions) {
        try {
            paymentTransactionRepository.deleteAll(transactions);
            LOGGER.info(sessionId, JOB_NAME, "deleteProcessedTransactions", 
                    "Deleted " + transactions.size() + " transactions");
        } catch (Exception e) {
            LOGGER.error(sessionId, JOB_NAME, "deleteProcessedTransactions", 
                    "Failed to delete processed transactions: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Record audit trail for all operations
     */
    private void recordAudit(String sessionId, String partnerId, Double amount, 
                            String status, String errorMessage) {
        try {
            PaymentProcessingAudit audit = new PaymentProcessingAudit();
            audit.setAuditId(UUID.randomUUID().toString());
            audit.setSessionId(sessionId);
            audit.setPartnerId(partnerId);
            audit.setAmount(amount);
            audit.setStatus(status);
            audit.setErrorMessage(errorMessage);
            audit.setProcessedDtimes(LocalDateTime.now());
            auditRepository.save(audit);
        } catch (Exception e) {
            LOGGER.error(sessionId, JOB_NAME, "recordAudit", 
                    "Failed to record audit: " + e.getMessage());
        }
    }

    private void notifyAdministrators(String subject, Exception exception, String sessionId) {
        try {
            // TODO: Implement notification mechanism (email, Slack, monitoring system, etc.)
            LOGGER.error(sessionId, JOB_NAME, "notifyAdministrators", 
                    "Alert: " + subject + " - Session: " + sessionId + " - Error: " + exception.getMessage());
        } catch (Exception e) {
            LOGGER.error(sessionId, JOB_NAME, "notifyAdministrators", 
                    "Failed to send notification: " + e.getMessage());
        }
    }
}