package org.example.sharedprompts.domain.payment.provider.paypal.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.properties.PaypalProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * PayPal API 헤더 생성기
 *
 * <p>단일 책임: HTTP 헤더 생성 및 OAuth 토큰 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class PayPalHeadersProvider {

    private static final String PAYPAL_OAUTH_URL = "https://api-m.paypal.com/v1/oauth2/token";

    private final PaypalProperties properties;
    private final RestTemplate restTemplate;

    // Access Token 캐싱을 위한 필드 (Thread-Safe)
    private volatile String cachedAccessToken;
    private volatile long tokenExpiresAt;
    private final Object tokenLock = new Object();

    /**
     * JSON 요청용 헤더 생성
     */
    public HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * JSON 요청용 헤더 생성 (멱등성 키 포함)
     */
    public HttpHeaders createJsonHeaders(String idempotencyKey) {
        HttpHeaders headers = createJsonHeaders();
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            headers.set("PayPal-Request-Id", idempotencyKey);
        }
        return headers;
    }

    /**
     * GET 요청용 헤더 생성
     */
    public HttpHeaders createGetHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        return headers;
    }

    /**
     * PayPal 액세스 토큰 획득 (Thread-Safe)
     */
    public String getAccessToken() {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return cachedAccessToken;
        }

        synchronized (tokenLock) {
            if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiresAt) {
                return cachedAccessToken;
            }

            log.debug("PayPal 액세스 토큰 갱신 시작");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(properties.getClientId(), properties.getClientSecret());

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_OAUTH_URL,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String newToken = (String) response.getBody().get("access_token");
                Object expiresIn = response.getBody().get("expires_in");

                if (newToken == null || newToken.isEmpty()) {
                    throw new RuntimeException("PayPal 액세스 토큰 획득 실패: 응답에 access_token이 없습니다");
                }

                long newExpiresAt = System.currentTimeMillis();
                if (expiresIn != null) {
                    long expiresInSeconds = ((Number) expiresIn).longValue();
                    // 최소 TTL 설정: 300초(5분) 이하인 경우 300초로 설정
                    long effectiveExpiresIn = Math.max(expiresInSeconds, 300);
                    newExpiresAt += (effectiveExpiresIn - 300) * 1000; // 만료 5분 전에 갱신
                } else {
                    // expires_in이 없는 경우 기본값 3600초(1시간) 사용
                    newExpiresAt += (3600 - 300) * 1000; // 만료 5분 전에 갱신
                }

                this.tokenExpiresAt = newExpiresAt;
                this.cachedAccessToken = newToken;

                log.debug("PayPal 액세스 토큰 갱신 완료");
                return newToken;
            }

            throw new RuntimeException("PayPal 액세스 토큰 획득 실패");
        }
    }
}
