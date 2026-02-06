package org.example.sharedprompts.domain.payment.application.command.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.PaymentExecutionService;
import org.example.sharedprompts.domain.payment.application.command.PaymentValidationService;
import org.example.sharedprompts.domain.payment.application.command.orchestrator.PaymentTransactionOrchestrator;
import org.example.sharedprompts.domain.payment.application.command.service.refund.PaymentRefundPostProcessor;
import org.example.sharedprompts.domain.payment.application.command.service.refund.RefundExecutionResult;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRefundService {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentValidationService validationService;
    private final PaymentExecutionService executionService;
    private final PaymentTransactionOrchestrator orchestrator;
    private final PaymentRefundPostProcessor postProcessor;

    public Payment refund(Long userId, PaymentRefundRequestDto request) {
        Long paymentId = request.getPaymentIdAsLong();
        String lockKey = orchestrator.createLockKey("payment", paymentId) + ":state";

        log.info("결제 환불 시작: paymentId={}, userId={}, requestedAmount={}",
                paymentId, userId, request.getAmount());

        RefundExecutionResult result = orchestrator.executeWithLockAndTransaction(lockKey, () -> {
            Payment payment = paymentJpaAdapter.findById(paymentId)
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

            validationService.validatePaymentOwnership(payment, userId);
            validationService.validateRefundableStatus(payment);
            BigDecimal refundAmount = validationService.validateRefundAmount(request.getAmount(), payment);

            log.debug("환불 실행: paymentId={}, refundAmount={}, reason={}",
                    paymentId, refundAmount, request.getReasonOrDefault());

            Payment refundedPayment = executionService.executeRefund(
                    payment, refundAmount, request.getReasonOrDefault());

            log.info("환불 실행 완료: paymentId={}, refundAmount={}", paymentId, refundAmount);

            return new RefundExecutionResult(refundedPayment, refundAmount);
        });

        postProcessor.processAfterCommit(result.getRefundedPayment(), userId, result.getRefundAmount(),
                request.getReasonOrDefault());

        log.info("결제 환불 완료: paymentId={}, userId={}, refundAmount={}",
                paymentId, userId, result.getRefundAmount());

        return result.getRefundedPayment();
    }
}

