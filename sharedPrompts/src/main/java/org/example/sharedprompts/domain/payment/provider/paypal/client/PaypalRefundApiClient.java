package org.example.sharedprompts.domain.payment.provider.paypal.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.provider.paypal.dto.PaypalRefundResponse;
import org.example.sharedprompts.domain.payment.provider.paypal.util.PayPalHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.paypal.util.PayPalJsonConverter;
import org.example.sharedprompts.domain.payment.provider.paypal.util.PayPalResponseParser;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * PayPal 환불 API Client
 *
 * <p>단일 책임: 환불 API 호출만 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaypalRefundApiClient {

    private static final String PAYPAL_PAYMENTS_URL = "https://api-m.paypal.com/v2/payments";

    private final RestTemplate restTemplate;
    private final PayPalHeadersProvider headersProvider;
    private final PayPalJsonConverter jsonConverter;
    private final PayPalResponseParser responseParser;

    /**
     * 환불
     *
     * @param captureId Capture ID (필수)
     * @param amount 환불 금액 (필수)
     * @param currency 통화 코드 (필수)
     * @param reason 환불 사유 (필수)
     * @param idempotencyKey 멱등성 키 (선택)
     * @return RefundResponse
     * @throws IllegalArgumentException 필수 필드 누락 시
     * @throws RuntimeException API 호출 실패 시
     */
    public PaypalRefundResponse refund(String captureId, BigDecimal amount, String currency, String reason, String idempotencyKey) {
        validateRequired(captureId, "captureId");
        validateRequired(amount, "amount");
        validateRequired(currency, "currency");
        validateRequired(reason, "reason");

        try {
            HttpHeaders headers = headersProvider.createJsonHeaders(idempotencyKey);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("currency_code", currency);
            amountMap.put("value", amount.toString());
            requestBody.put("amount", amountMap);
            requestBody.put("note_to_payer", reason);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_PAYMENTS_URL + "/captures/" + captureId + "/refund",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if ((response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK)
                    && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                BigDecimal refundedAmount = responseParser.extractRefundedAmount(body, amount);
                String status = (String) body.get("status");

                log.info("PayPal 결제 환불 성공: captureId={}, refundedAmount={}", captureId, refundedAmount);
                return new PaypalRefundResponse(
                        refundedAmount,
                        status != null ? status : "COMPLETED",
                        LocalDateTime.now(),
                        jsonConverter.convertToJson(body)
                );
            }

            throw new RuntimeException("PayPal 결제 환불 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("PayPal 결제 환불 API 호출 실패: captureId={}, amount={}, error={}", captureId, amount, e.getMessage(), e);
            throw new RuntimeException("PayPal 결제 환불 실패: " + e.getMessage(), e);
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
