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
        // intValueExact()를 사용하여 데이터 손실(소수점 절삭, 오버플로우) 시 예외 발생
        // 이는 조용한 데이터 손실을 방지하고 문제를 명시적으로 드러냅니다
        response.setTotalAmount(payment.getAmount().intValueExact());
        if (payment.getApprovedAt() != null) {
            response.setApprovedAt(payment.getApprovedAt().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime());
        }
        response.setMethod(payment.getPaymentMethod().name());
        return response;
    }
}

