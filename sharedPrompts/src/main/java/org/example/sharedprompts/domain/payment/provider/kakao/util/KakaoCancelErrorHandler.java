package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * KakaoPay 결제 취소/환불 API 에러 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoCancelErrorHandler {

    public RuntimeException handleCancelError(RestClientException e, String tid) {
        log.error("KakaoPay cancel failed: tid={}, message={}", tid, e.getMessage());
        return new RuntimeException("KakaoPay 결제 취소 실패", e);
    }

    public RuntimeException handleRefundError(RestClientException e, String tid, long amount) {
        log.error("KakaoPay refund failed: tid={}, amount={}, message={}",
                tid, amount, e.getMessage());
        return new RuntimeException("KakaoPay 결제 환불 실패", e);
    }
}

