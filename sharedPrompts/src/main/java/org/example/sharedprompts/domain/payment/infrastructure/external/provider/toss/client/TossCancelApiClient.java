package org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.properties.TossPayProperties;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.dto.TossCancelResponse;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.dto.TossRefundResponse;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.util.TossCancelErrorHandler;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.util.TossCancelResponseParser;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.util.TossPayHeadersProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * TossPay 결제 취소 / 환불 API Client
 * API: POST /v1/payments/{paymentKey}/cancel
 */
@Slf4j
@Component("tossCancelApiClient")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class TossCancelApiClient {

    private static final String CANCEL_PATH = "/%s/cancel";

    private final TossPayProperties properties;

    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;

    private final TossPayHeadersProvider headersProvider;
    private final TossCancelResponseParser responseParser;
    private final TossCancelErrorHandler errorHandler;

    public TossCancelResponse cancel(String paymentKey, String reason, String idempotencyKey) {
        validateRequest(paymentKey, reason);

        try {
            ResponseEntity<Map<String, Object>> response = executeRequest(paymentKey, createCancelBody(reason), idempotencyKey);
            return parseCancelResponse(paymentKey, response);
        } catch (RestClientException e) {
            throw errorHandler.handleRestClientError(e, paymentKey);
        }
    }

    public TossRefundResponse refund(String paymentKey, long amount, String reason, String idempotencyKey) {
        validateRequest(paymentKey, reason, amount);

        try {
            ResponseEntity<Map<String, Object>> response = executeRequest(paymentKey, createRefundBody(reason, amount), idempotencyKey);
            return parseRefundResponse(paymentKey, amount, response);
        } catch (RestClientException e) {
            throw errorHandler.handleRestClientError(e, paymentKey);
        }
    }

    private void validateRequest(String paymentKey, String reason) {
        validateRequired(paymentKey, "paymentKey");
        validateRequired(reason, "reason");
    }

    private void validateRequest(String paymentKey, String reason, long amount) {
        validateRequest(paymentKey, reason);
        if (amount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다: amount=" + amount);
        }
    }

    private ResponseEntity<Map<String, Object>> executeRequest(String paymentKey, Map<String, Object> requestBody, String idempotencyKey) {
        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headersProvider.createJsonHeaders(idempotencyKey));

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                properties.getBaseUrl() + String.format(CANCEL_PATH, paymentKey),
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("TossPay API 응답 실패: status=" + response.getStatusCode());
        }

        return response;
    }

    private TossCancelResponse parseCancelResponse(String paymentKey, ResponseEntity<Map<String, Object>> response) {
        return responseParser.parseCancel(paymentKey, response.getBody());
    }

    private TossRefundResponse parseRefundResponse(String paymentKey, long requestedAmount, ResponseEntity<Map<String, Object>> response) {
        return responseParser.parseRefund(paymentKey, requestedAmount, response.getBody());
    }

    private Map<String, Object> createCancelBody(String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("cancelReason", reason);
        return body;
    }

    private Map<String, Object> createRefundBody(String reason, long amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("cancelReason", reason);
        body.put("cancelAmount", amount);
        return body;
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}
