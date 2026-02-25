package org.example.sharedprompts.domain.payment.application.port.in.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;

import java.math.BigDecimal;

/**
 * 결제 환불 결과
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundResult {

    private Long paymentId;
    private PaymentStatus status;
    private BigDecimal refundAmount;
    private String reason;
}
