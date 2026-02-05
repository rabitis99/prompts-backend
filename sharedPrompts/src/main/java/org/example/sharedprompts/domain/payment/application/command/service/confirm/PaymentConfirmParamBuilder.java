package org.example.sharedprompts.domain.payment.application.command.service.confirm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.service.PaymentValidator;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConfirmParamBuilder {

    private final PaymentValidator paymentValidator;

    public Map<String, String> buildAdditionalParams(Payment payment, PaymentConfirmRequest request) {
        Map<String, String> additionalParams = new HashMap<>();
        if (payment.getPaymentMethod() == PaymentMethod.KAKAO_PAY) {
            paymentValidator.validateKakaoPayPgToken(request.getPgToken());
            additionalParams.put("pgToken", request.getPgToken());
        }
        if (payment.getPaymentMethod() == PaymentMethod.TOSS && request.getTossOrderId() != null && !request.getTossOrderId().isEmpty()) {
            additionalParams.put("tossOrderId", request.getTossOrderId());
            log.debug("Toss Payments orderId를 프론트엔드에서 받은 값으로 사용: tossOrderId={}, paymentId={}", 
                    request.getTossOrderId(), payment.getId());
        }
        return additionalParams;
    }
}

