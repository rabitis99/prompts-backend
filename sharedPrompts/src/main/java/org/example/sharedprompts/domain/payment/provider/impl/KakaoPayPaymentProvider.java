package org.example.sharedprompts.domain.payment.provider.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.CancelResult;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.model.RefundResult;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.provider.kakao.client.KakaoApproveApiClient;
import org.example.sharedprompts.domain.payment.provider.kakao.client.KakaoCancelApiClient;
import org.example.sharedprompts.domain.payment.provider.kakao.client.KakaoReadyApiClient;
import org.example.sharedprompts.domain.payment.provider.kakao.client.KakaoStatusApiClient;
import org.example.sharedprompts.domain.payment.provider.kakao.mapper.KakaoPayStatusMapper;
import org.example.sharedprompts.domain.payment.provider.kakao.policy.KakaoPayAmountPolicy;
import org.example.sharedprompts.domain.payment.provider.kakao.policy.KakaoPayRefundPolicy;
import org.example.sharedprompts.domain.payment.provider.kakao.webhook.KakaoPayWebhookParser;
import org.example.sharedprompts.domain.payment.provider.kakao.webhook.KakaoPayWebhookVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * KakaoPay Payment Provider 구현체
 *
 * <p>단일 책임: PaymentProvider 인터페이스 구현 및 모듈 조합
 * - client: 외부 API 호출
 * - mapper: 상태 매핑
 * - policy: 금액 검증/변환, 환불 정책
 * - webhook: 서명 검증과 payload 파싱
 *
 * <p>Null 안전성: 모든 public API는 Null 반환 금지
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class KakaoPayPaymentProvider implements PaymentProvider {

    private final KakaoReadyApiClient kakaoReadyApiClient;
    private final KakaoApproveApiClient kakaoApproveApiClient;
    private final KakaoStatusApiClient kakaoStatusApiClient;
    private final KakaoCancelApiClient kakaoCancelApiClient;
    private final KakaoPayAmountPolicy amountPolicy;
    private final KakaoPayRefundPolicy refundPolicy;
    private final KakaoPayStatusMapper statusMapper;
    private final KakaoPayWebhookVerifier webhookVerifier;
    private final KakaoPayWebhookParser webhookParser;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.KAKAO_PAY;
    }

    @Override
    public boolean supportsIdempotency() {
        return false; // KakaoPay는 tid 자체가 고유 식별자
    }

    @Override
    public boolean requiresPreparation() {
        return true;
    }

    @Override
    public PrepareResult preparePayment(
            String orderId,
            BigDecimal amount,
            String currency,
            String itemName,
            String userId
    ) {
        validateRequired(orderId, "orderId");
        validateRequired(amount, "amount");
        validateRequired(currency, "currency");
        validateKrwCurrency(currency);
        validateRequired(itemName, "itemName");
        validateRequired(userId, "userId");

        try {
            long kakaoAmount = amountPolicy.toKakaoAmount(amount);
            var readyResponse = kakaoReadyApiClient.ready(orderId, userId, kakaoAmount, itemName);
            
            log.info("KakaoPay 결제 준비 성공: tid={}, orderId={}", readyResponse.tid(), orderId);
            return PrepareResult.success(
                    readyResponse.tid(),
                    readyResponse.redirectUrl(),
                    readyResponse.metadata()
            );
        } catch (Exception e) {
            log.error("KakaoPay 결제 준비 실패: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new RuntimeException("KakaoPay 결제 준비 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentResult confirmPayment(
            String paymentKey, // KakaoPay에서는 tid
            String orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            String userId,
            java.util.Map<String, String> additionalParams
    ) {
        validateRequired(paymentKey, "paymentKey (tid)");
        validateRequired(orderId, "orderId");
        validateRequired(amount, "amount");
        validateRequired(currency, "currency");
        validateKrwCurrency(currency);
        validateRequired(userId, "userId");

        // KakaoPay 신규 API는 pg_token 필수
        String pgToken = additionalParams != null ? additionalParams.get("pgToken") : null;
        if (pgToken == null || pgToken.isEmpty()) {
            throw new IllegalArgumentException("pgToken은(는) 필수입니다 (KakaoPay 결제 승인 시 필수)");
        }

        try {
            var response = kakaoApproveApiClient.approve(paymentKey, orderId, userId, pgToken);
            PaymentStatus status = statusMapper.map(response.status());

            log.info("KakaoPay 결제 승인 성공: tid={}, orderId={}, status={}", paymentKey, orderId, status);
            return PaymentResult.builder()
                    .externalPaymentId(paymentKey)
                    .orderId(orderId)
                    .amount(amount)
                    .currency(currency)
                    .status(status)
                    .metadata(response.metadata())
                    .approvedAt(response.approvedAt())
                    .build();
        } catch (Exception e) {
            log.error("KakaoPay 결제 승인 실패: tid={}, orderId={}, error={}", paymentKey, orderId, e.getMessage(), e);
            return PaymentResult.builder()
                    .externalPaymentId(paymentKey)
                    .orderId(orderId)
                    .amount(amount)
                    .currency(currency)
                    .status(PaymentStatus.FAILED)
                    .failureReason("KakaoPay API 호출 실패: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResult getPaymentStatus(String externalPaymentId) {
        validateRequired(externalPaymentId, "externalPaymentId");

        try {
            var response = kakaoStatusApiClient.status(externalPaymentId);
            PaymentStatus status = statusMapper.map(response.status());
            BigDecimal amount = amountPolicy.fromKakaoAmount(response.amount());

            log.debug("KakaoPay 결제 상태 조회 성공: tid={}, status={}", externalPaymentId, status);
            return PaymentResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .orderId(response.orderId())
                    .status(status)
                    .amount(amount)
                    .currency("KRW")
                    .metadata(response.metadata())
                    .build();
        } catch (Exception e) {
            log.error("KakaoPay 결제 상태 조회 실패: tid={}, error={}", externalPaymentId, e.getMessage(), e);
            return PaymentResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(PaymentStatus.FAILED)
                    .failureReason("KakaoPay 상태 조회 실패: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public CancelResult cancelPayment(String externalPaymentId, String reason, String idempotencyKey) {
        validateRequired(externalPaymentId, "externalPaymentId");
        validateRequired(reason, "reason");

        try {
            // 취소 전 원본 금액 조회 (취소 API 호출 전에 조회하여 Payment 엔티티에 저장)
            var statusResponse = kakaoStatusApiClient.status(externalPaymentId);
            long totalAmount = statusResponse.amount();
            long taxFreeAmount = statusResponse.taxFreeAmount();
            BigDecimal originalAmount = amountPolicy.fromKakaoAmount(totalAmount);
            BigDecimal originalTaxFreeAmount = amountPolicy.fromKakaoAmount(taxFreeAmount);

            var response = kakaoCancelApiClient.cancel(externalPaymentId, reason, totalAmount, taxFreeAmount);
            
            log.info("KakaoPay 결제 취소 성공: tid={}, originalAmount={}, taxFreeAmount={}", 
                    externalPaymentId, originalAmount, originalTaxFreeAmount);
            return CancelResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(PaymentStatus.CANCELED)
                    .canceledAt(response.canceledAt())
                    .reason(reason)
                    .metadata(response.metadata())
                    .originalAmount(originalAmount)
                    .taxFreeAmount(originalTaxFreeAmount)
                    .build();
        } catch (Exception e) {
            log.error("KakaoPay 결제 취소 실패: tid={}, error={}", externalPaymentId, e.getMessage(), e);
            throw new RuntimeException("KakaoPay 결제 취소 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public RefundResult refundPayment(String externalPaymentId, BigDecimal amount, String reason, String idempotencyKey) {
        validateRequired(externalPaymentId, "externalPaymentId");
        validateRequired(amount, "amount");
        validateRequired(reason, "reason");

        try {
            long kakaoAmount = amountPolicy.toKakaoAmount(amount);
            var response = kakaoCancelApiClient.refund(externalPaymentId, kakaoAmount, reason);
            
            BigDecimal refundedAmount = amountPolicy.fromKakaoAmount(response.refundedAmount());
            PaymentStatus refundStatus = refundPolicy.determineStatus(refundedAmount, amount);

            log.info("KakaoPay 결제 환불 성공: tid={}, refundedAmount={}", externalPaymentId, refundedAmount);
            return RefundResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(refundStatus)
                    .refundedAmount(refundedAmount)
                    .refundedAt(response.refundedAt())
                    .reason(reason)
                    .metadata(response.metadata())
                    .build();
        } catch (Exception e) {
            log.error("KakaoPay 결제 환불 실패: tid={}, amount={}, error={}", externalPaymentId, amount, e.getMessage(), e);
            throw new RuntimeException("KakaoPay 결제 환불 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (payload == null || payload.isEmpty()) {
            log.warn("KakaoPay Webhook 검증 실패: payload가 비어있습니다");
            return false;
        }
        if (signature == null || signature.isEmpty()) {
            log.warn("KakaoPay Webhook 검증 실패: signature가 비어있습니다");
            return false;
        }

        boolean verified = webhookVerifier.verify(payload, signature);
        if (!verified) {
            log.warn("KakaoPay Webhook 서명 검증 실패");
        }
        return verified;
    }

    @Override
    public WebhookEvent parseWebhook(String payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("KakaoPay Webhook payload는 필수입니다");
        }

        try {
            WebhookEvent event = webhookParser.parse(payload);
            log.debug("KakaoPay Webhook 파싱 성공: eventType={}, externalPaymentId={}", 
                    event.eventType(), event.externalPaymentId());
            return event;
        } catch (Exception e) {
            log.error("KakaoPay Webhook 파싱 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("KakaoPay Webhook 파싱 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }

    private void validateRequired(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + "은(는) 0보다 커야 합니다: " + value);
        }
    }

    private void validateKrwCurrency(String currency) {
        if (!"KRW".equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("KakaoPay는 KRW만 지원합니다: currency=" + currency);
        }
    }
}
