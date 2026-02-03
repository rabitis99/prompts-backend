package org.example.sharedprompts.domain.payment.provider.kakao.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.KakaoPayProperties;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoCancelResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoRefundResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayJsonConverter;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayResponseParser;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * KakaoPay 결제 취소/환불 API Client (신규 API - open-api.kakaopay.com)
 *
 * <p>단일 책임: 결제 취소 및 환불 API 호출만 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoCancelApiClient {

    private static final String KAKAO_PAY_API_URL = "https://open-api.kakaopay.com/online/v1/payment";
    private static final String CANCEL_ENDPOINT = "/cancel";

    private final KakaoPayProperties properties;
    private final RestTemplate restTemplate;
    private final KakaoPayHeadersProvider headersProvider;
    private final KakaoPayJsonConverter jsonConverter;
    private final KakaoPayResponseParser responseParser;
    private final KakaoStatusApiClient kakaoStatusApiClient;

    /**
     * 결제 취소
     * 
     * @param tid 결제 고유 ID (필수)
     * @param reason 취소 사유 (필수)
     * @return CancelResponse
     * @throws IllegalArgumentException 필수 필드 누락 시
     * @throws RuntimeException API 호출 실패 시
     */
    public KakaoCancelResponse cancel(String tid, String reason) {
        validateRequired(tid, "tid");
        validateRequired(reason, "reason");

        try {
            HttpHeaders headers = headersProvider.createJsonHeaders();

            // 전체 취소를 위해 총 금액 조회
            long totalAmount = kakaoStatusApiClient.status(tid).amount();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cid", properties.getCid());
            requestBody.put("tid", tid);
            requestBody.put("cancel_amount", totalAmount);
            requestBody.put("cancel_tax_free_amount", 0);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_API_URL + CANCEL_ENDPOINT,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                log.info("KakaoPay 결제 취소 성공: tid={}", tid);
                return new KakaoCancelResponse(
                        responseParser.parseCanceledAt(body),
                        jsonConverter.convertToJson(body)
                );
            }

            throw new RuntimeException("KakaoPay 결제 취소 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("KakaoPay 결제 취소 API 호출 실패: tid={}, error={}", tid, e.getMessage(), e);
            throw new RuntimeException("KakaoPay 결제 취소 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 결제 환불
     * 
     * @param tid 결제 고유 ID (필수)
     * @param amount 환불 금액 (원 단위, 필수)
     * @param reason 환불 사유 (필수)
     * @return RefundResponse
     * @throws IllegalArgumentException 필수 필드 누락 시
     * @throws RuntimeException API 호출 실패 시
     */
    public KakaoRefundResponse refund(String tid, long amount, String reason) {
        validateRequired(tid, "tid");
        validateRequired(reason, "reason");
        if (amount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다: amount=" + amount);
        }

        try {
            HttpHeaders headers = headersProvider.createJsonHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cid", properties.getCid());
            requestBody.put("tid", tid);
            requestBody.put("cancel_amount", amount);
            requestBody.put("cancel_tax_free_amount", 0);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_API_URL + CANCEL_ENDPOINT,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                // 신규 API는 canceled_amount 객체로 반환됨
                @SuppressWarnings("unchecked")
                Map<String, Object> canceledAmountMap = (Map<String, Object>) body.get("canceled_amount");
                long refundedAmount = amount;
                if (canceledAmountMap != null && canceledAmountMap.get("total") != null) {
                    refundedAmount = Long.parseLong(canceledAmountMap.get("total").toString());
                }

                log.info("KakaoPay 결제 환불 성공: tid={}, amount={}", tid, refundedAmount);
                return new KakaoRefundResponse(
                        refundedAmount,
                        responseParser.parseCanceledAt(body),
                        jsonConverter.convertToJson(body)
                );
            }

            throw new RuntimeException("KakaoPay 결제 환불 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("KakaoPay 결제 환불 API 호출 실패: tid={}, amount={}, error={}", tid, amount, e.getMessage(), e);
            throw new RuntimeException("KakaoPay 결제 환불 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}

