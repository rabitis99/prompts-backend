package org.example.sharedprompts.domain.payment.provider.kakao.mapper;

import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class KakaoPayStatusMapper {

    public PaymentStatus map(String status) {
        if (status == null) {
            return PaymentStatus.PENDING;
        }

        return switch (status) {
            case "SUCCESS_PAYMENT" -> PaymentStatus.SUCCESS;
            case "CANCEL_PAYMENT" -> PaymentStatus.CANCELED;
            case "PART_CANCEL_PAYMENT" -> PaymentStatus.PARTIALLY_REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }
}