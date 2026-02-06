package org.example.sharedprompts.domain.payment.application.command.service.confirm;

import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.dto.payment.response.PaymentConfirmResponse;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.ZoneId;

@Component
public class PaymentConfirmResponseMapper {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    public PaymentConfirmResponse toConfirmResponse(Payment payment, PaymentConfirmRequest request) {
        PaymentConfirmResponse response = new PaymentConfirmResponse();
        response.setPaymentKey(payment.getExternalPaymentId());
        response.setOrderId(request.getOrderId());
        response.setStatus(payment.getStatus().name());
        response.setTotalAmount(payment.getAmount().setScale(0, RoundingMode.HALF_UP).intValue());
        if (payment.getApprovedAt() != null) {
            response.setApprovedAt(payment.getApprovedAt().atZone(ZONE_SEOUL).toOffsetDateTime());
        }
        response.setMethod(payment.getPaymentMethod().name());
        return response;
    }
}

