package org.example.sharedprompts.domain.payment.application.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 환불 결과 모델
 */
@Getter
@Builder
public class RefundResult {

    /**
     * 외부 결제사에서 발급한 결제 ID
     */
    private final String externalPaymentId;

    /**
     * 환불 후 상태
     */
    private final PaymentStatus status;

    /**
     * 환불 금액
     */
    private final BigDecimal refundedAmount;

    /**
     * 환불 시각
     */
    private final LocalDateTime refundedAt;

    /**
     * 환불 사유
     */
    private final String reason;

    /**
     * 추가 메타데이터 (JSON 형태)
     */
    private final String metadata;

    /**
     * 환불 성공 여부
     */
    public boolean isSuccess() {
        return status == PaymentStatus.REFUNDED || status == PaymentStatus.PARTIALLY_REFUNDED;
    }
}
