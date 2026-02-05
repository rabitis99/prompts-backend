package org.example.sharedprompts.domain.payment.application.command.service.confirm;

import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.dto.payment.response.PaymentConfirmResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentConfirmResponseMapper {

    public PaymentConfirmResponse toConfirmResponse(Payment payment, PaymentConfirmRequest request) {
        PaymentConfirmResponse response = new PaymentConfirmResponse();
        response.setPaymentKey(payment.getExternalPaymentId());
        response.setOrderId(request.getOrderId());
        response.setStatus(payment.getStatus().name());
        response.setTotalAmount(payment.getAmount().intValue());
        if (payment.getApprovedAt() != null) {
            response.setApprovedAt(payment.getApprovedAt().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime());
        }
        response.setMethod(payment.getPaymentMethod().name());
        return response;
    }
}

