package org.example.sharedprompts.domain.payment.application.port.out.repository;

import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentQueryRepositoryPort {

    Optional<Payment> findById(Long id);

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    long countByStatusAndCreatedAtAfter(PaymentStatus status, LocalDateTime dateTime);

    long countTodayPaymentsByStatus(Long userId, PaymentStatus status);

    /**
     * 재시도 가능한 결제 조회 (재시도 정책은 호출부에서 파라미터로 전달).
     */
    List<Payment> findRetryablePayments(PaymentStatus status, int maxRetry, LocalDateTime now);

    List<Payment> findExpiredPendingPayments(LocalDateTime expirationTime);
}
