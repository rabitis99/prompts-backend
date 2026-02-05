package org.example.sharedprompts.domain.payment.provider.kakao.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.properties.KakaoPayProperties;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoReadyResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayJsonConverter;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
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
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class KakaoReadyApiClient {

    private static final String KAKAO_PAY_API_URL = "https://open-api.kakaopay.com/online/v1/payment";
    private static final String READY_ENDPOINT = "/ready";

    private final KakaoPayProperties properties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final KakaoPayHeadersProvider headersProvider;
    private final KakaoPayJsonConverter jsonConverter;

    public KakaoReadyResponse ready(
            String orderId,
            String userId,
            long amount,
            String itemName
    ) {
        validateRequired(orderId, "orderId");
        validateRequired(userId, "userId");
        validateRequired(itemName, "itemName");

        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다: amount=" + amount);
        }

        try {
            Map<String, Object> body = createReadyRequestBody(orderId, userId, amount, itemName);

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(body, headersProvider.createJsonHeaders());

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_API_URL + READY_ENDPOINT,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("KakaoPay ready API failed: status=" + response.getStatusCode());
            }

            Map<String, Object> responseBody = response.getBody();
            String tid = (String) responseBody.get("tid");
            String redirectUrl = (String) responseBody.get("next_redirect_pc_url");

            if (!hasText(tid) || !hasText(redirectUrl)) {
                throw new RuntimeException("KakaoPay ready 응답 필수 값 누락");
            }

            log.info("KakaoPay ready success: tid={}, orderId={}", tid, orderId);

            return new KakaoReadyResponse(
                    tid,
                    redirectUrl,
                    jsonConverter.convertToJson(responseBody)
            );
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String masked = SensitiveDataMasker.maskSensitiveData(e.getResponseBodyAsString());
            log.error("KakaoPay ready failed: orderId={}, status={}, error={}",
                    orderId, e.getStatusCode(), masked);

            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new RuntimeException("KakaoPay 인증 실패 (403). 설정값을 확인하세요.", e);
            }
            throw new RuntimeException("KakaoPay 결제 준비 실패", e);
        } catch (RestClientException e) {
            log.error("KakaoPay ready failed: orderId={}, message={}", orderId, e.getMessage());
            throw new RuntimeException("KakaoPay 결제 준비 실패", e);
        }
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
        } catch (Exception e) {
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
