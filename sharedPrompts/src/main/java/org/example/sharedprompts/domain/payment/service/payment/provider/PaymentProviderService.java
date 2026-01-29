package org.example.sharedprompts.domain.payment.service.payment.provider;

import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;

import java.math.BigDecimal;

/**
 * 결제사별 결제 처리 서비스 인터페이스
 * Strategy 패턴 적용
 */
public interface PaymentProviderService {

    /**
     * 이 서비스가 처리할 결제 수단 반환
     */
    PaymentMethod getPaymentMethod();

    /**
     * 결제 승인 요청
     */
    String approvePayment(Payment payment);

    /**
     * 결제 상태 조회
     */
    PaymentStatus checkPaymentStatus(String externalPaymentId);

    /**
     * 결제 취소
     */
    void cancelPayment(String externalPaymentId, String reason);

    /**
     * 결제 환불
     */
    void refundPayment(String externalPaymentId, BigDecimal amount, String reason);

    /**
     * Webhook 서명 검증
     */
    boolean verifyWebhookSignature(String payload, String signature);

    /**
     * Webhook 처리
     */
    void processWebhook(String payload);
}

