package org.example.sharedprompts.domain.payment.provider.toss.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.TossPayProperties;
import org.example.sharedprompts.domain.payment.provider.toss.dto.TossStatusResponse;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossPayJsonConverter;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossPayResponseParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * TossPay 결제 상태 조회 API Client (v1 API)
 *
 * <p>단일 책임: 결제 상태 조회 API 호출만 담당
 * <p>API: GET /v1/payments/{paymentKey}
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class TossStatusApiClient {

    private final TossPayProperties properties;
    private final RestTemplate restTemplate;
    private final TossPayHeadersProvider headersProvider;
    private final TossPayJsonConverter jsonConverter;
    private final TossPayResponseParser responseParser;

    /**
     * 결제 상태 조회
     *
     * @param paymentKey 결제 키 (필수)
     * @return StatusResponse
     * @throws IllegalArgumentException paymentKey가 null이거나 비어있을 때
     * @throws RuntimeException API 호출 실패 시
     */
    public TossStatusResponse status(String paymentKey) {
        validateRequired(paymentKey, "paymentKey");

        try {
            HttpHeaders headers = headersProvider.createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    properties.getBaseUrl() + "/" + paymentKey,
                    HttpMethod.GET,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String status = (String) body.get("status");
                Object totalAmountObj = body.get("totalAmount");
                String currency = (String) body.get("currency");
                String orderId = (String) body.get("orderId");

                validateResponseString(status, "status");
                validateResponseNotNull(totalAmountObj, "totalAmount");
                validateResponseString(currency, "currency");
                validateResponseString(orderId, "orderId");

                BigDecimal totalAmount = new BigDecimal(totalAmountObj.toString());

                log.debug("TossPay 결제 상태 조회 성공: paymentKey={}, status={}", paymentKey, status);
                return new TossStatusResponse(
                        status,
                        totalAmount,
                        currency,
                        orderId,
                        responseParser.parseApprovedAt(body),
                        jsonConverter.convertToJson(body)
                );
            }

            throw new RuntimeException("TossPay 결제 상태 조회 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("TossPay 결제 상태 조회 API 호출 실패: paymentKey={}, error={}", paymentKey, e.getMessage(), e);
            throw new RuntimeException("TossPay 결제 상태 조회 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }

    private void validateResponseString(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new RuntimeException("TossPay status 응답에 " + fieldName + "가 없습니다");
        }
    }

    private void validateResponseNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new RuntimeException("TossPay status 응답에 " + fieldName + "가 없습니다");
        }
    }
}
