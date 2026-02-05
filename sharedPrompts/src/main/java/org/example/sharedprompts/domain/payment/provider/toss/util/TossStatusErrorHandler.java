package org.example.sharedprompts.domain.payment.provider.toss.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * TossPay 결제 상태 조회 API 에러 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossStatusErrorHandler {

    public RuntimeException handleHttpClientError(HttpClientErrorException e, String paymentKey) {
        String errorDetails = e.getResponseBodyAsString();
        log.error("TossPay 결제 상태 조회 API 호출 실패: paymentKey={}, status={}, error={}",
                paymentKey, e.getStatusCode(), errorDetails, e);

        if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
            return new RuntimeException(
                    "TossPay 인증 실패 (403). PAYMENT_TOSS_SECRET_KEY 환경변수와 TossPayments 개발자 콘솔 설정을 확인하세요.", e);
        }

        return new RuntimeException("TossPay 결제 상태 조회 실패: " + e.getMessage(), e);
    }

    public RuntimeException handleHttpServerError(HttpServerErrorException e, String paymentKey) {
        String errorDetails = e.getResponseBodyAsString();
        log.error("TossPay 결제 상태 조회 API 호출 실패 (5xx): paymentKey={}, status={}, error={}",
                paymentKey, e.getStatusCode(), errorDetails, e);
        return new RuntimeException("TossPay 결제 상태 조회 실패 (5xx): " + e.getMessage(), e);
    }
}

