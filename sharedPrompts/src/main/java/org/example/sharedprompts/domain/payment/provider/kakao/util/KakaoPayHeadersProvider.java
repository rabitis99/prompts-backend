package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.KakaoPayProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * KakaoPay 헤더 제공 유틸리티 (신규 API - open-api.kakaopay.com)
 *
 * <p>단일 책임: KakaoPay API 요청 헤더 생성만 담당
 * <p>신규 API는 SECRET_KEY 인증 방식과 JSON Content-Type 사용
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
