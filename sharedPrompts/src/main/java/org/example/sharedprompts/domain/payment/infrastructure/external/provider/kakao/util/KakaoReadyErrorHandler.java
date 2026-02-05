package org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * KakaoPay 결제 준비 API 에러 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoReadyErrorHandler {

    public RuntimeException handleHttpClientError(HttpClientErrorException e, String orderId) {
        String masked = SensitiveDataMasker.maskSensitiveData(e.getResponseBodyAsString());
        log.error("KakaoPay ready failed: orderId={}, status={}, error={}",
                orderId, e.getStatusCode(), masked);

        if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
            return new RuntimeException("KakaoPay 인증 실패 (403). 설정값을 확인하세요.", e);
        }
        return new RuntimeException("KakaoPay 결제 준비 실패", e);
    }

    public RuntimeException handleRestClientError(RestClientException e, String orderId) {
        log.error("KakaoPay ready failed: orderId={}, message={}", 
                orderId, SensitiveDataMasker.maskSensitiveData(e.getMessage()));
        return new RuntimeException("KakaoPay 결제 준비 실패", e);
    }
}

