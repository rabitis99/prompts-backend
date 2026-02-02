package org.example.sharedprompts.domain.payment.provider.kakao.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.KakaoPayProperties;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakakoReadyResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayJsonConverter;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * KakaoPay 결제 준비 API Client
 * 
 * <p>단일 책임: 결제 준비 API 호출만 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakakoReadyApiClient {

    private static final String KAKAO_PAY_API_URL = "https://kapi.kakao.com/v1/payment";
    private static final String READY_ENDPOINT = "/ready";

    private final KakaoPayProperties properties;
    private final RestTemplate restTemplate;
    private final KakaoPayHeadersProvider headersProvider;
    private final KakaoPayJsonConverter jsonConverter;

    /**
     * 결제 준비 요청
     * 
     * @param orderId 주문 ID (필수)
     * @param userId 사용자 ID (필수)
     * @param amount 결제 금액 (원 단위, 필수)
     * @param itemName 상품명 (필수)
     * @return ReadyResponse
     * @throws IllegalArgumentException 필수 필드 누락 시
     * @throws RuntimeException API 호출 실패 시
     */
    public KakakoReadyResponse ready(String orderId, String userId, long amount, String itemName) {
        validateRequired(orderId, "orderId");
        validateRequired(userId, "userId");
        validateRequired(itemName, "itemName");
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다: amount=" + amount);
        }

        try {
            HttpHeaders headers = headersProvider.createFormHeaders();

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("cid", properties.getCid());
            requestBody.add("partner_order_id", orderId);
            requestBody.add("partner_user_id", userId);
            requestBody.add("item_name", itemName);
            requestBody.add("quantity", "1");
            requestBody.add("total_amount", String.valueOf(amount));
            requestBody.add("tax_free_amount", "0");
            requestBody.add("approval_url", properties.getApprovalUrl());
            requestBody.add("cancel_url", properties.getCancelUrl());
            requestBody.add("fail_url", properties.getFailUrl());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_API_URL + READY_ENDPOINT,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String tid = (String) body.get("tid");
                String redirectUrl = (String) body.get("next_redirect_pc_url");
                
                if (tid == null || tid.isEmpty()) {
                    throw new RuntimeException("KakaoPay ready 응답에 tid가 없습니다");
                }
                if (redirectUrl == null || redirectUrl.isEmpty()) {
                    throw new RuntimeException("KakaoPay ready 응답에 redirectUrl이 없습니다");
                }

                log.info("KakaoPay 결제 준비 성공: tid={}, orderId={}", tid, orderId);
                return new KakakoReadyResponse(tid, redirectUrl, jsonConverter.convertToJson(body));
            }

            throw new RuntimeException("KakaoPay 결제 준비 실패: status=" + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("KakaoPay 결제 준비 API 호출 실패: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new RuntimeException("KakaoPay 결제 준비 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}

