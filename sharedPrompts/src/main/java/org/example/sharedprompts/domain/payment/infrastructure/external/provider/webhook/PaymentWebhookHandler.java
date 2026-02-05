package org.example.sharedprompts.domain.payment.infrastructure.external.provider.webhook;

import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;

import java.util.Map;

/**
 * 결제 Provider별 Webhook 처리 인터페이스
 *
 * - 서명 검증
 * - 페이로드 파싱
 * ※ 상태 변경은 수행하지 않음
 */
public interface PaymentWebhookHandler {

    /** 처리 대상 결제 수단 */
    PaymentMethod getPaymentMethod();

    /** Webhook 서명 검증 */
    boolean verifyWebhookSignature(String payload, String signature);

    /**
     * 헤더 기반 서명 검증 확장 포인트
     *
     * 기본 구현은 signature 헤더를 추출하여 기존 메서드로 위임
     */
    default boolean verifyWebhookSignature(String payload, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return verifyWebhookSignature(payload, (String) null);
        }

        String signature = null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase("signature")) {
                signature = entry.getValue();
                break;
            }
        }

        return verifyWebhookSignature(payload, signature);
    }

    /** Webhook 페이로드 파싱 */
    WebhookEvent parseWebhook(String payload);
}
