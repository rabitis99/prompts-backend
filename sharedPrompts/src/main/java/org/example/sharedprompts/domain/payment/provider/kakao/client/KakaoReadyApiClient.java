package org.example.sharedprompts.domain.payment.provider.kakao.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.KakaoPayProperties;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoReadyResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayJsonConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * KakaoPay 결제 준비 API Client (신규 API - open-api.kakaopay.com)
 *
 * <p>단일 책임: 결제 준비 API 호출만 담당
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
    public KakaoReadyResponse ready(String orderId, String userId, long amount, String itemName) {
        validateRequired(orderId, "orderId");
        validateRequired(userId, "userId");
        validateRequired(itemName, "itemName");
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다: amount=" + amount);
        }
        validateRequired(properties.getCid(), "cid");
        validateRequired(properties.getApprovalUrl(), "approvalUrl");
        validateRequired(properties.getCancelUrl(), "cancelUrl");
        validateRequired(properties.getFailUrl(), "failUrl");
        try {
            HttpHeaders headers = headersProvider.createJsonHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cid", properties.getCid());
            requestBody.put("partner_order_id", orderId);
            requestBody.put("partner_user_id", userId);
            requestBody.put("item_name", itemName);
            requestBody.put("quantity", 1);
            requestBody.put("total_amount", amount);
            requestBody.put("tax_free_amount", 0);
            // 프론트 success 페이지가 결제 승인에 필요한 정보를 복원할 수 있도록 쿼리 파라미터 포함
            // (tid는 ready 응답 이후에만 알 수 있으므로 포함 불가)
            requestBody.put("approval_url", buildApprovalUrl(orderId, amount));
            requestBody.put("cancel_url", properties.getCancelUrl());
            requestBody.put("fail_url", properties.getFailUrl());

            log.debug("KakaoPay 결제 준비 요청: cid={}, orderId={}, userId={}, amount={}, itemName={}, approvalUrl={}",
                    properties.getCid(), orderId, userId, amount, itemName, requestBody.get("approval_url"));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

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
                return new KakaoReadyResponse(tid, redirectUrl, jsonConverter.convertToJson(body));
            }

            throw new RuntimeException("KakaoPay 결제 준비 실패: status=" + response.getStatusCode());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 403 에러에 대한 상세 정보 로깅
            String errorDetails = e.getResponseBodyAsString();
            log.error("KakaoPay 결제 준비 API 호출 실패: orderId={}, status={}, error={}", 
                    orderId, e.getStatusCode(), errorDetails, e);
            
            // 403 에러인 경우 더 명확한 에러 메시지 제공
            if (e.getStatusCode() == org.springframework.http.HttpStatus.FORBIDDEN) {
                log.error("KakaoPay 403 Forbidden - 가능한 원인:");
                log.error("1. PAYMENT_KAKAO_SECRET 환경변수가 올바르게 설정되었는지 확인");
                log.error("2. KakaoPay 개발자 콘솔에서 Secret Key(dev)가 올바른지 확인");
                log.error("3. IP 화이트리스트 설정이 있는지 확인");
                log.error("4. CID({})가 올바른지 확인", properties.getCid());
                throw new RuntimeException(
                    String.format("KakaoPay 인증 실패 (403): %s. PAYMENT_KAKAO_SECRET 환경변수와 KakaoPay 개발자 콘솔 설정을 확인하세요.", 
                        errorDetails != null ? errorDetails : e.getMessage()), e);
            }
            throw new RuntimeException("KakaoPay 결제 준비 실패: " + e.getMessage(), e);
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

    /**
     * KakaoPay approval_url 구성
     *
     * <p>카카오 결제 완료 후 success 페이지로 리다이렉트될 때
     * 프론트가 결제 승인에 필요한 값을 복원할 수 있도록 orderId/amount를 함께 전달합니다.
     */
    private String buildApprovalUrl(String orderId, long amount) {
        // properties.approvalUrl은 "https://yourdomain.com/payment/success" 형태의 base URL을 가정
        // 기존 쿼리 파라미터가 있더라도 안전하게 append 되도록 UriComponentsBuilder 사용
        return UriComponentsBuilder.fromUriString(properties.getApprovalUrl())
                .queryParam("orderId", orderId)
                .queryParam("amount", amount)
                .build(true)
                .toUriString();
    }
}

