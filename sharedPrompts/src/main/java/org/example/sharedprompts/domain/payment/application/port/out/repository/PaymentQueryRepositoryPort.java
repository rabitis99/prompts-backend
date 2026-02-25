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

    long countTodaySuccessfulPayments(Long userId, PaymentStatus status);

    List<Payment> findRetryablePayments();

    List<Payment> findExpiredPendingPayments(LocalDateTime expirationTime);
}
