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

    @PostConstruct
    public void validateConfiguration() {
        String secret = properties.getSecret();
        if (secret == null || secret.trim().isEmpty()) {
            log.error("KakaoPay Secret Key가 설정되지 않았습니다. PAYMENT_KAKAO_SECRET 환경변수를 확인하세요.");
        } else {
            // Secret Key가 설정되었는지 확인 (길이만 로깅, 실제 값은 노출하지 않음)
            log.info("KakaoPay Secret Key가 설정되었습니다. (길이: {}자)", secret.trim().length());
        }
    }

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
            throw new IllegalStateException("KakaoPay secret이 설정되지 않았습니다. 환경변수 PAYMENT_KAKAO_SECRET을 확인하세요.");
        }
        // 공백 제거 (환경변수에서 오는 경우 공백이 포함될 수 있음)
        secret = secret.trim();
        if (secret.isEmpty()) {
            throw new IllegalStateException("KakaoPay secret이 비어있습니다. 환경변수 PAYMENT_KAKAO_SECRET을 확인하세요.");
        }
        
        // KakaoPay 신규 API (open-api.kakaopay.com)는 SECRET_KEY 형식 사용
        // 형식: "SECRET_KEY {secret_key}"
        String authorizationHeader = "SECRET_KEY " + secret;
        headers.set("Authorization", authorizationHeader);
        
        // 디버깅: Authorization 헤더 형식 확인 (실제 값은 마스킹)
        log.debug("KakaoPay Authorization 헤더 생성: SECRET_KEY {} (secret 길이: {}자)", 
                secret.substring(0, Math.min(4, secret.length())) + "***", secret.length());
        
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
