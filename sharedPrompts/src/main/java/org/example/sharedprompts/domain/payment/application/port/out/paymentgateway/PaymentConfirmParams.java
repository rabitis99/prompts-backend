package org.example.sharedprompts.domain.payment.application.port.out.paymentgateway;

import java.util.Map;

/**
 * 결제 승인 호출에 필요한 파라미터.
 * 유스케이스(애플리케이션 계층)에서 해석한 뒤 포트로 전달하며,
 * 어댑터는 이 값을 그대로 PG에 전달만 한다.
 */
public record PaymentConfirmParams(
        String paymentKey,
        Map<String, String> additionalParams
) {
    public PaymentConfirmParams {
        additionalParams = additionalParams != null ? additionalParams : java.util.Collections.emptyMap();
    }

    public static PaymentConfirmParams of(String paymentKey, Map<String, String> additionalParams) {
        return new PaymentConfirmParams(paymentKey, additionalParams);
    }
}
