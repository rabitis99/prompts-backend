package org.example.sharedprompts.domain.payment.provider.toss.mapper;

import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.springframework.stereotype.Component;

/**
 * TossPay Status Mapper
 * 
 * <p>단일 책임: TossPay 외부 상태 → PaymentStatus 매핑만 담당
 */
@Component
public class TossPayStatusMapper {

    /**
     * TossPay 상태를 PaymentStatus로 매핑
     * 
     * @param status TossPay 상태 (null 가능)
     * @return PaymentStatus (null이면 PENDING 반환)
     */
    public PaymentStatus map(String status) {
        if (status == null || status.isEmpty()) {
            return PaymentStatus.PENDING;
        }

        return switch (status) {
            case "DONE" -> PaymentStatus.SUCCESS;
            case "CANCELED" -> PaymentStatus.CANCELED;
            case "PARTIAL_CANCELED" -> PaymentStatus.PARTIALLY_REFUNDED;
            case "ABORTED", "EXPIRED" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }
}

