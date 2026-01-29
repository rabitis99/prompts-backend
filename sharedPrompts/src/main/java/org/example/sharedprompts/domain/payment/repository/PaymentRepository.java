package org.example.sharedprompts.domain.payment.repository;

import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 결제 Repository
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 외부 결제 ID로 조회
     */
    Optional<Payment> findByExternalPaymentId(String externalPaymentId);

    /**
     * 사용자 ID와 결제 ID로 조회
     */
    @Query("SELECT p FROM Payment p WHERE p.id = :id AND p.user.id = :userId")
    Optional<Payment> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 사용자의 특정 날짜 결제 횟수 조회
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.user.id = :userId AND DATE(p.createdAt) = DATE(:date) AND p.status = :status")
    long countByUserIdAndDateAndStatus(
            @Param("userId") Long userId,
            @Param("date") LocalDateTime date,
            @Param("status") PaymentStatus status
    );

    /**
     * 사용자의 오늘 성공한 결제 횟수 조회
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.user.id = :userId AND DATE(p.createdAt) = CURRENT_DATE AND p.status = 'SUCCESS'")
    long countTodaySuccessfulPayments(@Param("userId") Long userId);

    /**
     * 사용자의 결제 목록 조회
     */
    List<Payment> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 상태의 결제 목록 조회
     */
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    /**
     * 재시도가 필요한 결제 목록 조회 (PENDING 상태이고 재시도 횟수가 제한 미만)
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.retryCount < :maxRetryCount ORDER BY p.createdAt ASC")
    List<Payment> findPendingPaymentsForRetry(@Param("maxRetryCount") int maxRetryCount);
}

