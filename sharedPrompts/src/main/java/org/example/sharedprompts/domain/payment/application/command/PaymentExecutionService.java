package org.example.sharedprompts.domain.payment.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.execution.PaymentExecutionServiceTransaction;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.exception.DuplicateOrderIdException;
import org.example.sharedprompts.domain.payment.infrastructure.retry.RetryStrategy;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentExecutionService {

    private final PaymentExecutionServiceTransaction paymentExecutionServiceTransaction;
    private final RetryStrategy immediateRetryStrategy;

    public Payment executePayment(Payment payment, BigDecimal actualAmount) {
        return executePayment(payment, actualAmount, Collections.emptyMap());
    }

    public Payment executePayment(Payment payment, BigDecimal actualAmount, Map<String, String> additionalParams) {
        return executePaymentWithImmediateRetry(payment, actualAmount, additionalParams, 0);
    }

    private Payment executePaymentWithImmediateRetry(
            Payment payment, 
            BigDecimal actualAmount, 
            Map<String, String> additionalParams,
            int immediateRetryCount
    ) {
        try {
            return paymentExecutionServiceTransaction.executePaymentInTransaction(payment.getId(), actualAmount, additionalParams);
        } catch (DuplicateOrderIdException e) {
            return paymentExecutionServiceTransaction.handleDuplicateOrderIdException(payment.getId(), e, actualAmount);
        } catch (ApiException e) {
            if (immediateRetryStrategy.shouldRetry(e, immediateRetryCount)) {
                log.warn("결제 실행 실패, 즉시 재시도 시도: paymentId={}, attempt={}/{}, error={}",
                        payment.getId(), immediateRetryCount + 1, immediateRetryStrategy.getMaxAttempts(), e.getMessage());

                long delayMs = immediateRetryStrategy.calculateDelay(immediateRetryCount);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "재시도 중단: " + e.getMessage());
                }

                Payment freshPayment = paymentExecutionServiceTransaction.executePaymentInTransaction(payment.getId(), actualAmount, additionalParams);
                return executePaymentWithImmediateRetry(freshPayment, actualAmount, additionalParams, immediateRetryCount + 1);
            }
            throw e;
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "알 수 없는 오류";
            log.error("결제 실행 중 예외 발생: paymentId={}, error={}", payment.getId(), errorMessage, e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 실행 실패: " + errorMessage, e);
        }
    }

    public Payment executeCancel(Payment payment, String reason) {
        return paymentExecutionServiceTransaction.executeCancel(payment, reason);
    }
    
    public Payment executeRefund(Payment payment, BigDecimal refundAmount, String reason) {
        return paymentExecutionServiceTransaction.executeRefund(payment, refundAmount, reason);
    }
}

