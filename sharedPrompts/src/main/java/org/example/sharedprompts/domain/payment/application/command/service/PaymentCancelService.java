package org.example.sharedprompts.domain.payment.application.command.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.PaymentExecutionService;
import org.example.sharedprompts.domain.payment.application.command.PaymentValidationService;
import org.example.sharedprompts.domain.payment.application.command.orchestrator.CompensationHandler;
import org.example.sharedprompts.domain.payment.application.command.orchestrator.PaymentTransactionOrchestrator;
import org.example.sharedprompts.domain.payment.application.command.postprocess.PaymentPostProcessService;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.compensation.CompensationTaskType;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCancelService {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentValidationService validationService;
    private final PaymentExecutionService executionService;
    private final PaymentPostProcessService postProcessService;
    private final PaymentTransactionOrchestrator orchestrator;
    private final CompensationHandler compensationHandler;

    public Payment cancel(Long userId, PaymentCancelRequestDto request) {
        Long paymentId = request.getPaymentIdAsLong();
        String lockKey = orchestrator.createLockKey("payment", paymentId) + ":state";

        log.info("결제 취소 시작: paymentId={}, userId={}", paymentId, userId);

        Payment canceledPayment = orchestrator.executeWithLockAndTransaction(lockKey, () -> {
            Payment payment = paymentJpaAdapter.findById(paymentId)
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

            validationService.validatePaymentOwnership(payment, userId);
            validationService.validateCancelableStatus(payment);

            try {
                Payment result = executionService.executeCancel(payment, request.getReasonOrDefault());
                log.info("결제 취소 실행 완료: paymentId={}, userId={}", paymentId, userId);
                return result;
            } catch (Exception e) {
                log.error("결제 취소 실패: paymentId={}, userId={}, error={}", 
                        paymentId, userId, e.getMessage(), e);
                throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 취소 실패: " + e.getMessage());
            }
        });

        processCancelPostCommit(canceledPayment, userId, request.getReasonOrDefault());

        log.info("결제 취소 완료: paymentId={}, userId={}", canceledPayment.getId(), userId);

        return canceledPayment;
    }

    private void processCancelPostCommit(Payment canceledPayment, Long userId, String reason) {
        if (canceledPayment == null || canceledPayment.getId() == null) {
            log.error("결제 취소 후처리 스킵: canceledPayment가 유효하지 않음");
            return;
        }

        try {
            postProcessService.processPaymentCancelAfterCommit(
                    canceledPayment.getId(),
                    userId,
                    reason
            );
            log.debug("결제 취소 후처리 완료: paymentId={}, userId={}", 
                    canceledPayment.getId(), userId);
        } catch (Exception postProcessException) {
            log.error("결제 취소 성공 후 후처리 실패: paymentId={}, userId={}, error={}",
                    canceledPayment.getId(), userId, postProcessException.getMessage(), postProcessException);

            try {
                compensationHandler.handlePostProcessFailure(
                        CompensationTaskType.POINT_RECOVERY_CANCEL,
                        canceledPayment.getId(),
                        userId,
                        canceledPayment.getUsedPointAmount(),
                        postProcessException.getMessage()
                );
                log.info("결제 취소 후처리 실패에 대한 보상 처리 완료: paymentId={}", 
                        canceledPayment.getId());
            } catch (Exception compensationException) {
                log.error("결제 취소 후처리 실패에 대한 보상 처리도 실패: paymentId={}, userId={}, error={}",
                        canceledPayment.getId(), userId, compensationException.getMessage(), compensationException);
            }
        }
    }
}

