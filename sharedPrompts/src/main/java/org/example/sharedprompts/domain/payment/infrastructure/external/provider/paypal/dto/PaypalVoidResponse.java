package org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.dto;

import java.time.Instant;

/**
 * PayPal Authorization void 응답
 */
public record PaypalVoidResponse(Instant canceledAt, String metadata) {
}
