package org.example.sharedprompts.domain.payment.repository.payment;

import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
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
     *
     * <p>동일 Payment를 confirm/cancel/refund/webhook 등이 동시에 갱신할 때
     * OptimisticLock 충돌을 줄이기 위해 PESSIMISTIC_WRITE 락으로 조회합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    /**
     * 외부 결제 ID로 조회
     */
    Optional<Payment> findByExternalPaymentId(String externalPaymentId);

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
     * nextRetryAt 기반 재시도 대상 결제 조회
     * - PENDING 상태인 결제만 재시도 대상
     * - 재시도 횟수가 최대값 미만이고
     * - nextRetryAt이 null이거나 현재 시간 이하인 결제
     * 
     * <p>SUCCESS, FAILED, CANCELED, REFUNDED 등 최종 상태는 재시도 대상이 아닙니다.
     * <p>MySQL에서는 ASC 정렬 시 NULL 값이 자동으로 맨 앞에 정렬되므로 NULLS FIRST 구문이 필요 없습니다.
     */
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.retryCount < :maxRetry AND (p.nextRetryAt IS NULL OR p.nextRetryAt <= :now) ORDER BY p.nextRetryAt ASC")
    List<Payment> findRetryablePayments(@Param("status") PaymentStatus status, @Param("maxRetry") int maxRetry, @Param("now") LocalDateTime now);

    /**
     * 만료된 PENDING 결제 조회
     * - PENDING 상태이고
     * - createdAt이 만료 시간(expirationTime) 이전인 결제
     * - 복구되지 않은 포인트가 있는 결제만 조회 (usedPointAmount > 0)
     *
     * @param expirationTime 만료 기준 시간 (createdAt + expirationMinutes)
     * @return 만료된 PENDING 결제 목록
     */
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt <= :expirationTime AND p.usedPointAmount > 0 ORDER BY p.createdAt ASC")
    List<Payment> findExpiredPendingPayments(@Param("status") PaymentStatus status, @Param("expirationTime") LocalDateTime expirationTime);
}

