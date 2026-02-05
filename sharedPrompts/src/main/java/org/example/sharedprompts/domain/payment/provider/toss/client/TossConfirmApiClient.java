package org.example.sharedprompts.domain.payment.provider.toss.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.properties.TossPayProperties;
import org.example.sharedprompts.domain.payment.provider.toss.dto.TossConfirmResponse;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossPayJsonConverter;
import org.example.sharedprompts.domain.payment.provider.toss.util.TossPayResponseParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.example.sharedprompts.domain.payment.provider.toss.exception.DuplicateOrderIdException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * TossPay 결제 승인 API Client (v1 API)
 *
 * <p>단일 책임: 결제 승인 API 호출만 담당
 * <p>API: POST /v1/payments/confirm
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class TossConfirmApiClient {

    private final TossPayProperties properties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final TossPayHeadersProvider headersProvider;
    private final TossPayJsonConverter jsonConverter;
    private final TossPayResponseParser responseParser;
    private final ObjectMapper objectMapper;

    /**
     * 결제 승인 요청
     *
     * @param paymentKey 결제 키 (필수)
     * @param orderId 주문 ID (필수)
     * @param amount 결제 금액 (원 단위, 필수)
     * @return ConfirmResponse
     * @throws IllegalArgumentException 필수 필드 누락 시
     * @throws RuntimeException API 호출 실패 시
     */
    public TossConfirmResponse confirm(String paymentKey, String orderId, long amount) {
        validateRequired(paymentKey, "paymentKey");
        validateRequired(orderId, "orderId");
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다: amount=" + amount);
        }

        try {
            HttpHeaders headers = headersProvider.createJsonHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("paymentKey", paymentKey);
            requestBody.put("orderId", orderId);
            requestBody.put("amount", amount);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    properties.getBaseUrl() + "/confirm",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String status = (String) body.get("status");
                Object totalAmountObj = body.get("totalAmount");
                String currency = (String) body.get("currency");
                String orderIdFromResponse = (String) body.get("orderId");

                if (status == null || status.isEmpty()) {
                    throw new RuntimeException("TossPay confirm 응답에 status가 없습니다");
                }
                if (totalAmountObj == null) {
                    throw new RuntimeException("TossPay confirm 응답에 totalAmount가 없습니다");
                }
                if (currency == null || currency.isEmpty()) {
                    throw new RuntimeException("TossPay confirm 응답에 currency가 없습니다");
                }
                if (orderIdFromResponse == null || orderIdFromResponse.isEmpty()) {
                    throw new RuntimeException("TossPay confirm 응답에 orderId가 없습니다");
                }

                BigDecimal totalAmount;
                try {
                    totalAmount = new BigDecimal(totalAmountObj.toString());
                } catch (NumberFormatException e) {
                    log.error("TossPay confirm 응답의 totalAmount 형식이 올바르지 않습니다: {}", totalAmountObj);
                    throw new RuntimeException("TossPay confirm 응답의 totalAmount 형식이 올바르지 않습니다: " + totalAmountObj, e);
                }

                log.info("TossPay 결제 승인 성공: paymentKey={}, orderId={}, status={}", paymentKey, orderId, status);
                return new TossConfirmResponse(
                        paymentKey,
                        status,
                        totalAmount,
                        currency,
                        orderIdFromResponse,
                        responseParser.parseApprovedAt(body),
                        jsonConverter.convertToJson(body)
                );
            }

            throw new RuntimeException("TossPay 결제 승인 실패: status=" + response.getStatusCode());
        } catch (HttpServerErrorException e) {
            // HTTP 5xx 에러 처리 (S021 중복 주문번호 오류 포함)
            String errorDetails = e.getResponseBodyAsString();
            log.error("TossPay 결제 승인 API 호출 실패 (5xx): paymentKey={}, orderId={}, status={}, error={}", 
                    paymentKey, orderId, e.getStatusCode(), errorDetails, e);
            
            // S021 오류 감지: 이미 사용된 주문번호
            if (errorDetails != null && (errorDetails.contains("S021") || 
                errorDetails.contains("이미 사용된 주문번호") ||
                errorDetails.contains("FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING"))) {
                log.warn("TossPay 중복 주문번호 오류 감지 (S021): paymentKey={}, orderId={}. " +
                        "결제가 이미 확인되었을 가능성이 있습니다. Payment 상태를 확인하세요.", 
                        paymentKey, orderId);
                throw new DuplicateOrderIdException(
                    "TossPay 중복 주문번호 오류 (S021): 이미 사용된 주문번호입니다. " +
                    "결제가 이미 확인되었을 가능성이 있습니다.",
                    paymentKey, orderId, e);
            }
            
            throw new RuntimeException("TossPay 결제 승인 실패 (5xx): " + e.getMessage(), e);
        } catch (HttpClientErrorException e) {
            // HTTP 4xx 에러에 대한 상세 정보 로깅
            String errorDetails = e.getResponseBodyAsString();
            log.error("TossPay 결제 승인 API 호출 실패: paymentKey={}, orderId={}, status={}, error={}", 
                    paymentKey, orderId, e.getStatusCode(), errorDetails, e);
            
            // 403 에러인 경우 더 명확한 에러 메시지 제공
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("TossPay 403 Forbidden - 가능한 원인:");
                log.error("1. PAYMENT_TOSS_SECRET_KEY 환경변수가 올바르게 설정되었는지 확인");
                log.error("2. TossPayments 개발자 콘솔에서 Secret Key가 올바른지 확인");
                log.error("3. API Key와 Secret Key가 동일한 환경(테스트/프로덕션)에 속하는지 확인");
                log.error("4. IP 화이트리스트 설정이 있는지 확인");
                log.error("5. paymentKey({})가 유효하고 만료되지 않았는지 확인", paymentKey);
                log.error("6. orderId({})가 올바른 형식인지 확인", orderId);
                
                // 에러 응답에서 code와 message 추출 시도
                String errorMessage = extractErrorMessage(errorDetails);
                throw new RuntimeException(
                    String.format("TossPay 인증 실패 (403): %s. PAYMENT_TOSS_SECRET_KEY 환경변수와 TossPayments 개발자 콘솔 설정을 확인하세요.", 
                        errorMessage != null ? errorMessage : errorDetails != null ? errorDetails : e.getMessage()), e);
            }
            throw new RuntimeException("TossPay 결제 승인 실패: " + e.getMessage(), e);
        } catch (RestClientException e) {
            log.error("TossPay 결제 승인 API 호출 실패: paymentKey={}, orderId={}, error={}", paymentKey, orderId, e.getMessage(), e);
            throw new RuntimeException("TossPay 결제 승인 실패: " + e.getMessage(), e);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }

    /**
     * TossPayments 에러 응답에서 메시지 추출
     * 
     * @param errorResponseBody JSON 형식의 에러 응답 본문
     * @return 추출된 에러 메시지, 파싱 실패 시 null
     */
    private String extractErrorMessage(String errorResponseBody) {
        if (errorResponseBody == null || errorResponseBody.isEmpty()) {
            return null;
        }
        
        try {
            JsonNode root = objectMapper.readTree(errorResponseBody);
            String code = root.path("code").asText(null);
            String message = root.path("message").asText(null);
            
            if (code != null && message != null) {
                return String.format("code=%s, message=%s", code, message);
            } else if (message != null) {
                return message;
            }
        } catch (Exception e) {
            log.debug("에러 응답 파싱 실패: {}", errorResponseBody, e);
        }
        
        return errorResponseBody;
    }
}
