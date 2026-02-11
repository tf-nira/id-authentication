package io.mosip.authentication.common.service.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_payment_transactions")
public class PartnerPaymentTransaction {

    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "partner_id")
    private String partnerId;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "log_dtimes")
    private LocalDateTime logDtimes;
    
 //  Added processing flag for idempotency
    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed = false;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDateTime getLogDtimes() {
        return logDtimes;
    }

    public void setLogDtimes(LocalDateTime logDtimes) {
        this.logDtimes = logDtimes;
    }
    
    public Boolean getIsProcessed() {
        return isProcessed;
    }

    public void setIsProcessed(Boolean isProcessed) {
        this.isProcessed = isProcessed;
    }
    
    
}