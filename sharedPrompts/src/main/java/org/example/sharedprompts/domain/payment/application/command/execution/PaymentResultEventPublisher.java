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
        // 이벤트 발행 전에 Payment 상태를 동기적으로 업데이트
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        // idempotencyKey 저장
        payment.updateIdempotencyKey(idempotencyKey);
        
        // Payment 상태 업데이트 및 저장
        Payment savedPayment = executionTemplate.applyResultAndSave(
                payment,
                result,
                (p, r) -> resultProcessor.applyPaymentResult(p, r, actualAmount)
        );
        
        // 상태 업데이트 후 이벤트 발행 (이벤트 리스너는 추가 검증/후처리만 수행)
        eventPublisher.publishPaymentResultApplied(paymentId, result, actualAmount, idempotencyKey);
        
        return savedPayment;
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

