package org.example.sharedprompts.domain.payment.provider.toss.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.provider.toss.exception.DuplicateOrderIdException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * TossPay 결제 승인 API 에러 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossConfirmErrorHandler {

    private final ObjectMapper objectMapper;

    public RuntimeException handleHttpServerError(HttpServerErrorException e, String paymentKey, String orderId) {
        String errorDetails = e.getResponseBodyAsString();
        log.error("TossPay 결제 승인 API 호출 실패 (5xx): paymentKey={}, orderId={}, status={}, error={}",
                paymentKey, orderId, e.getStatusCode(), errorDetails, e);

        if (isDuplicateOrderIdError(errorDetails)) {
            log.warn("TossPay 중복 주문번호 오류 감지 (S021): paymentKey={}, orderId={}", paymentKey, orderId);
            return new DuplicateOrderIdException(
                    "TossPay 중복 주문번호 오류 (S021): 이미 사용된 주문번호입니다. " +
                            "결제가 이미 확인되었을 가능성이 있습니다.",
                    paymentKey, orderId, e);
        }

        return new RuntimeException("TossPay 결제 승인 실패 (5xx): " + e.getMessage(), e);
    }

    public RuntimeException handleHttpClientError(HttpClientErrorException e, String paymentKey, String orderId) {
        String errorDetails = e.getResponseBodyAsString();
        log.error("TossPay 결제 승인 API 호출 실패: paymentKey={}, orderId={}, status={}, error={}",
                paymentKey, orderId, e.getStatusCode(), errorDetails, e);

        if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
            String errorMessage = extractErrorMessage(errorDetails);
            return new RuntimeException(
                    String.format("TossPay 인증 실패 (403): %s. PAYMENT_TOSS_SECRET_KEY 환경변수와 TossPayments 개발자 콘솔 설정을 확인하세요.",
                            errorMessage != null ? errorMessage : errorDetails != null ? errorDetails : e.getMessage()), e);
        }

        return new RuntimeException("TossPay 결제 승인 실패: " + e.getMessage(), e);
    }

    private boolean isDuplicateOrderIdError(String errorDetails) {
        if (errorDetails == null) {
            return false;
        }
        return errorDetails.contains("S021") ||
                errorDetails.contains("이미 사용된 주문번호") ||
                errorDetails.contains("FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING");
    }

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

