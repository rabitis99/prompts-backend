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
    private final boolean fcmEnabled;

    /**
     * Construct a PaymentProperties object with values sourced from Spring configuration properties.
     *
     * Values are injected from the environment using the corresponding property keys; specified defaults
     * are applied when a property is not provided.
     *
     * @param kakaoSecret         KakaoPay secret key (property: payment.kakao.secret).
     * @param kakaoCid            KakaoPay CID, default "TC0ONETIME" (property: payment.kakao.cid).
     * @param tossApiKey          Toss Payments API key (property: payment.toss.api-key).
     * @param tossSecret          Toss Payments secret key (property: payment.toss.secret-key).
     * @param tossBaseUrl         Toss Payments base URL, default "https://api.tosspayments.com/v1/payments" (property: payment.toss.base-url).
     * @param tossConfirmEndpoint Toss Payments confirm endpoint, default "/confirm" (property: payment.toss.confirm-endpoint).
     * @param paypalClientId      PayPal client ID (property: payment.paypal.client-id).
     * @param paypalClientSecret  PayPal client secret (property: payment.paypal.client-secret).
     * @param exchangeRateApiKey  Exchange rate API key (property: payment.exchange-rate.api-key).
     * @param exchangeRateApiUrl  Exchange rate API URL, default "https://api.exchangerate-api.com/v4/latest/" (property: payment.exchange-rate.api-url).
     * @param webhookSecret       Secret used to validate incoming webhooks (property: payment.webhook.secret).
     * @param maxRetryAttempts    Maximum number of retry attempts for transient operations, default 3 (property: payment.retry.max-attempts).
     * @param retryDelayMs        Delay between retry attempts in milliseconds, default 1000 (property: payment.retry.delay-ms).
     * @param cashbackRate        Cashback rate applied to transactions, default 0.01 (property: payment.cashback.rate).
     * @param pointRate           Point accrual rate applied to transactions, default 0.005 (property: payment.point.rate).
     * @param fcmProjectId        Firebase Cloud Messaging project ID for HTTP v1 API (property: payment.fcm.project-id).
     * @param fcmCredentialsPath  File system path to FCM service account credentials (property: payment.fcm.credentials-path).
     * @param fcmEnabled          Whether FCM integration is enabled, default true (property: payment.fcm.enabled).
     */
    public PaymentProperties(
            @Value("${payment.kakao.secret:}") String kakaoSecret,
            @Value("${payment.kakao.cid:TC0ONETIME}") String kakaoCid,
            @Value("${payment.toss.api-key:}") String tossApiKey,
            @Value("${payment.toss.secret-key:}") String tossSecret,
            @Value("${payment.toss.base-url:https://api.tosspayments.com/v1/payments}") String tossBaseUrl,
            @Value("${payment.toss.confirm-endpoint:/confirm}") String tossConfirmEndpoint,
            @Value("${payment.paypal.client-id:}") String paypalClientId,
            @Value("${payment.paypal.client-secret:}") String paypalClientSecret,
            @Value("${payment.exchange-rate.api-key:}") String exchangeRateApiKey,
            @Value("${payment.exchange-rate.api-url:https://api.exchangerate-api.com/v4/latest/}") String exchangeRateApiUrl,
            @Value("${payment.webhook.secret:}") String webhookSecret,
            @Value("${payment.retry.max-attempts:3}") int maxRetryAttempts,
            @Value("${payment.retry.delay-ms:1000}") long retryDelayMs,
            @Value("${payment.cashback.rate:0.01}") double cashbackRate,
            @Value("${payment.point.rate:0.005}") double pointRate,
            @Value("${payment.fcm.project-id:}") String fcmProjectId,
            @Value("${payment.fcm.credentials-path:}") String fcmCredentialsPath,
            @Value("${payment.fcm.enabled:true}") boolean fcmEnabled) {
        this.kakaoSecret = kakaoSecret;
        this.kakaoCid = kakaoCid;
        this.tossApiKey = tossApiKey;
        this.tossSecret = tossSecret;
        this.tossBaseUrl = tossBaseUrl;
        this.tossConfirmEndpoint = tossConfirmEndpoint;
        this.paypalClientId = paypalClientId;
        this.paypalClientSecret = paypalClientSecret;
        this.exchangeRateApiKey = exchangeRateApiKey;
        this.exchangeRateApiUrl = exchangeRateApiUrl;
        this.webhookSecret = webhookSecret;
        this.maxRetryAttempts = maxRetryAttempts;
        this.retryDelayMs = retryDelayMs;
        this.cashbackRate = cashbackRate;
        this.pointRate = pointRate;
        this.fcmProjectId = fcmProjectId;
        this.fcmCredentialsPath = fcmCredentialsPath;
        this.fcmEnabled = fcmEnabled;
    }
}
