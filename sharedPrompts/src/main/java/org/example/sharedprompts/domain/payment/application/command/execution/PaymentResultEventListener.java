package org.example.sharedprompts.domain.payment.application.command.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.infrastructure.messaging.event.PaymentEvent;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultEventListener {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentResultProcessor resultProcessor;
    private final PaymentExecutionTemplate executionTemplate;
    private final FailedPaymentEventService failedEventService;
    private final PaymentEventOptimisticLockHandler optimisticLockHandler;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            noRetryFor = {ApiException.class}
    )
    public void handlePaymentResultApplied(PaymentEvent.PaymentResultApplied event) {
        try {
            Payment payment = paymentJpaAdapter.findByIdForUpdate(event.paymentId())
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

            if (!canApplyPaymentResult(payment, event.result())) {
                log.info("결제 결과 적용 건너뜀: paymentId={}, currentStatus={}, resultStatus={}",
                        event.paymentId(), payment.getStatus(), event.result().getStatus());
                return;
            }

            payment.updateIdempotencyKey(event.idempotencyKey());
            
            executionTemplate.applyResultAndSave(
                    payment,
                    event.result(),
                    (p, r) -> resultProcessor.applyPaymentResult(p, r, event.actualAmount())
            );

            log.debug("결제 결과 적용 완료: paymentId={}", event.paymentId());
        } catch (OptimisticLockingFailureException e) {
            if (optimisticLockHandler.handleOptimisticLockFailure(
                    e, event.paymentId(),
                    payment -> !canApplyPaymentResult(payment, event.result())
            )) {
                return;
            }
            throw e;
        } catch (Exception e) {
            log.error("결제 결과 적용 실패: paymentId={}", event.paymentId(), e);
            throw e;
        }
    }

    @Recover
    public void recoverPaymentResultApplied(Exception e, PaymentEvent.PaymentResultApplied event) {
        log.error("결제 결과 적용 최종 실패, 실패 이벤트 저장: paymentId={}, attempts={}", event.paymentId(), MAX_RETRY_ATTEMPTS, e);
        failedEventService.saveFailedEvent("PaymentResultApplied", event.paymentId(), event, e, MAX_RETRY_ATTEMPTS);
    }

    private boolean canApplyPaymentResult(Payment payment, org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult result) {
        PaymentStatus currentStatus = payment.getStatus();
        
        if (result.isSuccess() && currentStatus == PaymentStatus.SUCCESS) {
            return false;
        }
        
        if (!result.isSuccess() && currentStatus == PaymentStatus.FAILED) {
            return false;
        }
        
        return currentStatus.isPending();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            noRetryFor = {ApiException.class}
    )
    public void handleCancelResultApplied(PaymentEvent.CancelResultApplied event) {
        try {
            Payment payment = paymentJpaAdapter.findByIdForUpdate(event.paymentId())
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

            if (payment.getStatus() == PaymentStatus.CANCELED) {
                log.info("취소 결과 적용 건너뜀: paymentId={}, 이미 취소 상태", event.paymentId());
                return;
            }

            PaymentStatus status = payment.getStatus();
            // 취소 가능한 상태: PENDING 또는 SUCCESS (환불되지 않은 경우)
            boolean isCancelable = status == PaymentStatus.PENDING 
                    || (status == PaymentStatus.SUCCESS && (payment.getRefundedAmount() == null 
                            || payment.getRefundedAmount().compareTo(BigDecimal.ZERO) == 0));
            if (!isCancelable) {
                log.warn("취소 결과 적용 불가: paymentId={}, currentStatus={}", event.paymentId(), payment.getStatus());
                return;
            }

            executionTemplate.applyResultAndSave(
                    payment,
                    event.result(),
                    (p, r) -> resultProcessor.applyCancelResult(p, r)
            );

            log.debug("취소 결과 적용 완료: paymentId={}", event.paymentId());
        } catch (OptimisticLockingFailureException e) {
            if (optimisticLockHandler.handleOptimisticLockFailure(
                    e, event.paymentId(),
                    payment -> payment.getStatus() == PaymentStatus.CANCELED
            )) {
                return;
            }
            throw e;
        } catch (Exception e) {
            log.error("취소 결과 적용 실패: paymentId={}", event.paymentId(), e);
            throw e;
        }
    }

    @Recover
    public void recoverCancelResultApplied(Exception e, PaymentEvent.CancelResultApplied event) {
        log.error("취소 결과 적용 최종 실패, 실패 이벤트 저장: paymentId={}, attempts={}", event.paymentId(), MAX_RETRY_ATTEMPTS, e);
        failedEventService.saveFailedEvent("CancelResultApplied", event.paymentId(), event, e, MAX_RETRY_ATTEMPTS);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            noRetryFor = {ApiException.class}
    )
    public void handleRefundResultApplied(PaymentEvent.RefundResultApplied event) {
        try {
            Payment payment = paymentJpaAdapter.findByIdForUpdate(event.paymentId())
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

            if (payment.getStatus() == PaymentStatus.REFUNDED) {
                log.info("환불 결과 적용 건너뜀: paymentId={}, 이미 완전 환불 상태", event.paymentId());
                return;
            }

            if (!payment.getStatus().isRefundable()) {
                log.warn("환불 결과 적용 불가: paymentId={}, currentStatus={}", event.paymentId(), payment.getStatus());
                return;
            }

            if (payment.getIdempotencyKey() != null 
                    && event.idempotencyKey().equals(payment.getIdempotencyKey())) {
                log.info("환불 결과 적용 건너뜀 (멱등성 키 일치): paymentId={}", event.paymentId());
                return;
            }

            payment.updateIdempotencyKey(event.idempotencyKey());
            
            executionTemplate.applyResultAndSave(
                    payment,
                    event.result(),
                    (p, r) -> resultProcessor.applyRefundResult(p, r, event.refundAmount())
            );

            log.debug("환불 결과 적용 완료: paymentId={}", event.paymentId());
        } catch (OptimisticLockingFailureException e) {
            if (optimisticLockHandler.handleOptimisticLockFailure(
                    e, event.paymentId(),
                    payment -> payment.getStatus() == PaymentStatus.REFUNDED
                            || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED
                            || (payment.getIdempotencyKey() != null 
                                    && event.idempotencyKey().equals(payment.getIdempotencyKey()))
            )) {
                return;
            }
            throw e;
        } catch (Exception e) {
            log.error("환불 결과 적용 실패: paymentId={}", event.paymentId(), e);
            throw e;
        }
    }

    @Recover
    public void recoverRefundResultApplied(Exception e, PaymentEvent.RefundResultApplied event) {
        log.error("환불 결과 적용 최종 실패, 실패 이벤트 저장: paymentId={}, attempts={}", event.paymentId(), MAX_RETRY_ATTEMPTS, e);
        failedEventService.saveFailedEvent("RefundResultApplied", event.paymentId(), event, e, MAX_RETRY_ATTEMPTS);
    }
}

