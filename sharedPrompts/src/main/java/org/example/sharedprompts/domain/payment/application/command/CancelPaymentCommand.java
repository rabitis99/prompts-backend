package org.example.sharedprompts.domain.payment.application.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CancelPaymentCommand {
    private final Long userId;
    private final Long paymentId;
    private final String reason;
}

