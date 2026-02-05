package org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.mapper;

import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.springframework.stereotype.Component;

/**
 * PayPal Status Mapper
 * 
 * <p>단일 책임: PayPal 외부 상태 → PaymentStatus 매핑만 담당
 */
@Component
public class PayPalStatusMapper {

    /**
     * PayPal 상태를 PaymentStatus로 매핑
     * 
     * @param status PayPal 상태 (null 가능)
     * @return PaymentStatus (null이면 PENDING 반환)
     */
    public PaymentStatus map(String status) {
        if (status == null || status.isEmpty()) {
            return PaymentStatus.PENDING;
        }

        return switch (status) {
            case "COMPLETED" -> PaymentStatus.SUCCESS;
            case "CANCELLED" -> PaymentStatus.CANCELED;
            case "PARTIALLY_REFUNDED" -> PaymentStatus.PARTIALLY_REFUNDED;
            case "REFUNDED" -> PaymentStatus.REFUNDED;
            case "VOIDED" -> PaymentStatus.CANCELED;
            case "PAYER_ACTION_REQUIRED" -> PaymentStatus.PENDING;
            case "DECLINED" -> PaymentStatus.FAILED;
            case "FAILED" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }
}

