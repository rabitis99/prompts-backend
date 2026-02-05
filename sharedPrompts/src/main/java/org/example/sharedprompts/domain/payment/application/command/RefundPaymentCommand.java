package org.example.sharedprompts.domain.payment.application.command;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RefundPaymentCommand {
    private final Long userId;
    private final Long paymentId;
    private final BigDecimal amount;
    private final String reason;
}

