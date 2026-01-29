package org.example.sharedprompts.dto.payment.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토스페이먼츠 결제 승인 요청 DTO
 */
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {
    private String orderId;
    private int amount;
    private String paymentKey;

    public PaymentConfirmRequest(String orderId, int amount, String paymentKey) {
        this.orderId = orderId;
        this.amount = amount;
        this.paymentKey = paymentKey;
    }
}

