package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.config.KakaoPayProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
     * 기본 인증 헤더 생성 (신규 API용)
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
        // 신규 API: SECRET_KEY {secret_key}
        headers.set("Authorization", "SECRET_KEY " + secret);
        return headers;
    }

    /**
     * JSON 헤더 생성 (신규 API 기본)
     *
     * @return HttpHeaders
     */
    public HttpHeaders createJsonHeaders() {
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Form URL Encoded 헤더 생성 (레거시 호환용)
     *
     * @return HttpHeaders
     * @deprecated 신규 API는 JSON 사용. createJsonHeaders() 사용 권장
     */
    @Deprecated
    public HttpHeaders createFormHeaders() {
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }
}

