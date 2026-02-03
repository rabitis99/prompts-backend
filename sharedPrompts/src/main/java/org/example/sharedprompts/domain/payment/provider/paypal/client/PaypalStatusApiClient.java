package org.example.sharedprompts.domain.payment.provider.paypal.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.provider.paypal.dto.PaypalOrderDetailsResponse;
import org.example.sharedprompts.domain.payment.provider.paypal.dto.PaypalOrderStatusResponse;
import org.example.sharedprompts.domain.payment.provider.paypal.util.PayPalHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.paypal.util.PayPalJsonConverter;
import org.example.sharedprompts.domain.payment.provider.paypal.util.PayPalResponseParser;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * PayPal 주문 상태 조회 API Client
 *
 * <p>단일 책임: 주문 상태 조회 API 호출만 담당
 */
@Slf4j
@Component("paypalStatusApiClient")
@RequiredArgsConstructor
public class PaypalStatusApiClient {

    private static final String PAYPAL_ORDERS_URL = "https://api-m.paypal.com/v2/checkout/orders";

    private final RestTemplate restTemplate;
    private final PayPalHeadersProvider headersProvider;
    private final PayPalJsonConverter jsonConverter;
    private final PayPalResponseParser responseParser;

    /**
     * 주문 상태 조회
     *
     * @param orderId PayPal 주문 ID (필수)
     * @return OrderStatusResponse
     * @throws IllegalArgumentException orderId가 null이거나 비어있을 때
     * @throws RuntimeException API 호출 실패 시
     */
    public PaypalOrderStatusResponse getOrderStatus(String orderId) {
        validateRequired(orderId, "orderId");

        try {
            HttpHeaders headers = headersProvider.createGetHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_ORDERS_URL + "/" + orderId,
                    HttpMethod.GET,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String status = (String) body.get("status");
                if (status == null || status.isEmpty()) {
                    throw new RuntimeException("PayPal getOrderStatus 응답에 status가 없습니다");
                }

                PayPalResponseParser.OrderInfo orderInfo = responseParser.extractOrderInfo(body);

                log.debug("PayPal 주문 상태 조회 성공: orderId={}, status={}", orderId, status);
                return new PaypalOrderStatusResponse(
                        status,
                        orderInfo.amount(),
                        orderInfo.currency(),
                        responseParser.parseApprovedAt(body),
                        jsonConverter.convertToJson(body)
                );
            }

            throw new RuntimeException("PayPal 주문 상태 조회 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("PayPal 주문 상태 조회 API 호출 실패: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new RuntimeException("PayPal 주문 상태 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 주문 상세 정보 조회 (Authorization/Capture ID 추출용)
     *
     * @param orderId PayPal 주문 ID (필수)
     * @return OrderDetailsResponse
     * @throws IllegalArgumentException orderId가 null이거나 비어있을 때
     * @throws RuntimeException API 호출 실패 시
     */
    public PaypalOrderDetailsResponse getOrderDetails(String orderId) {
        validateRequired(orderId, "orderId");

        try {
            HttpHeaders headers = headersProvider.createGetHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_ORDERS_URL + "/" + orderId,
                    HttpMethod.GET,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String status = (String) body.get("status");
                if (status == null || status.isEmpty()) {
                    throw new RuntimeException("PayPal getOrderDetails 응답에 status가 없습니다");
                }

                String authorizationId = responseParser.extractAuthorizationId(body);
                String captureId = responseParser.extractCaptureId(body);
                PayPalResponseParser.OrderInfo orderInfo = responseParser.extractOrderInfo(body);
                String currency = orderInfo.currency();

                return new PaypalOrderDetailsResponse(status, authorizationId, captureId, currency, jsonConverter.convertToJson(body));
            }

            throw new RuntimeException("PayPal 주문 정보 조회 실패: orderId=" + orderId);
        } catch (RestClientException e) {
            log.error("PayPal 주문 정보 조회 API 호출 실패: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new RuntimeException("PayPal 주문 정보 조회 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}
