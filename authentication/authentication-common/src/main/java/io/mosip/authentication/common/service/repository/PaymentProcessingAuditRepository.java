package io.mosip.authentication.common.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import io.mosip.authentication.common.service.entity.PaymentProcessingAudit;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for audit trail operations
 */
@Repository
public interface PaymentProcessingAuditRepository extends JpaRepository<PaymentProcessingAudit, String> {
    
    List<PaymentProcessingAudit> findBySessionId(String sessionId);
    
    List<PaymentProcessingAudit> findByStatus(String status);
    
    @Query("SELECT a FROM PaymentProcessingAudit a WHERE a.processedDtimes BETWEEN :startTime AND :endTime ORDER BY a.processedDtimes DESC")
    List<PaymentProcessingAudit> findByProcessedDtimesBetween(
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT a FROM PaymentProcessingAudit a WHERE a.partnerId = :partnerId AND a.processedDtimes BETWEEN :startTime AND :endTime")
    List<PaymentProcessingAudit> findPartnerTransactionHistory(
        @Param("partnerId") String partnerId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT COUNT(a) FROM PaymentProcessingAudit a WHERE a.status LIKE 'FAILED%'")
    Long countFailedOperations();
}