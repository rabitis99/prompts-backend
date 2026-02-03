package org.example.sharedprompts.domain.payment.provider.paypal.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.PaypalProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * PayPal Webhook Verifier
 * 
 * <p>단일 책임: Webhook 서명 검증만 담당
 * - 상태 변경 없음
 * - Null 안전성 보장
 * - PayPal verify-webhook-signature API 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayPalWebhookVerifier {

    private static final String VERIFY_ENDPOINT_PATH = "/v1/notifications/verify-webhook-signature";

    private final PaypalProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Webhook 서명 검증
     * 
     * <p>PayPal은 POST /v1/notifications/verify-webhook-signature 엔드포인트를 사용합니다.
     * signature 파라미터는 JSON 형태로 다음 헤더 정보를 포함해야 합니다:
     * - transmissionId: PAYPAL-TRANSMISSION-ID 헤더
     * - transmissionTime: PAYPAL-TRANSMISSION-TIME 헤더
     * - certUrl: PAYPAL-CERT-URL 헤더
     * - authAlgo: PAYPAL-AUTH-ALGO 헤더
     * - transmissionSig: PAYPAL-TRANSMISSION-SIG 헤더
     * 
     * @param payload Webhook 페이로드 (필수)
     * @param signature 서명 정보 (JSON 형태, 필수)
     * @return 검증 성공 여부
     */
    public boolean verify(String payload, String signature) {
        if (payload == null || payload.isEmpty()) {
            log.warn("PayPal Webhook 검증 실패: payload가 비어있습니다");
            return false;
        }
        if (signature == null || signature.isEmpty()) {
            log.warn("PayPal Webhook 검증 실패: signature가 비어있습니다");
            return false;
        }

        try {
            String webhookId = properties.getWebhookId();
            if (webhookId == null || webhookId.isEmpty()) {
                log.warn("PayPal Webhook ID가 설정되지 않았습니다");
                return false;
            }

            // signature 파라미터에서 PayPal 헤더 정보 파싱
            @SuppressWarnings("unchecked")
            Map<String, String> signatureData = objectMapper.readValue(signature, Map.class);

            String transmissionId = signatureData.get("transmissionId");
            String transmissionTime = signatureData.get("transmissionTime");
            String certUrl = signatureData.get("certUrl");
            String authAlgo = signatureData.get("authAlgo");
            String transmissionSig = signatureData.get("transmissionSig");

            if (transmissionId == null || transmissionSig == null) {
                log.warn("PayPal Webhook 서명 검증에 필요한 헤더가 누락되었습니다");
                return false;
            }

            // PayPal verify-webhook-signature API 호출
            String accessToken = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("auth_algo", authAlgo);
            requestBody.put("cert_url", certUrl);
            requestBody.put("transmission_id", transmissionId);
            requestBody.put("transmission_sig", transmissionSig);
            requestBody.put("transmission_time", transmissionTime);
            requestBody.put("webhook_id", webhookId);
            requestBody.put("webhook_event", objectMapper.readValue(payload, Map.class));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String verifyEndpoint = properties.getBaseUrl() + VERIFY_ENDPOINT_PATH;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    verifyEndpoint,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String verificationStatus = (String) response.getBody().get("verification_status");
                boolean verified = "SUCCESS".equals(verificationStatus);
                if (!verified) {
                    log.warn("PayPal Webhook 서명 검증 실패: status={}", verificationStatus);
                }
                return verified;
            }

            return false;
        } catch (RestClientException e) {
            log.error("PayPal Webhook 서명 검증 API 호출 실패: error={}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("PayPal Webhook 서명 검증 실패: error={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * PayPal 액세스 토큰 획득 (간단한 구현)
     * 실제로는 PayPalApiClient의 토큰 관리 로직을 공유해야 하지만,
     * 단일 책임 원칙을 위해 여기서도 구현
     */
    private String getAccessToken() {
        // 실제 구현은 PayPalApiClient와 동일한 로직 사용
        // 여기서는 간단히 구현 (실제로는 공통 서비스로 분리 권장)
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(properties.getClientId(), properties.getClientSecret());

            org.springframework.util.MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> request = 
                    new org.springframework.http.HttpEntity<>(body, headers);

            String tokenEndpoint = properties.getBaseUrl() + "/v1/oauth2/token";
            org.springframework.http.ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenEndpoint,
                    org.springframework.http.HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }

            throw new RuntimeException("PayPal 액세스 토큰 획득 실패");
        } catch (Exception e) {
            log.error("PayPal 액세스 토큰 획득 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("PayPal 액세스 토큰 획득 실패", e);
        }
    }
}

