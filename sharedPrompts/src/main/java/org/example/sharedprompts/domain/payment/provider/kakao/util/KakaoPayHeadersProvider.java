package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.config.KakaoPayProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

/**
 * KakaoPay 헤더 제공 유틸리티 (신규 API - open-api.kakaopay.com)
 *
 * <p>단일 책임: KakaoPay API 요청 헤더 생성만 담당
 * <p>신규 API는 SECRET_KEY 인증 방식과 JSON Content-Type 사용
 */
@Component
@RequiredArgsConstructor
public class KakaoPayHeadersProvider {

    private final KakaoPayProperties properties;

    /**
     * 기본 인증 헤더 생성
     *
     * @return HttpHeaders
     * @throws IllegalStateException secret이 설정되지 않았을 때
     */
    public HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String secret = properties.getSecret();
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("KakaoPay secret이 설정되지 않았습니다");
        }
        headers.set("Authorization", "SECRET_KEY " + secret);
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
