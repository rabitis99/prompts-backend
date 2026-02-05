package org.example.sharedprompts.domain.payment.application.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 취소 결과 모델
 */
@Getter
@Builder
public class CancelResult {

    /**
     * 외부 결제사에서 발급한 결제 ID
     */
    private final String externalPaymentId;

    /**
     * 취소 후 상태
     */
    private final PaymentStatus status;

    /**
     * 취소 시각
     */
    private final LocalDateTime canceledAt;

    /**
     * 취소 사유
     */
    private final String reason;

    /**
     * 추가 메타데이터 (JSON 형태)
     */
    private final String metadata;

    /**
     * 원본 결제 금액 (카카오페이 취소 시 사용, 선택적)
     */
    private final BigDecimal originalAmount;

    /**
     * 면세 금액 (카카오페이 취소 시 사용, 선택적)
     */
    private final BigDecimal taxFreeAmount;

    /**
     * 취소 성공 여부
     */
    public boolean isSuccess() {
        return status == PaymentStatus.CANCELED;
    }
}
