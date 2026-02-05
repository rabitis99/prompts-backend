package org.example.sharedprompts.domain.payment.provider.kakao.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.properties.KakaoPayProperties;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoApproveResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayJsonConverter;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayResponseParser;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * KakaoPay 결제 승인 API Client
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class KakaoApproveApiClient {

    private static final String KAKAO_PAY_API_URL = "https://open-api.kakaopay.com/online/v1/payment";
    private static final String APPROVE_ENDPOINT = "/approve";
    private static final String DEFAULT_SUCCESS_STATUS = "SUCCESS_PAYMENT";

    private final KakaoPayProperties properties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final KakaoPayHeadersProvider headersProvider;
    private final KakaoPayJsonConverter jsonConverter;
    private final KakaoPayResponseParser responseParser;

    public KakaoApproveResponse approve(String tid, String orderId, String userId, String pgToken) {
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
                
                String errorCode = (String) body.get("code");
                String errorMsg = (String) body.get("msg");
                String error = (String) body.get("error");
                
                if (errorCode != null || errorMsg != null || error != null) {
                    String responseBodyJson = jsonConverter.convertToJson(body);
                    String errorMessage = String.format(
                            "KakaoPay approve 에러 응답: code=%s, msg=%s, error=%s, responseBody=%s", 
                            errorCode, errorMsg, error, responseBodyJson);
                    log.error("KakaoPay approve API 에러: tid={}, orderId={}, {}", tid, orderId, errorMessage);
                    throw new RuntimeException(errorMessage);
                }
                
                if (status == null || status.isEmpty()) {
                    Object approvedAt = body.get("approved_at");
                    if (approvedAt != null) {
                        status = DEFAULT_SUCCESS_STATUS;
                        log.debug("KakaoPay approve 응답에 status가 없지만 approved_at이 있어 성공으로 간주: tid={}, orderId={}", 
                                tid, orderId);
                    } else {
                        String responseBodyJson = jsonConverter.convertToJson(body);
                        log.error("KakaoPay approve 응답에 status와 approved_at이 모두 없습니다: tid={}, orderId={}, responseBody={}", 
                                tid, orderId, responseBodyJson);
                        throw new RuntimeException(
                                String.format("KakaoPay approve 응답에 status와 approved_at이 모두 없습니다. 응답 본문: %s", responseBodyJson));
                    }
                }

                log.info("KakaoPay 결제 승인 성공: tid={}, orderId={}, status={}", tid, orderId, status);
                return new KakaoApproveResponse(
                        status,
                        responseParser.parseApprovedAt(body),
                        jsonConverter.convertToJson(body)
                );
            }

            throw new RuntimeException("KakaoPay 결제 승인 실패: status=" + response.getStatusCode());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String errorDetails = e.getResponseBodyAsString();
            String maskedErrorDetails = errorDetails != null ? SensitiveDataMasker.maskSensitiveData(errorDetails) : null;
            log.error("KakaoPay 결제 승인 API 호출 실패: tid={}, orderId={}, status={}, error={}", 
                    tid, orderId, e.getStatusCode(), maskedErrorDetails, e);
            
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("KakaoPay 403 Forbidden - tid={}, orderId={}, 가능한 원인: " +
                        "1) PAYMENT_KAKAO_SECRET 환경변수 확인, " +
                        "2) Secret Key(dev) 확인, " +
                        "3) IP 화이트리스트 확인, " +
                        "4) CID 확인", tid, orderId, properties.getCid());
                throw new RuntimeException(
                    String.format("KakaoPay 인증 실패 (403): %s. PAYMENT_KAKAO_SECRET 환경변수와 KakaoPay 개발자 콘솔 설정을 확인하세요.", 
                        errorDetails != null ? errorDetails : e.getMessage()), e);
            }
            throw new RuntimeException("KakaoPay 결제 승인 실패: " + e.getMessage(), e);
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

