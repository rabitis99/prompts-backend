package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * KakaoPay 결제 상태 조회 API 에러 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoStatusErrorHandler {

    public RuntimeException handleRestClientError(RestClientException e, String tid) {
        log.error("KakaoPay status failed: tid={}, message={}", tid, e.getMessage());
        return new RuntimeException("KakaoPay 결제 상태 조회 실패", e);
    }
}

