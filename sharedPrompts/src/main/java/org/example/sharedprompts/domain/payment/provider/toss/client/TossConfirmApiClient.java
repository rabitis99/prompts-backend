package org.example.sharedprompts.domain.payment.provider.toss.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.TossPayProperties;
import org.example.sharedprompts.domain.payment.provider.toss.dto.TossConfirmResponse;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossPayJsonConverter;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossPayResponseParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * TossPay 결제 승인 API Client (v1 API)
 *
 * <p>단일 책임: 결제 승인 API 호출만 담당
 * <p>API: POST /v1/payments/confirm
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class TossConfirmApiClient {

    private final TossPayProperties properties;
    private final RestTemplate restTemplate;
    private final TossPayHeadersProvider headersProvider;
    private final TossPayJsonConverter jsonConverter;
    private final TossPayResponseParser responseParser;

    /**
     * 결제 승인 요청
     *
     * @param paymentKey 결제 키 (필수)
     * @param orderId 주문 ID (필수)
     * @param amount 결제 금액 (원 단위, 필수)
     * @return ConfirmResponse
     * @throws IllegalArgumentException 필수 필드 누락 시
     * @throws RuntimeException API 호출 실패 시
     */
    public TossConfirmResponse confirm(String paymentKey, String orderId, long amount) {
        validateRequired(paymentKey, "paymentKey");
        validateRequired(orderId, "orderId");
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다: amount=" + amount);
        }

        try {
            HttpHeaders headers = headersProvider.createJsonHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("paymentKey", paymentKey);
            requestBody.put("orderId", orderId);
            requestBody.put("amount", amount);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    properties.getBaseUrl() + "/confirm",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String status = (String) body.get("status");
                Object totalAmountObj = body.get("totalAmount");
                String currency = (String) body.get("currency");
                String orderIdFromResponse = (String) body.get("orderId");

                if (status == null || status.isEmpty()) {
                    throw new RuntimeException("TossPay confirm 응답에 status가 없습니다");
                }
                if (totalAmountObj == null) {
                    throw new RuntimeException("TossPay confirm 응답에 totalAmount가 없습니다");
                }
                if (currency == null || currency.isEmpty()) {
                    throw new RuntimeException("TossPay confirm 응답에 currency가 없습니다");
                }
                if (orderIdFromResponse == null || orderIdFromResponse.isEmpty()) {
                    throw new RuntimeException("TossPay confirm 응답에 orderId가 없습니다");
                }

                BigDecimal totalAmount;
                try {
                    totalAmount = new BigDecimal(totalAmountObj.toString());
                } catch (NumberFormatException e) {
                    log.error("TossPay confirm 응답의 totalAmount 형식이 올바르지 않습니다: {}", totalAmountObj);
                    throw new RuntimeException("TossPay confirm 응답의 totalAmount 형식이 올바르지 않습니다: " + totalAmountObj, e);
                }

                log.info("TossPay 결제 승인 성공: paymentKey={}, orderId={}, status={}", paymentKey, orderId, status);
                return new TossConfirmResponse(
                        paymentKey,
                        status,
                        totalAmount,
                        currency,
                        orderIdFromResponse,
                        responseParser.parseApprovedAt(body),
                        jsonConverter.convertToJson(body)
                );
            }

            throw new RuntimeException("TossPay 결제 승인 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("TossPay 결제 승인 API 호출 실패: paymentKey={}, orderId={}, error={}", paymentKey, orderId, e.getMessage(), e);
            throw new RuntimeException("TossPay 결제 승인 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}
