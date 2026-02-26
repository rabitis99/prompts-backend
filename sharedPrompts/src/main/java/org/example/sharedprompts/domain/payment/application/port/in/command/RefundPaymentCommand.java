package org.example.sharedprompts.domain.payment.application.port.in.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 결제 환불 명령
 * 전체/부분 환불 모두 지원 (refundAmount null이면 전체 환불)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundPaymentCommand {

    private Long paymentId;
    private Long userId;
    private BigDecimal refundAmount; // null이면 전체 환불
    private String reason;
}
