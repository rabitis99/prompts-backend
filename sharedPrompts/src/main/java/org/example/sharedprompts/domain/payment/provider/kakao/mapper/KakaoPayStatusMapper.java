package org.example.sharedprompts.domain.payment.provider.kakao.mapper;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KakaoPayStatusMapper {

    public PaymentStatus map(String status) {
        if (status == null) {
            log.warn("KakaoPay status is null");
            return PaymentStatus.PENDING;
        }

        return switch (status) {
            case "READY", "SEND_TMS", "OPEN_PAYMENT" ->
                    PaymentStatus.PENDING;

            case "SUCCESS_PAYMENT" ->
                    PaymentStatus.SUCCESS;

            case "CANCEL_PAYMENT" ->
                    PaymentStatus.CANCELED;

            case "PART_CANCEL_PAYMENT" ->
                    PaymentStatus.PARTIALLY_REFUNDED;

            case "FAIL_PAYMENT", "ABORTED", "EXPIRED" ->
                    PaymentStatus.FAILED;

            default -> {
                log.error("정의되지 않은 KakaoPay 상태값 수신: {}", status);
                yield PaymentStatus.UNKNOWN;
            }
        };
    }

}