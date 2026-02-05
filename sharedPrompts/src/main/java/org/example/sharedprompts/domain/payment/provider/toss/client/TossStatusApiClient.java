package org.example.sharedprompts.domain.payment.provider.toss.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.properties.TossPayProperties;
import org.example.sharedprompts.domain.payment.provider.toss.dto.TossStatusResponse;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossStatusErrorHandler;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossStatusResponseParser;
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

import java.util.Map;

/**
 * TossPay 결제 상태 조회 API Client (v1 API)
 *
 * API: GET /v1/payments/{paymentKey}
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class TossStatusApiClient {

    private final TossPayProperties properties;

    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;

    private final TossPayHeadersProvider headersProvider;
    private final TossStatusResponseParser responseParser;
    private final TossStatusErrorHandler errorHandler;

    public TossStatusResponse status(String paymentKey) {
        validateRequired(paymentKey, "paymentKey");

        try {
            ResponseEntity<Map<String, Object>> response = executeRequest(paymentKey);
            return parseResponse(response);
        } catch (HttpClientErrorException e) {
            throw errorHandler.handleHttpClientError(e, paymentKey);
        } catch (HttpServerErrorException e) {
            throw errorHandler.handleHttpServerError(e, paymentKey);
        } catch (RestClientException e) {
            log.error("TossPay 결제 상태 조회 API 호출 실패: paymentKey={}, error={}", 
                    SensitiveDataMasker.maskPaymentKey(paymentKey), 
                    SensitiveDataMasker.maskSensitiveData(e.getMessage()), e);
            throw new RuntimeException("TossPay 결제 상태 조회 실패: " + e.getMessage(), e);
        }
    }

    private ResponseEntity<Map<String, Object>> executeRequest(String paymentKey) {
        HttpHeaders headers = headersProvider.createHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        return restTemplate.exchange(
                properties.getBaseUrl() + "/" + paymentKey,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
    }

    private TossStatusResponse parseResponse(ResponseEntity<Map<String, Object>> response) {
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("TossPay 결제 상태 조회 실패: status=" + response.getStatusCode());
        }
        return responseParser.parse(response.getBody());
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}
