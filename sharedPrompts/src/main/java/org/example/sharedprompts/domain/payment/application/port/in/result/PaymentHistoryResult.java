package org.example.sharedprompts.domain.payment.application.port.in.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 내역 아이템
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResult {

    private Long id;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
}
