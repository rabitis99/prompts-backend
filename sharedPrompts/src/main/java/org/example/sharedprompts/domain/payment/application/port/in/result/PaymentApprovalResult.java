package org.example.sharedprompts.domain.payment.application.port.in.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;

import java.math.BigDecimal;

/**
 * 결제 승인 결과
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApprovalResult {

    private Long paymentId;
    private PaymentStatus status;
    private String externalPaymentId;
    private BigDecimal amount;
    private String currency;
    /** 결제 준비 시 PG사에서 받은 메타데이터(JSON 문자열). 카카오페이 시 next_redirect_pc_url 등 포함 */
    private String metadata;
}
