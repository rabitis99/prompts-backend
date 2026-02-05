package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * KakaoPay 결제 승인 API 에러 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoApproveErrorHandler {

    public RuntimeException handleHttpClientError(HttpClientErrorException e, String tid, String orderId) {
        String errorDetails = e.getResponseBodyAsString();
        String masked = errorDetails != null
                ? SensitiveDataMasker.maskSensitiveData(errorDetails)
                : null;

        log.error(
                "KakaoPay approve HTTP 오류: tid={}, orderId={}, status={}, body={}",
                tid, orderId, e.getStatusCode(), masked
        );

        if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
            return new RuntimeException("KakaoPay 인증 실패 (403). 설정값을 확인하세요.", e);
        }

        return new RuntimeException("KakaoPay 결제 승인 HTTP 오류: " + e.getStatusCode(), e);
    }

    public RuntimeException handleRestClientError(RestClientException e, String tid, String orderId) {
        log.error(
                "KakaoPay approve 통신 오류: tid={}, orderId={}",
                tid, orderId, e
        );
        return new RuntimeException("KakaoPay 결제 승인 통신 실패", e);
    }
}

