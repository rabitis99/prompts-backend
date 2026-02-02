package org.example.sharedprompts.domain.payment.provider;

import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.model.CancelResult;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.model.RefundResult;

import java.math.BigDecimal;

/**
 * 결제 Provider 인터페이스
 * 
 * <p>단일 책임: 외부 결제 API 호출만 담당
 * - 상태 변경 절대 수행하지 않음
 * - 응답을 공통 도메인 모델(PaymentResult)로 변환하여 반환
 * - 멱등성을 위한 idempotencyKey 제공
 */
public interface PaymentProvider {
    
    /**
     * 이 Provider가 처리할 결제 수단 반환
     */
    PaymentMethod getPaymentMethod();

    /**
     * 멱등성 키 지원 여부 반환
     *
     * <p>결제사별 멱등성 지원 현황:
     * - Toss: 미지원 (paymentKey 자체가 고유 식별자 역할)
     * - KakaoPay: 미지원 (tid 자체가 고유 식별자 역할)
     * - PayPal: 지원 (PayPal-Request-Id 헤더)
     *
     * @return 멱등성 키 지원 여부
     */
    default boolean supportsIdempotency() {
        return false;
    }

    /**
     * 결제 준비 요청 (선택적 구현)
     *
     * <p>일부 결제사(KakaoPay, PayPal)는 결제 승인 전 준비 단계가 필요:
     * - KakaoPay: POST /online/v1/payment/ready → tid 발급
     * - PayPal: POST /v2/checkout/orders → orderId 생성
     * - Toss: 클라이언트에서 결제 위젯이 paymentKey를 발급하므로 서버 준비 불필요
     *
     * @param orderId 주문 ID
     * @param amount 결제 금액
     * @param currency 통화 코드
     * @param itemName 상품명
     * @param userId 사용자 ID
     * @return PrepareResult (tid, 리다이렉션 URL 등)
     */
    default PrepareResult preparePayment(
            String orderId,
            BigDecimal amount,
            String currency,
            String itemName,
            String userId
    ) {
        return PrepareResult.notRequired();
    }

    /**
     * 결제 준비 단계 필요 여부
     *
     * @return 준비 단계 필요 여부
     */
    default boolean requiresPreparation() {
        return false;
    }

    /**
     * 결제 승인/확인 요청
     *
     * <p>각 결제사 공식 권장 방식에 따라 호출:
     * - Toss: POST /v1/payments/confirm
     * - KakaoPay: POST /online/v1/payment/approve (ready 후)
     * - PayPal: POST /v2/checkout/orders/{orderId}/capture
     *
     * @param paymentKey 결제 키 (paymentKey, tid, orderId 등)
     * @param orderId 주문 ID
     * @param amount 결제 금액
     * @param currency 통화 코드
     * @param idempotencyKey 멱등성 키 (중복 호출 방지)
     * @param userId 사용자 ID (KakaoPay의 partner_user_id 등에 사용)
     * @return PaymentResult (외부 API 응답을 도메인 모델로 변환)
     */
    PaymentResult confirmPayment(
            String paymentKey,
            String orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            String userId
    );
    
    /**
     * 결제 상태 조회
     * 
     * @param externalPaymentId 외부 결제 ID
     * @return PaymentResult
     */
    PaymentResult getPaymentStatus(String externalPaymentId);
    
    /**
     * 결제 취소
     *
     * @param externalPaymentId 외부 결제 ID
     * @param reason 취소 사유
     * @param idempotencyKey 멱등성 키
     * @return CancelResult 취소 결과
     */
    CancelResult cancelPayment(String externalPaymentId, String reason, String idempotencyKey);

    /**
     * 결제 환불
     *
     * @param externalPaymentId 외부 결제 ID
     * @param amount 환불 금액
     * @param reason 환불 사유
     * @param idempotencyKey 멱등성 키
     * @return RefundResult 환불 결과
     */
    RefundResult refundPayment(String externalPaymentId, BigDecimal amount, String reason, String idempotencyKey);
    
    /**
     * 환불된 금액 조회
     * 
     * <p>외부 결제사 API에서 실제 환불된 금액을 조회합니다.
     * 모든 결제사가 이 기능을 지원하는 것은 아니므로, 지원하지 않는 경우 Optional.empty()를 반환합니다.
     * 
     * @param externalPaymentId 외부 결제 ID
     * @return 환불된 금액 (지원하지 않는 경우 Optional.empty())
     */
    default java.util.Optional<BigDecimal> getRefundedAmount(String externalPaymentId) {
        // 기본 구현: 지원하지 않는 결제사는 Optional.empty() 반환
        return java.util.Optional.empty();
    }
    
    /**
     * Webhook 서명 검증
     * 
     * @param payload Webhook 페이로드
     * @param signature 서명
     * @return 검증 성공 여부
     */
    boolean verifyWebhookSignature(String payload, String signature);
    
    /**
     * Webhook 페이로드 파싱
     * 
     * <p>WebhookHandler에서 호출하여 이벤트 정보 추출
     * 실제 상태 변경은 하지 않음
     * 
     * @param payload Webhook 페이로드
     * @return WebhookEvent (이벤트 타입, 결제 ID, 상태 등)
     */
    WebhookEvent parseWebhook(String payload);
    
    /**
     * Webhook 이벤트 정보
     */
    record WebhookEvent(
            String eventType,
            String externalPaymentId,
            String orderId,
            PaymentResult paymentResult
    ) {}

    /**
     * 결제 준비 결과
     *
     * @param required 준비 단계 필요 여부
     * @param tid 결제 고유 ID (카카오페이의 tid, PayPal의 orderId 등)
     * @param redirectUrl 사용자 인증을 위한 리다이렉션 URL
     * @param metadata 추가 메타데이터
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

