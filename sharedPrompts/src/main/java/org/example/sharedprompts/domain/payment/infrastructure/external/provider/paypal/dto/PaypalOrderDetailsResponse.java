package org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.dto;

/**
 * PayPal 주문 상세 정보 응답
 */
public record PaypalOrderDetailsResponse(
        String status,
        String authorizationId,
        String captureId,
        String currency,
        String metadata
) {}
