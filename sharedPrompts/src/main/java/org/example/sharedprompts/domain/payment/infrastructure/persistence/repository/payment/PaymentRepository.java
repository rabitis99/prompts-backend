package org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.payment;

import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 결제 Repository
 */
public interface PaymentRepository extends JpaRepository<Payment, Long>, CustomPaymentRepository {

    /**
     * 결제 상태 변경 직렬화를 위한 row-level lock 조회

     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.user WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.user WHERE p.id = :id")
    Optional<Payment> findByIdWithFetchJoin(@Param("id") Long id);

    /**
     * 외부 결제 ID로 조회
     */
    Optional<Payment> findByExternalPaymentId(String externalPaymentId);

    /**
     * 외부 결제 ID 존재 여부 (엔티티 로드 없이 EXISTS 쿼리)
     */
    boolean existsByExternalPaymentId(String externalPaymentId);

    /**
     * 멱등성 키로 조회
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * 전체 사용자의 특정 날짜 결제 횟수 조회 (userId 필터 없음)
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE DATE(p.createdAt) = DATE(:date) AND p.status = :status")
    long countByDateAndStatus(
            @Param("date") LocalDateTime date,
            @Param("status") PaymentStatus status
    );

    /**
     * 사용자의 오늘 성공한 결제 횟수 조회
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.user.id = :userId AND DATE(p.createdAt) = CURRENT_DATE AND p.status = :status")
    long countTodaySuccessfulPayments(@Param("userId") Long userId, @Param("status") PaymentStatus status);

    /**
     * 사용자의 누적 성공 결제 금액 합계 조회
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.user.id = :userId AND p.status = :status")
    java.math.BigDecimal sumTotalPaymentAmount(@Param("userId") Long userId, @Param("status") PaymentStatus status);

    /**
     */
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.retryCount < :maxRetry AND (p.nextRetryAt IS NULL OR p.nextRetryAt <= :now) ORDER BY p.nextRetryAt ASC")
    List<Payment> findRetryablePayments(@Param("status") PaymentStatus status, @Param("maxRetry") int maxRetry, @Param("now") LocalDateTime now);

    /**
     * 만료된 PENDING 결제 조회
     */
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.user WHERE p.status = :status AND p.createdAt <= :expirationTime AND p.usedPointAmount > 0 ORDER BY p.createdAt ASC")
    List<Payment> findExpiredPendingPayments(@Param("status") PaymentStatus status, @Param("expirationTime") LocalDateTime expirationTime);
}

