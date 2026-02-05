package org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.properties.PaypalProperties;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.util.PayPalHeadersProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class PayPalWebhookVerifier {

    private static final String VERIFY_ENDPOINT_PATH = "/v1/notifications/verify-webhook-signature";

    private final PaypalProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PayPalHeadersProvider headersProvider;

    /**
     * Webhook 서명 검증
     * 
     * <p>PayPal은 POST /v1/notifications/verify-webhook-signature 엔드포인트를 사용합니다.
     * (권장) headers(Map) 기반으로 다음 헤더 정보를 전달받아 검증합니다:
     * - transmissionId: PAYPAL-TRANSMISSION-ID 헤더
     * - transmissionTime: PAYPAL-TRANSMISSION-TIME 헤더
     * - certUrl: PAYPAL-CERT-URL 헤더
     * - authAlgo: PAYPAL-AUTH-ALGO 헤더
     * - transmissionSig: PAYPAL-TRANSMISSION-SIG 헤더
     * 
     * @param payload Webhook 페이로드 (필수)
     * @param headers 서명 검증에 필요한 PayPal 헤더들
     * @return 검증 성공 여부
     */
    public boolean verify(String payload, Map<String, String> headers) {
        if (payload == null || payload.isEmpty()) {
            log.warn("PayPal Webhook 검증 실패: payload가 비어있습니다");
            return false;
        }
        if (headers == null || headers.isEmpty()) {
            log.warn("PayPal Webhook 검증 실패: headers가 비어있습니다");
            return false;
        }

        try {
            String webhookId = properties.getWebhookId();
            if (webhookId == null || webhookId.isEmpty()) {
                log.warn("PayPal Webhook ID가 설정되지 않았습니다");
                return false;
            }

            // 헤더 키는 클라이언트/서버에 따라 대소문자 변형이 있을 수 있으므로 case-insensitive로 접근
            Map<String, String> normalized = new HashMap<>();
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey() != null) {
                    normalized.put(e.getKey().toLowerCase(), e.getValue());
                }
            }

            String transmissionId = normalized.get("paypal-transmission-id");
            String transmissionTime = normalized.get("paypal-transmission-time");
            String certUrl = normalized.get("paypal-cert-url");
            String authAlgo = normalized.get("paypal-auth-algo");
            String transmissionSig = normalized.get("paypal-transmission-sig");

            if (transmissionId == null || transmissionSig == null) {
                log.warn("PayPal Webhook 서명 검증에 필요한 헤더가 누락되었습니다: transmissionId={}, transmissionSig={}",
                        transmissionId != null, transmissionSig != null);
                return false;
            }
            
            // cert_url, auth_algo, transmission_time도 필수일 수 있으므로 검증
            if (certUrl == null || authAlgo == null || transmissionTime == null) {
                log.warn("PayPal Webhook 서명 검증에 필요한 추가 헤더가 누락되었습니다: certUrl={}, authAlgo={}, transmissionTime={}",
                        certUrl != null, authAlgo != null, transmissionTime != null);
                // API 호출 시 실패할 수 있지만, 일부 환경에서는 선택적일 수 있으므로 경고만 로깅
            }

            // PayPal verify-webhook-signature API 호출
            HttpHeaders requestHeaders = headersProvider.createJsonHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("auth_algo", authAlgo);
            requestBody.put("cert_url", certUrl);
            requestBody.put("transmission_id", transmissionId);
            requestBody.put("transmission_sig", transmissionSig);
            requestBody.put("transmission_time", transmissionTime);
            requestBody.put("webhook_id", webhookId);
            requestBody.put("webhook_event", objectMapper.readValue(payload, Map.class));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, requestHeaders);

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
     * 하위 호환: signature(JSON) 문자열로 전달받던 구버전 API
     */
    public boolean verify(String payload, String signature) {
        if (signature == null || signature.isEmpty()) {
            log.warn("PayPal Webhook 검증 실패: signature가 비어있습니다");
            return false;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> signatureData = objectMapper.readValue(signature, Map.class);
            return verify(payload, signatureData);
        } catch (Exception e) {
            log.error("PayPal Webhook signature(JSON) 파싱 실패: error={}", e.getMessage(), e);
            return false;
        }
    }
}

