package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 결제 관련 설정 Properties (Immutable)
 */
@Getter
@Component
public class PaymentProperties {

    // 카카오페이
    private final String kakaoSecret;
    private final String kakaoCid;

    // 토스
    private final String tossApiKey;
    private final String tossSecret;
    private final String tossBaseUrl;
    private final String tossConfirmEndpoint;

    // 페이팔
    private final String paypalClientId;
    private final String paypalClientSecret;
    private final String paypalWebhookId;

    // 환율 API (선택적)
    private final String exchangeRateApiKey;
    private final String exchangeRateApiUrl;

    // Webhook 설정
    private final String webhookSecret;

    // 재시도 설정
    private final int maxRetryAttempts;
    private final long retryDelayMs;

    // 포인트/캐시백 설정
    private final double cashbackRate;
    private final double pointRate;

    // FCM 설정 (HTTP v1)
    private final String fcmProjectId;
    private final String fcmCredentialsPath;
    private final String fcmClasspathResource;
    private final boolean fcmEnabled;

    public PaymentProperties(
            @Value("${payment.kakao.secret:}") String kakaoSecret,
            @Value("${payment.kakao.cid:TC0ONETIME}") String kakaoCid,
            @Value("${payment.toss.api-key:}") String tossApiKey,
            @Value("${payment.toss.secret-key:}") String tossSecret,
            @Value("${payment.toss.base-url:https://api.tosspayments.com/v1/payments}") String tossBaseUrl,
            @Value("${payment.toss.confirm-endpoint:/confirm}") String tossConfirmEndpoint,
            @Value("${payment.paypal.client-id:}") String paypalClientId,
            @Value("${payment.paypal.client-secret:}") String paypalClientSecret,
            @Value("${payment.paypal.webhook-id:}") String paypalWebhookId,
            @Value("${payment.exchange-rate.api-key:}") String exchangeRateApiKey,
            @Value("${payment.exchange-rate.api-url:https://api.exchangerate-api.com/v4/latest/}") String exchangeRateApiUrl,
            @Value("${payment.webhook.secret:}") String webhookSecret,
            @Value("${payment.retry.max-attempts:3}") int maxRetryAttempts,
            @Value("${payment.retry.delay-ms:1000}") long retryDelayMs,
            @Value("${payment.cashback.rate:0.01}") double cashbackRate,
            @Value("${payment.point.rate:0.005}") double pointRate,
            @Value("${payment.fcm.project-id:}") String fcmProjectId,
            @Value("${payment.fcm.credentials-path:}") String fcmCredentialsPath,
            @Value("${payment.fcm.classpath-resource:firebase/firebase-adminsdk.json}") String fcmClasspathResource,
            @Value("${payment.fcm.enabled:true}") boolean fcmEnabled) {
        this.kakaoSecret = kakaoSecret;
        this.kakaoCid = kakaoCid;
        this.tossApiKey = tossApiKey;
        this.tossSecret = tossSecret;
        this.tossBaseUrl = tossBaseUrl;
        this.tossConfirmEndpoint = tossConfirmEndpoint;
        this.paypalClientId = paypalClientId;
        this.paypalClientSecret = paypalClientSecret;
        this.paypalWebhookId = paypalWebhookId;
        this.exchangeRateApiKey = exchangeRateApiKey;
        this.exchangeRateApiUrl = exchangeRateApiUrl;
        this.webhookSecret = webhookSecret;
        this.maxRetryAttempts = maxRetryAttempts;
        this.retryDelayMs = retryDelayMs;
        this.cashbackRate = cashbackRate;
        this.pointRate = pointRate;
        this.fcmProjectId = fcmProjectId;
        this.fcmCredentialsPath = fcmCredentialsPath;
        this.fcmClasspathResource = fcmClasspathResource;
        this.fcmEnabled = fcmEnabled;
    }
}

