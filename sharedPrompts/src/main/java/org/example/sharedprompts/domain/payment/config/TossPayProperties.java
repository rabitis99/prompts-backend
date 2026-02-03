package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 토스페이먼츠 설정 Properties (Immutable)
 *
 * <p>v1 API 사용 (공식 Payment API)
 * - Base URL: https://api.tosspayments.com/v1/payments
 * - 인증: Basic Auth (시크릿키 base64 인코딩)
 * - 참고: /v2는 Payout API 전용 (/v2/payouts, /v2/balances)
 */
@Getter
@Component
public class TossPayProperties {

    private final String apiKey;
    private final String secret;
    private final String baseUrl;
    private final String confirmEndpoint;

    public TossPayProperties(
            @Value("${payment.toss.api-key:}") String apiKey,
            @Value("${payment.toss.secret-key:}") String secret,
            @Value("${payment.toss.base-url:https://api.tosspayments.com/v1/payments}") String baseUrl,
            @Value("${payment.toss.confirm-endpoint:/confirm}") String confirmEndpoint) {
        this.apiKey = apiKey;
        this.secret = secret;
        this.baseUrl = baseUrl;
        this.confirmEndpoint = confirmEndpoint;
    }
}




