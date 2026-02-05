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
        return false;
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
        validateKrwCurrency(currency);

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
            String paymentKey,
            String orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            String userId,
            java.util.Map<String, String> additionalParams
    ) {
        validateKrwCurrency(currency);

        String pgToken = additionalParams != null ? additionalParams.get("pgToken") : null;
        if (pgToken == null || pgToken.isEmpty()) {
            throw new IllegalArgumentException("pgToken은(는) 필수입니다");
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
        try {
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
        if (payload == null || payload.isEmpty() || signature == null || signature.isEmpty()) {
            return false;
        }
        return webhookVerifier.verify(payload, signature);
    }

    @Override
    public WebhookEvent parseWebhook(String payload) {
        try {
            return webhookParser.parse(payload);
        } catch (Exception e) {
            log.error("KakaoPay Webhook 파싱 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("KakaoPay Webhook 파싱 실패: " + e.getMessage(), e);
        }
    }

    private void validateKrwCurrency(String currency) {
        if (!"KRW".equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("KakaoPay는 KRW만 지원합니다: currency=" + currency);
        }
    }
}
