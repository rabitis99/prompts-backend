package org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.client;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.properties.KakaoPayProperties;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.dto.KakaoApproveResponse;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.util.KakaoApproveErrorHandler;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.util.KakaoApproveResponseParser;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.util.KakaoPayHeadersProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * KakaoPay 결제 승인 API Client
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class KakaoApproveApiClient {

    private static final String KAKAO_PAY_API_URL = "https://open-api.kakaopay.com/online/v1/payment";
    private static final String APPROVE_ENDPOINT = "/approve";

    private final KakaoPayProperties properties;
    private final RestTemplate restTemplate;
    private final KakaoPayHeadersProvider headersProvider;
    private final KakaoApproveResponseParser responseParser;
    private final KakaoApproveErrorHandler errorHandler;

    public KakaoApproveApiClient(
            KakaoPayProperties properties,
            @Qualifier("paymentRestTemplate") RestTemplate restTemplate,
            KakaoPayHeadersProvider headersProvider,
            KakaoApproveResponseParser responseParser,
            KakaoApproveErrorHandler errorHandler
    ) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.headersProvider = headersProvider;
        this.responseParser = responseParser;
        this.errorHandler = errorHandler;
    }

    public KakaoApproveResponse approve(String tid, String orderId, String userId, String pgToken, String idempotencyKey) {
        validateRequest(tid, orderId, userId, pgToken);

        try {
            ResponseEntity<Map<String, Object>> response = executeRequest(tid, orderId, userId, pgToken, idempotencyKey);
            return parseResponse(tid, orderId, response);
        } catch (HttpClientErrorException e) {
            throw errorHandler.handleHttpClientError(e, tid, orderId);
        } catch (RestClientException e) {
            throw errorHandler.handleRestClientError(e, tid, orderId);
        }
    }

    private void validateRequest(String tid, String orderId, String userId, String pgToken) {
        validateRequired(tid, "tid");
        validateRequired(orderId, "orderId");
        validateRequired(userId, "userId");
        validateRequired(pgToken, "pgToken");
    }

    private ResponseEntity<Map<String, Object>> executeRequest(String tid, String orderId, String userId, String pgToken, String idempotencyKey) {
        HttpHeaders headers = headersProvider.createJsonHeaders(idempotencyKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("cid", properties.getCid());
        requestBody.put("tid", tid);
        requestBody.put("partner_order_id", orderId);
        requestBody.put("partner_user_id", userId);
        requestBody.put("pg_token", pgToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        return restTemplate.exchange(
                KAKAO_PAY_API_URL + APPROVE_ENDPOINT,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {
                }
        );
    }

    private KakaoApproveResponse parseResponse(String tid, String orderId, ResponseEntity<Map<String, Object>> response) {
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw errorHandler.handleUnexpectedResponse(response.getStatusCode(), tid, orderId);
        }
        return responseParser.parse(tid, orderId, response.getBody());
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}
