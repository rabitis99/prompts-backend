package org.example.sharedprompts.domain.payment.provider.paypal.dto;

import java.time.LocalDateTime;

/**
 * PayPal Authorization void 응답
 */
public record PaypalVoidResponse(LocalDateTime canceledAt, String metadata) {
}
