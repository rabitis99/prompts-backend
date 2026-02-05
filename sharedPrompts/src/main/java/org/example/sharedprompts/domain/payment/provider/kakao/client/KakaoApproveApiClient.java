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
import org.springframework.web.client.HttpClientErrorException;
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
                    new org.springframework.core.ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("KakaoPay approve 응답이 비정상입니다: status=" + response.getStatusCode());
            }

            Map<String, Object> body = response.getBody();

            String errorCode = (String) body.get("code");
            String errorMsg = (String) body.get("msg");
            String error = (String) body.get("error");

            if (errorCode != null || errorMsg != null || error != null) {
                String responseBodyJson = jsonConverter.convertToJson(body);
                log.error(
                        "KakaoPay approve 에러 응답: tid={}, orderId={}, code={}, msg={}, error={}",
                        tid, orderId, errorCode, errorMsg, error
                );
                throw new RuntimeException("KakaoPay approve 에러 응답: " + responseBodyJson);
            }

            String status = (String) body.get("status");
            if (status == null || status.isEmpty()) {
                Object approvedAt = body.get("approved_at");
                if (approvedAt != null) {
                    status = DEFAULT_SUCCESS_STATUS;
                    log.warn(
                            "KakaoPay approve 응답에 status가 없어 approved_at 기준으로 성공 처리: tid={}, orderId={}",
                            tid, orderId
                    );
                } else {
                    String responseBodyJson = jsonConverter.convertToJson(body);
                    log.error(
                            "KakaoPay approve 응답에 status/approved_at 모두 없음: tid={}, orderId={}",
                            tid, orderId
                    );
                    throw new RuntimeException("KakaoPay approve 응답 형식 오류: " + responseBodyJson);
                }
            }

            return new KakaoApproveResponse(
                    status,
                    responseParser.parseApprovedAt(body),
                    jsonConverter.convertToJson(body)
            );

        } catch (HttpClientErrorException e) {
            String errorDetails = e.getResponseBodyAsString();
            String masked = errorDetails != null
                    ? SensitiveDataMasker.maskSensitiveData(errorDetails)
                    : null;

            log.error(
                    "KakaoPay approve HTTP 오류: tid={}, orderId={}, status={}, body={}",
                    tid, orderId, e.getStatusCode(), masked
            );

            throw new RuntimeException("KakaoPay 결제 승인 HTTP 오류: " + e.getStatusCode(), e);

        } catch (RestClientException e) {
            log.error(
                    "KakaoPay approve 통신 오류: tid={}, orderId={}",
                    tid, orderId, e
            );
            throw new RuntimeException("KakaoPay 결제 승인 통신 실패", e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}
