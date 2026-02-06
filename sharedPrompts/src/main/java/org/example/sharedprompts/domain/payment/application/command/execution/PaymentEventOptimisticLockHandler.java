package org.example.sharedprompts.domain.payment.application.command.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventOptimisticLockHandler {

    private final PaymentJpaAdapter paymentJpaAdapter;

    public boolean handleOptimisticLockFailure(
            OptimisticLockingFailureException e,
            Long paymentId,
            PaymentStatusValidation validation
    ) {
        log.warn("낙관적 락 충돌, 상태 재검증: paymentId={}", paymentId);
        
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        if (!validation.isValid(payment)) {
            log.info("낙관적 락 충돌 후 상태 재검증: 이미 처리됨, paymentId={}", paymentId);
            return true;
        }
        
        return false;
    }

    @FunctionalInterface
    public interface PaymentStatusValidation {
        boolean isValid(Payment payment);
    }
}

