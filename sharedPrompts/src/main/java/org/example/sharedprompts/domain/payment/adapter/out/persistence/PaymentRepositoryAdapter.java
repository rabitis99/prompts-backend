package org.example.sharedprompts.domain.payment.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentCommandRepositoryPort;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentQueryRepositoryPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.payment.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 결제 저장소 어댑터
 * PaymentCommandRepositoryPort와 PaymentQueryRepositoryPort를 구현하여 영속성 레이어를 추상화합니다.
 */
@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentCommandRepositoryPort, PaymentQueryRepositoryPort {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentRepository paymentRepository;

    @Override
    public Payment save(Payment payment) {
        return paymentJpaAdapter.save(payment);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentJpaAdapter.findById(id);
    }

    @Override
    public Optional<Payment> findByIdForUpdate(Long id) {
        return paymentJpaAdapter.findByIdForUpdate(id);
    }

    @Override
    public Optional<Payment> findByExternalPaymentId(String externalPaymentId) {
        return paymentJpaAdapter.findByExternalPaymentId(externalPaymentId);
    }

    @Override
    public Page<Payment> findByUserId(Long userId, Pageable pageable) {
        return paymentJpaAdapter.findByUserIdWithFetchJoin(userId, pageable);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        // TODO: 이 메서드는 기존 어댑터에 없으므로 구현 필요
        // 예: return paymentRepository.findByIdempotencyKey(idempotencyKey);
        return Optional.empty();
    }

    @Override
    public long countByStatusAndCreatedAtAfter(PaymentStatus status, LocalDateTime dateTime) {
        return paymentJpaAdapter.countByDateAndStatus(dateTime, status);
    }

    @Override
    public long countTodaySuccessfulPayments(Long userId, PaymentStatus status) {
        return paymentJpaAdapter.countTodaySuccessfulPayments(userId, status);
    }

    @Override
    public List<Payment> findRetryablePayments() {
        // TODO: 적절한 파라미터를 설정하여 호출
        return paymentJpaAdapter.findRetryablePayments(
                PaymentStatus.PENDING,
                5,  // maxRetry
                LocalDateTime.now()
        );
    }

    @Override
    public List<Payment> findExpiredPendingPayments(LocalDateTime expirationTime) {
        return paymentJpaAdapter.findExpiredPendingPayments(
                PaymentStatus.PENDING,
                expirationTime
        );
    }

    @Override
    public boolean existsByExternalPaymentId(String externalPaymentId) {
        return findByExternalPaymentId(externalPaymentId).isPresent();
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    @Override
    public void delete(Payment payment) {
        paymentRepository.delete(payment);
    }
}
