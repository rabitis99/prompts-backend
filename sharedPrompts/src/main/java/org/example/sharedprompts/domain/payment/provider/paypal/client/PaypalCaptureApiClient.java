package org.example.sharedprompts.domain.payment.provider.paypal.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.provider.paypal.dto.PaypalCaptureResponse;
import org.example.sharedprompts.domain.payment.provider.paypal.util.PayPalHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.paypal.util.PayPalJsonConverter;
import org.example.sharedprompts.domain.payment.provider.paypal.util.PayPalResponseParser;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * PayPal 결제 캡처 API Client (v2 API - 최신)
 *
 * <p>단일 책임: 결제 캡처(승인) API 호출만 담당
 * <p>API: POST /v2/checkout/orders/{orderId}/capture
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class PaypalCaptureApiClient {

    private static final String PAYPAL_ORDERS_URL = "https://api-m.paypal.com/v2/checkout/orders";

    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final PayPalHeadersProvider headersProvider;
    private final PayPalJsonConverter jsonConverter;
    private final PayPalResponseParser responseParser;

    /**
     * 결제 캡처 (승인)
     *
     * @param orderId PayPal 주문 ID (필수)
     * @param idempotencyKey 멱등성 키 (선택)
     * @return CaptureResponse
     * @throws IllegalArgumentException orderId가 null이거나 비어있을 때
     * @throws RuntimeException API 호출 실패 시
     */
    public PaypalCaptureResponse capture(String orderId, String idempotencyKey) {
        validateRequired(orderId, "orderId");

        try {
            HttpHeaders headers = headersProvider.createJsonHeaders(idempotencyKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_ORDERS_URL + "/" + orderId + "/capture",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if ((response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK)
                    && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String status = (String) body.get("status");
                if (status == null || status.isEmpty()) {
                    throw new RuntimeException("PayPal capture 응답에 status가 없습니다");
                }

                PayPalResponseParser.CaptureInfo captureInfo = responseParser.extractCaptureInfo(body);

                log.info("PayPal 결제 캡처 성공: orderId={}, status={}", orderId, status);
                return new PaypalCaptureResponse(
                        status,
                        captureInfo.amount(),
                        captureInfo.currency(),
                        responseParser.parseApprovedAt(body),
                        jsonConverter.convertToJson(body)
                );
            }

            throw new RuntimeException("PayPal 결제 캡처 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("PayPal 결제 캡처 API 호출 실패: orderId={}, error={}", 
                    orderId, SensitiveDataMasker.maskSensitiveData(e.getMessage()), e);
            throw new RuntimeException("PayPal 결제 캡처 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}
