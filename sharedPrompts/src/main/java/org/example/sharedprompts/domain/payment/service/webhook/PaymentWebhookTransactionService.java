package org.example.sharedprompts.domain.payment.service.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.provider.webhook.PaymentWebhookHandler;
import org.example.sharedprompts.domain.payment.provider.webhook.WebhookEvent;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.validator.PaymentValidator;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Payment Webhook Transaction Service
 *
 * <p>단일 책임: Webhook 처리 시 트랜잭션 내에서 Payment 상태 변경 및 저장
 * <p>별도 @Service로 분리하여 @Transactional이 정상 작동하도록 함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookTransactionService {

    private final PaymentRepository paymentRepository;
    private final PaymentValidator paymentValidator;

    /**
     * 트랜잭션 내에서 Payment 상태 변경 및 저장
     *
     * <p>Redis 작업은 이 트랜잭션과 독립적으로 수행됨
     */
    @Transactional
    public Payment processPaymentInTransaction(Payment payment, WebhookEvent event) {
        // Double-check: 트랜잭션 시작 후 다시 상태 확인 (동시성 처리)
        Payment freshPayment = paymentRepository.findById(payment.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        if (freshPayment.getStatus() != PaymentStatus.PENDING) {
            log.info("Payment 상태가 이미 변경됨 (동시 처리): paymentId={}, status={}",
                    freshPayment.getId(), freshPayment.getStatus());
            return freshPayment;
        }

        // PaymentResult 검증 및 적용
        PaymentResult paymentResult = event.paymentResult();
        if (paymentResult != null) {
            validatePaymentResult(freshPayment, paymentResult);
            applyWebhookResult(freshPayment, paymentResult);
        }

        try {
            return paymentRepository.saveAndFlush(freshPayment);
        } catch (ObjectOptimisticLockingFailureException e) {
            // 낙관적 락 충돌: 다른 트랜잭션이 먼저 업데이트함
            log.info("낙관적 락 충돌 발생 (동시 처리): paymentId={}", payment.getId());
            return paymentRepository.findById(payment.getId())
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        }
    }

    /**
     * PaymentResult 검증
     */
    private void validatePaymentResult(Payment payment, PaymentResult paymentResult) {
        BigDecimal actualAmount = payment.getAmount().subtract(
                payment.getUsedPointAmount() != null ? payment.getUsedPointAmount() : BigDecimal.ZERO
        );
        paymentValidator.validatePaymentResult(payment, paymentResult, actualAmount);
    }

    /**
     * Webhook 결과를 Payment에 적용
     */
    private void applyWebhookResult(Payment payment, PaymentResult paymentResult) {
        payment.applyWebhookResult(
                paymentResult.getExternalPaymentId(),
                paymentResult.getStatus(),
                paymentResult.getApprovedAt(),
                paymentResult.getFailureReason()
        );
    }
}

