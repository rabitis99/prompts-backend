package org.example.sharedprompts.domain.payment.provider.kakao.mapper;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.springframework.stereotype.Component;

@Slf4j
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
            default -> {
                log.warn("알 수 없는 KakaoPay 상태값: {}", status);
                yield PaymentStatus.PENDING;
            }
        };
    }
}