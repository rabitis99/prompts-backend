package org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.properties.PaypalProperties;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.dto.PaypalCreateOrderResponse;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.util.PayPalHeadersProvider;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.util.PayPalJsonConverter;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.util.PayPalResponseParser;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PayPal 주문 생성 API Client (v2 API - 최신)
 *
 * <p>단일 책임: 주문 생성(결제 준비) API 호출만 담당
 * <p>API: POST /v2/checkout/orders
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class PaypalCreateOrderApiClient {

    private static final String PAYPAL_ORDERS_URL = "https://api-m.paypal.com/v2/checkout/orders";

    private final PaypalProperties properties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final PayPalHeadersProvider headersProvider;
    private final PayPalJsonConverter jsonConverter;
    private final PayPalResponseParser responseParser;

    /**
     * 주문 생성 (결제 준비)
     *
     * @param orderId 주문 ID (필수)
     * @param amount 결제 금액 (필수)
     * @param currency 통화 코드 (필수)
     * @param itemName 상품명 (선택)
     * @param idempotencyKey 멱등성 키 (선택)
     * @return CreateOrderResponse
     * @throws IllegalArgumentException 필수 필드 누락 시
     * @throws RuntimeException API 호출 실패 시
     */
    public PaypalCreateOrderResponse createOrder(String orderId, BigDecimal amount, String currency, String itemName, String idempotencyKey) {
        validateRequired(orderId, "orderId");
        validateRequired(amount, "amount");
        validateRequired(currency, "currency");

        try {
            HttpHeaders headers = headersProvider.createJsonHeaders(idempotencyKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("intent", "CAPTURE");

            Map<String, Object> purchaseUnit = new HashMap<>();
            purchaseUnit.put("reference_id", orderId);

            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("currency_code", currency);
            amountMap.put("value", amount.toString());
            purchaseUnit.put("amount", amountMap);

            if (itemName != null && !itemName.isEmpty()) {
                purchaseUnit.put("description", itemName);
            }

            requestBody.put("purchase_units", List.of(purchaseUnit));

            Map<String, Object> applicationContext = new HashMap<>();
            // PaypalProperties에서 @NotBlank로 검증되므로 null이 될 수 없음
            applicationContext.put("return_url", properties.getReturnUrl());
            applicationContext.put("cancel_url", properties.getCancelUrl());
            requestBody.put("application_context", applicationContext);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_ORDERS_URL,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if ((response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK)
                    && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String paypalOrderId = (String) body.get("id");
                if (paypalOrderId == null || paypalOrderId.isEmpty()) {
                    throw new RuntimeException("PayPal createOrder 응답에 id가 없습니다");
                }

                String approveUrl = responseParser.extractApproveUrl(body);

                log.info("PayPal 주문 생성 성공: orderId={}, paypalOrderId={}", 
                        orderId, SensitiveDataMasker.maskPaymentKey(paypalOrderId));
                return new PaypalCreateOrderResponse(
                        paypalOrderId,
                        approveUrl,
                        jsonConverter.convertToJson(body)
                );
            }

            throw new RuntimeException("PayPal 주문 생성 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("PayPal 주문 생성 API 호출 실패: orderId={}, error={}", 
                    orderId, SensitiveDataMasker.maskSensitiveData(e.getMessage()), e);
            throw new RuntimeException("PayPal 주문 생성 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }

    private void validateRequired(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + "은(는) 0보다 커야 합니다: " + value);
        }
    }
}
