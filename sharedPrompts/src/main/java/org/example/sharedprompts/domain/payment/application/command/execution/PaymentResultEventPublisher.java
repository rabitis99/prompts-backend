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
    private final PaymentResultProcessor resultProcessor;
    private final PaymentExecutionTemplate executionTemplate;

    @Transactional
    public Payment publishPaymentResult(
            Long paymentId,
            PaymentResult result,
            BigDecimal actualAmount,
            String idempotencyKey
    ) {
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        payment.updateIdempotencyKey(idempotencyKey);
        
        Payment savedPayment = executionTemplate.applyResultAndSave(
                payment,
                result,
                (p, r) -> resultProcessor.applyPaymentResult(p, r, actualAmount)
        );
        
        eventPublisher.publishPaymentResultApplied(paymentId, result, actualAmount, idempotencyKey);
        
        return savedPayment;
    }

    @Transactional
    public Payment publishCancelResult(Long paymentId, CancelResult result) {
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        eventPublisher.publishCancelResultApplied(paymentId, result);
        
        return payment;
    }

    @Transactional
    public Payment publishRefundResult(
            Long paymentId,
            RefundResult result,
            BigDecimal refundAmount,
            String idempotencyKey
    ) {
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        eventPublisher.publishRefundResultApplied(paymentId, result, refundAmount, idempotencyKey);
        
        return payment;
    }
}

