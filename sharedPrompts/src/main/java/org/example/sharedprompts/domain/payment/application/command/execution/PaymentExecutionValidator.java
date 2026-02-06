package org.example.sharedprompts.domain.payment.application.command.execution;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class PaymentExecutionValidator {

    public void validatePaymentForExecution(Payment payment) {
        if (payment.getExternalPaymentId() == null || payment.getExternalPaymentId().isEmpty()) {
            if (payment.getRetryCount() > 0) {
                throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, 
                        "재시도할 수 없습니다. paymentKey가 없습니다. 새로운 결제를 요청해주세요.");
            }
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, 
                    "결제 승인을 위해서는 paymentKey가 필요합니다. /payments/confirm 엔드포인트를 사용해주세요.");
        }
    }

    public void validateExternalPaymentId(Payment payment) {
        if (payment.getExternalPaymentId() == null || payment.getExternalPaymentId().isEmpty()) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "외부 결제 ID가 없습니다.");
        }
    }

    public String determineOrderId(Payment payment, Map<String, String> additionalParams) {
        if (payment.getPaymentMethod() == PaymentMethod.TOSS && additionalParams != null) {
            String tossOrderId = additionalParams.get("tossOrderId");
            if (tossOrderId != null && !tossOrderId.isEmpty()) {
                log.debug("Toss Payments orderId를 프론트엔드에서 받은 값으로 사용: tossOrderId={}, paymentId={}", 
                        tossOrderId, payment.getId());
                return tossOrderId;
            }
        }
        return String.valueOf(payment.getId());
    }
}

