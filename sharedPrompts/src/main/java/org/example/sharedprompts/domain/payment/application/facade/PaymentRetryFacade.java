package org.example.sharedprompts.domain.payment.application.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.config.properties.RetryProperties;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.application.command.PaymentExecutionService;
import org.example.sharedprompts.domain.payment.application.command.PaymentRetryService;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRetryFacade {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentExecutionService executionService;
    private final PaymentLoggingService loggingService;
    private final RetryProperties retryProperties;
    private final PaymentRetryService paymentRetryService;

    public Payment commitRetryState(Payment payment) {
        return paymentRetryService.commitRetryState(payment.getId());
    }

    @Transactional
    public PaymentResult retryPayment(Long paymentId) {
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        if (!payment.isRetryable(retryProperties.getMaxAttempts())) {
            throw new ApiException(ErrorCode.PAYMENT_RETRY_EXCEEDED);
        }
        
        if (payment.getExternalPaymentId() == null || payment.getExternalPaymentId().isEmpty()) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR,
                    "재시도할 수 없습니다. paymentKey가 없습니다. 새로운 결제를 요청해주세요.");
        }
        
        loggingService.logRetryAttempt(payment, payment.getRetryCount() + 1);

        Payment updatedPayment = commitRetryState(payment);
        
        BigDecimal actualAmount = updatedPayment.getAmount().subtract(
                updatedPayment.getUsedPointAmount() != null ? updatedPayment.getUsedPointAmount() : java.math.BigDecimal.ZERO
        );

        Payment executedPayment = executionService.executePayment(updatedPayment, actualAmount);

        return PaymentResult.builder()
                .externalPaymentId(executedPayment.getExternalPaymentId())
                .status(executedPayment.getStatus())
                .amount(executedPayment.getAmount())
                .currency(executedPayment.getCurrency())
                .orderId(String.valueOf(executedPayment.getId()))
                .approvedAt(executedPayment.getApprovedAt())
                .failureReason(executedPayment.getFailureReason())
                .build();
    }

    @Transactional
    public PaymentResponseDto retryPaymentAsResponse(Long paymentId) {
        retryPayment(paymentId);
        
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        return PaymentResponseDto.from(payment);
    }
    
}

