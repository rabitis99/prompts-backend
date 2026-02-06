package org.example.sharedprompts.domain.payment.infrastructure.messaging.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.application.dto.response.CancelResult;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.application.dto.response.RefundResult;

import java.math.BigDecimal;

/**
 * 결제 이벤트
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentEvent {

    /**
     * 결제 실패 이벤트
     */
    public record PaymentFailed(Long paymentId, Long userId, String reason, String paymentMethod) {}

    /**
     * 결제 성공 이벤트
     */
    public record PaymentSucceeded(Long paymentId, Long userId, String paymentMethod) {}

    /**
     * 결제 취소 이벤트
     */
    public record PaymentCanceled(Long paymentId, Long userId, String reason) {}

    /**
     * 결제 환불 이벤트
     */
    public record PaymentRefunded(Long paymentId, Long userId, String reason) {}

    public record PaymentResultApplied(
            Long paymentId,
            PaymentResult result,
            BigDecimal actualAmount,
            String idempotencyKey
    ) {}

    public record CancelResultApplied(
            Long paymentId,
            CancelResult result
    ) {}

    public record RefundResultApplied(
            Long paymentId,
            RefundResult result,
            BigDecimal refundAmount,
            String idempotencyKey
    ) {}
}

