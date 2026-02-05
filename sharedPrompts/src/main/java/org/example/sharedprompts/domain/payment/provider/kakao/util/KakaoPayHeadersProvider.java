package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.properties.KakaoPayProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

/**
 * KakaoPay 헤더 제공 유틸리티 (신규 API - open-api.kakaopay.com)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class KakaoPayHeadersProvider {

    private final KakaoPayProperties properties;

    public HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String secret = properties.getSecret();
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("KakaoPay secret이 설정되지 않았습니다. 환경변수 PAYMENT_KAKAO_SECRET을 확인하세요.");
        }
        // 공백 제거 (환경변수에서 오는 경우 공백이 포함될 수 있음)
        secret = secret.trim();
        if (secret.isEmpty()) {
            throw new IllegalStateException("KakaoPay secret이 비어있습니다. 환경변수 PAYMENT_KAKAO_SECRET을 확인하세요.");
        }

        String authValue;
        if (secret.startsWith("SECRET_KEY ")) {
            authValue = secret;
        } else if (secret.startsWith("SECRET_KEY")) {
            // "SECRET_KEY"만 있고 공백이 없는 경우 처리
            authValue = "SECRET_KEY " + secret.substring("SECRET_KEY".length()).trim();
        } else {
            authValue = "SECRET_KEY " + secret;
        }
        headers.set(HttpHeaders.AUTHORIZATION, authValue);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        return headers;
    }

    /**
     * JSON 헤더 생성
     *
     * @return HttpHeaders
     */
    public HttpHeaders createJsonHeaders() {
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
