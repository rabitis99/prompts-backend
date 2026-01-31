package org.example.sharedprompts.domain.payment.service.facade;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AmountProcessingResult {
    private final BigDecimal originalAmount;
    private final BigDecimal convertedAmount;
    private final BigDecimal usedPointAmount;
    private final BigDecimal actualPaymentAmount;
}
