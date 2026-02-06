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
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultEventListener {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentResultProcessor resultProcessor;
    private final PaymentExecutionTemplate executionTemplate;
    private final FailedPaymentEventService failedEventService;
    private final PaymentEventOptimisticLockHandler optimisticLockHandler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            noRetryFor = {ApiException.class}
    )
    public void handlePaymentResultApplied(PaymentEvent.PaymentResultApplied event) {
        try {
            Payment payment = paymentJpaAdapter.findById(event.paymentId())
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

            if (!canApplyPaymentResult(payment, event.result())) {
                log.info("결제 결과 적용 건너뜀: paymentId={}, currentStatus={}, resultStatus={}",
                        event.paymentId(), payment.getStatus(), event.result().getStatus());
                return;
            }

            // idempotencyKey 저장 (이벤트 발행 시점의 키를 저장)
            payment.updateIdempotencyKey(event.idempotencyKey());
            
            executionTemplate.applyResultAndSave(
                    payment,
                    event.result(),
                    r -> resultProcessor.applyPaymentResult(payment, r, event.actualAmount())
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
        log.error("결제 결과 적용 최종 실패, 실패 이벤트 저장: paymentId={}, attempts=3", event.paymentId(), e);
        failedEventService.saveFailedEvent("PaymentResultApplied", event.paymentId(), event, e);
    }

    private boolean canApplyPaymentResult(Payment payment, org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult result) {
        PaymentStatus currentStatus = payment.getStatus();
        
        if (result.isSuccess() && currentStatus == PaymentStatus.SUCCESS) {
            return false;
        }
        
        if (result.isFailure() && currentStatus == PaymentStatus.FAILED) {
            return false;
        }
        
        return currentStatus.isPending();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            noRetryFor = {ApiException.class}
    )
    public void handleCancelResultApplied(PaymentEvent.CancelResultApplied event) {
        try {
            Payment payment = paymentJpaAdapter.findById(event.paymentId())
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

            if (payment.getStatus() == PaymentStatus.CANCELED) {
                log.info("취소 결과 적용 건너뜀: paymentId={}, 이미 취소 상태", event.paymentId());
                return;
            }

            if (!payment.getStatus().isRefundable() && payment.getStatus() != PaymentStatus.PENDING) {
                log.warn("취소 결과 적용 불가: paymentId={}, currentStatus={}", event.paymentId(), payment.getStatus());
                return;
            }

            executionTemplate.applyResultAndSave(
                    payment,
                    event.result(),
                    r -> resultProcessor.applyCancelResult(payment, r)
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
        log.error("취소 결과 적용 최종 실패, 실패 이벤트 저장: paymentId={}, attempts=3", event.paymentId(), e);
        failedEventService.saveFailedEvent("CancelResultApplied", event.paymentId(), event, e);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            noRetryFor = {ApiException.class}
    )
    public void handleRefundResultApplied(PaymentEvent.RefundResultApplied event) {
        try {
            Payment payment = paymentJpaAdapter.findById(event.paymentId())
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

            if (payment.getStatus() == PaymentStatus.REFUNDED) {
                log.info("환불 결과 적용 건너뜀: paymentId={}, 이미 완전 환불 상태", event.paymentId());
                return;
            }

            if (!payment.getStatus().isRefundable()) {
                log.warn("환불 결과 적용 불가: paymentId={}, currentStatus={}", event.paymentId(), payment.getStatus());
                return;
            }

            // idempotencyKey 저장 (이벤트 발행 시점의 키를 저장)
            payment.updateIdempotencyKey(event.idempotencyKey());
            
            executionTemplate.applyResultAndSave(
                    payment,
                    event.result(),
                    r -> resultProcessor.applyRefundResult(payment, r, event.refundAmount())
            );

            log.debug("환불 결과 적용 완료: paymentId={}", event.paymentId());
        } catch (OptimisticLockingFailureException e) {
            if (optimisticLockHandler.handleOptimisticLockFailure(
                    e, event.paymentId(),
                    payment -> payment.getStatus() == PaymentStatus.REFUNDED
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
        log.error("환불 결과 적용 최종 실패, 실패 이벤트 저장: paymentId={}, attempts=3", event.paymentId(), e);
        failedEventService.saveFailedEvent("RefundResultApplied", event.paymentId(), event, e);
    }
}

