package org.example.sharedprompts.domain.payment.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.config.properties.RetryProperties;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRetryService {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final RetryProperties retryProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment commitRetryState(Long paymentId) {
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.incrementRetryCount();
        payment.scheduleNextRetry(retryProperties.getDelayMs());
        payment.markPending();
        
        return paymentJpaAdapter.save(payment);
    }
}

