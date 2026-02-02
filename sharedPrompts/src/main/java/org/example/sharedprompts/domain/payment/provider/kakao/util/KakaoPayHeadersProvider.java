package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.config.KakaoPayProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * KakaoPay 헤더 제공 유틸리티
 * 
 * <p>단일 책임: KakaoPay API 요청 헤더 생성만 담당
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
        headers.set("Authorization", "KakaoAK " + secret);
        return headers;
    }

    /**
     * Form URL Encoded 헤더 생성
     * 
     * @return HttpHeaders
     */
    public HttpHeaders createFormHeaders() {
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }
}

