package org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.payment.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentJpaAdapter {

    private final PaymentRepository paymentRepository;

    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }

    public Payment saveAndFlush(Payment payment) {
        return paymentRepository.saveAndFlush(payment);
    }

    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    public Optional<Payment> findByIdForUpdate(Long id) {
        return paymentRepository.findByIdForUpdate(id);
    }

    public Optional<Payment> findByExternalPaymentId(String externalPaymentId) {
        return paymentRepository.findByExternalPaymentId(externalPaymentId);
    }

    public Page<Payment> findByUserIdWithFetchJoin(Long userId, Pageable pageable) {
        return paymentRepository.findByUserIdWithFetchJoin(userId, pageable);
    }

    public Page<Payment> findAllWithFetchJoin(Pageable pageable) {
        return paymentRepository.findAllWithFetchJoin(pageable);
    }

    public long countByDateAndStatus(LocalDateTime date, PaymentStatus status) {
        return paymentRepository.countByDateAndStatus(date, status);
    }

    public long countTodaySuccessfulPayments(Long userId, PaymentStatus status) {
        return paymentRepository.countTodaySuccessfulPayments(userId, status);
    }

    public List<Payment> findRetryablePayments(PaymentStatus status, int maxRetry, LocalDateTime now) {
        return paymentRepository.findRetryablePayments(status, maxRetry, now);
    }

    public List<Payment> findExpiredPendingPayments(PaymentStatus status, LocalDateTime expirationTime) {
        return paymentRepository.findExpiredPendingPayments(status, expirationTime);
    }

    public BigDecimal sumTotalPaymentAmount(Long userId, PaymentStatus status) {
        return paymentRepository.sumTotalPaymentAmount(userId, status);
    }
}

