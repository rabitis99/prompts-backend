package org.example.sharedprompts.domain.payment.infrastructure.messaging.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.webhook.WebhookEvent;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.domain.service.PaymentValidator;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookTransactionService {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentValidator paymentValidator;

    @Transactional
    public Payment processPaymentInTransaction(Payment payment, WebhookEvent event) {
        Payment freshPayment = paymentJpaAdapter.findById(payment.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        if (freshPayment.getStatus() != PaymentStatus.PENDING) {
            log.info("Payment 상태가 이미 변경됨: paymentId={}, status={}",
                    freshPayment.getId(), freshPayment.getStatus());
            return freshPayment;
        }

        PaymentResult paymentResult = event.paymentResult();
        if (paymentResult != null) {
            validatePaymentResult(freshPayment, paymentResult);
            applyWebhookResult(freshPayment, paymentResult);
        }

        try {
            return paymentJpaAdapter.saveAndFlush(freshPayment);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.info("낙관적 락 충돌 발생: paymentId={}", payment.getId());
            return paymentJpaAdapter.findById(payment.getId())
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        }
    }

    private void validatePaymentResult(Payment payment, PaymentResult paymentResult) {
        BigDecimal actualAmount = payment.getAmount().subtract(
                payment.getUsedPointAmount() != null ? payment.getUsedPointAmount() : BigDecimal.ZERO
        );
        paymentValidator.validatePaymentResult(payment, paymentResult, actualAmount);
    }

    private void applyWebhookResult(Payment payment, PaymentResult paymentResult) {
        payment.applyWebhookResult(
                paymentResult.getExternalPaymentId(),
                paymentResult.getStatus(),
                paymentResult.getApprovedAt(),
                paymentResult.getFailureReason()
        );
    }
}

