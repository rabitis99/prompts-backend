package org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.client;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.properties.KakaoPayProperties;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.dto.KakaoReadyResponse;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.util.KakaoPayHeadersProvider;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.util.KakaoReadyErrorHandler;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.util.KakaoReadyResponseParser;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * KakaoPay 결제 준비 API Client
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class KakaoReadyApiClient {

    private static final String KAKAO_PAY_API_URL = "https://open-api.kakaopay.com/online/v1/payment";
    private static final String READY_ENDPOINT = "/ready";

    private final KakaoPayProperties properties;
    private final RestTemplate restTemplate;
    private final KakaoPayHeadersProvider headersProvider;
    private final KakaoReadyResponseParser responseParser;
    private final KakaoReadyErrorHandler errorHandler;

    public KakaoReadyApiClient(
            KakaoPayProperties properties,
            @Qualifier("paymentRestTemplate") RestTemplate restTemplate,
            KakaoPayHeadersProvider headersProvider,
            KakaoReadyResponseParser responseParser,
            KakaoReadyErrorHandler errorHandler
    ) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.headersProvider = headersProvider;
        this.responseParser = responseParser;
        this.errorHandler = errorHandler;
    }

    public KakaoReadyResponse ready(
            String orderId,
            String userId,
            long amount,
            String itemName,
            String idempotencyKey
    ) {
        validateRequest(orderId, userId, itemName, amount);

        try {
            ResponseEntity<Map<String, Object>> response = executeRequest(orderId, userId, amount, itemName, idempotencyKey);
            return parseResponse(orderId, response);
        } catch (HttpClientErrorException e) {
            throw errorHandler.handleHttpClientError(e, orderId);
        } catch (RestClientException e) {
            throw errorHandler.handleRestClientError(e, orderId);
        }
    }

    private void validateRequest(String orderId, String userId, String itemName, long amount) {
        validateRequired(orderId, "orderId");
        validateRequired(userId, "userId");
        validateRequired(itemName, "itemName");

        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다: amount=" + amount);
        }
    }

    private ResponseEntity<Map<String, Object>> executeRequest(String orderId, String userId, long amount, String itemName, String idempotencyKey) {
        Map<String, Object> body = createReadyRequestBody(orderId, userId, amount, itemName);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headersProvider.createJsonHeaders(idempotencyKey));

        return restTemplate.exchange(
                KAKAO_PAY_API_URL + READY_ENDPOINT,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {
                }
        );
    }

    private KakaoReadyResponse parseResponse(String orderId, ResponseEntity<Map<String, Object>> response) {
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR,
                    "KakaoPay ready API failed: status=" + response.getStatusCode());
        }
        return responseParser.parse(orderId, response.getBody());
    }

    /* =========================
       Internal helpers
     ========================= */

    private Map<String, Object> createReadyRequestBody(
            String orderId,
            String userId,
            long amount,
            String itemName
    ) {
        validateRequired(properties.getCid(), "cid");
        validateRequired(properties.getApprovalUrl(), "approvalUrl");
        validateRequired(properties.getCancelUrl(), "cancelUrl");
        validateRequired(properties.getFailUrl(), "failUrl");

        Map<String, Object> body = new HashMap<>();
        body.put("cid", properties.getCid());
        body.put("partner_order_id", orderId);
        body.put("partner_user_id", userId);
        body.put("item_name", itemName);
        body.put("quantity", 1);
        body.put("total_amount", amount);
        body.put("tax_free_amount", 0);
        body.put("approval_url", buildApprovalUrl(orderId, amount));
        body.put("cancel_url", properties.getCancelUrl());
        body.put("fail_url", properties.getFailUrl());

        log.debug("KakaoPay ready request: orderId={}, userId={}, amount={}",
                orderId, maskUserId(userId), amount);

        return body;
    }

    private String buildApprovalUrl(String orderId, long amount) {
        return UriComponentsBuilder
                .fromUriString(properties.getApprovalUrl())
                .queryParam("orderId", orderId)
                .queryParam("amount", amount)
                .build(true)
                .toUriString();
    }

    private String maskUserId(String userId) {
        try {
            return SensitiveDataMasker.maskUserId(Long.parseLong(userId));
        } catch (NumberFormatException e) {
            return SensitiveDataMasker.maskString(userId, 0, Math.max(2, userId.length() - 2));
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}