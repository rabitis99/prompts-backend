package org.example.sharedprompts.domain.payment.provider.paypal.dto;

/**
 * PayPal 주문 생성 응답
 */
public record PaypalCreateOrderResponse(String orderId, String approveUrl, String metadata) {
    public PaypalCreateOrderResponse {
        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalArgumentException("orderId는 필수입니다");
        }
    }
}
