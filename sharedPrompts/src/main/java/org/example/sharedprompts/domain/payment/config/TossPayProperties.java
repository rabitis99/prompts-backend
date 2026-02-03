package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 토스페이먼츠 설정 Properties (Immutable)
 *
 * <p>v2 API 사용 (2024년 릴리즈)
 * - Base URL: https://api.tosspayments.com/v2/payments
 * - 인증: Basic Auth (시크릿키 base64 인코딩)
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
            @Value("${payment.toss.base-url:https://api.tosspayments.com/v2/payments}") String baseUrl,
            @Value("${payment.toss.confirm-endpoint:/confirm}") String confirmEndpoint) {
        this.apiKey = apiKey;
        this.secret = secret;
        this.baseUrl = baseUrl;
        this.confirmEndpoint = confirmEndpoint;
    }
}



