package org.example.sharedprompts.domain.payment.provider.toss.util;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.properties.TossPayProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * TossPay API 헤더 생성기
 *
 * <p>단일 책임: HTTP 헤더 생성
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class TossPayHeadersProvider {

    private final TossPayProperties properties;

    /**
     * 기본 인증 헤더 생성
     */
    public HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String secret = properties.getSecret();
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("TossPay secret이 설정되지 않았습니다");
        }
        String auth = secret + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        return headers;
    }

    /**
     * JSON 요청용 헤더 생성
     */
    public HttpHeaders createJsonHeaders() {
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
