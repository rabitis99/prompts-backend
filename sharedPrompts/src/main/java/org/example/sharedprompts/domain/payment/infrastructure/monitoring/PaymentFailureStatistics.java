package org.example.sharedprompts.domain.payment.infrastructure.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Map;

/**
 * 결제 실패 통계
 */
@Getter
@Builder
@AllArgsConstructor
public class PaymentFailureStatistics {
    private final LocalDate date;
    private final long totalFailures;
    private final Map<String, Long> failuresByMethod;
}

