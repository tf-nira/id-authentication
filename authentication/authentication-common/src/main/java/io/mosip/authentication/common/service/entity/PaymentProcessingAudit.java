package io.mosip.authentication.common.service.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 *  Audit entity for tracking all payment processing operations
 * Allows forensic analysis and recovery of failed transactions
 */
@Entity
@Table(name = "payment_processing_audit", indexes = {
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_partner_id", columnList = "partner_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_processed_dtimes", columnList = "processed_dtimes")
})
public class PaymentProcessingAudit {

    @Id
    @Column(name = "audit_id")
    private String auditId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "partner_id")
    private String partnerId;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "status", nullable = false)
    private String status; // SUCCESS, FAILED_PARTNER_NOT_FOUND, FAILED_INSUFFICIENT_BALANCE, FAILED_EXCEPTION, SKIPPED_*

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "processed_dtimes", nullable = false)
    private LocalDateTime processedDtimes;

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getProcessedDtimes() {
        return processedDtimes;
    }

    public void setProcessedDtimes(LocalDateTime processedDtimes) {
        this.processedDtimes = processedDtimes;
    }
}