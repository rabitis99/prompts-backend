package org.example.sharedprompts.domain.payment.provider.kakao.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.properties.KakaoPayProperties;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoStatusResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayJsonConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * KakaoPay 결제 상태 조회 API Client
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class KakaoStatusApiClient {

    private static final String KAKAO_PAY_API_URL = "https://open-api.kakaopay.com/online/v1/payment";
    private static final String STATUS_ENDPOINT = "/order";

    private final KakaoPayProperties properties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final KakaoPayHeadersProvider headersProvider;
    private final KakaoPayJsonConverter jsonConverter;

    public KakaoStatusResponse status(String tid) {
        validateRequired(tid, "tid");

        try {
            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(createStatusRequestBody(tid), headersProvider.createJsonHeaders());

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_API_URL + STATUS_ENDPOINT,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("KakaoPay status API failed: status=" + response.getStatusCode());
            }

            KakaoStatusResponse result = parseResponse(response.getBody());

            log.debug("KakaoPay status success: tid={}, status={}", tid, result.status());
            return result;
        } catch (RestClientException e) {
            log.error("KakaoPay status failed: tid={}, message={}", tid, e.getMessage());
            throw new RuntimeException("KakaoPay 결제 상태 조회 실패", e);
        }
    }

    /* =========================
       Internal helpers
     ========================= */

    private Map<String, Object> createStatusRequestBody(String tid) {
        validateRequired(properties.getCid(), "cid");

        Map<String, Object> body = new HashMap<>();
        body.put("cid", properties.getCid());
        body.put("tid", tid);
        return body;
    }

    private KakaoStatusResponse parseResponse(Map<String, Object> body) {
        String status = requireText(body.get("status"), "status");
        String orderId = requireText(body.get("partner_order_id"), "partner_order_id");

        @SuppressWarnings("unchecked")
        Map<String, Object> amountMap = (Map<String, Object>) body.get("amount");
        if (amountMap == null) {
            throw new RuntimeException("KakaoPay status 응답에 amount가 없습니다");
        }

        long totalAmount = parseLong(amountMap.get("total"), "amount.total");
        long taxFreeAmount = amountMap.get("tax_free") != null
                ? parseLong(amountMap.get("tax_free"), "amount.tax_free")
                : 0L;

        return new KakaoStatusResponse(
                status,
                orderId,
                totalAmount,
                taxFreeAmount,
                jsonConverter.convertToJson(body)
        );
    }

    private long parseLong(Object value, String fieldName) {
        if (value == null) {
            throw new RuntimeException("KakaoPay status 응답에 " + fieldName + "이 없습니다");
        }
        return Long.parseLong(value.toString());
    }

    private String requireText(Object value, String fieldName) {
        if (value == null || value.toString().isBlank()) {
            throw new RuntimeException("KakaoPay status 응답에 " + fieldName + "이 없습니다");
        }
        return value.toString();
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}