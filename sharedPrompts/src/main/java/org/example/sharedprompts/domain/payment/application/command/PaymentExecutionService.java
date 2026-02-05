package org.example.sharedprompts.domain.payment.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.application.dto.response.CancelResult;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.application.dto.response.RefundResult;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.exception.DuplicateOrderIdException;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.infrastructure.idempotency.IdempotencyService;
import org.example.sharedprompts.domain.payment.infrastructure.retry.RetryStrategy;
import org.example.sharedprompts.domain.payment.domain.service.PaymentValidator;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentExecutionService {

    private final PaymentProviderFactory providerFactory;
    private final PaymentValidator paymentValidator;
    private final PaymentJpaAdapter paymentJpaAdapter;
    private final IdempotencyService idempotencyService;
    private final RetryStrategy immediateRetryStrategy;

    @Autowired
    @Lazy
    private PaymentExecutionService self;

    @Transactional
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
            return self.executePaymentInTransaction(payment.getId(), actualAmount, additionalParams);
        } catch (DuplicateOrderIdException e) {
            return self.handleDuplicateOrderIdException(payment.getId(), e, actualAmount);
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

                Payment freshPayment = self.executePaymentInTransaction(payment.getId(), actualAmount, additionalParams);
                return executePaymentWithImmediateRetry(freshPayment, actualAmount, additionalParams, immediateRetryCount + 1);
            }
            throw e;
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "알 수 없는 오류";
            log.error("결제 실행 중 예외 발생: paymentId={}, error={}", payment.getId(), errorMessage, e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 실행 실패: " + errorMessage, e);
        }
    }

    @Transactional
    public Payment executePaymentInTransaction(Long paymentId, BigDecimal actualAmount, Map<String, String> additionalParams) {
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment가 이미 완료 상태: paymentId={}, externalPaymentId={}",
                    payment.getId(), payment.getExternalPaymentId());
            return payment;
        }

        String idempotencyKey = idempotencyService.generateForPayment(payment);
        payment.updateIdempotencyKey(idempotencyKey);

        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());

        if (payment.getExternalPaymentId() == null || payment.getExternalPaymentId().isEmpty()) {
            if (payment.getRetryCount() > 0) {
                throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, 
                        "재시도할 수 없습니다. paymentKey가 없습니다. 새로운 결제를 요청해주세요.");
            }
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, 
                    "결제 승인을 위해서는 paymentKey가 필요합니다. /payments/confirm 엔드포인트를 사용해주세요.");
        }

        String orderIdForProvider = String.valueOf(payment.getId());
        if (payment.getPaymentMethod() == PaymentMethod.TOSS && additionalParams != null) {
            String tossOrderId = additionalParams.get("tossOrderId");
            if (tossOrderId != null && !tossOrderId.isEmpty()) {
                orderIdForProvider = tossOrderId;
                log.debug("Toss Payments orderId를 프론트엔드에서 받은 값으로 사용: tossOrderId={}, paymentId={}", 
                        tossOrderId, payment.getId());
            }
        }
        
        try {
            PaymentResult result = provider.confirmPayment(
                    payment.getExternalPaymentId(),
                    orderIdForProvider,
                    actualAmount,
                    payment.getCurrency(),
                    idempotencyKey,
                    String.valueOf(payment.getUser().getId()),
                    additionalParams != null ? additionalParams : Collections.emptyMap()
            );

            if (result.getExternalPaymentId() != null) {
                payment.updateExternalPaymentId(result.getExternalPaymentId());
            }

            try {
                paymentValidator.validatePaymentResult(payment, result, actualAmount);

                if (result.isSuccess()) {
                    payment.markSuccess(result.getExternalPaymentId());
                } else {
                    payment.markFailed(result.getFailureReason() != null ? result.getFailureReason() : "결제 승인 실패");
                }
            } catch (ApiException e) {
                log.error("결제 검증 실패 - 외부 결제는 완료되었으나 검증 불일치: paymentId={}, externalPaymentId={}, error={}",
                        payment.getId(), result.getExternalPaymentId(), e.getMessage());
                payment.markFailed("검증 실패: " + e.getMessage());
                paymentJpaAdapter.save(payment);
                throw e;
            }

            return paymentJpaAdapter.save(payment);
        } catch (DuplicateOrderIdException e) {
            throw e;
        }
    }

    @Transactional
    public Payment handleDuplicateOrderIdException(
            Long paymentId,
            DuplicateOrderIdException e,
            BigDecimal actualAmount
    ) {
        log.warn("TossPay 중복 주문번호 오류 발생: paymentId={}, paymentKey={}, orderId={}. " +
                "결제 상태를 확인합니다.", paymentId, e.getPaymentKey(), e.getOrderId());
        
        Payment freshPayment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        if (freshPayment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment가 이미 SUCCESS 상태입니다 (webhook으로 처리됨): paymentId={}", paymentId);
            return freshPayment;
        }
        
        try {
            PaymentProvider statusProvider = providerFactory.getProvider(freshPayment.getPaymentMethod());
            PaymentResult statusResult = statusProvider.getPaymentStatus(freshPayment.getExternalPaymentId());
            
            if (statusResult.isSuccess()) {
                log.info("TossPayments에서 결제가 이미 확인되었습니다: paymentId={}, externalPaymentId={}", 
                        paymentId, freshPayment.getExternalPaymentId());
                
                paymentValidator.validatePaymentResult(freshPayment, statusResult, actualAmount);
                freshPayment.markSuccess(statusResult.getExternalPaymentId());
                
                return paymentJpaAdapter.save(freshPayment);
            } else {
                log.warn("TossPayments에서 결제가 실패 상태입니다: paymentId={}, status={}", 
                        paymentId, statusResult.getStatus());
                freshPayment.markFailed("중복 주문번호 오류: " + e.getMessage());
                return paymentJpaAdapter.save(freshPayment);
            }
        } catch (Exception statusCheckException) {
            log.error("TossPayments 결제 상태 조회 실패: paymentId={}, error={}", 
                    paymentId, statusCheckException.getMessage(), statusCheckException);
            freshPayment.markFailed("중복 주문번호 오류 및 상태 조회 실패: " + e.getMessage());
            return paymentJpaAdapter.save(freshPayment);
        }
    }

    @Transactional
    public Payment executeCancel(Payment payment, String reason) {
        if (payment.getExternalPaymentId() == null) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "외부 결제 ID가 없습니다.");
        }

        String idempotencyKey = idempotencyService.generateForCancel(payment);
        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());

        CancelResult result = provider.cancelPayment(payment.getExternalPaymentId(), reason, idempotencyKey);

        if (!result.isSuccess()) {
            throw new ApiException(ErrorCode.PAYMENT_CANCEL_FAILED, "결제 취소 실패");
        }

        payment.markCanceled();
        
        if (result.getOriginalAmount() != null && result.getTaxFreeAmount() != null) {
            payment.updateOriginalAmounts(result.getOriginalAmount(), result.getTaxFreeAmount());
        }
        
        return paymentJpaAdapter.save(payment);
    }
    
    @Transactional
    public Payment executeRefund(Payment payment, BigDecimal refundAmount, String reason) {
        if (payment.getExternalPaymentId() == null) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "외부 결제 ID가 없습니다.");
        }

        String idempotencyKey = idempotencyService.generateForRefund(payment);
        payment.updateIdempotencyKey(idempotencyKey);

        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());

        RefundResult result = provider.refundPayment(payment.getExternalPaymentId(), refundAmount, reason, idempotencyKey);

        if (!result.isSuccess()) {
            throw new ApiException(ErrorCode.PAYMENT_REFUND_FAILED, "결제 환불 실패");
        }

        BigDecimal actualRefundedAmount = result.getRefundedAmount() != null ? result.getRefundedAmount() : refundAmount;
        payment.refund(actualRefundedAmount);
        return paymentJpaAdapter.save(payment);
    }
}

