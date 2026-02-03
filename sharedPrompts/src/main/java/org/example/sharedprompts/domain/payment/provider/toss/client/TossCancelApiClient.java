package org.example.sharedprompts.domain.payment.provider.toss.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.TossPayProperties;
import org.example.sharedprompts.domain.payment.provider.toss.dto.TossCancelResponse;
import org.example.sharedprompts.domain.payment.provider.toss.dto.TossRefundResponse;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossPayJsonConverter;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossPayResponseParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * TossPay 결제 취소/환불 API Client (v1 API)
 *
 * <p>단일 책임: 결제 취소 및 환불 API 호출만 담당
 * <p>API: POST /v1/payments/{paymentKey}/cancel
 */
@Slf4j
@Component("tossCancelApiClient")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class TossCancelApiClient {

    private final TossPayProperties properties;
    private final RestTemplate restTemplate;
    private final TossPayHeadersProvider headersProvider;
    private final TossPayJsonConverter jsonConverter;
    private final TossPayResponseParser responseParser;

    /**
     * 결제 취소
     *
     * @param paymentKey 결제 키 (필수)
     * @param reason 취소 사유 (필수)
     * @return CancelResponse
     * @throws IllegalArgumentException 필수 필드 누락 시
     * @throws RuntimeException API 호출 실패 시
     */
    public TossCancelResponse cancel(String paymentKey, String reason) {
        validateRequired(paymentKey, "paymentKey");
        validateRequired(reason, "reason");

        try {
            HttpHeaders headers = headersProvider.createJsonHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cancelReason", reason);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    properties.getBaseUrl() + "/" + paymentKey + "/cancel",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                log.info("TossPay 결제 취소 성공: paymentKey={}", paymentKey);
                return new TossCancelResponse(
                        responseParser.parseCanceledAt(body),
                        jsonConverter.convertToJson(body)
                );
            }

            throw new RuntimeException("TossPay 결제 취소 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("TossPay 결제 취소 API 호출 실패: paymentKey={}, error={}", paymentKey, e.getMessage(), e);
            throw new RuntimeException("TossPay 결제 취소 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 결제 환불
     *
     * @param paymentKey 결제 키 (필수)
     * @param amount 환불 금액 (원 단위, 필수)
     * @param reason 환불 사유 (필수)
     * @return RefundResponse
     * @throws IllegalArgumentException 필수 필드 누락 시
     * @throws RuntimeException API 호출 실패 시
     */
    public TossRefundResponse refund(String paymentKey, long amount, String reason) {
        validateRequired(paymentKey, "paymentKey");
        validateRequired(reason, "reason");
        if (amount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다: amount=" + amount);
        }

        try {
            HttpHeaders headers = headersProvider.createJsonHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cancelReason", reason);
            requestBody.put("cancelAmount", amount);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    properties.getBaseUrl() + "/" + paymentKey + "/cancel",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                log.info("TossPay 결제 환불 성공: paymentKey={}, amount={}", paymentKey, amount);
                return new TossRefundResponse(
                        amount,
                        responseParser.parseCanceledAt(body),
                        jsonConverter.convertToJson(body)
                );
            }

            throw new RuntimeException("TossPay 결제 환불 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("TossPay 결제 환불 API 호출 실패: paymentKey={}, amount={}, error={}", paymentKey, amount, e.getMessage(), e);
            throw new RuntimeException("TossPay 결제 환불 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}
