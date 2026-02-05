package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * KakaoPay 결제 상태 조회 API 에러 처리
 */
@Slf4j
@Component
public class KakaoStatusErrorHandler {

    public RuntimeException handleHttpClientError(HttpClientErrorException e, String tid) {
        String errorDetails = e.getResponseBodyAsString();
        String masked = errorDetails != null
                ? SensitiveDataMasker.maskSensitiveData(errorDetails)
                : null;

        log.error(
                "KakaoPay status HTTP 오류: tid={}, status={}, body={}",
                tid, e.getStatusCode(), masked
        );

        if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
            return new RuntimeException("KakaoPay 인증 실패 (403). 설정값을 확인하세요.", e);
        }

        return new RuntimeException("KakaoPay 결제 상태 조회 HTTP 오류: " + e.getStatusCode(), e);
    }

    public RuntimeException handleRestClientError(RestClientException e, String tid) {
        log.error("KakaoPay status failed: tid={}, message={}", tid, e.getMessage());
        return new RuntimeException("KakaoPay 결제 상태 조회 실패", e);
    }
}

