package org.example.sharedprompts.domain.payment.provider.kakao.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.KakaoPayProperties;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakakoStatusResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayJsonConverter;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * KakaoPay 결제 상태 조회 API Client
 * 
 * <p>단일 책임: 결제 상태 조회 API 호출만 담당
 */
@Slf4j
@Component("kakaoStatusApiClient")
@RequiredArgsConstructor
public class KakakoStatusApiClient {

    private static final String KAKAO_PAY_API_URL = "https://kapi.kakao.com/v1/payment";
    private static final String STATUS_ENDPOINT = "/order";

    private final KakaoPayProperties properties;
    private final RestTemplate restTemplate;
    private final KakaoPayHeadersProvider headersProvider;
    private final KakaoPayJsonConverter jsonConverter;

    /**
     * 결제 상태 조회
     * 
     * @param tid 결제 고유 ID (필수)
     * @return StatusResponse
     * @throws IllegalArgumentException tid가 null이거나 비어있을 때
     * @throws RuntimeException API 호출 실패 시
     */
    public KakakoStatusResponse status(String tid) {
        validateRequired(tid, "tid");

        try {
            HttpHeaders headers = headersProvider.createFormHeaders();

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("cid", properties.getCid());
            requestBody.put("tid", tid);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_API_URL + STATUS_ENDPOINT,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String status = (String) body.get("status");
                String orderId = (String) body.get("partner_order_id");
                Object amountObj = body.get("amount");
                
                if (status == null || status.isEmpty()) {
                    throw new RuntimeException("KakaoPay status 응답에 status가 없습니다");
                }
                if (orderId == null || orderId.isEmpty()) {
                    throw new RuntimeException("KakaoPay status 응답에 orderId가 없습니다");
                }
                if (amountObj == null) {
                    throw new RuntimeException("KakaoPay status 응답에 amount가 없습니다");
                }

                long amount = Long.parseLong(amountObj.toString());

                log.debug("KakaoPay 결제 상태 조회 성공: tid={}, status={}", tid, status);
                return new KakakoStatusResponse(status, orderId, amount, jsonConverter.convertToJson(body));
            }

            throw new RuntimeException("KakaoPay 결제 상태 조회 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("KakaoPay 결제 상태 조회 API 호출 실패: tid={}, error={}", tid, e.getMessage(), e);
            throw new RuntimeException("KakaoPay 결제 상태 조회 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}

