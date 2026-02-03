package org.example.sharedprompts.domain.payment.provider.kakao.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.KakaoPayProperties;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakakoApproveResponse;
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
 * KakaoPay 결제 승인 API Client (신규 API - open-api.kakaopay.com)
 *
 * <p>단일 책임: 결제 승인 API 호출만 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakakoApproveApiClient {

    private static final String KAKAO_PAY_API_URL = "https://open-api.kakaopay.com/online/v1/payment";
    private static final String APPROVE_ENDPOINT = "/approve";

    private final KakaoPayProperties properties;
    private final RestTemplate restTemplate;
    private final KakaoPayHeadersProvider headersProvider;
    private final KakaoPayJsonConverter jsonConverter;
    private final KakaoPayResponseParser responseParser;

    /**
     * 결제 승인 요청
     *
     * @param tid 결제 고유 ID (필수)
     * @param orderId 주문 ID (필수)
     * @param userId 사용자 ID (필수, ready 시 사용한 partner_user_id와 동일해야 함)
     * @param pgToken 결제 승인 토큰 (필수, 클라이언트에서 리다이렉트 시 전달받음)
     * @return ApproveResponse
     * @throws IllegalArgumentException 필수 필드 누락 시
     * @throws RuntimeException API 호출 실패 시
     */
    public KakakoApproveResponse approve(String tid, String orderId, String userId, String pgToken) {
        validateRequired(tid, "tid");
        validateRequired(orderId, "orderId");
        validateRequired(userId, "userId");
        validateRequired(pgToken, "pgToken");

        try {
            HttpHeaders headers = headersProvider.createJsonHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cid", properties.getCid());
            requestBody.put("tid", tid);
            requestBody.put("partner_order_id", orderId);
            requestBody.put("partner_user_id", userId);
            requestBody.put("pg_token", pgToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_API_URL + APPROVE_ENDPOINT,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String status = (String) body.get("status");
                
                if (status == null || status.isEmpty()) {
                    throw new RuntimeException("KakaoPay approve 응답에 status가 없습니다");
                }

                log.info("KakaoPay 결제 승인 성공: tid={}, orderId={}, status={}", tid, orderId, status);
                return new KakakoApproveResponse(
                        status,
                        responseParser.parseApprovedAt(body),
                        jsonConverter.convertToJson(body)
                );
            }

            throw new RuntimeException("KakaoPay 결제 승인 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("KakaoPay 결제 승인 API 호출 실패: tid={}, orderId={}, error={}", tid, orderId, e.getMessage(), e);
            throw new RuntimeException("KakaoPay 결제 승인 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}

