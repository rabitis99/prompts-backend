package org.example.sharedprompts.domain.payment.application.port.out.repository;

import org.example.sharedprompts.domain.payment.domain.entity.Payment;

import java.util.Optional;

public interface PaymentCommandRepositoryPort {

    Payment save(Payment payment);

    Optional<Payment> findById(Long id);

    Optional<Payment> findByIdForUpdate(Long id);

    Optional<Payment> findByExternalPaymentId(String externalPaymentId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsByExternalPaymentId(String externalPaymentId);

    boolean existsById(Long id);

    void delete(Payment payment);
}
