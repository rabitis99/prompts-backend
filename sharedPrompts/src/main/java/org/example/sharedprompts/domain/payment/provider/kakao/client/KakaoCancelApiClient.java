package org.example.sharedprompts.domain.payment.provider.kakao.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.properties.KakaoPayProperties;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoCancelResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoRefundResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoCancelErrorHandler;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoCancelResponseParser;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayHeadersProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

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
    private final KakaoCancelResponseParser responseParser;
    private final KakaoCancelErrorHandler errorHandler;
    private final KakaoStatusApiClient kakaoStatusApiClient;

    public KakaoCancelResponse cancel(
            String tid,
            String reason,
            long totalAmount,
            long taxFreeAmount,
            String idempotencyKey
    ) {
        validateRequired(tid, "tid");
        validateRequired(reason, "reason");
        if (totalAmount <= 0) {
            throw new IllegalArgumentException("취소 금액은 0보다 커야 합니다: totalAmount=" + totalAmount);
        }

        try {
            ResponseEntity<Map<String, Object>> response = executeRequest(tid, totalAmount, taxFreeAmount, idempotencyKey);
            return parseCancelResponse(tid, response);
        } catch (RestClientException e) {
            throw errorHandler.handleCancelError(e, tid);
        }
    }

    public KakaoRefundResponse refund(
            String tid,
            long amount,
            String reason,
            String idempotencyKey
    ) {
        validateRequest(tid, reason, amount);

        try {
            var status = kakaoStatusApiClient.status(tid);

            long cancelTaxFreeAmount = calculateTaxFreeAmount(
                    status.amount(),
                    status.taxFreeAmount(),
                    amount
            );

            ResponseEntity<Map<String, Object>> response = executeRequest(tid, amount, cancelTaxFreeAmount, idempotencyKey);
            return parseRefundResponse(tid, amount, response);
        } catch (RestClientException e) {
            throw errorHandler.handleRefundError(e, tid, amount);
        }
    }

    private void validateRequest(String tid, String reason, long amount) {
        validateRequired(tid, "tid");
        validateRequired(reason, "reason");

        if (amount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다: amount=" + amount);
        }
    }

    private ResponseEntity<Map<String, Object>> executeRequest(String tid, long cancelAmount, long cancelTaxFreeAmount, String idempotencyKey) {
        Map<String, Object> body = createCancelRequestBody(tid, cancelAmount, cancelTaxFreeAmount);

        HttpHeaders headers = headersProvider.createJsonHeaders();
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                KAKAO_PAY_API_URL + CANCEL_ENDPOINT,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("KakaoPay cancel API failed: status=" + response.getStatusCode());
        }

        return response;
    }

    private KakaoCancelResponse parseCancelResponse(String tid, ResponseEntity<Map<String, Object>> response) {
        return responseParser.parseCancel(tid, response.getBody());
    }

    private KakaoRefundResponse parseRefundResponse(String tid, long requestedAmount, ResponseEntity<Map<String, Object>> response) {
        return responseParser.parseRefund(tid, requestedAmount, response.getBody());
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

    private void validateRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}