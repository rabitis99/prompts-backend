package org.example.sharedprompts.domain.payment.provider.kakao.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.properties.KakaoPayProperties;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoCancelResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoRefundResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayJsonConverter;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayResponseParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * KakaoPay 결제 취소 / 환불 API Client
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class KakaoCancelApiClient {

    private static final String KAKAO_PAY_API_URL = "https://open-api.kakaopay.com/online/v1/payment";
    private static final String CANCEL_ENDPOINT = "/cancel";

    private final KakaoPayProperties properties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final KakaoPayHeadersProvider headersProvider;
    private final KakaoPayJsonConverter jsonConverter;
    private final KakaoPayResponseParser responseParser;
    private final KakaoStatusApiClient kakaoStatusApiClient;

    /* =========================
       Public APIs
     ========================= */

    public KakaoCancelResponse cancel(
            String tid,
            String reason,
            long totalAmount,
            long taxFreeAmount
    ) {
        validateRequired(tid, "tid");
        validateRequired(reason, "reason");

        try {
            Map<String, Object> body =
                    createCancelRequestBody(tid, totalAmount, taxFreeAmount);

            ResponseEntity<Map<String, Object>> response = callCancelApi(body);

            Map<String, Object> responseBody = response.getBody();
            log.info("KakaoPay cancel success: tid={}", tid);

            return new KakaoCancelResponse(
                    responseParser.parseCanceledAt(responseBody),
                    jsonConverter.convertToJson(responseBody)
            );
        } catch (RestClientException e) {
            log.error("KakaoPay cancel failed: tid={}, message={}", tid, e.getMessage());
            throw new RuntimeException("KakaoPay 결제 취소 실패", e);
        }
    }

    public KakaoRefundResponse refund(
            String tid,
            long amount,
            String reason
    ) {
        validateRequired(tid, "tid");
        validateRequired(reason, "reason");

        if (amount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다: amount=" + amount);
        }

        try {
            var status = kakaoStatusApiClient.status(tid);

            long cancelTaxFreeAmount = calculateTaxFreeAmount(
                    status.amount(),
                    status.taxFreeAmount(),
                    amount
            );

            Map<String, Object> body =
                    createCancelRequestBody(tid, amount, cancelTaxFreeAmount);

            ResponseEntity<Map<String, Object>> response = callCancelApi(body);

            Map<String, Object> responseBody = response.getBody();
            long refundedAmount = extractRefundedAmount(Objects.requireNonNull(responseBody), amount);

            log.info("KakaoPay refund success: tid={}, amount={}", tid, refundedAmount);

            return new KakaoRefundResponse(
                    refundedAmount,
                    responseParser.parseCanceledAt(responseBody),
                    jsonConverter.convertToJson(responseBody)
            );
        } catch (RestClientException e) {
            log.error("KakaoPay refund failed: tid={}, amount={}, message={}",
                    tid, amount, e.getMessage());
            throw new RuntimeException("KakaoPay 결제 환불 실패", e);
        }
    }

    /* =========================
       Internal helpers
     ========================= */

    private ResponseEntity<Map<String, Object>> callCancelApi(Map<String, Object> body) {
        HttpHeaders headers = headersProvider.createJsonHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                KAKAO_PAY_API_URL + CANCEL_ENDPOINT,
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("KakaoPay cancel API failed: status=" + response.getStatusCode());
        }

        return response;
    }

    private Map<String, Object> createCancelRequestBody(
            String tid,
            long cancelAmount,
            long cancelTaxFreeAmount
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("cid", properties.getCid());
        body.put("tid", tid);
        body.put("cancel_amount", cancelAmount);
        body.put("cancel_tax_free_amount", cancelTaxFreeAmount);
        return body;
    }

    private long calculateTaxFreeAmount(
            long originalAmount,
            long originalTaxFreeAmount,
            long refundAmount
    ) {
        if (originalAmount <= 0 || originalTaxFreeAmount <= 0) {
            return 0;
        }

        return BigDecimal.valueOf(originalTaxFreeAmount)
                .multiply(BigDecimal.valueOf(refundAmount))
                .divide(BigDecimal.valueOf(originalAmount), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    @SuppressWarnings("unchecked")
    private long extractRefundedAmount(Map<String, Object> body, long fallbackAmount) {
        Map<String, Object> canceledAmount =
                (Map<String, Object>) body.get("canceled_amount");

        if (canceledAmount != null && canceledAmount.get("total") != null) {
            return Long.parseLong(canceledAmount.get("total").toString());
        }
        return fallbackAmount;
    }

    private void validateRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}