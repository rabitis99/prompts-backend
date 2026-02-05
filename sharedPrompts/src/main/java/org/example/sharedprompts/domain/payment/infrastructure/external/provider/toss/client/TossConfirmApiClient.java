package org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.properties.TossPayProperties;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.dto.TossConfirmResponse;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.util.TossConfirmErrorHandler;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.util.TossConfirmResponseParser;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.util.TossPayHeadersProvider;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * TossPay 결제 승인 API Client
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class TossConfirmApiClient {

    private final TossPayProperties properties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final TossPayHeadersProvider headersProvider;
    private final TossConfirmResponseParser responseParser;
    private final TossConfirmErrorHandler errorHandler;

    public TossConfirmResponse confirm(String paymentKey, String orderId, long amount, String idempotencyKey) {
        validateRequest(paymentKey, orderId, amount);

        try {
            ResponseEntity<Map<String, Object>> response = executeRequest(paymentKey, orderId, amount, idempotencyKey);
            return parseResponse(paymentKey, response);
        } catch (HttpServerErrorException e) {
            throw errorHandler.handleHttpServerError(e, paymentKey, orderId);
        } catch (HttpClientErrorException e) {
            throw errorHandler.handleHttpClientError(e, paymentKey, orderId);
        } catch (RestClientException e) {
            log.error("TossPay 결제 승인 API 호출 실패: paymentKey={}, orderId={}, error={}", 
                    SensitiveDataMasker.maskPaymentKey(paymentKey), orderId, 
                    SensitiveDataMasker.maskSensitiveData(e.getMessage()), e);
            throw new RuntimeException("TossPay 결제 승인 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequest(String paymentKey, String orderId, long amount) {
        if (paymentKey == null || paymentKey.isEmpty()) {
            throw new IllegalArgumentException("paymentKey은(는) 필수입니다");
        }
        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalArgumentException("orderId은(는) 필수입니다");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다: amount=" + amount);
        }
    }

    private ResponseEntity<Map<String, Object>> executeRequest(String paymentKey, String orderId, long amount, String idempotencyKey) {
        HttpHeaders headers = headersProvider.createJsonHeaders(idempotencyKey);
        Map<String, Object> requestBody = createRequestBody(paymentKey, orderId, amount);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        return restTemplate.exchange(
                properties.getBaseUrl() + "/confirm",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
    }

    private Map<String, Object> createRequestBody(String paymentKey, String orderId, long amount) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("paymentKey", paymentKey);
        requestBody.put("orderId", orderId);
        requestBody.put("amount", amount);
        return requestBody;
    }

    private TossConfirmResponse parseResponse(String paymentKey, ResponseEntity<Map<String, Object>> response) {
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("TossPay 결제 승인 실패: status=" + response.getStatusCode());
        }
        return responseParser.parse(paymentKey, response.getBody());
    }
}
