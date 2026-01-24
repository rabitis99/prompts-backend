package org.example.sharedprompts.domain.audit.auth.repository;

import org.example.sharedprompts.domain.audit.auth.AuthAuditLog;
import org.example.sharedprompts.domain.audit.auth.enums.AuthEventType;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * 인증 보안 이벤트 로그 저장소
 */
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {

    /**
     * 복합 필터링으로 인증 이벤트 로그 조회
     */
    @Query("SELECT a FROM AuthAuditLog a WHERE " +
           "(:eventType IS NULL OR a.eventType = :eventType) AND " +
           "(:provider IS NULL OR a.provider = :provider) AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:failReason IS NULL OR a.failReason = :failReason) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuthAuditLog> findWithFilters(
            @Param("eventType") AuthEventType eventType,
            @Param("provider") Provider provider,
            @Param("userId") Long userId,
            @Param("failReason") AuthFailReason failReason,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}

