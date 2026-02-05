package org.example.sharedprompts.domain.payment.application.command.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.PaymentValidationService;
import org.example.sharedprompts.domain.payment.application.command.orchestrator.PaymentTransactionOrchestrator;
import org.example.sharedprompts.domain.payment.application.command.service.amount.PaymentAmountProcessingService;
import org.example.sharedprompts.domain.payment.application.command.service.confirm.*;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.dto.payment.response.PaymentConfirmResponse;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConfirmService {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentValidationService validationService;
    private final PaymentAmountProcessingService amountProcessingService;
    private final PaymentTransactionOrchestrator orchestrator;
    private final PaymentConfirmValidator confirmValidator;
    private final PaymentConfirmParamBuilder paramBuilder;
    private final PaymentConfirmExecutionHandler executionHandler;
    private final PaymentConfirmOptimisticLockHandler optimisticLockHandler;
    private final PaymentConfirmPostProcessor postProcessor;
    private final PaymentConfirmResponseMapper responseMapper;

    public PaymentConfirmResponse confirm(Long userId, PaymentConfirmRequest request) {
        Long paymentId = request.getOrderIdAsLong();
        String lockKey = orchestrator.createLockKey("payment", paymentId) + ":state";

        log.info("결제 승인 시작: paymentId={}, userId={}", paymentId, userId);

        try {
            PaymentExecutionResult result = orchestrator.executeWithLockAndTransaction(lockKey, () -> {
                return executePaymentConfirmation(paymentId, userId, request);
            });

            if (result.isSucceeded()) {
                postProcessor.processSuccessPostCommit(
                        result.getPayment(), 
                        userId, 
                        result.getProcessingTime(),
                        result.getActualAmount(), 
                        result.getOriginalAmount()
                );
            } else {
                log.debug("결제 성공 후처리 스킵: paymentId={}, status={}", 
                        paymentId, result.getPayment().getStatus());
            }

            log.info("결제 승인 완료: paymentId={}, userId={}, succeeded={}", 
                    paymentId, userId, result.isSucceeded());

            return responseMapper.toConfirmResponse(result.getPayment(), request);

        } catch (ObjectOptimisticLockingFailureException e) {
            return optimisticLockHandler.handleOptimisticLockException(paymentId, request, e, responseMapper);
        }
    }

    private PaymentExecutionResult executePaymentConfirmation(Long paymentId, Long userId,
                                                              PaymentConfirmRequest request) {
        long startTime = System.currentTimeMillis();

        Payment payment = paymentJpaAdapter.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        validationService.validatePaymentOwnership(payment, userId);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("결제가 이미 완료됨: paymentId={}, userId={}", paymentId, userId);
            return new PaymentExecutionResult(payment, 0, true, null, null);
        }

        confirmValidator.validateAndPreparePayment(payment, request);

        BigDecimal actualAmount = amountProcessingService.calculateActualAmount(
                payment.getAmount(),
                payment.getUsedPointAmount()
        );
        BigDecimal originalAmount = payment.getAmount();

        var additionalParams = paramBuilder.buildAdditionalParams(payment, request);

        try {
            return executionHandler.executePayment(payment, actualAmount, originalAmount, additionalParams, userId, startTime);
        } catch (ObjectOptimisticLockingFailureException e) {
            return optimisticLockHandler.handleOptimisticLockInTransaction(paymentId, request, e);
        }
    }
}

