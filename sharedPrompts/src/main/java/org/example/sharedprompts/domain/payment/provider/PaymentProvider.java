package org.example.sharedprompts.domain.payment.provider;

import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.model.CancelResult;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.model.RefundResult;

import java.math.BigDecimal;

/**
 * 결제 Provider 인터페이스
 */
public interface PaymentProvider {
    
    /**
     */
    PaymentMethod getPaymentMethod();

    /**
     * 결제 준비 요청 (선택적 구현)
     */
    default PrepareResult preparePayment(
            String orderId,
            BigDecimal amount,
            String currency,
            String itemName,
            String userId,
            String idempotencyKey
    ) {
        return PrepareResult.notRequired();
    }

    /**
     * 결제 준비 단계 필요 여부
     * @return 준비 단계 필요 여부
     */
    default boolean requiresPreparation() {
        return false;
    }

    /**
     * 결제 승인/확인 요청
     */
    PaymentResult confirmPayment(
            String paymentKey,
            String orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            String userId,
            java.util.Map<String, String> additionalParams
    );

    /**
     * 결제 상태 조회
     */
    PaymentResult getPaymentStatus(String externalPaymentId);
    
    /**
     * 결제 취소
     */
    CancelResult cancelPayment(String externalPaymentId, String reason, String idempotencyKey);

    /**
     * 결제 환불
     */
    RefundResult refundPayment(String externalPaymentId, BigDecimal amount, String reason, String idempotencyKey);


    /**
     * 결제 준비 결과
     *
     * @param required 준비 단계 필요 여부
     * @param tid 결제 고유 ID (카카오페이의 tid, PayPal의 orderId 등)
     */
    record PrepareResult(
            boolean required,
            String tid,
            String redirectUrl,
            String metadata
    ) {
        public static PrepareResult notRequired() {
            return new PrepareResult(false, null, null, null);
        }

        public static PrepareResult success(String tid, String redirectUrl, String metadata) {
            return new PrepareResult(true, tid, redirectUrl, metadata);
        }
    }
}

