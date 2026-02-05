package org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * TossPay 결제 취소/환불 API 에러 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossCancelErrorHandler {

    public RuntimeException handleRestClientError(RestClientException e, String paymentKey) {
        log.error("TossPay 취소/환불 API 호출 실패: paymentKey={}", 
                SensitiveDataMasker.maskPaymentKey(paymentKey), e);
        return new RuntimeException("TossPay 취소/환불 실패", e);
    }
}

