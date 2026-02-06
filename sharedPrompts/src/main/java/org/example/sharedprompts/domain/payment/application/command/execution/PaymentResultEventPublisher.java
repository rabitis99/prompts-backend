package org.example.sharedprompts.domain.payment.application.command.execution;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.application.dto.response.CancelResult;
import org.example.sharedprompts.domain.payment.application.dto.response.RefundResult;
import org.example.sharedprompts.domain.payment.infrastructure.messaging.event.PaymentEventPublisher;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PaymentResultEventPublisher {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentEventPublisher eventPublisher;

    @Transactional
    public Payment publishPaymentResult(
            Long paymentId,
            PaymentResult result,
            BigDecimal actualAmount,
            String idempotencyKey
    ) {
        eventPublisher.publishPaymentResultApplied(paymentId, result, actualAmount, idempotencyKey);
        
        // 반환값은 조회만 수행 (트랜잭션 내에서 일관성 보장)
        return paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    @Transactional
    public Payment publishCancelResult(Long paymentId, CancelResult result) {

        eventPublisher.publishCancelResultApplied(paymentId, result);
        
        return paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    @Transactional
    public Payment publishRefundResult(
            Long paymentId,
            RefundResult result,
            BigDecimal refundAmount,
            String idempotencyKey
    ) {
        eventPublisher.publishRefundResultApplied(paymentId, result, refundAmount, idempotencyKey);
        
        return paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
    }
}

