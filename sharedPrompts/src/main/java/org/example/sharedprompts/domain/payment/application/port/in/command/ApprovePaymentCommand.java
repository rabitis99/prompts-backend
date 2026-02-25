package org.example.sharedprompts.domain.payment.application.port.in.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentUserType;

import java.math.BigDecimal;

/**
 * 결제 승인 요청 명령
 * 사용자(userId)가 결제를 요청할 때 사용되는 커맨드
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovePaymentCommand {

    private Long userId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private BigDecimal usePointAmount;
    private PaymentUserType userType;
    private String metadata;

    /**
     * 최소 필수 정보로만 커맨드 생성
     */
    public static ApprovePaymentCommand of(
            Long userId,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod
    ) {
        return ApprovePaymentCommand.builder()
                .userId(userId)
                .amount(amount)
                .currency(currency)
                .paymentMethod(paymentMethod)
                .usePointAmount(BigDecimal.ZERO)
                .build();
    }
}
