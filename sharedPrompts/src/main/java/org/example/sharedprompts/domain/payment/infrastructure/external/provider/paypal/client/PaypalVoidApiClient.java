package org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.dto.PaypalVoidResponse;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.util.PayPalHeadersProvider;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.util.PayPalJsonConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

/**
 * PayPal Authorization void API Client
 *
 * <p>단일 책임: Authorization void(취소) API 호출만 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class PaypalVoidApiClient {

    private static final String PAYPAL_PAYMENTS_URL = "https://api-m.paypal.com/v2/payments";

    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final PayPalHeadersProvider headersProvider;
    private final PayPalJsonConverter jsonConverter;

    /**
     * Authorization void (취소)
     *
     * @param authorizationId Authorization ID (필수)
     * @param idempotencyKey 멱등성 키 (선택)
     * @return VoidResponse
     * @throws IllegalArgumentException authorizationId가 null이거나 비어있을 때
     * @throws RuntimeException API 호출 실패 시
     */
    public PaypalVoidResponse voidAuthorization(String authorizationId, String idempotencyKey) {
        validateRequired(authorizationId, "authorizationId");

        try {
            HttpHeaders headers = headersProvider.createJsonHeaders(idempotencyKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_PAYMENTS_URL + "/authorizations/" + authorizationId + "/void",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.NO_CONTENT || response.getStatusCode() == HttpStatus.OK) {
                log.info("PayPal Authorization void 성공: authorizationId={}", authorizationId);
                String metadata = response.getBody() != null ? jsonConverter.convertToJson(response.getBody()) : "{}";
                return new PaypalVoidResponse(Instant.now(), metadata);
            }

            throw new RuntimeException("PayPal Authorization void 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("PayPal Authorization void API 호출 실패: authorizationId={}, error={}", authorizationId, e.getMessage(), e);
            throw new RuntimeException("PayPal Authorization void 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}
