package org.example.sharedprompts.domain.payment.service.facade;

import java.math.BigDecimal;

public record AmountProcessingResult(
        BigDecimal originalAmount,
        BigDecimal convertedAmount,
        BigDecimal usedPointAmount,
        BigDecimal actualPaymentAmount
) {}
